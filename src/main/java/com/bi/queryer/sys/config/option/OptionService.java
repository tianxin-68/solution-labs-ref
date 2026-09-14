package com.bi.queryer.sys.config.option;

import com.bi.queryer.sys.base.BaseDao;
import com.bi.queryer.sys.common.ResponseMessage;
import com.bi.queryer.sys.config.SystemOption;
import com.bi.queryer.sys.db.DataSourceType;
import com.bi.queryer.sys.enums.Enabled;
import com.bi.queryer.sys.startup.SystemInitializer;
import com.bi.queryer.sys.user.UserManager;
import com.bi.queryer.sys.user.vo.User;
import com.bi.queryer.util.UpperCaseMap;
import com.bi.queryer.util.component.datagrid.DataGrid;
import com.bi.queryer.util.component.datagrid.DataGridColumn;
import com.bi.queryer.util.component.datagrid.IDataGridDataSetProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class OptionService implements IDataGridDataSetProvider {


    @Autowired
    private BaseDao dao;

    /**
     * 构建datagrid
     * @param paramMap
     * @return
     */
    public DataGrid buildDataGrid(Map paramMap){
        DataGrid grid = null;
        if("true".equalsIgnoreCase(paramMap.get("autoSize") + "")){
            grid = new DataGrid(-1, -1);
        }else{
            grid = new DataGrid(920, 400);
        }
        grid.setPagination(true);// 使用分页
        grid.setShowBorder(true);
        grid.setFormatLink(false);

        // 添加列
        DataGridColumn col = new DataGridColumn("OPT_CODE", "选项编码", -1, "left");
        grid.addColumn(col);

        col = new DataGridColumn("OPT_NAME", "选项名称", -1, "left");
        grid.addColumn(col);

        col = new DataGridColumn("OPT_VALUE", "选项值", -1, "left");
        grid.addColumn(col);

        col = new DataGridColumn("REMARK", "备注", 200);
        grid.addColumn(col);

        col = new DataGridColumn("operator", "操作");
        col.setOperator(true);

        //只有管理员才可以修改超级密码
        User user = UserManager.get();
        if(Enabled.value(user.getIsAdmin())){
            col.addOperation("修改", "editRow")
                    .addOperation("删除", "deleteRow");
        }else{
            String expression = "value('OPT_CODE') != 'login.super.pwd'";
            col.addOperation("修改", "editRow",expression)
                    .addOperation("删除", "deleteRow",expression);
        }


        col.setFrozen(true);
        col.setCanHide(false);
        grid.addColumn(col);

        // 设置数据集provider
        grid.setDataSetProvider(this, paramMap);

        return grid;
    }

    protected void buidQueryParam(Map paramMap){
        // TODO
        // 其他参数
        // 不用添加分页参数
    }

    @Override
    public List<UpperCaseMap> getDataSet(Map queryParamMap) {
        buidQueryParam(queryParamMap);
        return dao.queryMapList("option.queryOptionList", queryParamMap, DataSourceType.Default);
    }

    @Override
    public Integer getDataSetTotalSize(Map queryParamMap) {
        buidQueryParam(queryParamMap);
        return dao.queryCount("option.queryOptionListCount", queryParamMap, DataSourceType.Default);
    }

    public ResponseMessage insertOption(SystemOption systemOption){
        ResponseMessage result = new ResponseMessage();

        try {

            //编号不能重复
            String code = systemOption.getCode();
            Integer count = dao.queryCount("option.checkOptionCodeValidate",code);

            if(count>0){
                return new ResponseMessage(false,"编号不能重复");
            }

            dao.update("option.insertOption",systemOption,DataSourceType.Default);
            flushSystemConfig();

        }catch (Exception e){
            result = new ResponseMessage(false,e.getMessage());
        }

        return result;
    }

    public ResponseMessage updateOption(SystemOption systemOption){
        ResponseMessage result = new ResponseMessage();

        try {
            if("login.super.pwd".equalsIgnoreCase(systemOption.getCode())){
                if(!systemOption.isChangeValue()){
                    systemOption.setValue((String) dao.queryObject("option.querySuperPassWord",null));
                }
            }

            dao.update("option.updateOption",systemOption,DataSourceType.Default);
            flushSystemConfig();
        }catch (Exception e){
            result = new ResponseMessage(false,e.getMessage());
        }

        return result;
    }

    /**
     * 删除系统选项
     * @param code 选项编码
     * @return
     */
    public ResponseMessage deleteOptionByCode(String code){
        ResponseMessage result = new ResponseMessage();

        try {
            dao.delete("option.deleteOptionByCode",code);
            flushSystemConfig();
        }catch (Exception e){
            result = new ResponseMessage(false,e.getMessage());
        }

        return result;
    }

    /**
     * 获取所有的系统选项
     * @return
     */
    public List<SystemOption> getAllSystemOption() {
        List<SystemOption> list = (List<SystemOption>) dao.queryObjectList("option.getAllSystemOption", null);
        if(list == null){
            return new ArrayList<>();
        }
        return list;
    }

    /**
     * 刷新系统缓存
     */
    public void flushSystemConfig(){
        Map<String,String> params = new HashMap<>();
        params.put("_modules_key","SystemConfig");
        SystemInitializer.initialize(params);
    }
}
