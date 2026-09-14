package com.bi.queryer.util.component.datagrid;

/**
 * 表格工具栏，放置在表格上方
 * @author contributor
 *
 */
public class DataGridToolButton {
	private String title = ""; // 标题
	private String id = ""; // dom的ID
	private String code = "";// 编码，用于参数key
	private String value = "";// 参数value
	private int index = 0;
	private boolean active = false;
	private boolean show = true;// 是否显示
	private String align = "left";// 对齐方式
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getCode() {
		return code;
	}
	public void setCode(String code) {
		this.code = code;
	}
	public String getValue() {
		return value;
	}
	public void setValue(String value) {
		this.value = value;
	}
	public int getIndex() {
		return index;
	}
	public void setIndex(int index) {
		this.index = index;
	}
	public boolean isActive() {
		return active;
	}
	public void setActive(boolean active) {
		this.active = active;
	}
	public boolean isShow() {
		return show;
	}
	public void setShow(boolean show) {
		this.show = show;
	}
	public String getAlign() {
		return align;
	}
	public void setAlign(String align) {
		this.align = align;
	}
}
