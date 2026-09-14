package com.bi.queryer.ssm.engine.analysis.cfg.total;

import com.bi.queryer.ssm.enums.AnalysisTotalType;
import com.bi.queryer.sys.enums.Enabled;
import com.bi.queryer.util.BIUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author contributor
 * @Date 18:12 2023-07-10
 * @Description 总计/小计配置
 **/
public class AnalysisTotalConfig {

    protected Integer isActive = Enabled.NO.getId();

    protected List<AnalysisTotalItemConfig> items = new ArrayList<>();


    private AnalysisTotalAggConfig aggConfig = new AnalysisTotalAggConfig();

    /**
     * 统计维度id列表
     */
//    protected List<String> statsDimIdList = new ArrayList<>();

    /**
     * 总计类型
     */
//    protected AnalysisTotalType totalType = AnalysisTotalType.NONE;

    public AnalysisTotalConfig(){

    }

//    public List<String> getStatsDimIdList() {
//        return statsDimIdList;
//    }
//
//    public void setStatsDimIdList(List<String> statsDimIdList) {
//        this.statsDimIdList = statsDimIdList;
//    }
//
//    public List<String> getStatsDimCodeList(){
//        List<String> codeList = new ArrayList<>();
//        if(BIUtil.isEmpty(getStatsDimIdList())){
//            return codeList;
//        }
//        for(String id : statsDimIdList){
//            MetaField metaField = SSDMetaCacheManager.getField(id);
//            if(metaField != null){
//                codeList.add(metaField.getCode());
//            }
//        }
//        return codeList;
//    }

    public void add(AnalysisTotalItemConfig item){
        this.items.add(item);
    }

    public boolean isActive(){
        boolean active = Enabled.value(this.isActive);
        if(!active || BIUtil.isEmpty(items)){
            return false;
        }

        for(AnalysisTotalItemConfig item : items){
            active = active || item.isActive();
        }

        return active;
    }

    public Integer getIsActive() {
        return isActive;
    }

    public void setIsActive(Integer isActive) {
        this.isActive = isActive;
    }

    public List<AnalysisTotalItemConfig> getItems() {
        return items;
    }

    public void setItems(List<AnalysisTotalItemConfig> items) {
        this.items = items;
    }

    /**
     * 获取占比总计类型
     * @return
     */
    public AnalysisTotalType getZbTotalType(){
        if(!isActive()){
            return AnalysisTotalType.NONE;
        }

        return items.get(0).getTotalType();
    }

    public AnalysisTotalItemConfig getItem(AnalysisTotalType totalType){
        if(BIUtil.isEmpty(items)){
            return null;
        }
        for(AnalysisTotalItemConfig item : items){
            if(totalType == item.getTotalType()){
                return item;
            }
        }
        return null;
    }

    public AnalysisTotalItemConfig getItem(AnalysisTotalType totalType,String measureId) {
        if (BIUtil.isEmpty(items)) {
            return null;
        }
        for (AnalysisTotalItemConfig item : items) {
            if (totalType == item.getTotalType() && item.getMeasureId().equalsIgnoreCase(measureId)) {
                return item;
            }
        }
        return null;
    }

    // 是否有分析汇总计项
    public boolean hasAnalysisTotalItem(AnalysisTotalType totalType){
        if(BIUtil.isEmpty(items)){
            return false;
        }
        for(AnalysisTotalItemConfig item : items){
            if(totalType == item.getTotalType()){
                return true;
            }
        }
        return false;
    }

    public AnalysisTotalAggConfig getAggConfig() {
        return aggConfig;
    }

    public void setAggConfig(AnalysisTotalAggConfig aggConfig) {
        this.aggConfig = aggConfig;
    }
}
