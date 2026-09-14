package com.bi.queryer.ssm.mgr.tableRel;

import com.bi.queryer.ssm.enums.FieldJoinType;
import com.bi.queryer.ssm.meta.MetaTable;
import com.bi.queryer.ssm.meta.MetaTableRelation;
import com.bi.queryer.ssm.util.SSDUtil;
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

/**
 * @author contributor
 */
@Service
public class TableRelService implements IDataGridDataSetProvider {

    @Autowired
    private BaseDao dao;

    /**
     * 构建datagrid
     */
    public DataGrid buildDataGrid(Map<String, String> paramMap) {
        DataGrid grid = new DataGrid(-1, -1);
        String width = paramMap.get("width");
        String height = paramMap.get("height");
        if(!StringUtil.isEmpty(width) && !StringUtil.isEmpty(height)){
            grid.setWidth(Integer.valueOf(width));
            grid.setHeight(Integer.valueOf(height));
        }
        grid.setAutoSize(true);
        grid.setShowExport(false);
        grid.setFormatLink(false);
        grid.setPagination(true);// 使用分页
        grid.setShowHideColumnButton(false);


        DataGridColumn column = new DataGridColumn("id", "id", -1); // 添加列
        column.setHidden(true); // id不需要显示
        grid.addColumn(column);

        column = new DataGridColumn("primaryTableName", "表名", -1, "left"); // 添加列
        column.setRemoteSortable(true);
        grid.addColumn(column);

        column = new DataGridColumn("primaryTableId", "表id", -1, "left");
        column.setHidden(true);
        grid.addColumn(column);

        column = new DataGridColumn("subTableName", "关联表名", -1, "left");
        column.setRemoteSortable(true);
        grid.addColumn(column);

        column = new DataGridColumn("subTableId", "关联表名id", -1, "left");
        column.setHidden(true);
        grid.addColumn(column);

        column = new DataGridColumn("primaryFieldName", "主表关联字段", -1, "left"); // 添加列
        column.setRemoteSortable(true);
        grid.addColumn(column);

        column = new DataGridColumn("primaryFieldId", "主表关联字段id", -1, "left");
        column.setHidden(true);
        grid.addColumn(column);

        column = new DataGridColumn("subFieldName", "从表关联字段", -1,"left");
        column.setRemoteSortable(true);
        grid.addColumn(column);

        column = new DataGridColumn("subFieldId", "从表关联字段id", -1, "left");
        column.setHidden(true);
        grid.addColumn(column);

        column = new DataGridColumn("subFieldName2", "从表关联字段2", -1,"left");
        column.setRemoteSortable(true);
        grid.addColumn(column);

        column = new DataGridColumn("subFieldId2", "从表关联字段id2", -1, "left");
        column.setHidden(true);
        grid.addColumn(column);

        column = new DataGridColumn("joinExpression", "关联类型", -1,"left");
        column.setRemoteSortable(true);
        grid.addColumn(column);

        column = new DataGridColumn("joinOnType", "关联匹配类型", -1,"left");
        column.setRemoteSortable(true);
        grid.addColumn(column);

        column = new DataGridColumn("weight", "权重", -1,"left");
        column.setRemoteSortable(true);
        grid.addColumn(column);

        column = new DataGridColumn("isActive", "是否启用", -1); // 添加列
        column.addValueDisplayRule("1", "启用")
                .addValueDisplayRule("0", "<span style='color:red'>禁用</span>");
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
        column.addOperation("修改","editRow")
                .addOperation("删除","deleteRow")
                .addOperation("禁用/启用","activeRow");

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
        DataSourceType dataEnvDataSourceType = SSDUtil.getDataEnvDataSourceType();
        return dao.queryMapList("tableRel.queryTableRelList", queryParamMap,dataEnvDataSourceType);
    }

    protected void buidQueryParam(Map paramMap) {
    }

    @Override
    public Integer getDataSetTotalSize(Map queryParamMap) {
        buidQueryParam(queryParamMap);
        DataSourceType dataEnvDataSourceType = SSDUtil.getDataEnvDataSourceType();
        return  dao.queryCount("tableRel.queryTableRelListCount", queryParamMap,dataEnvDataSourceType);
    }

    public ResponseMessage saveTableRel(MetaTableRelation metaTableRelation){

        ResponseMessage result = new ResponseMessage();
        User user = UserManager.get();
        DataSourceType dataEnvDataSourceType = SSDUtil.getDataEnvDataSourceType();

        try {

            //新增
            if(BIUtil.isEmpty(metaTableRelation.getId())){

                metaTableRelation.setId(Guid.id());
                metaTableRelation.setCreatedBy(user.getName());
                dao.insert("tableRel.insertIntoTableRel",metaTableRelation,dataEnvDataSourceType);
                return result;
            }

            metaTableRelation.setUpdatedBy(user.getName());
            dao.update("tableRel.updateTableRel",metaTableRelation,dataEnvDataSourceType);

        }catch (Exception e){
            return  new ResponseMessage(false,e.getMessage());
        }

        return result;
    }

