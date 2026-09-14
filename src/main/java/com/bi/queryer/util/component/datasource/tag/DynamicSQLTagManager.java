package com.bi.queryer.util.component.datasource.tag;

import org.dom4j.Element;

public abstract class DynamicSQLTagManager {
	
	/**
	 * 通过element获取Tag
	 * @param element
	 * @return
	 */
	public static DynamicSQLTag getTag(Element element){
		String tagName = element.getName();
		DynamicSQLTag tag = null;
		DynamicSQLTagType type = DynamicSQLTagType.getType(tagName);
		switch(type){
		case isEmpty:
			tag = new IsEmptyTag(element);
			break;
		case isNotEmpty:
			tag = new IsNotEmptyTag(element);
			break;
		case isEqual:
			tag = new IsEqualTag(element);
			break;
		case isNotEqual:
			tag = new IsNotEqualTag(element);
			break;
		}
		return tag;
	}
}
