package com.bi.queryer.util;

import com.alibaba.fastjson.JSONObject;

/**
 * 对象json序列化
 * @author contributor
 *
 */
public interface JSONSerializable {
	public JSONObject toJSON();
}
