package com.bi.queryer.util.component.menu.render;

import com.alibaba.fastjson.JSONObject;
import com.bi.queryer.util.component.ComponentRender;
import com.bi.queryer.util.component.menu.MenuItemComponent;

public class MenuItemComponentRender extends ComponentRender<MenuItemComponent>{

	public MenuItemComponentRender(MenuItemComponent component) {
		super(component);
	}
	
	@Override
	public String buildBeginHtml() {
		StringBuilder html = new StringBuilder();
		html.append("<div ")
			.append(buildDataOptions(buildOptions()))
			.append(" class='h5-menu-item'>")
			.append(component.getName())
			.append("</div>");
		return html.toString();
	}
	
	@Override
	public String buildEndHtml() {
		StringBuilder html = new StringBuilder();
		return html.toString();
	}

	@Override
	public JSONObject buildOptions() {
		JSONObject json = new JSONObject();
		json.put("href", component.getHref());
		return json;
	}

}
