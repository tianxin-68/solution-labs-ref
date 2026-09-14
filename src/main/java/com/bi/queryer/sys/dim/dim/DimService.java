package com.bi.queryer.sys.dim.dim;

import com.bi.queryer.sys.base.BaseDao;
import com.bi.queryer.sys.common.ResponseMessage;
import com.bi.queryer.sys.config.SystemConfig;
import com.bi.queryer.sys.db.DBType;
import com.bi.queryer.sys.db.DataSourceType;
import com.bi.queryer.sys.dim.vo.Dim;
import com.bi.queryer.sys.dim.vo.DimItem;
import com.bi.queryer.sys.user.UserManager;
import com.bi.queryer.sys.user.vo.User;
import com.bi.queryer.util.*;
import com.bi.queryer.util.component.datagrid.DataGrid;
import com.bi.queryer.util.component.datagrid.DataGridColumn;
import com.bi.queryer.util.component.datagrid.IDataGridDataSetProvider;
import com.bi.queryer.util.network.HttpUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.beanutils.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author contributor
 */
@Service
public class DimService implements IDataGridDataSetProvider {

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
        grid.setPagination(true);// 使用分页
        grid.setShowHideColumnButton(false);
        grid.setFormatLink(false);


        DataGridColumn column = new DataGridColumn("cfgId", "cfgId", -1); // 添加列
        column.setHidden(true); // id不需要显示
        grid.addColumn(column);

        column = new DataGridColumn("moduleCode", "模块编码", -1, "left"); // 添加列
        grid.addColumn(column);

        column = new DataGridColumn("moduleName", "模块名称", -1, "left");
        grid.addColumn(column);

        column = new DataGridColumn("dimDatasource", "数据源", -1, "left");
        for (DataSourceType dataSourceType: DataSourceType.values() ) {
            column.addValueDisplayRule(dataSourceType.toString(),dataSourceType.getDesc());
        }
        grid.addColumn(column);

        column = new DataGridColumn("dimCode", "维度编码", -1, "left");
        grid.addColumn(column);

        column = new DataGridColumn("dimName", "维度名称", -1, "left");
        grid.addColumn(column);

        column = new DataGridColumn("dimTableName", "维度表名", 160, "left");
        grid.addColumn(column);

        column = new DataGridColumn("dimTableSql", "维度表sql", -1, "left");
        column.setClob(true);
        grid.addColumn(column);

        column = new DataGridColumn("dimDataApi", "维度数据api", -1, "left");
        column.setClob(true);
        grid.addColumn(column);

        column = new DataGridColumn("codeFieldName", "维度编码列字段名", -1, "left");
        grid.addColumn(column);

        column = new DataGridColumn("valueFieldName", "维度值列字段名", -1, "left");
        grid.addColumn(column);

        column = new DataGridColumn("parentCodeFieldName", "上级维度编码列字段名", 160, "left");
        grid.addColumn(column);

        column = new DataGridColumn("rootValue", "根节点值", -1, "left");
        grid.addColumn(column);

        column = new DataGridColumn("isActive", "启用", -1); // 添加列
        column.addValueDisplayRule("1", "启用")
                .addValueDisplayRule("0", "禁用");
        grid.addColumn(column);

        column = new DataGridColumn("createdTime", "创建日期", 140);
        grid.addColumn(column);

        column = new DataGridColumn("createdBy", "创建人", -1);
        grid.addColumn(column);
        column = new DataGridColumn("updatedTime", "修改时间", 140);
        grid.addColumn(column);
        column = new DataGridColumn("updatedBy", "修改人", -1);
        grid.addColumn(column);

        column = new DataGridColumn("operator", "操作");
        column.setOperator(true);
        column.addOperation("新增","addRow")
                .addOperation("修改","editRow")
                .addOperation("删除","deleteRow");


        column.setFrozen(true);
        column.setCanHide(false);
        column.setWidth(160);
        grid.addColumn(column);

