package com.bi.queryer.sys.base;

import cn.hutool.core.util.StrUtil;
import com.bi.queryer.sys.enums.RuntimeEnv;
import com.bi.queryer.sys.model.BIModelAndView;
import com.bi.queryer.sys.user.UserManager;
import com.bi.queryer.sys.user.vo.User;
import com.bi.queryer.util.BIUtil;
import com.bi.queryer.util.encryption.AES;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.ModelAttribute;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.*;

public class BaseController {
	
	protected Logger logger = Logger.getLogger(this.getClass());
	
	@Autowired
	protected HttpServletRequest request;
	 
	@Autowired
	protected HttpServletResponse response;
	
	protected Map<String, Object> params = new HashMap<String, Object>();
	
	protected String resultUrl = "";

	private static String keyByteStr = "pYyPs7JhbzPpd7Hf";

    private static String encryptStr = "AES:";
	
	/**
	 * 线程安全初始化reque，respose对象
	 * 
	 * @param request
	 * @param response
	 */
	@ModelAttribute
	public void initParameters(HttpServletRequest request, HttpServletResponse response) {
		try{

			byte keyByte[] = keyByteStr.getBytes("utf-8");

			this.request = request;
			this.response = response;
			request.setCharacterEncoding("utf-8");
			response.setCharacterEncoding("utf-8");
			Enumeration enu = request.getParameterNames();  
			params = new HashMap<String, Object>();
			while(enu.hasMoreElements()){
				String paraName = (String)enu.nextElement();
				String value = request.getParameter(paraName).trim();
				if(value.contains(encryptStr)){
					value = value.replace(encryptStr,"");
					value = new String(AES.decrypt(value, keyByte), "utf-8");
				}
				params.put(paraName, value);
			}
		}catch(Throwable e) {
			e.printStackTrace();
		}
	}

	/**
	 * 从请求流中获取参数信息
	 */
	public void initStreamParameters() {
		try{
			byte keyByte[] = keyByteStr.getBytes("utf-8");

			InputStream is = request.getInputStream();
			BufferedReader bf = new BufferedReader(new InputStreamReader(is, "UTF-8"));
			StringBuffer buffer = new StringBuffer();
			String line = "";
			while ((line = bf.readLine()) != null) {
				buffer.append(line);
			}
			String postParam = buffer.toString();
			// url转码
			//postParam = URLDecoder.decode(postParam, "utf-8");
			Map<String, Object> postJSON = null;
			try {
				postJSON = JSONObject.parseObject(postParam);
			}catch(Exception e) {
				postJSON = BIUtil.fetchParameters(postParam);
			}
			if(postJSON != null) {
				for(String key : postJSON.keySet()) {
					String value = String.valueOf(postJSON.get(key));
					if(value.contains(encryptStr)){
						long t1 = System.currentTimeMillis();
						value = value.replace(encryptStr,"");
						value = new String(AES.decrypt(value, keyByte), "utf-8");
						params.put(key, value);
						long t2 = System.currentTimeMillis();
						System.out.println("************参数解密耗时：" + (t2-t1) + "毫秒");
					}else{
						params.put(key, postJSON.get(key));
					}

				}
			}
		}catch (Exception e) {
			System.out.print("x-api:-------------->参数解析错误");
			String errorMsg = e.getMessage();
			if (StrUtil.isNotEmpty(errorMsg) && !errorMsg.contains("Stream closed")) {
				e.printStackTrace();
			}
		}
	}
	

	public HttpServletRequest getRequest() {
		return request;
	}

	public void setRequest(HttpServletRequest request) {
		this.request = request;
	}

	public HttpServletResponse getResponse() {
		return response;
	}

	public void setResponse(HttpServletResponse response) {
		this.response = response;
	}
	
	public void writeJSON(Object msg) {
		writeJSON(JSON.toJSONString(msg));
	}
	
	public void writeJSON(JSONObject json) {
		writeJSON(json.toString());
	}
	
	public HttpSession getSession() {
		return this.request.getSession();
	}

