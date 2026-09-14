package com.bi.queryer.util;

import org.dom4j.Element;
/**
 * 
 * 对象xml序列化存储
 * @author contributor
 *
 */
public interface XMLSerializable {
	
	public void load(Element e); 
	
	public void save(Element e);
}
