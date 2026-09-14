package com.bi.queryer.sys.authapply.model;

import java.math.BigDecimal;

public class TaskBaseParam {

    //创建时只保存填写项操作 传默认值：false
    private Boolean isCreateSave = false;

    //外部接口创建下一步处理人标志 如果需要指定下一步处理人传true，否则false
    private Boolean outerCreateFlag = false;

    //工单流程配置Id
    private Integer taskConfigId;

    //工单内容
    private String taskContent;

    //工单映射名称
    private String systemName;

    //任务标题
    private String taskTitle;

    //指定工单下一步处理人
    private String appointTaskOwner;

    //任务优先级
    private String taskPriority;

    //用于幂等查询条件
    private Integer orderId;

    //总金额(如有总金额需求，需给此字段赋值)
    private BigDecimal totalMoney;

    private String userEmail;

    public Boolean getCreateSave() {
        return isCreateSave;
    }

    public void setCreateSave(Boolean createSave) {
        isCreateSave = createSave;
    }

    public Boolean getOuterCreateFlag() {
        return outerCreateFlag;
    }

    public void setOuterCreateFlag(Boolean outerCreateFlag) {
        this.outerCreateFlag = outerCreateFlag;
    }

    public Integer getTaskConfigId() {
        return taskConfigId;
    }

    public void setTaskConfigId(Integer taskConfigId) {
        this.taskConfigId = taskConfigId;
    }

    public String getTaskContent() {
        return taskContent;
    }

    public void setTaskContent(String taskContent) {
        this.taskContent = taskContent;
    }

    public String getSystemName() {
        return systemName;
    }

    public void setSystemName(String systemName) {
        this.systemName = systemName;
    }

    public String getTaskTitle() {
        return taskTitle;
    }

    public void setTaskTitle(String taskTitle) {
        this.taskTitle = taskTitle;
    }

    public String getAppointTaskOwner() {
        return appointTaskOwner;
    }

    public void setAppointTaskOwner(String appointTaskOwner) {
        this.appointTaskOwner = appointTaskOwner;
    }

    public String getTaskPriority() {
        return taskPriority;
    }

    public void setTaskPriority(String taskPriority) {
        this.taskPriority = taskPriority;
    }

    public Integer getOrderId() {
        return orderId;
    }

    public void setOrderId(Integer orderId) {
        this.orderId = orderId;
    }

    public BigDecimal getTotalMoney() {
        return totalMoney;
    }

    public void setTotalMoney(BigDecimal totalMoney) {
        this.totalMoney = totalMoney;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }
}
