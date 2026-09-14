package com.bi.queryer.ssm.engine.config.field;

import com.bi.queryer.util.JSONSerializable;
import com.alibaba.fastjson.JSONObject;
import org.dom4j.Element;

/**
 * 字段操作符
 * @author contributor
 *
 */
public class FieldOperator implements JSONSerializable {
	private String type = "";
	
	private QueryField preField = null;
	
	private QueryField nextField = null;

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	@Override
	public JSONObject toJSON() {
		JSONObject json = new JSONObject();
		json.put("type", type);
		return json;
	}

	public void load(Element e) {
		if(e == null){
			return;
		}
		this.type = e.attributeValue("type");
	}
	
	public void save(Element e) {
		e.attributeValue("type", type);
	}

	public QueryField getPreField() {
		return preField;
	}

	public void setPreField(QueryField preField) {
		this.preField = preField;
	}

	public QueryField getNextField() {
		return nextField;
	}

	public void setNextField(QueryField nextField) {
		this.nextField = nextField;
	}
}
