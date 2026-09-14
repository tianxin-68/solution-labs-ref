package com.bi.queryer.ssm.mgr.fieldCtg;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import com.bi.queryer.ssm.enums.CategoryType;
import com.bi.queryer.ssm.enums.DataAuthItemType;
import com.bi.queryer.ssm.enums.OwnerType;
import com.bi.queryer.ssm.meta.*;
import com.bi.queryer.ssm.mgr.dataset.model.DatasetRsq;
import com.bi.queryer.ssm.mgr.fieldCtg.model.*;
import com.bi.queryer.ssm.query.SSDQueryService;
import com.bi.queryer.ssm.util.SSDUtil;
import com.bi.queryer.sys.base.AbstractTransaction;
import com.bi.queryer.sys.base.BaseDao;
import com.bi.queryer.sys.common.KeyValuePair;
import com.bi.queryer.sys.common.ResponseMessage;
import com.bi.queryer.sys.common.SSMResponseMessage;
import com.bi.queryer.sys.config.SC;
import com.bi.queryer.sys.db.DataSourceType;
import com.bi.queryer.sys.dim.vo.DataAuth;
import com.bi.queryer.sys.enums.Enabled;
import com.bi.queryer.sys.user.UserManager;
import com.bi.queryer.sys.user.vo.User;
import com.bi.queryer.util.*;
import com.bi.queryer.util.component.datagrid.DataGrid;
import com.bi.queryer.util.component.datagrid.DataGridColumn;
import com.bi.queryer.util.component.datagrid.IDataGridDataSetProvider;
import com.bi.queryer.util.excelUtil.ExcelHelper;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.PageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletResponse;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author contributor
 */
@Service
public class FieldCtgService implements IDataGridDataSetProvider {

	@Autowired
	private BaseDao dao;

	public ResponseMessage buildFieldCtgTree(String type, String datasetId) {

		ResponseMessage result = new ResponseMessage();

		DataSourceType dataEnvDataSourceType = SSDUtil.getDataEnvDataSourceType();
		List<MetaFieldCategory> list = (List<MetaFieldCategory>) dao.queryObjectList("fieldCtg.queryFieldCtgByType", type,dataEnvDataSourceType);

		//目录附加继承信息
		Map<String, Integer> inheritMap = queryAllInherit();
		if (CollUtil.isNotEmpty(list)) {
			for (MetaFieldCategory mfc : list) {
				Integer IsInherited = inheritMap.get(mfc.getId());
				if (IsInherited != null) {
					mfc.setIsInherited(IsInherited);
				}
			}
		}

		//设置数据集
		List<DatasetRsq> datasetRsqList = (List<DatasetRsq>) dao.queryObjectList("ssm.dataset.listAll", null,dataEnvDataSourceType);

		JSONArray data = new JSONArray();

		list = bulidFieldCtgTree(list);

		CategoryType categoryType = CategoryType.get(type);
		if (CategoryType.Front == categoryType && CollUtil.isNotEmpty(datasetRsqList)) {

			List<SSMDatasetCtgRel> ssmDatasetCtgRelList = (List<SSMDatasetCtgRel>) dao.queryObjectList("ssm.dataset.queryAllDatasetCtgRel", null,dataEnvDataSourceType);

			Map<String, List<String>> datasetCtgMap = new HashMap<>();
			for (SSMDatasetCtgRel datasetCtgRel : ssmDatasetCtgRelList) {
				List<String> ctgList = datasetCtgMap.get(datasetCtgRel.getDatasetId());
				if (ctgList == null) {
					ctgList = new ArrayList<>();
				}
				ctgList.add(datasetCtgRel.getCtgId());
				datasetCtgMap.put(datasetCtgRel.getDatasetId(), ctgList);
			}

			for (DatasetRsq datasetRsq : datasetRsqList) {

				//过滤数据集
				if (StrUtil.isNotEmpty(datasetId) && !datasetId.equalsIgnoreCase(datasetRsq.getDatasetId())) {
					continue;
				}

				List<MetaFieldCategory> datasetChildList = list.stream().filter(a -> StrUtil.isNotEmpty(a.getDatasetId()) && a.getDatasetId().contains(datasetRsq.getDatasetId()))
						.collect(Collectors.toList());

				JSONObject datasetJson = new JSONObject();
				datasetJson.put("id", datasetRsq.getDatasetId());
				datasetJson.put("name", datasetRsq.getDatasetName());
				datasetJson.put("nodeType", "dataset");
				datasetJson.put("isActive", datasetRsq.getIsActive());
				datasetJson.put("children", BIUtil.toJSONArray(datasetChildList));
				data.add(datasetJson);
			}

		} else {
			//设置根节点
			JSONObject rootJSON = new JSONObject();
			rootJSON.put("id", BIConsts.Category_Root_Id);
			rootJSON.put("name", BIConsts.Category_Root_Name);
			rootJSON.put("children", BIUtil.toJSONArray(list));
			data.add(rootJSON);
		}

		result.setData(data);

		return result;
	}

	/**
	 * @param list
	 * @return
	 */
	public List<MetaFieldCategory> bulidFieldCtgTree(List<MetaFieldCategory> list) {

		List<MetaFieldCategory> trees = new ArrayList<MetaFieldCategory>();

		for (MetaFieldCategory treeNode : list) {

			if (BIConsts.Category_Root_Id.equalsIgnoreCase(treeNode.getParentId())) {
				trees.add(treeNode);
			}

			for (MetaFieldCategory it : list) {
				if (it.getParentId() != null && it.getParentId().equals(treeNode.getId())) {
					if (treeNode.getChildren() == null) {
						treeNode.setChildren(new ArrayList<MetaFieldCategory>());
					}
					treeNode.getChildren().add(it);
				}
			}
		}
		return trees;
	}

