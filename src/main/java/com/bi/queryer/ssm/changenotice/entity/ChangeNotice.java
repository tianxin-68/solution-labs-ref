package com.bi.queryer.ssm.changenotice.entity;

import lombok.Data;

/**
 * 变更通知主表实体，对应表 mgp_change_notice
 */
@Data
public class ChangeNotice {

    /** 通知ID */
    private Long id;
    /** 通知标题 */
    private String title;
    /** 负责人域账号，多个逗号分隔 */
    private String owners;
    /** 变更类型：REPLACE_OFFLINE/METRIC_CALIBER/DIM_CONTENT/USAGE_CONSTRAINT/DATA_BACKFILL */
    private String changeType;
    /** 变更说明，富文本HTML */
    private String description;
    /** 新对象变更说明，富文本HTML（替换/下线提醒新对象场景） */
    private String newObjDescription;
    /** 触发范围：GLOBAL-全局 / LOCAL-局部，见 ChangeNoticeTriggerScope */
    private String triggerScope;
    /** 局部触发时路由的数据表名，多个逗号分隔 */
    private String localRouteTableNames;
    /** 替换/下线提醒目标：REMIND_OLD/REMIND_NEW，多个逗号分隔 */
    private String remindTargets;
    /** 替换/下线关联的替换任务ID，多个逗号分隔 */
    private String replaceTaskId;
    /** 生效开始时间（发布时间） */
    private String startTime;
    /** 到期时间，到期后停止提醒 */
    private String expireTime;
    /** 状态：ACTIVE-生效中 / OFFLINE-已下线 */
    private String status;
    /** 发布时间 */
    private String publishTime;
    /** 是否有效：0-否 / 1-是 */
    private Integer isActive;
    /** 创建人（域账号） */
    private String createdBy;
    /** 更新人（域账号） */
    private String updatedBy;
}
