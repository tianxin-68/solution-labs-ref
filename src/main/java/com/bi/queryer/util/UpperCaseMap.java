package com.bi.queryer.util;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import com.alibaba.fastjson.JSONObject;

public class UpperCaseMap extends HashMap implements JSONSerializable, Serializable, Cloneable {

	private static final long serialVersionUID = 9212885340802716103L;

	public UpperCaseMap(int initialCapacity, float loadFactor) {
		super(initialCapacity, loadFactor);
	}

	public UpperCaseMap(int initialCapacity) {
		super(initialCapacity);
	}
	public UpperCaseMap() {
		
	}
	public UpperCaseMap(Map m) {
		putAll(m);
	}

	public Object put(Object key, Object value) {
		return super.put(convertKey(key), value);
	}

	@Override
	public Object get(Object key) {
		return super.get(convertKey(key));
	}

	@Override
	public boolean containsKey(Object key) {
		return super.containsKey(convertKey(key));
	}

	@Override
	public Object remove(Object key) {
		return super.remove(convertKey(key));
	}

	public Object clone() {
		return super.clone();
	}

	protected Object convertKey(Object key) {
		if (key != null)
			return key.toString().toUpperCase().trim();

		return null;
	}

	@Override
	public JSONObject toJSON() {
		JSONObject json = new JSONObject();
		json.putAll(this);
		return json;
	}
	
	@Override
	public boolean equals(Object o) {
		if(o == null) return false;
		String str1 = this.toString();
		String str2 = o.toString();
		return str1.equals(str2);
	}
	
	@Override
	public int hashCode() {
		return this.toString().hashCode();
	}
}
