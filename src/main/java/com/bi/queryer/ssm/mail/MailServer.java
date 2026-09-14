package com.bi.queryer.ssm.mail;


import cn.hutool.core.collection.ListUtil;
import cn.hutool.http.HttpRequest;
import com.bi.queryer.sys.base.BaseDao;
import com.bi.queryer.sys.config.SC;
import com.bi.queryer.sys.db.DBUtil;
import com.bi.queryer.util.BIUtil;
import com.bi.queryer.util.period.DateUtil;
import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;
import java.util.stream.Collectors;

public class MailServer {

	public static void send(MailItem item) {
		MailThread mt = new MailThread(item);
		mt.start();
	}

	public static void sendSensitiveData(Map<String, Object> map) {
		SensitiveDataMailThread smt = new SensitiveDataMailThread(map);
		smt.start();
	}
}


/**
 * 邮件发送线程
 *
 * @author contributor
 */
class MailThread extends Thread {

	public final static String notification_url = "http://localhost:9010/WeiXinWork/Push/Text";
	public final static String notification_requestID = "change-me";
	public final static String notification_etlJobSchedule = "多维分析";

	private MailItem item = null;

	public MailThread(MailItem item) {
		this.item = item;
	}

	protected void send() throws Exception {
		String subject = item.getSubject();

		StringBuilder htmlContent = new StringBuilder();
		htmlContent.append("<style type=\"text/css\">");
		htmlContent.append(" table {").append("border-right: 1px solid #804040;")
				.append("border-bottom: 1px solid #804040;")
				.append("border-collapse:collapse;")
				.append("}");
		htmlContent.append(" table td {")
				.append("border-left: 1px solid #804040;")
				.append("border-top: 1px solid #804040;")
				.append("width:120px;")
				.append("}");
		htmlContent.append("</style>");

		htmlContent.append("<table>");
		// 表头
		htmlContent.append("<tr>");
		htmlContent.append("<td>").append(item.getUserName()).append("</td>");
		htmlContent.append("<td>").append("查询日志ID").append("</td>");
		htmlContent.append("<td>").append(item.getId()).append("</td>");
		htmlContent.append("<td>查询开始时间：</td>");
		htmlContent.append("<td>").append(DateUtil.toDateStr(item.getBeginTime(), "yyyy-MM-dd HH:mm:ss")).append("</td>");
		htmlContent.append("<td>查询异常时间：</td>");
		htmlContent.append("<td>").append(DateUtil.toDateStr(item.getEndTime(), "yyyy-MM-dd HH:mm:ss")).append("</td>");
		htmlContent.append("</tr>");

		// 错误信息
		htmlContent.append("<tr>");
		htmlContent.append("<td>");
		htmlContent.append("异常信息");
		htmlContent.append("</td>");
		htmlContent.append("<td colspan='6'>");
		htmlContent.append(item.getInfo());
		htmlContent.append("</td>");
		htmlContent.append("</tr>");

		// 查询SQL
		htmlContent.append("<tr>");
		htmlContent.append("<td>");
		htmlContent.append("SQL");
		htmlContent.append("</td>");
		htmlContent.append("<td colspan='6'>");
		htmlContent.append(item.getQuerySQL());
		htmlContent.append("</td>");
		htmlContent.append("</tr>");
		htmlContent.append("</table>");

		String[] mailToArray = SC.v("mail.to").split(",");

		Set<String> receivers = new HashSet<>();
		receivers.addAll(Arrays.asList(mailToArray));
		if(BIUtil.isNotEmpty(this.item.getMailTo())){
			Set<String> mailTo = this.item.getMailTo();
			for(String c : mailTo){
				if(!c.contains("@") && BIUtil.isNotEmpty(c)){
					receivers.add(c + "@example.com");
				}
			}
		}


		BaseDao dao = DBUtil.getBaseDao();
		List<String> receiverMailList = receivers.stream().filter(c->BIUtil.isNotEmpty(c)).collect(Collectors.toList());
		List<String> validReceiverMailList = (List<String>)dao.queryObjectList("user.queryValidUserEmailList", receiverMailList);
		BIUtil.sendMail(subject, htmlContent.toString(), validReceiverMailList);
	}