	public ResponseMessage saveFieldCtg(MetaFieldCategory metaFieldCategory) {

		ResponseMessage result = new ResponseMessage();

		User user = UserManager.get();
		DataSourceType dataEnvDataSourceType = SSDUtil.getDataEnvDataSourceType();

		try {

			//如果没有设置数据集，取默认数据集
			if (StringUtil.isEmpty(metaFieldCategory.getDatasetId())) {
				metaFieldCategory.setDatasetId(SC.v("ssm.default.datasetId", ""));
			}

			List<String> ctgIdList = ListUtil.list(true);

			//父级为空，设置为-1
			if (StrUtil.isEmpty(metaFieldCategory.getParentId())) {
				metaFieldCategory.setParentId(BIConsts.Category_Root_Id);
			}

			//序号为空，设置最大的序号加一
			if (metaFieldCategory.getShowOrder() == null) {

				Double maxXh = (double) dao.queryCount("fieldCtg.queryMaxOrderByParentId", metaFieldCategory,dataEnvDataSourceType);
				metaFieldCategory.setShowOrder(maxXh + 1);
			}

			if (BIUtil.isEmpty(metaFieldCategory.getId())) {

				metaFieldCategory.setId(Guid.id());
				metaFieldCategory.setCreatedBy(user.getName());
				dao.insert("fieldCtg.insertIntoFieldCtg", metaFieldCategory,dataEnvDataSourceType);

				//新目录默认继承
				DatasetCtgInheritEntity datasetCtgInheritEntity = new DatasetCtgInheritEntity();
				datasetCtgInheritEntity.setItemType(DataAuthItemType.CTG.getCode());
				datasetCtgInheritEntity.setItemValue(metaFieldCategory.getId());
				datasetCtgInheritEntity.setIsInherited(Enabled.YES.getId());
				datasetCtgInheritEntity.setCreatedBy(user.getName());
				dao.insert("ssm.dataset.ctg.inherit.add", datasetCtgInheritEntity,dataEnvDataSourceType);
			} else {

				//查询数据库中的目录对应的数据集信息
				SSMDatasetCtgRel ssmDatasetCtgRel = (SSMDatasetCtgRel) dao.queryObject("ssm.dataset.queryDatasetCtgRelByCtgId", metaFieldCategory.getId(),dataEnvDataSourceType);
				if (ssmDatasetCtgRel != null && !ssmDatasetCtgRel.getDatasetId().equalsIgnoreCase(metaFieldCategory.getDatasetId())) {
					//需要更新子目录的数据集id
					//获取当前目录下的所有子目录
					List<MetaFieldCategory> ctgList = (List<MetaFieldCategory>) dao.queryObjectList("fieldCtg.queryFieldCtgByType", CategoryType.Front.toString().toLowerCase(),dataEnvDataSourceType);
					ctgList = getChildrenList(ctgList, metaFieldCategory.getId());

					setCtgIdList(ctgList, ctgIdList);
				}

				metaFieldCategory.setUpdatedBy(user.getName());
				dao.update("fieldCtg.updateFieldCtg", metaFieldCategory,dataEnvDataSourceType);

			}

			//处理目录与数据集的关系
			if (StrUtil.isNotEmpty(metaFieldCategory.getDatasetId())) {

				ctgIdList.add(metaFieldCategory.getId());
				Map<String, Object> datasetCtgRelMap = new HashMap<>();
				datasetCtgRelMap.put("ctgIdList", ctgIdList);
				datasetCtgRelMap.put("datasetId", metaFieldCategory.getDatasetId());

				dao.delete("ssm.dataset.batchDeleteDatasetCtgRelByCtgId", datasetCtgRelMap,dataEnvDataSourceType);
				dao.insert("ssm.dataset.batchInsertDatasetCtgRel", datasetCtgRelMap,dataEnvDataSourceType);
			}

			// 处理内测人员的目录赋权数据
			dao.delete("authority.deleteCtgDataAuth", metaFieldCategory);
			if (StrUtil.isNotEmpty(metaFieldCategory.getModuleBetaMember())) {
				String[] betaMembers = metaFieldCategory.getModuleBetaMember().split(",");
				List<DataAuth> dataAuthList = ListUtil.list(false);
				for (String betaMember : betaMembers) {
					DataAuth dataAuth = new DataAuth();
					dataAuth.setAuthId(BIConsts.Category_Manual_Auth_Prefix + Guid.id());
					dataAuth.setOwnerType("User");
					dataAuth.setOwnerId(betaMember.replace("@example.com", ""));
					dataAuth.setModuleCode("ssd_ctg");
					dataAuth.setModuleName("自助取数_目录");
					dataAuth.setDimCode("ssm_ctg");
					dataAuth.setDimName("多维分析-目录权限");
					dataAuth.setItemCode(metaFieldCategory.getId());
					dataAuth.setItemValue(metaFieldCategory.getName());
					dataAuth.setDimDisplayName("【自助取数_目录/多维分析-目录权限】" + metaFieldCategory.getName());
					dataAuth.setRemark("多维分析后台管理目录owner赋权给内测人员");
					dataAuth.setCreatedBy(user.getName());
					dataAuthList.add(dataAuth);
				}
				dao.insert("authority.insertCtgDataAuth", dataAuthList);
			}
		} catch (Exception e) {
			return new ResponseMessage(false, e.getMessage());
		}

		return result;
	}

	public ResponseMessage deleteFieldCtgById(String id) {

		ResponseMessage result = new ResponseMessage();

		try {

			DataSourceType dataEnvDataSourceType = SSDUtil.getDataEnvDataSourceType();
			dao.executeTranscation(dataEnvDataSourceType, new AbstractTransaction() {
				@Override
				public void execute() {
					deleteFieldCtgRecursion(id);
				}
			});


		} catch (Exception e) {
			return new ResponseMessage(false, e.getMessage());
		}

		return result;
	}

	/**
	 * 递归删除
	 * @param id
	 */
	public void deleteFieldCtgRecursion(String id) {

		DataSourceType dataEnvDataSourceType = SSDUtil.getDataEnvDataSourceType();

		List<MetaFieldCategory> list = (List<MetaFieldCategory>) dao.queryObjectList("fieldCtg.queryChildFieldCtgByParentId", id,dataEnvDataSourceType);
		dao.delete("fieldCtg.deleteFieldCtgById", id,dataEnvDataSourceType);
		dao.delete("ssm.dataset.deleteDatasetCtgRelByCtgId", id,dataEnvDataSourceType);

		dao.delete("ssm.field.ctg.rel.deleteById",id,dataEnvDataSourceType);

		DatasetCtgInheritEntity datasetCtgInheritEntity = new DatasetCtgInheritEntity();
		datasetCtgInheritEntity.setItemType(DataAuthItemType.CTG.getCode());
		datasetCtgInheritEntity.setItemValue(id);
		dao.delete("ssm.dataset.ctg.inherit.delete", datasetCtgInheritEntity,dataEnvDataSourceType);

		if (BIUtil.isNotEmpty(list)) {
			for (MetaFieldCategory metaFieldCategory : list) {
				deleteFieldCtgRecursion(metaFieldCategory.getId());
			}
		}

	}


