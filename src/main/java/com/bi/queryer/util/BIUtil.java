package com.bi.queryer.util;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import com.bi.queryer.ssm.enums.AggExpressionType;
import com.bi.queryer.ssm.enums.TerminalType;
import com.bi.queryer.sys.config.SC;
import com.bi.queryer.sys.db.DBUtil;
import com.bi.queryer.sys.db.DataType;
import com.bi.queryer.sys.enums.RuntimeEnv;
import com.bi.queryer.sys.env.EnvType;
import com.bi.queryer.sys.env.EnvVariableManager;
import com.bi.queryer.sys.env.RegVariable;
import com.bi.queryer.sys.user.UserManager;
import com.bi.queryer.sys.user.vo.User;
import com.bi.queryer.util.encryption.DesEncryption;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.sun.mail.util.MailSSLSocketFactory;
import org.apache.commons.lang3.StringUtils;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.net.InetAddress;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public abstract class BIUtil {

	public static final SerializerFeature[] features = {SerializerFeature.WriteMapNullValue, // 输出空置字段
			// SerializerFeature.WriteNullListAsEmpty,
			// // list字段如果为null，输出为[]，而不是null
			// SerializerFeature.WriteNullNumberAsZero,
			// // 数值字段如果为null，输出为0，而不是null
			// SerializerFeature.WriteNullBooleanAsFalse,
			// // Boolean字段如果为null，输出为false，而不是null
			// SerializerFeature.WriteNullStringAsEmpty,
			// // 字符类型字段如果为null，输出为""，而不是null
			SerializerFeature.WriteDateUseDateFormat // 日期格式化yyyy-MM-dd
			// HH:mm:ss
	};

	/**
	 * 获取运行时环境
	 *
	 * @return
	 */
	public static RuntimeEnv getRuntimeEnv() {
		Map<String, String> systemEnv = System.getenv();
		String envValue = systemEnv.get("CONFIGENV");
		if (StringUtil.isEmpty(envValue)) {
			envValue = systemEnv.get("config.env");
		}
		Properties systemProp = System.getProperties();
		if (StringUtil.isEmpty(envValue) && systemProp != null) {
			envValue = systemProp.getProperty("config.env");
		}
		RuntimeEnv env = RuntimeEnv.getEnvByValue(envValue);
		if (env == null || env == RuntimeEnv.Unknow) {// 未配置环境，则为生产环境
			env = RuntimeEnv.Product;
		}

		return env;
	}

	public static String getJDBCFileName() {
		RuntimeEnv env = BIUtil.getRuntimeEnv();
		if (env == null || env == RuntimeEnv.Unknow) {// 未配置环境，则为生产环境
			env = RuntimeEnv.Product;
		}
		String fileName = "";
		switch (env) {
			case Dev:
				fileName = "jdbc.dev.properties";
				break;
			case DevFms:
				fileName = "jdbc.dev_fms.properties";
				break;
			case Test:
				fileName = "jdbc.test.properties";
				break;
			case TestFms:
				fileName = "jdbc.test_fms.properties";
				break;
			case UT:
				fileName = "jdbc.ut.properties";
				break;
			case ProductFms:
				fileName = "jdbc.fms.properties";
				break;
			case Product:
			case Unknow:
			default:
				fileName = "jdbc.properties";
				break;
		}
		return fileName;
	}

	public static String getWebsocketBasePath(HttpServletRequest request) {
		String basePath = getBasePath(request);
		String schema = SC.v("app.protocol", "http");
		if ("http".equalsIgnoreCase(schema)) {

		}
		String ws = basePath.replaceFirst("http://", "ws://").replaceFirst("https://", "wss://");
		return ws;
	}

	/**
	 * 获取应用Base路径,结尾带/
	 *
	 * @param request
	 * @return 应用Base路径
	 */
	public static String getBasePath(HttpServletRequest request) {
		String path = getBaseURL(request) + request.getContextPath();
		return path;
	}

	/**
	 * 获取上下文路径，不带协议
	 *
	 * @param request
	 * @return
	 */
	public static String getContextPath(HttpServletRequest request) {
		String path = getBasePath(request);
		path = path.replace("http://", "").replace("https://", "");
		return path;
	}

	/**
	 * 获取请求基路径
	 *
	 * @param request
	 * @return
	 */
	public static String getBaseURL(HttpServletRequest request) {
		String schema = SC.v("app.protocol", "http");
		String url = request.getRequestURL().toString();
		String prefix = schema + "://";
		url = url.replaceFirst("http://", "").replaceFirst("https://", "");
		int index = url.indexOf("/");
		url = url.substring(0, index);
		url = prefix + url;
		return url;
	}

	/**
	 * 判断字符串是否空串， 字符串为“null”或“NULL”时也视为空
	 *
	 * @param str 要进行空值判断的字符串
	 * @return 为空则返回true
	 */
	public static boolean isEmpty(String str) {
		boolean rtn = false;
		if (str == null || str.equalsIgnoreCase("")
				|| str.equalsIgnoreCase("null")) {
			rtn = true;
		}
		return rtn;
	}

	public static boolean isNotEmpty(String str) {
		return !isEmpty(str);
	}

	public static boolean isEmpty(Collection<?> c) {
		if (c == null || c.isEmpty()) {
			return true;
		} else {
			return false;
		}
	}

	public static boolean isNotEmpty(Collection<?> c) {
		return !isEmpty(c);
	}

	public static boolean isEmpty(Map<?, ?> c) {
		if (c == null || c.isEmpty()) {
			return true;
		} else {
			return false;
		}
	}

	public static boolean isNotEmpty(Map<?, ?> c) {
		return !isEmpty(c);
	}


	public static boolean isEmpty(BitSet bitSet) {
		return (bitSet == null || bitSet.isEmpty());
	}

	/**
	 * 获取请求IP地址
	 *
	 * @param request
	 * @return
	 */
	public static String getClientIP(HttpServletRequest request) {
		String ip = request.getHeader("x-forwarded-for");
		if (ip == null || ip.length() == 0 || ip.equalsIgnoreCase("unknown")) {
			ip = request.getHeader("Proxy-Client-IP");
		}
		if (ip == null || ip.length() == 0 || ip.equalsIgnoreCase("unknown")) {
			ip = request.getHeader("WL-Proxy-Client-IP");
		}
		if (ip == null || ip.length() == 0 || ip.equalsIgnoreCase("unknown")) {
			ip = request.getRemoteAddr();
			if (ip.equals("127.0.0.1") || ip.equals("0:0:0:0:0:0:0:1")) {
				// 根据网卡取本机配置的IP
				InetAddress inet = null;
				try {
					inet = InetAddress.getLocalHost();
				} catch (UnknownHostException e) {
					e.printStackTrace();
				}
				ip = inet.getHostAddress();
			}
		}
		// 对于通过多个代理的情况，第一个IP为客户端真实IP,多个IP按照','分割
		if (ip != null && ip.length() > 15) { // "***.***.***.***".length() = 15
			if (ip.indexOf(",") > 0) {
				ip = ip.substring(0, ip.indexOf(","));
			}
		}
		return ip;
	}

	public static String getServerIP() {
		Map<String, String> envMap = System.getenv();
		if (envMap.containsKey("INS_ADDR") && !StringUtil.isEmpty(envMap.get("INS_ADDR"))) {
			return envMap.get("INS_ADDR");
		}
		String ip = "";
		try {
			ip = InetAddress.getLocalHost().getHostAddress();
			// System.out.println(ip);
		} catch (UnknownHostException e1) {
			e1.printStackTrace();
		}
		return ip;
	}

	public static String[] getServerIPTable() {
		String ipList = SC.v("ssm.server.iptable");
		System.out.println("ipList:" + ipList);
		String localIP = getServerIP();
		if (StringUtil.isEmpty(ipList)) {
			ipList = localIP;
		}
		String[] serverIPTable = ipList.split(",");
		List<String> ipTable = Arrays.asList(serverIPTable);
		List<String> newIPTable = new ArrayList<String>();
		newIPTable.addAll(ipTable);
		if (!ipTable.contains(localIP)) {
			newIPTable.add(localIP);
		}
		newIPTable.toArray(serverIPTable);
		return newIPTable.toArray(serverIPTable);
	}

	public static boolean isStaticResource(HttpServletRequest request) {
		String uri = request.getRequestURI();
		boolean result = uri.contains("/static/") || uri.contains("favicon.ico");
		return result;
	}

	/**
	 * list转为字符串
	 *
	 * @param list
	 * @param itemSeparator
	 * @param itemWrapString
	 * @return
	 */
	public static String listToStr(Collection<? extends Object> list, String itemSeparator, String itemWrapString) {
		return listToStr(list, itemSeparator, itemWrapString, itemWrapString);
	}

	/**
	 * list转为字符串
	 *
	 * @param list
	 * @param itemSeparator
	 * @param
	 * @return
	 */
	public static String listToStr(Collection<? extends Object> list, String itemSeparator, String itemLeftWrapString, String itemRightWrapString) {
		StringBuilder sb = new StringBuilder();
		if (list == null || list.isEmpty()) {
			return "";
		}
		Iterator iter = list.iterator();
		int i = -1;
		while (iter.hasNext()) {
			i++;
			Object item = iter.next(); //list.get(i);
			String itemStr = itemLeftWrapString + item + itemRightWrapString;
			if (i == 0) {
				sb.append(itemStr);
			} else {
				sb.append(itemSeparator).append(itemStr);
			}
		}
		return sb.toString();
	}

	public static String listToStr(Collection<? extends Object> list, String itemSeparator) {
		return listToStr(list, itemSeparator, "");
	}

	public static <E> ArrayList<E> newArrayList(E[] elements) {
		if (elements == null) {
			return null;
		}
		ArrayList<E> list = new ArrayList<E>(elements.length);
		Collections.addAll(list, elements);
		return list;
	}

	public static String listToStr(Collection<? extends Object> list) {
		return listToStr(list, ",", "");
	}

	/**
	 * 最小公倍数
	 *
	 * @return
	 */
	public static int minMultiple(int a, int b) {
		int r = a, s = a, t = b;
		if (a < b) {
			r = a;
			a = b;
			b = r;
		}
		while (r != 0) {
			r = a % b;
			a = b;
			b = r;
		}
		return s * t / a;
	}

	/**
	 * 最小公倍数
	 */
	public static int minCommonMultiple(List<Integer> datas) {
		if (datas == null)
			return 1;
		int len = datas.size();
		if (len == 1)
			return datas.get(0);
		if (len % 2 != 0) {
			datas.add(1);
		}
		List<Integer> newDatas = new ArrayList<Integer>();
		for (int i = 0; i < len; i++) {
			newDatas.add(minMultiple(datas.get(i), datas.get(++i)));
		}
		return minCommonMultiple(newDatas);
	}

	/**
	 * 最小公倍数
	 */
	public static int minCommonMultiple(Integer[] datas) {
		List<Integer> dataList = new ArrayList<Integer>();
		for (Integer d : datas) {
			dataList.add(d);
		}
		return minCommonMultiple(dataList);
	}

	/**
	 * 含外部参数信息替换的动态字符串生成
	 */
	public static String getStrWithReg(String str, Map<String, String> infoMap) {
		return getStrWithReg(str, infoMap, "#");
	}

	/**
	 * 含外部参数信息替换的动态字符串生成
	 */
	public static String getStrWithReg(String str, Map<String, String> infoMap, String reg) {
		String expression = "";
		return getStrWithReg(expression);
	}

	/**
	 * 正则获取包含正则表达式的动态字符串方法
	 */
	public static String getStrWithReg(String str) {
		if (StringUtil.isEmpty(str)) {
			return str;
		}
		StringBuffer result = new StringBuffer();
		result.append(str);
		for (RegVariable var : RegVariable.values()) {
			getStrWithReg(result, var);
		}
		return result.toString();
	}

	/**
	 * 通过具体的正则表达式生成字符串
	 *
	 * @param str
	 * @param var
	 * @return
	 */
	private static void getStrWithReg(StringBuffer str, RegVariable var) {
		if (str == null || StringUtil.isEmpty(str.toString())) {
			return;
		}
		String label = var.getLabel();
		Pattern pattern = Pattern.compile(label + "\\s*\\{[^\\}]+\\}");
		String rst = str.toString();
		Matcher matcher = pattern.matcher(rst);
		while (matcher.find()) {
			// 需要转义字符替换方案
			String[][] replaceInfo = new String[][]{{"\\(", "__aaa__"}, {"\\)", "__bbb__"}};
			String value = "";
			String key = matcher.group();
			for (String[] info : replaceInfo) {
				key = key.replaceAll(info[1], info[0]);
			}
			switch (var) {
				case Normal:
					value = EnvVariableManager.value(key);
					break;
				case Sql:
					try {
						value = parseSql(key);
					} catch (Exception e) {
						e.printStackTrace();
					}
					break;
			}
			StringBuffer reg = new StringBuffer();
			reg.append(label).append("\\{");
			key = key.replaceAll(label + "\\{", "").replaceAll("\\}", "");
			for (String[] info : replaceInfo) {
				key = key.replaceAll(info[0], info[1]);
			}
			reg.append(key).append("\\}");
			for (String[] info : replaceInfo) {
				rst = rst.replaceAll(info[0], info[1]);
			}
			rst = rst.replaceAll(reg.toString(), value);
		}
		str.setLength(0);
		str.append(rst);
	}

	/**
	 * 解析单行表达式
	 */
	public static String parseSql(String expression) throws Exception {
		String value = "";
		if (StringUtil.isEmpty(expression)) {
			return value;
		}
		StringBuffer sb = new StringBuffer();
		sb.append("SELECT (").append(expression).append(") FROM DUAL");
		Connection conn = null;
		PreparedStatement psp = null;
		ResultSet rst = null;
		try {
			conn = DBUtil.getConn();
			psp = conn.prepareStatement(sb.toString());
			rst = psp.executeQuery();
			while (rst.next()) {
				value = StringUtil.ifNull(rst.getObject(1));
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new Exception("decode表达式有误");
		} finally {
			if (rst != null) {
				rst.close();
			}
			if (psp != null) {
				psp.close();
			}
			if (conn != null) {
				conn.close();
			}
		}
		return value;
	}

	public static Object nvl(Object o) {
		if (o instanceof String) {
			return nvl(o, "");
		}
		if (o instanceof Integer) {
			return nvl(o, 0);
		}
		if (o instanceof BigDecimal) {
			return nvl(o, new BigDecimal(0));
		}
		if (o instanceof Double) {
			return nvl(o, 0D);
		}
		return o;
	}

	public static String nvl(Object str, String nullValue) {
		if (isEmpty(str + "")) {
			return nullValue;
		}
		return str.toString();
	}

	public static Double nvl(Object d, Double nullValue) {
		if (d == null) {
			return nullValue;
		}
		return (Double) d;
	}

	public static Integer nvl(Object d, Integer nullValue) {
		if (d == null) {
			return nullValue;
		}
		if (d instanceof String) {
			try {
				return Integer.valueOf(d.toString());
			} catch (Exception e) {

			}
		}
		return (Integer) d;
	}

	public static BigDecimal nvl(Object d, BigDecimal nullValue) {
		if (d == null) {
			return nullValue;
		}
		return (BigDecimal) d;
	}

	/**
	 * 执行命令行
	 */
	public static boolean cmdProcess(String cmd_info) {
		boolean b = false;
		String show = "";
		try {
			String exePrel = cmd_info;
			show += "执行命令：\n" + exePrel + "\n";
			Process child = Runtime.getRuntime().exec(exePrel);
			InputStream in = child.getInputStream();
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String line;
			line = br.readLine();
			while (line != null) {
				show += line + "\n";
				line = br.readLine();
			}
			try {
				child.waitFor();
				int returnValue = child.exitValue();
				show += "返回值为：" + returnValue + "\n";

				if (0 == returnValue) {
					show += "执行成功\n";
					b = true;
				} else {
					show += "执行失败\n";
				}
			} catch (Exception e) {
				show += e.toString();
				e.printStackTrace();
			}
		} catch (Exception e) {
			show += e.toString();
			e.printStackTrace();
		} finally {
			System.out.println(show);
		}
		return b;
	}

	public static String replaceEach(String str, String[] placeholders, String[] placements) {
		if (isEmpty(str)) {
			return str;
		}
		return StringUtils.replaceEach(str, placeholders, placements);
		/*
		for(int i = 0; i < placeholders.length; i++) {
			if(i > placements.length - 1) {
				break;
			}
			StringUtils.replaceEach(text, searchList, replacementList);
			str = str.replaceAll(placeholders[i], placements[i]);
		}
		return str;
		*/
	}

	public static User getLocalUser() {
		return null;
	}

	/**
	 * 分割List
	 *
	 * @param list     待分割的list
	 * @param pageSize 每段list的大小
	 * @return List<< List < T>>
	 * @author contributor
	 * @date 2012.1.13
	 */
	public static <T> List<List<T>> splitList(List<T> list, int pageSize) {

		int listSize = list.size(); // list的大小
		int page = (listSize + (pageSize - 1)) / pageSize; // 页数

		List<List<T>> listArray = new ArrayList<List<T>>(); // 创建list数组
		// ,用来保存分割后的list
		for (int i = 0; i < page; i++) { // 按照数组大小遍历
			List<T> subList = new ArrayList<T>(); // 数组每一位放入一个分割后的list
			for (int j = 0; j < listSize; j++) { // 遍历待分割的list
				int pageIndex = ((j + 1) + (pageSize - 1)) / pageSize; // 当前记录的页码(第几页)
				if (pageIndex == (i + 1)) { // 当前记录的页码等于要放入的页码时
					subList.add(list.get(j)); // 放入list中的元素到分割后的list(subList)
				}

				if ((j + 1) == ((j + 1) * pageSize)) { // 当放满一页时退出当前循环
					break;
				}
			}
			listArray.add(subList); // 将分割后的list放入对应的数组的位中
		}
		return listArray;
	}


	/**
	 * 将一组数据平均分成n组
	 *
	 * @param source 要分组的数据源
	 * @param n      平均分成n组
	 * @param <T>
	 * @return
	 */
	public static <T> List<List<T>> averageAssign(List<T> source, int n) {
		List<List<T>> result = new ArrayList<List<T>>();
		int remainder = source.size() % n;  //(先计算出余数)
		int number = source.size() / n;  //然后是商
		int offset = 0;//偏移量
		for (int i = 0; i < n; i++) {
			List<T> value = null;
			if (remainder > 0) {
				value = source.subList(i * number + offset, (i + 1) * number + offset + 1);
				remainder--;
				offset++;
			} else {
				value = source.subList(i * number + offset, (i + 1) * number + offset);
			}
			result.add(value);
		}
		return result;
	}

	/**
	 * 判断字符串是否为整数
	 *
	 * @param str
	 * @return
	 */
	public static boolean isInteger(String str) {
		Pattern pattern = Pattern.compile("^[-\\+]?[\\d]*$");
		return pattern.matcher(str).matches();
	}

	/**
	 * 将给定字符串中环境变量替换为实际值
	 *
	 * @param str
	 * @return
	 */
	public static String replaceEnvVariables(String str) {
		return replaceEnvVariables(str, EnvType.System);
	}

	/**
	 * 将给定字符串中环境变量替换为实际值
	 *
	 * @param str
	 * @return
	 */
	public static String replaceEnvVariables(String str, EnvType type) {
		if (StringUtil.isEmpty(str)) {
			return str;
		}
//		Pattern pattern = Pattern.compile("(?<=\\$\\{)(.+?)(?=\\})");
		Pattern pattern = Pattern.compile("(?<=)(\\$\\{.+?\\})(?=)");
		Matcher matcher = pattern.matcher(str);
		Map<String, String> variableValues = new HashMap<String, String>();
		while (matcher.find()) {
			String variableName = matcher.group();
			String variableValue = EnvVariableManager.value(variableName, type);
			variableValues.put(variableName, variableValue);
		}
		for (Entry<String, String> entry : variableValues.entrySet()) {
			str = StringUtils.replace(str, entry.getKey(), entry.getValue());
//			str = str.replaceAll(entry.getKey(), entry.getValue());
		}
		return str;
	}

	/**
	 * 随机字符串
	 *
	 * @param length
	 * @return
	 */
	public static String randomString(int length) { //length表示生成字符串的长度
		String base = "abcdefghijklmnopqrstuvwxyz0123456789";
		Random random = new Random();
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < length; i++) {
			int number = random.nextInt(base.length());
			sb.append(base.charAt(number));
		}
		return sb.toString();
	}

	public static void addCookie(String key, String value, HttpServletResponse response) {

//		value = DesEncryption.encrypt(value);
		Cookie cookie = new Cookie(key, value);
		cookie.setPath("/");
		int time = 60 * 60 * 24;
		cookie.setMaxAge(time);
		response.addCookie(cookie);
	}

	public static String getCookie(String key, HttpServletRequest request) {
		Cookie[] cookies = request.getCookies();
		String value = null;
		if (cookies != null) {
			for (Cookie cookie : cookies) {
				String cookieItemName = cookie.getName();
				if (cookieItemName.equals(key)) {
					value = cookie.getValue();
//					value = DesEncryption.decrypt(value);
					break;
				}
			}
		}
		return value;
	}

	public static void removeCookie(String key, HttpServletRequest request, HttpServletResponse response) {
		Cookie[] cookies = request.getCookies();
		if (cookies != null) {
			for (Cookie cookie : cookies) {
				String cookieItemName = cookie.getName();
				if (cookieItemName.equals(key)) {
					cookie.setValue(null);
					cookie.setMaxAge(0);
					cookie.setPath("/");
					response.addCookie(cookie);
//			    	break;
				}
			}
		}
	}

	public static String toJSONString(Object object) {
		return JSON.toJSONString(object, features);
	}

	public static JSONObject toJSONObject(Object object) {
		return JSONObject.parseObject(toJSONString(object));
	}

	public static JSONArray toJSONArray(Object object) {
		return JSONArray.parseArray(toJSONString(object));
	}

	public static Map<String, Object> fetchUrlParameters(String url) {
		Map<String, Object> params = new LinkedHashMap<String, Object>();
		if (StringUtil.isEmpty(url) || url.indexOf("?") == -1) {
			return params;
		}
		String paramStr = url.substring(url.indexOf("?") + 1);
		return fetchParameters(paramStr);
	}

	public static Map<String, Object> fetchParameters(String paramStr) {
		Map<String, Object> params = new LinkedHashMap<String, Object>();
		if (isEmpty(paramStr)) {
			return params;
		}
		String[] paramItems = StringUtils.split(paramStr, "&");
		for (String item : paramItems) {
			String[] keyValue = StringUtils.split(item, "=");
			if (keyValue.length > 0) {
				if (keyValue.length == 1) {
					params.put(keyValue[0], "");
				} else {
					String value = EnvVariableManager.value(keyValue[1]);
					params.put(keyValue[0], value);
				}
			}
		}
		return params;
	}

	/**
	 * 构造集合创建时间
	 *
	 * @param t
	 * @param <T>
	 * @return
	 */
	public static <T> void buildCreateTime(List<T> t) {

		try {

			if (isNotEmpty(t)) {
				int i = 0;
				for (T t1 : t) {
					Field f = t1.getClass().getDeclaredField("createdTime");
					f.setAccessible(true);
					//扩大时间间隔
					f.set(t1, new Timestamp(System.currentTimeMillis() + i * 1000));

					i++;
				}
			}
		} catch (Exception e) {

		}

	}

	/**
	 * 转换URL：将系统内置变量转为实际值
	 *
	 * @param url
	 * @return
	 */
	public static String parseEnvURL(String url) {
		String newUrl = url;
		try {
			if (StringUtil.isEmpty(url) || url.indexOf("?") == -1) {
				return url;
			}
			String baseUrl = url.substring(0, url.indexOf("?"));
			Map<String, String> params = parseURL(url);
			String param = "";
			for (Entry<String, String> entry : params.entrySet()) {
				param = param + "&" + URLEncoder.encode(entry.getKey(), "UTF-8") + "="
						+ URLEncoder.encode(entry.getValue(), "UTF-8");
			}
			param = param.replaceFirst("\\&", "");
			newUrl = baseUrl + "?" + param;
		} catch (Exception e) {
			e.printStackTrace();
		}

		return newUrl;
	}

	/**
	 * 解析URL返回URL参数列表
	 *
	 * @param url
	 * @return
	 */
	public static Map<String, String> parseURL(String url) {
		Map<String, String> params = new LinkedHashMap<String, String>();
		if (StringUtil.isEmpty(url) || url.indexOf("?") == -1) {
			return params;
		}
		String paramStr = url.substring(url.indexOf("?") + 1);
		String[] paramItems = StringUtils.split(paramStr, "&");
		for (String item : paramItems) {
			String[] keyValue = StringUtils.split(item, "=");
			if (keyValue.length > 0) {
				if (keyValue.length == 1) {
					params.put(keyValue[0], "");
				} else {
					String value = EnvVariableManager.value(keyValue[1]);
					params.put(keyValue[0], value);
				}
			}
		}
		return params;
	}

	public static String firstLowerCase(String s) {
		if (Character.isLowerCase(s.charAt(0)))
			return s;
		else
			return (new StringBuilder()).append(Character.toLowerCase(s.charAt(0))).append(s.substring(1)).toString();
	}

	public static boolean isNumber(String str) {
		if (isEmpty(str)) {
			return false;
		}
		String reg = "\\d+(\\.\\d+)?";
		return str.matches(reg);

	}

	/**
	 * 发送邮件
	 *
	 * @param subject
	 * @param htmlContent
	 */
	public static void sendMail(String subject, String htmlContent, List<String> receiverMailSet) {

		try {
			Properties prop = new Properties();
			//协议
			prop.setProperty("mail.transport.protocol", "smtp");
			//服务器
			prop.setProperty("mail.smtp.host", SC.v("mail.host", "smtp.exmail.qq.com"));
			//端口
			prop.setProperty("mail.smtp.port", SC.v("mail.host.port", "465"));
			//使用smtp身份验证
			prop.setProperty("mail.smtp.auth", "true");
			//使用SSL，企业邮箱必需！
			//开启安全协议
			MailSSLSocketFactory sf = null;
			try {
				sf = new MailSSLSocketFactory();
				sf.setTrustAllHosts(true);
				prop.put("mail.smtp.ssl.enable", "true");
				prop.put("mail.smtp.ssl.socketFactory", sf);
			} catch (GeneralSecurityException e1) {
				e1.printStackTrace();
			}

			String account = SC.v("mail.from", "bi_service@example.com");
			String password = SC.v("mail.from.password");
			String nickName = SC.v("mail.from.username", "数据服务");
			password = DesEncryption.decrypt(password);
			Session session = Session.getDefaultInstance(prop, new MailAuthenticator(account, password));
			MimeMessage mimeMessage = new MimeMessage(session);
			//发件人
			mimeMessage.setFrom(new InternetAddress(account, nickName));        //可以设置发件人的别名
			//mimeMessage.setFrom(new InternetAddress(account));    //如果不需要就省略
			//收件人
			mimeMessage.addRecipients(Message.RecipientType.TO, CollectionUtil.join(receiverMailSet, ","));
			//主题
			mimeMessage.setSubject(subject);
			//时间
			mimeMessage.setSentDate(new Date());
			//容器类，可以包含多个MimeBodyPart对象
			Multipart mp = new MimeMultipart();

			//MimeBodyPart可以包装文本，图片，附件
			MimeBodyPart body = new MimeBodyPart();
			//HTML正文
			body.setContent(htmlContent, "text/html; charset=UTF-8");
			mp.addBodyPart(body);

			//设置邮件内容
			mimeMessage.setContent(mp);
			//仅仅发送文本
			//mimeMessage.setText(content);
			mimeMessage.saveChanges();
			Transport.send(mimeMessage);
			System.out.println("邮件发送成功..");

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 发送邮件
	 *
	 * @param subject
	 * @param htmlContent
	 */
	public static void sendMail(String subject, String htmlContent, String receiverMailAddress, String ccMailAddress) {
		try {
			Properties prop = new Properties();
			//协议
			prop.setProperty("mail.transport.protocol", "smtp");
			//服务器
			prop.setProperty("mail.smtp.host", SC.v("mail.host", "smtp.exmail.qq.com"));
			//端口
			prop.setProperty("mail.smtp.port", SC.v("mail.host.port", "465"));
			//使用smtp身份验证
			prop.setProperty("mail.smtp.auth", "true");
			//使用SSL，企业邮箱必需！
			//开启安全协议
			MailSSLSocketFactory sf = null;
			try {
				sf = new MailSSLSocketFactory();
				sf.setTrustAllHosts(true);
				prop.put("mail.smtp.ssl.enable", "true");
				prop.put("mail.smtp.ssl.socketFactory", sf);
			} catch (GeneralSecurityException e1) {
				e1.printStackTrace();
			}

			String account = SC.v("mail.from", "bi_service@example.com");
			String password = SC.v("mail.from.password");
			String nickName = SC.v("mail.from.username", "数据服务");
			password = DesEncryption.decrypt(password);
			Session session = Session.getDefaultInstance(prop, new MailAuthenticator(account, password));

			MimeMessage mimeMessage = new MimeMessage(session);
			try {
				//发件人
				mimeMessage.setFrom(account);        //可以设置发件人的别名
				//收件人
				mimeMessage.addRecipients(Message.RecipientType.TO, receiverMailAddress);

				if (BIUtil.isNotEmpty(ccMailAddress)) {
					//抄送人
					mimeMessage.setRecipients(Message.RecipientType.CC, ccMailAddress);
				}

				//主题
				mimeMessage.setSubject(subject);
				//时间
				mimeMessage.setSentDate(new Date());
				//容器类，可以包含多个MimeBodyPart对象
				Multipart mp = new MimeMultipart();

				//MimeBodyPart可以包装文本，图片，附件
				MimeBodyPart body = new MimeBodyPart();
				//HTML正文
				body.setContent(htmlContent, "text/html; charset=UTF-8");
				mp.addBodyPart(body);

				//设置邮件内容
				mimeMessage.setContent(mp);
				//仅仅发送文本
				//mimeMessage.setText(content);
				mimeMessage.saveChanges();
				Transport.send(mimeMessage);
				System.out.println("邮件发送成功..");
			} catch (Exception e) {
				e.printStackTrace();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void send(String subject, String htmlContent) {
		Properties prop = new Properties();
		//协议
		prop.setProperty("mail.transport.protocol", "smtp");
		//服务器
		prop.setProperty("mail.smtp.host", "smtp.exmail.qq.com");
		//端口
		prop.setProperty("mail.smtp.port", "465");
		//使用smtp身份验证
		prop.setProperty("mail.smtp.auth", "true");
		//使用SSL，企业邮箱必需！
		//开启安全协议
		MailSSLSocketFactory sf = null;
		try {
			sf = new MailSSLSocketFactory();
			sf.setTrustAllHosts(true);
			prop.put("mail.smtp.ssl.enable", "true");
			prop.put("mail.smtp.ssl.socketFactory", sf);
		} catch (GeneralSecurityException e1) {
			e1.printStackTrace();
		}

		Session session = Session.getDefaultInstance(prop, new MailAuthenticator("bi_service@example.com", "changeme"));
		session.setDebug(true);
		MimeMessage mimeMessage = new MimeMessage(session);
		try {
			//发件人
			mimeMessage.setFrom(new InternetAddress("bi_service@example.com", "数据服务"));        //可以设置发件人的别名
			//mimeMessage.setFrom(new InternetAddress(account));    //如果不需要就省略
			//收件人
			mimeMessage.addRecipient(Message.RecipientType.TO, new InternetAddress("contributor@example.com"));
			//主题
			mimeMessage.setSubject(subject);
			//时间
			mimeMessage.setSentDate(new Date());
			//容器类，可以包含多个MimeBodyPart对象
			Multipart mp = new MimeMultipart();

			//MimeBodyPart可以包装文本，图片，附件
			MimeBodyPart body = new MimeBodyPart();
			//HTML正文
			body.setContent(htmlContent, "text/html; charset=UTF-8");
			mp.addBodyPart(body);

			//设置邮件内容
			mimeMessage.setContent(mp);
			//仅仅发送文本
			//mimeMessage.setText(content);
			mimeMessage.saveChanges();
			Transport.send(mimeMessage);
			System.out.println("邮件发送成功..");
		} catch (MessagingException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		System.out.println("sssss");

		List<User> users = new ArrayList<User>();
		for (int i = 0; i < 4; i++) {
			User u = new User("name_" + i);
			u.setRealName("张" + i);
			users.add(u);
		}
		String json = JSON.toJSONString(users.get(0));
		System.out.println(json);
		User u = JSON.parseObject(json, User.class);
		System.out.println(u);

		json = JSON.toJSONString(users);
		List<User> users2 = (List<User>) JSON.parseArray(json, User.class);
		System.out.println(BIUtil.listToStr(users2));

	}

	static class MailAuthenticator extends Authenticator {
		String u = null;
		String p = null;

		public MailAuthenticator(String u, String p) {
			this.u = u;
			this.p = p;
		}

		@Override
		protected PasswordAuthentication getPasswordAuthentication() {
			return new PasswordAuthentication(u, p);
		}

	}

	/**
	 * 校验时间
	 *
	 * @param sDate
	 * @return
	 */
	public static boolean isLegalDate(String sDate, String dataType) {

		if ((sDate == null)) {
			return false;
		}

		DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");

		if (DataType.Datetime.toString().equalsIgnoreCase(dataType)) {
			formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		}

		try {
			Date date = formatter.parse(sDate);
			return sDate.equals(formatter.format(date));
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * 校验时间
	 *
	 * @param sDate
	 * @param dataFormat
	 * @return
	 */
	public static boolean validDate(String sDate, String dataFormat) {

		if ((sDate == null)) {
			return false;
		}

		DateFormat formatter = new SimpleDateFormat(dataFormat);
		try {
			formatter.parse(sDate);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * 获取异常堆栈
	 *
	 * @param e
	 * @return
	 */
	public static String getStackTrace(Throwable e) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		try {
			e.printStackTrace(pw);
			return sw.toString();
		} finally {
			pw.close();
		}

	}

	/**
	 * 获取utoken
	 *
	 * @return
	 */
	public static String getUToken(HttpServletRequest request) {

		String Cookies_Token = "_token";

		// 1 先从request的参数中获取
		String token = request.getParameter(BIConsts.User_Token);

		//2 再从request的header中获取
		if (BIUtil.isEmpty(token)) {
			token = request.getHeader(BIConsts.User_Token);
		}

		//3 最后从cookies中获取
		if (BIUtil.isEmpty(token)) {
			token = BIUtil.getCookie(BIConsts.Cookies_Prefix + Cookies_Token, request);
		}

		return token;

	}

	/**
	 * 判断2个字符串数组，是否相同
	 * @param list1
	 * @param list2
	 * @return
	 */
	public static boolean isListEqual(List<String> list1 ,List<String> list2) {

		if (list1.size() != list2.size()) {
			return false;
		}

		for (int i = 0; i < list1.size(); i++) {

			if (!list1.get(i).equalsIgnoreCase(list2.get(i))) {
				return false;
			}
		}

		return true;
	}

	/**
	 * 判断2个字符串数组，是否相同,不看顺序
	 * @param list1
	 * @param list2
	 * @return
	 */
	public static boolean isListEqualWithoutSort(List<String> list1 ,List<String> list2) {

		if(list1 == null){
			list1 = new ArrayList<>();
		}

		if(list2 == null){
			list2 = new ArrayList<>();
		}

		if (list1.size() != list2.size()) {
			return false;
		}

		Map<String, String> map = new HashMap<>();
		for (String value : list1) {
			map.put(value, "");
		}

		for (String value : list2) {
			if (!map.containsKey(value)) {
				return false;
			}
		}

		return true;
	}

	/**
	 * 判断2个字符串数组，是否相同,不看顺序
	 * @param str1
	 * @param str2
	 * @return
	 */
	public static boolean isListEqualWithoutSort(String str1 ,String str2) {

		List<String> list1 = new ArrayList<>();
		if (StrUtil.isNotEmpty(str1)) {
			list1 = Arrays.asList(str1.split(","));
			//去重
			list1 = list1.stream().distinct().collect(Collectors.toList());
		}

		List<String> list2 = new ArrayList<>();
		if (StrUtil.isNotEmpty(str2)) {
			list2 = Arrays.asList(str2.split(","));
			//去重
			list2 = list2.stream().distinct().collect(Collectors.toList());
		}

		if (list1.size() != list2.size()) {
			return false;
		}

		Map<String, String> map = new HashMap<>();
		for (String value : list1) {
			map.put(value, "");
		}

		for (String value : list2) {
			if (!map.containsKey(value)) {
				return false;
			}
		}

		return true;
	}


	/**
	 * 通过分号分割脚本内容
	 * @param line
	 * @return 多个脚本
	 */
	public static List<String> splitScriptBySemicolon(String line) {

		List<String> cmdList = new ArrayList<String>();
		StringBuilder command = new StringBuilder();

		// Marker to track if there is a special section open
		SectionType sectionType = null;

		// Index of the last seen delimiter in the given line
		int lastDelimiterIndex = 0;

		// Marker to track if the previous character was an escape character
		boolean wasPrevEscape = false;

		int index = 0;

		// Iterate through the line and invoke the addCmdPart method whenever the delimiter is seen that is not inside a
		// quoted string
		for (; index < line.length(); ) {
			if (!wasPrevEscape && sectionType == null && line.startsWith("'", index)) {
				// Opening non-escaped single quote
				sectionType = SectionType.SINGLE_QUOTED;
				index++;
			} else if (!wasPrevEscape && sectionType == SectionType.SINGLE_QUOTED && line.startsWith("'", index)) {
				// Closing non-escaped single quote
				sectionType = null;
				index++;
			} else if (!wasPrevEscape && sectionType == null && line.startsWith("\"", index)) {
				// Opening non-escaped double quote
				sectionType = SectionType.DOUBLE_QUOTED;
				index++;
			} else if (!wasPrevEscape && sectionType == SectionType.DOUBLE_QUOTED && line.startsWith("\"", index)) {
				// Closing non-escaped double quote
				sectionType = null;
				index++;
			} else if (sectionType == null && line.startsWith("--", index)) {
				// Opening line comment with (non-escapable?) double-dash
				sectionType = SectionType.LINE_COMMENT;
				wasPrevEscape = false;
				index += 2;
			} else if (sectionType == SectionType.LINE_COMMENT && line.startsWith("\n", index)) {
				// Closing line comment with (non-escapable?) newline
				sectionType = null;
				wasPrevEscape = false;
				index++;
			} else if (sectionType == null && line.startsWith("/*", index)) {
				// Opening block comment with (non-escapable?) /*
				sectionType = SectionType.BLOCK_COMMENT;
				wasPrevEscape = false;
				index += 2;
			} else if (sectionType == SectionType.BLOCK_COMMENT && line.startsWith("*/", index)) {
				// Closing line comment with (non-escapable?) newline
				sectionType = null;
				wasPrevEscape = false;
				index += 2;
			} else if (line.startsWith("\\", index)) {
				// Escape character seen (anywhere)
				wasPrevEscape = !wasPrevEscape;
				index++;
			} else if (sectionType == null && line.startsWith(";", index)) {
				// If the delimiter is seen, and the line isn't inside a section, then treat
				// line[lastDelimiterIndex] to line[index] as a single command
				addCmdPart(cmdList, command, line.substring(lastDelimiterIndex, index));
				index += ";".length();
				lastDelimiterIndex = index;
				wasPrevEscape = false;
			} else {
				wasPrevEscape = false;
				index++;
			}
		}
		// If the line doesn't end with the delimiter or if the line is empty, add the cmd part
		if (lastDelimiterIndex != index || line.length() == 0) {
			addCmdPart(cmdList, command, line.substring(lastDelimiterIndex, index));
		}
		return cmdList;
	}

	private enum SectionType {
		SINGLE_QUOTED, DOUBLE_QUOTED, LINE_COMMENT, BLOCK_COMMENT
	}

	private static void addCmdPart(List<String> cmdList, StringBuilder command, String cmdpart) {
		if (cmdpart.endsWith("\\")) {
			command.append(cmdpart.substring(0, cmdpart.length() - 1)).append(";");
			return;
		} else {
			command.append(cmdpart);
		}
		cmdList.add(command.toString());
		command.setLength(0);
	}

	public static void sleep(long millis){
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * 是否是count(distinct)聚合表达式
	 * @return
	 */
	public static boolean isCountDistinctAggExpression(String aggExpression){
		if(BIUtil.isEmpty(aggExpression)){
			return false;
		}
		AggExpressionType aggExpressionType = AggExpressionType.get(aggExpression);;
		if(aggExpressionType == AggExpressionType.Count_Distinct){
			return true;
		}
		String expr = aggExpression.replaceAll("\\s+", "");
		if(expr.toLowerCase().contains(BIConsts.COUNT_DISTINCT_FLAG)){
			return true;
		}
		return false;
	}

	public static boolean isApiUser() {
		User user = UserManager.get();
		if (user == null) {
			return false;
		}
		return isApiUser(user.getName());
	}

	public static boolean isApiUser(String userName){
		String users = SC.v("ssm.api.invoke.user.list", "ssm_simulator_01");
		List<String> userNameList = Arrays.asList(users.split(","));
		return userNameList.contains(userName);
	}

	public static boolean isBossUser() {
		String users = SC.v("ssm.boss.user.list", "chenmin");
		List<String> userNameList = Arrays.asList(users.split(","));
		User user = UserManager.get();
		if (user == null) {
			return false;
		}
		return userNameList.contains(user.getName());
	}


	/**
	 * 获取终端类型
	 * @param userAgent
	 * @return
	 */
	public static String getUserTerminalType(String userAgent) {

		if (StrUtil.isEmpty(userAgent)) {
			return TerminalType.PC.getCode();
		}

		// 定义移动端设备可能包含的关键词
		// 这些关键词通常在移动版浏览器的 userAgent 中出现
		String mobileKeywords = SC.v("ssm.user.agent.mobile.keywords","Mobile,Android,iPhone,iPad,iPod,BlackBerry,IEMobile,Opera Mini,Opera Mobi");
		String[] mobileKeywordArray = mobileKeywords.split(",");

		// 将 userAgent 转换为小写，方便进行不区分大小写的匹配
		userAgent = userAgent.toLowerCase();

		for (String keyword : mobileKeywordArray) {
			if (userAgent.contains(keyword.toLowerCase())) {
				return TerminalType.MOBILE.getCode();
			}
		}

		return TerminalType.PC.getCode();
	}

	/**
	 * 脱敏guid32
	 * @param guid
	 * @return
	 */
	public static String desensitizeGuid32(String guid) {
		if (guid == null || guid.length() != 32) {
			return "******";
		}
		// 前4位 + 20个* + 后8位
		return guid.substring(0, 4) + "********************" + guid.substring(24);
	}

	/**
	 * 兼容邮箱格式的用户名解析：若包含 @ 则取 @ 前的域账号部分，否则原样返回
	 * 例如：contributor@example.com -> contributor
	 */
	public static String resolveUserName(String userName) {
		if (userName == null || userName.isEmpty()) {
			return userName;
		}
		int atIndex = userName.indexOf('@');
		if (atIndex > 0) {
			return userName.substring(0, atIndex);
		}
		return userName;
	}

}


