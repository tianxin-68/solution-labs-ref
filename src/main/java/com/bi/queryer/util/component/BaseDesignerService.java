package com.bi.queryer.util.component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 组件设计器服务基类
 * @author contributor
 *
 */
public abstract class BaseDesignerService {
	
	/**
	 * 导出
	 * @param c
	 */
	public void export(Component c, HttpServletRequest request, HttpServletResponse response){
		
	}
	
	/**
	 * 组件类型
	 * @return
	 */
	public abstract ComponentType getType();
}
