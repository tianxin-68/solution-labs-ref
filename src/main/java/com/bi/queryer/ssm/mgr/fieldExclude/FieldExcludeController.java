package com.bi.queryer.ssm.mgr.fieldExclude;

import com.bi.queryer.ssm.meta.MetaFieldExclude;
import com.bi.queryer.sys.base.BaseController;
import com.bi.queryer.sys.common.ResponseMessage;
import com.bi.queryer.sys.model.BIModelAndView;
import com.bi.queryer.util.component.datagrid.DataGrid;
import com.bi.queryer.util.component.datagrid.render.VueDataGridRender;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.util.Map;

/**
 * @author contributor
 */
@RestController
@Scope("prototype")
@RequestMapping("fieldExclude")
public class FieldExcludeController extends BaseController {

    @Autowired
    private FieldExcludeService fieldExcludeService = null;

    @RequestMapping("")
    public BIModelAndView execute(){
        BIModelAndView biModelAndView = new BIModelAndView("ssm/fieldExclude/fieldExclude_manager");
        return biModelAndView;
    }

    @RequestMapping("datagrid/vue")
    public void buildVueDataGrid() {
        DataGrid grid = fieldExcludeService.buildDataGrid(toStringMap());
        grid.setRenderClassName(VueDataGridRender.class.getName());
        String result = grid.toJSON().toString();
        writeJSON(result);
    }

    @RequestMapping("saveFieldExclude")
    public ResponseMessage saveFieldExclude(@RequestBody MetaFieldExclude metaFieldExclude){
        return  fieldExcludeService.saveFieldExclude(metaFieldExclude);
    }

    @RequestMapping("deleteFieldExclude")
    public ResponseMessage deleteFieldExclude(@RequestBody MetaFieldExclude metaFieldExclude ){
        return  fieldExcludeService.deleteFieldExclude(metaFieldExclude);
    }

    @RequestMapping("getFieldExcludeIds")
    public ResponseMessage getFieldExcludeIds(@RequestBody Map<String,String> map){
        return fieldExcludeService.getFieldExcludeIds(map);
    }

    @RequestMapping("exportMethod")
    public void exportMethod(@RequestParam(value = "searchTxt", required = false) String searchTxt,
                             @RequestParam(value = "type", required = false) String type,
                             HttpServletResponse response) {

        fieldExcludeService.exportMethod(searchTxt, type, response);
    }

    @RequestMapping("importMethod")
    public ResponseMessage importMethod(@RequestParam("files") MultipartFile files){
        return fieldExcludeService.importMethod(files);
    }
}
