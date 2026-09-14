package com.bi.queryer.ssm.inspection.metric;

import com.bi.queryer.sys.common.ResponseMessage;
import com.bi.queryer.sys.enums.Enabled;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * 巡检指标
 */
@Controller
@Scope("prototype")
@RequestMapping("ssm/inspection/metric")
public class InspectionMetricController {

    @Autowired
    private InspectionMetricService inspectionMetricService;

    /**
     * 执行指标
     * @param tableId
     * @param dsKey 数据源 Key，不传默认 Doris_Master
     * @return
     */
    @RequestMapping("/execute")
    @ResponseBody
    public ResponseMessage execute(String tableId, String dsKey) {
        ResponseMessage result = new ResponseMessage();
        inspectionMetricService.execute(tableId, Enabled.NO.getId(), dsKey);
        return result;
    }

    /**
     * 执行指标,查询2年的数据
     * @param tableId
     * @param dsKey 数据源 Key，不传默认 Doris_Master
     * @return
     */
    @RequestMapping("/executeTwoYears")
    @ResponseBody
    public ResponseMessage executeTwoYears(String tableId, String dsKey) {
        ResponseMessage result = new ResponseMessage();
        inspectionMetricService.execute(tableId, Enabled.YES.getId(), dsKey);
        return result;
    }

}
