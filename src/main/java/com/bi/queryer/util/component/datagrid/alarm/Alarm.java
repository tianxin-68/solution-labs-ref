package com.bi.queryer.util.component.datagrid.alarm;

import com.bi.queryer.util.component.datagrid.DataGridColumn;

public abstract class Alarm  implements Comparable<Alarm>{
	
	public abstract AlarmType getType();
	
	/**
	 * 预警表达式：[field1] + [field2] > value 
	 * <br/>
	 * 只支持 + - * / 操作符
	 * <br/>
	 * 字段名必须带中括号[],字段名最好大写
	 * @return
	 */
	public abstract String expression();
	
	protected DataGridColumn column = null;
	
	@Override
	public boolean equals(Object obj) {
		if(obj == null) return false;
		Alarm a = (Alarm) obj;
		if(this.getType() == null || a.getType() == null) return false;
		return this.getType().toString().equalsIgnoreCase(a.getType().toString());
	}
	
	@Override
	public int hashCode() {
		if(this.getType() != null) {
			return this.getType().hashCode();
		}else{
			return super.hashCode();
		}
	}
	
	@Override
	public int compareTo(Alarm o) {
		if(o == null) return -1;
		int id1 = this.getType().getId();
		int id2 = o.getType().getId();
		return (id2 - id1);
	}

	public DataGridColumn getColumn() {
		return column;
	}

	public void setColumn(DataGridColumn column) {
		this.column = column;
	}
}
