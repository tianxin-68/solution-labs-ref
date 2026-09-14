package com.bi.queryer.util.component.datasource;

import java.util.ArrayList;
import java.util.List;

import org.dom4j.Element;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.bi.queryer.util.JSONSerializable;
import com.bi.queryer.util.XMLSerializable;

public class Datasource implements JSONSerializable, XMLSerializable{
	private String type;
	private String sql;
	private String json;
	private String content;
	private List<DatasourceParameter> parameters = new ArrayList<DatasourceParameter>();
	
	/**数据源引用相关*/
	private Boolean hasRef = false;
	private String refType = "";
	private String refId = "";
	private String refName = "";
	
	public void addParameter(DatasourceParameter parameter){
		if(!this.parameters.contains(parameter)) {
			this.parameters.add(parameter);
		}
	}
	public List<DatasourceParameter> getParameters(){
		return parameters;
	}
	public void setParameters(List<DatasourceParameter> parameters) {
		this.parameters = parameters;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public String getSql() {
		return sql;
	}
	public void setSql(String sql) {
		this.sql = sql;
	}
	public String getJson() {
		return json;
	}
	public void setJson(String json) {
		this.json = json;
	}

	public String getContent() {
		if("json".equalsIgnoreCase(type)) {
			content = json;
		}else {
			content = sql;
		}
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}
	
	public void load(Element e){
		if(e == null) {
			return;
		}
		this.type = e.attributeValue("type");
		Element sqlEle = e.element("SQL");
		if(sqlEle != null) {
			 this.sql = sqlEle.getText();
		}

        Element refEl = e.element("Ref");
        if (refEl != null) {
            this.refType = refEl.attributeValue("type");
            this.refId = refEl.attributeValue("id");
            this.refName = refEl.attributeValue("name");
        }

        Element jsonEle = e.element("JSON");
		if(jsonEle != null) {
			 this.json = jsonEle.getText();
		}
		Element parametersEle = e.element("Parameters");
		if(parametersEle != null) {
			List paramEleList = parametersEle.elements("Parameter");
			if(paramEleList != null) {
				this.parameters.clear();
				for(int i = 0; i < paramEleList.size(); i++){
					DatasourceParameter p = new DatasourceParameter();
					p.load((Element)paramEleList.get(i));
					this.addParameter(p);
				}
			}
		}
	}

	@Override
	public void save(Element e) {
		if(e == null) return ;
		e.addAttribute("type", type);
		Element contentEle = null;
		if("json".equalsIgnoreCase(type)) {
			contentEle = e.addElement("JSON");
		}else {
			contentEle = e.addElement("SQL");
		}
		contentEle.addCDATA(this.content);
		
		Element paramtersEle = e.addElement("Parameters");
		for(DatasourceParameter p : this.parameters){
			Element pe = paramtersEle.addElement("Parameter");
			p.save(pe);
		}
	}

    public void saveChartDs(Element e) {
        if (e == null) return;

        e.addAttribute("type", this.type);
        e.addAttribute("hasRef", this.hasRef + "");
        if (hasRef) {
            Element refEle = e.addElement("Ref");
            refEle.addAttribute("type",this.refType);
            refEle.addAttribute("id",this.refId);
            refEle.addAttribute("name",this.refName);
        } else {
            Element sqlEle = e.addElement("SQL");
            sqlEle.addCDATA(this.sql);
        }
    }

	@Override
	public JSONObject toJSON() {
		JSONObject json = new JSONObject();
		json.put("type", type);
		json.put("content", this.getContent());
		JSONArray parameterArray = new JSONArray();
		for(DatasourceParameter p : this.parameters){
			JSONObject paramJSON = p.toJSON();
			parameterArray.add(paramJSON);
		}
		json.put("parameters", parameterArray);
		return json;
	}
	public Boolean getHasRef() {
		return hasRef;
	}
	public void setHasRef(Boolean hasRef) {
		this.hasRef = hasRef;
	}
	public String getRefType() {
		return refType;
	}
	public void setRefType(String refType) {
		this.refType = refType;
	}
	public String getRefId() {
		return refId;
	}
	public void setRefId(String refId) {
		this.refId = refId;
	}
	public String getRefName() {
		return refName;
	}
	public void setRefName(String refName) {
		this.refName = refName;
	}
	
}
