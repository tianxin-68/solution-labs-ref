package com.bi.queryer.util.component.datagrid;

import org.dom4j.Element;

import com.bi.queryer.util.component.XMLSerializable;

public class DataGridForamtter extends XMLSerializable{
	private String expression = "";

	public String getExpression() {
		return expression;
	}

	public void setExpression(String expression) {
		this.expression = expression;
	}

	@Override
	public void load(Element formatterEle) {
		if(formatterEle == null) return;
		expression = formatterEle.getTextTrim();
	}
	
}
