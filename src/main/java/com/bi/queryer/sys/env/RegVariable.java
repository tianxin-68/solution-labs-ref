package com.bi.queryer.sys.env;


/**
 * 正则表达式计算的范围
 * @author contributor
 *
 */
public enum RegVariable {
	Normal("\\$","普通正则表达式"),
	Sql("SQL", "SQL片段式正则表达式");
	

	private String label = "";
	private String desc = "";
	
	private RegVariable(String label, String desc){
		this.label = label;
		this.desc = desc;
	}

	public static RegVariable getType(String typeStr){
		for(RegVariable type : values()){
			if(type.toString().equalsIgnoreCase(typeStr)) {
				return type;
			}
		}
		return Normal;
	}
	

	public String getDesc() {
		return desc;
	}

	public void setDesc(String desc) {
		this.desc = desc;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}
	
}
