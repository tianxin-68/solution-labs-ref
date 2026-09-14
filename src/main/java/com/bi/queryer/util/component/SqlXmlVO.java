package com.bi.queryer.util.component;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import com.bi.queryer.util.StringUtil;

public class SqlXmlVO<T extends Component> {

	private long infoId;
	private String infoName;
	private String infoConfig;
	private int isActive = 1;
	
	
	public void load(T component){
		if(StringUtil.isEmpty(infoConfig)){
			return;
		}
		SAXReader saxReader = new SAXReader();
		try {
			Document document = saxReader.read(new StringReader(infoConfig));
			Element root = document.getRootElement();
			Element sqlStr = root.element("SqlStr");
			Element sqlSource = root.element("SqlSource");
			Element params = root.element("Params");
			if(params != null){
				List<SqlParam> list = new ArrayList();
				List<Element> paramsList = params.elements("Param");
				for(Element el : paramsList){
					SqlParam param = new SqlParam();
					param.setName(StringUtil.ifNull(el.attributeValue("name")));
					param.setType(StringUtil.ifNull(el.attributeValue("type")));
					param.setValue(StringUtil.ifNull(el.attributeValue("value")));
					list.add(param);
				}
			}
		} catch (DocumentException e) {
			e.printStackTrace();
		}
	}
	
	public void save(T component){
		Document docSql = XmlHelper.createDocument();
		Element rootSql = docSql.addElement("Sql");
		Element sqlStr = rootSql.addElement("SqlStr");
		Element sqlSource = rootSql.addElement("SqlSource");
		this.setInfoConfig(docSql.asXML());
	}
	
	public long getInfoId() {
		return infoId;
	}
	public void setInfoId(long infoId) {
		this.infoId = infoId;
	}
	public String getInfoName() {
		return infoName;
	}
	public void setInfoName(String infoName) {
		this.infoName = infoName;
	}
	public String getInfoConfig() {
		return infoConfig;
	}
	public void setInfoConfig(String infoConfig) {
		this.infoConfig = infoConfig;
	}
	public int getIsActive() {
		return isActive;
	}
	public void setIsActive(int isActive) {
		this.isActive = isActive;
	}

	
}
