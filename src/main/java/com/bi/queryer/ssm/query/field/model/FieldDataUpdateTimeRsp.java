package com.bi.queryer.ssm.query.field.model;

import com.bi.queryer.sys.enums.Enabled;

public class FieldDataUpdateTimeRsp {

    /**
     * 数据更新时间
     */
    private String dataUpdateTime;

    /**
     * 是否实时
     */
    private Integer isRt = Enabled.NO.getId();

    /**
     * 是否显示提示
     * 具体实时更新时间正在建设中
     */
    private Integer isShowRemark = Enabled.NO.getId();

    /**
     * 说明
     */
    private String remark;

    public String getDataUpdateTime() {
        return dataUpdateTime;
    }

    public void setDataUpdateTime(String dataUpdateTime) {
        this.dataUpdateTime = dataUpdateTime;
    }

    public Integer getIsRt() {
        return isRt;
    }

    public void setIsRt(Integer isRt) {
        this.isRt = isRt;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public Integer getIsShowRemark() {
        return isShowRemark;
    }

    public void setIsShowRemark(Integer isShowRemark) {
        this.isShowRemark = isShowRemark;
    }
}
