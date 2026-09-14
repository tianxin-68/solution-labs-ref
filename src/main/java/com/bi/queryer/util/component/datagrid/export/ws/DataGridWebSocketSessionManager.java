package com.bi.queryer.util.component.datagrid.export.ws;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.websocket.Session;

public abstract class DataGridWebSocketSessionManager {
	// 用户->每个类型的所有session
	public static Map<String, CopyOnWriteArraySet<Session>> sessionMap = new HashMap<String, CopyOnWriteArraySet<Session>>();
	
	public static void cache(String userName, Session session){
		if(sessionMap.containsKey(userName)){
			sessionMap.get(userName).add(session);
		}else{
			CopyOnWriteArraySet<Session> sessions = new CopyOnWriteArraySet<Session>();
			sessions.add(session);
			sessionMap.put(userName, sessions);
		}
	}
	
	public static void remove(String userName, Session session){
		if(sessionMap.containsKey(userName)){
			sessionMap.get(userName).remove(session);
		}
	}
	
	public static CopyOnWriteArraySet<Session> getAllSession(String userName){
		CopyOnWriteArraySet<Session> sessions = sessionMap.get(userName);
		if(sessions == null) sessions = new CopyOnWriteArraySet<Session>();
		
		// 剔除已被关闭的session
		CopyOnWriteArraySet<Session> newSessions = new CopyOnWriteArraySet<Session>();
		for(Session s : sessions){
			if(s.isOpen()){
				newSessions.add(s);
			}
		}
		return newSessions;
	}
}