	/**
	 * 构建datagrid
	 */
	public DataGrid buildDataGrid(Map<String, String> paramMap) {
		DataGrid grid = new DataGrid(-1, -1);
		String width = paramMap.get("width");
		String height = paramMap.get("height");
		if (!StringUtil.isEmpty(width) && !StringUtil.isEmpty(height)) {
			grid.setWidth(Integer.valueOf(width));
			grid.setHeight(Integer.valueOf(height));
		}
		grid.setAutoSize(true);
		grid.setShowExport(false);
		grid.setFormatLink(false);
		grid.setPagination(true);// 使用分页
		grid.setShowHideColumnButton(false);


		DataGridColumn column = new DataGridColumn("FIELDID", "id", -1); // 添加列
		column.setHidden(true); // id不需要显示
		grid.addColumn(column);

		column = new DataGridColumn("CTGNAME", "分类目录", -1, "left");
		grid.addColumn(column);

		column = new DataGridColumn("TABLENAME", "所属表", -1, "left"); // 添加列
		grid.addColumn(column);

		column = new DataGridColumn("FIELDTITLE", "字段标题", -1, "left");
		grid.addColumn(column);

		column = new DataGridColumn("FIELDNAME", "字段名", -1, "left");
		grid.addColumn(column);

		column = new DataGridColumn("FIELDCODE", "字段编码", -1, "left");
		grid.addColumn(column);

		column = new DataGridColumn("RPTDEVOWNER", "分析师", -1, "left");
		grid.addColumn(column);

		column = new DataGridColumn("OVERSIZED", "是否含有超大表", -1, "left");
		grid.addColumn(column);

		column = new DataGridColumn("DATADEVOWNER", "开发负责人", -1, "left");
		grid.addColumn(column);

		column = new DataGridColumn("operator", "操作");
		column.setOperator(true);
		column.addOperation("删除", "deleteRow");

		column.setFrozen(true);
		column.setCanHide(false);
		column.setWidth(160);
		grid.addColumn(column);

		grid.setDataSetProvider(this, paramMap);
		return grid;
	}

	@Override
	public List<MetaField> getDataSet(Map queryParamMap) {
		buidQueryParam(queryParamMap);
		DataSourceType dataEnvDataSourceType = SSDUtil.getDataEnvDataSourceType();
		return dao.queryMapList("fieldCtg.queryCtgFieldList", queryParamMap,dataEnvDataSourceType);
	}

	protected void buidQueryParam(Map paramMap) {

		DataSourceType dataEnvDataSourceType = SSDUtil.getDataEnvDataSourceType();
		//通过目录id，找目录全路径id
		String ctgId = BIUtil.nvl(paramMap.get("ctgId") ,"") ;
		if(StrUtil.isNotEmpty(ctgId)){
			List<String> ctgIdPath = (List<String>)dao.queryObjectList("fieldCtg.queryCtgIdPathByCtgId",ctgId,dataEnvDataSourceType);
			if(ctgIdPath == null){
				ctgIdPath = new ArrayList<>();
			}

			ctgIdPath.add(ctgId);
			paramMap.put("ctgIdList",ctgIdPath);
		}

	}

	@Override
	public Integer getDataSetTotalSize(Map queryParamMap) {
		buidQueryParam(queryParamMap);
		DataSourceType dataEnvDataSourceType = SSDUtil.getDataEnvDataSourceType();
		return dao.queryCount("fieldCtg.queryCtgFieldListCount", queryParamMap,dataEnvDataSourceType);
	}

	public ResponseMessage saveField(Map<String, Object> map) {

		ResponseMessage result = new ResponseMessage();
		DataSourceType dataEnvDataSourceType = SSDUtil.getDataEnvDataSourceType();

		try {

			String ctgType = map.get("type") + "";
			CategoryType categoryType = CategoryType.get(ctgType);
			String ctgId = map.get("ctgId") + "";
			List<String> fieldIds = (List<String>) map.get("fieldIds");

			String userName = UserManager.get().getName();

			switch (categoryType) {
				//后台目录 先删后插
				case Back:

					dao.executeTranscation(dataEnvDataSourceType, new AbstractTransaction() {
						@Override
						public void execute() {
							//1 先删除该目录关联的字段
							dao.delete("ssm.field.ctg.rel.deleteByCtgId",ctgId,dataEnvDataSourceType);

							if (BIUtil.isNotEmpty(fieldIds)) {
								dao.delete("ssm.field.ctg.rel.deleteByFieldIds",map,dataEnvDataSourceType);

								List<FieldCtgRelEntity> fieldCtgRelList = new ArrayList<>();
								for(String fieldId : fieldIds){
									FieldCtgRelEntity fieldCtgRelEntity = FieldCtgRelEntity.builder()
											.fieldId(fieldId)
											.ctgType(categoryType.toString().toLowerCase())
											.ctgId(ctgId)
											.isActive(Enabled.YES.getId())
											.createdBy(userName)
											.build();
									fieldCtgRelList.add(fieldCtgRelEntity);
								}

								map.put("fieldCtgRelList",fieldCtgRelList);

								//2 保存该目录
								dao.insert("ssm.field.ctg.rel.insert", map,dataEnvDataSourceType);
							}

						}
					});

					break;
				case Front:
					saveFrontCtgField(ctgId, fieldIds);
					break;
			}


		} catch (Exception e) {
			return new ResponseMessage(false, e.getMessage());
		}

		return result;

	}

	/**
	 * 保存前端目录字段
	 * @param ctgId
	 * @param fieldIds
	 */
	public void saveFrontCtgField(String ctgId,List<String> fieldIds){

		DataSourceType dataEnvDataSourceType = SSDUtil.getDataEnvDataSourceType();

		//数据库中不存在，前端传入的字段集合中存在 执行insert
		List<String> insertFieldIdList = new ArrayList<>();

		// 数据库中存在，前端传入的字段集合中存在，但数据库中is_active = 0 执行update is_active = 1
		List<String> activeFieldIdList = new ArrayList<>();

		//数据库中存在，前端传入的集合中不存在 执行update is_active = 0
		List<String> disableFieldIdList = new ArrayList<>();

		//查询目录下已有的字段
		List<FieldCtgRelEntity> fieldCtgRelEntityList = (List<FieldCtgRelEntity>)dao.queryObjectList("ssm.field.ctg.rel.selectByCtgId",ctgId,dataEnvDataSourceType);

		if(CollUtil.isEmpty(fieldCtgRelEntityList)){
			insertFieldIdList.addAll(fieldIds);
		}else{

			if(CollUtil.isEmpty(fieldIds)){
				disableFieldIdList = fieldCtgRelEntityList.stream().map(FieldCtgRelEntity::getFieldId).collect(Collectors.toList());
			}else{

				//方便寻找 前端传入的字段是不是在数据库中存在
				Map<String,String> dbfieldIdMap = new HashMap<>();
				for(FieldCtgRelEntity relEntity : fieldCtgRelEntityList){

					String fieldId = relEntity.getFieldId();
					dbfieldIdMap.put(fieldId,"");
					if(fieldIds.contains(fieldId)){

						//已存在有效的关联关系，不处理
						if (Enabled.value(relEntity.getIsActive())) {
							continue;
						}

						activeFieldIdList.add(fieldId);

					}else{
						disableFieldIdList.add(fieldId);
					}

				}

				for(String fieldId : fieldIds){

					if(dbfieldIdMap.containsKey(fieldId)){
						continue;
					}

					insertFieldIdList.add(fieldId);

				}

			}
		}

		String userName = UserManager.get().getName();
		Map<String,Object> paramMap = new HashMap<>();
		paramMap.put("ctgId",ctgId);
		paramMap.put("updatedBy",userName);

		//入库
		if(CollUtil.isNotEmpty(insertFieldIdList)){
			List<FieldCtgRelEntity> fieldCtgRelList = new ArrayList<>();
			for(String fieldId : insertFieldIdList) {
				FieldCtgRelEntity fieldCtgRelEntity = FieldCtgRelEntity.builder()
						.fieldId(fieldId)
						.ctgType(CategoryType.Front.toString().toLowerCase())
						.ctgId(ctgId)
						.isActive(Enabled.YES.getId())
						.createdBy(userName)
						.build();
				fieldCtgRelList.add(fieldCtgRelEntity);
			}
			paramMap.put("fieldCtgRelList",fieldCtgRelList);
			dao.insert("ssm.field.ctg.rel.insert", paramMap,dataEnvDataSourceType);
		}

		if(CollUtil.isNotEmpty(activeFieldIdList)){
			paramMap.put("activeFieldIdList",activeFieldIdList);
			dao.update("ssm.field.ctg.rel.activeByMap",paramMap,dataEnvDataSourceType);
		}

		if(CollUtil.isNotEmpty(disableFieldIdList)){
			paramMap.put("disableFieldIdList",disableFieldIdList);
			dao.update("ssm.field.ctg.rel.disableByMap",paramMap,dataEnvDataSourceType);
		}

	}

