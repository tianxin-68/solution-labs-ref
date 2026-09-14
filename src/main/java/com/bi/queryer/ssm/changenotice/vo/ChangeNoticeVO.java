package com.bi.queryer.ssm.changenotice.vo;

import cn.hutool.core.util.StrUtil;
import com.bi.queryer.ssm.changenotice.entity.ChangeNotice;
import com.bi.queryer.ssm.changenotice.enums.ChangeNoticeType;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 变更通知匹配出参。
 * changeObjects 各类型均可能返回；
 * mustQueryObjects/mustQueryFilters 仅使用约束类型填充；
 * remindFieldObjects 仅数据回刷类型填充（命中的提醒范围字段）。
 */
@Data
public class ChangeNoticeVO {

    /** 通知ID */
    private Long id;
    /** 标题 */
    private String title;
    /** 变更类型 */
    private String changeType;
    /** 变更类型中文描述 */
    private String changeTypeDesc;
    /** 富文本HTML */
    private String description;
    /** 新对象变更说明，富文本HTML */
    private String newObjDescription;
    /** 生效开始时间 */
    private String startTime;
    /** 到期时间 */
    private String expireTime;
    /** 发布时间 */
    private String publishTime;
    /** 发布人域账号：优先 updatedBy，否则 createdBy */
    private String publishBy;
    /** 发布人真实姓名，匹配结束后统一批量回填 */
    private String publishByRealName;
    /** 状态 */
    private String status;
    /**
     * 是否命中「提醒旧对象」（REMIND_OLD）。
     * 1：命中，前端展示 description，并可按 needApply 引导一键应用；0：未命中。
     */
    private Integer hitRemindOld = 0;
    /**
     * 是否命中「提醒新对象」（REMIND_NEW）。
     * 1：命中，前端展示 newObjDescription（告知口径变化，无需再替换）；0：未命中。
     */
    private Integer hitRemindNew = 0;
    /**
     * 是否需要前端一键应用。
     * 1：需要应用（如替换/下线的字段替换、使用约束的必须同查补全）；
     * 0：无需一键应用。
     */
    private Integer needApply = 0;
    /** 命中的变更对象（含 relObject* 替换信息；needReplace 标识是否参与一键替换） */
    private List<ChangeNoticeObjectVO> changeObjects = new ArrayList<>();
    /** 必须同查字段（仅 USAGE_CONSTRAINT 填充，object_ctg=MUST_QUERY） */
    private List<ChangeNoticeObjectVO> mustQueryObjects = new ArrayList<>();
    /** 必须同查筛选（仅 USAGE_CONSTRAINT 填充） */
    private List<ChangeNoticeObjectFilterVO> mustQueryFilters = new ArrayList<>();
    /**
     * 命中的提醒范围字段（仅 DATA_BACKFILL 填充，object_ctg=REMIND_FIELD）。
     * 仅返回与当前查询字段 code 有交集的项；未配置提醒范围时为空列表。
     */
    private List<ChangeNoticeObjectVO> remindFieldObjects = new ArrayList<>();

    /**
     * 主表实体转 VO（不含对象明细）
     * @param entity 通知实体
     * @return VO
     */
    public static ChangeNoticeVO from(ChangeNotice entity) {
        if (entity == null) {
            return null;
        }
        ChangeNoticeVO vo = new ChangeNoticeVO();
        vo.setId(entity.getId());
        vo.setTitle(entity.getTitle());
        vo.setChangeType(entity.getChangeType());
        ChangeNoticeType type = ChangeNoticeType.fromCode(entity.getChangeType());
        vo.setChangeTypeDesc(type != null ? type.getDesc() : null);
        vo.setDescription(entity.getDescription());
        vo.setNewObjDescription(entity.getNewObjDescription());
        vo.setStartTime(entity.getStartTime());
        vo.setExpireTime(entity.getExpireTime());
        vo.setPublishTime(entity.getPublishTime());
        // updated_by 有值取 updated_by，否则取 created_by；真实姓名由 Service 统一批量回填
        vo.setPublishBy(StrUtil.isNotBlank(entity.getUpdatedBy())
                ? entity.getUpdatedBy().trim()
                : StrUtil.trim(entity.getCreatedBy()));
        vo.setStatus(entity.getStatus());
        return vo;
    }
}
