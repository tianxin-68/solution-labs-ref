package com.bi.queryer.ssm.meta;

import java.util.ArrayList;
import java.util.List;

public class MetaBaseCategory {
	// 类别id
	private Integer id;
	
	// 类别名称
	private String name;
	
	// 类别显示顺序
	private Double showOrder;
	
	// 父id
	private Integer parentId;
	
	private List<MetaBaseCategory> children = new ArrayList<MetaBaseCategory>();
	
	private MetaBaseCategory parent = null;
	
	private List<MetaField> fields = new ArrayList<MetaField>();

	public List<MetaField> getFields() {
		return fields;
	}

	public void setFields(List<MetaField> fields) {
		this.fields = fields;
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

	public Double getShowOrder() {
		return showOrder;
	}

	public void setShowOrder(Double showOrder) {
		this.showOrder = showOrder;
	}

	public Integer getParentId() {
		return parentId;
	}

	public void setParentId(Integer parentId) {
		this.parentId = parentId;
	}

	public MetaBaseCategory getParent() {
		return parent;
	}

	public void setParent(MetaBaseCategory parent) {
		this.parent = parent;
	}

	public List<MetaBaseCategory> getChildren() {
		return children;
	}

	public void setChildren(List<MetaBaseCategory> children) {
		this.children = children;
	}

}