	public ResponseMessage getFieldByCtgId(Map<String, Object> map) {

		ResponseMessage result = new ResponseMessage();

		DataSourceType dataEnvDataSourceType = SSDUtil.getDataEnvDataSourceType();
		List<String> fieldIds = (List<String>) dao.queryObjectList("fieldCtg.getFieldByCtgId", map,dataEnvDataSourceType);
		result.setData(fieldIds);

		return result;
	}

	public ResponseMessage deleteFieldCtgAssociate(Map<String, Object> map) {

		ResponseMessage result = new ResponseMessage();
		DataSourceType dataEnvDataSourceType = SSDUtil.getDataEnvDataSourceType();

		try {

			String ctgId = BIUtil.nvl(map.get("ctgId") ,"");
			if(StrUtil.isEmpty(ctgId)){
				return new ResponseMessage(false, "目录id必传,请刷新页面后重试！");
			}

			map.put("updatedBy",UserManager.get().getName());
			dao.update("fieldCtg.deleteFieldCtgAssociate", map,dataEnvDataSourceType);
		} catch (Exception e) {
			return new ResponseMessage(false, e.getMessage());
		}

		return result;
	}


	public void exportMethod(String searchTxt,String ctgType, String ctgId,String type, HttpServletResponse response) {

		Map<String, Object> queryParamMap = new HashMap<>();
		queryParamMap.put("searchTxt", searchTxt);
		queryParamMap.put("ctgId", ctgId);
		queryParamMap.put("type", ctgType);

		buidQueryParam(queryParamMap);

		DataSourceType dataEnvDataSourceType = SSDUtil.getDataEnvDataSourceType();
		List<FieldCtgExportEntity> fieldCtgExportEntityList = (List<FieldCtgExportEntity>)dao.queryObjectList("fieldCtg.queryExportCtgFieldList", queryParamMap,dataEnvDataSourceType);

		String[] cloumnNames = new String[]{
				"目录id", "目录名称", "目录路径", "字段id", "字段名",
				"字段编码", "字段标题", "所属表id","所属表名", "表owner"
		};

		List<String[]> cloumnValues = new ArrayList<>();

		if (CollUtil.isNotEmpty(fieldCtgExportEntityList)) {

			for (FieldCtgExportEntity fieldCtgExportEntity : fieldCtgExportEntityList) {

				String[] strs = new String[]{
						fieldCtgExportEntity.getCtgId(),
						fieldCtgExportEntity.getCtgName(),
						fieldCtgExportEntity.getCtgPath(),
						fieldCtgExportEntity.getFieldId(),
						fieldCtgExportEntity.getFieldName(),
						fieldCtgExportEntity.getFieldCode(),
						fieldCtgExportEntity.getFieldTitle(),
						fieldCtgExportEntity.getTableId(),
						fieldCtgExportEntity.getTableName(),
						fieldCtgExportEntity.getTableOwner()
				};
				cloumnValues.add(strs);
			}
		}

		ExcelHelper.exportExcel(cloumnNames, cloumnValues, type, response);

	}


	/**
	 * @param req
	 */
	public ResponseMessage queryFieldCtgAuthSetting(CtgAuthDetailReq req) {

		DataSourceType dataEnvDataSourceType = SSDUtil.getDataEnvDataSourceType();

		//1、获取当前目录下的所有子目录
		List<MetaFieldCategory> ctgList = (List<MetaFieldCategory>) dao.queryObjectList("fieldCtg.queryFieldCtgByType", "front",dataEnvDataSourceType);
		ctgList = getChildrenList(ctgList, req.getCtgId());
		List<String> ctgIdList = ListUtil.list(true);
		setCtgIdList(ctgList, ctgIdList);
		//2、查询子目录下的所有字段关联业务线及渠道的数量，数量为0的界面不展示配置项
		Map<String, Object> queryMap = new HashMap<>();
		queryMap.put("ctgIdList", ctgIdList);
		queryMap.put("dimCodeList", req.getDimCodeList());
		List<CtgAuthDetailRsp> ctgAuthShowList = (List<CtgAuthDetailRsp>) dao.queryObjectList("fieldCtg.queryColumnRelatedCount", queryMap,dataEnvDataSourceType);
		Map<String, Boolean> ctgAuthShowMap = ctgAuthShowList.stream().collect(Collectors.toMap(CtgAuthDetailRsp::getDimCode, CtgAuthDetailRsp::getIsShow));
		//3、查询已配置的数据权限列表
		List<CtgNeedAuthEntity> authList = (List<CtgNeedAuthEntity>) dao.queryObjectList("fieldCtg.queryCtgNeedAuthById", req.getCtgId(),dataEnvDataSourceType);
		//4、组装返回结果
		Map<String, CtgAuthDetailRsp> resultMap = MapUtil.newHashMap();
		req.getDimCodeList().forEach(item -> {
			CtgAuthDetailRsp detailRsp = new CtgAuthDetailRsp();
			detailRsp.setDimCode(item);
			if (ctgAuthShowMap.get(item) != null) {
				detailRsp.setIsShow(ctgAuthShowMap.get(item));
			}
			if (detailRsp.getIsShow()) {
				detailRsp.setItemList(authList.stream().filter(m -> item.equals(m.getDimCode())).map(k -> k.getItemCode()).collect(Collectors.toList()));
			}
			resultMap.put(item, detailRsp);
		});
		return new ResponseMessage(resultMap);
	}

