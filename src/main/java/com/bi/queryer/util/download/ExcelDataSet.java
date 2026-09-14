package com.bi.queryer.util.download;

import java.util.List;

import com.bi.queryer.util.JSONSerializable;
/**
 * Excel数据集
 * 支持多sheet
 * @author contributor
 *
 */
public class ExcelDataSet {

	private String name;
	
	private List<FileFieldItem> fields = null;
	
	private List<? extends JSONSerializable> dataList = null;
	
	public ExcelDataSet(String name, List<FileFieldItem> fields, List<? extends JSONSerializable> dataList) {
		this.name = name;
		this.fields = fields;
		this.dataList = dataList;
	}

	public List<FileFieldItem> getFields() {
		return fields;
	}

	public void setFields(List<FileFieldItem> fields) {
		this.fields = fields;
	}

	public List<? extends JSONSerializable> getDataList() {
		return dataList;
	}

	public void setDataList(List<? extends JSONSerializable> dataList) {
		this.dataList = dataList;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
