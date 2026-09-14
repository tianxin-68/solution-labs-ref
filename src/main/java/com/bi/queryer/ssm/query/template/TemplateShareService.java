package com.bi.queryer.ssm.query.template;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.StrUtil;
import com.bi.queryer.ssm.meta.SSDQueryTemplate;
import com.bi.queryer.ssm.query.ctg.enums.QueryTemplateCategoryType;
import com.bi.queryer.ssm.query.ctg.model.UserTemplateCategory;
import com.bi.queryer.ssm.query.template.enums.QueryTemplateShareType;
import com.bi.queryer.ssm.query.template.enums.QueryTemplateType;
import com.bi.queryer.ssm.query.template.enums.TemplateViewType;
import com.bi.queryer.ssm.query.template.enums.ViewStatusType;
import com.bi.queryer.ssm.query.template.model.*;
import com.bi.queryer.ssm.query.template.view.TemplateViewService;
import com.bi.queryer.ssm.query.template.view.model.TemplateViewEntity;
import com.bi.queryer.sys.base.AbstractTransaction;
import com.bi.queryer.sys.base.BaseDao;
import com.bi.queryer.sys.config.SC;
import com.bi.queryer.sys.db.DataSourceType;
import com.bi.queryer.sys.enums.Enabled;
import com.bi.queryer.sys.exception.BIException;
import com.bi.queryer.sys.user.UserManager;
import com.bi.queryer.sys.user.vo.User;
import com.bi.queryer.util.BIUtil;
import com.bi.queryer.util.Guid;
import com.bi.queryer.util.SpringContextUtil;
import com.bi.queryer.util.StringUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Scope("prototype")
public class TemplateShareService extends TemplateBaseService {
	@Autowired
	protected BaseDao dao = null;

	/**
	 * 分享模版给用户
	 * @param req
	 */
	public void shareTemplatesToUsers(TemplateShareReq req) {
		dao.executeTranscation(DataSourceType.Default, new AbstractTransaction() {
			@Override
			public void execute() {
				List<TemplateRsp> tplList = getTemplateRspList(req);
				List<TemplateShareEntity> shareEntityList = ListUtil.list(false);
				List<String> userNames = normalizeUserNames(req.getUserNames());
				List<User> userList = getUserListByNames(userNames);
				User user = UserManager.get();
				for (TemplateRsp templateRsp : tplList) {
					for (User sharedUser : userList) {
						// 生成分享记录
						TemplateShareEntity shareEntity = buildTmpUserShareEntity(user, templateRsp, sharedUser);
						shareEntityList.add(shareEntity);
					}
				}
				if (BIUtil.isNotEmpty(shareEntityList)) {
					dao.insert("ssm.template.batchAddTplShare", shareEntityList);
				}
				shareTemplatesToUsers(tplList, userList, null);
			}
		});
	}

	/**
	 * 分享模版给部门
	 * @param req
	 */
	public void shareTemplatesToDept(TemplateShareReq req) {
		dao.executeTranscation(DataSourceType.Default, new AbstractTransaction() {
			@Override
			public void execute() {
				List<TemplateRsp> tplList = getTemplateRspList(req);
				List<TemplateShareEntity> shareEntityList = ListUtil.list(false);
				List<User> userList = getUserListByDept(req.getDeptId());
				User user = UserManager.get();
				// 移除自己（不分享给自己）
				/*userList = userList.stream().filter(item -> !user.getName().equals(item.getName())).collect(Collectors.toList());*/
				for (TemplateRsp templateRsp : tplList) {
					// 生成分享记录
					TemplateShareEntity shareEntity = buildTmpDeptShareEntity(user, templateRsp, req.getDeptId());
					shareEntityList.add(shareEntity);
				}
				if (BIUtil.isNotEmpty(shareEntityList)) {
					dao.insert("ssm.template.batchAddTplShare", shareEntityList);
				}
				shareTemplatesToUsers(tplList, userList, null);
			}
		});
	}

