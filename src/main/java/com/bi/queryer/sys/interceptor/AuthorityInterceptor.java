package com.bi.queryer.sys.interceptor;

import com.bi.queryer.sys.common.ResponseMessage;
import com.bi.queryer.sys.config.SC;
import com.bi.queryer.sys.enums.TokenErrorType;
import com.bi.queryer.sys.user.UserManager;
import com.bi.queryer.sys.user.UserTokenManager;
import com.bi.queryer.sys.user.vo.User;
import com.bi.queryer.util.BIConsts;
import com.bi.queryer.util.BIUtil;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class AuthorityInterceptor extends HandlerInterceptorAdapter {

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
		request.setCharacterEncoding("utf-8");
		response.setCharacterEncoding("utf-8");
		crossDomain(request, response);
		ServletContext app = request.getSession().getServletContext();
		String cxt = BIUtil.getBasePath(request);
		app.setAttribute(BIConsts.App_Cxt, cxt);
		app.setAttribute(BIConsts.App_Version, SC.version);
		app.setAttribute(BIConsts.App_Name, SC.appName);
		String url = cxt + request.getRequestURI();
		String queryString = request.getQueryString();
		if(!BIUtil.isEmpty(queryString)) {
			url = url + "?" + queryString;
		}
		if(BIUtil.isStaticResource(request) || "options".equalsIgnoreCase(request.getMethod())) {
			return super.preHandle(request, response, handler);
		}
		Object controllerObject = null;
		// 有免check注解标识的，直接跳过
		if(handler instanceof HandlerMethod){
			HandlerMethod hm = (HandlerMethod)handler;

			FreeCheckAuthority freeChkFlag = hm.getMethodAnnotation(FreeCheckAuthority.class);
			if(freeChkFlag != null) {
				this.setFreeCheckUser(request, response);
				return true;
			}
			controllerObject = hm.getBean();
			Class<?> clazz = controllerObject.getClass();
			String className = controllerObject.getClass().getName();
			// 处理AOP动态代理导致注解丢失的问题
			clazz = className.contains("$") ? Class.forName(className.substring(0, className.indexOf("$"))) : clazz;
			if(clazz.isAnnotationPresent(FreeCheckAuthority.class)) {
				this.setFreeCheckUser(request, response);
				return true;
			}
		}
		
		if(controllerObject == null) {
			response.sendRedirect(cxt + "/error/404");
			return false;
		}
		
		// 校验token
		ResponseMessage tokenCheckResult = UserTokenManager.check(request, response, false);
		if(!tokenCheckResult.getSuccess()) {
			System.out.println("check token:" + tokenCheckResult.getMessage());
//			String redirectUrl = cxt + "/login?" + BIConsts.Token_Error + "=" + tokenCheckResult.getMessage();
			String redirectUrl = cxt + "/sso";
			//  若用户非法，则调整到非授权权限页面
			if(TokenErrorType.Invalid_User == TokenErrorType.get(tokenCheckResult.getMessage())){
				redirectUrl = cxt + "/unAuth";
			}
//			if(isViewUrl) {// 是视图url，追加登录后跳转url
//				redirectUrl = redirectUrl + "&" + BIConsts.Login_Redirect + "=" + url;
//			}
			response.sendRedirect(redirectUrl);
			return false;
		}
		
		// 校验成功后添加user到当前线程中
		User user = (User) tokenCheckResult.getData();
		UserManager.set(user);
		
		return super.preHandle(request, response, handler);
	}

	protected void  crossDomain(HttpServletRequest request, HttpServletResponse response){
		if(!"true".equalsIgnoreCase(SC.v("cross.domain.access"))) {
			return;
		}
		response.setHeader("P3P", "CP=CAO PSA OUR");
		response.setHeader("Access-Control-Allow-Origin", request.getHeader("Origin"));
		response.setHeader("Access-Control-Expose-Headers", "Content-disposition,error_msg");
//		response.setHeader("Access-Control-Allow-Origin", "*");
//		response.setHeader("Access-Control-Allow-Origin","null");
		response.setHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS, DELETE");
		response.setHeader("Access-Control-Max-Age", "7200"); // 2 * 60 * 60
		Enumeration e = request.getHeaderNames();
		List<String> allowHeaders = new ArrayList<>();
		while(e.hasMoreElements()) {
			allowHeaders.add(e.nextElement() + "");
		}
		String s = BIUtil.listToStr(allowHeaders, ", ");
		s = s + ", Origin, X-Requested-With, Content-Type, Accept, u_token";
//		response.setHeader("Access-Control-Allow-Headers", "s");

		String accessControlRequestHeaders = request.getHeader("Access-Control-Request-Headers");
		if(BIUtil.isNotEmpty(accessControlRequestHeaders)){
			allowHeaders.add(accessControlRequestHeaders);
		}

//		response.setHeader("Access-Control-Allow-Headers", "*");
		response.setHeader("Access-Control-Allow-Headers", accessControlRequestHeaders);
		response.setHeader("Access-Control-Allow-Credentials", "true");
		response.setHeader("XDomainRequestAllowed","1");

		//后端响应头添加 PNA 许可
		response.setHeader("Access-Control-Allow-Private-Network", "true");
	}

	protected void setFreeCheckUser(HttpServletRequest request, HttpServletResponse response){
		ResponseMessage tokenCheckResult = UserTokenManager.check(request, response, true);
		if(tokenCheckResult.getData() != null) {
			User user = (User) tokenCheckResult.getData();
			UserManager.set(user);
		}
	}

}
