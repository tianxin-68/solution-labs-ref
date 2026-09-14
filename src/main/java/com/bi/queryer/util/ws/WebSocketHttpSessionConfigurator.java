package com.bi.queryer.util.ws;

import javax.servlet.http.HttpSession;
import javax.websocket.HandshakeResponse;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerEndpointConfig;

public class WebSocketHttpSessionConfigurator extends ServerEndpointConfig.Configurator {

	@Override

	public void modifyHandshake(ServerEndpointConfig config, HandshakeRequest request, HandshakeResponse response) {

		HttpSession httpSession = (HttpSession) request.getHttpSession();

		if (null != httpSession) {
			config.getUserProperties().put("httpSession", httpSession);
		}

	}

}