	/**
	 * 分享模板给用户
	 * @param tplList
	 * @param userList
	 * @param userCopyCategories
	 */
	public void shareTemplatesToUsers(List<TemplateRsp> tplList, List<User> userList, List<UserTemplateCategory> userCopyCategories) {
		User user = UserManager.get();
		if (CollectionUtil.isEmpty(userList) || CollectionUtil.isEmpty(tplList)) {
			return;
		}

		List<TemplateEntity> templateEntityList = ListUtil.list(false);
		List<TemplateShareRelEntity> shareRelEntityList = ListUtil.list(false);

		Map<String,Object> paramMap = new HashMap<>();
		List<String> tplIdList = tplList.stream().map(TemplateRsp::getTplId).collect(Collectors.toList());
		paramMap.put("tplIdList",tplIdList );
		paramMap.put("tplIds",tplIdList);

		//查询模版视图的信息
		List<TemplateViewEntity> viewList = (List<TemplateViewEntity>) dao.queryObjectList("ssm.template.view.batchGetByTplId",paramMap);
		Map<String,List<TemplateViewEntity>> viewMap = viewList.stream().collect(Collectors.groupingBy(TemplateViewEntity::getTplId));

		// 模版的跳转配置数据同步分享
		List<TemplateLinkRsp> linkToOtherTemplateList = (List<TemplateLinkRsp>) dao.queryObjectList("ssm.template.link.queryLinkToOtherTemplateList",paramMap );

		TemplateLinkService templateLinkService = (TemplateLinkService)SpringContextUtil.getBean("templateLinkService");
		templateLinkService.buildTemplateLinkField(linkToOtherTemplateList);

		Map<String, List<TemplateLinkRsp>> linkMap = linkToOtherTemplateList.stream().collect(Collectors.groupingBy(TemplateLinkRsp::getQueryTplId));
		List<TemplateLinkEntity> templateLinkEntityList = new ArrayList<>();

		//模版视图配置，采用insert into select 的方式批量插入，需要构造cfgId的映射关系
		List<String> cfgIdList = new ArrayList<>();
		List<String> cfgIdCaseWhenSqlList = new ArrayList<>();
		Map<String,String> cfgIdMap = new HashMap<>();

		//视图集合
		List<TemplateViewEntity> newTemplateViewList = new ArrayList<>();
		//默认的视图配置
		List<TemplateViewEntity> defaultTemplateViewList = new ArrayList<>();

		for (TemplateRsp templateRsp : tplList) {

			// 生成模版配置数据
			List<TemplateViewEntity> templateViewList = viewMap.get(templateRsp.getTplId());
			if(CollUtil.isEmpty(templateViewList)){
				continue;
			}

			for(TemplateViewEntity templateView : templateViewList){
				cfgIdList.add(templateView.getCfgId());
				String newCfgId = Guid.id();
				cfgIdCaseWhenSqlList.add(String.format(" when cfg_id = '%s' then '%s' ", templateView.getCfgId(), newCfgId));
				cfgIdMap.put(templateView.getCfgId(), newCfgId);
			}

			for (User sharedUser : userList) {
				// 生成模版数据
				String tplId = Guid.id();
				TemplateEntity templateEntity = new TemplateEntity();
				BeanUtils.copyProperties(templateRsp, templateEntity);
				templateEntity.setTplId(tplId);
				templateEntity.setTplType(QueryTemplateType.NORMAL.getId());
				templateEntity.setTplOwner(sharedUser.getName());
				templateEntity.setCreatedBy(sharedUser.getName());
				templateEntity.setUpdatedBy(user.getName());
				templateEntity.setShareBy(user.getName());
				String ctgId = QueryTemplateCategoryType.SHARE.getId();
				if (BIUtil.isNotEmpty(userCopyCategories)) {
					Optional<UserTemplateCategory> userTemplateCategory = userCopyCategories.stream()
							.filter(u -> sharedUser.getName().equals(u.getUserName()))
							.filter(u -> u.getSourceId().equals(templateRsp.getCtgId())).findFirst();
					if (userTemplateCategory.isPresent()) {
						ctgId = userTemplateCategory.get().getTargetId();
					}
				}
				templateEntity.setCtgId(ctgId);
				templateEntity.setDatasetId(templateRsp.getDatasetId());
				templateEntityList.add(templateEntity);
				// 生成分享模版与原始模版关联数据
				TemplateShareRelEntity shareRelEntity = new TemplateShareRelEntity();
				shareRelEntity.setSourceTplId(templateRsp.getTplId());
				shareRelEntity.setTargetTplId(tplId);
				shareRelEntity.setCreatedBy(user.getName());
				shareRelEntity.setRootTplId(templateRsp.getTplId());
				shareRelEntityList.add(shareRelEntity);

				Map<String, String> viewIdMap = new HashMap<>();

				//生成视图信息
				boolean buildDefaultTemplateView = false;
				for(TemplateViewEntity templateView : templateViewList) {
					TemplateViewEntity viewEntity = new TemplateViewEntity();

					String viewId = Guid.id();
					viewEntity.setViewId(viewId);
					viewEntity.setViewName(templateView.getViewName());
					viewEntity.setTplId(tplId);
					viewEntity.setCfgId(cfgIdMap.get(templateView.getCfgId()));
					viewEntity.setSortId(templateView.getSortId());
					viewEntity.setCreatedBy(sharedUser.getName());
					viewEntity.setIsActive(templateView.getIsActive());
					viewEntity.setAssetExpiresTime(templateView.getAssetExpiresTime());
					viewEntity.setDatasetId(StrUtil.isNotEmpty(templateView.getDatasetId()) ? templateView.getDatasetId() : templateRsp.getDatasetId());

					viewIdMap.put(templateView.getViewId(), viewId);

					newTemplateViewList.add(viewEntity);

					//取第一个有效的视图为默认视图
					if (!buildDefaultTemplateView && Enabled.value(viewEntity.getIsActive())) {
						TemplateViewEntity defaultView = new TemplateViewEntity();
						defaultView.setViewId(viewId);
						defaultView.setTplId(tplId);
						defaultView.setCreatedBy(sharedUser.getName());
						defaultView.setIsDefault(Enabled.YES.getId());
						defaultTemplateViewList.add(defaultView);

						buildDefaultTemplateView = true;
					}

				}

				// 同步分享跳转配置
				List<TemplateLinkRsp> linkRspList = linkMap.get(templateRsp.getTplId());
				if(CollUtil.isEmpty(linkRspList)){
					continue;
				}
				List<TemplateLinkRsp> linkList = new ArrayList<>();
				for (TemplateLinkRsp linkItem : linkRspList ) {

					String viewId = viewIdMap.get(linkItem.getQueryViewId());
					if (StrUtil.isEmpty(viewId)) {
						continue;
					}

					linkItem.setQueryTplId(tplId);
					linkItem.setQueryViewId(viewId);
					linkList.add(linkItem);
				}

				List<TemplateLinkEntity> tplLinkList = templateLinkService.buildLinkEntityList(linkList);
				templateLinkEntityList.addAll(tplLinkList);
			}
		}

		dao.executeTranscation(DataSourceType.Default, new AbstractTransaction() {
			@Override
			public void execute() {

				dao.insert("ssm.template.batchAddTemplate", templateEntityList);
				dao.insert("ssm.template.batchAddTemplateRel", shareRelEntityList);

				Map<String, Object> map = new HashMap<>();
				paramMap.put("viewList", newTemplateViewList);
				dao.insert("ssm.template.view.bacthAdd", paramMap);

				//保存视图配置信息
				if (CollUtil.isNotEmpty(cfgIdList)) {
					String cfgCaseWhenSql = String.format(" case %s else cfg_id end  ", BIUtil.listToStr(cfgIdCaseWhenSqlList," "));
					paramMap.put("cfgIdCaseWhenSql", cfgCaseWhenSql);
					paramMap.put("cfgIdList", cfgIdList);

					dao.insert("ssm.template.insertIntoTemplateCfg", paramMap);
					dao.insert("ssm.query.template.cfg.dtl.insertIntoTemplateCfgDtl", paramMap);
				}


				//保存模版视图跳转信息
				if (CollectionUtil.isNotEmpty(templateLinkEntityList)) {
					map.put("templateLinkEntityList", templateLinkEntityList);
					dao.insert("ssm.template.link.batchInsertTemplateLink", map);
					templateLinkService.saveTemplateLinkFiled(templateLinkEntityList);
				}

				/**
				 * 保存默认视图
				 */
				if(CollUtil.isNotEmpty(defaultTemplateViewList)){
					map.put("defaultTemplateViewList", defaultTemplateViewList);
					dao.insert("ssm.template.view.user.cfg.batchAdd", map);
				}
			}
		});

	}

