package com.bi.queryer.sys.cache;

import com.bi.queryer.sys.base.BaseDao;
import com.bi.queryer.sys.config.SC;
import com.bi.queryer.sys.db.DBUtil;
import com.bi.queryer.util.BIUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 缓存管理
 * @author contributor
 *
 */
public abstract class CacheManager {
	private static final ThreadLocal<Map<String, Object>> currentCache = new ThreadLocal<Map<String, Object>>();
	
	/**
	 * 设置缓存
	 * @param ctype
	 * @param key
	 * @param value
	 */
	public static void set(CacheType type, String key, Object value, String remark) {
		try {
			BaseDao dao = DBUtil.getBaseDao();
			String valueStr = "";
			if(value instanceof String || value instanceof Integer || value instanceof Double) {
				valueStr = value + "";
			}else {
				valueStr = JSON.toJSONString(value);
			}
			CacheItem item = new CacheItem(type.toString(), key, valueStr, remark);
			dao.insert("cache.setCache", item);
		}catch(Exception e) {
			System.err.println("cache失败：" + e.getMessage());
		}
	}
	
	public static void set(CacheType type, String key, Object value) {
		set(type, key, value, "");
	}
	
	public static String get(CacheType type, String key) {
		String value = null;
		try {
			Map<String, String> params = new HashMap<String, String>();
			params.put("type", type.toString());
			params.put("key", key);
			params.put("cacheDuration", SC.v("cache.duration.minute", "5"));
			BaseDao dao = DBUtil.getBaseDao();
			value = (String) dao.queryObject("cache.getCache", params);
		}catch(Exception e) {
			System.err.println("cache失败：" + e.getMessage());
		}
		return value;
	}
	
	public static List<?> getList(CacheType type, String key, Class<?> clazz) {
		long t1 = System.currentTimeMillis();
		Object currentValue = null;
		if(currentCache.get() != null  && currentCache.get().containsKey(type + key)) {
			currentValue = currentCache.get().get(type + key);
		}
		if(currentValue != null ) {
			return (List<?>) currentValue;
		}
		String value = get(type, key);
		if(BIUtil.isEmpty(value)) {
			return null;
		}
		List<?> list = null;
		try {
			list = JSONArray.parseArray(value, clazz);
			
			Map<String, Object> current = new HashMap<String, Object>();
			current.put(type + key, list);
			currentCache.set(current);
		}catch(Exception e) {
			System.err.println("cache失败：" + e.getMessage());
		}
		long t2 = System.currentTimeMillis();
		System.out.println("cache-getList耗时：" + ((t2 - t1) ) + "ms");
		return list;
	}
	
	public static Object getObject(CacheType type, String key, Class<?> clazz) {
		long t1 = System.currentTimeMillis();
		Object currentValue = null;
		if(currentCache.get() != null  && currentCache.get().containsKey(type + key)) {
			currentValue = currentCache.get().get(type + key);
		}
		if(currentValue != null ) {
			return currentValue;
		}
		Object object = null;
		try {
			String value = get(type, key);
			if(BIUtil.isEmpty(value)) {
				return null;
			}
			object = JSONObject.parseObject(value, clazz);
			Map<String, Object> current = new HashMap<String, Object>();
			current.put(type + key, object);
			currentCache.set(current);
		}catch(Exception e) {
			e.printStackTrace();
		}
		long t2 = System.currentTimeMillis();
		System.out.println("cache-getObject耗时：" + ((t2 - t1) ) + "ms");
		return object;
	}
	
	/**
	 * 通过类型和key清理
	 * @param type
	 * @param key
	 */
	public static void remove(CacheType type, String key) {
		BaseDao dao = DBUtil.getBaseDao();
		Map<String, String> params = new HashMap<String, String>();
		params.put("type", type.toString());
		params.put("key", key);
		dao.delete("cache.removeCache", params);
	}
	
	/**
	 * 通过类型清理
	 * @param type
	 */
	public static void remove(CacheType type) {
		remove(type, null);
	}
	
	/**
	 * 删除所有缓存
	 */
	public static void clear() {
		remove(null, null);
	}
}
