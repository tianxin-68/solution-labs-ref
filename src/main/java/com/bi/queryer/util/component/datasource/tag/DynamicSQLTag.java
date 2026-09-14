package com.bi.queryer.util.component.datasource.tag;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.dom4j.Element;

/**
 * SQL标签基类
 * @author contributor
 *
 */
public abstract class DynamicSQLTag {
	protected String property = null;
	protected String compareValue = null;
	protected Element element = null;
	protected Element parent = null;
	protected Map<String, ?> parameters = null;
	
	public DynamicSQLTag(Element element){
		this.element = element;
	}
	
	/**
	 * 初始化
	 */
	protected void initialize(){
		if(element == null){
			return;
		}
		parent = element.getParent();
		property = element.attributeValue("property");
		compareValue = element.attributeValue("compareValue");
	}
	
	/**
	 * 是否满足标签条件
	 * @return
	 */
	public boolean conform() throws DynamicSQLTagException{
		initialize();
		String validateResult = validate();
		if(StringUtils.isNotEmpty(validateResult)){
			throw new DynamicSQLTagException(validateResult);
		}
		return accept();
	}
	
	/**
	 * 标签是否接受对应查询条件，子类实现
	 * @return boolean
	 */
	public abstract boolean accept(); 
	
	/**
	 * 验证标签的合法性
	 * @return null或者""为合法，否则不合法
	 */
	public String validate(){
		if(StringUtils.isEmpty(property)){
			return "<" + getType().toString() + ">" + "标签必须包含property属性";
		}
		return "";
	}
	
	/**
	 * 获取标签类型
	 * @return 标签类型
	 */
	public abstract DynamicSQLTagType getType();

	public String getProperty() {
		return property;
	}

	public void setProperty(String property) {
		this.property = property;
	}

	public String getCompareValue() {
		return compareValue;
	}

	public void setCompareValue(String compareValue) {
		this.compareValue = compareValue;
	}

	public Element getElement() {
		return element;
	}

	public void setElement(Element element) {
		this.element = element;
	}

	public Element getParent() {
		return parent;
	}

	public void setParent(Element parent) {
		this.parent = parent;
	}

	public Map<String, ?> getParameters() {
		return parameters;
	}

	public void setParameters(Map<String, ?> parameters) {
		this.parameters = parameters;
	}
}
