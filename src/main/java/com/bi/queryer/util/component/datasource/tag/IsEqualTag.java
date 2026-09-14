package com.bi.queryer.util.component.datasource.tag;

import org.apache.commons.lang3.StringUtils;
import org.dom4j.Element;

public class IsEqualTag extends DynamicSQLTag{

	public IsEqualTag(Element element) {
		super(element);
	}

	@Override
	public boolean accept() {
		if(parameters.get(property) != null){
			String parameterValue = String.valueOf(parameters.get(property));
			return parameterValue.equals(compareValue);
		}
		return false;
	}

	@Override
	public DynamicSQLTagType getType() {
		return DynamicSQLTagType.isEqual;
	}
	
	@Override
	public String validate() {
		String result = super.validate();
		
		if(StringUtils.isEmpty(result)){
			if(StringUtils.isEmpty(compareValue)){
				result = "<isEqual>标签必须包含compareValue属性";
			}
		}
		
		return result;
	}
}
