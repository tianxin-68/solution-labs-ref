package com.bi.queryer.ssm.mgr.tableDef;

import cn.hutool.core.collection.CollUtil;
import com.bi.queryer.ssm.engine.QueryEngineType;
import com.bi.queryer.ssm.meta.MetaTable;
import com.bi.queryer.ssm.util.SSDUtil;
import com.bi.queryer.sys.enums.Enabled;
import com.bi.queryer.util.excelUtil.ExcelHelper;
import com.bi.queryer.sys.base.AbstractTransaction;
import com.bi.queryer.sys.base.BaseDao;
import com.bi.queryer.sys.common.ResponseMessage;
import com.bi.queryer.sys.db.DataSourceType;
import com.bi.queryer.sys.user.UserManager;
import com.bi.queryer.sys.user.vo.User;
import com.bi.queryer.util.BIUtil;
import com.bi.queryer.util.Guid;
import com.bi.queryer.util.StringUtil;
import com.bi.queryer.util.component.datagrid.DataGrid;
import com.bi.queryer.util.component.datagrid.DataGridColumn;
import com.bi.queryer.util.component.datagrid.IDataGridDataSetProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author contributor
 */
@Service
public class TableDefService implements IDataGridDataSetProvider {

	@Autowired
	private BaseDao dao;

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

		DataGridColumn column = new DataGridColumn("id", "id", -1); // 添加列
		column.setClob(true); // id不需要显示
		grid.addColumn(column);

		column = new DataGridColumn("tableSchema", "schema", 200, "left"); // 添加列
		column.setRemoteSortable(true);
		grid.addColumn(column);

		column = new DataGridColumn("name", "表名", 240, "left"); // 添加列
		column.setRemoteSortable(true);
		grid.addColumn(column);

		column = new DataGridColumn("isFactTable", "类型", -1, "left"); // 添加列
		column.addValueDisplayRule("1", "事实表").addValueDisplayRule("0", "维度表");
		column.setRemoteSortable(true);
		grid.addColumn(column);

		column = new DataGridColumn("tableDesc", "描述", -1, "left");
		column.setRemoteSortable(true);
		grid.addColumn(column);

		column = new DataGridColumn("granularity", "数据粒度", -1, "left");
		column.setRemoteSortable(true);
		grid.addColumn(column);

		column = new DataGridColumn("origTableName", "原始表名", 120, "left");
		column.setRemoteSortable(true);
		grid.addColumn(column);

		column = new DataGridColumn("tableOwner", "负责人", 120, "left");
		column.setRemoteSortable(true);
		grid.addColumn(column);

		column = new DataGridColumn("isKpiTable", "指标库表", 120, "left");
		column.addValueDisplayRule("1", "是").addValueDisplayRule("0", "否");
		column.setRemoteSortable(true);
		grid.addColumn(column);

		column = new DataGridColumn("supportQueryEngines", "支持查询引擎", 120, "left");
		column.setRemoteSortable(true);
		grid.addColumn(column);


		column = new DataGridColumn("isActive", "是否启用", -1); // 添加列
		column.addValueDisplayRule("1", "启用").addValueDisplayRule("0", "<span style='color:red'>禁用</span>");
		column.setRemoteSortable(true);
		grid.addColumn(column);

		column = new DataGridColumn("createdTime", "创建日期", 140);
		column.setRemoteSortable(true);
		grid.addColumn(column);

		column = new DataGridColumn("createdBy", "创建人", -1);
		column.setRemoteSortable(true);
		grid.addColumn(column);

		column = new DataGridColumn("updatedTime", "更新时间", 140);
		column.setRemoteSortable(true);
		grid.addColumn(column);
		column = new DataGridColumn("updatedBy", "更新人", -1);
		column.setRemoteSortable(true);
		grid.addColumn(column);


		column = new DataGridColumn("operator", "操作");
		column.setOperator(true);
		column.addOperation("修改", "editRow")
//                .addOperation("禁用/启用","activeRow")
				.addOperation("删除", "deleteRow");

		column.setFrozen(true);
		column.setCanHide(false);
		column.setWidth(160);
		grid.addColumn(column);