    public ResponseMessage deleteTableRelById(String id){

        ResponseMessage result = new ResponseMessage();

        try {

            DataSourceType dataEnvDataSourceType = SSDUtil.getDataEnvDataSourceType();
            dao.delete("tableRel.deleteTableRelById",id,dataEnvDataSourceType);
        }catch (Exception e){
            return  new ResponseMessage(false,e.getMessage());
        }

        return result;
    }

    public ResponseMessage updateTableRelActiveType(Map<String,Object> map){

        ResponseMessage result = new ResponseMessage();

        try {
            DataSourceType dataEnvDataSourceType = SSDUtil.getDataEnvDataSourceType();
            dao.update("tableRel.updateTableRelActiveType",map,dataEnvDataSourceType);
        }catch (Exception e){
            return  new ResponseMessage(false,e.getMessage());
        }

        return  result;
    }

    public void exportMethod(String searchTxt, String type, HttpServletResponse response){

        Map<String,Object> map = new HashMap<>();
        map.put("searchTxt",searchTxt);
        map.put("pageRowLower",0);
        map.put("prePageSize",Integer.MAX_VALUE);

        DataSourceType dataEnvDataSourceType = SSDUtil.getDataEnvDataSourceType();
        List<MetaTableRelation> list = (List<MetaTableRelation>)dao.queryObjectList("tableRel.queryTableRelList",map,dataEnvDataSourceType);

        String[] cloumnNames = new String[] { "关系编号", "主表编号", "从表编号", "主表关联字段", "从表关联字段", "从表关联字段2", "关联类型", "关联匹配类型","权重值","启用" };

        List<String[]> cloumnValues = new ArrayList<>();

        if (list != null && list.size() > 0) {

            for (MetaTableRelation metaTableRelation : list) {
                String[] strs = new String[]{
                        metaTableRelation.getId(),
                        metaTableRelation.getPrimaryTableId(),
                        metaTableRelation.getSubTableId(),
                        metaTableRelation.getPrimaryFieldId(),
                        metaTableRelation.getSubFieldId(),
                        metaTableRelation.getSubFieldId2(),
                        metaTableRelation.getJoinExpression(),
                        metaTableRelation.getJoinOnType(),
                        BIUtil.nvl(metaTableRelation.getWeight(), ""),
                        metaTableRelation.getIsActive() == 0 ? "否" : "是"
                };
                cloumnValues.add(strs);
            }
        }

        ExcelHelper.exportExcel(cloumnNames,cloumnValues,type,response);

    }

    public ResponseMessage importMethod(MultipartFile files){

        ResponseMessage result = new ResponseMessage();

        // 获取前端传过来的file
        InputStream inputStream = null;
        StringBuilder msg = new StringBuilder();

        try{

            if(files!=null) {

                String fileName = new String(files.getOriginalFilename().getBytes("ISO-8859-1"), "UTF-8");
                inputStream = files.getInputStream();

                List<MetaTableRelation> list = ExcelHelper.convertToList(MetaTableRelation.class, fileName, inputStream, 1, 8, 0);

                if (list.size() == 0) {
                    return new ResponseMessage(false,"文件内容为空");
                }

                for (int i = 0; i < list.size(); i++) {
                    MetaTableRelation metaTableRelation = list.get(i);
                    if(BIUtil.isEmpty(metaTableRelation.getPrimaryTableId())){
                        msg.append("第" + (i + 1) + "行主表编号为空,");
                    }
                    if(BIUtil.isEmpty(metaTableRelation.getSubTableId())){
                        msg.append("第" + (i + 1) + "行从表编号为空,");
                    }
                    if(BIUtil.isEmpty(metaTableRelation.getPrimaryFieldId())){
                        msg.append("第" + (i + 1) + "行主表关联字段为空,");
                    }
                    if(BIUtil.isEmpty(metaTableRelation.getSubFieldId())){
                        msg.append("第" + (i + 1) + "行从表关联字段为空,");
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
                            for (MetaTableRelation metaTableRelation : list) {

                                if(BIUtil.isEmpty(metaTableRelation.getJoinExpression())){
                                    metaTableRelation.setJoinExpression(FieldJoinType.LeftJoin.getCode());
                                }

                                //插入数据库
                                if(BIUtil.isEmpty(metaTableRelation.getId())){
                                    metaTableRelation.setId(Guid.id());
                                }

                                metaTableRelation.setCreatedBy(user.getName());
                                metaTableRelation.setUpdatedBy(user.getName());

                                int count = dao.queryCount("tableRel.checkTableRelExist", metaTableRelation.getId(),dataEnvDataSourceType);
                                if (count == 0) {
                                    dao.insert("tableRel.insertIntoTableRel",metaTableRelation,dataEnvDataSourceType);
                                } else {
                                    dao.update("tableRel.updateTableRel",metaTableRelation,dataEnvDataSourceType);
                                }
                            }
                        }
                    });


                }
            }

        }catch (Exception e){
            return new ResponseMessage(false,e.getMessage());
        }

        return result;
    }


}
