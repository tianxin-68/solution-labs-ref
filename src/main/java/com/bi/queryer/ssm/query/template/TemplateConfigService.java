package com.bi.queryer.ssm.query.template;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import com.bi.queryer.ssm.engine.QueryContext;
import com.bi.queryer.ssm.engine.QueryEngine;
import com.bi.queryer.ssm.engine.QueryFactory;
import com.bi.queryer.ssm.engine.accelerate.cache.RedisCacheManager;
import com.bi.queryer.ssm.engine.config.QueryConfigure;
import com.bi.queryer.ssm.engine.model.StarModel;
import com.bi.queryer.ssm.exception.SSDException;
import com.bi.queryer.ssm.meta.SSDQueryTemplate;
import com.bi.queryer.ssm.meta.SysEtlJobInfo;
import com.bi.queryer.ssm.mgr.dataset.DatasetService;
import com.bi.queryer.ssm.mgr.dataset.model.DatasetRsq;
import com.bi.queryer.ssm.portal.template.entity.TmpAnalysisTplCtgRelEntity;
import com.bi.queryer.ssm.portal.vo.rsp.PortalPublicDomainResp;
import com.bi.queryer.ssm.query.ctg.QueryTemplateCategoryService;
import com.bi.queryer.ssm.query.ctg.TemplateSpaceService;
import com.bi.queryer.ssm.query.ctg.enums.QueryTemplateCategoryType;
import com.bi.queryer.ssm.query.ctg.enums.TemplateSpaceRoleType;
import com.bi.queryer.ssm.query.ctg.model.QueryTemplateCategory;
import com.bi.queryer.ssm.query.ctg.model.TemplateSpaceDetailRsp;
import com.bi.queryer.ssm.query.template.change.log.TemplateChangeLogService;
import com.bi.queryer.ssm.query.template.enums.DataTypeEnum;
import com.bi.queryer.ssm.query.template.enums.FavTemplateType;
import com.bi.queryer.ssm.query.template.enums.QueryTemplateType;
import com.bi.queryer.ssm.query.template.enums.TemplateViewType;
import com.bi.queryer.ssm.query.template.model.*;
import com.bi.queryer.ssm.query.template.view.TemplateViewService;
import com.bi.queryer.ssm.query.template.view.model.TemplateViewEntity;
import com.bi.queryer.ssm.query.template.view.model.TemplateViewSaveReq;
import com.bi.queryer.ssm.util.SSDUtil;
import com.bi.queryer.sys.base.AbstractTransaction;
import com.bi.queryer.sys.common.SSMResponseMessage;
import com.bi.queryer.sys.config.SC;
import com.bi.queryer.sys.db.DataSourceType;
import com.bi.queryer.sys.enums.Enabled;
import com.bi.queryer.sys.exception.BIException;
import com.bi.queryer.sys.user.UserManager;
import com.bi.queryer.sys.user.vo.User;
import com.bi.queryer.sys.user.vo.UserRsp;
import com.bi.queryer.util.*;
import com.tx.cache.common.exception.KVException;
import com.tx.cache.redis.RedisCacheClient;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Scope("prototype")
public class TemplateConfigService extends TemplateBaseService {

	@Autowired
	private QueryTemplateCategoryService categoryService;

	@Autowired
	protected TemplateLinkService linkService = null;

	@Autowired
	private TemplateViewService templateViewService = null;

	@Autowired
	private DatasetService datasetService;

	@Autowired
	protected TemplateChangeLogService templateChangeLogService = null;

	public void batchFav(TemplateBatchOperateReq req) {
		if (CollectionUtil.isEmpty(req.getTplIdList())) {
			return;
		}
		User user = UserManager.get();
		Map<String, Object> queryParams = MapUtil.newHashMap();
		queryParams.put("userName", user.getName());
		queryParams.put("tplIdList", req.getTplIdList());
		List<TemplateFavEntity> favList = dao.queryObjectList("ssm.template.fav.selectByTplIds", queryParams, TemplateFavEntity.class);
		List<TemplateFavEntity> insertList = new ArrayList<>(favList.size());
		Map<String, TemplateFavEntity> favEntityMap = favList.stream().collect(Collectors.toMap(TemplateFavEntity::getTplId, v -> v));
		for (String tplId : req.getTplIdList()) {
			TemplateFavEntity favEntity = favEntityMap.get(tplId);
			if (favEntity != null) {
				favEntity.setIsFav(Enabled.YES.getId());
				favEntity.setCtgId(req.getCtgId());
				dao.insert("ssm.template.fav.updateByPrimaryKeySelective", favEntity);
			} else {
				TemplateFavEntity newEntity = new TemplateFavEntity();
				newEntity.setFavId(Guid.id());
				newEntity.setTplId(tplId);
				newEntity.setIsFav(Enabled.YES.getId());
				newEntity.setFavTplType(req.getTplType());
				newEntity.setCtgId(req.getCtgId());
				newEntity.setCreatedBy(user.getName());
				insertList.add(newEntity);
			}
		}
		if (CollectionUtil.isNotEmpty(insertList)) {
			dao.insert("ssm.template.fav.batchInsert", insertList);
		}
	}

	//修改我的收藏里的目录分类
	public void updateFav(TemplateBatchOperateReq req) {
		if (CollectionUtil.isEmpty(req.getTplIdList())) {
			return;
		}
		User user = UserManager.get();
		Map<String, Object> queryParams = MapUtil.newHashMap();
		queryParams.put("userName", user.getName());
		queryParams.put("tplIdList", req.getTplIdList());
		List<TemplateFavEntity> favList = dao.queryObjectList("ssm.template.fav.selectByTplIds", queryParams, TemplateFavEntity.class);
		Map<String, TemplateFavEntity> existFavMap;
		if (CollectionUtil.isEmpty(favList)) {
			return;
		} else {
			existFavMap = favList.stream().collect(Collectors.toMap(TemplateFavEntity::getTplId, v -> v));
		}
		for (String tplId : req.getTplIdList()) {
			TemplateFavEntity favEntity = existFavMap.get(tplId);
			if (favEntity == null) {
				continue;
			}
			favEntity.setCtgId(req.getCtgId());
			favEntity.setUpdatedBy(user.getName());
			dao.insert("ssm.template.fav.updateByPrimaryKeySelective", favEntity);
		}
	}

	public void cancelFavorite(TemplateBatchOperateReq req) {
		if (CollectionUtil.isEmpty(req.getTplIdList())) {
			return;
		}
		User user = UserManager.get();
		Map<String, Object> queryParams = MapUtil.newHashMap();
		queryParams.put("userName", user.getName());
		queryParams.put("tplIdList", req.getTplIdList());
		// 取消收藏 只是对于公域模板
		dao.delete("ssm.template.fav.deleteByTmpIdAndUserName", queryParams);
	}

	/**
	 * 置顶模版
	 *
	 * @param req
	 */
	public void top(TemplateOperateReq req) {
		User user = UserManager.get();
		Map<String, Object> queryParams = MapUtil.newHashMap();
		queryParams.put("userName", user.getName());
		List<TemplateFavEntity> favList = dao.queryObjectList("ssm.template.fav.selectByUserName", queryParams, TemplateFavEntity.class);
		List<TemplateFavEntity> topList = Collections.emptyList();
		TemplateFavEntity existEntity = null;
		if (CollectionUtil.isNotEmpty(favList)) {
			topList = favList.stream().filter(v -> Enabled.YES.getId().equals(v.getIsTop())).collect(Collectors.toList());
			existEntity = favList.stream().filter(v -> Objects.equals(v.getTplId(), req.getTplId())).findFirst().orElse(null);
		}
		//if (CollectionUtil.isNotEmpty(topList) && topList.size() >= Integer.parseInt(SC.v("template.top.max.num", "10"))) {
		//	throw new BIException("最多支持设置" + SC.v("template.top.max.num", "10") + "个置顶，请取消已设置顶后再设置！");
		//}

		Double defaultSort = 1D;
		if (existEntity == null || !Objects.equals(existEntity.getTopSortId(), defaultSort)) {
			// 所有已有的置顶后移
			List<String> favIds = topList.stream().filter(v -> !Objects.equals(v.getTplId(), req.getTplId()))
					.map(TemplateFavEntity::getFavId).collect(Collectors.toList());
			if (CollectionUtil.isNotEmpty(favIds)) {
				Map<String, Object> updateParams = new HashMap<>();
				updateParams.put("favIds", favIds);
				updateParams.put("delta", 1);
				dao.update("ssm.template.fav.incTplTopSort", updateParams);
			}
		}

		// 修改置顶
		if (existEntity != null) {
			existEntity.setTopSortId(defaultSort);
			existEntity.setIsTop(Enabled.YES.getId());
			existEntity.setUpdatedBy(user.getName());
			dao.update("ssm.template.fav.updateByPrimaryKey", existEntity);
		} else {
			TemplateFavEntity favEntity = new TemplateFavEntity();
			favEntity.setFavId(Guid.id());
			favEntity.setTplId(req.getTplId());
			favEntity.setIsTop(Enabled.YES.getId());
			favEntity.setIsFav(Enabled.NO.getId());
			favEntity.setFavTplType(req.getTplType());
			favEntity.setTopSortId(defaultSort);
			favEntity.setCreatedBy(user.getName());
			dao.insert("ssm.template.fav.insertSelective", favEntity);
		}
	}


	/**
	 * 取消模版置顶
	 *
	 * @param req
	 */
	public void cancelTop(TemplateBatchOperateReq req) {
		if (CollectionUtil.isEmpty(req.getTplIdList())) {
			return;
		}
		User user = UserManager.get();
		Map<String, Object> queryParams = MapUtil.newHashMap();
		queryParams.put("userName", user.getName());
		queryParams.put("tplIdList", req.getTplIdList());
		List<TemplateFavEntity> existEntities = dao.queryObjectList("ssm.template.fav.selectByTplIdAndUserName", queryParams, TemplateFavEntity.class);
		List<String> deleteIds = new ArrayList<>();
		for (TemplateFavEntity existEntity : existEntities) {
			if (Enabled.YES.getId().equals(existEntity.getIsFav())) {
				existEntity.setIsTop(Enabled.NO.getId());
				existEntity.setTopSortId(null);
				existEntity.setUpdatedBy(user.getName());
				dao.update("ssm.template.fav.updateByPrimaryKey", existEntity);
			} else {
				deleteIds.add(existEntity.getTplId());
			}
		}

		if (CollectionUtil.isNotEmpty(deleteIds)) {
			// 没有置顶也不收藏了, 就删除这条记录
			queryParams.put("tplIdList", deleteIds);
			dao.delete("ssm.template.fav.deleteByTmpIdAndUserName", queryParams);
		}
	}


