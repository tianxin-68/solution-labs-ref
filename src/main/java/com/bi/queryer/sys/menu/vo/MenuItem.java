package com.bi.queryer.sys.menu.vo;

import com.bi.queryer.sys.enums.Enabled;
import com.bi.queryer.sys.enums.MenuOpenMode;
import com.bi.queryer.sys.enums.MenuType;
import com.bi.queryer.sys.enums.Platform;
import com.bi.queryer.util.BIConsts;
import com.bi.queryer.util.BIUtil;
import com.bi.queryer.util.StringUtil;
import com.alibaba.fastjson.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 菜单项
 * @author contributor
 *
 */
public class MenuItem implements Comparable<MenuItem>{
	private String id = null;
	
	private String  code = "";
	
	private String name = "";
	
	private  String enName = "";
	
	private String description = "";
	
	private String parentId = null;
	
	private MenuItem parent = null;
	
	private List<MenuItem> children = new ArrayList<MenuItem>();
	
	// 菜单路径（含自己）
	private List<MenuItem> path = new ArrayList<MenuItem>();
	
	private Integer showIndex = 0;
	
	private String style = "";
	
	private String url = "";
	
	private Integer openMode ;
	
	private Integer isApply ;
	
	private String type;
	
	private Integer unauthShowMode ;
	
	private Integer isActive ;
	
	private String createdTime;
	
	private String updatedTime;
	
	private Integer level = -1; // 级次
	
	private String platform = Platform.PC.toString(); // 平台
	
	// 元数据
	private MenuItemMeta meta = new MenuItemMeta();

	// tips

	private Integer isShow = 1; // 是否显示

	//全路径
	private String wholePath;

	//含有报表的目录
	private boolean containRptDir = false;

	//一级目录显示顺序
	private Integer firstDirShowIndex = 0;

	/**
	 * 是否是顶部模块
	 */
	private Integer isTopModule =0;

	/**
	 * 是否可搜索
	 */
	private Integer isSearch = Enabled.YES.getId();

	/**
	 * 所属系统
	 */
	private String menuSystem;

	/**
	 * 功能权限
	 */
	private List<Map<String,String>> funcs = new ArrayList<>();

	public Integer getIsShow() {
		return isShow;
	}
	public void setIsShow(Integer isShow) {
		this.isShow = isShow;
	}
	public MenuItemMeta getMeta() {
		return meta;
	}
	public void setMeta(MenuItemMeta meta) {
		this.meta = meta;
	}
	public String getPlatform() {
		return platform;
	}
	public void setPlatform(String platform) {
		this.platform = platform;
	}
	public Integer getLevel() {
		return level;
	}
	public void setLevel(Integer level) {
		this.level = level;
	}
	public MenuItem(){
		
	}
	public MenuItem(String id){
		this.id = id;
	}
	public MenuItem(String id, String name){
		this(id);
		this.name = name;
	}

	public JSONObject toTreeNode(){
		JSONObject node = new JSONObject();
		node.put("id", id + "");
		node.put("text", name);
		node.put("label", name);
		node.put("code", code);
		// "icon-item-new"
		node.put("iconCls", style);
		node.put("state", "close");
		
		node.put("description", description);
		node.put("metaReqOwner", meta.getReqOwner());
		node.put("metaReqRecOwner", meta.getReqRecOwner());
		
		JSONObject attributes = new JSONObject();
		attributes.put("id", this.getId());
		attributes.put("name", this.getName());
		attributes.put("code", this.getCode());
		attributes.put("url", this.getUrl());
		attributes.put("parentId", parentId);
		if(parent != null){
			attributes.put("parentName", parent.name);
		}
		attributes.put("style", style);
		attributes.put("menuType", MenuType.getType(this.getType()).toString()); //菜单类型：COMMON，BIEE，TABLEAU
		attributes.put("type", attributes.get("menuType"));
		MenuOpenMode openMode = MenuOpenMode.get(this.getOpenMode());
		attributes.put("openMode", openMode.toString());
		attributes.put("showIndex", showIndex);
		attributes.put("unauthShowMode", unauthShowMode);
		attributes.put("isApply", isApply);
		attributes.put("isShow", isShow);
		attributes.put("description", description);
		attributes.put("level", level);
		attributes.put("platform", Platform.get(platform).toString());
		attributes.put("isTopModule",this.getIsTopModule());
		attributes.put("isSearch",this.getIsSearch());
		attributes.put("menuSystem",this.menuSystem);
		attributes.put("funcs",funcs);

		if(meta == null) {
			meta = new MenuItemMeta();
		}
		node.put("metaReqOwner", meta.getReqOwner());
		node.put("metaReqRecOwner", meta.getReqRecOwner());
		attributes.put("metaLabels", meta.getLabels());

		//设置标签集合
		if(BIUtil.isNotEmpty(meta.getLabels())){
			attributes.put("tagList", meta.getLabels().split(","));
		}else{
			attributes.put("tagList", new ArrayList<>());
		}

		//设置tab页签集合
		if(BIUtil.isNotEmpty(meta.getMetaItems())){
			attributes.put("metaItems", meta.getMetaItems().split(","));
		}else{
			attributes.put("metaItems", new ArrayList<>());
		}

		attributes.put("path",wholePath);
		String tipType = "";
		attributes.put("tipType", tipType);
		
		// 设置icon
		node.put("attributes", attributes);
		return node;
	}

