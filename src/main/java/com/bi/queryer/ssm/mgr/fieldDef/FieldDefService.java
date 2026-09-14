package com.bi.queryer.ssm.mgr.fieldDef;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import com.bi.queryer.ssm.enums.AggExpressionType;
import com.bi.queryer.ssm.enums.CategoryType;
import com.bi.queryer.ssm.enums.FieldFilterMode;
import com.bi.queryer.ssm.enums.FieldFilterType;
import com.bi.queryer.ssm.meta.*;
import com.bi.queryer.ssm.mgr.fieldDef.model.AsyncMgpMetricReq;
import com.bi.queryer.ssm.mgr.fieldDef.model.FieldReq;
import com.bi.queryer.ssm.mgr.fieldDef.model.ManualIndexWhitePaperEntity;
import com.bi.queryer.ssm.mgr.fieldDef.model.whitePaper.DimBasicInfo;
import com.bi.queryer.ssm.mgr.fieldDef.model.whitePaper.MetricBasicInfo;
import com.bi.queryer.ssm.mgr.fieldDef.model.whitePaper.MetricDetail;
import com.bi.queryer.ssm.util.SSDUtil;
import com.bi.queryer.sys.base.AbstractTransaction;
import com.bi.queryer.sys.base.BaseDao;
import com.bi.queryer.sys.common.ResponseMessage;
import com.bi.queryer.sys.config.SC;
import com.bi.queryer.sys.db.DBUtil;
import com.bi.queryer.sys.db.DataSourceType;
import com.bi.queryer.sys.db.DataType;
import com.bi.queryer.sys.enums.Enabled;
import com.bi.queryer.sys.user.UserManager;
import com.bi.queryer.sys.user.vo.User;
import com.bi.queryer.util.BIUtil;
import com.bi.queryer.util.Guid;
import com.bi.queryer.util.StringUtil;
import com.bi.queryer.util.component.datagrid.DataGrid;
import com.bi.queryer.util.component.datagrid.DataGridColumn;
import com.bi.queryer.util.component.datagrid.IDataGridDataSetProvider;
import com.bi.queryer.util.dataBaseImport.DataBaseImportFactory;
import com.bi.queryer.util.dataBaseImport.vo.TableField;
import com.bi.queryer.util.excelUtil.ExcelHelper;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author contributor
 */
@Service
public class FieldDefService implements IDataGridDataSetProvider {

    @Autowired
    private BaseDao dao;

    /**
     * 字段树
     */
    protected static List<JSONObject> fieldTree = new ArrayList<>();


    //初始化白皮书查询线程池
    private static ThreadPoolExecutor kpiQueryTpe = new ThreadPoolExecutor(4, 20, 20,
            TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(100), Executors.defaultThreadFactory(),
            new ThreadPoolExecutor.CallerRunsPolicy());


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


        DataGridColumn column = new DataGridColumn("belongTableName", "所属表", 260); // 添加列
        grid.addColumn(column);

        column = new DataGridColumn("id", "字段id", -1); // 添加列
        column.setClob(true);
        grid.addColumn(column);

        column = new DataGridColumn("code", "字段code", 200, "left"); // 添加列
        grid.addColumn(column);

        column = new DataGridColumn("name", "字段名", 200, "left"); // 添加列
        grid.addColumn(column);

        column = new DataGridColumn("title", "标题", 200, "left");
        grid.addColumn(column);

        column = new DataGridColumn("dataType", "数据类型", -1, "left"); // 添加列
        grid.addColumn(column);

        column = new DataGridColumn("weight", "字段权重", -1, "left");
        grid.addColumn(column);

        column = new DataGridColumn("isMeasure", "度量", -1); // 添加列
        column.addValueDisplayRule("1", "是")
                .addValueDisplayRule("0", "否");
        grid.addColumn(column);

        column = new DataGridColumn("aggExpression", "聚合方式", 280, "left");
        grid.addColumn(column);

        column = new DataGridColumn("isShow", "显示", -1); // 添加列
        column.addValueDisplayRule("1", "是")
                .addValueDisplayRule("0", "否");
        grid.addColumn(column);

        column = new DataGridColumn("showFormatExpression", "显示格式", -1, "left");
        grid.addColumn(column);

        column = new DataGridColumn("isResult", "可查询", -1); // 添加列
        column.addValueDisplayRule("1", "是")
                .addValueDisplayRule("0", "否");
        grid.addColumn(column);

        column = new DataGridColumn("isFilter", "可筛选", -1); // 添加列
        column.addValueDisplayRule("1", "是")
                .addValueDisplayRule("0", "否");
        grid.addColumn(column);

        column = new DataGridColumn("filterShowType", "筛选类型", -1, "left");
        for (FieldFilterType fieldFilterType : FieldFilterType.values()) {
            column.addValueDisplayRule(fieldFilterType.getCode(), fieldFilterType.getDesc());
        }
        grid.addColumn(column);

        column = new DataGridColumn("filterTableId", "筛选数据表id", -1, "left");
        column.setHidden(true);
        grid.addColumn(column);

        column = new DataGridColumn("filterTableName", "筛选数据表名", 140, "left");
        grid.addColumn(column);

        column = new DataGridColumn("filterValueMode", "筛选值方式", 140, "left");
        for (FieldFilterMode fieldFilterMode : FieldFilterMode.values()) {
            column.addValueDisplayRule(fieldFilterMode.getCode(), fieldFilterMode.getDesc());
        }
        grid.addColumn(column);

