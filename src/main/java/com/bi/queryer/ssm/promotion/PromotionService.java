package com.bi.queryer.ssm.promotion;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.bi.queryer.ssm.mgr.fieldDef.model.ManualIndexWhitePaperEntity;
import com.bi.queryer.ssm.promotion.model.*;
import com.bi.queryer.sys.base.AbstractTransaction;
import com.bi.queryer.sys.base.BaseDao;
import com.bi.queryer.sys.db.DataSourceType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Scope("prototype")
public class PromotionService {

    @Autowired
    private BaseDao dao;

    public List<PromoYearRsp> list() {

        List<PromotionCfg> promotionList = PromotionManager.getAllPromotionCfgList();

        Map<String, PromoYearRsp> yearMap = new LinkedHashMap<>();

        //构建 年份 → 名称数组 → 阶段数组
        for (PromotionCfg promotionCfg : promotionList) {

            PromoYearRsp yearNode = yearMap.computeIfAbsent(promotionCfg.getPromoYear().toString(), y -> new PromoYearRsp(y));

            Optional<PromoNameRsp> promoNameOpt = yearNode.getPromoNameList().stream().filter(n -> n.getName().equals(promotionCfg.getPromoName())).findAny();
            if (promoNameOpt.isPresent()) {
                promoNameOpt.get().getPhaseList().add(new PromoPhaseRsp(promotionCfg.getPromoPhase()));
            } else {
                PromoNameRsp promoNameNode = new PromoNameRsp(promotionCfg.getPromoName());
                promoNameNode.getPhaseList().add(new PromoPhaseRsp(promotionCfg.getPromoPhase()));
                yearNode.getPromoNameList().add(promoNameNode);
            }

            yearMap.put(promotionCfg.getPromoYear().toString(), yearNode);
        }

        List<PromoYearRsp> result = new ArrayList<>();
        if (CollUtil.isNotEmpty(yearMap.values())) {
            result = yearMap.values().stream().collect(Collectors.toList());
        }

        return result;
    }

    /**
     * 获取活动时间段
     * @param req
     * @return
     */
    public PromotionDateRangeRsp getPromotionDateRange(PromotionDateRangeReq req) {
        PromotionDateRangeRsp rsp = new PromotionDateRangeRsp();

        List<String> dateList = new ArrayList<>();

        //汇总行没有时间，取配置的所有活动的起止时间
        if (StrUtil.isEmpty(req.getPromotionValue())) {

            for (String promotionIdentifier : req.getPromotionConfigList()) {
                PromotionCfg promotionCfg = PromotionManager.getPromotionCfg(promotionIdentifier);
                if (promotionCfg == null) {
                    continue;
                }

                dateList.add(promotionCfg.getStartDate());
                dateList.add(promotionCfg.getEndDate());
            }

        } else {
            //活动格式化后 = 2025双十一·开门红(2025-10-30-2025-11-03)
            String[] promotionArr = req.getPromotionValue().split("\\【");
            String promotionIdentifier = promotionArr[0].replaceAll("(\\d{4})([^·]+)·(.+)", "$1-$2-$3");

            PromotionCfg promotionCfg = PromotionManager.getPromotionCfg(promotionIdentifier);
            if (promotionCfg != null) {
                dateList.add(promotionCfg.getStartDate());
                dateList.add(promotionCfg.getEndDate());
            }
        }

        //添加截止时间的逻辑
        List<String> result = new ArrayList<>();
        Date dataSnapshotDate = DateUtil.parseDate(req.getDataSnapshotDate());
        //截止时间处理
        for (String dateValue : dateList) {
            if(DateUtil.compare(DateUtil.parseDate(dateValue),dataSnapshotDate ) > 0){
                result.add(req.getDataSnapshotDate());
            }else{
                result.add(dateValue);
            }
        }

        Collections.sort(result);

        rsp.setStartDate(result.get(0));
        rsp.setEndDate(result.get(result.size() - 1));
        return rsp;
    }

    public void init() {

        //查询当前最新的活动配置
        List<PromotionCfg> promotionList = (List<PromotionCfg>) dao.queryObjectList("ssm.promotion.getMgpPromotionConfig", null, DataSourceType.Data_Studio);


        dao.executeTranscation(DataSourceType.Default, new AbstractTransaction() {
            @Override
            public void execute() {

                dao.delete("ssm.promotion.delete", null);

                if (CollUtil.isNotEmpty(promotionList)) {
                    Map<String, Object> param = new HashMap<>();
                    param.put("list", promotionList);
                    dao.insert("ssm.promotion.batchInsert", param);
                }

            }
        });

        //刷新缓存
        PromotionManager.refresh();
    }

}