	/**
	 * 对外部应用提供
	 * @return
	 */
	public JSONObject toTreeNodeExternalProvision(){
		JSONObject node = new JSONObject();
		node.put("id", id + "");
		node.put("label", name);
		node.put("url",url);
		node.put("hidden",false);
		node.put("parentId",parentId);
		return node;
	}
	
	@Override
	public String toString() {
		return "id:" + id + "\t code:" + code + "\t name:" + name;
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj == null) return false;
		if(id == null) return false;
		return id.equals(((MenuItem)obj).id);
	}
	
	public void addChild(MenuItem child){
		if(!children.contains(child)){
			this.children.add(child);
		}
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getEnName() {
		return enName;
	}

	public void setEnName(String enName) {
		this.enName = enName;
	}

	public String getStyle() {
		return style;
	}

	public void setStyle(String style) {
		this.style = style;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public Integer getOpenMode() {
		return openMode;
	}

	public void setOpenMode(Integer openMode) {
		this.openMode = openMode;
	}

	public Integer getIsApply() {
		return isApply;
	}

	public void setIsApply(Integer isApply) {
		this.isApply = isApply;
	}

	public Integer getUnauthShowMode() {
		return unauthShowMode;
	}

	public void setUnauthShowMode(Integer unauthShowMode) {
		this.unauthShowMode = unauthShowMode;
	}

	public Integer getIsActive() {
		return isActive;
	}

	public void setIsActive(Integer isActive) {
		this.isActive = isActive;
	}

	public String getCreatedTime() {
		return createdTime;
	}

	public void setCreatedTime(String createdTime) {
		this.createdTime = createdTime;
	}

	public String getUpdatedTime() {
		return updatedTime;
	}

	public void setUpdatedTime(String updatedTime) {
		this.updatedTime = updatedTime;
	}

	public MenuItem getParent() {
		return parent;
	}

	public void setParent(MenuItem parent) {
		this.parent = parent;
	}

	public List<MenuItem> getChildren() {
		return children;
	}

	public void setChildren(List<MenuItem> children) {
		this.children = children;
	}

	public boolean isRoot() {
		return BIConsts.MENU_ROOT_ID.equals(this.id);
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public String getType() {
		if(type == null){
			type = MenuType.COMMON.toString();
		}
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public Integer getShowIndex() {
		return showIndex;
	}
	public void setShowIndex(Integer showIndex) {
		this.showIndex = showIndex;
	}
	public String getCode() {
		if(StringUtil.isEmpty(code)) {
			return id + "";
		}
		return code;
	}
	public void setCode(String code) {
		this.code = code;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getId() {
		return id;
	}
	public String getParentId() {
		return parentId;
	}
	public void setParentId(String parentId) {
		this.parentId = parentId;
	}
	@Override
	public int compareTo(MenuItem o) {
		if(o == null) return -1;
		return this.showIndex - o.getShowIndex();
	}

	public MenuItem addPath(MenuItem item) {
		if(item == null) {
			return this;
		}
		if(this.path.contains(item)) {
			return this;
		}
		this.path.add(item);
		return this;
	}
	public List<MenuItem> getPath() {
		return path;
	}
	public void setPath(List<MenuItem> path) {
		this.path = path;
	}

	public String getWholePath() {
		return wholePath;
	}

	public void setWholePath(String wholePath) {
		this.wholePath = wholePath;
	}

	public boolean isContainRptDir() {
		return containRptDir;
	}

	public void setContainRptDir(boolean containRptDir) {
		this.containRptDir = containRptDir;
	}

	public Integer getFirstDirShowIndex() {
		return firstDirShowIndex;
	}

	public void setFirstDirShowIndex(Integer firstDirShowIndex) {
		this.firstDirShowIndex = firstDirShowIndex;
	}

	public Integer getIsTopModule() {
		return isTopModule;
	}

	public void setIsTopModule(Integer isTopModule) {
		this.isTopModule = isTopModule;
	}

	public Integer getIsSearch() {
		return isSearch;
	}

	public void setIsSearch(Integer isSearch) {
		this.isSearch = isSearch;
	}

	public String getMenuSystem() {
		return menuSystem;
	}

	public void setMenuSystem(String menuSystem) {
		this.menuSystem = menuSystem;
	}

	public List<Map<String, String>> getFuncs() {
		return funcs;
	}

	public void setFuncs(List<Map<String, String>> funcs) {
		this.funcs = funcs;
	}
}
