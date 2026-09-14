package com.bi.queryer.ssm.schedule;


import com.bi.queryer.sys.base.BaseController;
import com.bi.queryer.sys.common.ResponseMessage;
import com.bi.queryer.sys.interceptor.FreeCheckAuthority;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 需要使用调度的接口（定时运行的功能）
 * @author contributor
 */
@RestController
@RequestMapping("schedule")
public class ScheduleInterfaceController extends BaseController {

    @Autowired
    private ScheduleInterfaceService scheduleInterfaceService;


    /**
     * 定时刷新字段查询统计表数据
     * 用于目录归档功能
     * 调度任务每月运行后调用
     * @return
     */
    @RequestMapping("refreshSSDQueryFieldStats")
    @FreeCheckAuthority
    public ResponseMessage refreshSSDQueryFieldStats(){
        return scheduleInterfaceService.refreshSSDQueryFieldStats();
    }

}
