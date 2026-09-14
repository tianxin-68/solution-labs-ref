package com.bi.queryer.ssm.engine.config.field;

import com.bi.queryer.util.JSONSerializable;
import com.bi.queryer.util.StringUtil;
import com.alibaba.fastjson.JSONObject;
import org.dom4j.Element;

public class FieldValue implements JSONSerializable, Cloneable, Comparable{
	private String id ;
	
	private String title;
	
	private String realId;
	
	private String levelFieldName;

	private String levelFieldId;

	private String filterShowType;

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public FieldValue(){

	}

	public FieldValue(String id, String title) {
		this.id = id;
		this.title = title;
	}

	public FieldValue(String id, String title, String filterShowType) {
		this.id = id;
		this.title = title;
		this.filterShowType = filterShowType;
	}

	public FieldValue clone(){
		FieldValue copy = new FieldValue();
		copy.id = id;
		copy.title = title;
		copy.realId = realId;
		copy.levelFieldName = levelFieldName;
		copy.levelFieldId = levelFieldId;
		copy.filterShowType = filterShowType;
		return copy;
	}
	
	@Override
	public boolean equals(Object obj) {
		if(id != null){
			return id.equals(((FieldValue)obj).getId());
		}else{
			return false;
		}
	}

	@Override
	public int hashCode() {
		return id == null ? super.hashCode() : id.hashCode();
	}

	@Override
	public String toString() {
		return id + "\t" + title;
	}
	
	@Override
	public JSONObject toJSON() {
		JSONObject json = new JSONObject();
		json.put("id", id);
		json.put("value", title);
		json.put("filterShowType", filterShowType);
		if(!StringUtil.isEmpty(realId)){
			json.put("realId", realId);
			json.put("levelFieldName", levelFieldName);
			json.put("levelFieldId", levelFieldId);
		}
		return json;
	}

	public void load(Element e) {
		if(e == null){
			return;
		}
		this.id = e.attributeValue("id");
		this.realId = e.attributeValue("realId"); // 存储级联字段id
		this.levelFieldName = e.attributeValue("levelFieldName"); // 存储级联名称id
		String str = e.attributeValue("levelFieldId"); // 存储级联名称id
		if(!StringUtil.isEmpty(str)){
			try{
				this.levelFieldId = str;
			}catch(Exception ex){
				ex.printStackTrace();
			}
		}
		this.title = e.getTextTrim();
	}

	public void save(Element e) {
		e.addAttribute("id", id);
		e.setText(title);
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getRealId() {
		return realId;
	}

	public void setRealId(String realId) {
		this.realId = realId;
	}

	public String getLevelFieldName() {
		return levelFieldName;
	}

	public void setLevelFieldName(String levelFieldName) {
		this.levelFieldName = levelFieldName;
	}

	public String getLevelFieldId() {
		return levelFieldId;
	}

	public void setLevelFieldId(String levelFieldId) {
		this.levelFieldId = levelFieldId;
	}

	@Override
	public int compareTo(Object o) {
		if(o == null) {
			return -1;
		}
		FieldValue v = (FieldValue) o;
		if(id == null || v.getId() == null) {
			return -1;
		}
		return id.compareToIgnoreCase(((FieldValue) o).getId());
	}

	public String getFilterShowType() {
		return filterShowType;
	}

	public void setFilterShowType(String filterShowType) {
		this.filterShowType = filterShowType;
	}
}