	/**
	 * 设置json返回结果
	 */
	public void writeJSON(String json) {
		try {
			if(json == null) {
				return;
			}
			request.setCharacterEncoding("utf-8");
			response.setCharacterEncoding("utf-8");
			OutputStream pw = getResponse().getOutputStream();
			pw.write(json.getBytes("utf-8"));
			pw.flush();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * 页面跳转
	 * @param url
	 */
	public void redirect(String url){
		try{
			if(url == null ||"".equals(url)){
				url = "/welcome.jsp";
			}
			getResponse().sendRedirect(url);
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	/**
	 * 页面转发
	 * @param url
	 */
	public void dispatch(String url){
		try{
			if(url == null ||"".equals(url)){
				url = "/welcome.jsp";
			}
			getRequest().getRequestDispatcher(url).forward(getRequest(), getResponse());
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	/**
	 * 参数字符串值
	 * @param key
	 * @return
	 */
	public String stringValue(String key) {
		Object str = params.get(key);
		if(str == null) {
			return null;
		}else {
			return str.toString();
		}
	}
	
	public String stringValue(String key, String defaultValue) {
		String value = stringValue(key);
		if(value == null) {
			value = defaultValue;
		}
		return value;
	}
	
	/**
	 * 参数整数串值
	 * @param key
	 * @return
	 */
	public Integer intValue(String key) {
		Object str = params.get(key);
		if(str == null) {
			return null;
		}else {
			return Integer.valueOf(params.get(key) + "");
		}
	}
	
	public Integer intValue(String key, Integer defaultValue) {
		Integer value = intValue(key);
		if(value == null) {
			value = defaultValue;
		}
		return value;
	}

	/**
	 * 参数long串值
	 * @param key
	 * @return
	 */
	public Long longValue(String key) {
		Object str = params.get(key);
		if(str == null) {
			return null;
		}else {
			return Long.valueOf(params.get(key) + "");
		}
	}

	public Long longValue(String key, Long defaultValue) {
		Long value = longValue(key);
		if(value == null) {
			value = defaultValue;
		}
		return value;
	}
	
	/**
	 * 参数整数串值
	 * @param key
	 * @return
	 */
	public Double doubleValue(String key) {
		Object str = params.get(key);
		if(str == null) {
			return null;
		}else {
			return Double.valueOf(params.get(key) + "");
		}
	}
	
	public Map<String, String> toStringMap(){
		Map<String, String> stringMap = new HashMap<String, String>();
		if(params != null) {
			for(String key : params.keySet()) {
				stringMap.put(key, stringValue(key));
			}
		}
		return stringMap;
	}
	
	public void log() {
		
	}
	
	public User getUser() {
		return UserManager.get();
	}
	
	protected void addEnv(BIModelAndView v) {
		String ssoServer = "";
		String portalServer = "";
		RuntimeEnv env = BIUtil.getRuntimeEnv();
		switch(env) {
		case Dev:
			ssoServer = "https://access.example.com";
			portalServer = BIUtil.getBasePath(this.getRequest());
			break;
		case Test:
			ssoServer = "https://access.example.com";
			portalServer = "https://biportal.example.com";
			break;
		case UT:
			ssoServer = "https://accessut.example.com";
			portalServer = "https://bissdserver-ut.example.com";
			break;
		case Product:
		default:
			ssoServer = "https://access.example.com";
			portalServer =  BIUtil.getBasePath(this.getRequest());
			break;
		}
		v.addJsVariable("ssoServer", ssoServer);
		v.addJsVariable("portalServer", portalServer);
	}
	
	public String appendInnerParameters(String url){
		try{
			// 用户名
			User user = getUser();
			Map<String, String> urlParams = BIUtil.parseURL(url);
			List<String> buildInList = new ArrayList<String>();
			if(user != null){
				if(!urlParams.containsKey("userName")){
					buildInList.add("userName=" + user.getName());
				}
				if(!urlParams.containsKey("user_name")){
					buildInList.add("user_name=" + user.getName());
				}
//				if(!urlParams.containsKey(BIConsts.User_Token)){
//					buildInList.add(BIConsts.User_Token + "=" + user.getToken());
//				}
			}
			
			String buildInStr = "";
			for(int i = 0; i < buildInList.size(); i++){
				if(i == 0){
					buildInStr = buildInList.get(i);
				}else{
					buildInStr = buildInStr + "&" + buildInList.get(i);
				}
			}
			
			if(!"".equals(buildInStr)){
				if(url.indexOf("?") != -1){
					url = url + "&" + buildInStr;
				}else{
					url = url + "?" + buildInStr;
				}
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		
		return url;
	}
}