	protected void sendNotification() {
		try {
			String messageText = "多维分析-后台错误:请查看邮件！";
			List<String> sendUser = new ArrayList<>();
			String notificationUser = SC.v("notification.ssm.admin");
			sendUser.add(notificationUser);

			JSONObject jsonObject = new JSONObject();

			jsonObject.put("Content", messageText);
			jsonObject.put("Sign", notification_etlJobSchedule);
			jsonObject.put("Users", sendUser);

			String result = HttpRequest.post(notification_url)
					.header("RequestID", notification_requestID)
					.body(jsonObject.toJSONString())
					.execute().body();

			System.out.println("BI Queryer通结果：" + result);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void run() {
		try {
			send();
		} catch (Exception e) {
			System.out.println("邮件发送失败：" + e.getMessage());
			e.printStackTrace();
		}
		try {
			sendNotification();
		} catch (Exception e) {
			System.out.println("BI Queryer通消息发送失败：" + e.getMessage());
			e.printStackTrace();
		}

	}
}

/**
 * 敏感数据邮件申请
 */
class SensitiveDataMailThread extends Thread {
	private Map<String, Object> map = null;

	public SensitiveDataMailThread(Map<String, Object> map) {
		this.map = map;
	}

	protected void send() {
		String subject = map.get("subject").toString();
		//敏感字段
		String sensitiveDataName = BIUtil.nvl(map.get("sensitiveDataName"), "");
		//申请原因
		String description = BIUtil.nvl(map.get("description"), "");

		//模板名字
		String templateName = BIUtil.nvl(map.get("templateName"), "");

		String sendName = BIUtil.nvl(map.get("sendName"), "");
		String realName = BIUtil.nvl(map.get("realName"), "");
		sendName = sendName + "@example.com";

		StringBuilder htmlContent = new StringBuilder();
		htmlContent.append("<style type=\"text/css\">");
		htmlContent.append(" table {").append("border-right: 1px solid #804040;")
				.append("border-bottom: 1px solid #804040;")
				.append("border-collapse:collapse;")
				.append("}");
		htmlContent.append(" table td {")
				.append("border-left: 1px solid #804040;")
				.append("border-top: 1px solid #804040;")
				.append("width:120px;")
				.append("}");
		htmlContent.append("</style>");

		htmlContent.append("<table>");

		// 表头
		htmlContent.append("<tr>");
		htmlContent.append("<td>").append("申请人：").append("</td>");
		htmlContent.append("<td style='width:720px;'>").append(realName).append("(").append(sendName).append(")").append("</td>");
		htmlContent.append("</tr>");


		htmlContent.append("<tr>");
		htmlContent.append("<td>").append("敏感字段：").append("</td>");
		htmlContent.append("<td style='width:720px;'>").append(sensitiveDataName).append("</td>");
		htmlContent.append("</tr>");

		// 查询SQL
		htmlContent.append("<tr>");
		htmlContent.append("<td>");
		htmlContent.append("申请原因：");
		htmlContent.append("</td>");
		htmlContent.append("<td style='width:720px;'>");
		htmlContent.append(description);
		htmlContent.append("</td>");
		htmlContent.append("</tr>");

		htmlContent.append("<tr>");
		htmlContent.append("<td>").append("查询模板：").append("</td>");
		htmlContent.append("<td style='width:720px;'>").append(templateName).append("</td>");
		htmlContent.append("</tr>");

		htmlContent.append("</table>");

		String receiverMailAddress = BIUtil.nvl(map.get("receiverMailAddress"), "");
		String ccMailAddress = SC.v("ssm.mail.cc", "contributor@example.com");

		if (BIUtil.isNotEmpty(receiverMailAddress) && receiverMailAddress.indexOf(sendName) == -1) {
			receiverMailAddress += "," + sendName;
		}

		String mailApprove = SC.v("ssm.mail.approve", "contributor@example.com");
		if (BIUtil.isNotEmpty(receiverMailAddress) && receiverMailAddress.indexOf(mailApprove) == -1) {
			receiverMailAddress += "," + mailApprove;
		}


		BIUtil.sendMail(subject, htmlContent.toString(), receiverMailAddress, ccMailAddress);
	}

	public void run() {
		try {
			send();
		} catch (Exception e) {
			System.out.println("邮件发送失败...");
			e.printStackTrace();
		}

	}
}
