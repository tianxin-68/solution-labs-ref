package com.bi.queryer.ssm.meta;

import com.bi.queryer.ssm.enums.DataEnv;
import com.bi.queryer.sys.dim.vo.DataAuth;
import com.bi.queryer.util.BIUtil;
import com.alibaba.fastjson.JSON;

/**
 * User: contributor
 * Date: 2020/2/20
 * Time: 14:28
 * Description: 字段数据权限
 */
public class MetaFieldDataAuth extends DataAuth {
    private String fieldCode;

    private String whitePaperCode;

    private String authMode;

    /**
     * 数据库中的真实dimCode
     */
    private String dimRealCode;

    private String dataEnv = DataEnv.OLD_SSM.getCode();

    /**
     * 是否可申请
     */
    private Integer isApply;

    public String getFieldCode() {
        return fieldCode;
    }

    public void setFieldCode(String fieldCode) {
        this.fieldCode = fieldCode;
    }

    public String getAuthMode() {
        return authMode;
    }

    public void setAuthMode(String authMode) {
        this.authMode = authMode;
    }

    public String getDataEnv() {
        return dataEnv;
    }

    public void setDataEnv(String dataEnv) {
        this.dataEnv = dataEnv;
    }

    @Override
    public String toString() {
        return  "fieldCode:" + fieldCode + "\t"
                + "dimCode:" + getDimCode() + "\t"
                + "moduleCode:" + getModuleCode() + "\t"
                + "itemCode:" + getItemCode() + "\t"
                + "authMode:" + authMode + "\t"
                + "isApply:" + isApply;
    }

    public String getDimRealCode() {
        return dimRealCode;
    }

    public void setDimRealCode(String dimRealCode) {
        this.dimRealCode = dimRealCode;
    }

    public String getWhitePaperCode() {
        return whitePaperCode;
    }

    public void setWhitePaperCode(String whitePaperCode) {
        this.whitePaperCode = whitePaperCode;
    }

    public Integer getIsApply() {
        return isApply;
    }

    public void setIsApply(Integer isApply) {
        this.isApply = isApply;
    }

    @Override
    public boolean equals(Object obj) {
        MetaFieldDataAuth  mf = (MetaFieldDataAuth)obj;
        return  fieldCode != null ? fieldCode.equals(mf.getFieldCode()) : super.equals(obj);
    }

    public MetaFieldDataAuth clone(){
        MetaFieldDataAuth mf = JSON.parseObject(BIUtil.toJSONString(this), this.getClass());
        return mf;
    }
}
