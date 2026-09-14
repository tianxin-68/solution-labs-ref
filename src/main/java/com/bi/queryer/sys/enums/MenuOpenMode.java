package com.bi.queryer.sys.enums;

/**
 * 菜单连接打开模式
 * @author contributor
 *
 */
public enum MenuOpenMode {
	ContentArea(0, "内容区"),
	NewWindow(1, "新窗口");
	
	private MenuOpenMode(int id, String desc) {
		this.id = id;
		this.desc = desc;
	}
	
	public static MenuOpenMode get(Integer id){
		if(id == null){
			return ContentArea;
		}
		for(MenuOpenMode m : MenuOpenMode.values()){
			if(m.getId() == id){
				return m;
			}
		}
		return ContentArea;
	}
	
	public static MenuOpenMode get(String code){
		for(MenuOpenMode m : MenuOpenMode.values()){
			if(m.toString().equalsIgnoreCase(code)){
				return m;
			}
		}
		return ContentArea;
	}

	private int id;
	
	private String desc;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getDesc() {
		return desc;
	}

	public void setDesc(String desc) {
		this.desc = desc;
	}
}
