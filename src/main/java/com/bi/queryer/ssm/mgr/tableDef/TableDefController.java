package com.bi.queryer.ssm.mgr.tableDef;

import com.bi.queryer.ssm.meta.MetaTable;
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
import java.util.List;
import java.util.Map;

/**
 * 表管理
 * @author contributor
 */
@RestController
@Scope("prototype")
@RequestMapping("tableDef")
public class TableDefController extends BaseController {

    @Autowired
    private TableDefService tableDefService = null;

    @RequestMapping("")
    public BIModelAndView execute(){
        BIModelAndView biModelAndView = new BIModelAndView("ssm/tableDef/tableDef_manager");
        return biModelAndView;
    }

    @RequestMapping("datagrid/vue")
    public void buildVueDataGrid() {
        DataGrid grid = tableDefService.buildDataGrid(toStringMap());
        grid.setRenderClassName(VueDataGridRender.class.getName());
        String result = grid.toJSON().toString();
        writeJSON(result);
    }

    @RequestMapping("saveTableDef")
    public ResponseMessage saveTableDef(@RequestBody MetaTable metaTable){
        return tableDefService.saveTableDef(metaTable);
    }

    @RequestMapping("deleteTableDefByTableId")
    public ResponseMessage deleteTableDefByTableId(String tableId){
        return tableDefService.deleteTableDefByTableId(tableId);
    }

    @RequestMapping("updateTableDefActiveType")
    public ResponseMessage updateTableDefActiveType(@RequestBody Map<String,Object> map){
        return tableDefService.updateTableDefActiveType(map);
    }

    @RequestMapping("queryTableList")
    public ResponseMessage queryTableList(@RequestBody Map<String,Object> map) {
        return tableDefService.queryTableList(map);
    }

    @RequestMapping("exportMethod")
    public void exportMethod(@RequestParam(value = "searchTxt", required = false) String searchTxt,
                             @RequestParam(value = "type", required = false) String type,
                             HttpServletResponse response) {

        tableDefService.exportMethod(searchTxt, type, response);
    }

    @RequestMapping("importMethod")
    public ResponseMessage importMethod(@RequestParam("files") MultipartFile files){
        return tableDefService.importMethod(files);
    }

    /**
     * 查询etl任务列表
     * @return
     */
    @RequestMapping("queryEtljobsList")
    public ResponseMessage queryEtljobsList(){
        String jobName = stringValue("jobName");
        return tableDefService.queryEtljobsList(jobName);
    }

    /**
     * 通过表id获取etl集合
     * @return
     */
    @RequestMapping("getEtlJobListByTableId")
    public ResponseMessage getEtlJobListByTableId(){
        String tableId = stringValue("tableId");
        return tableDefService.getEtlJobListByTableId(tableId);
    }

    /**
     * 批量判断 etl 作业是否仍被表引用（引用表已删除的不算）
     * @param etlJobList etl 作业名列表
     * @return etlJob -> 是否被引用
     */
    @RequestMapping("checkEtlJobsReferenced")
    public ResponseMessage checkEtlJobsReferenced(@RequestBody List<String> etlJobList){
        return tableDefService.checkEtlJobsReferenced(etlJobList);
    }

}
