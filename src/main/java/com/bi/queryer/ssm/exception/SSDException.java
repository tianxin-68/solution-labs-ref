package com.bi.queryer.ssm.exception;

import com.bi.queryer.sys.exception.BIException;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SSDException extends BIException {

	private static final long serialVersionUID = 1L;

	private String type = "";

	/**
	 * 未授权标识
	 */
	public static final String UnAuth_Key = "_unauth_";

	/**
	 * 自定义字段check错误标识
	 */
	public static final String Custom_Field_Check_Error = "_custom_check_error_";

	/**
	 * 手动kill错误标识
	 */
	public static final String Manual_Kill_Error = "manual_kill_error";

	// 邮件接收人列表
	private Set<String> mailTo = new HashSet<>();

	// 执行sql
	private String sql = "";

	// 无效模型信息（模型名列表、负责人列表、模板id），供前端展示
	private Map<String, Object> invalidModelInfo = new HashMap<>();

	public SSDException() {

	}


	public SSDException(String message) {
		super(message);
	}

	public SSDException(String message, String type) {
		super(message);
		this.type = type;
	}

	public SSDException(Throwable e) {
		super(e);
	}

	public SSDException(String message, Throwable e) {
		super(message, e);
	}

	public String getType() {
		return this.type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public Set<String> getMailTo() {
		return mailTo;
	}

	public void setMailTo(Set<String> mailTo) {
		this.mailTo = mailTo;
	}

	public String getSql() {
		return sql;
	}

	public void setSql(String sql) {
		this.sql = sql;
	}

	public Map<String, Object> getInvalidModelInfo() {
		return invalidModelInfo;
	}

	public void setInvalidModelInfo(Map<String, Object> invalidModelInfo) {
		this.invalidModelInfo = invalidModelInfo;
	}
}
