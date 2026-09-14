package com.bi.queryer.ssm.mail;


import com.bi.queryer.ssm.query.log.SSDQueryLogEntity;
import com.bi.queryer.sys.config.SC;

import java.util.HashSet;
import java.util.Set;

public class MailItem extends SSDQueryLogEntity {
	
	private String subject = "";

	public String getSubject() {
		return subject;
	}

	// 邮件接收人列表
	private Set<String> mailTo = new HashSet<>();

	public void setSubject(String subject) {
		this.subject = subject;
	}

	public MailItem(){
		
	}
	
	public MailItem(SSDQueryLogEntity log){
		this.userName = log.getUserName();
		
		this.templateId = log.getTemplateId();
		
		this.querySQL = log.getQuerySQL();
		
		this.beginTime = log.getBeginTime();
		
		this.endTime = log.getEndTime(); 
		
		this.success = log.getSuccess();
		
		this.info = log.getInfo();
		
		this.subject = SC.appName + "-查询失败-" + log.getUserName();

		this.id = log.getId();
	}

	public Set<String> getMailTo() {
		return mailTo;
	}

	public void setMailTo(Set<String> mailTo) {
		this.mailTo = mailTo;
	}
}
