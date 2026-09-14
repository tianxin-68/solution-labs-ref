package com.bi.queryer.util.component.portlet;

/**
 * portlet类型枚举
 * @author contributor
 *
 */
public enum PortletType {
	VLayout("com.bi.queryer.util.rpt.designer.portlet.VLayoutPortlet", "com.bi.queryer.util.rpt.designer.render.VLayoutPortletRender", "垂直布局"),
	HLayout("", "", "水平布局"),
	Tab("", "", "页签"),
	Swiper("", "", "左右滑屏"),
	Chart("", "", "图表"),
	DataGrid("", "", "表格"),
	FitKPI("", "", "自适应KPI面板"),
	NavKPI("", "", "导航KPI面板");
	
	private String modelClass;
	private String renderClass;
	private String desc;
	
	private PortletType(String modelClass, String renderClass, String desc){
		this.modelClass = modelClass;
		this.renderClass = renderClass;
		this.desc = desc;
	}
	
	public static PortletType getType(String typeStr){
		for(PortletType type : values()){
			if(type.toString().equalsIgnoreCase(typeStr)){
				return type;
			}
		}
		return VLayout;
	}

	public String getModelClass() {
		return modelClass;
	}

	public void setModelClass(String modelClass) {
		this.modelClass = modelClass;
	}

	public String getRenderClass() {
		return renderClass;
	}

	public void setRenderClass(String renderClass) {
		this.renderClass = renderClass;
	}

	public String getDesc() {
		return desc;
	}

	public void setDesc(String desc) {
		this.desc = desc;
	}
}