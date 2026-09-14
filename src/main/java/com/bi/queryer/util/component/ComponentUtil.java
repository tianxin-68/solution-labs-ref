package com.bi.queryer.util.component;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.servlet.http.HttpServletRequest;

import com.bi.queryer.sys.base.BaseDao;
import com.bi.queryer.sys.config.SystemConfig;
import com.bi.queryer.sys.db.DataType;
import com.bi.queryer.util.BIUtil;
import com.bi.queryer.util.SpringContextUtil;
import com.bi.queryer.util.StringUtil;
import com.bi.queryer.util.component.api.ComponentRequest;
import com.bi.queryer.util.component.common.Platform;
import com.bi.queryer.util.component.common.Theme;
import com.bi.queryer.util.component.datasource.Datasource;
import com.bi.queryer.util.component.datasource.DatasourceParameter;
import com.bi.queryer.util.component.datasource.DefaultDataSetProvider;

public abstract class ComponentUtil {
	
	public static Object getBean(String beanName){
		Object bean = SpringContextUtil.getBean(beanName);
		return bean;
	}
	
	public static DefaultDataSetProvider getDefaultDataSetProvider(){
		DefaultDataSetProvider provider = (DefaultDataSetProvider) getBean("defaultDataSetProvider");
		return provider;
	}
	
	/**
	 * 获取完整的URL,带http://
	 * @param request
	 * @param url
	 * @return
	 */
	public static String getFullURL(ComponentRequest request, String url){
		String baseURL = BIUtil.getBasePath(request.getServletRequest()) ;
		if(url == null) return baseURL;
		String fullURL = url;
		if(url.toUpperCase().indexOf("HTTP") != -1){
			return url;
		}else if(url.startsWith("/")){
			url = url.replaceFirst("\\/", "");
		}
		fullURL = baseURL + url ;
		return fullURL;
	}
	
	/**
	 * 通过平台获取
	 * @param platform
	 * @return
	 */
	public static String getTheme(String theme){
		return getTheme(theme, Platform.PC.toString());
	}
	public static String getTheme(String theme, HttpServletRequest request){
		return getTheme(theme, request.getParameter(ComponentConstants.Parameter_Platform));
	}
	public static String getTheme(String theme, String platform){
		Theme t = Theme.get(theme);
		if(t == Theme.Auto){
			Platform p = Platform.get(platform);
			if(p == Platform.Android || p == Platform.IOS){
				return Theme.Black.toString().toLowerCase();
			}else{
				return Theme.Default.toString().toLowerCase();
			}
		}else {
			return t.toString().toLowerCase();
		}
	}
	
	/**
	 * portal运行时SQL日志
	 * @param log
	 */
	public static void log(final ComponentSQLLog log) {
		ExecutorService  es = Executors.newSingleThreadExecutor();
		es.execute(new Runnable() {
			@Override
			public void run() {
				BaseDao dao = (BaseDao) SpringContextUtil.getBean("baseDao");
				dao.insert("designerLog.logSQL", log);
			}
		});
	}
	
	/**
	 * 记录日志
	 * @param title
	 * @param info
	 */
	public static void log(String title, Object info){
		if(!SystemConfig.isDebug){
			return;
		}
		Calendar c = Calendar.getInstance();
		String time = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(c.getTime());
		
		String infoStr = "";
		if(info != null) {
			infoStr = info.toString();
		}
		/*
		if(info instanceof List){
			List<?> list = (List<?>) info;
			for(Object o : list){
				infoStr = infoStr + ";" + o;
			}
		}
		if(info instanceof Map){
			Map<?, ?> map = (Map<?, ?>) info;
			for(Object value : map.values()){
				infoStr = infoStr + ";" + value;
			}
		}
		*/
		
		System.out.println("★☆Component Log[" + time + "] \t" + title + ":" + infoStr);
	}
	
	/**
	 * 
	 * @param ds 数据源
	 * @param queryParams 原始查询参数
	 * @return 合并查询参数：数据源的参数列表和外部传递过来的参数进行合并，且对字符串类型参数添加单引号
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static Map mergeQueryParameters(Datasource ds, Map queryParams){
		if(ds == null || ds.getParameters() == null || ds.getParameters().isEmpty()) {
			return queryParams;
		}
		Map resultParams = new HashMap();
		Map<String, DatasourceParameter> dsParamMap = new HashMap<String, DatasourceParameter>();
		for(DatasourceParameter dp : ds.getParameters()){
			dsParamMap.put(dp.getId(), dp);
			if(dp.getDataType() == DataType.String && !StringUtil.isEmpty(dp.getValue())) {
				resultParams.put(dp.getId(), wrapQueryString(dp.getValue()));
			}else {
				resultParams.put(dp.getId(), dp.getValue());
			}
		}
		
		if(queryParams == null) queryParams = new HashMap();
		for(Object key : queryParams.keySet()){
			Object value = queryParams.get(key);
			if(dsParamMap.containsKey(key)){
				if(dsParamMap.get(key).getDataType() == DataType.String && !StringUtil.isEmpty(value + "")) {
					value = wrapQueryString(value + "");
				}
			}
			resultParams.put(key, value);
		}
		return resultParams;
	}
	
	/**
	 * 对SQL参数的字符串进行包装
	 * @param valueStr
	 * @return
	 */
	public static String wrapQueryString(String valueStr){
		if(StringUtil.isEmpty(valueStr)) {
			return "";
		}
		String newValueStr = "";
		if(valueStr.indexOf(",") != -1) {
			List<String> valueList = Arrays.asList(valueStr.split(","));
			newValueStr = BIUtil.listToStr(valueList, ",", "'");
		}else {
			newValueStr = "'" + valueStr + "'";
		}
		return newValueStr;
	}
}