	/**
	 * 置顶模版更换顺序
	 *
	 * @param req
	 * @return
	 */
	public void moveTopTpl(MoveTopTplReq req) {
		if (req.getTplId() == null || req.getTargetTplId() == null || req.getPos() == null ||
				Objects.equals(req.getTplId(), req.getTargetTplId())) {
			return;
		}
		List<TemplateFavRsp> topList = topList();
		int start = -1;
		int end = -1;
		int index = 0;
		String moveFavId = null;
		Double targetSortId = 0D;
		List<String> moveFavIdList = new ArrayList<>();
		for (TemplateFavRsp top : topList) {
			index++;
			if (Objects.equals(top.getTplId(), req.getTplId())) {
				start = index;
				moveFavId = top.getFavId();
			}

			if (Objects.equals(top.getTplId(), req.getTargetTplId())) {
				end = index;
				targetSortId = top.getTopSortId();
				if (start > 0 && "bottom".equals(req.getPos())) {
					moveFavIdList.add(top.getFavId());
				} else if (start < 0 && "top".equals(req.getPos())) {
					moveFavIdList.add(top.getFavId());
				}
			} else if (start * end <= 0) {
				moveFavIdList.add(top.getFavId());
			}
		}

		moveFavIdList.remove(moveFavId);
		Map<String, Object> updateParams = new HashMap<>();
		if (start < end) {
			if (CollectionUtil.isNotEmpty(moveFavIdList)) {
				updateParams.put("favIds", moveFavIdList);
				updateParams.put("delta", -1);
				dao.update("ssm.template.fav.incTplTopSort", updateParams);
			}

			updateParams.put("favId", moveFavId);
			updateParams.put("topSortId", Objects.equals("top", req.getPos()) ? targetSortId - 1 : targetSortId);
		} else {
			if (CollectionUtil.isNotEmpty(moveFavIdList)) {
				updateParams.put("favIds", moveFavIdList);
				updateParams.put("delta", 1);
				dao.update("ssm.template.fav.incTplTopSort", updateParams);
			}

			updateParams.put("favId", moveFavId);
			updateParams.put("topSortId", Objects.equals("bottom", req.getPos()) ? targetSortId + 1 : targetSortId);
		}
		dao.update("ssm.template.fav.updateTplTopSort", updateParams);
	}

	/**
	 * 查询置顶列表
	 */
	public List<TemplateFavRsp> topList() {
		User user = UserManager.get();
		Map<String, Object> queryParams = MapUtil.newHashMap();
		queryParams.put("userName", user.getName());
		List<TemplateFavRsp> res = dao.queryObjectList("ssm.template.fav.queryTopList", queryParams, TemplateFavRsp.class);
		if (CollUtil.isNotEmpty(res)) {
			return res;
		}
		return Collections.emptyList();
	}

	public List<QueryTemplateCategory> listAllViewableTree(String datasetType) {
		List<QueryTemplateCategory> categories = categoryService.getAllViewableSpaceCategories();
		List<String> ctgIds = categories.stream().map(QueryTemplateCategory::getId).collect(Collectors.toList());
		List<TemplateCtgTreeRsp> tree = getCategoryTplTree(ctgIds, false, null, datasetType);

		List<String> myRootIds = new ArrayList<>();
		myRootIds.add(QueryTemplateCategoryType.MY.getId());
		myRootIds.add(QueryTemplateCategoryType.FAV.getId());
		myRootIds.add(QueryTemplateCategoryType.SHARE.getId());
		List<TemplateCtgTreeRsp> myTree = getCategoryTplTree(myRootIds, true, null, datasetType);
		List<QueryTemplateCategory> all = new ArrayList<>(categories);

		for (TemplateCtgTreeRsp rsp : tree) {
			QueryTemplateCategory ctg = new QueryTemplateCategory();
			ctg.setId(rsp.getId());
			ctg.setName(rsp.getName());
			ctg.setParentId(rsp.getParentId());
			if (FavTemplateType.QUERY_TEMPLATE.getCode().equals(rsp.getType())) {
				// 和老接口对齐
				ctg.setType("template");
			} else {
				ctg.setType(rsp.getType());
			}
			ctg.setSortId(rsp.getSortId());
			all.add(ctg);
		}
		for (TemplateCtgTreeRsp rsp : myTree) {
			if (QueryTemplateCategoryType.SHARE.getId().equals(rsp.getId())) {
				continue;
			}
			QueryTemplateCategory ctg = new QueryTemplateCategory();
			ctg.setId(rsp.getId());
			ctg.setName(rsp.getName());
			if (myRootIds.contains(rsp.getParentId()) || StringUtils.isEmpty(rsp.getParentId())) {
				ctg.setParentId(QueryTemplateCategoryType.MY.getId());
			} else {
				ctg.setParentId(rsp.getParentId());
			}

			if (FavTemplateType.QUERY_TEMPLATE.getCode().equals(rsp.getType())) {
				ctg.setType("template");
			} else {
				ctg.setType(rsp.getType());
			}
			ctg.setSortId(rsp.getSortId());
			all.add(ctg);
		}

		// 去掉我的看板
		Map<String, List<QueryTemplateCategory>> ctgMap = all.stream()
				.filter(v -> !FavTemplateType.ANALYSIS_TEMPLATE.getCode().equals(v.getType()) && !FavTemplateType.TMP_ANALYSIS_TEMPLATE.getCode().equals(v.getType()))
				.collect(Collectors.groupingBy(QueryTemplateCategory::getParentId));

		//构造我的空间
		QueryTemplateCategory my = new QueryTemplateCategory();
		my.setId(QueryTemplateCategoryType.MY.getId());
		my.setName(QueryTemplateCategoryType.MY.getDesc());
		my.setParentId(QueryTemplateCategoryType.MY.getId());
		my.setType(QueryTemplateCategoryType.MY.getId());
		all.add(my);

		List<QueryTemplateCategory> roots = all.stream().filter(v -> CollUtil.isNotEmpty(ctgMap.get(v.getId())))
				.filter(v -> ctgIds.contains(v.getId()) || myRootIds.contains(v.getId()))
				.collect(Collectors.toList());
		for (QueryTemplateCategory root : roots) {
			attachChildrenAndPruneEmptyDirs(root, ctgMap);
		}
		roots.removeIf(r -> CollUtil.isEmpty(r.getChildren()));
		return roots;
	}

	/**
	 * 按 ctgMap 递归挂载子节点，并剔除下方既无查询模板也无非空子目录的目录节点。
	 */
	private void attachChildrenAndPruneEmptyDirs(QueryTemplateCategory node, Map<String, List<QueryTemplateCategory>> ctgMap) {
		List<QueryTemplateCategory> raw = ctgMap.get(node.getId());
		if (CollUtil.isEmpty(raw)) {
			node.setChildren(new ArrayList<>());
			return;
		}
		List<QueryTemplateCategory> next = new ArrayList<>();
		for (QueryTemplateCategory child : raw) {
			if (isQueryTemplateTreeLeaf(child)) {
				next.add(child);
			} else {
				attachChildrenAndPruneEmptyDirs(child, ctgMap);
				if (CollUtil.isNotEmpty(child.getChildren())) {
					next.add(child);
				}
			}
		}
		node.setChildren(next);
	}

	private boolean isQueryTemplateTreeLeaf(QueryTemplateCategory c) {
		if (c == null || c.getType() == null) {
			return false;
		}
		String t = c.getType();
		return FavTemplateType.QUERY_TEMPLATE.getCode().equals(t) || BIConsts.TEMPLATE_NODE_TYPE.equalsIgnoreCase(t);
	}

	/**
	 * 获取目录树（含权限）
	 *
	 * @return 指定id下的目录树
	 */
	public List<QueryTemplateCategory> templateTree(String rootId, String datasetType) {
		List<QueryTemplateCategory> categories = categoryService.getCategoryTree(BIConsts.TEMPLATE_CTG_ROOT_ID);

		categories = categories.stream().filter(c -> c.getId().equalsIgnoreCase(rootId)).collect(Collectors.toList());

		//查询目录下的查询模板
		TemplatePageListReq templatePageListReq = TemplatePageListReq.builder()
				.ctgId(rootId)
				.currPageNo(1)
				.datasetType(datasetType)
				.prePageSize(Integer.MAX_VALUE)
				.isShowAll(Enabled.YES.getId())
				.build();
		TemplatePageListRsp templatePageListRsp = templateList(templatePageListReq);
		if (CollUtil.isEmpty(templatePageListRsp.getRows())) {
			return categories;
		}

		//将看板按目录分组
		Map<String, List<QueryTemplateCategory>> ctgTemplateMap = new HashMap<>();
		for (TemplateRsp templateRsp : templatePageListRsp.getRows()) {
			String ctgId = templateRsp.getCtgId();
			List<QueryTemplateCategory> templateCtg = ctgTemplateMap.get(ctgId);
			if (templateCtg == null) {
				templateCtg = new ArrayList<>();
			}

			QueryTemplateCategory queryTemplateCategory = new QueryTemplateCategory();
			queryTemplateCategory.setId(templateRsp.getTplId());
			queryTemplateCategory.setName(templateRsp.getTplName());
			queryTemplateCategory.setParentId(ctgId);
			queryTemplateCategory.setType(BIConsts.TEMPLATE_NODE_TYPE);
			queryTemplateCategory.setSortId((double) templateCtg.size());

			templateCtg.add(queryTemplateCategory);
			ctgTemplateMap.put(ctgId, templateCtg);
		}

		for (QueryTemplateCategory queryTemplateCategory : categories) {
			buildCtgSubTemplate(queryTemplateCategory, ctgTemplateMap);
		}

		return categories;
	}

	public void buildCtgSubTemplate(QueryTemplateCategory queryTemplateCategory, Map<String, List<QueryTemplateCategory>> ctgTemplateMap) {

		if (queryTemplateCategory == null) {
			return;
		}

		List<QueryTemplateCategory> queryTemplateCategoryList = ctgTemplateMap.get(queryTemplateCategory.getId());
		if (CollUtil.isNotEmpty(queryTemplateCategoryList)) {
			queryTemplateCategory.getChildren().addAll(queryTemplateCategoryList);
		}

		List<QueryTemplateCategory> ctgChild = queryTemplateCategory.getChildren().stream().filter(q -> !q.getType().equalsIgnoreCase(BIConsts.TEMPLATE_NODE_TYPE)).collect(Collectors.toList());
		if (CollUtil.isNotEmpty(ctgChild)) {
			for (QueryTemplateCategory child : ctgChild) {
				buildCtgSubTemplate(child, ctgTemplateMap);
			}
		}


	}

