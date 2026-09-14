package com.bi.queryer.util.download;

/**
 * 文件下载类型
 * @author contributor
 *
 */
public enum FileDownloadType {
	/**
	 * 废弃，推荐使用07格式
	 */
	@Deprecated
	Excel03(".xls","2003版excel"),
	Excel07(".xlsx","2007版excel"),
	CSV(".csv","CSV文本,逗号分割"),
	TEXT(".txt","文本");
	
	private String extName ;
	
	private String desc;
	
	private FileDownloadType(String extName, String desc){
		this.extName = extName;
		this.desc = desc;
	}
	
	public static FileDownloadType getType(String extName){
		for(FileDownloadType type : values()){
			if(type.getExtName().equalsIgnoreCase(extName)){
				return type;
			}
		}
		return CSV;
	}
	
	public String getExtName() {
		return extName;
	}

	public void setExtName(String extName) {
		this.extName = extName;
	}

	public String getDesc() {
		return desc;
	}

	public void setDesc(String desc) {
		this.desc = desc;
	}
}
