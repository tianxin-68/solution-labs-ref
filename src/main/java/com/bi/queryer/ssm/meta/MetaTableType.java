package com.bi.queryer.ssm.meta;

/**
 * 元数据表类型
 * @author contributor
 *
 */
public enum MetaTableType {
	None(-1, "未知"),
	Fact(1, "事实表"),
	Dimension(0, "维度表");
	
	private Integer id;
	
	private String name;

	private MetaTableType(Integer id, String name) {
		this.id = id;
		this.name = name;
	}
	
	public static MetaTableType getType(Integer id){
		for(MetaTableType t : MetaTableType.values()){
			if(t.id.equals(id)){
				return t;
			}
		}
		return None;
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
