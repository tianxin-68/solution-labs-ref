package com.bi.queryer.ssm.enums;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public enum FieldSortType {
	DESC("desc"),
	ASC("asc"),
	NONE("");

	private String code = "";
	private FieldSortType(String code){
		this.code = code;
	}
	
	public static FieldSortType getType(String typeStr){
		for(FieldSortType t : FieldSortType.values()){
			if(t.toString().equalsIgnoreCase(typeStr)){
				return t;
			}
		}
		return NONE;
	}

	// 转换成为 List<Map<String, String>>, 对外提供查询和遍历功能
	public static List<Map<String, String>> toListMap() {
		List<Map<String, String>> listMap = new ArrayList<>();
		for (FieldSortType i : FieldSortType.values()) {
			Map<String, String> map = new HashMap<>();
			map.put("value", i.toString());
			map.put("label", i.toString());
			listMap.add(map);
		}
		return listMap;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}
}
