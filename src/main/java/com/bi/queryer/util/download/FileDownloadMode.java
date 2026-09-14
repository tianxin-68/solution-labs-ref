package com.bi.queryer.util.download;

/**
 * 文件导出方式
 * @author contributor
 *
 */
public enum FileDownloadMode {
	DEFAULT("default","默认方式"),
	FAST("fast","快速导出(仅支持CSV)");
	
	private String modeName ;
	
	private String desc;
	
	private FileDownloadMode(String modeName, String desc){
		this.modeName = modeName;
		this.desc = desc;
	}
	
	public static FileDownloadMode getType(String modeName){
		for(FileDownloadMode type : values()){
			if(type.getModeName().equalsIgnoreCase(modeName)){
				return type;
			}
		}
		return DEFAULT;
	}
	
	public String getModeName() {
		return modeName;
	}

	public void setModeName(String modeName) {
		this.modeName = modeName;
	}

	public String getDesc() {
		return desc;
	}

	public void setDesc(String desc) {
		this.desc = desc;
	}
}
