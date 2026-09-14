package com.bi.queryer.util.component.datasource;

import org.dom4j.Element;

import com.alibaba.fastjson.JSONObject;
import com.bi.queryer.sys.db.DataType;
import com.bi.queryer.util.JSONSerializable;
import com.bi.queryer.util.XMLSerializable;

/**
 * 数据源参数
 * @author contributor
 *
 */
public class DatasourceParameter implements JSONSerializable, XMLSerializable{
	private String id;
	
	private String value;
	
	private DataType dataType = DataType.String;
	
	@Override
	public boolean equals(Object obj) {
		if(obj == null) return false;
		DatasourceParameter p = (DatasourceParameter) obj;
		if(p.getId() == null) return false;
		return p.getId().equals(id);
	}
	
	public void load(Element e){
		if(e == null) {
			return;
		}
		this.id = e.attributeValue("id");
		this.dataType = DataType.getType(e.attributeValue("dataType"));
		this.value = e.attributeValue("value");
	}
	
	public void save(Element e){
		if(e == null) return;
		e.addAttribute("id", this.id);
		e.addAttribute("dataType", this.dataType.toString());
		e.addAttribute("value", this.value);
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public DataType getDataType() {
		return dataType;
	}

	public void setDataType(DataType dataType) {
		this.dataType = dataType;
	}

	@Override
	public JSONObject toJSON() {
		JSONObject parameter = JSONObject.parseObject(JSONObject.toJSONString(this));
		return parameter;
	}
}
