package com.bi.queryer.ssm.promotion;

import com.bi.queryer.ssm.promotion.model.PromoYearRsp;
import com.bi.queryer.ssm.promotion.model.PromotionDateRangeReq;
import com.bi.queryer.ssm.promotion.model.PromotionDateRangeRsp;
import com.bi.queryer.sys.common.SSMResponseMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
@Scope("prototype")
@RequestMapping("ssm/promotion")
public class PromotionController {

    @Autowired
    private PromotionService promotionService;

    /**
     * 获取活动配置列表
     * @return
     */
    @RequestMapping(value = "list", method = RequestMethod.POST)
    @ResponseBody
    public SSMResponseMessage<List<PromoYearRsp>> list() {
        return SSMResponseMessage.success("获取活动配置列表成功", promotionService.list());
    }

    /**
     * 获取活动时间段
     * @return
     */
    @RequestMapping(value = "getPromotionDateRange", method = RequestMethod.POST)
    @ResponseBody
    public SSMResponseMessage<PromotionDateRangeRsp> getPromotionDateRange(@RequestBody PromotionDateRangeReq req){
        return SSMResponseMessage.success("获取活动时间段成功", promotionService.getPromotionDateRange(req));
    }

    /**
     * 初始化活动配置
     * @return
     */
    @RequestMapping(value = "init", method = RequestMethod.GET)
    @ResponseBody
    public SSMResponseMessage init() {
        promotionService.init();
        return SSMResponseMessage.success("初始化成功");
    }

}