	/**
	 * 生成快照分享模版
	 * @param req
	 * @return
	 */
	public TemplateViewRsp shareTplSnapshot(TemplateAddReq req) {

		TemplateViewRsp templateViewRsp = new TemplateViewRsp();

		//如果没有设置数据集，取默认数据集
		if (StringUtil.isEmpty(req.getDatasetId())) {
			req.setDatasetId(SC.v("ssm.default.datasetId", ""));
		}

		String sourceTplId = req.getTplId();
		String sourceViewId = req.getViewId();
		req.setCtgId(QueryTemplateCategoryType.SNAPSHOT.getId());

		dao.executeTranscation(DataSourceType.Default, new AbstractTransaction() {
			@Override
			public void execute() {
				User user = UserManager.get();
				Map<String, Object> queryParams = new HashMap<>();
				queryParams.put("tplId", req.getTplId());
				queryParams.put("viewId", req.getViewId());
				queryParams.put("tplIds", Arrays.asList(req.getTplId()));

				QueryTemplateType queryTemplateType = QueryTemplateType.get(req.getTplType());
				//快照视图-只保存当前配置生成新的模版视图
				//需要将视图类型设置为公共视图，状态设置为启用
				if(queryTemplateType == QueryTemplateType.SNAPSHOT){
					req.setIsViewValid(Enabled.YES.getId());
					req.setViewStatus(ViewStatusType.ACTIVE.getCode());
					req.setViewType(TemplateViewType.PUBLIC.getCode());
				}

				//查询跳转信息
				List<TemplateLinkRsp> linkList = (List<TemplateLinkRsp>) dao.queryObjectList("ssm.template.link.queryLinkToOtherTemplateList", queryParams);
				TemplateLinkService templateLinkService = (TemplateLinkService)SpringContextUtil.getBean("templateLinkService");
				templateLinkService.buildTemplateLinkField(linkList);
				//构造跳转信息
				List<TemplateLinkAddReq> linkAddReqList = templateLinkService.buildTemplateLinkAddReq(linkList);
				req.setTemplateLinkList(linkAddReqList);

				TemplateConfigService templateConfigService = (TemplateConfigService) SpringContextUtil.getBean("templateConfigService");
				TemplateViewRsp tplViewRsp = templateConfigService.saveAsTemplateAndView(req);

				templateViewRsp.setTplId(tplViewRsp.getTplId());
				templateViewRsp.setViewId(tplViewRsp.getViewId());
				templateViewRsp.setViewIdMappingList(tplViewRsp.getViewIdMappingList());

				if (StrUtil.isNotEmpty(sourceTplId)) {
					// 插入分享关系（快照场景：一个模板对应一个视图）
					String rootTplId = (String) dao.queryObject("ssm.template.queryRootTplIdByTargetTplId", sourceTplId);
					if (StrUtil.isEmpty(rootTplId)) {
						rootTplId = sourceTplId;
					}
					TemplateShareRelEntity shareRelEntity = new TemplateShareRelEntity();
					shareRelEntity.setSourceTplId(sourceTplId);
					shareRelEntity.setTargetTplId(tplViewRsp.getTplId());
					shareRelEntity.setRootTplId(rootTplId);
					shareRelEntity.setSourceViewId(sourceViewId);
					shareRelEntity.setTargetViewId(tplViewRsp.getViewId());
					String rootViewId = (String) dao.queryObject("ssm.template.queryRootViewIdByTargetViewId", sourceViewId);
					if (StrUtil.isEmpty(rootViewId)) {
						rootViewId = sourceViewId;
					}
					shareRelEntity.setRootViewId(rootViewId);
					shareRelEntity.setCreatedBy(user.getName());
					dao.insert("ssm.template.addTemplateRel", shareRelEntity);
				}

			}
		});
		return templateViewRsp;
	}


