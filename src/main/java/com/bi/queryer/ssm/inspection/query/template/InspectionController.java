package com.bi.queryer.ssm.inspection.query.template;

import cn.hutool.core.util.StrUtil;
import com.bi.queryer.sys.common.ResponseMessage;
import com.bi.queryer.sys.db.DataSourceType;
import com.bi.queryer.util.BIUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

/**
 * 巡检模版前端控制器
 */
@Controller
@Scope("prototype")
@RequestMapping("ssm/inspection")
public class InspectionController {

    @Autowired
    private InspectionService inspectionService ;

    /**
     * 执行模版巡检
     * @param viewIds
     * @param queryMode
     * @param inspectDsKey 数据源 Key，不传默认 Doris_Master
     * @return
     */
    @RequestMapping("/template/execute")
    @ResponseBody
    public ResponseMessage execute(String viewIds, String queryMode, String inspectDsKey) {
        if (StrUtil.isBlank(inspectDsKey)) {
            inspectDsKey = DataSourceType.Doris_Master.getKey();
        }
        ResponseMessage result = new ResponseMessage();
        List<String> infos = inspectionService.execute(viewIds, queryMode, inspectDsKey);
        result.setData(BIUtil.listToStr(infos, "\n"));
        return result;
    }

}