	/**
	 * 查询模版列表
	 *
	 * @param req
	 * @return
	 */
	public TemplatePageListRsp templateList(TemplatePageListReq req) {
		User user = UserManager.get();
		List<String> ctgIdList;
		if (CollUtil.isNotEmpty(req.getCtgIds())) {
			ctgIdList = req.getCtgIds();
		} else {
			ctgIdList = getTemplateCtgChildNode(req.getCtgId(), req.getShowAllCtg());
		}

		Map<String, Object> queryParams = new HashMap<>();
		queryParams.put("userName", user.getName());
		queryParams.put("templateSearchText", req.getTemplateSearchText());
		queryParams.put("ctgIdList", ctgIdList);
		queryParams.put("ctgId", req.getCtgId());
		queryParams.put("tplIds", req.getTplIds());
		int pageRowLower = (req.getCurrPageNo() - 1) * req.getPrePageSize();
		queryParams.put("pageRowLower", pageRowLower);
		queryParams.put("prePageSize", req.getPrePageSize());
		queryParams.put("isShowAll", req.getIsShowAll());
		queryParams.put("sortKey", req.getSortKey());
		queryParams.put("sortType", "asc".equals(req.getSortType()) ? "asc" : "desc");
		List<TemplateViewPageRsp> tplList = dao.queryObjectList("ssm.template.queryTemplateByCtg", queryParams, TemplateViewPageRsp.class);

		//查询数据集相关信息
		List<TemplateViewPageRsp> res = new ArrayList<>(tplList.size());
		if (CollUtil.isNotEmpty(tplList)) {
			List<String> datasetIdList = tplList.stream().map(TemplateViewPageRsp::getDatasetId).collect(Collectors.toList());

			Map<String, DatasetRsq> datasetMap = new HashMap<>();
			if (CollUtil.isNotEmpty(datasetIdList)) {
				List<DatasetRsq> datasetList = datasetService.listAll();
				datasetList.forEach(d -> datasetMap.put(d.getDatasetId(), d));
			}

			//查询视图信息
			List<String> tplIdList = tplList.stream().map(TemplateViewPageRsp::getTplId).collect(Collectors.toList());
			List<TemplateViewEntity> viewList = templateViewService.getByTplIdList(tplIdList);
			Map<String, List<TemplateViewEntity>> viewMap = viewList.stream().collect(Collectors.groupingBy(TemplateViewEntity::getTplId));

			//查询用户默认视图配置
			Map<String, Object> paramMap = new HashMap<>();
			paramMap.put("userName", user.getName());
			paramMap.put("tplIdList", tplIdList);
			List<String> userDefaultViewIdList = (List<String>) dao.queryObjectList("ssm.template.view.user.cfg.queryUserDefaultViewByTplIdList", paramMap);

			// 加目录路径
			List<String> ctgIds = tplList.stream().map(TemplateViewPageRsp::getCtgId).filter(StringUtils::isNotEmpty).collect(Collectors.toList());
			List<TemplateCtgPathEntity> ctgPathEntities = categoryService.getCtgPath(ctgIds);
			Map<String, String> ctgIdPathMap = ctgPathEntities.stream().collect(Collectors.toMap(TemplateCtgPathEntity::getCtgId, TemplateCtgPathEntity::getCtgNamePath));

			for (TemplateViewPageRsp templateRsp : tplList) {
				DatasetRsq dataset = datasetMap.get(templateRsp.getDatasetId());
				if (dataset != null) {
					templateRsp.setDatasetName(dataset.getDatasetName());
					templateRsp.setDatasetType(dataset.getDatasetType());
				}
				// 更加入参数据类型过滤
				if (!filterTemplateByDatasetDataType(dataset, req.getDatasetType())) {
					continue;
				}

				String userDefaultViewId = "";
				List<TemplateViewEntity> tplViewList = new ArrayList<>();
				List<TemplateViewEntity> viewEntities = viewMap.getOrDefault(templateRsp.getTplId(), Collections.emptyList());
				for (TemplateViewEntity viewEntity : viewEntities) {
					//如果用户设置了默认视图，默认选中默认视图
					if (userDefaultViewIdList.contains(viewEntity.getViewId())) {
						userDefaultViewId = viewEntity.getViewId();
						viewEntity.setIsDefault(Enabled.YES.getId());
					}

					tplViewList.add(viewEntity);
				}

				if (CollUtil.isNotEmpty(tplViewList)) {
					Collections.sort(tplViewList);

					//没有设置默认视图，取第一个视图
					if (StrUtil.isEmpty(userDefaultViewId)) {
						userDefaultViewId = tplViewList.get(0).getViewId();
					}
				}

				templateRsp.setDefaultSelectViewId(userDefaultViewId);
				templateRsp.setViewList(tplViewList);

				templateRsp.setCtgNamePath(ctgIdPathMap.get(templateRsp.getCtgId()));

				res.add(templateRsp);
			}
		}

		TemplatePageListRsp pageListRsp = new TemplatePageListRsp();
		if (req.getPrePageSize() != null && Integer.MAX_VALUE > req.getPrePageSize()) {
			Integer total = dao.queryCount("ssm.template.queryTemplateCountByCtg", queryParams);
			pageListRsp.setTotal(total);
		}
		pageListRsp.setRows(res);
		return pageListRsp;
	}

	private boolean filterTemplateByDatasetDataType(DatasetRsq dataset, String datasetType) {
		if (StringUtils.isEmpty(datasetType) || dataset == null) {
			return true;
		}

		if (DataTypeEnum.OFFLINE.getCode().equals(datasetType)) {
			//离线看板：数据集=使用离线或无标签的数据集的模版
			return DataTypeEnum.OFFLINE.getCode().equals(dataset.getDatasetType()) ||
					StringUtils.isEmpty(dataset.getDatasetType());
		}

		if (DataTypeEnum.REAL_TIME.getCode().equals(datasetType)) {
			//实时看板：数据集=使用实时的数据集的模版
			return DataTypeEnum.REAL_TIME.getCode().equals(dataset.getDatasetType());
		}

		return false;
	}

	private List<String> getTemplateCtgChildNode(String ctgId, Integer showAllCtg) {
		List<QueryTemplateCategory> categoryList;
		if (Enabled.YES.getId().equals(showAllCtg)) {
			categoryList = categoryService.getAllSpaceChildren(ctgId);
		} else {
			categoryList = categoryService.getAllChildren(ctgId);
		}
		List<String> ctgIdList = ListUtil.list(false);
		ctgIdList.addAll(categoryList.stream().map(QueryTemplateCategory::getId).collect(Collectors.toList()));
		ctgIdList.add(ctgId);
		return ctgIdList;
	}

	/**
	 * 批量删除模版
	 *
	 * @param req
	 */
	public void batchDelete(TemplateBatchOperateReq req) {
		checkOperateAuth(req, false);

		List<TemplateViewEntity> templateViewEntityList = templateViewService.getAllViewByTplIdList(req.getTplIdList());

		dao.executeTranscation(DataSourceType.Default, new AbstractTransaction() {
			@Override
			public void execute() {

				//删除模版基础配置
				dao.delete("ssm.template.batchDeleteTpl", req);

				//删除模版视图基础信息
				dao.delete("ssm.template.view.batchDeleteByTplIdList", req.getTplIdList());
				//删除视图默认信息
				dao.delete("ssm.template.view.user.cfg.batchDeleteByTplIdList", req.getTplIdList());

				// 批量删除config配置数据
				List<String> cfgIdList = templateViewEntityList.stream().map(TemplateViewEntity::getCfgId).collect(Collectors.toList());
				if (CollUtil.isNotEmpty(cfgIdList)) {
					dao.delete("ssm.template.cfg.batchDeleteByCfgIdList", cfgIdList);
					dao.delete("ssm.query.template.cfg.dtl.batchDeleteByCfgIdList", cfgIdList);
				}

				//批量删除跳转配置
				dao.delete("ssm.template.link.batchDeleteTemplateLinkByTemplateIds", req);

				//删除模版跳转配置字段配置
				List<String> linkIdList = (List<String>) dao.queryObjectList("ssm.template.link.queryLinkidBySourceTplIdOrTargetTpl", req);
				if (CollUtil.isNotEmpty(linkIdList)) {
					Map<String, Object> params = new HashMap<>();
					params.put("linkIdList", linkIdList);
					dao.delete("ssm.template.link.field.batchDeleteByLinkId", params);
				}
			}
		});

	}

	/**
	 * 批量移动模版
	 * @param req
	 */
	public void batchMove(TemplateMoveCtgReq req) {
		if (CollectionUtil.isEmpty(req.getTplIdList()) || req.getCtgId() == null || req.getMoveSource() == null) {
			return;
		}

		if (QueryTemplateCategoryType.SPACE.getId().equals(req.getMoveSource())) {
			checkOperateAuth(req, true);
			// 修改模版目录
			TemplatePageListReq favTemplateListReq = TemplatePageListReq.builder()
					.tplIds(req.getTplIdList())
					.currPageNo(1)
					.prePageSize(Integer.MAX_VALUE)
					.isShowAll(Enabled.NO.getId())
					.build();
			List<TemplateViewPageRsp> templates = templateList(favTemplateListReq).getRows();
			if (CollUtil.isEmpty(templates)) {
				return;
			}

			// 只修改是公共空间的目录
			List<String> myTplIds = templates.stream().filter(v -> QueryTemplateCategoryType.SPACE.getId().equals(v.getCtgType()))
					.map(TemplateEntity::getTplId).collect(Collectors.toList());
			if (CollUtil.isEmpty(myTplIds)) {
				return;
			}
			req.setTplIdList(myTplIds);
			dao.update("ssm.template.batchMoveTpl", req);
		} else {
			// 修改模版目录
			TemplatePageListReq favTemplateListReq = TemplatePageListReq.builder()
					.tplIds(req.getTplIdList())
					.currPageNo(1)
					.prePageSize(Integer.MAX_VALUE)
					.isShowAll(Enabled.NO.getId())
					.build();
			List<TemplateViewPageRsp> templates = templateList(favTemplateListReq).getRows();

			// 修改临时看板目录
			Map<String, Object> map = new HashMap<>();
			map.put("tplIdList", req.getTplIdList());
			map.put("ctgId", req.getCtgId());
			dao.delete("ssd.tmpAnalysisTplCtgRel.updateTmpTplCtg", map);


			// 只修改不是公共空间的目录
			List<String> myTplIds = templates.stream().filter(v -> !QueryTemplateCategoryType.SPACE.getId().equals(v.getCtgType()))
					.map(TemplateEntity::getTplId).collect(Collectors.toList());
			if (CollUtil.isEmpty(myTplIds)) {
				return;
			}
			req.setTplIdList(myTplIds);
			dao.update("ssm.template.batchMoveTpl", req);
		}
	}

