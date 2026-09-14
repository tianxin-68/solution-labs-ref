package com.bi.queryer.util.component.menu;

import com.bi.queryer.util.component.Component;
import com.bi.queryer.util.component.ComponentConstants;
import com.bi.queryer.util.component.ComponentType;
import com.bi.queryer.util.component.ComponentUtil;

/**
 * 菜单项，数据库模块表的实体类
 * @author contributor
 *
 */
public class MenuItemComponent extends Component{

	private static final long serialVersionUID = 1L;
	
	/**
	 * 菜单地址
	 */
	protected String href = "";
	
	/**
	 * 图标/背景图
	 */
	protected String image = "";
	
	/**
	 * 直接上级ID
	 */
	protected String parentId = "";
	
	/**
	 * 菜单项相对根节点全路径
	 */
	protected String path = "";
	
	/**
	 * 相对根节点的级次
	 */
	protected int itemLevel = 0;
	
	/**
	 * 根节点id
	 */
	protected String rootId = "";
	
	/**
	 * 菜单项样式表
	 */
	protected String styleClass = "";
	
	/**
	 * 是否可用标识
	 */
	protected int authStatus = 1;// 1:已授权 ，0 ：未授权
	
	protected int unauthShowStatus = 0; // 0:显示（未授权），1：不显示
	
	protected String description = "";
	
	/**
	 * 显示顺序
	 */
	protected int showIndex = 1;
	
	
	@Override
	public ComponentType getType() {
		return ComponentType.MenuItem;
	}

	public String getHref() {
		return href;
	}
	
	public String getHref(boolean appendParameter){
		href = ComponentUtil.getFullURL(request, href);
		// 添加参数
		if(href.indexOf("?") == -1){
			href = href + "?";
		}
		href = href + "&" + ComponentConstants.Parameter_Module_Id + "=" + id 
				+ "&" + ComponentConstants.Parameter_Module_Pid + "=" + parentId
				+ "&" + ComponentConstants.Parameter_Module_Code + "=" + code
				+ "&" +  ComponentConstants.Parameter_Module_Root + "=" + rootId;
		return href;
	}

	public void setHref(String href) {
		this.href = href;
	}

	public String getImage() {
		return image;
	}

	public void setImage(String image) {
		this.image = image;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getParentId() {
		return parentId;
	}

	public void setParentId(String parentId) {
		this.parentId = parentId;
	}

	public int getItemLevel() {
		return itemLevel;
	}

	public void setItemLevel(int itemLevel) {
		this.itemLevel = itemLevel;
	}

	public String getRootId() {
		return rootId;
	}

	public void setRootId(String rootId) {
		this.rootId = rootId;
	}

	public String getStyleClass() {
		return styleClass;
	}

	public void setStyleClass(String styleClass) {
		this.styleClass = styleClass;
	}


	public boolean authorized(){
		return this.authStatus == 1;
	}

	public int getAuthStatus() {
		return authStatus;
	}

	public void setAuthStatus(int authStatus) {
		this.authStatus = authStatus;
	}

	public int getUnauthShowStatus() {
		return unauthShowStatus;
	}

	public void setUnauthShowStatus(int unauthShowStatus) {
		this.unauthShowStatus = unauthShowStatus;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public int getShowIndex() {
		return showIndex;
	}

	public void setShowIndex(int showIndex) {
		this.showIndex = showIndex;
	}
}
