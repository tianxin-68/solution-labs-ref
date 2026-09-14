package com.bi.queryer.sys.authapply;

import com.bi.queryer.ssm.query.field.QueryFieldService;
import com.bi.queryer.sys.authapply.model.TaskParam;
import com.bi.queryer.sys.base.BaseDao;
import com.bi.queryer.sys.common.ResponseMessage;
import com.bi.queryer.sys.config.SC;
import com.bi.queryer.sys.interceptor.OperLog;
import com.bi.queryer.sys.menu.MenuService;
import com.bi.queryer.sys.user.UserManager;
import com.bi.queryer.sys.user.constant.AuthApplyType;
import com.bi.queryer.sys.user.constant.SsdDataAuthApplyType;
import com.bi.queryer.sys.user.model.HrEmployee;
import com.bi.queryer.sys.user.model.SysAuthApplyRecord;
import com.bi.queryer.sys.user.vo.SsdDataAuthVo;
import com.bi.queryer.sys.user.vo.SsmAuthApplyResultVo;
import com.bi.queryer.sys.user.vo.SsmAuthApplyVo;
import com.bi.queryer.sys.user.vo.User;
import com.bi.queryer.util.BIUtil;
import com.alibaba.fastjson.JSON;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class SsmAuthApplyService {

	private static final String NO_MODULE_AUTH_MSG = "您暂时没有模块权限，请先选择要查看的模块";
	private static final String DIRECT_APPLY_TASK_ID = "";

	private final Integer rptTaskConfigId = Integer.parseInt(SC.v("ssm.workorder.api.task.config.id", "2913"));
	private final String rptSystemName = SC.v("ssm.workorder.api.task.config.system.name", "多维分析权限申请");
	private final String createTaskPath = SC.v("portal.workorder.api.url",
			"http://gateway.example.com:9010/workorder-engine/api/workOrderEngine/outerCreateWorkOrder");

	@Autowired
	private BaseDao dao;

	@Autowired
	private MenuService menuService;

	@Autowired
	private AuthApplyService authApplyService;

	@Autowired
	private QueryFieldService queryFieldService;

	@OperLog(operModule = "用户申请多维分析权限")
	public ResponseMessage ssmAuthApply(SsmAuthApplyVo ssmAuthApplyVo) {
		User user = UserManager.get();
		if (user == null) {
			throw new RuntimeException("获取用户信息失败");
		}

		HrEmployee hrEmployee = queryHrEmployee(user);
		List<SsdDataAuthVo> ctgList = normalizeList(ssmAuthApplyVo.getCtgList());
		List<SsdDataAuthVo> dataAuthList = normalizeList(ssmAuthApplyVo.getDataAuthList());

		// 1. 仅删除行级权限且无模块新增：直接生效，不建工单，但写入申请记录
		if (ctgList.isEmpty() && isOnlyRowAuthDelete(dataAuthList)) {
			authApplyService.applyRowDataAuthImmediately(user.getName(), dataAuthList);
			SysAuthApplyRecord record = buildApplyRecord(user, ssmAuthApplyVo, DIRECT_APPLY_TASK_ID,
					Collections.emptyList());
			insertApplyRecords(Collections.singletonList(record));
			markApplyFinished(record.getApplyId());

			SsmAuthApplyResultVo result = new SsmAuthApplyResultVo();
			result.setDirectApply(true);
			return ResponseMessage.success(result);
		}

		Map<String, WorkOrderGroup> workOrderGroupMap = new LinkedHashMap<>();
		boolean storeCtgInRecord = true;

		if (ctgList.isEmpty() && hasRowAuthNeedApproval(dataAuthList)) {
			// 2. 仅行级权限申请（含新增）：取当前数据集下第一个有权限的模块作为审批参考
			String ctgId = (String) queryFieldService.getFirstAuthCtgId(ssmAuthApplyVo.getDatasetId()).getData();
			if (BIUtil.isEmpty(ctgId)) {
				return ResponseMessage.fail(NO_MODULE_AUTH_MSG);
			}
			SsdDataAuthVo referenceCtg = new SsdDataAuthVo();
			referenceCtg.setItemCode(ctgId);
			SsmWorkOrderApprovalRoute route = SsmTaskUtils.buildApprovalRoute(menuService, hrEmployee, referenceCtg);
			WorkOrderGroup group = new WorkOrderGroup(route);
			group.getReferenceCtgList().add(referenceCtg);
			workOrderGroupMap.put(route.buildMergeKey(), group);
			storeCtgInRecord = false;
		} else {
			// 3. 含模块权限申请：按所选模块审批链路建单（可合并）
			for (SsdDataAuthVo ctgVo : ctgList) {
				SsmWorkOrderApprovalRoute route = SsmTaskUtils.buildApprovalRoute(menuService, hrEmployee, ctgVo);
				workOrderGroupMap.computeIfAbsent(route.buildMergeKey(), key -> new WorkOrderGroup(route))
						.getCtgList().add(ctgVo);
			}
		}

		return createWorkOrders(user, hrEmployee, ssmAuthApplyVo, workOrderGroupMap, storeCtgInRecord);
	}

	private ResponseMessage createWorkOrders(User user, HrEmployee hrEmployee, SsmAuthApplyVo ssmAuthApplyVo,
	                                         Map<String, WorkOrderGroup> workOrderGroupMap,
	                                         boolean storeCtgInRecord) {
		if (workOrderGroupMap.isEmpty()) {
			return ResponseMessage.fail("创建权限申请失败");
		}

		List<SysAuthApplyRecord> recordList = new ArrayList<>();
		for (WorkOrderGroup group : workOrderGroupMap.values()) {
			TaskParam taskParam = SsmTaskUtils.createSsmTaskParam(user, hrEmployee, group, ssmAuthApplyVo);
			taskParam.setTaskConfigId(rptTaskConfigId);
			taskParam.setSystemName(rptSystemName);
			taskParam.setUserEmail(user.getEmail());
			Integer taskId = SsmTaskUtils.createTask(createTaskPath, taskParam);
			if (taskId == null) {
				throw new RuntimeException("创建工单出错");
			}
			List<SsdDataAuthVo> ctgListForRecord = storeCtgInRecord ? group.getCtgList() : Collections.emptyList();
			recordList.add(buildApplyRecord(user, ssmAuthApplyVo, String.valueOf(taskId), ctgListForRecord));
		}

		insertApplyRecords(recordList);

		SsmAuthApplyResultVo result = new SsmAuthApplyResultVo();
		result.setDirectApply(false);
		result.setTaskIds(recordList.stream().map(SysAuthApplyRecord::getTaskId).collect(Collectors.toList()));
		return ResponseMessage.success(result);
	}

	private HrEmployee queryHrEmployee(User user) {
		Map<String, String> paramMap = new HashMap<>();
		paramMap.put("email", user.getEmail());
		return (HrEmployee) dao.queryObject("user.queryEmployeeByEmail", paramMap);
	}

	private SysAuthApplyRecord buildApplyRecord(User user, SsmAuthApplyVo ssmAuthApplyVo, String taskId,
	                                            List<SsdDataAuthVo> ctgListForRecord) {
		AuthApplyType applyType = AuthApplyType.getType("ssm");
		SsmAuthApplyVo authApplyVo = new SsmAuthApplyVo();
		authApplyVo.setCtgList(ctgListForRecord);
		authApplyVo.setDataAuthList(ssmAuthApplyVo.getDataAuthList());
		authApplyVo.setOwnAuthList(ssmAuthApplyVo.getOwnAuthList());
		authApplyVo.setApplyReason(ssmAuthApplyVo.getApplyReason());

		SysAuthApplyRecord record = new SysAuthApplyRecord();
		record.setApplyId(UUID.randomUUID().toString());
		record.setTaskId(taskId);
		record.setAuthType(applyType.getType());
		record.setAuthInfo(JSON.toJSONString(authApplyVo));
		record.setCreateTime(new Date());
		record.setCreatedBy(user.getName());
		return record;
	}

	private void insertApplyRecords(List<SysAuthApplyRecord> recordList) {
		if (BIUtil.isEmpty(recordList)) {
			return;
		}
		int i = (int) dao.insert("authority.batchInsertAuthApplyRecord", recordList);
		if (i != recordList.size()) {
			throw new RuntimeException("创建权限申请失败");
		}
	}

	private void markApplyFinished(String applyId) {
		Map<String, String> map = new HashMap<>();
		map.put("applyId", applyId);
		dao.update("authority.updateApplyFinishTime", map);
	}

	private List<SsdDataAuthVo> normalizeList(List<SsdDataAuthVo> list) {
		return list == null ? Collections.emptyList() : list;
	}

	private boolean isOnlyRowAuthDelete(List<SsdDataAuthVo> dataAuthList) {
		if (dataAuthList.isEmpty()) {
			return false;
		}
		boolean hasDelete = false;
		for (SsdDataAuthVo rowAuth : dataAuthList) {
			if (BIUtil.isEmpty(rowAuth.getItemCode())) {
				continue;
			}
			if (SsdDataAuthApplyType.isDelete(rowAuth.getApplyType())) {
				hasDelete = true;
			} else if (SsdDataAuthApplyType.isAdd(rowAuth.getApplyType())) {
				return false;
			}
		}
		return hasDelete;
	}

	private boolean hasRowAuthNeedApproval(List<SsdDataAuthVo> dataAuthList) {
		for (SsdDataAuthVo rowAuth : dataAuthList) {
			if (BIUtil.isEmpty(rowAuth.getItemCode())) {
				continue;
			}
			if (SsdDataAuthApplyType.isAdd(rowAuth.getApplyType())) {
				return true;
			}
		}
		return false;
	}

	static class WorkOrderGroup {
		private final SsmWorkOrderApprovalRoute route;
		private final List<SsdDataAuthVo> ctgList = new ArrayList<>();
		private final List<SsdDataAuthVo> referenceCtgList = new ArrayList<>();

		WorkOrderGroup(SsmWorkOrderApprovalRoute route) {
			this.route = route;
		}

		SsmWorkOrderApprovalRoute getRoute() {
			return route;
		}

		List<SsdDataAuthVo> getCtgList() {
			return ctgList;
		}

		List<SsdDataAuthVo> getReferenceCtgList() {
			return referenceCtgList;
		}

		List<SsdDataAuthVo> getDisplayCtgList() {
			return BIUtil.isNotEmpty(ctgList) ? ctgList : referenceCtgList;
		}
	}
}
