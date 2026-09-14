package com.bi.queryer.ssm.inspection.table;

import com.bi.queryer.sys.common.ResponseMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * 巡检表分区
 */
@Controller
@Scope("prototype")
@RequestMapping("ssm/inspection/table")
public class InspectionTableController {

    @Autowired
    private InspectionTableService inspectionTableService;

    /**
     * 执行表分区巡检
     * @param tableName
     * @param dsKey 数据源 Key，不传默认 Doris_Master
     * @return
     */
    @RequestMapping("/execute")
    @ResponseBody
    public ResponseMessage execute(String tableName, String dsKey) {
        ResponseMessage result = new ResponseMessage();
        inspectionTableService.execute(tableName, dsKey);
        return result;
    }

}
