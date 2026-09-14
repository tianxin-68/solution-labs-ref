package com.bi.queryer.ssm.mgr.tableRel;

import com.bi.queryer.ssm.meta.MetaTableRelation;
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
@RequestMapping("tableRel")
public class TableRelController extends BaseController {

    @Autowired
    private TableRelService tableRelService = null;

    @RequestMapping("")
    public BIModelAndView execute(){
        BIModelAndView biModelAndView = new BIModelAndView("ssm/tableRel/tableRel_manager");
        return biModelAndView;
    }

    @RequestMapping("datagrid/vue")
    public void buildVueDataGrid() {
        DataGrid grid = tableRelService.buildDataGrid(toStringMap());
        grid.setRenderClassName(VueDataGridRender.class.getName());
        String result = grid.toJSON().toString();
        writeJSON(result);
    }

    @RequestMapping("saveTableRel")
    public ResponseMessage saveTableRel(@RequestBody MetaTableRelation metaTableRelation){
        return tableRelService.saveTableRel(metaTableRelation);
    }

    @RequestMapping("deleteTableRelById")
    public ResponseMessage deleteTableRelById(){
        String id = stringValue("id");
        return tableRelService.deleteTableRelById(id);
    }

    @RequestMapping("updateTableRelActiveType")
    public ResponseMessage updateTableRelActiveType(@RequestBody Map<String,Object> map){
        return tableRelService.updateTableRelActiveType(map);
    }

    @RequestMapping("exportMethod")
    public void exportMethod(@RequestParam(value = "searchTxt", required = false) String searchTxt,
                             @RequestParam(value = "type", required = false) String type,
                             HttpServletResponse response) {

        tableRelService.exportMethod(searchTxt, type, response);
    }

    @RequestMapping("importMethod")
    public ResponseMessage importMethod(@RequestParam("files") MultipartFile files){
        return tableRelService.importMethod(files);
    }

}