        column = new DataGridColumn("filterSQL", "筛选数据SQL", 140, "left");
        column.setClob(true);
        grid.addColumn(column);

        column = new DataGridColumn("showOrder", "显示顺序", -1, "left");
        grid.addColumn(column);

        column = new DataGridColumn("isSensitive", "敏感", -1); // 添加列
        column.addValueDisplayRule("1", "是")
                .addValueDisplayRule("0", "否");
        grid.addColumn(column);

        column = new DataGridColumn("sensitiveDataType", "敏感类型", -1, "left");
        grid.addColumn(column);

        column = new DataGridColumn("fieldKeyName", "key字段名", -1, "left");
        grid.addColumn(column);

        column = new DataGridColumn("fieldKeyType", "key字段数据类型", 160, "left");
        grid.addColumn(column);


        column = new DataGridColumn("categoryId", "前台目录id", -1, "left");
        column.setHidden(true);
        grid.addColumn(column);

        column = new DataGridColumn("categoryName", "前台目录", -1, "left");
        grid.addColumn(column);


        column = new DataGridColumn("virtualCategoryId", "后台目录id", -1, "left");
        column.setHidden(true);
        grid.addColumn(column);

        column = new DataGridColumn("virtualCategoryName", "后台目录", -1, "left");
        grid.addColumn(column);


        column = new DataGridColumn("isActive", "启用", -1, "left");
        column.addValueDisplayRule("1", "是")
                .addValueDisplayRule("0", "否");
        grid.addColumn(column);

        column = new DataGridColumn("remark", "字段描述", -1);
        column.setHidden(true);
        grid.addColumn(column);

        column = new DataGridColumn("dimTableId", "维表id", -1);
        column.setHidden(true);
        grid.addColumn(column);

        column = new DataGridColumn("dimTableName", "维表名称", -1);
        column.setHidden(true);
        grid.addColumn(column);

        column = new DataGridColumn("factTableId", "事实表id", -1);
        column.setHidden(true);
        grid.addColumn(column);

        column = new DataGridColumn("factTableName", "事实表名", -1);
        column.setHidden(true);
        grid.addColumn(column);

        column = new DataGridColumn("fieldKeyId", "fieldKeyId", -1);
        column.setHidden(true);
        grid.addColumn(column);

        column = new DataGridColumn("kpiName", "kpiName", -1);
        column.setHidden(true);
        grid.addColumn(column);

        column = new DataGridColumn("kpiNo", "kpiNo", -1);
        column.setHidden(true);
        grid.addColumn(column);

        column = new DataGridColumn("canColDim", "可否作为列维度", -1);
        column.setHidden(true);
        grid.addColumn(column);

        column = new DataGridColumn("isCommonDate", "公共日期", -1);
        column.setHidden(true);
        grid.addColumn(column);

        column = new DataGridColumn("dateGranularity", "公共日期粒度", -1);
        column.setHidden(true);
        grid.addColumn(column);

        column = new DataGridColumn("fieldExtendList", "fieldExtendList", -1);
        column.setHidden(true);
        grid.addColumn(column);

        column = new DataGridColumn("exportAppendString", "exportAppendString", -1);
        column.setHidden(true);
        grid.addColumn(column);

        column = new DataGridColumn("sensitiveDataType", "sensitiveDataType", -1);
        column.setHidden(true);
        grid.addColumn(column);

        column = new DataGridColumn("filterTips", "提示说明", -1);
        column.setHidden(true);
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
               // .addOperation("层级", "editRowHierarchy")
                .addOperation("查看关联模板", "viewAssociateTemplate")
                .addOperation("权限关联", "authAssociate")
                .addOperation("删除", "deleteRow");

