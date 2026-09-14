package com.bi.queryer.sys.user;

import cn.hutool.core.util.StrUtil;
import com.bi.queryer.sys.user.vo.User;
import com.bi.queryer.util.BIUtil;

import javax.servlet.http.HttpServletRequest;

public abstract class UserManager {
	private static final ThreadLocal<User> currentUser = new ThreadLocal<User>();
	
	public static void set(User user) {
		currentUser.set(user);
	}
	
	public static User get() {
		return currentUser.get();
	}
	
	public static void remove() {
		currentUser.remove();
	}

	public static String getToken(HttpServletRequest request) {
		String token = "";

		try {
			token = request.getHeader("u_token");
			if (StrUtil.isEmpty(token)) {
				token = request.getParameter("u_token");
			}

			if (StrUtil.isEmpty(token)) {
				token = BIUtil.getCookie("u_token", request);
			}
		} catch (Exception var3) {
		}

		return token;
	}
}
