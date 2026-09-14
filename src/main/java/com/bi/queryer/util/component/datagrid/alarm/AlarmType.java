package com.bi.queryer.util.component.datagrid.alarm;

/**
 * 列预警类型
 * @author contributor
 *
 */
public enum AlarmType {
	Red_Font(1, "红色字体"),
	Red_Flag(2, "红色图标"),
	Red_Arrow(3, "红色箭头"),
	
	Yellow_Font(4, "黄色字体"),
	Yellow_Flag(5, "黄色图标"),
	Yellow_Arrow(6, "黄色箭头"),
	
	Green_Font(7,"绿色字体"),
	Green_Flag(8, "绿色图标"),
	Green_Arrow(9, "绿色箭头");
	
	private String desc;
	
	private Integer id;
	
	AlarmType(Integer id, String desc){
		this.id = id;
		this.desc = desc;
	}
	
	public static AlarmType getType(String typeStr){
		for(AlarmType type : values()){
			if(type.toString().equalsIgnoreCase(typeStr)){
				return type;
			}
		}
		return Green_Flag;
	}

	public String getDesc() {
		return desc;
	}

	public void setDesc(String desc) {
		this.desc = desc;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}
	
}

