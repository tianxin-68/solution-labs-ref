package com.bi.queryer.util.component.menu.render;

import com.alibaba.fastjson.JSONObject;
import com.bi.queryer.util.component.Component;
import com.bi.queryer.util.component.ComponentRender;
import com.bi.queryer.util.component.menu.EntryPointContainer;

public class EntryPointContainerRender extends ComponentRender<EntryPointContainer>{

	protected Component[] children = null;
	
	public EntryPointContainerRender(EntryPointContainer component) {
		super(component);
		children = component.getChildren();
	}
	
	@Override
	public String buildBeginHtml() {
		if(children == null || children.length == 0){
			return "";
		}
		StringBuilder html = new StringBuilder();
		html.append("<div ").append(" class='h5-module-main' ").append(">");
			html.append("<div ")
				.append(" class='h5-module-container' >");
		return html.toString();
	}
	
	@Override
	public String buildEndHtml() {
		if(children == null || children.length == 0){
			return "";
		}
		StringBuilder html = new StringBuilder();
			html.append("</div>"); // h5-module-container
		html.append("</div>");// h5-module-main
		return html.toString();
	}
	
	@Override
	public JSONObject buildOptions() {
		return null;
	}

}