	public List<MetaFieldCategory> getChildrenList(List<MetaFieldCategory> ctgList, String id) {

		MetaFieldCategory ctgNode = null;
		for (MetaFieldCategory treeNode : ctgList) {
			if (!StringUtil.isEmpty(id) && id.equals(treeNode.getId())) {
				ctgNode = treeNode;
			}
			ctgList.forEach(ctg -> {
				if (ctg.getParentId() != null && ctg.getParentId().equals(treeNode.getId())) {
					if (treeNode.getChildren() == null) {
						treeNode.setChildren(new ArrayList<MetaFieldCategory>());
					}
					treeNode.getChildren().add(ctg);
				}
			});
		}
		return ctgNode == null ? ListUtil.list(true) : ctgNode.getChildren();
	}

	private void setCtgIdList(List<MetaFieldCategory> children, List<String> ctgIdList) {
		children.forEach(item -> {
			ctgIdList.add(item.getId());
			if (CollectionUtil.isNotEmpty(item.getChildren())) {
				setCtgIdList(item.getChildren(), ctgIdList);
			}
		});
	}

	/**
	 * 先删除所有配置，再插入新的配置
	 * @param req
	 * @return
	 */
	@Transactional
	public ResponseMessage saveCtgAuthSetting(CtgAuthSaveReq req) {

		DataSourceType dataEnvDataSourceType = SSDUtil.getDataEnvDataSourceType();

		try {
			// 删除目录配置的权限数据
			dao.delete("fieldCtg.deleteCtgNeedAuthById", req.getCtgId(),dataEnvDataSourceType);
			// 构建批量插入数据，批量插入
			List<CtgNeedAuthEntity> entityList = ListUtil.list(false);
			User user = UserManager.get();
			req.getAuthList().forEach(item -> {
				item.setCtgId(req.getCtgId());
				item.setCreatedTime(new Date());
				item.setCreatedBy(user.getName());
				entityList.add(item);
			});
			if (CollectionUtil.isNotEmpty(entityList)) {
				dao.insert("fieldCtg.insertCtgNeedAuth", entityList,dataEnvDataSourceType);
			}
		} catch (Exception e) {
			return new ResponseMessage(false, e.getMessage());
		}
		return new ResponseMessage();
	}

	/*获取权限申请的目录列表*/
	public ResponseMessage queryAuthApplyCtgList(CtgListReq req) {
		// 从缓存获取目录所有数据
		Map<String, MetaFieldCategory> categoryMap = SSDMetaCacheManager.getFrontCategories();
		List<MetaFieldCategory> allFrontCategories = categoryMap.values().stream()
				.filter(category -> BIConsts.Category_Root_Id.equalsIgnoreCase(category.getParentId()))
				.collect(Collectors.toList());
		//获取权限申请目录数据
		List<MetaFieldCategory> treeData = getAuthCtgList(allFrontCategories);
		if (StrUtil.isNotEmpty(req.getDatasetId())) {
			treeData = treeData.stream().filter(category -> req.getDatasetId().contains(category.getDatasetId())).collect(Collectors.toList());
		}

		Collections.sort(treeData);

//		CollectionUtil.sort(treeData, new Comparator<MetaFieldCategory>() {
//			@Override
//			public int compare(MetaFieldCategory o1, MetaFieldCategory o2) {
//
//				if (o1.getShowOrder() == null) {
//					return -1;
//				}
//				if (o2.getShowOrder() == null) {
//					return 1;
//				}
//				return o1.getShowOrder() > o2.getShowOrder() ? 1 : -1;
//			}
//		});
		return new ResponseMessage(treeData);
	}

	private List<MetaFieldCategory> getAuthCtgList(List<MetaFieldCategory> allFrontCategories) {
		List<MetaFieldCategory> resultList = ListUtil.list(false);
		allFrontCategories.forEach(item -> {
			if (item.getIsModule() != null && item.getIsModule() == 1) {
				MetaFieldCategory category = BeanUtil.copyProperties(item, MetaFieldCategory.class);
				category.setParent(null);
				if (Objects.equals("公共维度", category.getName())) {
					MetaDataset dataset = SSDMetaCacheManager.getDataset(item.getDatasetId());
					if (dataset != null) {
						category.setName(category.getName() + "(" + dataset.getDatasetName() + ")");
					}
				}
				resultList.add(category);
				if (CollectionUtil.isNotEmpty(category.getChildren())) {
					category.setChildren(getAuthCtgList(category.getChildren()));
				}
			}
		});
		return resultList;
	}

	public ResponseMessage getModuleDescList(Integer pageNum, Integer pageSize) {

		DataSourceType dataEnvDataSourceType = SSDUtil.getDataEnvDataSourceType();

		Map paramMap = MapUtil.newHashMap();
		paramMap.put("startNum", (pageNum - 1) * pageSize);
		paramMap.put("pageSize", pageSize);
		List<MetaFieldCategory> list = (List<MetaFieldCategory>) dao.queryObjectList("fieldCtg.queryModuleDescList", paramMap,dataEnvDataSourceType);
		PageInfo<MetaFieldCategory> pageInfo = new PageInfo<>(list);
		Integer total = dao.queryCount("fieldCtg.queryModuleDescListCount", paramMap,dataEnvDataSourceType);
		pageInfo.setTotal(total);
		return new ResponseMessage(pageInfo);
	}

	// 判断当前目录下是否设置了dimCode对应的关联字段，根据parentId循环匹配，直到所有的parentId都为-1，为避免脏数据导致循环死查询，设置最大循环次数为5
    /*private void dealAuthFieldData(List<Map<String, String>> authFieldList, Map<String, Map<String, Boolean>> authFieldMap, int queryNum) {
        Iterator<Map<String, String>> iterator = authFieldList.iterator();
        while (iterator.hasNext()) {
            Map<String, String> item = iterator.next();
            String ctgId = item.get("ctgId");
            String dimCode = item.get("dimCode");
            if (authFieldMap.get(ctgId) != null) {
                authFieldMap.get(ctgId).put(dimCode, true);
                iterator.remove();
            }
        }
        if (queryNum >= 5||CollectionUtil.isEmpty(authFieldList)) {
            return;
        }
        List<Map<String, String>> authFieldParentList = (List<Map<String, String>>) dao.queryObjectList("fieldCtg.queryAuthFieldParentList", authFieldList);
        if (CollectionUtil.isNotEmpty(authFieldParentList)) {
            queryNum++;
            dealAuthFieldData(authFieldParentList, authFieldMap, queryNum);
        }
    }*/

