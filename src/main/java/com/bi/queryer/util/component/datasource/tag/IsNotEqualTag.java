package com.bi.queryer.util.component.datasource.tag;

import org.apache.commons.lang3.StringUtils;
import org.dom4j.Element;

public class IsNotEqualTag extends DynamicSQLTag{

	public IsNotEqualTag(Element element) {
		super(element);
	}

	@Override
	public boolean accept() {
		String parameterValue = parameters.get(property) == null ? "": String.valueOf(parameters.get(property));
		if(StringUtils.isNotEmpty(compareValue)){
			return !compareValue.equals(parameterValue);
		}else{
			return true;
		}
	}

	@Override
	public DynamicSQLTagType getType() {
		return DynamicSQLTagType.isNotEqual;
	}

	@Override
	public String validate() {
		String result = super.validate();
		
		if(StringUtils.isEmpty(result)){
			if(StringUtils.isEmpty(compareValue)){
				result = "<isNotEqual>标签必须包含compareValue属性";
			}
		}
		
		return result;
	}
	
}
