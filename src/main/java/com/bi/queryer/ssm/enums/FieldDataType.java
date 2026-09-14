package com.bi.queryer.ssm.enums;

import com.bi.queryer.sys.db.DataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 字段数据类型
 * @author contributor
 *
 */
public enum FieldDataType {
	String,
	Double,
	Integer,
//	Bigint,
	Date,
	Datetime;
	
	public static FieldDataType getType(String type){
		DataType dataType = DataType.getType(type);
		if(dataType != null && (dataType == DataType.BigInt || dataType == DataType.Integer)){
			return Integer;
		}
		for(FieldDataType t : values()){
			if(t.toString().equalsIgnoreCase(type)){
				return t;
			}
		}
		return String;
	}

	// 转换成为 List<Map<String, String>>, 对外提供查询和遍历功能
	public static List<Map<String, String>> toListMap() {
		List<Map<String, String>> listMap = new ArrayList<>();
		for (FieldDataType i : FieldDataType.values()) {
			Map<String, String> map = new HashMap<>();
			map.put("value", i.toString());
			map.put("label", i.toString());
			listMap.add(map);
		}
		return listMap;
	}
}
