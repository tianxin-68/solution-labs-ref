package com.bi.queryer.util.component.datasource.tag;

import org.apache.commons.lang3.StringUtils;
import org.dom4j.Element;

public class IsNotEmptyTag extends DynamicSQLTag{

	public IsNotEmptyTag(Element element) {
		super(element);
	}

	@Override
	public boolean accept() {
		if(parameters.get(property) != null){
			String parameterValue = parameters.get(property) == null ? "": String.valueOf(parameters.get(property));
			return StringUtils.isNotEmpty(parameterValue);
		}
		return false;
	}

	@Override
	public DynamicSQLTagType getType() {
		return DynamicSQLTagType.isNotEmpty;
	}

}