	/**
	 * 校验模版操作权限
	 *
	 * @param req
	 */
	private void checkOperateAuth(TemplateBatchOperateReq req, Boolean checkCtg) {
		Map<String, QueryTemplateCategory> ctgMap = getQueryTemplateCategoryMap();
		List<TemplateRsp> tplList = (List<TemplateRsp>) dao.queryObjectList("ssm.template.queryTemplateByIds", req.getTplIdList());
		for (TemplateRsp ssdQueryTemplate : tplList) {
			if ("snapshot".equals(ssdQueryTemplate.getCtgId())) {
				return;
			}
			QueryTemplateCategory category = ctgMap.get(ssdQueryTemplate.getCtgId());
			if (QueryTemplateCategoryType.SPACE.getId().equals(category.getType())) {
				Boolean hasAuth = checkHasAuth(ctgMap, ssdQueryTemplate.getCtgId());
				if (!hasAuth) {
					throw new SSDException("无【" + ssdQueryTemplate.getTplName() + "】模版操作权限");
				}
			}
		}
		if (checkCtg) {
			QueryTemplateCategory templateCategory = ctgMap.get(req.getCtgId());
			if (templateCategory == null) {
				throw new SSDException("操作失败，移动目录不存在！");
			}

			if (QueryTemplateCategoryType.MY.getId().equals(templateCategory.getId())) {
				return;
			}
			if (Enabled.isFalse(templateCategory.getIsAdmin())) {
				throw new SSDException("操作失败，您没有移动目的目录操作权限！");
			}
		}
	}

	/**
	 * 是否有模版操作权限
	 *
	 * @return
	 */
	private Boolean checkHasAuth(Map<String, QueryTemplateCategory> ctgMap, String ctgId) {
		QueryTemplateCategory category = ctgMap.get(ctgId);
		if (category == null) {
			return false;
		}
		boolean hasAuth = false;
		if (QueryTemplateCategoryType.SPACE.getId().equals(category.getType())) {
			while (category != null && !hasAuth) {
				if (Enabled.YES.getId().equals(category.getIsAdmin())) {
					hasAuth = true;
				}
				category = ctgMap.get(category.getParentId());
			}
		}
		return hasAuth;
	}

	private void checkCtgAuth(String ctgId,boolean isOwner) {
		if (QueryTemplateCategoryType.MY.getId().equals(ctgId)) {
			return;
		}
		if (QueryTemplateCategoryType.SPACE.getId().equals(ctgId)) {
			throw new SSDException("操作失败，模版不能保存到共享空间！");
		}

		//2025-11-12 模板owner可以编辑共享空间的模板
		if(isOwner){
			return;
		}

		Map<String, QueryTemplateCategory> ctgMap = getQueryTemplateCategoryMap();
		QueryTemplateCategory templateCategory = ctgMap.get(ctgId);
		if (templateCategory == null || Enabled.isFalse(templateCategory.getIsAdmin())) {
			throw new SSDException("操作失败，您没有相关目录操作权限！");
		}
	}

	/**
	 * 新建/编辑模版
	 *
	 * @param req
	 * @return
	 */
	@SneakyThrows
	public TemplateViewRsp saveTemplate(TemplateAddReq req) {

		TemplateViewRsp templateViewSaveRsp = new TemplateViewRsp();

		User user = UserManager.get();
		if (BIUtil.isEmpty(req.getTplName())) {
			throw new SSDException("模板名称不能为空");
		}

		//生成快照时，不校验目录权限
		QueryTemplateType queryTemplateType = QueryTemplateType.get(req.getTplType());
		if (QueryTemplateType.NORMAL == queryTemplateType) {
			boolean isOwner = (req.getTplOwner() + ",").contains(user.getName() + ",");
			checkCtgAuth(req.getCtgId(),isOwner);
		}

		//owner限制人数：不超过5个人
		if(StrUtil.isNotEmpty(req.getTplOwner())) {
			Integer tplOwnerSize = req.getTplOwner().split(",").length;
			Integer maxTplOwnerSize = Integer.parseInt(SC.v("ssm.max.tpl.owner.size", "5"));
			if (tplOwnerSize > maxTplOwnerSize) {
				throw new SSDException("模版owner不能超过" + maxTplOwnerSize + "人!");
			}
		}

		//如果没有设置数据集，取默认数据集
		if (StringUtil.isEmpty(req.getDatasetId())) {
			req.setDatasetId(SC.v("ssm.default.datasetId", ""));
		}

		//如果没有设置模版拥有者，取当前用户
		if (StrUtil.isEmpty(req.getTplOwner())) {
			req.setTplOwner(user.getName());
		}

		String tplConfig = decryptTplConfig(req);
		if (StrUtil.isEmpty(req.getTplId())) {
			TemplateEntity templateEntity = new TemplateEntity();
			BeanUtil.copyProperties(req, templateEntity);

			templateEntity.setTplId(Guid.id());
			templateEntity.setCreatedBy(user.getName());
			templateEntity.setTplOwner(req.getTplOwner());

			//没有模版类型，默认为普通模版
			if (StrUtil.isEmpty(templateEntity.getTplType())) {
				templateEntity.setTplType(QueryTemplateType.NORMAL.getId());
			}

			templateEntity.setDatasetId(req.getDatasetId());

			dao.executeTranscation(DataSourceType.Default, new AbstractTransaction() {
				@Override
				public void execute() {
					dao.insert("ssm.template.saveTemplate", templateEntity);

					TemplateViewSaveReq templateViewSaveReq = new TemplateViewSaveReq();
					templateViewSaveReq.setTplId(templateEntity.getTplId());
					templateViewSaveReq.setTplConfig(tplConfig);
					templateViewSaveReq.setViewName(req.getViewName());

					//新增模版时-新增默认是公共视图
					templateViewSaveReq.setViewType(TemplateViewType.PUBLIC.getCode());
					templateViewSaveReq.setViewStatus(req.getViewStatus());

					templateViewSaveReq.setTplConfigFieldDimCodes(req.getTplConfigFieldDimCodes());
					templateViewSaveReq.setTplConfigFieldMeasureCodes(req.getTplConfigFieldMeasureCodes());
					templateViewSaveReq.setTplConfigFieldDimAsset(req.getTplConfigFieldDimAsset());
					templateViewSaveReq.setTplConfigFieldMeasureAsset(req.getTplConfigFieldMeasureAsset());

					templateViewSaveReq.setTemplateLinkList(req.getTemplateLinkList());
					templateViewSaveReq.setInvalidFieldCodeList(req.getInvalidFieldCodeList());
					templateViewSaveReq.setIsViewValid(req.getIsViewValid());
					String viewId = templateViewService.add(templateViewSaveReq);
					templateViewSaveRsp.setViewId(viewId);

				}
			});
			templateViewSaveRsp.setTplId(templateEntity.getTplId());
			return templateViewSaveRsp;
		} else {
			TemplateEntity templateEntity = (TemplateEntity) dao.queryObject("ssm.template.queryTemplateEntityById", req);
			if (templateEntity == null) {
				throw new SSDException("该模版已被删除，无法保存！");
			}
			String originalCtgId = templateEntity.getCtgId();
			String originalCreatedBy = templateEntity.getCreatedBy();
			BeanUtil.copyProperties(req, templateEntity);
			templateEntity.setUpdatedBy(user.getName());
			templateEntity.setTplOwner(req.getTplOwner());
			templateEntity.setDatasetId(req.getDatasetId());

			// 自动追加的 owner 修改原创建人的模板时，忽略 ctg_id 的更改（保持原目录）
			boolean isAutoAppendOwner = templateViewService.resolveAutoAppendOwners(originalCreatedBy).stream()
					.anyMatch(o -> o.equalsIgnoreCase(user.getName()));
			if (isAutoAppendOwner) {
				templateEntity.setCtgId(originalCtgId);
			}
			dao.executeTranscation(DataSourceType.Default, new AbstractTransaction() {
				@Override
				public void execute() {
					dao.update("ssm.template.updateTemplateBase", templateEntity);

					templateViewSaveRsp.setViewId(req.getViewId());

					//视图失效时，只保存模版，不保存视图
					//2025-11-24 迭代 资产不一致也可以保存，状态为临时视图
					//if (Enabled.value(req.getIsViewValid())) {

						//兼容历史数据
						String viewId = req.getViewId();
						if (StrUtil.isEmpty(req.getViewId())) {
							//viewId = templateEntity.getTplId();
							throw new SSDException("入参缺少视图id，无法保存！请刷新页面后重试！");
						}

						TemplateViewSaveReq templateViewSaveReq = new TemplateViewSaveReq();
						templateViewSaveReq.setTplId(templateEntity.getTplId());
						templateViewSaveReq.setViewId(viewId);
						templateViewSaveReq.setViewName(req.getViewName());
						templateViewSaveReq.setViewType(req.getViewType());
						templateViewSaveReq.setViewStatus(req.getViewStatus());
						templateViewSaveReq.setIsViewValid(req.getIsViewValid());

						templateViewSaveReq.setTplConfig(tplConfig);
						templateViewSaveReq.setTplConfigFieldDimCodes(req.getTplConfigFieldDimCodes());
						templateViewSaveReq.setTplConfigFieldMeasureCodes(req.getTplConfigFieldMeasureCodes());
						templateViewSaveReq.setTplConfigFieldDimAsset(req.getTplConfigFieldDimAsset());
						templateViewSaveReq.setTplConfigFieldMeasureAsset(req.getTplConfigFieldMeasureAsset());

						templateViewSaveReq.setIsSaveTemplateLink(Enabled.YES.getId());
						templateViewSaveReq.setTemplateLinkList(req.getTemplateLinkList());
						templateViewSaveReq.setInvalidFieldCodeList(req.getInvalidFieldCodeList());
						templateViewService.update(templateViewSaveReq);

						templateViewSaveRsp.setViewId(viewId);
					//}

				}
			});

			templateViewSaveRsp.setTplId(templateEntity.getTplId());
			return templateViewSaveRsp;
		}
	}