        grid.setDataSetProvider(this, paramMap);
        return grid;
    }

    @Override
    public List<Dim> getDataSet(Map queryParamMap) {
        buidQueryParam(queryParamMap);
        return dao.queryMapList("dim.queryDimList", queryParamMap);
    }

    protected void buidQueryParam(Map paramMap) {
    }

    @Override
    public Integer getDataSetTotalSize(Map queryParamMap) {
        buidQueryParam(queryParamMap);
        return  dao.queryCount("dim.queryDimListCount", queryParamMap);
    }

    public ResponseMessage saveDim(Dim dim){
        ResponseMessage result = new ResponseMessage();

        try {

            int count = dao.queryCount("dim.checkDimCodeExist",dim);

            if(count>0){
                return new ResponseMessage(false,"该模块下维度编码已存在");
            }

            User user = UserManager.get();

            if (BIUtil.isEmpty(dim.getCfgId())) {

                String id = Guid.id();
                dim.setCfgId(id);
                dim.setCreatedBy(user.getName());
                dao.insert("dim.insertDim", dim);

                return result;
            }


            dim.setUpdatedBy(user.getName());
            dao.update("dim.updateDim", dim);


        }catch (Exception e){
            return new ResponseMessage(false, e.getMessage());
        }

        return result;
    }

    public ResponseMessage deleteDimByCfgid(String cfgId){
        ResponseMessage result = new ResponseMessage();

        try {

            dao.delete("dim.deleteDimByCfgid",cfgId);
        }catch (Exception e){
            return new ResponseMessage(false, e.getMessage());
        }
        return result;
    }

    public ResponseMessage buildDataAuthTree(){
        ResponseMessage result = new ResponseMessage();

        String dimCode = SystemConfig.value("app.dimCode");
        List<DimItem> data = (List<DimItem>)dao.queryObjectList("dim.getModuleDimList",dimCode);
        result.setData(data);
        return result;
    }

    public ResponseMessage getDimDetailList(Map<String,String> map) {
        ResponseMessage result = new ResponseMessage();

        try{

            Dim dim = (Dim) dao.queryObject("dim.getDimInfoByCode", map);
            if (dim != null) {

                //有api接口的从api接口获取数据
                if(BIUtil.isNotEmpty(dim.getDimDataApi())){

                    Map mapPost = new HashMap();
                    Map<String,String> mapHeader = new HashMap<>();
                    mapHeader.put(BIConsts.User_Token,map.get("token"));
                    String resultSql = HttpUtil.doPost(dim.getDimDataApi(),mapPost,"",mapHeader,10000);

                    if(BIUtil.isNotEmpty(resultSql)){
                        result = JSONObject.parseObject(resultSql, ResponseMessage.class);
                    }

                }else{
                    DataSourceType dataSourceType = DataSourceType.getType(dim.getDimDatasource());
                    List<DimItem> list = new ArrayList<>();

                    //presto的参数需要为map
                    if(DBType.Presto == DBType.getType(dataSourceType.getDialect())){
                        Map<String,Object> mapPresto = BeanUtils.describe(dim);
                        List<BIMap> biMapList = (List<BIMap>)dao.queryObjectList("dim.getDimTableDetail",mapPresto,dataSourceType);

                        //查询结果转换，bimap -> dimitem
                        if(BIUtil.isNotEmpty(biMapList)){
                            for (BIMap bIMap :biMapList ) {
                                DimItem dimItem = JSONObject.parseObject(JSONObject.toJSONString(bIMap), DimItem.class);
                                list.add(dimItem);
                            }
                        }

                    }else{
                        list = (List<DimItem>)dao.queryObjectList("dim.getDimTableDetail",dim,dataSourceType);
                    }

                    JSONArray data = new JSONArray();

                    list = bulidDimTree(list,dim);
                    //设置根节点
                    JSONObject rootJSON = new JSONObject();
                    rootJSON.put("id","-1");
                    rootJSON.put("label","全部");
                    rootJSON.put("children", BIUtil.toJSONArray(list));
                    data.add(rootJSON);

                    result.setData(data);
                }


            }
        }catch(Exception e){
            return new ResponseMessage(false, e.getMessage());
        }

        return result;
    }


    /**
     * 两层循环实现建树
     * @param treeNodes 传入的树节点列表
     * @return
     */
    public static List<DimItem> bulidDimTree(List<DimItem> treeNodes, Dim dim) {

        List<DimItem> trees = new ArrayList<DimItem>();
        String rootValue = dim.getRootValue();

        for (DimItem treeNode : treeNodes) {

            //添加模块编码名称与维度编码名称
            treeNode.setModuleCode(dim.getModuleCode());
            treeNode.setDimCode(dim.getDimCode());
            treeNode.setModuleName(dim.getModuleName());
            treeNode.setDimName(dim.getDimName());

            if (BIUtil.isEmpty(rootValue)) {
                if (BIUtil.isEmpty(treeNode.getParentId())) {
                    trees.add(treeNode);
                }
            } else {
                if (rootValue.equalsIgnoreCase(treeNode.getParentId())) {
                    trees.add(treeNode);
                }
            }

            for (DimItem it : treeNodes) {
                if (it.getParentId() != null && it.getParentId().equals(treeNode.getId())) {
                    if (treeNode.getChildren() == null) {
                        treeNode.setChildren(new ArrayList<DimItem>());
                    }
                    treeNode.getChildren().add(it);
                }
            }
        }
        return trees;
    }
}
