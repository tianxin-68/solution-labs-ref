package com.bi.queryer.ssm.engine.result;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.collection.ListUtil;
import com.bi.queryer.sys.db.DataType;
import com.bi.queryer.sys.enums.Enabled;
import com.bi.queryer.sys.enums.SortType;
import com.bi.queryer.util.BIConsts;
import com.bi.queryer.util.BIUtil;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * @Author contributor
 * @Date 15:23 2022-10-20
 * @Description 数据集字段
 **/
public class ResultDataSetColumn implements Comparator<ResultDataSetColumn> , Comparable, Serializable {

    private String id = "";

    private String code = "";

    private String title = "";

    private String dataType = DataType.String.toString();

    private String type = "";

    private Integer isTree = 0;

    private String parentCode = ""; // 上级id

    private String dataFormat = "";

    private Integer level = 0 ;// 级次

    private Integer isShow = Enabled.YES.getId();

    private String rawCode = ""; // 字段原生字段

    private String rawTitle = ""; // 字段原生标题

    private List<ResultDataSetColumn> children = new ArrayList<>();

    /**
     * 是否是锁定列
     */
    private Integer isFixed = Enabled.NO.getId();

    /**
     * 是否可点击表头排序
     */
    private Integer canOrder = Enabled.NO.getId();

    /**
     * 排序方式
     */
    private String orderType = SortType.NONE.toString();

    /**
     * 字段所属的原始查询区域
     */
    private String rawQueryArea = "";

    /**
     * 是否有过滤器
     */
    private Integer hasFilter = Enabled.NO.getId();

    /**
     * 是否可过滤
     */
    private Integer canFilter = Enabled.NO.getId();

    private String exportAppendString = "";

    protected boolean exportable = true;// 字段可以导出

    protected boolean hasAlarm = false;

    protected boolean isZb = false; // 是否是占比

    protected boolean isTotal = false; // 是否是总计列

    protected boolean isWholeTableTotal = false; // 整表总计列

    protected String calcMode = "";

    protected String calcType = "";

    /**
     * 自定义对比索引
     */
    private Integer compareIndex = 0;

    /**
     * 贡献率计算方式
     */
    private String ctrCalcMode;

    /**
     * 占比、同环比配置
     */
    private ResultDataSetZbThbConfig zbThbConfig = new ResultDataSetZbThbConfig();

    private ResultDataSetTargetConfig targetConfig;

    /**
     * 是否显示农历日期
     */
    private Boolean isShowLunarDate = false;

    /**
     * 排序id
     */
    private Double sortId = 0.0;

    /**
     * 是否有权限
     */
    private boolean hasAuth = true;

    // 汇总自定义聚合类型
    private String totalAggTypeDesc;

    public ResultDataSetColumn(){

    }

    public ResultDataSetColumn(String code, String title) {
        this.code = code;
        this.title = title;
    }

    @Override
    public int compare(ResultDataSetColumn o1, ResultDataSetColumn o2) {
        if(o1.getSortId() == null || o2.getSortId() == null) {
            return 0;
        }
        double diff = o1.getSortId() - o2.getSortId();
        if(diff == 0){
            return 0;
        }
        return diff > 0 ? 1 : -1;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ResultDataSetColumn that = (ResultDataSetColumn) o;
        return id.equals(that.id) || code.equalsIgnoreCase(that.code);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "TableField{" +
                "id='" + id + '\'' +
                ", filedCode='" + code + '\'' +
                ", fieldTitle='" + title + '\'' +
                ", dataType='" + dataType + '\'' +
                ", fieldType='" + type + '\'' +
                '}';
    }

    public String getExportAppendString() {
        return exportAppendString;
    }

    public void setExportAppendString(String exportAppendString) {
        this.exportAppendString = exportAppendString;
    }

    public Integer getIsTree() {
        return isTree;
    }

    public void setIsTree(Integer isTree) {
        this.isTree = isTree;
    }

