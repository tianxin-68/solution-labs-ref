package com.bi.queryer.util.component.portlet.render;

import com.alibaba.fastjson.JSONObject;
import com.bi.queryer.util.component.Component;
import com.bi.queryer.util.component.ComponentConstants;
import com.bi.queryer.util.component.ComponentEntity;
import com.bi.queryer.util.component.ComponentManagerServiceFactory;
import com.bi.queryer.util.component.ComponentRender;
import com.bi.queryer.util.component.IComponentManagerService;
import com.bi.queryer.util.component.common.Alignment;
import com.bi.queryer.util.component.portlet.Portlet;
import com.bi.queryer.util.component.portlet.PortletRenderMode;

public abstract class PortletRender<T extends Portlet> extends ComponentRender<T>{
	
	public PortletRender(T component) {
		super(component);
	}
	
	public StringBuilder beginHtml(){
		StringBuilder html = new StringBuilder();
		if(component == null) return html;
		// 先添加装饰html
		html.append(decorateBeginHtml());
		
		// 再添加portlet本身html
		if(component.getRenderMode() == PortletRenderMode.Designer){
			html.append(designerBeginHtml());
		}else {
			html.append(buildBeginHtml());
		}
		return html;
	}
	
	public StringBuilder endHtml(){
		StringBuilder html = new StringBuilder();
		if(component == null) return html;
		if(component.getRenderMode() == PortletRenderMode.Designer){
			html.append(designerEndHtml());
		}else {
			html.append(buildEndHtml());
		}
		html.append(decorateEndHtml());
		return html;
	}
	
	@Override
	public String buildBeginHtml() {
		return super.buildBeginHtml();
	}
	
	@Override
	public String buildEndHtml() {
		return super.buildEndHtml();
	}
	
	
	/**
	 * 对portlet渲染的html装饰开始
	 * @return
	 */
	public String decorateBeginHtml(){
		StringBuilder html = new StringBuilder();
		String style = "";
		if(component == null) return "";
		if(component.getRenderMode() == PortletRenderMode.Designer){
			style = "designer-portlet-wrap";
			if(getLayoutInParent() == Alignment.Left){
				style = style + " designer-layout-left";
			}
			html.append("<div id='").append(component.getId() + "_WRAP").append("' ")
					.append(" class='").append(style).append("'")
					.append("");
			html.append(">");
		}
		
		// 发布模式下，不添加装饰代码
		/*
		else{
			style = "";
			if(getLayoutInParent() == Alignment.Left){
				style = style + " h5-hlayout";
			}
			html.append("<div id='").append(component.getId() + "_WRAP").append("' ")
					.append(" class='").append(style).append("'")
					.append("");
			html.append(">");
		}
		*/
		return html.toString();
	}
	
	/**
	 * 对portlet渲染的html装饰结束
	 * @return
	 */
	public String decorateEndHtml(){
		StringBuilder html = new StringBuilder();
		if(component.getRenderMode() == PortletRenderMode.Designer){
			html.append("</div>");
		}
		return html.toString();
	}
	
	/**
	 * 获取布局样式表，用于支持水平布局的组件调用
	 * @return
	 */
	public String getLayoutCSS(){
		String style = "";
		if(getLayoutInParent() == Alignment.Left){
			style = "h5-hlayout";
		}
		return style;
	}
	
	/**
	 * 组件编辑器开始html
	 * @return
	 */
	public String designerBeginHtml(){
		StringBuilder html = new StringBuilder();
		html.append("<div id='").append(component.getId()).append("' ");
		html.append(" class='designer-portlet'");
		JSONObject designerOpt = desigerOption();
		html.append(ComponentConstants.PORTLET_DESIGNER_OPTION).append("=").append("'").append(designerOpt).append("'");
		html.append(" >");
		return html.toString();
	}
	
	/**
	 * 组件编辑器结束html
	 * @return
	 */
	public String designerEndHtml(){
		StringBuilder html = new StringBuilder();
		html.append("</div>");
		return html.toString();
	}
	
	/**
	 * 设计器模式时选项
	 * @return
	 */
	public JSONObject desigerOption(){
		JSONObject json = new JSONObject();
		json.put("name", component.getName());
		json.put(ComponentConstants.Parameter_Component_ID, component.getId());
		json.put(ComponentConstants.Parameter_Component_PID, component.getParentId());
		json.put(ComponentConstants.Parameter_Component_Type, component.getType());
		json.put("typeName", component.getType().getDesc());
		json.put("categoryId", component.getCategoryId());
		String cname = component.getName();
		
		// 数据组件，需重新获取引用Name，避免资源名称修改或资源被删除
		if("data".equalsIgnoreCase(component.getCategoryId())){
			IComponentManagerService mgrService = ComponentManagerServiceFactory.getMgrService(component.getType());
			ComponentEntity entity = mgrService.getById(component.getEntityId());
			cname = entity.getName();
		}
		json.put("entityId", component.getEntityId());
		component.setEntityName(cname);
		json.put("entityName", cname);
		json.put("isRoot", component.isRoot());
		
		// 实际宽度为
		json.put("width", component.getRealWidth());
		json.put("height", component.getRealHeight());
		json.put("margin", component.getMargin().toString());
		return json;
	}
	
	/**
	 * 获取其在父节点中的布局方向
	 * @return
	 */
	public Alignment getLayoutInParent(){
		Component p = component.getParent();
		// 如果父亲是水平布局，则其在父容器中为居左对齐
		if(p != null && (p.getChildLayoutAlign() == Alignment.Horizontal || p.getChildLayoutAlign() == Alignment.Response)){
			return Alignment.Left;
		}
		return Alignment.Auto;
	}

}