	/**
	 * 模版另存为
	 *
	 * @param req
	 * @return
	 */
	public TemplateViewRsp saveAsTemplateAndView(TemplateAddReq req) {

		String tplId = req.getTplId();
		String viewId = req.getViewId();
		List<TemplateViewMapping> viewIdMappingList = new ArrayList<>();

		//模版另存保存当前的配置
		req.setTplId("");
		req.setViewId("");
		TemplateViewRsp templateViewSaveRsp = saveTemplate(req);

		TemplateViewMapping templateViewMapping = new TemplateViewMapping(viewId, templateViewSaveRsp.getViewId());
		viewIdMappingList.add(templateViewMapping);

		//修改当前配置的视图的排序与原始视图一致
		templateViewService.syncViewSorting(viewId, templateViewSaveRsp.getViewId());

		//拷贝模版其他的视图
		List<TemplateViewMapping> copyViewIdMappingList =  templateViewService.copyView(tplId, templateViewSaveRsp.getTplId(), viewId,req.getTplType());
		if(CollUtil.isNotEmpty(copyViewIdMappingList)){
			viewIdMappingList.addAll(copyViewIdMappingList);
		}

		//重新判断其他视图是否有效
		//场景： 前端修改当前视图配置，与其他有效视图不一致
		if (Enabled.value(req.getIsViewValid())) {

			//兼容历史数据, 如果配置了维度或指标字段，则设置无效
			if (StrUtil.isNotEmpty(req.getTplConfigFieldDimCodes()) || StrUtil.isNotEmpty(req.getTplConfigFieldMeasureCodes())) {
				TemplateCfgDtlEntity tplCfgDtlEntity = new TemplateCfgDtlEntity(
						req.getTplConfigFieldDimCodes(),
						req.getTplConfigFieldMeasureCodes(),
						SSDUtil.decryptTplConfigFieldAsset(req.getTplConfigFieldDimAsset()),
						SSDUtil.decryptTplConfigFieldAsset(req.getTplConfigFieldMeasureAsset())
				);
				tplCfgDtlEntity.setDatasetId(req.getDatasetId());
				templateViewService.setViewDisabled(templateViewSaveRsp.getTplId(), templateViewSaveRsp.getViewId(), tplCfgDtlEntity);
			}
		}

		templateViewSaveRsp.setViewIdMappingList(viewIdMappingList);
		return templateViewSaveRsp;
	}

	public void updateTemplateTitle(TemplateAddReq req) {
		User user = UserManager.get();
		TemplateEntity templateEntity = (TemplateEntity) dao.queryObject("ssm.template.queryTemplateEntityById", req);
		templateEntity.setUpdatedBy(user.getName());
		templateEntity.setTplName(req.getTplName());
		dao.update("ssm.template.updateTemplateTitle", templateEntity);
	}

	public void updateTemplateTitleAndDesc(TemplateAddReq req) {
		User user = UserManager.get();
		TemplateEntity templateEntity = (TemplateEntity) dao.queryObject("ssm.template.queryTemplateEntityById", req);
		templateEntity.setUpdatedBy(user.getName());
		templateEntity.setTplName(req.getTplName());
		templateEntity.setTplDesc(req.getTplDesc());
		dao.update("ssm.template.updateTemplateTitleAndDesc", templateEntity);
	}


	public SSDQueryTemplate getTemplateBaseInfoById(String templateId, String viewId) {
		// 需判断模版是否可保存（我的模版可保存，他人分享不可保存，公共空间模版管理员可保存）
		User user = UserManager.get();
		Map<String, String> queryMap = new HashMap<>();
		queryMap.put("templateId", templateId);
		queryMap.put("userName", user.getName());

		//查询模版基础信息
		SSDQueryTemplate tpl = (SSDQueryTemplate) dao.queryObject("ssm.template.queryTemplateById", queryMap);
		if (tpl == null) {
			throw new SSDException("模版不存在，加载失败！模版id:"+templateId);
		}

		String tplDatasetId = tpl.getDatasetId();

		// 添加收藏相关消息
		TemplateFavEntity tplFavEntity = getTplFavInfo(templateId);
		if (tplFavEntity != null) {
			tpl.setIsFav(tplFavEntity.getIsFav());
			tpl.setFavCtgId(tplFavEntity.getCtgId());
		}

		//没有视图id，取默认视图
		if (StrUtil.isEmpty(viewId)) {
			viewId = templateViewService.getDefaultViewIdByTplId(templateId);
		}

		//判断视图是否存在，不存在将视图id置空，后续取第一个有效视图
		//场景，在看板中，已经使用的视图被删除
		if (StrUtil.isNotEmpty(viewId)) {
			TemplateViewEntity tv = templateViewService.getByViewId(viewId);
			if (tv == null) {
				viewId = "";
			}
		}

		//没有默认视图，取第一个有效的公共视图
		if (StrUtil.isEmpty(viewId)) {
			viewId = templateViewService.getFirstValidViewIdByTplId(templateId);
		}

		if (StrUtil.isEmpty(viewId)) {
			throw new SSDException("模版视图不存在，加载失败！");
		}

		if (StrUtil.isNotEmpty(viewId)) {
			TemplateViewEntity templateViewEntity = templateViewService.getByViewId(viewId);
			String cfgId = null;
			if (templateViewEntity != null) {
				cfgId = templateViewEntity.getCfgId();
				tpl.setConfig(templateViewEntity.getTplConfig());
				tpl.setViewId(templateViewEntity.getViewId());
				tpl.setViewName(templateViewEntity.getViewName());
				tpl.setViewType(templateViewEntity.getViewType());
				tpl.setViewStatus(templateViewEntity.getViewStatus());
				tpl.setIsViewValid(templateViewEntity.getIsActive());
				tpl.setViewCreatedBy(templateViewEntity.getCreatedBy());

				if (StrUtil.isNotEmpty(templateViewEntity.getDatasetId())) {
					tpl.setDatasetId(templateViewEntity.getDatasetId());
				}

				//个人视图-视图owner可保存
				TemplateViewType viewType = TemplateViewType.get(templateViewEntity.getViewType());
				if (TemplateViewType.PERSONAL == viewType) {
					if (templateViewEntity.getCreatedBy().equalsIgnoreCase(user.getName())) {
						tpl.setCanSaveView(Enabled.YES.getId());
					}
				} else {
					//非共享空间的模版公共视图-模版owner可保存
					if (Enabled.NO.getId().equals(tpl.getIsSpaceTpl())) {
						if ((tpl.getTplOwner() + ",").contains(user.getName() + ",")) {
							tpl.setCanSaveView(Enabled.YES.getId());
						}
					}
				}
			}

			// UT 有影子 dtl 时按 cfgId 取影子，与 tplConfig 影子读一致
			TemplateCfgDtlEntity cfgDtlEntity = templateViewService.getCfgDtlByTplId(templateId, cfgId);
			if (cfgDtlEntity != null) {
				tpl.setTplConfigFieldDimCodes(cfgDtlEntity.getTplConfigFieldDimCodes());
				tpl.setTplConfigFieldMeasureCodes(cfgDtlEntity.getTplConfigFieldMeasureCodes());
				tpl.setTplConfigFieldDimAsset(cfgDtlEntity.getTplConfigFieldDimAsset());
				tpl.setTplConfigFieldMeasureAsset(cfgDtlEntity.getTplConfigFieldMeasureAsset());
				tpl.setIsTplFieldCodeSaved(Enabled.YES.getId());
			}
		}

		String ctgId = tpl.getCtgId();
		//判断是不是快照模版
		QueryTemplateType queryTemplateType = QueryTemplateType.get(tpl.getTplType());
		if (QueryTemplateType.SNAPSHOT == queryTemplateType || QueryTemplateType.ANALYSIS_TEMPLATE_SNAPSHOT == queryTemplateType) {
			tpl.setIsSnapshotTpl(Enabled.YES.getId());

			//快照都可另存
			tpl.setCanSave(Enabled.YES.getId());

			//快照模版，查询原始的模版的路径
			String sourceTplCtgId = (String) dao.queryObject("ssm.template.querySourceTplCtgIdByTargetTplId", tpl.getId());
			if (StrUtil.isNotEmpty(sourceTplCtgId)) {
				ctgId = sourceTplCtgId;
			}
		}

		List<String> ctgIds = new ArrayList<>();
		ctgIds.add(ctgId);

		//判断模版是公域还是私域
		List<String> publicCtgIds = categoryService.getPublicCtgList(ctgIds);
		if (publicCtgIds.contains(ctgId)) {
			tpl.setIsInPublicDomain(Enabled.YES.getId());
		}

		// 按最终 datasetId 解析数据集类型；同时回填模板级数据集信息供前端对比展示
		List<DatasetRsq> datasetList = datasetService.listAll();
		if (StrUtil.isNotEmpty(tplDatasetId)) {
			tpl.setTplDatasetId(tplDatasetId);
			datasetList.stream()
					.filter(r -> Objects.equals(r.getDatasetId(), tplDatasetId))
					.findFirst()
					.ifPresent(r -> tpl.setTplDatasetName(r.getDatasetName()));
		}
		if (StrUtil.isNotEmpty(tpl.getDatasetId())) {
			datasetList.stream()
					.filter(r -> Objects.equals(r.getDatasetId(), tpl.getDatasetId()))
					.findFirst()
					.ifPresent(r -> tpl.setDatasetType(r.getDatasetType()));
		}

		return tpl;
	}


	// 获取模板收藏信息
	private TemplateFavEntity getTplFavInfo(String templateId) {
		User user = UserManager.get();
		Map<String, Object> queryParams = MapUtil.newHashMap();
		queryParams.put("userName", user.getName());
		queryParams.put("tplIdList", Collections.singletonList(templateId));
		List<TemplateFavEntity> existEntities = dao.queryObjectList("ssm.template.fav.selectByTplIdAndUserName", queryParams, TemplateFavEntity.class);
		if (CollUtil.isEmpty(existEntities)) {
			return null;
		}
		return existEntities.stream().filter(v -> FavTemplateType.QUERY_TEMPLATE.getCode().equals(v.getFavTplType()))
				.findFirst().orElse(null);
	}

