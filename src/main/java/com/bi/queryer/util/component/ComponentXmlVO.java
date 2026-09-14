package com.bi.queryer.util.component;

import java.sql.Clob;
import java.sql.Timestamp;

public class ComponentXmlVO {

    protected long infoId = 0;
    protected String infoName;
    protected long infoType;
    protected String infoConfig;
    protected SqlXmlVO sqlConfig;
    protected int isConfig;
	protected Timestamp createDate;
    protected String createUser;
    protected Timestamp updateDate;
    protected String updateUser;
    protected int isActive = 1;
    protected String note;
    
    
	public long getInfoId() {
		return infoId;
	}
	public void setInfoId(long infoId) {
		this.infoId = infoId;
	}
	public String getInfoName() {
		return infoName;
	}
	public void setInfoName(String infoName) {
		this.infoName = infoName;
	}


	public long getInfoType() {
		return infoType;
	}
	public void setInfoType(long infoType) {
		this.infoType = infoType;
	}
	
	public String getInfoConfig() {
		return infoConfig;
	}
	public void setInfoConfig(String infoConfig) {
		this.infoConfig = infoConfig;
	}

	public String getCreateUser() {
		return createUser;
	}
	public void setCreateUser(String createUser) {
		this.createUser = createUser;
	}

	public Timestamp getCreateDate() {
		return createDate;
	}
	public void setCreateDate(Timestamp createDate) {
		this.createDate = createDate;
	}
	
	public Timestamp getUpdateDate() {
		return updateDate;
	}
	public void setUpdateDate(Timestamp updateDate) {
		this.updateDate = updateDate;
	}
	public String getUpdateUser() {
		return updateUser;
	}
	public void setUpdateUser(String updateUser) {
		this.updateUser = updateUser;
	}

	public int getIsActive() {
		return isActive;
	}
	public void setIsActive(int isActive) {
		this.isActive = isActive;
	}
	public String getNote() {
		return note;
	}
	public void setNote(String note) {
		this.note = note;
	}
	public SqlXmlVO getSqlConfig() {
		return sqlConfig;
	}
	public void setSqlConfig(SqlXmlVO sqlConfig) {
		this.sqlConfig = sqlConfig;
	}
	public int getIsConfig() {
		return isConfig;
	}
	public void setIsConfig(int isConfig) {
		this.isConfig = isConfig;
	}
   

}
