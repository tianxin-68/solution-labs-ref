package com.bi.queryer.ssm.export;

import com.bi.queryer.sys.user.UserManager;
import com.bi.queryer.sys.user.vo.User;
import com.bi.queryer.util.ws.WebSocketHttpSessionConfigurator;

import javax.servlet.http.HttpSession;
import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

@ServerEndpoint(value = "/ws/ssm/export", configurator = WebSocketHttpSessionConfigurator.class)
public class SSDExportWebSocket {

	static ConcurrentHashMap<String, Session> sessionMap = new ConcurrentHashMap<String, Session>();

	private HttpSession httpSession;

	@OnOpen
	public void onOpen(Session session, EndpointConfig config) {

		System.out.println("websocket open :" + session.isOpen());

		this.httpSession = (HttpSession) config.getUserProperties().get("httpSession");
		this.httpSession.setAttribute("socketSession", session);
		User user = UserManager.get();
		if(user != null){
			sessionMap.put(user.getName(), session);
		}
	}

	@OnMessage
	public void onMessage(String msg, Session session) throws IOException {
		System.out.println(msg);
	}

	@OnClose
	public void onClose(Session session, CloseReason closeReason) {
		sessionMap.remove(session.getId());
		this.httpSession.removeAttribute("socketSession");
		System.out.println("websocket close");
	}

	@OnError
	public void error(Session session, Throwable ta) {
		sessionMap.remove(session.getId());
		this.httpSession.removeAttribute("socketSession");
		System.out.println(ta);

	}

	/**
	 * 往前台发送消息
	 * @param txt
	 */
	public static void sendMessage(String txt) {
		User user = UserManager.get();

		try {
			if (user != null) {
				Session session = sessionMap.get(user.getName());
				if (session != null&&session.isOpen()) {
					session.getBasicRemote().sendText(txt);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
