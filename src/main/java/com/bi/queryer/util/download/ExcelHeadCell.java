package com.bi.queryer.util.download;

public class ExcelHeadCell implements Comparable<ExcelHeadCell>{
	protected String field = "";
	protected String title = "";
	protected String originTitle = "";
	protected int rowIndex = -1;
	protected int colIndex = -1;
	protected int rowSpan = 0;
	protected int colSpan = 0;
	protected boolean isGroup = false;
	
	@Override
	public String toString() {
		String str = "field:" + field + "\t" +
					 "title:" + title + "\t" +
					 "rowIndex:" + rowIndex + "\t" +
					"colIndex:" + colIndex + "\t" +
					"rowSpan:" + rowSpan + "\t" +
					"colSpan:" + colSpan + "\t" +
					"isGroup:" + isGroup;
		return str;
	}
	


	@Override
	public int compareTo(ExcelHeadCell o) {
		if(this.rowIndex > o.rowIndex) {
			return -1;
		}else if(this.rowIndex < o.rowIndex){
			return 1;
		}else{
			return o.colIndex - this.colIndex;
		}
	}
	
	
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public String getOriginTitle() {
		return originTitle;
	}
	public void setOriginTitle(String originTitle) {
		this.originTitle = originTitle;
	}
	public int getRowIndex() {
		return rowIndex;
	}
	public void setRowIndex(int rowIndex) {
		this.rowIndex = rowIndex;
	}
	public int getColIndex() {
		return colIndex;
	}
	public void setColIndex(int colIndex) {
		this.colIndex = colIndex;
	}
	public int getRowSpan() {
		return rowSpan;
	}
	public void setRowSpan(int rowSpan) {
		this.rowSpan = rowSpan;
	}
	public int getColSpan() {
		return colSpan;
	}
	public void setColSpan(int colSpan) {
		this.colSpan = colSpan;
	}
}
