package com.bi.queryer.sys.interceptor;

import java.util.Arrays;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

public class ErrorPageInterceptor extends HandlerInterceptorAdapter {
	private List<Integer> errorCodeList = Arrays.asList(404, 403, 500);
	
	private ThreadLocal<Boolean> isRedirected = new ThreadLocal<Boolean>(); 

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {
//		if (errorCodeList.contains(response.getStatus()) && !BIUtil.isStaticResource(request)) {
//			this.redirect(request, response, handler);
//			return false;
//		}
		return super.preHandle(request, response, handler);
	}

	@Override
	public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
			ModelAndView modelAndView) throws Exception {
//		if (errorCodeList.contains(response.getStatus())  && !BIUtil.isStaticResource(request)) {
//			this.redirect(request, response, handler);
//			return;
//		}
		super.postHandle(request, response, handler, modelAndView);
	}
	
	private void redirect(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception{
		// 捕获异常后进行重定向，controller对应的requestMapping为/error/{code}
//		String url = BIUtil.getBasePath(request) + "/error/" + response.getStatus();
//		if(isRedirected.get() == null  || !isRedirected.get()) {
//			try {
//				response.sendRedirect(url);
//			}catch(Exception e) {
//				System.out.println(e.getMessage());
//			}
//			isRedirected.set(true);
//		}
	}
}
