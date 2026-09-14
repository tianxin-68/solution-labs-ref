package com.bi.queryer.util.component.datagrid.export.ws;

import java.io.IOException;

import javax.servlet.http.HttpSession;
import javax.websocket.CloseReason;
import javax.websocket.EndpointConfig;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import com.bi.queryer.sys.user.vo.User;
import com.bi.queryer.util.BIConsts;
import com.bi.queryer.util.ws.WebSocketHttpSessionConfigurator;

@ServerEndpoint(value = "/component/datagrid/export", configurator = WebSocketHttpSessionConfigurator.class)
public class DataGridExportWebSocket {

	private HttpSession httpSession;

	@OnOpen
	public void onOpen(Session session, EndpointConfig config) {
//		System.out.println("datagrid export websocket open :" + session.isOpen());
		this.httpSession = (HttpSession) config.getUserProperties().get("httpSession");
		User user = getUser();
		if(user != null){
			DataGridWebSocketSessionManager.cache(user.getName(), session);
		}
	}

	@OnMessage
	public void onMessage(String msg, Session session) throws IOException {
		System.out.println(msg);
	}

	@OnClose
	public void onClose(Session session, CloseReason closeReason) {
		User user = getUser();
		if(user != null){
			DataGridWebSocketSessionManager.remove(user.getName(), session);
		}
		System.out.println("datagrid export websocket close");
	}

	@OnError
	public void error(Session session, Throwable ta) {
		User user = getUser();
		if(user != null){
			DataGridWebSocketSessionManager.remove(user.getName(), session);
		}
		System.out.println("datagrid export websocket :" + ta);

	}
	
	protected User getUser(){
		User user = null;
		try{
			user = (User) httpSession.getAttribute(BIConsts.SESSION_KEY_USER);
		}catch(Exception e){
			
		}
		return user;
	}
}
