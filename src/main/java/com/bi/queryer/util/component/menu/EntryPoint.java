package com.bi.queryer.util.component.menu;

import com.bi.queryer.sys.menu.vo.MenuItem;
import com.bi.queryer.util.StringUtil;
import com.bi.queryer.util.component.Component;
import com.bi.queryer.util.component.ComponentConstants;
import com.bi.queryer.util.component.ComponentType;
import com.bi.queryer.util.component.ComponentUtil;

public class EntryPoint extends Component{

	private static final long serialVersionUID = 1L;
	
	private  MenuItem menuItem = null;
	
	//是否最后一个
	private Boolean isLast = false; 
	
	private String href = "";
	
	private boolean authorized = false;// 是否有权限
	
	public String getHref() {
		return href;
	}

	public void setHref(String href) {
		this.href = href;
	}
	
	public boolean isAuthorized() {
		return authorized;
	}

	public void setAuthorized(boolean authorized) {
		this.authorized = authorized;
	}

	public MenuItem getMenuItem() {
		return menuItem;
	}

	public void setMenuItem(MenuItem menuItem) {
		this.menuItem = menuItem;
	}

	public EntryPoint(MenuItem menuItem){
		this.menuItem = menuItem;
		this.href = menuItem.getUrl();
		this.name = menuItem.getName();
	}
	
	public String getHref(boolean appendParameter){
		if(StringUtil.isEmpty(href)){
			return "";
		}
		href = ComponentUtil.getFullURL(request, href);
		// 添加参数
		if(href.indexOf("?") == -1){
			href = href + "?";
		}
		href = href + "&" + ComponentConstants.Parameter_Module_Id + "=" + id 
				+ "&" + ComponentConstants.Parameter_Module_Pid + "=" + menuItem.getParentId()
				+ "&" + ComponentConstants.Parameter_Module_Code + "=" + code 
				+ "&menuId=" + menuItem.getId()
				+ "&userName="+request.getParams().get("userName");
		return href;
	}
	
	/**
	 * 所属第N屏幕
	 */
	private int screenIndex = 1;
	
	@Override
	public ComponentType getType() {
		return ComponentType.EntryPoint;
	}

	public int getScreenIndex() {
		return screenIndex;
	}

	public void setScreenIndex(int screenIndex) {
		this.screenIndex = screenIndex;
	}

	public Boolean getIsLast() {
		return isLast;
	}

	public void setIsLast(Boolean isLast) {
		this.isLast = isLast;
	}
	
	

}
