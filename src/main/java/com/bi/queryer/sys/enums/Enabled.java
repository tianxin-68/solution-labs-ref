package com.bi.queryer.sys.enums;

import com.bi.queryer.util.BIUtil;

/**
 * 是否启用枚举类
 * @author contributor
 *
 */
public enum Enabled {

	YES(1, "是","true"),
	NO(0, "否","false"),
	OTHER(-999999, "其他","other");
	
	private Enabled (Integer id, String desc,String code) {
		this.id = id;
		this.desc = desc;
		this.code = code;
	}

	public static boolean isTrue(Integer id){
		return value(id);
	}

	public static boolean isTrue(String id){
		return value(id);
	}

	public static boolean isFalse(Integer id){
		return !value(id);
	}

	public static boolean isFalse(String id){
		return !value(id);
	}
	
	public static boolean value(Integer id) {
		return Enabled.YES == getType(id);
	}
	
	public static boolean value(String id) {
		if(BIUtil.isEmpty(id)) {
			return false;
		}
		Integer intVal =  0;
		try {
			intVal = Integer.valueOf(id);
		}catch(Exception e) {
			e.printStackTrace();
		}
		return Enabled.YES == getType(intVal);
	}
	
	public static Enabled getType(Integer id){
		for(Enabled type : values()){
			if(type.getId().equals(id)){
				return type;
			}
		}
		return NO;
	}
	
	private Integer id;
	
	private String desc;

	private String code;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getDesc() {
		return desc;
	}

	public void setDesc(String desc) {
		this.desc = desc;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}
}