    /*private Map<String, Map<String, List<String>>> buildCtgAuthNeedData(List<CtgNeedAuthEntity> ctgNeedAuthList, Map<String, MetaFieldCategory> categoryMap) {
        Map<String, Map<String, List<String>>> ctgNeedAuthResult = MapUtil.newHashMap();
        ctgNeedAuthList.forEach(item -> {
            MetaFieldCategory category = categoryMap.get(item.getCtgId());
            if (category != null && hasDimRelatedField(category, item.getDimCode())) {
                Map<String, List<String>> resultMap = ctgNeedAuthResult.get(item.getCtgId());
                if (resultMap == null) {
                    resultMap = MapUtil.newHashMap();
                    ctgNeedAuthResult.put(item.getCtgId(), resultMap);
                }
                List<String> resultList = resultMap.get(item.getDimCode());
                if (resultList == null) {
                    resultList = ListUtil.list(false);
                    resultMap.put(item.getDimCode(), resultList);
                }
                resultList.add(item.getItemCode());
            }
        });
        return ctgNeedAuthResult;
    }*/

	/**
	 * 保存数据集和目录的权限配置
	 * @param datasetCtgDataAuthSaveReq
	 * @return
	 */
	public SSMResponseMessage saveDatasetCtgDataAuth(DatasetCtgDataAuthSaveReq datasetCtgDataAuthSaveReq) {

		DataSourceType dataEnvDataSourceType = SSDUtil.getDataEnvDataSourceType();

		User user = UserManager.get();
		String userName = user.getName();

		Map<String, String> queryMap = new HashMap<>();
		queryMap.put("itemType", datasetCtgDataAuthSaveReq.getItemType());
		queryMap.put("itemValue", datasetCtgDataAuthSaveReq.getItemValue());

		//先查询权限，【全部】修改和【数据集】变动发送BI Queryer通
		List<DatasetCtgDataAuthDetailRsq> dataAuthList = (List<DatasetCtgDataAuthDetailRsq>) dao.queryObjectList("ssm.data.auth.dataset.ctg.selectByMap", queryMap,dataEnvDataSourceType);
		List<String> oldAuthList = dataAuthList.stream().map(DatasetCtgDataAuthDetailRsq::getOwnerId).collect(Collectors.toList());
		List<String> newAuthList = datasetCtgDataAuthSaveReq.getDataAuthList().stream().map(DatasetCtgDataAuthDetailReq::getOwnerId).collect(Collectors.toList());

		boolean isAuthChange = checkAuthChange(oldAuthList, newAuthList);

		dao.executeTranscation(dataEnvDataSourceType, new AbstractTransaction() {
			@Override
			public void execute() {

				Map<String, Object> map = new HashMap<>();
				map.put("authList", datasetCtgDataAuthSaveReq.getDataAuthList());
				map.put("createdBy", userName);
				map.put("itemType", datasetCtgDataAuthSaveReq.getItemType());
				map.put("itemValue", datasetCtgDataAuthSaveReq.getItemValue());

				//先删除
				dao.delete("ssm.data.auth.dataset.ctg.delete", map,dataEnvDataSourceType);

				if (CollUtil.isNotEmpty(datasetCtgDataAuthSaveReq.getDataAuthList())) {
					//保存权限配置
					dao.insert("ssm.data.auth.dataset.ctg.add", map,dataEnvDataSourceType);
				}

				//全部不处理继承
				DataAuthItemType dataAuthItemType = DataAuthItemType.get(datasetCtgDataAuthSaveReq.getItemType());
				if (DataAuthItemType.ALL != dataAuthItemType) {
					//处理权限继承关系
					DatasetCtgInheritEntity datasetCtgInheritEntity = new DatasetCtgInheritEntity();
					datasetCtgInheritEntity.setItemType(datasetCtgDataAuthSaveReq.getItemType());
					datasetCtgInheritEntity.setItemValue(datasetCtgDataAuthSaveReq.getItemValue());

					//默认继承
					if (datasetCtgDataAuthSaveReq.getIsInherited() == null) {
						datasetCtgDataAuthSaveReq.setIsInherited(Enabled.YES.getId());
					}
					datasetCtgInheritEntity.setIsInherited(datasetCtgDataAuthSaveReq.getIsInherited());
					datasetCtgInheritEntity.setCreatedBy(userName);
					datasetCtgInheritEntity.setUpdatedBy(userName);

					//判断是否存在继承关系
					Integer inheritCount = dao.queryCount("ssm.dataset.ctg.inherit.selectInheritCount", map,dataEnvDataSourceType);
					if (inheritCount > 0) {
						dao.update("ssm.dataset.ctg.inherit.update", datasetCtgInheritEntity,dataEnvDataSourceType);
					} else {
						dao.insert("ssm.dataset.ctg.inherit.add", datasetCtgInheritEntity,dataEnvDataSourceType);
					}
				}

			}
		});

		if (isAuthChange) {
			sendAuthChangeMsg(datasetCtgDataAuthSaveReq);
		}
		return SSMResponseMessage.success("保存成功");
	}

	/**
	 * 发送权限变更BI Queryer通
	 */
	public void sendAuthChangeMsg(DatasetCtgDataAuthSaveReq datasetCtgDataAuthSaveReq) {

		User user = UserManager.get();
		DataAuthItemType dataAuthItemType = DataAuthItemType.get(datasetCtgDataAuthSaveReq.getItemType());
		if (DataAuthItemType.CTG == dataAuthItemType) {
			return;
		}

		DataSourceType dataEnvDataSourceType = SSDUtil.getDataEnvDataSourceType();

		new Thread() {
			@Override
			public void run() {

				SSDQueryService ssdQueryService = (SSDQueryService) SpringContextUtil.getBean("SSDQueryService");

				String authMsg = "";
				if (CollUtil.isNotEmpty(datasetCtgDataAuthSaveReq.getDataAuthList())) {

					List<KeyValuePair> deptList = (List<KeyValuePair>) dao.queryObjectList("dept.queryAllDept", null);
					Map<String, String> deptMap = new HashMap<>();
					for (KeyValuePair kv : deptList) {
						deptMap.put(kv.getKey(), kv.getValue() + "");
					}

					List<String> authNames = new ArrayList<>();
					for (DatasetCtgDataAuthDetailReq detailReq : datasetCtgDataAuthSaveReq.getDataAuthList()) {
						OwnerType ownerType = OwnerType.get(detailReq.getOwnerType());
						switch (ownerType) {
							case USER:
								authNames.add(detailReq.getOwnerId());
								break;
							case DEPT:
								String deptName = deptMap.get(detailReq.getOwnerId());
								if (StrUtil.isNotEmpty(deptName)) {
									authNames.add(deptName);
								}
								break;
						}
					}

					if (CollUtil.isNotEmpty(authNames)) {
						authMsg += BIUtil.listToStr(authNames, "，");
					}

				} else {
					authMsg = "空";
				}

				if (DataAuthItemType.ALL == dataAuthItemType) {

					List<DatasetRsq> datasetRsqList = (List<DatasetRsq>) dao.queryObjectList("ssm.dataset.ctg.inherit.queryInheritedDataset", null,dataEnvDataSourceType);
					if (CollUtil.isNotEmpty(datasetRsqList)) {
						for (DatasetRsq datasetRsq : datasetRsqList) {
							String msg = String.format("你负责的数据集【%s】继承的全部权限配置已变更，变更为：%s，若有疑问请联系修改人%s",
									datasetRsq.getDatasetName(), authMsg, user.getName());
							List<String> users = new ArrayList<>();
							users.add(datasetRsq.getDataDevOwner());

							if (CollUtil.isNotEmpty(users)) {
								ssdQueryService.sendMsg(msg, users);
							}

						}
					}

				} else if (DataAuthItemType.DATASET == dataAuthItemType) {

					List<MetaFieldCategory> metaFieldCategoryList = (List<MetaFieldCategory>) dao.queryObjectList("ssm.dataset.ctg.inherit.queryInheritedCtgByDatasetId", datasetCtgDataAuthSaveReq.getItemValue(),dataEnvDataSourceType);
					if (CollUtil.isNotEmpty(metaFieldCategoryList)) {
						for (MetaFieldCategory metaFieldCategory : metaFieldCategoryList) {
							String msg = String.format("你负责的模块【%s】继承的数据集权限配置已变更，变更为：%s，若有疑问请联系修改人%s",
									metaFieldCategory.getName(), authMsg, user.getName());
							List<String> users = new ArrayList<>();
							users.add(metaFieldCategory.getDataDevOwner());

							if (CollUtil.isNotEmpty(users)) {
								ssdQueryService.sendMsg(msg, users);
							}
						}
					}

				}

			}
		}.start();

	}

