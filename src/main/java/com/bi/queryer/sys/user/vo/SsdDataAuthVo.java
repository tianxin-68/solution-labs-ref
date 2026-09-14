package com.bi.queryer.sys.user.vo;

public class SsdDataAuthVo {

    private String moduleCode;

    private String moduleName;

    private String dimCode;

    private String dimName;

    private String itemCode;

    private String itemValue;

    private Integer months;

    /**
     * 操作类型：add=新增权限，delete=删除权限；默认 add
     */
    private String applyType;

    /**
     * 模块敏感等级，如 c1/c4
     */
    private String sensitiveLevel;

    /**
     * 使用场景说明
     */
    private String dataUsageScenario;

    /**
     * 目录是否开启权限继承：1=继承，0=未开启（由前台传入，工单在目录名后标注用）
     */
    private Integer isInherited;

    public String getModuleCode() {
        return moduleCode;
    }

    public void setModuleCode(String moduleCode) {
        this.moduleCode = moduleCode;
    }

    public String getModuleName() {
        return moduleName;
    }

    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }

    public String getDimCode() {
        return dimCode;
    }

    public void setDimCode(String dimCode) {
        this.dimCode = dimCode;
    }

    public String getDimName() {
        return dimName;
    }

    public void setDimName(String dimName) {
        this.dimName = dimName;
    }

    public String getItemCode() {
        return itemCode;
    }

    public void setItemCode(String itemCode) {
        this.itemCode = itemCode;
    }

    public String getItemValue() {
        return itemValue;
    }

    public void setItemValue(String itemValue) {
        this.itemValue = itemValue;
    }

    public Integer getMonths() {
        return months;
    }

    public void setMonths(Integer months) {
        this.months = months;
    }

    public String getApplyType() {
        return applyType;
    }

    public void setApplyType(String applyType) {
        this.applyType = applyType;
    }

    public String getSensitiveLevel() {
        return sensitiveLevel;
    }

    public void setSensitiveLevel(String sensitiveLevel) {
        this.sensitiveLevel = sensitiveLevel;
    }

    public String getDataUsageScenario() {
        return dataUsageScenario;
    }

    public void setDataUsageScenario(String dataUsageScenario) {
        this.dataUsageScenario = dataUsageScenario;
    }

    public Integer getIsInherited() {
        return isInherited;
    }

    public void setIsInherited(Integer isInherited) {
        this.isInherited = isInherited;
    }
}