	/**
	 * 通过id获取模板(含权限）
	 *
	 * @param templateId
	 * @return
	 */
	public SSDQueryTemplate getTemplateById(String templateId, String viewId) {
		// 需判断模版是否可保存（我的模版可保存，他人分享不可保存，公共空间模版管理员可保存）
		SSDQueryTemplate tpl = this.getTemplateBaseInfoById(templateId, viewId);
		if (tpl == null) {
			throw new SSDException("模版已被删除，加载失败！模版id:" + templateId);
		}

		User user = UserManager.get();
		List<String> tplOnwer = Arrays.asList(tpl.getTplOwner().split(","));

		boolean isOwner = tplOnwer.contains(user.getName());

		if (Enabled.YES.getId().equals(tpl.getIsSpaceTpl())) {
			Boolean hasAuth = checkUserCtgAuth(tpl.getCtgId());

			//2025-11-13 模板owner也可以操作共享空间的模板
			tpl.setCanSave(hasAuth || isOwner ? Enabled.YES.getId() : Enabled.NO.getId());
			//公共空间的公共视图，权限等同于模版
			TemplateViewType viewType = TemplateViewType.get(tpl.getViewType());
			if (TemplateViewType.PUBLIC == viewType) {
				tpl.setCanSaveView(tpl.getCanSave());
			}
		}

		// 个人视图：若创建人不是当前用户，则不可保存
		TemplateViewType currentViewType = TemplateViewType.get(tpl.getViewType());
		if (TemplateViewType.PERSONAL == currentViewType && !user.getName().equalsIgnoreCase(tpl.getViewCreatedBy())) {
			tpl.setCanSave(Enabled.NO.getId());
		}

		//判断是否需要轮询检测模版资产是否变更
		boolean isEnablePollTemplateAssetChange = "true".equalsIgnoreCase(SC.v("ssm.poll.template.asset.change.enable", "true"));
		if (isEnablePollTemplateAssetChange) {

			// 条件满足以下，添加视图更新提醒
			//  1 模板是多owner或者在共享空间
			//  2 可以保存当前模板公共视图的人
//			if (Enabled.value(tpl.getCanSave())) {
//				if (Enabled.value(tpl.getIsSpaceTpl()) || tplOnwer.size() > 1) {
//					tpl.setIsPollTemplateAssetChange(Enabled.YES.getId());
//					tpl.setPollTemplateAssetChangeInterval(Integer.parseInt(SC.v("ssm.poll.template.asset.change.interval", "30")));
//				}
//			}

			//初版不限制，所有场景都需要轮询检测
			tpl.setIsPollTemplateAssetChange(Enabled.YES.getId());

		}

		// 特定创建人新建模板时，自动追加配置的 owner
		List<String> autoAppendOwners = templateViewService.resolveAutoAppendOwners(tpl.getCreatedBy());
		if (autoAppendOwners.contains(user.getName())) {
			tpl.setCanSave(Enabled.YES.getId());
			tpl.setCanSaveView(Enabled.YES.getId());
		}

		return tpl;
	}


	/**
	 * 通过视图id查询模版
	 * @param viewId
	 * @return
	 */
	public SSDQueryTemplate getTemplateByViewId(String viewId){
		TemplateViewEntity tv = templateViewService.getByViewId(viewId);
		return getTemplateById(tv.getTplId(), tv.getViewId());
	}

	/**
	 * 通过视图id查询模版id
	 * @param viewId
	 * @return
	 */
	public String getTplIdByViewId(String viewId) {
		return templateViewService.getTplIdByViewId(viewId);
	}

	/**
	 * 校验用户是否有目录权限
	 * @param ctgId
	 * @return
	 */
	public  Boolean checkUserCtgAuth(String ctgId) {
		Map<String, QueryTemplateCategory> ctgMap = getQueryTemplateCategoryMap();
		Boolean hasAuth = checkHasAuth(ctgMap, ctgId);
		return hasAuth;
	}

	/**
	 * 用户所有目录转Map
	 *
	 * @return
	 */
	public Map<String, QueryTemplateCategory> getQueryTemplateCategoryMap() {
		List<QueryTemplateCategory> ctgList = categoryService.getAllCategories();
		return ctgList.stream().collect(Collectors.toMap(QueryTemplateCategory::getId, k -> k));
	}

	/**
	 * 批量获取模板配置
	 *
	 * @param tplIdList
	 * @return
	 */
	public List<TemplateRsp> batchQueryTemplateCfg(List<String> tplIdList) {

		List<TemplateRsp> templateRspList = (List<TemplateRsp>) dao.queryObjectList("ssm.template.queryTemplateByIds", tplIdList);

		//查询模版的配置，取任一有效的配置
		List<TemplateViewEntity> tplCfgList = (List<TemplateViewEntity>) dao.queryObjectList("ssm.template.view.queryCfgByTplIds", tplIdList);
		if (CollUtil.isEmpty(tplCfgList)) {
			return templateRspList;
		}

		//设置模版的配置信息
		for (TemplateRsp templateRsp : templateRspList) {
			Optional<TemplateViewEntity> optionalTemplateView = tplCfgList.stream().filter(f -> f.getTplId().equals(templateRsp.getTplId())).findAny();
			if (!optionalTemplateView.isPresent()) {
				continue;
			}
			templateRsp.setTplConfig(optionalTemplateView.get().getTplConfig());
		}

		return templateRspList;
	}