        column.setFrozen(false);
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
        List<MetaField> metaFieldList = dao.queryMapList("fieldDef.queryFieldDefList", queryParamMap,dataEnvDataSourceType);
        buildFieldCtgInfo(metaFieldList,CategoryType.Front);
        buildFieldCtgInfo(metaFieldList,CategoryType.Back);
        return metaFieldList;
    }

    public void buildFieldCtgInfo(List<MetaField> metaFieldList, CategoryType categoryType){

        List<MetaFieldCtgRel> metaFieldCtgRelList = (List<MetaFieldCtgRel>)dao.queryObjectList("fieldDef.queryFieldCtgRel",categoryType.toString().toLowerCase());
        if(CollUtil.isEmpty(metaFieldCtgRelList)){
            return;
        }

        Map<String,MetaFieldCtgRel> metaFieldCtgRelMap = new HashMap<>();
        for(MetaFieldCtgRel metaFieldCtgRel : metaFieldCtgRelList){
            metaFieldCtgRelMap.put(metaFieldCtgRel.getFieldId(),metaFieldCtgRel);
        }

        for(MetaField metaField : metaFieldList) {
            MetaFieldCtgRel metaFieldCtgRel = metaFieldCtgRelMap.get(metaField.getId());
            if (metaFieldCtgRel == null) {
                continue;
            }

            if(CategoryType.Front == categoryType){
                metaField.setCategoryId(metaFieldCtgRel.getCtgId());
                metaField.setCategoryName(metaFieldCtgRel.getCtgName());
            }else{
                metaField.setVirtualCategoryId(metaFieldCtgRel.getCtgId());
                metaField.setVirtualCategoryName(metaFieldCtgRel.getCtgName());
            }
        }

    }

    protected void buidQueryParam(Map paramMap) {
    }

    @Override
    public Integer getDataSetTotalSize(Map queryParamMap) {
        buidQueryParam(queryParamMap);
        DataSourceType dataEnvDataSourceType = SSDUtil.getDataEnvDataSourceType();
        return dao.queryCount("fieldDef.queryFieldDefListCount", queryParamMap,dataEnvDataSourceType);
    }

    /**
     * 保存
     * @param metaField
     * @return
     */
    @Transactional
    public ResponseMessage saveFieldDef(MetaField metaField) {

        ResponseMessage responseMessage = new ResponseMessage();
        DataSourceType dataEnvDataSourceType = SSDUtil.getDataEnvDataSourceType();

        try {

            User user = UserManager.get();
            dao.executeTranscation(dataEnvDataSourceType, new AbstractTransaction() {
                @Override
                public void execute() {
                    if (BIUtil.isEmpty(metaField.getId())) {

                        metaField.setId(Guid.id());
                        metaField.setCreatedBy(user.getName());
                        dao.insert("fieldDef.insertIntoFieldDef", metaField,dataEnvDataSourceType);
                    } else {
                        metaField.setUpdatedBy(user.getName());
                        dao.update("fieldDef.updateFieldDef", metaField,dataEnvDataSourceType);
                    }
                    dealKpiRelData(metaField);
                }
            });
        } catch (Exception e) {
            return new ResponseMessage(false, e.getMessage());
        }

        return responseMessage;
    }

    /**
     * 处理白皮书关联指标
     */
    private void dealKpiRelData(MetaField metaField) {
        User user = UserManager.get();

        DataSourceType dataEnvDataSourceType = SSDUtil.getDataEnvDataSourceType();

        // 更新字段code与白皮书指标的绑定关系
        dao.delete("fieldDef.deleteCodeKpiRel", metaField,dataEnvDataSourceType);
        if (StrUtil.isNotEmpty(metaField.getKpiNo())) {
            Map<String, Object> relMap = new HashMap<>();
            relMap.put("fieldCode", metaField.getCode());
            relMap.put("kpiName", metaField.getKpiName());
            relMap.put("kpiNo", metaField.getKpiNo());
            relMap.put("createdBy", user.getName());
            relMap.put("measureCode", Enabled.isTrue(metaField.getIsMeasure()) ? metaField.getKpiNo() : null);
            relMap.put("dimCode1", Enabled.isTrue(metaField.getIsMeasure()) ? null : metaField.getKpiNo());
            dao.insert("fieldDef.insertCodeKpiRel", relMap,dataEnvDataSourceType);
        }
    }

    public ResponseMessage deleteFieldDefById(String id) {

        ResponseMessage responseMessage = new ResponseMessage(id);

        try {

            DataSourceType dataEnvDataSourceType = SSDUtil.getDataEnvDataSourceType();
            dao.delete("fieldDef.deleteFieldDefById", id,dataEnvDataSourceType);

        } catch (Exception e) {
            return new ResponseMessage(false, e.getMessage(), id);
        }

        return responseMessage;
    }

    public ResponseMessage buildFieldTree() {
        ResponseMessage responseMessage = new ResponseMessage();

        DataSourceType dataEnvDataSourceType = SSDUtil.getDataEnvDataSourceType();

        List<TreeNode> tableList = (List<TreeNode>) dao.queryObjectList("tableDef.queryAllTableActive", null,dataEnvDataSourceType);
        List<TreeNode> fieldList = (List<TreeNode>) dao.queryObjectList("fieldDef.queryFieldListByTableId", null,dataEnvDataSourceType);

        Map<String,List<TreeNode>> tableMap = new HashMap<>();
        if(CollUtil.isNotEmpty(fieldList)) {
            for (TreeNode fieldNode : fieldList) {
                String tableId = fieldNode.getParentId();
                List<TreeNode> tableFieldList = tableMap.get(tableId);
                if (CollUtil.isEmpty(tableFieldList)) {
                    tableFieldList = new ArrayList<>();
                }

                tableFieldList.add(fieldNode);
                tableMap.put(tableId, tableFieldList);
            }
        }

        List<JSONObject> treeList = new ArrayList<>();

        if (BIUtil.isNotEmpty(tableList)) {

            for (TreeNode treeNode : tableList) {

                JSONObject jsonObject = treeNode.toTreeNode();
                List<TreeNode> children = tableMap.get(treeNode.getId());
                if (CollUtil.isEmpty(children)) {
                    children = new ArrayList<>();
                }

                jsonObject.put("children", children);
                treeList.add(jsonObject);
            }
        }

        responseMessage.setData(treeList);

        return responseMessage;
    }

    public ResponseMessage saveHierarchyInfo(HierarchyInfo hierarchyInfo) {

        ResponseMessage responseMessage = new ResponseMessage();

        try {

            DataSourceType dataEnvDataSourceType = SSDUtil.getDataEnvDataSourceType();

            Integer count = dao.queryCount("fieldDef.queryHierarchyInfoCountByFieldId", hierarchyInfo.getFieldId(),dataEnvDataSourceType);

            if (count == 0) {
                dao.insert("fieldDef.insertIntoHierarchyInfo", hierarchyInfo,dataEnvDataSourceType);
            } else {
                dao.update("fieldDef.updateHierarchyInfo", hierarchyInfo,dataEnvDataSourceType);
            }


        } catch (Exception e) {
            return new ResponseMessage(false, e.getMessage());
        }

        return responseMessage;
    }

    public ResponseMessage getHierarchyInfoByFieldId(String fieldId) {

        ResponseMessage responseMessage = new ResponseMessage();

        DataSourceType dataEnvDataSourceType = SSDUtil.getDataEnvDataSourceType();
        HierarchyInfo hierarchyInfo = (HierarchyInfo) dao.queryObject("fieldDef.getHierarchyInfoByFieldId", fieldId,dataEnvDataSourceType);

        if (hierarchyInfo == null || BIUtil.isEmpty(hierarchyInfo.getFieldId())) {

            hierarchyInfo = new HierarchyInfo();
            hierarchyInfo.setFieldId(fieldId);
            hierarchyInfo.setIsActive(1);
        }

        responseMessage.setData(hierarchyInfo);

        return responseMessage;
    }

    public void exportMethod(String searchTxt, String type, String tableId, String fact_table, HttpServletResponse response) {

        Map<String, Object> map = new HashMap<>();
        map.put("searchTxt", searchTxt);
        map.put("tableId", tableId);
        map.put("fact_table", fact_table);
        map.put("pageRowLower", 0);
        map.put("prePageSize", Integer.MAX_VALUE);

        DataSourceType dataEnvDataSourceType = SSDUtil.getDataEnvDataSourceType();
        List<MetaField> list = (List<MetaField>) dao.queryObjectList("fieldDef.queryFieldDefList", map,dataEnvDataSourceType);

        String[] cloumnNames = new String[]{"字段id", "字段code", "字段名", "字段标题", "数据类型", "字段权重", "度量", "聚合表达式",
                "是否显示", "显示格式", "可查询", "可筛选", "筛选类型", "筛选数据表id", "筛选数据表名", "筛选值方式", "筛选数据SQL", "字段显示顺序",
                "是否敏感字段", "敏感类型", "自动扩展", "key字段",
                "前台目录(弃用)", "后台目录（弃用）", "启用", "字段描述",
                "事实表id", "事实表名",
                "维表id", "维表名称", "可否作为列维度",
                "公共日期", "公共日期粒度", "关联白皮书指标编码"
        };

        List<String[]> cloumnValues = new ArrayList<>();

        if (list != null && list.size() > 0) {

            for (MetaField metaField : list) {

                String filterShowType = "";
                String filterValueMode = "";
                if (BIUtil.isNotEmpty(metaField.getFilterShowType())) {
                    filterShowType = FieldFilterType.get(metaField.getFilterShowType()).getDesc();
                }
                if (BIUtil.isNotEmpty(metaField.getFilterValueMode())) {
                    filterValueMode = FieldFilterMode.get(metaField.getFilterValueMode()).getDesc();
                }

                String[] strs = new String[]{
                        metaField.getId(),
                        metaField.getCode(),
                        metaField.getName(),
                        metaField.getTitle(),
                        metaField.getDataType(),
                        BIUtil.nvl(metaField.getWeight(), ""),
                        trueOrFalse(metaField.getIsMeasure()),
                        metaField.getAggExpression(),
                        trueOrFalse(metaField.getIsShow()),
                        metaField.getShowFormatExpression(),
                        trueOrFalse(metaField.getIsResult()),
                        trueOrFalse(metaField.getIsFilter()),
                        filterShowType,
                        metaField.getFilterTableId(),
                        metaField.getFilterTableName(),
                        filterValueMode,
                        metaField.getFilterSQL(),
                        BIUtil.nvl(metaField.getShowOrder(), ""),
                        trueOrFalse(metaField.getIsSensitive()),
                        metaField.getSensitiveDataType(),
                        metaField.getFieldExtendList(),
                        metaField.getFieldKeyId(),
                        "",
                        "",
                       // metaField.getCategoryId(),
                        //metaField.getVirtualCategoryId(),
                        trueOrFalse(metaField.getIsActive()),
                        metaField.getRemark(),
                        metaField.getFactTableId(),
                        metaField.getFactTableName(),
                        metaField.getDimTableId(),
                        metaField.getDimTableName(),
                        trueOrFalse(metaField.getCanColDim()),
                        trueOrFalse(metaField.getIsCommonDate()),
                        metaField.getDateGranularity(),
                        metaField.getKpiNo()
                };
                cloumnValues.add(strs);
            }
        }

        ExcelHelper.exportExcel(cloumnNames, cloumnValues, type, response);

    }

    public String trueOrFalse(Integer num) {
        if (num != null) {
            return num == 1 ? "是" : "否";
        }
        return "否";
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

                List<MetaField> list = ExcelHelper.convertToList(MetaField.class, fileName, inputStream, 1, 34, 0);

                if (list.size() == 0) {
                    return new ResponseMessage(false, "文件内容为空");
                }

                for (int i = 0; i < list.size(); i++) {
                    MetaField metaField = list.get(i);
                    if (BIUtil.isEmpty(metaField.getCode())) {
                        msg.append("第" + (i + 1) + "行字段code为空,");
                    }
                    if (BIUtil.isEmpty(metaField.getName())) {
                        msg.append("第" + (i + 1) + "行字段名称为空,");
                    }
                    if (BIUtil.isEmpty(metaField.getDataType())) {
                        msg.append("第" + (i + 1) + "行字段数据类型为空,");
                    }
                    if (BIUtil.isEmpty(metaField.getTitle())) {
                        msg.append("第" + (i + 1) + "行字段标题为空,");
                    }
                    if ((metaField.getWeight() == null)) {
                        msg.append("第" + (i + 1) + "行字段权重为空,");
                    }
                    if ((BIUtil.isNotEmpty(metaField.getDimTableId())) && BIUtil.isNotEmpty(metaField.getFactTableId())) {
                        msg.append("第" + (i + 1) + "行事实表id与维度表id不能同时存在,");
                    }
                    if ((BIUtil.isEmpty(metaField.getDimTableId())) && BIUtil.isEmpty(metaField.getFactTableId())) {
                        msg.append("第" + (i + 1) + "行事实表id与维度表id至少一个需有值,");
                    }
//                    if (Enabled.isTrue(metaField.getIsShow()) && Enabled.isFalse(metaField.getIsCommonDate()) && StrUtil.isEmpty(metaField.getKpiNo())) {
//                        msg.append("第" + (i + 1) + "行关联白皮书指标编码为空,");
//                    }
                }

                if (BIUtil.isNotEmpty(msg.toString())) {
                    result.setSuccess(false);
                    result.setMessage(msg.toString());
                    return result;
                } else {

                    User user = UserManager.get();
                    DataSourceType dataEnvDataSourceType = SSDUtil.getDataEnvDataSourceType();

                    Map<String, ManualIndexWhitePaperEntity> indexCodeMap = getManualIndexWhitePaperMap(null);
                    dao.executeTranscation(dataEnvDataSourceType, new AbstractTransaction() {
                        @Override
                        public void execute() {
                            for (MetaField metaField : list) {

                                if (BIUtil.isNotEmpty(metaField.getFilterShowType())) {
                                    for (FieldFilterType fieldFilterType : FieldFilterType.values()) {
                                        if (fieldFilterType.getDesc().equalsIgnoreCase(metaField.getFilterShowType())) {
                                            metaField.setFilterShowType(fieldFilterType.getCode());
                                            break;
                                        }
                                    }
                                }

                                if (BIUtil.isNotEmpty(metaField.getFilterValueMode())) {
                                    for (FieldFilterMode fieldFilterMode : FieldFilterMode.values()) {
                                        if (fieldFilterMode.getDesc().equalsIgnoreCase(metaField.getFilterValueMode())) {
                                            metaField.setFilterValueMode(fieldFilterMode.getCode());
                                            break;
                                        }
                                    }
                                }

                                //插入数据库
                                if (BIUtil.isEmpty(metaField.getId())) {
                                    metaField.setId(Guid.id());
                                }

                                metaField.setCreatedBy(user.getName());
                                metaField.setUpdatedBy(user.getName());

                                int count = dao.queryCount("fieldDef.checkFieldExist", metaField.getId(),dataEnvDataSourceType);
                                if (count == 0) {
                                    dao.insert("fieldDef.insertIntoFieldDef", metaField,dataEnvDataSourceType);
                                } else {
                                    dao.update("fieldDef.updateFieldDef", metaField,dataEnvDataSourceType);
                                }
                                if (StrUtil.isNotEmpty(metaField.getKpiNo()) && indexCodeMap.get(metaField.getKpiNo()) != null) {
                                    metaField.setKpiName(indexCodeMap.get(metaField.getKpiNo()).getIndexName());
                                    dealKpiRelData(metaField);
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

    public ResponseMessage saveFieldDefAuthAssociate(Map<String, Object> map) {
        ResponseMessage responseMessage = new ResponseMessage();

        String fieldCode = map.get("fieldCode").toString();
        List<JSONObject> list = (List<JSONObject>) map.get("authDimList");

        DataSourceType dataEnvDataSourceType = SSDUtil.getDataEnvDataSourceType();
        dao.executeTranscation(dataEnvDataSourceType, new AbstractTransaction() {
            @Override
            public void execute() {

                //1 先删除关联
                dao.delete("fieldDef.deleteFieldAuthDim", fieldCode,dataEnvDataSourceType);

                //2 插入关联
                if (BIUtil.isNotEmpty(list)) {
                    map.put("createdBy", UserManager.get().getName());
                    dao.insert("fieldDef.insertIntoFieldAuthDim", map,dataEnvDataSourceType);
                }

            }
        });

        return responseMessage;
    }

    public ResponseMessage getFieldAuthDimByFieldCode(String fieldCode) {

        ResponseMessage responseMessage = new ResponseMessage();

        DataSourceType dataEnvDataSourceType = SSDUtil.getDataEnvDataSourceType();
        String str = (String) dao.queryObject("fieldDef.getFieldAuthDimByFieldCode", fieldCode,dataEnvDataSourceType);
        responseMessage.setData(str);

        return responseMessage;

    }

    /**
     * 获取字段所属表名
     * @param tableId
     * @return
     */
    public ResponseMessage getTableByFieldTableId(String tableId) {

        ResponseMessage responseMessage = new ResponseMessage();

        MetaTable metaTable = SSDMetaCacheManager.getTable(tableId);
        responseMessage.setData(metaTable);

        return responseMessage;
    }

    /**
     * 获取表字段
     * @param tableId
     * @return
     */
    public ResponseMessage getTableFieldList(String tableId) {

        ResponseMessage responseMessage = new ResponseMessage();
        List<TableField> list = new ArrayList<>();

        Connection conn = null;
        ResultSet rs = null;
        Statement stmt = null;

        try {

            MetaTable metaTable = SSDMetaCacheManager.getTable(tableId);
            if (metaTable != null) {

                //jdbc获取元数据
                String tableName = BIUtil.isNotEmpty(metaTable.getOrigTableName()) ? metaTable.getTableSchema() + "." + metaTable.getOrigTableName() : metaTable.getName();
                conn = DBUtil.getConn(DBUtil.getDataSourceType());

                stmt = conn.createStatement();

                String sql = " DESCRIBE " + tableName;

                rs = stmt.executeQuery(sql);

                while (rs.next()) {

                    TableField tableField = new TableField();

                    String field = rs.getString("Column");
                    tableField.setName(field);

                    String comment = rs.getString("Comment");
                    if (BIUtil.isEmpty(comment)) {
                        comment = field;
                    }
                    tableField.setComment(comment);

                    String dbType = rs.getString("Type");
                    tableField.setDataType(DataBaseImportFactory.getFieldDataType(dbType));

                    list.add(tableField);
                }

            }

            responseMessage.setData(list);

        } catch (Exception e) {
            return new ResponseMessage(false, e.getMessage());
        } finally {
            try {

                if (rs != null) {
                    rs.close();
                }
                if (stmt != null) {
                    stmt.close();
                }
                if (conn != null) {
                    conn.close();
                }

            } catch (Exception e) {
                e.printStackTrace();
            }

        }

        return responseMessage;
    }

    /**
     * 导入字段
     * @param map
     * @return
     */
    public ResponseMessage importField(Map<String, Object> map) {

        ResponseMessage responseMessage = new ResponseMessage();

        try {

            String tableId = map.get("tableId").toString();
            String isFactTable = map.get("isFactTable").toString();
            String tableName = map.get("tableName").toString();

            List<JSONObject> list = JSONObject.parseArray(JSONObject.toJSONString(map.get("list")), JSONObject.class);
            User user = UserManager.get();

            if (BIUtil.isNotEmpty(list)) {

                DataSourceType dataEnvDataSourceType = SSDUtil.getDataEnvDataSourceType();
                dao.executeTranscation(dataEnvDataSourceType, new AbstractTransaction() {
                    @Override
                    public void execute() {
                        boolean isAdd = false;

                        Map mapResult = new HashMap();

                        for (JSONObject jSONObject : list) {

                            isAdd = false;
                            String name = jSONObject.getString("name");

                            mapResult.put("tableId", tableId);
                            mapResult.put("name", name);
                            MetaField metaField = (MetaField) dao.queryObject("fieldDef.queryField", mapResult,dataEnvDataSourceType);
                            if (metaField == null) {
                                metaField = new MetaField();
                                metaField.setId(Guid.id());

                                isAdd = true;

                                if ("1".equalsIgnoreCase(isFactTable)) {

                                    metaField.setFactTableId(tableId);
                                    metaField.setFactTableName(tableName);

                                } else {

                                    metaField.setDimTableId(tableId);
                                    metaField.setDimTableName(tableName);
                                }

                            }

                            metaField.setName(name);
                            metaField.setCode(name);

                            String comment = jSONObject.getString("comment");
                            metaField.setTitle(comment);
                            metaField.setRemark(comment);

                            String dataType = jSONObject.getString("dataType");
                            metaField.setDataType(dataType);

                            //是否是度量
                            if (DataType.Integer.toString().equalsIgnoreCase(dataType)) {
                                metaField.setIsMeasure(Enabled.YES.getId());
                                metaField.setAggExpression(AggExpressionType.Sum.getCode());
                            } else {
                                metaField.setIsMeasure(Enabled.NO.getId());
                                metaField.setAggExpression("");
                            }

                            //可筛选
                            metaField.setIsFilter(Enabled.NO.getId());
                            //可查询
                            metaField.setIsResult(Enabled.YES.getId());

                            metaField.setCreatedBy(user.getName());
                            metaField.setUpdatedBy(user.getName());
                            metaField.setIsActive(Enabled.YES.getId());

                            if (isAdd) {
                                dao.insert("fieldDef.insertIntoFieldDef", metaField,dataEnvDataSourceType);
                            } else {
                                dao.update("fieldDef.updateFieldDef", metaField,dataEnvDataSourceType);
                            }

                        }
                    }
                });

            }

        } catch (Exception e) {
            return new ResponseMessage(false, e.getMessage());
        }

        return responseMessage;
    }

    /**
     * 查看关联模板
     * @return
     */
    public ResponseMessage viewAssociateTemplate(String fieldId) {

        ResponseMessage result = new ResponseMessage();

        try {

            List<SSDQueryTemplate> templateNames = (List<SSDQueryTemplate>) dao.queryObjectList("fieldDef.queryAssociateTemplates", fieldId);

            if (BIUtil.isEmpty(templateNames)) {
                templateNames = new ArrayList<>();
            }

            result.setData(templateNames);

        } catch (Exception e) {
            return new ResponseMessage(e);
        }

        return result;
    }

    public List<ManualIndexWhitePaperEntity> getAllWhitePaperList() {
        return (List<ManualIndexWhitePaperEntity>) dao.queryObjectList("fieldDef.getManualIndexWhitePaperList", null, DataSourceType.Data_Studio);
    }

    public List<ManualIndexWhitePaperEntity> getManualIndexWhitePaperList(List<String> fieldIds) {

        User user = UserManager.get();
        String token = user.getToken();

        //获取所有指标和维度编码集合
        List<String> dimCodeList = new ArrayList<>();
        List<String> metricCodeList = new ArrayList<>();

        List<MetaField> metaFieldList = new ArrayList<>();
        if(CollUtil.isEmpty(fieldIds)){
            metaFieldList.addAll( SSDMetaCacheManager.getFieldsCache().values());
        }else {
            fieldIds.forEach(fieldId -> {
                        MetaField metaField = SSDMetaCacheManager.getFieldsCache().get(fieldId);
                        if (metaField != null) {
                            metaFieldList.add(metaField);
                        }
                    }
            );
        }

        Map<String, MetaField> fieldMap = new HashMap<>(metaFieldList.size());
        for(MetaField metaField : metaFieldList) {
            fieldMap.put(metaField.getCode(), metaField);
            if (!Enabled.value(metaField.getIsShow())) {
                continue;
            }

            if(StrUtil.isEmpty(metaField.getKpiNo())){
                continue;
            }

            //扩展字段不处理
            if(StrUtil.isNotEmpty(metaField.getExtendSrcId())){
                continue;
            }

            if(Enabled.value(metaField.getIsMeasure())){
                metricCodeList.add(metaField.getKpiNo());
            }else{
                dimCodeList.add(metaField.getKpiNo());
            }
        }

        //构造返回值
        List<ManualIndexWhitePaperEntity> resultList = new ArrayList<>();

        CompletableFuture<Void> futureArray[] = new CompletableFuture[2];
        CompletableFuture<Void> future0 =  CompletableFuture.runAsync(() -> {
            resultList.addAll(queryMetricWhitePaperList(metricCodeList,token));
        },kpiQueryTpe);
        futureArray[0] = future0;

        CompletableFuture<Void> future1 =  CompletableFuture.runAsync(() -> {
            resultList.addAll(queryDimWhitePaperList(dimCodeList,token));
        },kpiQueryTpe);
        futureArray[1] = future1;
        CompletableFuture.allOf(futureArray).join();

        for (ManualIndexWhitePaperEntity entity: resultList) {
            MetaField field = fieldMap.get(entity.getIndexNo());
            if (field == null) {
                continue;
            }

            entity.setFieldType(field.getFieldType());
            MetricBasicInfo basicInfo = entity.getMetricBasicInfo();
            if (basicInfo == null || field.getCalcCodeExpression() == null) {
                continue;
            }
            basicInfo.setMetricExp(field.getCalcCodeExpression());
            Pattern pattern = Pattern.compile("\\[(.*?)\\]");
            Matcher matcher = pattern.matcher(field.getCalcCodeExpression());
            Map<String, MetricDetail> expRelMetrics = new HashMap<>(4);
            while (matcher.find()) {
                List<MetaField> metaFields = SSDMetaCacheManager.getFieldByCode(matcher.group(1));
                if (metaFields.size() > 0) {
                    MetaField metaField = metaFields.get(0);
                    MetricDetail detail = new MetricDetail();
                    detail.setMetricName(metaField.getTitle());
                    detail.setMetricCode(metaField.getCode());
                    expRelMetrics.put(metaField.getCode(), detail);
                }
            }
            basicInfo.setExpRelMetrics(expRelMetrics);
        }

        return resultList;
    }

    /**
     * 查询指标白皮书集合
     * @param codeList
     * @return
     */
    public List<ManualIndexWhitePaperEntity> queryMetricWhitePaperList(List<String> codeList,String token) {
        List<ManualIndexWhitePaperEntity> metricWhitePaperEntityList = new ArrayList<>();

        if (CollUtil.isEmpty(codeList)) {
            return metricWhitePaperEntityList;
        }

        codeList = codeList.stream().distinct().collect(Collectors.toList());
        JSONObject requestBody = new JSONObject();
        requestBody.put("codeList", codeList);

        String url = SC.v("ssm.white.paper.metric.query.url", "");
        String resultBody = HttpRequest.post(url)
                .header("u_token", token)
                .body(JSONObject.toJSONString(requestBody))
                .execute().body();

        JSONObject resultJson = JSONObject.parseObject(resultBody);
        if (resultJson != null) {
            List<MetricBasicInfo> metricBasicInfoList = JSONArray.parseArray(JSON.toJSONString(resultJson.get("data")), MetricBasicInfo.class);
            if (CollUtil.isNotEmpty(metricBasicInfoList)) {
                for (MetricBasicInfo metricBasicInfo : metricBasicInfoList) {
                    ManualIndexWhitePaperEntity manualIndexWhitePaperEntity = new ManualIndexWhitePaperEntity();
                    manualIndexWhitePaperEntity.setIndexNo(metricBasicInfo.getMetricCode());
                    manualIndexWhitePaperEntity.setIndexName(metricBasicInfo.getMetricName());
                    manualIndexWhitePaperEntity.setInterpretation(metricBasicInfo.getMetricDesc());
                    manualIndexWhitePaperEntity.setIsMeasure(Enabled.YES.getId());
                    manualIndexWhitePaperEntity.setMetricBasicInfo(metricBasicInfo);
                    manualIndexWhitePaperEntity.setFieldRemark(metricBasicInfo.getMetricRemark());
                    manualIndexWhitePaperEntity.setRelFieldRemark(metricBasicInfo.getRelMetricRemark());
                    metricWhitePaperEntityList.add(manualIndexWhitePaperEntity);
                }
            }

            metricBasicInfoList = null;
        }

        return metricWhitePaperEntityList;
    }

    /**
     * 查询维度白皮书集合
     * @param codeList
     * @return
     */
    public List<ManualIndexWhitePaperEntity> queryDimWhitePaperList(List<String> codeList,String token) {
        List<ManualIndexWhitePaperEntity> dimWhitePaperEntityList = new ArrayList<>();

        if (CollUtil.isEmpty(codeList)) {
            return dimWhitePaperEntityList;
        }

        codeList = codeList.stream().distinct().collect(Collectors.toList());
        JSONObject requestBody = new JSONObject();
        requestBody.put("codeList", codeList);

        String url = SC.v("ssm.white.paper.dim.query.url", "");
        String resultBody = HttpRequest.post(url)
                .header("u_token", token)
                .body(JSONObject.toJSONString(requestBody))
                .execute().body();

        JSONObject resultJson = JSONObject.parseObject(resultBody);
        if (resultJson != null) {
            List<DimBasicInfo> dimBasicInfoList = JSONArray.parseArray(JSON.toJSONString(resultJson.get("data")), DimBasicInfo.class);
            if (CollUtil.isNotEmpty(dimBasicInfoList)) {
                for (DimBasicInfo dimBasicInfo : dimBasicInfoList) {
                    ManualIndexWhitePaperEntity manualIndexWhitePaperEntity = new ManualIndexWhitePaperEntity();
                    manualIndexWhitePaperEntity.setIndexNo(dimBasicInfo.getDimCode());
                    manualIndexWhitePaperEntity.setIndexName(dimBasicInfo.getDimName());
                    manualIndexWhitePaperEntity.setInterpretation(dimBasicInfo.getDimDesc());
                    manualIndexWhitePaperEntity.setIsMeasure(Enabled.NO.getId());

                    buildDimItemValue(dimBasicInfo);

                    manualIndexWhitePaperEntity.setDimBasicInfo(dimBasicInfo);
                    dimWhitePaperEntityList.add(manualIndexWhitePaperEntity);
                }
            }

            dimBasicInfoList = null;
        }

        return dimWhitePaperEntityList;
    }

    /**
     * 只取前十个
     */
    public void buildDimItemValue(DimBasicInfo dimBasicInfo) {

        if (dimBasicInfo.getDimItemConfig() == null) {
            return;
        }

        String dimItemValue = dimBasicInfo.getDimItemConfig().getDimItemValue();
        if (StrUtil.isEmpty(dimItemValue)) {
            return;
        }

        List<String> dimItemValueList = Arrays.asList(dimItemValue.split(","));
        if (dimItemValueList.size() <= 10) {
            return;
        }

        String dimItemValueDisplay = StrUtil.join(",", dimItemValueList.subList(0, 10)) + "等";
        dimBasicInfo.getDimItemConfig().setDimItemValue(dimItemValueDisplay);

    }

    public Map<String, ManualIndexWhitePaperEntity> getManualIndexWhitePaperMap(List<String> fieldIds) {
        List<ManualIndexWhitePaperEntity> kpiIndexList = getManualIndexWhitePaperList(fieldIds);

        Map<String,ManualIndexWhitePaperEntity> resultMap = new HashMap<>();
        for(ManualIndexWhitePaperEntity manualIndexWhitePaperEntity : kpiIndexList ){
            String indexNo = manualIndexWhitePaperEntity.getIndexNo();
            resultMap.put(indexNo,manualIndexWhitePaperEntity);
        }
        return resultMap;
    }

    /**
     * 获取维度枚举值
     * @return
     */
    public Map<String,String> getDimEnumValueMap() {
        Map<String, String> dimEnumValueMap = new HashMap<>();
        List<ManualIndexWhitePaperEntity> dimEnumList = (List<ManualIndexWhitePaperEntity>) dao.queryObjectList("fieldDef.getDimEnumValues", null, DataSourceType.Data_Studio);
        if (CollUtil.isNotEmpty(dimEnumList)) {
//            for (ManualIndexWhitePaperEntity manualIndexWhitePaperEntity : dimEnumList) {
//
//                String dimEnumValue = manualIndexWhitePaperEntity.getDimEnumValues();
//                if (manualIndexWhitePaperEntity.getDimEnumCount() > 10) {
//                    dimEnumValue = dimEnumValue + "等";
//                }
//
//                dimEnumValueMap.put(manualIndexWhitePaperEntity.getIndexNo(), dimEnumValue);
//            }
        }
        return dimEnumValueMap;
    }

    public ResponseMessage queryAllFieldCodeKpiRel(FieldReq req) {
        List<String> fieldIds = req == null ?  null : req.getFieldIds();
        Map<String, ManualIndexWhitePaperEntity> resultMap = MapUtil.newHashMap();
        Map<String, ManualIndexWhitePaperEntity> indexCodeMap = getManualIndexWhitePaperMap(fieldIds);

        List<MetaField> metaFieldList = new ArrayList<>();
        if(CollUtil.isEmpty(fieldIds)){
            metaFieldList.addAll( SSDMetaCacheManager.getFieldsCache().values());
        }else {
            fieldIds.forEach(fieldId -> {
                MetaField field = SSDMetaCacheManager.getFieldsCache().get(fieldId);
                if (field != null) {
                    metaFieldList.add(field);
                }
            });
        }

        for(MetaField item : metaFieldList) {

            if (!Enabled.value(item.getIsShow())) {
                continue;
            }

            //扩展字段不处理
            if (StrUtil.isNotEmpty(item.getExtendSrcId())) {
                continue;
            }

            if (StrUtil.isNotEmpty(item.getKpiNo()) && indexCodeMap.get(item.getKpiNo()) != null) {
                resultMap.put(item.getCode(), indexCodeMap.get(item.getKpiNo()));
            }
        }

        return new ResponseMessage(resultMap);
    }

    /**
     * 同步指标平台替换数据
     * @param req
     * @return
     */
    public ResponseMessage asyncMgpMetric(AsyncMgpMetricReq req) {

        DataSourceType dataEnvDataSourceType = SSDUtil.getDataEnvDataSourceType();
        dao.update("fieldDef.asyncMgpMetric",req,dataEnvDataSourceType);
        return new ResponseMessage();
    }
}
