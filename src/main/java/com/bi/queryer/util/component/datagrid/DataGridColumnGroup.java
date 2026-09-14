package com.bi.queryer.util.component.datagrid;

import java.util.ArrayList;
import java.util.List;

import org.dom4j.Element;

/**
 * 分组用于表头合并
 * <br/>
 * 注：不支持冻结列和非冻结列进行列合并
 */
public class DataGridColumnGroup extends DataGridColumn {
	private static final long serialVersionUID = 1L;

	private DataGridColumnGroup parent = null;
	
	private List<DataGridColumnGroup> children = null;
	
	private int depth = 1;// 深度
	
	private List<DataGridColumn> columns = new ArrayList<DataGridColumn>();
	
	public DataGridColumnGroup(){
		
	}
	
	public DataGridColumnGroup(String id, String title){
		this.id = id;
		this.title = title;
		this.field = id;
	}
	
	/**
	 * 添加列
	 * @param col
	 * @return
	 */
	public DataGridColumnGroup addColumn(DataGridColumn col){
		col.setGroup(this.id);
		if(!columns.contains(col)){
			columns.add(col);
		}
		return this;
	}
	
	public DataGridColumnGroup addChild(DataGridColumnGroup grp){
		if(this.children == null){
			this.children = new ArrayList<DataGridColumnGroup>();
		}
		this.children.add(grp);
		grp.parent = this;
		return this;
	}

	public DataGridColumnGroup getParent() {
		return parent;
	}

	public void setParent(DataGridColumnGroup parent) {
		this.parent = parent;
	}

	@Override
	public void load(Element grpEle) {
		if(grpEle == null) return;
		this.id = grpEle.attributeValue("id");
		this.title = grpEle.attributeValue("title");
		List childEles = grpEle.elements("Group");
		
		// 暂时只支持2级
		int size = childEles.size();
		if(size > 0) {
			children = new ArrayList<DataGridColumnGroup>();
		}
		for(int i = 0; i < size; i++){
			Element childEle = (Element) childEles.get(i);
			DataGridColumnGroup grp = new DataGridColumnGroup();
			grp.load(childEle);
			grp.parent = this;
			children.add(grp);
			grp.setLevel(grp.getParent().getLevel() + 1);
		}
	}
	
	@Override
	public void save(Element grpEle) {
		if(grpEle == null) return;
		grpEle.addAttribute("id", id);
		grpEle.addAttribute("title", title);
		
		// children
		if(children == null) {
			return;
		}
		for(DataGridColumnGroup childGrp : children){
			Element childEle = grpEle.addElement("Group");
			childGrp.save(childEle);
		}
	}

	public int getDepth() {
		return depth;
	}

	public List<DataGridColumnGroup> getChildren() {
		return children;
	}

	public void setChildren(List<DataGridColumnGroup> children) {
		this.children = children;
	}

	public void setDepth(int depth) {
		this.depth = depth;
	}
	
	@Override
	public String toString() {
		return "id:" + "\t" + getId() + "\t" + "title:" + getTitle() + "\t" + "level:" + getLevel() + "\t" + "depth:" + getDepth();
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj == null) return false;
		if(!(obj instanceof DataGridColumnGroup)) return false;
		DataGridColumnGroup grp = (DataGridColumnGroup) obj;
		if(grp.getId() == null) return false;
		return grp.getId().equalsIgnoreCase(id);
	}

	public List<DataGridColumn> getColumns() {
		return columns;
	}

	public void setColumns(List<DataGridColumn> columns) {
		this.columns = columns;
	}
}
