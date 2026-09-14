package com.bi.queryer.ssm.meta;

/**
 * 元数据表类型
 * @author contributor
 *
 */
public enum MetaFieldType {
	Measure(1, "度量"),
	Dimension(0, "维度表");
	
	private Integer id;
	
	private String name;

	private MetaFieldType(Integer id, String name) {
		this.id = id;
		this.name = name;
	}
	
	public static MetaFieldType getType(Integer id){
		for(MetaFieldType t : MetaFieldType.values()){
			if(t.id.equals(id)){
				return t;
			}
		}
		return Dimension;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
}