	/**
	 * 生成部门模版分享记录
	 * @param user
	 * @param templateRsp
	 * @param deptId
	 * @return
	 */
	public TemplateShareEntity buildTmpDeptShareEntity(User user, TemplateRsp templateRsp, String deptId) {
		TemplateShareEntity shareEntity = new TemplateShareEntity();
		shareEntity.setPkid(Guid.id());
		shareEntity.setTplId(templateRsp.getTplId());
		shareEntity.setShareUserName(user.getName());
		shareEntity.setShareTime(new Date());
		shareEntity.setReceiverType(QueryTemplateShareType.DEPT.getId());
		shareEntity.setReceiverId(deptId);
		Map<String, DeptEntity> deptMap = getDeptMap(deptId);
		if (deptMap.get(deptId) != null) {
			shareEntity.setReceiverDesc(deptMap.get(deptId).getDeptPathName());
		}
		return shareEntity;
	}

	/**
	 * 生成部门模版分享记录
	 * @param user
	 * @param templateRsp
	 * @param sharedUser
	 * @return
	 */
	public TemplateShareEntity buildTmpUserShareEntity(User user, TemplateRsp templateRsp, User sharedUser) {
		TemplateShareEntity shareEntity = new TemplateShareEntity();
		shareEntity.setPkid(Guid.id());
		shareEntity.setTplId(templateRsp.getTplId());
		shareEntity.setShareUserName(user.getName());
		shareEntity.setShareTime(new Date());
		shareEntity.setReceiverType(QueryTemplateShareType.USER.getId());
		shareEntity.setReceiverId(sharedUser.getName());
		shareEntity.setReceiverDesc(sharedUser.getRealName());
		return shareEntity;
	}