		grid.setDataSetProvider(this, paramMap);
		return grid;
	}

	@Override
	public List<MetaTable> getDataSet(Map queryParamMap) {
		buidQueryParam(queryParamMap);
		return dao.queryMapList("tableDef.queryTableDefList", queryParamMap,SSDUtil.getDataEnvDataSourceType());
	}

	protected void buidQueryParam(Map paramMap) {
	}

	@Override
	public Integer getDataSetTotalSize(Map queryParamMap) {
		buidQueryParam(queryParamMap);
		return dao.queryCount("tableDef.queryTableDefListCount", queryParamMap,SSDUtil.getDataEnvDataSourceType());
	}

	/**
	 * 保存
	 * @param metaTable
	 * @return
	 */
	public ResponseMessage saveTableDef(MetaTable metaTable) {

		ResponseMessage result = new ResponseMessage();
		User user = UserManager.get();

		DataSourceType dataEnvDataSourceType = SSDUtil.getDataEnvDataSourceType();

		try {

			if (BIUtil.isEmpty(metaTable.getId())) {

				metaTable.setId(Guid.id());
				metaTable.setCreatedBy(user.getName());

				dao.executeTranscation(dataEnvDataSourceType, new AbstractTransaction() {
					@Override
					public void execute() {
						dao.insert("tableDef.insertIntoTableDef", metaTable,dataEnvDataSourceType);
						saveEtlJob(metaTable);
					}
				});


				return result;
			}

			metaTable.setUpdatedBy(user.getName());

			dao.executeTranscation(dataEnvDataSourceType, new AbstractTransaction() {
				@Override
				public void execute() {
					dao.update("tableDef.updateTableDef", metaTable,dataEnvDataSourceType);
					//更新字段表的表名
					dao.update("fieldDef.updateFieldTableName", metaTable,dataEnvDataSourceType);

					//禁用时
					if (!Enabled.value(metaTable.getIsActive())) {
						//更新字段表是否可用
						dao.update("fieldDef.updateFieldActiveByTableActive", metaTable,dataEnvDataSourceType);

						//更新关系表是否可用
						dao.update("tableRel.updateTableRelActiveByTable", metaTable,dataEnvDataSourceType);

						//判断表对应的目录是否需要禁止
						disableFieldDef(metaTable);

					}

					saveEtlJob(metaTable);
				}
			});

		} catch (Exception e) {
			return new ResponseMessage(false, e.getMessage());
		}


		return result;
	}

	/**
	 * 保存etl与表关联
	 */
	public void saveEtlJob(MetaTable metaTable) {

		DataSourceType dataEnvDataSourceType = SSDUtil.getDataEnvDataSourceType();

		//处理关联的etljob
		//1删除
		dao.delete("tableDef.deleteEtlJobByTableId", metaTable.getId(),dataEnvDataSourceType);
		//2插入
		if (BIUtil.isNotEmpty(metaTable.getEtlJobs())) {

			Map<String, Object> map = new HashMap<>();
			map.put("tableId", metaTable.getId());
			map.put("createdBy", UserManager.get().getName());
			map.put("etlJobList", metaTable.getEtlJobs());

			dao.insert("tableDef.insertIntoEtlJob", map,dataEnvDataSourceType);
		}
	}

	/**
	 * 获取etlJob集合
	 * @return
	 */
	public ResponseMessage getEtlJobListByTableId(String tableId) {
		ResponseMessage result = new ResponseMessage();
		try {
			DataSourceType dataEnvDataSourceType = SSDUtil.getDataEnvDataSourceType();
			List<String> list = (List<String>) dao.queryObjectList("tableDef.selectEtlJobByTableId", tableId,dataEnvDataSourceType);
			result.setData(list);

		} catch (Exception e) {
			return new ResponseMessage(e);
		}

		return result;
	}

	/**
	 * 批量判断 etl 作业是否仍被引用（olap 库 ssd_table_etl_job）。
	 * inner join ssd_table_def 过滤掉引用表已被删除的记录。
	 * @return etlJob -> 是否被引用
	 */
	public ResponseMessage checkEtlJobsReferenced(List<String> etlJobList) {
		ResponseMessage result = new ResponseMessage();
		try {
			Map<String, Boolean> referencedMap = new HashMap<>();
			if (CollUtil.isNotEmpty(etlJobList)) {
				DataSourceType dataEnvDataSourceType = SSDUtil.getMgpDataSourceType();
				List<String> referenced = (List<String>) dao.queryObjectList(
						"tableDef.selectReferencedEtlJobs", etlJobList, dataEnvDataSourceType);
				for (String etlJob : etlJobList) {
					referencedMap.put(etlJob, referenced != null && referenced.contains(etlJob));
				}
			}
			result.setData(referencedMap);
		} catch (Exception e) {
			return new ResponseMessage(e);
		}
		return result;
	}

	/**
	 * 删除
	 * @param tableId
	 * @return
	 */
	public ResponseMessage deleteTableDefByTableId(String tableId) {

		ResponseMessage result = new ResponseMessage();

		try {
			DataSourceType dataEnvDataSourceType = SSDUtil.getDataEnvDataSourceType();
			dao.executeTranscation(dataEnvDataSourceType, new AbstractTransaction() {
				@Override
				public void execute() {
					dao.delete("tableDef.deleteTableDefByTableId", tableId,dataEnvDataSourceType);
					dao.delete("fieldDef.deleteFieldByTableId", tableId,dataEnvDataSourceType);
					dao.delete("tableDef.deleteEtlJobByTableId", tableId,dataEnvDataSourceType);
				}
			});


		} catch (Exception e) {

			return new ResponseMessage(false, e.getMessage());
		}

		return result;
	}

	/**
	 * 禁用与启动
	 * @param map
	 * @return
	 */
	public ResponseMessage updateTableDefActiveType(Map<String, Object> map) {

		ResponseMessage result = new ResponseMessage();

		try {
			DataSourceType dataEnvDataSourceType = SSDUtil.getDataEnvDataSourceType();
			dao.update("tableDef.updateTableDefActiveType", map,dataEnvDataSourceType);

		} catch (Exception e) {

			return new ResponseMessage(false, e.getMessage());
		}

		return result;
	}

	/**
	 * 查询多维分析表信息
	 * @param map
	 * @return
	 */
	public ResponseMessage queryTableList(Map<String, Object> map) {

		DataSourceType dataEnvDataSourceType = SSDUtil.getDataEnvDataSourceType();

		List<MetaTable> list = (List<MetaTable>) dao.queryObjectList("tableDef.queryTableDefList", map,dataEnvDataSourceType);
		int totalCount = dao.queryCount("tableDef.queryTableDefListCount", map,dataEnvDataSourceType);

		Map<String, Object> mapResult = new HashMap<>();
		mapResult.put("list", list);
		mapResult.put("totalCount", totalCount);

		return new ResponseMessage(true, "", mapResult);
	}

	public void exportMethod(String searchTxt, String type, HttpServletResponse response) {

		Map<String, Object> map = new HashMap<>();
		map.put("searchTxt", searchTxt);
		map.put("pageRowLower", 0);
		map.put("prePageSize", Integer.MAX_VALUE);

		DataSourceType dataEnvDataSourceType = SSDUtil.getDataEnvDataSourceType();
		List<MetaTable> list = (List<MetaTable>) dao.queryObjectList("tableDef.queryTableDefList", map,dataEnvDataSourceType);

		String[] cloumnNames = new String[]{"表ID", "scheam", "表名", "是否是事实表", "粒度值", "原始表名", "表描述", "是否可用", "负责人", "是否是指标库表", "支持查询引擎",};

		List<String[]> cloumnValues = new ArrayList<>();

		if (list != null && list.size() > 0) {

			for (MetaTable metaTable : list) {
				String[] strs = new String[]{metaTable.getId(), metaTable.getTableSchema(), metaTable.getName(), metaTable.getIsFactTable() == 0 ? "否" : "是", BIUtil.nvl(metaTable.getGranularity(), ""), metaTable.getOrigTableName(), metaTable.getTableDesc(), metaTable.getIsActive() == 0 ? "否" : "是", metaTable.getTableOwner(), metaTable.getIsKpiTable() == 0 ? "否" : "是", metaTable.getSupportQueryEngines()};
				cloumnValues.add(strs);
			}
		}

		ExcelHelper.exportExcel(cloumnNames, cloumnValues, type, response);

	}

	public ResponseMessage importMethod(MultipartFile files) {

		ResponseMessage result = new ResponseMessage();

		// 获取前端传过来的file
		InputStream inputStream = null;
		StringBuilder msg = new StringBuilder();

		try {

			if (files != null) {

				String fileName = new String(files.getOriginalFilename().getBytes("ISO-8859-1"), "UTF-8");
				inputStream = files.getInputStream();

				List<MetaTable> list = ExcelHelper.convertToList(MetaTable.class, fileName, inputStream, 1, 10, 0);

				if (list.size() == 0) {
					return new ResponseMessage(false, "文件内容为空");
				}

				for (int i = 0; i < list.size(); i++) {
					MetaTable metaTable = list.get(i);
					if (BIUtil.isEmpty(metaTable.getTableSchema())) {
						msg.append("第" + (i + 1) + "行表数据库名为空,");
					}
					if (BIUtil.isEmpty(metaTable.getName())) {
						msg.append("第" + (i + 1) + "行表名为空,");
					}
					String supportQueryEngines = metaTable.getSupportQueryEngines();
					if (BIUtil.isEmpty(supportQueryEngines) || !supportQueryEngines.toLowerCase().contains(DataSourceType.Trino_Master.getDialect().toLowerCase()))
					{
						msg.append("第" + (i + 1) + "行表支持查询引擎为空或者不包含trino,");
					}
				}

				if (BIUtil.isNotEmpty(msg.toString())) {
					result.setSuccess(false);
					result.setMessage(msg.toString());
					return result;
				} else {

					User user = UserManager.get();

					DataSourceType dataEnvDataSourceType = SSDUtil.getDataEnvDataSourceType();
					dao.executeTranscation(dataEnvDataSourceType, new AbstractTransaction() {
						@Override
						public void execute() {
							for (MetaTable metaTable : list) {

								//插入数据库
								if (BIUtil.isEmpty(metaTable.getId())) {
									metaTable.setId(Guid.id());
								}

								metaTable.setCreatedBy(user.getName());
								metaTable.setUpdatedBy(user.getName());
								int count = dao.queryCount("tableDef.checkTableExist", metaTable.getId(),dataEnvDataSourceType);
								if (count == 0) {
									dao.insert("tableDef.insertIntoTableDef", metaTable,dataEnvDataSourceType);
								} else {
									dao.update("tableDef.updateTableDef", metaTable,dataEnvDataSourceType);
								}
							}
						}
					});


				}
			}

		} catch (Exception e) {
			return new ResponseMessage(false, e.getMessage());
		}

		return result;
	}

	/**
	 * 查询etl任务列表
	 * @return
	 */
	public ResponseMessage queryEtljobsList(String jobName) {

		ResponseMessage result = new ResponseMessage();

		try {

			Map<String, Object> map = new HashMap<>();
			map.put("jobName", jobName);

			List<String> list = (List<String>) dao.queryObjectList("tableDef.queryEtljobsList", map, DataSourceType.ETL);
			result.setData(list);

		} catch (Exception e) {
			return new ResponseMessage(e);
		}

		return result;
	}

	/**
	 * 通过表id集合查询表关联的etl作业
	 * @return
	 */
	public List<String> selectEtlJobByTableIdList(List<String> tableIds) {

		List<String> list = new ArrayList<>();
		try {

			DataSourceType dataEnvDataSourceType = SSDUtil.getDataEnvDataSourceType();
			List<String> ssmEtlJobs = (List<String>) dao.queryObjectList("tableDef.selectEtlJobByTableIdList", tableIds, dataEnvDataSourceType);

			//查询多维V2的etl作业
			List<String> olapEtlJobs = (List<String>) dao.queryObjectList("tableDef.selectEtlJobByTableIdList", tableIds, DataSourceType.OLAP);

			if (CollUtil.isNotEmpty(ssmEtlJobs)) {
				list.addAll(ssmEtlJobs);
			}

			if (CollUtil.isNotEmpty(olapEtlJobs)) {
				list.addAll(olapEtlJobs);
			}

			if (CollUtil.isNotEmpty(list)) {
				list = list.stream().distinct().collect(Collectors.toList());
			}


		} catch (Exception e) {
			return null;
		}

		return list;
	}

	/**
	 * 判断表对应的目录是否需要禁止
	 */
	public void disableFieldDef(MetaTable metaTable) {

		try {

			DataSourceType dataEnvDataSourceType = SSDUtil.getDataEnvDataSourceType();
			//1、查询表关联的字段的所有目录
			List<String> ctgList = (List<String>) dao.queryObjectList("fieldDef.getFieldCtgListByTableField", metaTable.getId(),dataEnvDataSourceType);

			//2、查询目录下所有字段对应的表是否要禁止禁止
			for (String str : ctgList) {
				List<String> tableList = (List<String>) dao.queryObjectList("fieldDef.getTableListByFieldCtg", str,dataEnvDataSourceType);
				if (CollUtil.isEmpty(tableList) || tableList.size() < 1) {
					dao.update("fieldCtg.updateFieldCtgIsActive", str,dataEnvDataSourceType);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
