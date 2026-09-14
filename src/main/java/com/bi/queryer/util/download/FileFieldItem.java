package com.bi.queryer.util.download;

import java.util.ArrayList;
import java.util.List;

import com.bi.queryer.sys.db.DataType;
import com.bi.queryer.util.component.datagrid.alarm.AlarmType;


public class FileFieldItem implements Comparable<FileFieldItem>{
	private String name = "";
	private String title = "";
	private DataType dataType = DataType.String;
	private int order = 0;// 字段顺序，用于导出的字段顺序
	private boolean exclude = false; // 排除字段，不导出此字段
	private int width = 100;// 列宽度
	protected List<AlarmType> alarmTypes = new ArrayList<AlarmType>();
	private boolean mergeCell = false;// 是否合并单元格：根据内容进行合并
	private String align = "center";// 对齐方式，默认居中
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public DataType getDataType() {
		if(dataType == null) {
			dataType = DataType.String;
		}
		return dataType;
	}
	public void setDataType(DataType dataType) {
		this.dataType = dataType;
	}
	public int getOrder() {
		return order;
	}
	public void setOrder(int order) {
		this.order = order;
	}
	@Override
	public int compareTo(FileFieldItem obj) {
		if(obj == null) return -1;
		if(order > obj.getOrder()){
			return 1;
		}else if(order < obj.getOrder()){
			return -1;
		}else {
			return 0;
		}
	}
	public boolean isExclude() {
		return exclude;
	}
	public void setExclude(boolean exclude) {
		this.exclude = exclude;
	}
	public int getWidth() {
		return width;
	}
	public void setWidth(int width) {
		this.width = width;
	}
	public List<AlarmType> getAlarmTypes() {
		return alarmTypes;
	}
	public void setAlarmTypes(List<AlarmType> alarmTypes) {
		this.alarmTypes = alarmTypes;
	}
	public boolean isMergeCell() {
		return mergeCell;
	}
	public void setMergeCell(boolean mergeCell) {
		this.mergeCell = mergeCell;
	}
	public String getAlign() {
		return align;
	}
	public void setAlign(String align) {
		this.align = align;
	}
	
	
}
