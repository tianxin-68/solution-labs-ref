package com.bi.queryer.ssm.mgr.hot;

import com.bi.queryer.ssm.engine.accelerate.hot.repair.HotTableRepairManager;
import com.bi.queryer.ssm.mgr.hot.model.HotTableRepairRequest;
import com.bi.queryer.sys.common.ResponseMessage;
import com.bi.queryer.util.BIUtil;
import org.springframework.context.annotation.Scope;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author contributor
 * @Date 11:11 2024/10/11
 * @Description 热表补数请求处理类
 **/
@RestController
@Scope("prototype")
@RequestMapping("ssm/hot/table/repair/task")
public class HotTableRepairTaskController {
    /**
     * 每天6:00开始初始化（上datastudio调度)
     * @param req
     * @return
     */
    @RequestMapping("initialize")
    @ResponseBody
    public ResponseMessage initialize(@RequestBody HotTableRepairRequest req) {
        ResponseMessage result = new ResponseMessage();
        List<String> taskInfos = HotTableRepairManager.initialize(req.getSourceTableName());
        result.setData(BIUtil.listToStr(taskInfos, "\n"));
        return result;
    }

    /**
     * 每天09:00~23:00开始，每个15分钟提交一次补数任务（上datastudio调度）
     * @param req
     * @return
     */
    @RequestMapping("submit")
    @ResponseBody
    public ResponseMessage submit(@RequestBody HotTableRepairRequest req) {
        ResponseMessage result = new ResponseMessage();
        List<String> taskInfos = HotTableRepairManager.submit(req.getSourceTableName());
        result.setData(BIUtil.listToStr(taskInfos, "\n"));
        return result;
    }

    @RequestMapping("cancel")
    @ResponseBody
    public ResponseMessage cancel(@RequestBody HotTableRepairRequest req) {
        ResponseMessage result = new ResponseMessage();
        List<String> canelist =  HotTableRepairManager.cancel(req.getSourceTableName());
        result.setData(BIUtil.listToStr(canelist, "\n"));
        return result;
    }

    @RequestMapping("delete")
    @ResponseBody
    public ResponseMessage delete(@RequestBody HotTableRepairRequest req) {
        ResponseMessage result = new ResponseMessage();
        HotTableRepairManager.delete(req.getSourceTableName());
        result.setData("删除成功");
        return result;
    }

    @RequestMapping("reset/status/bySourceTable")
    @ResponseBody
    public ResponseMessage resetStatusBySourceTable(@RequestBody HotTableRepairRequest req) {
        ResponseMessage result = new ResponseMessage();
        if(BIUtil.isEmpty(req.getSourceTableName()) || BIUtil.isEmpty(req.getStatus())){
            result.setData("sourceTableName和status不能为空");
            return result;
        }
        List<String> resetInfos = HotTableRepairManager.resetStatusBySourceTable(req.getSourceTableName(), req.getStatus());
        result.setData(BIUtil.listToStr(resetInfos, "\n"));
        return result;
    }

    @RequestMapping("reset/status/byId")
    @ResponseBody
    public ResponseMessage resetStatusById(@RequestBody HotTableRepairRequest req) {
        ResponseMessage result = new ResponseMessage();
        if(BIUtil.isEmpty(req.getTaskIdList()) || BIUtil.isEmpty(req.getStatus())){
            result.setData("id和status不能为空");
            return result;
        }
        List<String> resetInfos = HotTableRepairManager.resetStatusById(req.getTaskIdList(), req.getStatus());
        result.setData(BIUtil.listToStr(resetInfos, "\n"));
        return result;
    }

    /**
     * 重新运行通过id指定的任务
     * @param req
     * @return
     */
    @RequestMapping("redo/byId")
    @ResponseBody
    public ResponseMessage redoById(@RequestBody HotTableRepairRequest req) {
        ResponseMessage result = new ResponseMessage();
        if(BIUtil.isEmpty(req.getTaskIdList())){
            result.setData("id不能为空");
            return result;
        }
        List<String> redoInfos = HotTableRepairManager.redoById(req.getTaskIdList());
        result.setData(BIUtil.listToStr(redoInfos, "\n"));
        return result;
    }


    /**
     * 通过源表名重新补数
     * @param req
     * @return
     */
    @RequestMapping("redo/bySource")
    @ResponseBody
    public ResponseMessage redoBySourceTableName(@RequestBody HotTableRepairRequest req) {
        ResponseMessage result = new ResponseMessage();
        if(BIUtil.isEmpty(req.getSourceTableName())){
            result.setData("sourceTableName不能为空");
            return result;
        }
        List<String> taskInfos = new ArrayList<>();
        HotTableRepairManager.delete(req.getSourceTableName());
        taskInfos.add("删除成功");

        List<String> initInfos = HotTableRepairManager.initialize(req.getSourceTableName());
        taskInfos.addAll(initInfos);
        result.setData(BIUtil.listToStr(taskInfos, "\n"));
        return result;
    }

}
