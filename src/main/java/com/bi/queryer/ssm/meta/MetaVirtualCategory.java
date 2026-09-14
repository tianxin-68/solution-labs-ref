package com.bi.queryer.ssm.meta;

import java.util.ArrayList;
import java.util.List;

/**
 * 虚拟目录
 * @author contributor
 *
 */
public class MetaVirtualCategory{

	// 类别id
	private String id;
	
	// 类别名称
	private String name;
	
	// 类别显示顺序
	private Double showOrder;
	
	// 父id
	private String parentId;
	
	private List<MetaVirtualCategory> children = new ArrayList<MetaVirtualCategory>();
	
	private MetaVirtualCategory parent = null;
	
	private List<MetaField> fields = new ArrayList<MetaField>();

	public MetaVirtualCategory(){

	}

	public MetaVirtualCategory(String id, String name, Double showOrder, String parentId) {
		this.id = id;
		this.name = name;
		this.showOrder = showOrder;
		this.parentId = parentId;
	}

	public List<MetaField> getFields() {
		return fields;
	}

	public void setFields(List<MetaField> fields) {
		this.fields = fields;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
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

	public String getParentId() {
		return parentId;
	}

	public void setParentId(String parentId) {
		this.parentId = parentId;
	}

	public List<MetaVirtualCategory> getChildren() {
		return children;
	}

	public void setChildren(List<MetaVirtualCategory> children) {
		this.children = children;
	}

	public MetaVirtualCategory getParent() {
		return parent;
	}

	public void setParent(MetaVirtualCategory parent) {
		this.parent = parent;
	}

}
