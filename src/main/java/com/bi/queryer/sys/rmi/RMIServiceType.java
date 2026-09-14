package com.bi.queryer.sys.rmi;

public enum RMIServiceType {
	FileSync("文件同步"),
	CacheSync("缓存同步");
	
	private String desc;
	
	private RMIServiceType(String desc){
		this.desc = desc;
	}
	
	public static RMIServiceType getType(String typeStr){
		for(RMIServiceType type : values()){
			if(type.toString().equalsIgnoreCase(typeStr)){
				return type;
			}
		}
		return null;
	}
	
	public String getDesc() {
		return desc;
	}

	public void setDesc(String desc) {
		this.desc = desc;
	}
	
	
}
