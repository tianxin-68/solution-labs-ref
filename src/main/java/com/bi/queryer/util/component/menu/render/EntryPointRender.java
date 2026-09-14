package com.bi.queryer.util.component.menu.render;

import com.alibaba.fastjson.JSONObject;
import com.bi.queryer.util.StringUtil;
import com.bi.queryer.util.component.ComponentRender;
import com.bi.queryer.util.component.menu.EntryPoint;

public class EntryPointRender extends ComponentRender<EntryPoint> {

	public EntryPointRender(EntryPoint component) {
		super(component);
	}
	
	@Override
	public String buildBeginHtml() {
		StringBuilder html = new StringBuilder();
		String itemClass = component.getMenuItem().getStyle();
		if(StringUtil.isEmpty(itemClass)) {
			itemClass = "";
		}else{
			itemClass = " " + itemClass;
		}
//		if(component.getScreenIndex() == 1){
		if(component.getIndex() % 2 == 0){
			itemClass = itemClass + " h5-module-entrypoint-right";
		}
		if(component.getIndex() % 2 != 0 ){
			if(component.getIsLast()){
				itemClass = itemClass + " h5-module-entrypoint-left";
			}
		}
//		}
		
		
		String tip = "";
		String unAuthClass = "";
//		if(StringUtil.isEmpty(component.getHref())){
//			tip = "<label style='font-size:12px;'>(待开发)</label>";
//		}else if(!component.isAuthorized()){
//			unAuthClass = "unAuth";
//			tip = "<label style='color:#de5d48;font-size:12px;'>(未授权)</label>";
//		}
		
		html.append("<div ")
			.append(" title='").append(component.getName()).append("'")
			.append(buildDataOptions(buildOptions()))
			.append(" class='h5-module-entrypoint " + itemClass + "'").append(">");
			html.append("<a href='javascript:void(0)'><i></i><span class='" + unAuthClass + "'>").append(component.getName() + tip).append("</span></a>");
			/*
			html.append("<button type='button'")
				.append(" class='btn btn-info btn-lg btn-block'")
				.append(" >");
			html.append(component.getName());
			html.append("</button>");
			*/
		html.append("</div>");
		return html.toString();
	}
	
	@Override
	public String buildEndHtml() {
		return "";
	}

	@Override
	public JSONObject buildOptions() {
		JSONObject opts = new JSONObject();
		if(component.isAuthorized()){
			opts.put("href", component.getHref(true));
		}
		opts.put("userName",component.getRequest().getParams().get("userName"));
		return opts;
	}

}