	//生成看板的快照
	public TemplateViewRsp createAnalysisTemplateSnapshot(String tplId, String viewId) {

		TemplateConfigService templateConfigService = (TemplateConfigService) SpringContextUtil.getBean("templateConfigService");
		SSDQueryTemplate templateRsp = templateConfigService.getTemplateById(tplId, viewId);

		if (templateRsp == null) {
			throw new BIException(String.format("模板%s不存在", tplId));
		}

		TemplateAddReq templateAddReq = new TemplateAddReq();
		templateAddReq.setTplId(tplId);
		templateAddReq.setTplName(templateRsp.getName());
		templateAddReq.setTplDesc(templateRsp.getDescription());
		templateAddReq.setViewId(templateRsp.getViewId());
		templateAddReq.setViewName(templateRsp.getViewName());
		templateAddReq.setViewType(templateRsp.getViewType());
		templateAddReq.setTplConfig(templateRsp.getConfig());
		templateAddReq.setIsViewValid(templateRsp.getIsViewValid());
		templateAddReq.setViewStatus(templateRsp.getViewStatus());
		templateAddReq.setDatasetId(templateRsp.getDatasetId());

		//获取模版视图使用的维度和指标
		TemplateCfgDtlEntity viewCfg = (TemplateCfgDtlEntity) dao.queryObject("ssm.query.template.cfg.dtl.queryByViewId", templateRsp.getViewId());
		if (viewCfg != null) {
			templateAddReq.setTplConfigFieldDimCodes(viewCfg.getTplConfigFieldDimCodes());
			templateAddReq.setTplConfigFieldMeasureCodes(viewCfg.getTplConfigFieldMeasureCodes());
			templateAddReq.setTplConfigFieldDimAsset(viewCfg.getTplConfigFieldDimAsset());
			templateAddReq.setTplConfigFieldMeasureAsset(viewCfg.getTplConfigFieldMeasureAsset());
		}

		templateAddReq.setTplType(QueryTemplateType.ANALYSIS_TEMPLATE_SNAPSHOT.getId());
		TemplateViewRsp templateViewRsp = shareTplSnapshot(templateAddReq);
		return templateViewRsp;

	}

}
