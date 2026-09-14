package com.bi.queryer.ssm.api.ticket;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.bi.queryer.ssm.enums.DataSensitiveLevel;
import com.bi.queryer.ssm.meta.MetaField;
import com.bi.queryer.ssm.meta.MetaFieldCategory;
import com.bi.queryer.ssm.meta.SSDMetaCacheManager;
import com.bi.queryer.sys.authority.AuthorityService;
import com.bi.queryer.sys.base.BaseDao;
import com.bi.queryer.sys.enums.Enabled;
import com.bi.queryer.sys.exception.BIException;
import com.bi.queryer.sys.user.UserService;
import com.bi.queryer.sys.user.constant.AuthApplyType;
import com.bi.queryer.sys.user.model.SysAuthApplyRecord;
import com.bi.queryer.sys.user.vo.SsdDataAuthVo;
import com.bi.queryer.sys.user.vo.SsmAuthApplyVo;
import com.alibaba.fastjson.JSON;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class TicketDataSecurityService {


    @Autowired
    private UserService userService;

    @Autowired
    private AuthorityService authorityService;

    @Autowired
    private BaseDao dao;


    /**
     * 查询同部门人员对目标目录的权限开放情况：直接查ssm_user_ctg_auth_dtl表，
     * 这张表已经把权限继承关系落库展开了（继承来的权限auth_source='权限继承'也是独立一条记录），
     */
    public UserDeptCtgAuthRsp getUserDeptCtgAuth(UserDeptCtgAuthReq req) {
        String username = req.getUsername();
        String ctgName = req.getCtgName();
        String taskId = req.getTaskId();

        // 判空 + 去掉ctgName后面可能带的括号
        ctgName = dealWithCtgName(ctgName);

        UserDeptCtgAuthRsp res = new UserDeptCtgAuthRsp();

        // 1、根据username获取部门username列表
        List<String> deptUserNames = userService.queryDeptColleagueUserNames(username);

        // 限制人数 最大为10 暂不限制
        // List<String> queryUserNames = deptUserNames.size() > 10 ? deptUserNames.subList(0, 10) : deptUserNames;

        // 2、工单查ctgId，工单没有/匹配不到则降级用目录名称匹配（走内存缓存）
        MetaFieldCategory targetCategory = getTargetCategory(ctgName, taskId);
        String ctgId = targetCategory == null ? null : targetCategory.getId();
        if (Objects.isNull(ctgId)) {
            throw new BIException("目录[" + ctgName + "] 不存在");
        }

        // 3、查ssm_user_ctg_auth_dtl：同一人同一ctgId可能有多条(不同auth_source)记录，取auth_end_date最晚的一条
        Map<String, UserDeptCtgAuthDetail> userAuthDetailMap = queryDeptUsersAuthDetail(deptUserNames, ctgId);

        // 4、组装每个人的授权明细：查不到记录的人也带出来，中间三个字段(authStartDate/authEndDate/authSource)留空
        List<UserDeptCtgAuthDetail> authDetailList = new ArrayList<>();
        for (String deptUserName : deptUserNames) {
            UserDeptCtgAuthDetail detail = userAuthDetailMap.get(deptUserName);
            if (detail == null) {
                detail = new UserDeptCtgAuthDetail();
                detail.setUserName(deptUserName);
                detail.setHasAuth(Enabled.NO.getId());
            }
            authDetailList.add(detail);
        }

        // 5、组装res：userAuthDetailMap里只有查到授权记录的人才会被放进去，size()即为授权人数
        res.setDeptUserCount(deptUserNames.size());
        res.setAuthUserCount(userAuthDetailMap.size());
        res.setAuthDetailList(authDetailList);
        return res;
    }

    /**
     * 查ssm_user_ctg_auth_dtl：按ctgId+部门同事名单，查每个人对该目录的授权明细；
     * 同一人同一ctgId可能有多条(不同auth_source)记录，取auth_end_date最晚的一条
     * （authSource/authStartDate跟这条最晚记录保持对应，不是跨行拼凑的）
     *
     * @return userName -> 授权明细(已设置hasAuth=是)，查不到记录（没有权限）的人不会出现在这个Map里
     */
    private Map<String, UserDeptCtgAuthDetail> queryDeptUsersAuthDetail(List<String> deptUserNames, String ctgId) {
        if (CollUtil.isEmpty(deptUserNames)) {
            return new HashMap<>();
        }
        Map<String, Object> param = new HashMap<>();
        param.put("ctgId", ctgId);
        param.put("userNames", deptUserNames);
        // 批量查询UserDeptCtgAuthDetail 全量表：bi_ssm.ssm_user_ctg_auth_dtl
        List<UserDeptCtgAuthDetail> rows = (List<UserDeptCtgAuthDetail>) dao.queryObjectList(
                "ssm.user.ctg.auth.dtl.queryAuthDetailByCtgIdAndUserNames", param);
        Map<String, UserDeptCtgAuthDetail> result = new HashMap<>();
        if (CollUtil.isNotEmpty(rows)) {
            for (UserDeptCtgAuthDetail row : rows) {
                UserDeptCtgAuthDetail existing = result.get(row.getUserName());
                // 只要结束时间最晚的记录
                if (existing != null && existing.getAuthEndDate().compareTo(row.getAuthEndDate()) >= 0) {
                    continue;
                }
                row.setHasAuth(Enabled.YES.getId());
                result.put(row.getUserName(), row);
            }
        }
        return result;
    }

    /**
     * 解析目标目录（走内存缓存）：优先用工单信息解析出ctgId后按id查缓存；工单解析不出时降级按名称查缓存
     * （{@link SSDMetaCacheManager#getCategoryByName} 命中多个同名目录取第一个，不报歧义）
     *
     * @param ctgName 目录名称
     * @param taskId  工单号，可为空
     * @return 解析到的目录节点；工单和名称都匹配不到时返回null
     */
    private MetaFieldCategory getTargetCategory(String ctgName, String taskId) {
        String ctgId = getCtgIdFromTicket(ctgName, taskId);
        if (StringUtils.isNotBlank(ctgId)) {
            return SSDMetaCacheManager.getCategoryById(ctgId);
        }
        return null;
    }

    /**
     * 从工单信息里解析ctgId：工单必须查得到、authType必须是ssm、ctgList里必须有itemValue==ctgName的项且itemCode非空，
     * 否则返回null（任一环节不满足都视为失败，交给调用方走降级）
     */
    private String  getCtgIdFromTicket(String ctgName, String taskId) {
        if (StringUtils.isBlank(taskId)) {
            return null;
        }
        // authType = ssd 放行
        SysAuthApplyRecord applyRecord = authorityService.queryAuthApplyRecord(taskId);
        if (applyRecord == null || AuthApplyType.SSM != AuthApplyType.getType(applyRecord.getAuthType())) {
            return null;
        }
        // ctgList 在info里
        if (StringUtils.isBlank(applyRecord.getAuthInfo())) {
            return null;
        }
        SsmAuthApplyVo ssmAuthApplyVo = JSON.parseObject(applyRecord.getAuthInfo(), SsmAuthApplyVo.class);
        if (ssmAuthApplyVo == null || CollUtil.isEmpty(ssmAuthApplyVo.getCtgList())) {
            return null;
        }
        // 按照名称匹配
        for (SsdDataAuthVo ctg : ssmAuthApplyVo.getCtgList()) {
            if (StringUtils.equals(ctgName, ctg.getItemValue()) && StringUtils.isNotBlank(ctg.getItemCode())) {
                return ctg.getItemCode();
            }
        }
        return null;
    }

    /**
     * 查询目录下高敏指标/维度TopN（C3/C4级别，指标TOP20、维度TOP5）
     *
     * @param ctgName 目录名称
     * @param taskId  工单号，可为空
     * @return 高敏指标/维度TopN
     */
    public CtgSensitiveFieldRsp getTopNCtgSensitiveField(String ctgName, String taskId) {

        // 判空 + 去掉ctgName后面可能带的括号
        dealWithCtgName(ctgName);

        // 这里取完整对象而不是id 后面方便递归
        MetaFieldCategory rootCategory = getTargetCategory(ctgName, taskId);

        if (Objects.isNull(rootCategory)) {
            throw new BIException("目录[" + ctgName + "] 不存在");
        }

        CtgSensitiveFieldRsp rsp = new CtgSensitiveFieldRsp();
        List<MetaField> allFields = new ArrayList<>();

        // 递归拿到目录下所有字段
        collectAllFieldsFromRootCtg(rootCategory, allFields);

        // 可展示、可用于筛选或查询结果的字段 isShow = 1
        List<MetaField> visibleFields = allFields.stream()
                .filter(f -> Enabled.value(f.getIsShow()))
                .collect(Collectors.toList());

        // 暂时硬编码
        rsp.setMetrics(getTopNFields(visibleFields, true, 20));
        rsp.setDimensions(getTopNFields(visibleFields, false, 5));
        return rsp;
    }


    /**
     * 递归收集目录及其所有子孙目录下的字段
     */
    private void collectAllFieldsFromRootCtg(MetaFieldCategory category, List<MetaField> result) {
        if (category == null) {
            return;
        }
        if (CollUtil.isNotEmpty(category.getFields())) {
            result.addAll(category.getFields());
        }
        if (CollUtil.isNotEmpty(category.getChildren())) {
            for (MetaFieldCategory child : category.getChildren()) {
                collectAllFieldsFromRootCtg(child, result);
            }
        }
    }

    /**
     * 从字段列表里挑出高敏(C3/C4)的指标或维度，按敏感等级降序取前N个
     *
     * @param fields    候选字段
     * @param isMeasure true=只看指标，false=只看维度
     * @param topN      取前N个
     */
    private List<SensitiveFieldItem> getTopNFields(List<MetaField> fields, boolean isMeasure, int topN) {
        return fields.stream()
                .filter(f -> Enabled.value(f.getIsMeasure()) == isMeasure)
                .filter(f -> DataSensitiveLevel.get(f.getSensitiveLevel()).isHigh())
                .sorted(Comparator.comparingInt(
                        (MetaField f) -> DataSensitiveLevel.get(f.getSensitiveLevel()).getLevel()).reversed())
                .limit(topN)
                .map(f -> {
                    SensitiveFieldItem item = new SensitiveFieldItem();
                    item.setFieldId(f.getId());
                    item.setFieldCode(f.getCode());
                    item.setFieldTitle(f.getTitle());
                    item.setSensitiveLevel(f.getSensitiveLevel());
                    return item;
                })
                .collect(Collectors.toList());
    }

    // 去掉传进来的目录可能后面跟的括号
    private String dealWithCtgName(String ctgName) {
        if (StrUtil.isEmpty(ctgName)) {
            throw new BIException("目录名称：ctgName为必填项！");
        }
        int idx = ctgName.indexOf('(');
        if (idx == -1) {
            idx = ctgName.indexOf('（');
        }
        return idx == -1 ? ctgName : ctgName.substring(0, idx).trim();
    }

}