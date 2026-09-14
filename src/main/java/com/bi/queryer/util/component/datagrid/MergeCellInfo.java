package com.bi.queryer.util.component.datagrid;

public class MergeCellInfo {
	public int index = 0;
	
	public int rowspan = 1;
	
	public String field = null;

	public int getIndex() {
		return index;
	}

	public void setIndex(int index) {
		this.index = index;
	}

	public int getRowspan() {
		return rowspan;
	}

	public void setRowspan(int rowspan) {
		this.rowspan = rowspan;
	}

	public String getField() {
		return field;
	}

	public void setField(String field) {
		this.field = field;
	}
}
