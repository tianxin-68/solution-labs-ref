package com.bi.queryer.util.component.datagrid.export;

import java.io.IOException;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.servlet.http.HttpServletRequest;
import javax.websocket.Session;

import com.alibaba.fastjson.JSONObject;
import com.bi.queryer.sys.user.vo.User;
import com.bi.queryer.util.BIConsts;
import com.bi.queryer.util.component.datagrid.export.ws.DataGridWebSocketSessionManager;

public class DataGridExportUtil {
	
	/**
	 * 导出后操作：web socket通知ui导出完成
	 * @param userName
	 */
	public static void doPost(HttpServletRequest request, boolean success) {
		User user = (User) request.getSession().getAttribute(BIConsts.SESSION_KEY_USER);
		String userName = "";
		if(user != null) userName = user.getName();
		String notifiedKey = "_export_notified_";
		Boolean isNotified = "true".equalsIgnoreCase(request.getAttribute(notifiedKey) + "");
//		System.out.println(Thread.currentThread().getId() + "------------>" + isNotified);
		// 已通知，则不需重复通知
		if(isNotified != null && isNotified) {
			return;
		}
		CopyOnWriteArraySet<Session> sessions = DataGridWebSocketSessionManager.getAllSession(userName);
		if(sessions == null || sessions.isEmpty()){
			return;
		}
		// 是否所有都关闭了
		boolean isAllClosed = true;
		for(Session session : sessions){
			isAllClosed = isAllClosed && !session.isOpen();
		}
		if(isAllClosed){
			return;
		}
		// 数据查询
		if(sessions != null){
			for(Session session : sessions){
				try {
					if(session.isOpen()){
						JSONObject result = new JSONObject();
						if(success) {
							result.put("status", "success");
						}else {
							result.put("status", "failed");
						}
						session.getBasicRemote().sendText(result.toString());
					}else{
						System.out.println("send error: socket session [" + userName + "] socket is closed...");
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
		// 添加已通知标识
		request.setAttribute(notifiedKey, true);
	}
}
