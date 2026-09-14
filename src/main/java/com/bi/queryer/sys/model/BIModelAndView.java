package com.bi.queryer.sys.model;

import org.springframework.web.servlet.ModelAndView;

import com.bi.queryer.util.BIConsts;

public class BIModelAndView extends ModelAndView{
	
	public BIModelAndView() {
		super();
	}
	
	public BIModelAndView(String name) {
		super(name);
	}
	
	public void addJsVariable(String varName, String varValue) {
		this.addObject(BIConsts.Model_View_JS_Var_Prefix + varName, varValue);
	}
	
	public void addJsVariable(String varName, Object jsObject) {
		this.addObject(BIConsts.Model_View_JS_Var_Prefix + varName, jsObject);
	}
	
	
}
