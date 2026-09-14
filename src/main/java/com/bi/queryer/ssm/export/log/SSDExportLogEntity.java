package com.bi.queryer.ssm.export.log;

import lombok.Data;

import java.util.Date;

/**
 * 导出日志实体
 * 
 * @author contributor
 *
 */
@Data
public class SSDExportLogEntity {

	private String id;

	private String templateId;

	private String viewId;

	private String queryConfig;

	private String userName;

	private Date beginTime;

	private Date endTime;
	
	private String mode = "sync";

	private Integer success = 1;

	private String info;

	private Integer rows = 0;
	
	private String fileExt;
	
	private String fileName;

	private String status;

	/**
	 * 解密码
	 */
	private String expDecodeCode;

	/**
	 * 个人敏感是否解密
	 */
	private Integer decryptSensitiveField;

	/**
	 * 是否需要工单审批
	 */
	private Integer isApplyApprove;

	/**
	 * 设备指纹
	 */
	private String blackBox;

	private String querySql;

	private String queryFilter;

	//查询会话id
	private String sessionId;

	public SSDExportLogEntity() {
	}

	public SSDExportLogEntity(String id, String templateId, String queryConfig, String userName, Date beginTime, String mode, Integer rows, String expDecodeCode, Integer decryptSensitiveField, Integer isApplyApprove) {
		this.id = id;
		this.templateId = templateId;
		this.queryConfig = queryConfig;
		this.userName = userName;
		this.beginTime = beginTime;
		this.mode = mode;
		this.rows = rows;
		this.expDecodeCode = expDecodeCode;
		this.decryptSensitiveField = decryptSensitiveField;
		this.isApplyApprove = isApplyApprove;
	}

	public SSDExportLogEntity(String id, Integer success, String info, Integer rows, String fileName, String status) {
		this.id = id;
		this.success = success;
		this.info = info;
		this.rows = rows;
		this.fileName = fileName;
		this.status = status;
	}
}
