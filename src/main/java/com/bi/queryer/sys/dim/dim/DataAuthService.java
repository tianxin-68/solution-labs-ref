package com.bi.queryer.sys.dim.dim;

import com.bi.queryer.sys.base.BaseDao;
import com.bi.queryer.sys.common.ResponseMessage;
import com.bi.queryer.sys.dim.vo.Dim;
import com.bi.queryer.util.StringUtil;
import com.bi.queryer.util.component.datagrid.DataGrid;
import com.bi.queryer.util.component.datagrid.DataGridColumn;
import com.bi.queryer.util.component.datagrid.IDataGridDataSetProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * @author contributor
 */
@Service
public class DataAuthService implements IDataGridDataSetProvider {

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


        DataGridColumn column = new DataGridColumn("authId", "authId", -1); // 添加列
        column.setHidden(true); // id不需要显示
        grid.addColumn(column);

        column = new DataGridColumn("ownerType", "owner类型", -1, "left"); // 添加列
        grid.addColumn(column);

        column = new DataGridColumn("ownerId", "ownerID", -1, "left");
        grid.addColumn(column);

        column = new DataGridColumn("moduleCode", "模块编码", -1, "left");
        grid.addColumn(column);

        column = new DataGridColumn("moduleName", "模块名称", -1, "left");
        grid.addColumn(column);

        column = new DataGridColumn("dimCode", "维度编码", -1, "left");
        grid.addColumn(column);

        column = new DataGridColumn("dimName", "维度名称", -1, "left");
        grid.addColumn(column);

        column = new DataGridColumn("itemCode", "维成员编码", -1, "left");
        grid.addColumn(column);

        column = new DataGridColumn("itemValue", "维成员值", -1, "left");
        grid.addColumn(column);

        column = new DataGridColumn("remark", "备注", -1, "left");
        grid.addColumn(column);

        column = new DataGridColumn("createdTime", "创建日期", 140);
        grid.addColumn(column);

        column = new DataGridColumn("createdBy", "创建人", -1);
        grid.addColumn(column);

        column = new DataGridColumn("operator", "操作");
        column.setOperator(true);
        column.addOperation("删除","deleteRow");


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
        return dao.queryMapList("dim.queryDataAuthList", queryParamMap);
    }

    protected void buidQueryParam(Map paramMap) {
    }

    @Override
    public Integer getDataSetTotalSize(Map queryParamMap) {
        buidQueryParam(queryParamMap);
        return  dao.queryCount("dim.queryDataAuthListCount", queryParamMap);
    }

    public ResponseMessage deleteDataAuthByAuthId(String authId){
        ResponseMessage result = new ResponseMessage();
        dao.delete("dim.deleteDataAuthByAuthId",authId);
        return  result;
    }
}
