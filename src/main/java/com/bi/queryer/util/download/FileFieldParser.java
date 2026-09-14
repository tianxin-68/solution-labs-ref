package com.bi.queryer.util.download;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.bi.queryer.util.StringUtil;

public class FileFieldParser {
	private Class<?> clazz = null;
	
	private String[] fieldNames = null;
	
	private String[] fieldTitles = null;
	
	private String [] excludeFields = null; // 排除的字段
	
//	private Map<String, DataType> fieldDataTypes = new HashMap<String, DataType>();
	private Map<String, FileFieldItem> fieldItemMap = new HashMap<String, FileFieldItem>();
	
	public FileFieldParser(Class<?> clazz){
		this.clazz = clazz;
	}
	
	/**
	 * 获取所有字段信息
	 * @return
	 */
	public List<FileFieldItem> getFileFieldItems(){
		List<FileFieldItem> items = new ArrayList<FileFieldItem>();
		getFieldItems(items, clazz);
		return items;
	}
	
	/**
	 * 递归获取字段名和标题
	 * 
	 * @param items
	 * @param clazz
	 */
	public void getFieldItems(List<FileFieldItem> items, Class<?> clazz) {
		if (clazz == null)
			return;
		Field[] classFields = clazz.getDeclaredFields();
		for (int i = 0; i < classFields.length; i++) {
			FileFieldItem item = new FileFieldItem();
			FileField fileField = classFields[i].getAnnotation(FileField.class);
			if (fileField == null) {
				continue;
			}
			item.setName(classFields[i].getName());
			item.setOrder(fileField.order());
			item.setDataType(fileField.datatype());
			if (fileField != null) {
				if (StringUtil.isEmpty(fileField.title())) {
					item.setTitle(classFields[i].getName());
				} else {
					item.setTitle(fileField.title());
				}
			}
			if(excludeFields != null){
				for(String ef : excludeFields){
					if(classFields[i].getName().equalsIgnoreCase(ef)){
						item.setExclude(true);
						break;
					}
				}
			}
			items.add(item);
		}
		getFieldItems(items, clazz.getSuperclass());
	}
	
	/**
	 * 获取导出类的导出字段和导出标题
	 * 
	 * @param clazz
	 * @return List<String[]> 0:names,1:titles
	 */
	public void parse(List<FileFieldItem> items) {
		if(items == null || items.isEmpty()) return;
		Collections.sort(items);
		
		List<String> fieldNameList = new ArrayList<String>();
		List<String> fieldTitleList = new ArrayList<String>();
		for (int i = 0; i < items.size(); i++) {
			if(items.get(i).isExclude()){
				continue;
			}
			FileFieldItem item = items.get(i);
			fieldNameList.add(item.getName());
			fieldTitleList.add(item.getTitle());
			fieldItemMap.put(item.getName(), item);
//			DataType dataType = item.getDataType();
//			if(dataType == null) dataType = DataType.String;
//			fieldDataTypes.put(items.get(i).getName(), dataType);
		}
		
		fieldNames = new String[fieldNameList.size()];
		fieldNameList.toArray(fieldNames);
		fieldTitles = new String[fieldTitleList.size()];
		fieldTitleList.toArray(fieldTitles);
	}

	/**
	 * 获取导出类的导出字段和导出标题
	 * 
	 * @param clazz
	 * @return List<String[]> 0:names,1:titles
	 */
	public void parse() {
		parse(getFileFieldItems());
	}

	public String[] getFieldNames() {
		return fieldNames;
	}

	public void setFieldNames(String[] fieldNames) {
		this.fieldNames = fieldNames;
	}

	public String[] getFieldTitles() {
		return fieldTitles;
	}

	public void setFieldTitles(String[] fieldTitles) {
		this.fieldTitles = fieldTitles;
	}

	public Class<?> getClazz() {
		return clazz;
	}

	public void setClazz(Class<?> clazz) {
		this.clazz = clazz;
	}

	public String[] getExcludeFields() {
		return excludeFields;
	}

	public void setExcludeFields(String[] excludeFields) {
		this.excludeFields = excludeFields;
	}

	public Map<String, FileFieldItem> getFieldItemMap() {
		return fieldItemMap;
	}

	public void setFieldItemMap(Map<String, FileFieldItem> fieldItemMap) {
		this.fieldItemMap = fieldItemMap;
	}

	/*public Map<String, DataType> getFieldDataTypes() {
		return fieldDataTypes;
	}

	public void setFieldDataTypes(Map<String, DataType> fieldDataTypes) {
		this.fieldDataTypes = fieldDataTypes;
	}*/

}