    public String getDataFormat() {
        if(BIUtil.isEmpty(dataFormat)) {
            // 默认格式化
            if(DataType.getType(dataType) == DataType.Double){
                dataFormat = "###,###,##0.00";
            }
            if(DataType.getType(dataType).isInteger()){
                dataFormat = "###,###,##0";
            }
        }
        return dataFormat;
    }

    public void setDataFormat(String dataFormat) {
        this.dataFormat = dataFormat;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Integer getLevel() {
        return level;
    }

    public void setLevel(Integer level) {
        this.level = level;
    }

    public Integer getIsShow() {
        return isShow;
    }

    public void setIsShow(Integer isShow) {
        this.isShow = isShow;
    }

    public Integer getIsFixed() {
        return isFixed;
    }

    public void setIsFixed(Integer isFixed) {
        this.isFixed = isFixed;
    }

    public Integer getCanOrder() {
        return canOrder;
    }

    public void setCanOrder(Integer canOrder) {
        this.canOrder = canOrder;
    }

    public String getOrderType() {
        return orderType;
    }

    public void setOrderType(String orderType) {
        this.orderType = orderType;
    }

    public String getRawQueryArea() {
        return rawQueryArea;
    }

    public void setRawQueryArea(String rawQueryArea) {
        this.rawQueryArea = rawQueryArea;
    }

    public Integer getHasFilter() {
        return hasFilter;
    }

    public void setHasFilter(Integer hasFilter) {
        this.hasFilter = hasFilter;
    }

    public Integer getCanFilter() {
        return canFilter;
    }

    public void setCanFilter(Integer canFilter) {
        this.canFilter = canFilter;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getTitle() {
        if(!isHasAuth() && BIUtil.isNotEmpty(title) && !title.contains(BIConsts.NO_AUTH_COLUMN_TIPS)){
            title = title + BIConsts.NO_AUTH_COLUMN_TIPS;
        }
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDataType() {
        return dataType;
    }

    public void setDataType(String dataType) {
        this.dataType = dataType;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getRawCode() {
        return rawCode;
    }

    public void setRawCode(String rawCode) {
        this.rawCode = rawCode;
    }

    public String getRawTitle() {
        return rawTitle;
    }

    public void setRawTitle(String rawTitle) {
        this.rawTitle = rawTitle;
    }

    public List<ResultDataSetColumn> getChildren() {
        return children;
    }

    public void setChildren(List<ResultDataSetColumn> children) {
        this.children = children;
    }

    public boolean isExportable() {
        return exportable;
    }

    public void setExportable(boolean exportable) {
        this.exportable = exportable;
    }

    public String getParentCode() {
        return parentCode;
    }

    public void setParentCode(String parentCode) {
        this.parentCode = parentCode;
    }

    public void addChild(ResultDataSetColumn column){
        if(column == null){
            return;
        }
        if(!this.children.contains(column)) {
            this.children.add(column);
        }
        column.setParentCode(this.getCode());
        column.setLevel(this.getLevel() + 1);
    }

    public void setChild(Integer index, ResultDataSetColumn column){
        if(index < 0 || index > this.children.size() - 1){
            this.addChild(column);
        }
        this.children.set(index, column);
        column.setParentCode(this.getCode());
        column.setLevel(this.getLevel() + 1);
    }

    public void removeChild(ResultDataSetColumn child){
        if(child == null){
            return;
        }
        this.children.remove(child);
    }

    // 获取当前column的叶子节点数量
    public Integer getLeafNum() {
        if (CollectionUtil.isEmpty(this.getChildren())) {
            return 0;
        }
        Integer childNum = this.getChildren().size();
        for (ResultDataSetColumn column : this.getChildren()) {
            if (CollectionUtil.isNotEmpty(column.children)) {
                Integer num = column.getLeafNum();
                childNum += num > 0 ? (num - 1) : 0;
            }
        }
        return childNum;
    }

    // 获取当前column的最大层级数
    public Integer getMaxLevel() {
        Integer level = 1;
        Integer maxChildLevel = 0;
        if (CollectionUtil.isEmpty(this.getChildren())) {
            return level;
        }
        for (ResultDataSetColumn column : this.getChildren()) {
            if (column.getMaxLevel() > maxChildLevel) {
                maxChildLevel = column.getMaxLevel();
            }
        }
        return level + maxChildLevel;
    }


    //获取所有的叶子节点
    @JsonIgnore
    public List<ResultDataSetColumn> getLeafChildren() {
        List<ResultDataSetColumn> childList = ListUtil.list(false);
        if (CollectionUtil.isEmpty(this.getChildren())) {
            childList.add(this);
            return childList;
        }

        this.getLeafChildrenCascade(childList, this.getChildren());

        return childList;
    }

    public void getLeafChildrenCascade(List<ResultDataSetColumn> result, List<ResultDataSetColumn> children) {
        if(BIUtil.isEmpty(children)){
            return;
        }
        for(ResultDataSetColumn column : children) {
            if (BIUtil.isNotEmpty(column.children)) {
                getLeafChildrenCascade(result, column.children);
            } else{
                result.add(column);
            }
        }
    }

    public ResultDataSetColumn getChild(String childCode){
        for(ResultDataSetColumn c : children){
            if(childCode.equalsIgnoreCase(c.getCode())){
                return c;
            }
        }
        return null;
    }

    public boolean isHasAlarm() {
        return hasAlarm;
    }

    public void setHasAlarm(boolean hasAlarm) {
        this.hasAlarm = hasAlarm;
    }

    public boolean isZb() {
        return isZb;
    }

    public void setZb(boolean zb) {
        isZb = zb;
    }

    public boolean isTotal() {
        return isTotal;
    }

    public void setTotal(boolean total) {
        isTotal = total;
    }

    public boolean isWholeTableTotal() {
        return isWholeTableTotal;
    }

    public void setWholeTableTotal(boolean wholeTableTotal) {
        isWholeTableTotal = wholeTableTotal;
    }

    public ResultDataSetColumn clone(){
        return JSONObject.parseObject(BIUtil.toJSONString(this), this.getClass());
    }

    public String getCalcMode() {
        return calcMode;
    }

    public void setCalcMode(String calcMode) {
        this.calcMode = calcMode;
    }

    public String getCalcType() {
        return calcType;
    }

    public void setCalcType(String calcType) {
        this.calcType = calcType;
    }

    public Integer getCompareIndex() {
        return compareIndex;
    }

    public void setCompareIndex(Integer compareIndex) {
        this.compareIndex = compareIndex;
    }

    public String getCtrCalcMode() {
        return ctrCalcMode;
    }

    public void setCtrCalcMode(String ctrCalcMode) {
        this.ctrCalcMode = ctrCalcMode;
    }

    public Boolean getShowLunarDate() {
        return isShowLunarDate;
    }

    public void setShowLunarDate(Boolean showLunarDate) {
        isShowLunarDate = showLunarDate;
    }

    public Double getSortId() {
        return sortId;
    }

    public void setSortId(Double sortId) {
        this.sortId = sortId;
    }

    @Override
    public int compareTo(Object o) {
        return compare(this, (ResultDataSetColumn) o);
    }

    public boolean isHasAuth() {
        return hasAuth;
    }

    public void setHasAuth(boolean hasAuth) {
        this.hasAuth = hasAuth;
    }

    public ResultDataSetZbThbConfig getZbThbConfig() {
        return zbThbConfig;
    }

    public void setZbThbConfig(ResultDataSetZbThbConfig zbThbConfig) {
        this.zbThbConfig = zbThbConfig;
    }

    public ResultDataSetTargetConfig getTargetConfig() {
        return targetConfig;
    }

    public void setTargetConfig(ResultDataSetTargetConfig targetConfig) {
        this.targetConfig = targetConfig;
    }

    public String getTotalAggTypeDesc() {
        return totalAggTypeDesc;
    }

    public void setTotalAggTypeDesc(String totalAggTypeDesc) {
        this.totalAggTypeDesc = totalAggTypeDesc;
    }
}