	/**
	 * 校验权限是否改变
	 * @return
	 */
	public boolean checkAuthChange(List<String> oldAuthList, List<String> newAuthList) {
		boolean result = false;
		if (!newAuthList.equals(oldAuthList)) {
			result = true;
		}

		return result;
	}


	/**
	 * 查询权限配置
	 * @param datasetCtgDataAuthQueryReq
	 * @return
	 */
	public SSMResponseMessage<DatasetCtgDataAuthRsq> queryDatasetCtgDataAuth(DatasetCtgDataAuthQueryReq datasetCtgDataAuthQueryReq) {
		DatasetCtgDataAuthRsq datasetCtgDataAuthRsq = new DatasetCtgDataAuthRsq();
		DataSourceType dataEnvDataSourceType = SSDUtil.getDataEnvDataSourceType();

		Map<String, String> map = new HashMap<>();
		map.put("itemType", datasetCtgDataAuthQueryReq.getItemType());
		map.put("itemValue", datasetCtgDataAuthQueryReq.getItemValue());

		datasetCtgDataAuthRsq.setItemType(datasetCtgDataAuthQueryReq.getItemType());
		datasetCtgDataAuthRsq.setItemValue(datasetCtgDataAuthQueryReq.getItemValue());

		DataAuthItemType dataAuthItemType = DataAuthItemType.get(datasetCtgDataAuthQueryReq.getItemType());
		if (DataAuthItemType.ALL != dataAuthItemType) {
			DatasetCtgInheritEntity datasetCtgInheritEntity = (DatasetCtgInheritEntity) dao.queryObject("ssm.dataset.ctg.inherit.selectInheritInfo", map,dataEnvDataSourceType);

			if (datasetCtgInheritEntity != null) {
				datasetCtgDataAuthRsq.setIsInherited(datasetCtgInheritEntity.getIsInherited());
			}

		} else {
			datasetCtgDataAuthRsq.setIsInherited(Enabled.NO.getId());
		}

		//查询权限配置
		List<DatasetCtgDataAuthDetailRsq> dataAuthList = (List<DatasetCtgDataAuthDetailRsq>) dao.queryObjectList("ssm.data.auth.dataset.ctg.selectByMap", map,dataEnvDataSourceType);
		datasetCtgDataAuthRsq.setDataAuthList(dataAuthList);

		return SSMResponseMessage.success("", datasetCtgDataAuthRsq);
	}

	/**
	 * 查询继承的权限
	 * @param datasetCtgDataAuthQueryReq
	 * @return
	 */
	public SSMResponseMessage<List<DatasetCtgDataAuthInheritRsq>> queryDatasetCtgInheritedDataAuth(DatasetCtgDataAuthQueryReq datasetCtgDataAuthQueryReq) {
		List<DatasetCtgDataAuthInheritRsq> inheritList = new ArrayList<>();

		DataSourceType dataEnvDataSourceType = SSDUtil.getDataEnvDataSourceType();

		//目录附加继承信息
		Map<String, Integer> inheritMap = queryAllInherit();

		DataAuthItemType dataAuthItemType = DataAuthItemType.get(datasetCtgDataAuthQueryReq.getItemType());
		//数据集向上查询全部
		if (DataAuthItemType.DATASET == dataAuthItemType) {

			DatasetCtgDataAuthInheritRsq allIhInherit = new DatasetCtgDataAuthInheritRsq();
			allIhInherit.setInheritedName(DataAuthItemType.ALL.getName());

			List<DatasetCtgDataAuthDetailRsq> dataAuthList = queryDatasetCtgDataAuth(DataAuthItemType.ALL.getCode(), DataAuthItemType.ALL.getValue());
			allIhInherit.setDataAuthList(dataAuthList);
			inheritList.add(allIhInherit);
		}

		//目录查询上级->数据集->全部
		if (DataAuthItemType.CTG == dataAuthItemType) {

			List<MetaFieldCategory> list = (List<MetaFieldCategory>) dao.queryObjectList("fieldCtg.queryFieldCtgByType", CategoryType.Front.toString().toLowerCase(),dataEnvDataSourceType);
			Map<String, MetaFieldCategory> categoryMap = new HashMap<>();
			for (MetaFieldCategory mfc : list) {
				categoryMap.put(mfc.getId(), mfc);
			}

			buildInheritInfo(categoryMap, inheritMap, datasetCtgDataAuthQueryReq.getItemValue(), inheritList);

		}

		return SSMResponseMessage.success("", inheritList);
	}

