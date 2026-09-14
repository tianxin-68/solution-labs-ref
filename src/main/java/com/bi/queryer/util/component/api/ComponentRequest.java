package com.bi.queryer.util.component.api;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import com.bi.queryer.sys.user.vo.User;
import com.bi.queryer.util.BIConsts;
import com.bi.queryer.util.StringUtil;

/**
 * 组件请求，组合HttpServletRequest
 * @author contributor
 *
 */
public class ComponentRequest implements Serializable{
	
	private static final long serialVersionUID = 1L;
	protected HttpServletRequest servletRequest = null;
	
	protected Map<String, String> params = null;
	
	protected ComponentRequest(){
		
	}
	
	public ComponentRequest(HttpServletRequest servletRequest, Map<String, String> params){
		this();
		this.servletRequest = servletRequest;
		this.params = params;
		HttpSession session = getSession();
		User user = (User)session.getAttribute(BIConsts.SESSION_KEY_USER);
		if(user != null){
			this.params.put("userName", user.getName().toLowerCase());
		}
	}
	
	public User getUser(){
		User user = (User) this.servletRequest.getSession().getAttribute(BIConsts.SESSION_KEY_USER);
		return user;
	}
	
	public ComponentRequest clone(){
		ComponentRequest copy = new ComponentRequest();
		copy.servletRequest = this.servletRequest;
		copy.params = clone(this.params);
		return copy;
	}
	
	public HttpSession getSession(){
		return servletRequest.getSession();
	}
	
	public boolean contains(String name){
		return params.containsKey(name);
	}
	
	@Override
	public String toString() {
		String str = "";
		for(String key : params.keySet()){
			str = str + "&" + key + "=" + params.get(key);
		}
		str = str.replaceFirst("\\&", "");
		return str;
	}
	
	/**
	 * 克隆参数
	 * @param params
	 * @return
	 */
	public Map<String, String> clone(Map<String, String> queryParams){
		if(queryParams == null) queryParams = params;
		Map<String, String> copy = new HashMap<String, String>();
		for(Entry<String, ?> entry : queryParams.entrySet()){
			copy.put(entry.getKey(), entry.getValue() + "");
//			System.out.println(entry.getKey() + "=" + entry.getValue());
		}
		return copy;
	}
	
	
	/**
	 * 获取参数值
	 * @param paramName
	 * @return
	 */
	public String getString(String paramName){
		return this.params.get(paramName);
	}
	
	/**
	 * 获取参数值
	 * @param paramName
	 * @return
	 */
	public Integer getInteger(String paramName){
		String str = params.get(paramName);
		if(!StringUtil.isEmpty(str)){
			return Integer.valueOf(str);
		}else{
			return null;
		}
	}
	
	/**
	 * 获取当前URL
	 * @return
	 */
	public String getCurrentUrl(){
		String param = servletRequest.getQueryString();
		if(param == null) param = "";
		String currentUrl = servletRequest.getRequestURL().append("?").append(param).toString();
		return currentUrl;
	}
	
	public void addParameter(String name, Object value){
		params.put(name, value == null?"":value.toString());
	}
	
	public void removeParameter(String name){
		params.remove(name);
	}
	
	protected void initialize(){
	}

	public HttpServletRequest getServletRequest() {
		return servletRequest;
	}

	public void setServletRequest(HttpServletRequest servletRequest) {
		this.servletRequest = servletRequest;
	}

	public Map<String, String> getParams() {
		if(params == null){
			params = new HashMap<String, String>();
		}
		return params;
	}

	public void setParams(Map<String, String> params) {
		this.params = params;
	}
}
