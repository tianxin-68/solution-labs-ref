package com.bi.queryer.ssm.changenotice.vo;

import com.bi.queryer.ssm.changenotice.entity.ChangeNoticeObject;
import lombok.Data;

/**
 * 变更通知对象出参。
 * object* 为旧对象；relObject* 为替换新对象，为空表示纯下线。
 */
@Data
public class ChangeNoticeObjectVO {

    /** 对象编码 */
    private String objectId;
    /** 对象名称 */
    private String objectName;
    /** 对象类型 */
    private String objectType;
    /** 替换新对象编码 */
    private String relObjectId;
    /** 替换新对象名称 */
    private String relObjectName;
    /** 替换新对象类型 */
    private String relObjectType;
    /**
     * 是否需要对该对象执行替换。
     * 1：旧对象仍在使用，且配置了新对象（relObjectId 非空），前端一键替换时应处理本条；
     * 0：纯下线（无新对象）、或仅新对象口径提醒、或其他非替换场景。
     */
    private Integer needReplace = 0;

    /**
     * 实体转 VO（默认 needReplace=0）
     * @param entity 对象明细
     * @return VO
     */
    public static ChangeNoticeObjectVO from(ChangeNoticeObject entity) {
        return from(entity, 0);
    }

    /**
     * 实体转 VO
     * @param entity 对象明细
     * @param needReplace 是否需要执行替换，1=是 / 0=否
     * @return VO
     */
    public static ChangeNoticeObjectVO from(ChangeNoticeObject entity, Integer needReplace) {
        if (entity == null) {
            return null;
        }
        ChangeNoticeObjectVO vo = new ChangeNoticeObjectVO();
        vo.setObjectId(entity.getObjectId());
        vo.setObjectName(entity.getObjectName());
        vo.setObjectType(entity.getObjectType());
        vo.setRelObjectId(entity.getRelObjectId());
        vo.setRelObjectName(entity.getRelObjectName());
        vo.setRelObjectType(entity.getRelObjectType());
        vo.setNeedReplace(needReplace == null ? 0 : needReplace);
        return vo;
    }
}
