package com.bi.queryer.ssm.mgr.hot;

import com.bi.queryer.ssm.engine.accelerate.hot.HotTableCacheManager;
import com.bi.queryer.ssm.engine.accelerate.hot.HotTableManager;
import com.bi.queryer.ssm.mgr.hot.model.HotTableInfoRequest;
import com.bi.queryer.sys.common.ResponseMessage;
import com.bi.queryer.sys.interceptor.FreeCheckAuthority;
import com.bi.queryer.util.BIUtil;
import com.alibaba.fastjson.JSON;
import org.springframework.context.annotation.Scope;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @Author contributor
 * @Date 17:09 2024/8/19
 * @Description 热数据表请求入口类
 **/

@RestController
@Scope("prototype")
@RequestMapping("ssm/hot/table")
public class HotTableInfoController {

    /**
     * 每天01:00定时调度（配置到datastudio-作业调度中）
     * @return
     */
    @RequestMapping("initialize")
    @ResponseBody
    public ResponseMessage initialize(@RequestBody HotTableInfoRequest req) {
        ResponseMessage result = new ResponseMessage();
        List<String> resetInfos = HotTableManager.initialize(req.getSourceTableName());
        result.setData(BIUtil.listToStr(resetInfos, "\n"));
        return result;
    }

    /**
     * 重置数据完成时间
     * @param req
     * @return
     */
    @RequestMapping("resetDataFinishTime")
    @ResponseBody
    @FreeCheckAuthority
    public ResponseMessage resetDataFinishTime(@RequestBody HotTableInfoRequest req) {
        System.out.println("resetDataFinishTime入参:" + JSON.toJSONString(req));
        ResponseMessage result = new ResponseMessage();
        HotTableManager.onEtlJobFinish(req.getEtlJobName(), null);
        return result;
    }

}
