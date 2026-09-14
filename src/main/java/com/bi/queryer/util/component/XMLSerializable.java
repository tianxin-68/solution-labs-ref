package com.bi.queryer.util.component;

import java.io.File;
import java.io.FileInputStream;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
public abstract class XMLSerializable implements com.bi.queryer.util.XMLSerializable{
	protected String configPath = null;
	
	public void load(String configPath) {
		this.configPath = configPath;
		SAXReader saxReader = new SAXReader();
		try {
			Document document = saxReader.read(new FileInputStream(new File(configPath)));
			Element root = document.getRootElement();
			if(root != null) {
				load(root);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	/**
	 * 加载
	 * @param root
	 */
	public abstract void load(Element e);
	
	/**
	 * 保存
	 * @return
	 */
	public void save(Element e){
		
	}
}