	/**
	 * 获取模板的更新时间
	 *
	 * @param req
	 * @return
	 */
	public List<TemplateDataUpdateTimeRsp> getTemplateDataUpdateTime(TemplateDataUpdateTimeReq req) {

		List<TemplateDataUpdateTimeRsp> templateDataUpdateTimeRspList = new ArrayList<>();

		List<String> tplIdList = req.getTplIdList();
		if (CollUtil.isEmpty(tplIdList)) {
			return templateDataUpdateTimeRspList;
		}

		try {

			//查询所有的数据集
			List<DatasetRsq> result = (List<DatasetRsq>) dao.queryObjectList("ssm.dataset.listAll", null);
			DataSourceType mgpDataSourceType = SSDUtil.getMgpDataSourceType();
			List<DatasetRsq> mgpDatasetResult = (List<DatasetRsq>) dao.queryObjectList("ssm.dataset.listAll", null, mgpDataSourceType);
			if (CollUtil.isNotEmpty(mgpDatasetResult)) {
				result.addAll(mgpDatasetResult);
			}

			Map<String, DatasetRsq> datasetRsqMap = new HashMap<>();
			for (DatasetRsq datasetRsq : result) {
				datasetRsqMap.put(datasetRsq.getDatasetId(), datasetRsq);
			}

			List<TemplateRsp> templateRspList = batchQueryTemplateCfg(tplIdList);

			List<String> allEtlJobList = new ArrayList<>();

			//遍历所有的模板，找到模板查询字段对应的数据表的作业名
			Map<String, List<String>> templateEtljobMap = new HashMap<>();
			for (TemplateRsp templateRsp : templateRspList) {

				SSDQueryTemplate queryTemplate = new SSDQueryTemplate();
				queryTemplate.setConfig(templateRsp.getTplConfig());

				QueryConfigure queryConfigure = new QueryConfigure(queryTemplate);
				queryConfigure.load();

				queryConfigure.getSettings().setAclCheck(false);
				QueryContext cxt = new QueryContext();
				QueryEngine engine = QueryFactory.createEngine(queryConfigure, cxt);

				//找到模板使用的etljob
				List<String> templateEtljobList = new ArrayList<>();
				for (StarModel starModel : engine.getModels()) {
					templateEtljobList.addAll(starModel.getFactTable().getMeta().getEtlJobs());
					allEtlJobList.addAll(starModel.getFactTable().getMeta().getEtlJobs());
				}

				templateEtljobMap.put(templateRsp.getTplId(), templateEtljobList);
			}

			//etl作业更新时间map
			Map<String, Date> etljobUpdateTimeMap = new HashMap<>();
			if (CollUtil.isNotEmpty(allEtlJobList)) {
				List<SysEtlJobInfo> etlJobInfos = (List<SysEtlJobInfo>) dao.queryObjectList("ssm.query.querySysEtlJobInfo", allEtlJobList, DataSourceType.ETL);
				for (SysEtlJobInfo sysEtlJobInfo : etlJobInfos) {
					etljobUpdateTimeMap.put(sysEtlJobInfo.getEtlJob(), sysEtlJobInfo.getEtlTime());
				}
			}

			//封装结果集
			for (TemplateRsp templateRsp : templateRspList) {

				TemplateDataUpdateTimeRsp templateDataUpdateTimeRsp = new TemplateDataUpdateTimeRsp();
				templateDataUpdateTimeRsp.setTplId(templateRsp.getTplId());
				templateDataUpdateTimeRsp.setDatasetName(datasetRsqMap.get(templateRsp.getDatasetId()).getDatasetName());
				templateDataUpdateTimeRsp.setDataUpdateTime(getTemplateUpdateTime(templateEtljobMap.get(templateRsp.getTplId()), etljobUpdateTimeMap));

				templateDataUpdateTimeRspList.add(templateDataUpdateTimeRsp);

			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return templateDataUpdateTimeRspList;
	}

	/**
	 * 获取模板的更新时间
	 */
	public String getTemplateUpdateTime(List<String> etljobs, Map<String, Date> etljobUpdateTimeMap) {

		String result = "";

		if (CollUtil.isEmpty(etljobs)) {
			return "";
		}

		Date updateTimeMax = null;
		for (String etljob : etljobs) {
			Date updateTime = etljobUpdateTimeMap.get(etljob);
			if (updateTime != null) {
				updateTimeMax = getJobUpdateTime(updateTimeMax, updateTime);
			}
		}

		if (updateTimeMax != null) {
			SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			result = simpleDateFormat.format(updateTimeMax);
		}
		return result;
	}

	// 取更新时间方法，先比较日期，取日期小的，日期相同，取时间大的
	public Date getJobUpdateTime(Date time1, Date time2) {
		if (time1 == null) {
			return time2;
		}
		//日期相同，取时间大的
		if (DateUtil.format(time1, "yyyy-MM-dd").equals(DateUtil.format(time2, "yyyy-MM-dd"))) {
			return DateUtil.compare(time1, time2) > 0 ? time1 : time2;
		}
		//日期不同，取日期小的
		return DateUtil.compare(time1, time2) > 0 ? time2 : time1;
	}

	/**
	 * 通过分类id获取模版
	 * 模版在子目录也会查询出来
	 *
	 * @param ctgId
	 * @return
	 */
	public List<TemplateRsp> getTemplateByCtgId(String ctgId) {
		List<TemplateRsp> templateList = (List<TemplateRsp>) dao.queryObjectList("ssm.template.getTemplateByCtgId", ctgId);
		return templateList;
	}

	/**
	 * 通过模版id获取模版的基础信息
	 *
	 * @param tplId
	 * @return
	 */
	public TemplateRsp getBaseInfoByTplId(String tplId) {
		User user = UserManager.get();
		Map<String, String> queryMap = new HashMap<>();
		queryMap.put("templateId", tplId);
		queryMap.put("userName", user.getName());

		//查询模版基础信息
		SSDQueryTemplate tpl = (SSDQueryTemplate) dao.queryObject("ssm.template.queryTemplateById", queryMap);

		if (tpl == null) {
			throw new SSDException("模版不存在！");
		}

		TemplateRsp templateRsp = new TemplateRsp();
		templateRsp.setTplId(tpl.getId());
		templateRsp.setTplName(tpl.getName());
		return templateRsp;
	}

	public List<TemplateCtgTreeRsp> getCategoryTplTree(List<String> ctgRootIds, boolean isMySpace, String portalId) {
		List<TemplateCtgTreeRsp> result = getCategoryTplTree(ctgRootIds, isMySpace, portalId, null);

		// 获取公域信息
		fillTplPublicDomainInfo(result);

		// 顶层目录的父目录id设为null
		result.forEach(v -> {
			if (ctgRootIds.contains(v.getParentId())) {
				v.setParentId(null);
			}
		});
		return result;
	}

	/**
	 * 获取目录模板树
	 *
	 * @return 指定id下的目录树
	 */
	public List<TemplateCtgTreeRsp> getCategoryTplTree(List<String> ctgRootIds, boolean isMySpace, String portalId, String datasetType) {
		List<TemplateCtgTreeRsp> result = new ArrayList<>();
		if (CollUtil.isEmpty(ctgRootIds)) {
			return result;
		}

		List<QueryTemplateCategory> ctgTreeRsps = new ArrayList<>();
		List<QueryTemplateCategory> categories;
		if (StringUtils.isNotEmpty(portalId)) {
			categories = categoryService.getCategoriesWithAuth();
		} else {
			categories = categoryService.getAllCategories();
		}

		for (QueryTemplateCategory ctg : categories) {
			if (ctgRootIds.contains(ctg.getParentId())) {
				ctgTreeRsps.add(ctg);
			}
		}

		result = getSubCtgIds(ctgTreeRsps);
		List<String> allCtgIds = result.stream().map(QueryTemplateCategory::getId).collect(Collectors.toList());
		allCtgIds.addAll(ctgRootIds);

		//查询目录下的查询模板
		TemplatePageListReq templateListReq = TemplatePageListReq.builder()
				.ctgIds(allCtgIds)
				.datasetType(datasetType)
				.currPageNo(1)
				.prePageSize(Integer.MAX_VALUE)
				.isShowAll(Enabled.NO.getId())
				.build();
		TemplatePageListRsp templatePageListRsp = templateList(templateListReq);
		List<TemplateViewPageRsp> myTemplates = templatePageListRsp.getRows();

		for (TemplateViewPageRsp tpl : myTemplates) {
			TemplateCtgTreeRsp rsp = TemplateCtgTreeRsp.of(tpl);
			result.add(rsp);
		}

		// 他人分享的处理
		getShareTplTree(result, isMySpace);

		// 查我创建的个人多模板看板
		getTmpAnalysisTplTree(result, isMySpace);

		// 查我收藏和我的置顶
		getFavTplTree(result, isMySpace, datasetType);

		return result;
	}

	public List<TemplateCtgTreeRsp> getSubCtgIds(List<QueryTemplateCategory> roots) {
		List<TemplateCtgTreeRsp> result = new ArrayList<>();
		if (CollUtil.isEmpty(roots)) {
			return result;
		}

		for (QueryTemplateCategory ctg : roots) {
			result.add(TemplateCtgTreeRsp.of(ctg));
			result.addAll(getSubCtgIds(ctg.getChildren()));
		}
		return result;
	}

	// 查我创建的临时看板
	private List<TemplateCtgTreeRsp> getTmpAnalysisTplTree(List<TemplateCtgTreeRsp> result, boolean isMySpace) {
		if (!isMySpace) {
			return result;
		}
		/* genAI_feature/v3.15.0_start */
		Set<String> treeCtgIds = result.stream()
				.filter(r -> FavTemplateType.CTG.getCode().equals(r.getType()))
				.map(TemplateCtgTreeRsp::getId)
				.filter(StringUtils::isNotBlank)
				.collect(Collectors.toSet());
		treeCtgIds.add(QueryTemplateCategoryType.MY.getId());
		Map<String, Object> queryMap = new HashMap<>(4);
		queryMap.put("userName", UserManager.get().getName());
		queryMap.put("ctgIds", new ArrayList<>(treeCtgIds));
		List<TmpAnalysisTplCtgRelEntity> rows = dao.queryObjectList(
				"ssd.tmpAnalysisTplCtgRel.listMountedTmpAnalysisByUserAndCtgIds",
				queryMap,
				TmpAnalysisTplCtgRelEntity.class);
		if (CollUtil.isEmpty(rows)) {
			return result;
		}

		List<TemplateCtgPathEntity> ctgPathEntities = categoryService.getCtgPath(new ArrayList<>(treeCtgIds));
		Map<String, String> ctgIdToNamePath = ctgPathEntities.stream()
				.collect(Collectors.toMap(TemplateCtgPathEntity::getCtgId, TemplateCtgPathEntity::getCtgNamePath, (a, b) -> a));
		for (TmpAnalysisTplCtgRelEntity row : rows) {
			TemplateCtgTreeRsp node = TemplateCtgTreeRsp.of(row);
			node.setCtgNamePath(ctgIdToNamePath.get(row.getCtgId()));
			result.add(node);
		}
		/* genAI_feature/v3.15.0_end */
		return result;
	}

	private List<TemplateCtgTreeRsp> getFavTplTree(List<TemplateCtgTreeRsp> result, boolean isMySpace, String datasetType) {
		List<TemplateFavEntity> favTpls = getFavTpl();
		Map<String, TemplateFavEntity> favTplMap = favTpls.stream().collect(Collectors.toMap(TemplateFavEntity::getTplId, v -> v));

		for (TemplateCtgTreeRsp rsp : result) {
			TemplateFavEntity favTpl = favTplMap.remove(rsp.getId());
			if (favTpl == null) {
				continue;
			}

			rsp.setFavCtgId(favTpl.getCtgId());
			if (isMySpace) {
				//我的模板不能被收藏, 兼容历史数据
				rsp.setIsTop(favTpl.getIsTop());
				rsp.setIsFav(Enabled.NO.getId());
				rsp.setSortId(favTpl.getTopSortId());
			} else if (Enabled.YES.getId().equals(rsp.getIsFav())) {
				// 共享空间里的看板被收藏了
				rsp.setIsFav(Enabled.YES.getId());
			}
		}

		if (CollUtil.isEmpty(favTplMap) || !isMySpace) {
			return result;
		}

		//收藏的模板和看板
		TemplatePageListReq favTemplateListReq = TemplatePageListReq.builder()
				.tplIds(new ArrayList<>(favTplMap.keySet()))
				.currPageNo(1)
				.datasetType(datasetType)
				.prePageSize(Integer.MAX_VALUE)
				.isShowAll(Enabled.YES.getId())
				.showAllCtg(Enabled.YES.getId())
				.ctgId(QueryTemplateCategoryType.SPACE.getId())
				.build();
		List<TemplateViewPageRsp> favTemplates = templateList(favTemplateListReq).getRows();
		if (CollUtil.isEmpty(favTemplates)) {
			return result;
		}
		Map<String, TemplateViewPageRsp> favTplEntityMap = favTemplates.stream().collect(Collectors.toMap(TemplateEntity::getTplId, v -> v));

		for (TemplateFavEntity fav : favTpls) {
			if (FavTemplateType.ANALYSIS_TEMPLATE.getCode().equals(fav.getFavTplType())) {
				TemplateCtgTreeRsp rsp = TemplateCtgTreeRsp.of(null, fav);
				// 看板也都是收藏过来的
				rsp.setIsFav(Enabled.YES.getId());
				rsp.setType(FavTemplateType.ANALYSIS_TEMPLATE.getCode());
				result.add(rsp);
			} else if (favTplMap.containsKey(fav.getTplId())) {
				TemplateViewPageRsp favViewRsp = favTplEntityMap.get(fav.getTplId());
				if (favViewRsp == null) {
					continue;
				}
				TemplateCtgTreeRsp rsp = TemplateCtgTreeRsp.of(favViewRsp, fav);
				// 共享空间的都是收藏过来的
				rsp.setIsFav(Enabled.YES.getId());
				rsp.setType(fav.getFavTplType());
				result.add(rsp);
			}
		}
		return result;
	}

	// 他人分享单独一个文件件
	private void getShareTplTree(List<TemplateCtgTreeRsp> result, boolean isMySpace) {
		if (!isMySpace) {
			return;
		}

		boolean hasShare = false;
		for (TemplateCtgTreeRsp rsp : result) {
			if (QueryTemplateCategoryType.SHARE.getId().equals(rsp.getParentId())) {
				hasShare = true;
				break;
			}
		}

		if (hasShare) {
			return;
		}

		result.removeIf(v -> QueryTemplateCategoryType.SHARE.getId().equals(v.getId()));
	}

	public List<TemplateFavEntity> getFavTpl() {
		Map<String, String> queryMap = new HashMap<>();
		queryMap.put("userName", UserManager.get().getName());
		List<TemplateFavEntity> favTpls = dao.queryObjectList("ssm.template.fav.selectByUserName", queryMap, TemplateFavEntity.class);
		if (CollUtil.isEmpty(favTpls)) {
			return new ArrayList<>();
		}
		return favTpls;
	}

	// 填充公域相关信息
	private void fillTplPublicDomainInfo(List<TemplateCtgTreeRsp> rsps) {
		List<String> queryTplCtgIds = new ArrayList<>();
		List<String> favAnalysisTplIds = new ArrayList<>();
		Map<String, List<TemplateCtgTreeRsp>> tplMap = new HashMap<>();
		for (TemplateCtgTreeRsp tpl : rsps) {
			if (FavTemplateType.ANALYSIS_TEMPLATE.getCode().equals(tpl.getType())) {
				favAnalysisTplIds.add(tpl.getId());
				tplMap.computeIfAbsent(tpl.getId(), k -> new ArrayList<>()).add(tpl);
			} else if (FavTemplateType.QUERY_TEMPLATE.getCode().equals(tpl.getType())) {
				queryTplCtgIds.add(tpl.getCtgId());
				tplMap.computeIfAbsent(tpl.getCtgId(), k -> new ArrayList<>()).add(tpl);
			}
		}

		List<PortalPublicDomainResp> portalPublicDomainInfo = categoryService.getPublicDomainInfo(queryTplCtgIds, favAnalysisTplIds);

		for (PortalPublicDomainResp resp : portalPublicDomainInfo) {
			List<TemplateCtgTreeRsp> tplList = null;
			if (resp.getCtgId() != null) {
				tplList = tplMap.get(resp.getCtgId());
			} else if (resp.getAnalysisTplMenuId() != null) {
				tplList = tplMap.get(resp.getAnalysisTplMenuId());
			}

			if (tplList == null) {
				continue;
			}
			for (TemplateCtgTreeRsp tpl : tplList) {
				tpl.setIsInPublicDomain(resp.getIsInPublicDomain());
				String path = StringUtils.substringAfter(tpl.getCtgNamePath(), resp.getPortalMenuPath());
				tpl.setCtgNamePath((resp.getPortalName() == null ? "" : resp.getPortalName()) + resp.getPortalMenuPath() + (path == null ? "" : path));
				if (StringUtils.isEmpty(tpl.getName())) {
					tpl.setName(resp.getPortalMenuName());
				}
				if (FavTemplateType.ANALYSIS_TEMPLATE.getCode().equals(tpl.getType())) {
					tpl.setDescription(resp.getPortalMenuDesc());
					tpl.setRootCtgId(resp.getPortalId());
					tpl.setTplOwner(resp.getAnalysisTplOwners());
					tpl.setRootCtgName(resp.getPortalName());
				} else if (FavTemplateType.QUERY_TEMPLATE.getCode().equals(tpl.getType())) {
					tpl.setRootCtgId(resp.getSpaceCtgId());
					tpl.setRootCtgName(resp.getSpaceCtgName());
				}
			}
		}
	}

	/**
	 * 获取模版是否更新, 如果更新则返回模版信息
	 * @return
	 */
	public SSDQueryTemplate getAssetIfTemplateUpdated(TemplateAssetUpdatedReq templateAssetUpdatedReq) {
		SSDQueryTemplate result = new SSDQueryTemplate();

		boolean isEnablePollTemplateAssetChange = "true".equalsIgnoreCase(SC.v("ssm.poll.template.asset.change.enable", "true"));
		if (!isEnablePollTemplateAssetChange) {
			throw new BIException("模版资产变更提醒已禁用！");
		}

		if (StrUtil.isEmpty(templateAssetUpdatedReq.getUpdatedTime())) {
			return result;
		}

		String tplId = templateAssetUpdatedReq.getTplId();

		//是否资产变更
		Integer isAssetChange = Enabled.NO.getId();

		//1 先判断模版资产是否更新
		//先从redis获取
		String keyPrefix = BIConsts.TEMPLATE_ASSET_UPDATETIME_REDIS_KEY;
		String updateTime = "";
		try {
			RedisCacheClient<String, Object> redisCacheClient = RedisCacheManager.getRedisClient();
			updateTime = redisCacheClient.hget(keyPrefix, tplId);

		} catch (KVException e) {
			e.printStackTrace();
			//redis报错，兜底从db获取
			updateTime = templateChangeLogService.getTemplateLastChangeTime(tplId);
		}

		if (StrUtil.isEmpty(updateTime)) {
			return result;
		}

		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
		LocalDateTime dateTime1 = LocalDateTime.parse(updateTime, formatter);
		LocalDateTime dateTime2 = LocalDateTime.parse(templateAssetUpdatedReq.getUpdatedTime(), formatter);

		if (dateTime1.compareTo(dateTime2) > 0) {
			isAssetChange = Enabled.YES.getId();
		}

		//2.如果变更则返回最新模板资产信息
		if (Enabled.value(isAssetChange)) {
			result = getTemplateCfgDtlByTplId(tplId);
			result.setIsAssetChange(isAssetChange);
		}

		return result;
	}

	/**
	 * 获取模版配置的资产信息
	 * @param tplId
	 * @return
	 */
	public SSDQueryTemplate getTemplateCfgDtlByTplId(String tplId) {

		SSDQueryTemplate tpl = (SSDQueryTemplate)dao.queryObject("ssm.template.queryTemplateUpdateInfoById", tplId);
		if (tpl == null) {
			throw new SSDException(String.format("模版%s不存在！", tplId));
		}

		TemplateCfgDtlEntity templateCfgDtlEntity = (TemplateCfgDtlEntity) dao.queryObject("ssm.query.template.cfg.dtl.getByTplId", tplId);
		if (templateCfgDtlEntity != null) {

			tpl.setTplConfigFieldDimAsset(templateCfgDtlEntity.getTplConfigFieldDimAsset());
			tpl.setTplConfigFieldMeasureAsset(templateCfgDtlEntity.getTplConfigFieldMeasureAsset());
			tpl.setTplConfigFieldDimCodes(templateCfgDtlEntity.getTplConfigFieldDimCodes());
			tpl.setTplConfigFieldMeasureCodes(templateCfgDtlEntity.getTplConfigFieldMeasureCodes());

		}

		return tpl;
	}

	/**
	 * 获取模版基础信息
	 * @param templateId
	 * @return
	 */
	public SSDQueryTemplate queryTemplateById(String templateId) {
		Map<String, String> queryMap = new HashMap<>();
		queryMap.put("templateId", templateId);
		SSDQueryTemplate tpl = (SSDQueryTemplate) dao.queryObject("ssm.template.queryTemplateById", queryMap);

		if (tpl == null) {
			throw new SSDException(String.format("模版%s不存在！", templateId));
		}

		return tpl;
	}

	/**
	 * 判断当前用户是否是快照模板原模板的 owner
	 * 通过 tplId 查 root_tpl_id，再判断当前用户是否在原模板的 owner 用户集中
	 * @param tplId 快照模板ID
	 * @return 1=是 0=否
	 */
	public Integer isRootTplOwner(String tplId) {
		String rootTplId = (String) dao.queryObject("ssm.template.queryRootTplIdByTargetTplId", tplId);
		if (StrUtil.isEmpty(rootTplId)) {
			return Enabled.NO.getId();
		}
		TemplateOwnerRsp ownerRsp = getTemplateOwnerByTplId(rootTplId);
		String currentUser = UserManager.get().getName();
		boolean isOwner = ownerRsp.getTplOwnerList().stream().anyMatch(u -> currentUser.equalsIgnoreCase(u.getUserName()))
				|| ownerRsp.getSpaceOwnerList().stream().anyMatch(u -> currentUser.equalsIgnoreCase(u.getUserName()));
		return isOwner ? Enabled.YES.getId() : Enabled.NO.getId();
	}

	/**
	 * 获取模版的负责人
	 * @param tplId
	 * @return
	 */
	public TemplateOwnerRsp getTemplateOwnerByTplId(String tplId) {

		TemplateOwnerRsp templateOwnerRsp = new TemplateOwnerRsp();

		List<String> userNameList = new ArrayList<>();
		//模版owner
		List<String> tplOwnerList = new ArrayList<>();
		//共享空间owner
		List<String> spaceOwnerList = new ArrayList<>();

		SSDQueryTemplate tpl = queryTemplateById(tplId);
		templateOwnerRsp.setIsSpaceTpl(tpl.getIsSpaceTpl());

		//查询模板全路径
		String tplCtgFullPath = (String)dao.queryObject("ssm.template.queryCtgFullPathByCtgId", tpl.getCtgId());
		templateOwnerRsp.setTemplateCtgPath(tplCtgFullPath);

		if (StrUtil.isNotEmpty(tpl.getTplOwner())) {
			tplOwnerList = Arrays.asList(tpl.getTplOwner().split(","));
			userNameList.addAll(tplOwnerList);
		}

		//如果是共享空间，需要查询共享空间的管理员
		if (Enabled.value(tpl.getIsSpaceTpl())) {

			//查询共享空间的目录
			TemplateCtgPathEntity templateCtgPathEntity = (TemplateCtgPathEntity) dao.queryObject("ssm.template.ctg.getCtgPathByCtgId", tpl.getCtgId());
			String spaceCtgName = StringUtils.substring(templateCtgPathEntity.getCtgNamePath(), QueryTemplateCategoryType.SPACE.getDesc().length() + 1);
			spaceCtgName = StringUtils.substringBefore(spaceCtgName, "/");

			String spaceCtgId = StringUtils.substring(templateCtgPathEntity.getCtgIdPath(), QueryTemplateCategoryType.SPACE.getId().length() + 1);
			spaceCtgId = StringUtils.substringBefore(spaceCtgId, "/");

			TemplateSpaceService templateSpaceService = (TemplateSpaceService) SpringContextUtil.getBean("templateSpaceService");
			TemplateSpaceDetailRsp spaceDetailRsp = templateSpaceService.spaceDetail(spaceCtgId);

			spaceOwnerList = spaceDetailRsp.getUserList()
					.stream()
					.filter(v -> TemplateSpaceRoleType.ADMIN == TemplateSpaceRoleType.get(v.getOwnerRole()))
					.map(v -> v.getUserName()).collect(Collectors.toList());

			if (CollUtil.isNotEmpty(spaceOwnerList)) {
				userNameList.addAll(spaceOwnerList);
			}

			templateOwnerRsp.setSpaceName(spaceCtgName);

		}

		//获取用户名、部门等信息
		if (CollUtil.isNotEmpty(userNameList)) {

			List<UserRsp> userList = (List<UserRsp>) dao.queryObjectList("user.batchQueryUser", userNameList);
			Map<String, UserRsp> userMap = userList.stream().collect(Collectors.toMap(UserRsp::getUserName, v -> v));

			//设置模板owner
			for (String userName : tplOwnerList) {
				UserRsp user = userMap.get(userName);
				if (user == null) {
					continue;
				}
				templateOwnerRsp.getTplOwnerList().add(userMap.get(userName));
			}

			//设置空间owner
			for (String userName : spaceOwnerList) {
				UserRsp user = userMap.get(userName);
				if (user == null) {
					continue;
				}
				templateOwnerRsp.getSpaceOwnerList().add(userMap.get(userName));
			}

		}

		return templateOwnerRsp;
	}

	/**
	 * 获取模板owner，只返回用户名
	 * @param tplId
	 * @return
	 */
	public List<String> getTemplateOwnerNameByTplId(String tplId) {

		List<String> ownerNameList = new ArrayList<>();

		TemplateOwnerRsp templateOwnerRsp = getTemplateOwnerByTplId(tplId);

		if (CollUtil.isNotEmpty(templateOwnerRsp.getTplOwnerList())) {
			List<String> tplOwnerList = templateOwnerRsp.getTplOwnerList().stream().map(UserRsp::getUserName).collect(Collectors.toList());
			if (CollUtil.isNotEmpty(tplOwnerList)) {
				ownerNameList.addAll(tplOwnerList);
			}
		}

		if (CollUtil.isNotEmpty(templateOwnerRsp.getSpaceOwnerList())) {
			List<String> spaceOwnerList = templateOwnerRsp.getSpaceOwnerList().stream().map(UserRsp::getUserName).collect(Collectors.toList());
			if (CollUtil.isNotEmpty(spaceOwnerList)) {
				ownerNameList.addAll(spaceOwnerList);
			}
		}

		//去重
		ownerNameList = ownerNameList.stream().distinct().collect(Collectors.toList());
		return ownerNameList;
	}

}
