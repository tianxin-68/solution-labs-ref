package com.bi.queryer.ssm.meta;

import java.util.Date;

/**
 * 微信推送状态
 */
public class WeiXinPushVo{

	private Long id;

	private String status;
	
	private Date createdTime;
	
	private String dt;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public Date getCreatedTime() {
		return createdTime;
	}

	public void setCreatedTime(Date createdTime) {
		this.createdTime = createdTime;
	}

	public String getDt() {
		return dt;
	}

	public void setDt(String dt) {
		this.dt = dt;
	}


	public WeiXinPushVo(String status, Date createdTime, String dt) {
		this.status = status;
		this.createdTime = createdTime;
		this.dt = dt;
	}

	public WeiXinPushVo() {
	}
}
