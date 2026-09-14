package com.bi.queryer.util.component;

/**
 * xml解析器类型
 * @author contributor
 *
 */
public enum XmlHelperType {

	DataGrid("com.bi.queryer.util.component.datagrid.DataGrid", "com.bi.queryer.util.component.datagrid.xml.DataGridXmlHelper", "com.bi.queryer.util.component.datagrid.xml.DataGridXmlVO","DataGrid表格"),
	InputContainer("com.bi.queryer.util.component.input.InputContainer", "com.bi.queryer.util.component.input.xml.InputContainerXmlHelper", "com.bi.queryer.util.component.input.xml.InputContainerXmlVO","InputContainer输入"),
	Portal("com.bi.queryer.util.rpt.designer.model.Portal", "com.bi.queryer.util.rpt.designer.services.PortalXmlHelper", "com.bi.queryer.util.rpt.designer.model.PortalXmlVO","Portal"),
	Chart("com.bi.queryer.util.component.chart.Chart", "com.bi.queryer.util.component.chart.xml.ChartXmlHelper", "com.bi.queryer.util.component.chart.xml.ChartXmlVO","Chart输入"),
	KPINavGrid("com.bi.queryer.util.component.board.KPINavGrid", "com.bi.queryer.util.component.board.xml.KPINavGridXmlHelper", "com.bi.queryer.util.component.board.xml.KPINavGridXmlVO","KPI导航"),
	KPIFitGrid("com.bi.queryer.util.component.board.KPIFitGrid", "com.bi.queryer.util.component.board.xml.KPIFitGridXmlHelper", "com.bi.queryer.util.component.board.xml.KPIFitGridXmlVO","KPIFitGrid表格");
	
	private String modelClass ;
	private String xmlHelperClass;
	private String xmlVOClass;
	private String desc;
	
	private XmlHelperType(String modelClass, String xmlHelperClass,String xmlVOClass, String desc){
		this.modelClass = modelClass;
		this.xmlHelperClass = xmlHelperClass;
		this.xmlVOClass = xmlVOClass;
		this.desc = desc;
	}
	
	public static XmlHelperType getType(String typeStr){
		for(XmlHelperType type : values()){
			if(type.toString().equalsIgnoreCase(typeStr)) {
				return type;
			}
		}
		return null;
	}
	
	public static XmlHelperType getTypeByClassName(String className){
		for(XmlHelperType type : values()){
			if(type.getModelClass().equalsIgnoreCase(className)) {
				return type;
			}
		}
		return null;
	}

	public String getModelClass() {
		return modelClass;
	}

	public void setModelClass(String modelClass) {
		this.modelClass = modelClass;
	}

	public String getXmlHelperClass() {
		return xmlHelperClass;
	}

	public void setXmlHelperClass(String xmlHelperClass) {
		this.xmlHelperClass = xmlHelperClass;
	}

	public String getXmlVOClass() {
		return xmlVOClass;
	}

	public void setXmlVOClass(String xmlVOClass) {
		this.xmlVOClass = xmlVOClass;
	}

	public String getDesc() {
		return desc;
	}

	public void setDesc(String desc) {
		this.desc = desc;
	}
	
	
}
