package com.bi.queryer.sys.config.option;

import com.bi.queryer.sys.base.BaseController;
import com.bi.queryer.sys.common.ResponseMessage;
import com.bi.queryer.sys.config.SystemOption;
import com.bi.queryer.util.component.datagrid.DataGrid;
import com.bi.queryer.util.component.datagrid.render.VueDataGridRender;
import net.sf.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import java.util.Map;


/**
 * 系统选项的action
 */
@Controller
@Scope("prototype")
@RequestMapping("option")
public class OptionController extends BaseController {

    @Autowired
    private OptionService optionService = null;

    /**
     * 系统选项页面
     */
    @RequestMapping(value="")
    public ModelAndView execute() {
        ModelAndView v = new ModelAndView("/common/error_404");
        return v;
    }

    /**
     * 创建datagrid-vue
     */
    @RequestMapping("datagrid/vue")
    public void buildVueDataGrid(){
        DataGrid grid = optionService.buildDataGrid(params);
        grid.setRenderClassName(VueDataGridRender.class.getName());
        String result = grid.toJSON().toString();
        writeJSON(result);
    }

    @RequestMapping(value = "saveOption", method = RequestMethod.POST)
    public void saveOption(@RequestBody Map<String, Object> requestParams){

        ResponseMessage returnMsg = new ResponseMessage();
        SystemOption systemOption = new SystemOption();

        //系统配置对象赋值
        systemOption.setCode(jsonStringValue(requestParams, "code"));
        systemOption.setName(jsonStringValue(requestParams, "name"));
        systemOption.setValue(jsonStringValue(requestParams, "value"));
        systemOption.setRemark(jsonStringValue(requestParams, "remark"));

        //判断是新增还是编辑
        String type = jsonStringValue(requestParams, "type");
        //新增
        if("add".equalsIgnoreCase(type)){
            returnMsg = optionService.insertOption(systemOption);
        }else{
            returnMsg = optionService.updateOption(systemOption);
        }
        writeJSON(JSONObject.fromObject(returnMsg));
    }

    private String jsonStringValue(Map<String, Object> requestParams, String key){
        Object value = requestParams.get(key);
        return value == null ? null : value.toString();
    }

    @RequestMapping("deleteOptionByCode")
    public void deleteOptionByCode(){

        String code  = stringValue("code");
        writeJSON(JSONObject.fromObject(optionService.deleteOptionByCode(code)));

    }
}
