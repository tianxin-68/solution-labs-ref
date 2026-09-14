package com.bi.queryer.ssm.meta;

import com.bi.queryer.util.BIUtil;
import com.bi.queryer.util.JSONSerializable;
import com.alibaba.fastjson.JSONObject;
import lombok.Data;

@Data
public class SSDQuestionAndAnswer implements JSONSerializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 问题序号
	 */
	private Long questionId;
    /**
     * 主题类型
     */
    private String objectType;
	/**
	 * 主题id
	 */
	private String objectId;

	/**
	 * 主题名称
	 */
	private String objectTitle;

	/**
	 * 问题内容
	 */
	private String questionContent;

	/**
	 * 提问人
	 */
	private String questionOwner;

	/**
	 * 回答内容
	 */
	private String answerContent;

	/**
	 * 回答人
	 */
	private String answerOwner;

	/**
	 * 是否回答
	 */
	private Integer isAnswer;

	/**
	 * QA排序
	 */
	private Double sortId;

	/**
	 * 提问时间
	 */
	private String questionTime;

	/**
	 * 回答时间
	 */
	private String answerTime;

	/**
	 * 是否有效，1：是，0：否
	 */
	private Integer isActive;

	/**
	 * 创建时间
	 */
	private String createdTime;

	/**
	 * 修改时间
	 */
	private String updatedTime;

	/**
	 * 创建人
	 */
	private String createdBy;

	/**
	 * 修改人
	 */
	private String updatedBy;

	/**
	 * 查询开始时间
	 */
	private String questionTimeBegin;
	/**
	 * 查询结束时间
	 */
	private String questionTimeEnd;

	private Integer pageSize;

	private Integer pageNum;

    private String orderBy;

	@Override
	public JSONObject toJSON() {
		return BIUtil.toJSONObject(this);
	}
}
