package com.bi.queryer.util.component.datagrid;


/**
 * grid钻取模式
 * @author contributor
 *
 */
public enum DataGridDrillMode {
	SingleColumn("单列钻取"),
	SingleRow("单行钻取"),
	RedirectSelf("跳转-当前页面替换"),
	RedirectBlank("跳转-弹出页面替换");
	
	private String desc;
	
	public static DataGridDrillMode getType(String strType){
		for(DataGridDrillMode type : values()){
			if(type.toString().equalsIgnoreCase(strType)){
				return type;
			}
		}
		return SingleColumn;
	}
	private DataGridDrillMode(String desc){
		this.desc = desc;
	}

	public String getDesc() {
		return desc;
	}

	public void setDesc(String desc) {
		this.desc = desc;
	}
}