	/**
	 * 构造继承权限链路
	 * @param categoryMap
	 * @param inheritMap
	 * @param id
	 * @param inheritList
	 */
	public void buildInheritInfo(Map<String, MetaFieldCategory> categoryMap, Map<String, Integer> inheritMap, String id, List<DatasetCtgDataAuthInheritRsq> inheritList) {

		DataSourceType dataEnvDataSourceType = SSDUtil.getDataEnvDataSourceType();

		MetaFieldCategory metaFieldCategory = categoryMap.get(id);
		if (metaFieldCategory != null) {

			MetaFieldCategory parent = categoryMap.get(metaFieldCategory.getParentId());
			if (parent != null) {

				Integer isInherited = inheritMap.get(metaFieldCategory.getId());
				if (Enabled.value(isInherited)) {

					DatasetCtgDataAuthInheritRsq ihInherit = new DatasetCtgDataAuthInheritRsq();
					ihInherit.setInheritedName(parent.getName());
					List<DatasetCtgDataAuthDetailRsq> dataAuthList = queryDatasetCtgDataAuth(DataAuthItemType.CTG.getCode(), parent.getId());
					ihInherit.setDataAuthList(dataAuthList);

					inheritList.add(0, ihInherit);

					buildInheritInfo(categoryMap, inheritMap, parent.getId(), inheritList);

				}

			} else {

				//最上层目录查询数据集
				if (BIConsts.Category_Root_Id.equalsIgnoreCase(metaFieldCategory.getParentId())) {

					Integer isInherited = inheritMap.get(metaFieldCategory.getId());
					if (Enabled.value(isInherited)) {

						List<DatasetRsq> datasetRsqList = (List<DatasetRsq>) dao.queryObjectList("ssm.dataset.listAll", null,dataEnvDataSourceType);

						DatasetCtgDataAuthInheritRsq datasetIhInherit = new DatasetCtgDataAuthInheritRsq();
						Optional<DatasetRsq> opt = datasetRsqList.stream().filter(a -> a.getDatasetId().equalsIgnoreCase(metaFieldCategory.getDatasetId())).findAny();

						if (opt.isPresent()) {
							datasetIhInherit.setInheritedName(opt.get().getDatasetName());
							List<DatasetCtgDataAuthDetailRsq> dataAuthList = queryDatasetCtgDataAuth(DataAuthItemType.DATASET.getCode(), metaFieldCategory.getDatasetId());
							datasetIhInherit.setDataAuthList(dataAuthList);
							inheritList.add(0, datasetIhInherit);

							//数据集查全部
							Integer datasetInherited = inheritMap.get(metaFieldCategory.getDatasetId());
							if (Enabled.value(datasetInherited)) {
								DatasetCtgDataAuthInheritRsq allIhInherit = new DatasetCtgDataAuthInheritRsq();
								allIhInherit.setInheritedName(DataAuthItemType.ALL.getName());

								List<DatasetCtgDataAuthDetailRsq> alldataAuthList = queryDatasetCtgDataAuth(DataAuthItemType.ALL.getCode(), DataAuthItemType.ALL.getValue());
								allIhInherit.setDataAuthList(alldataAuthList);
								inheritList.add(0, allIhInherit);
							}

						}

					}

				}
			}
		}

	}

	/**
	 * 查询配置的权限
	 * @param itemType
	 * @param itemValue
	 * @return
	 */
	public List<DatasetCtgDataAuthDetailRsq> queryDatasetCtgDataAuth(String itemType, String itemValue) {
		Map<String, String> map = new HashMap<>();
		map.put("itemType", itemType);
		map.put("itemValue", itemValue);

		DataSourceType dataEnvDataSourceType = SSDUtil.getDataEnvDataSourceType();
		List<DatasetCtgDataAuthDetailRsq> dataAuthList = (List<DatasetCtgDataAuthDetailRsq>) dao.queryObjectList("ssm.data.auth.dataset.ctg.selectByMap", map,dataEnvDataSourceType);
		return dataAuthList;
	}

	/**
	 * 查询所有的继承关系
	 * @return
	 */
	public Map<String, Integer> queryAllInherit() {

		Map<String, Integer> map = new HashMap<>(5000);
		buildDatasetCtgInheritMap(map, DataSourceType.Default);

		DataSourceType mgpDataSourceType = SSDUtil.getMgpDataSourceType();
		buildDatasetCtgInheritMap(map, mgpDataSourceType);

		return map;
	}

	public void buildDatasetCtgInheritMap(Map<String, Integer> map,DataSourceType dataSourceType){
		List<DatasetCtgInheritEntity> ctgInheritEntityList = (List<DatasetCtgInheritEntity>) dao.queryObjectList("ssm.dataset.ctg.inherit.selectAllInherit", null,dataSourceType);

		if (CollUtil.isNotEmpty(ctgInheritEntityList)) {
			for (DatasetCtgInheritEntity datasetCtgInheritEntity : ctgInheritEntityList) {
				map.put(datasetCtgInheritEntity.getItemValue(), datasetCtgInheritEntity.getIsInherited());
			}
		}
	}

    /**
     * 人员转岗
     * @param employeeTransferReq
     * @return
     */
    public SSMResponseMessage employeeTransfer(EmployeeTransferReq employeeTransferReq) {

		DataSourceType dataEnvDataSourceType = SSDUtil.getDataEnvDataSourceType();
        List<DatasetCtgDataAuthRsq> dataAuthRsqList = (List<DatasetCtgDataAuthRsq>) dao.queryObjectList("ssm.data.auth.dataset.ctg.queryAuthByUserName", employeeTransferReq.getUserName(),dataEnvDataSourceType);

        if (CollUtil.isNotEmpty(dataAuthRsqList)) {
            dao.delete("ssm.data.auth.dataset.ctg.deleteByUserName", employeeTransferReq.getUserName(),dataEnvDataSourceType);
            Map<String, MetaFieldCategory> categoryMap = SSDMetaCacheManager.getFrontCategories();
            List<DatasetRsq> result = (List<DatasetRsq>) dao.queryObjectList("ssm.dataset.listAll", null,dataEnvDataSourceType);

            Set<String> userSet = new HashSet<>();
            for (DatasetCtgDataAuthRsq dataAuthRsq : dataAuthRsqList) {

                DataAuthItemType dataAuthItemType = DataAuthItemType.get(dataAuthRsq.getItemType());
                switch (dataAuthItemType) {
                    case CTG:
                        MetaFieldCategory metaFieldCategory = categoryMap.get(dataAuthRsq.getItemValue());
                        if (metaFieldCategory != null && StrUtil.isNotEmpty(metaFieldCategory.getDataDevOwner())) {
                            userSet.add(metaFieldCategory.getDataDevOwner());
                        }
                        break;
                    case DATASET:
                        Optional<DatasetRsq> opt = result.stream().filter(a -> a.getDatasetId().equalsIgnoreCase(dataAuthRsq.getItemValue())).findAny();
                        if (opt.isPresent()) {
                            if (StrUtil.isNotEmpty(opt.get().getDataDevOwner())) {
                                userSet.add(opt.get().getDataDevOwner());
                            }
                        }
                        break;
                }
            }

            SSDQueryService ssdQueryService = (SSDQueryService) SpringContextUtil.getBean("SSDQueryService");
            String msg = String.format("授权用户-%s-%s，已转岗至%s，权限将自动回收，若有数据权限需求，请提醒用户重新申请",
                    employeeTransferReq.getDeptNameHis(), employeeTransferReq.getUserName(), employeeTransferReq.getDeptNameCur());
            List<String> users = new ArrayList<>();
            users.addAll(userSet);

            if (CollUtil.isNotEmpty(users)) {
                ssdQueryService.sendMsg(msg, users);
            }
        }
        return new SSMResponseMessage();
    }

}
