package com.bi.queryer.sys.config;

/**
 * 系统选项（单个选项实体）
 * @author contributor
 *
 */
public class SystemOption {
	private String code ;
	
	private String name;
	
	private Integer type;
	
	private String parentCode;
	
	private String value;
	
	private String showType;
	
	private String selectValues; 
	
	private Integer showIndex;
	
	private String remark ;

	private boolean changeValue;
	
	@Override
	public String toString() {
		return name + "[" + code + "] = " + value;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Integer getType() {
		return type;
	}

	public void setType(Integer type) {
		this.type = type;
	}

	public String getParentCode() {
		return parentCode;
	}

	public void setParentCode(String parentCode) {
		this.parentCode = parentCode;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public String getShowType() {
		return showType;
	}

	public void setShowType(String showType) {
		this.showType = showType;
	}

	public String getSelectValues() {
		return selectValues;
	}

	public void setSelectValues(String selectValues) {
		this.selectValues = selectValues;
	}

	public Integer getShowIndex() {
		return showIndex;
	}

	public void setShowIndex(Integer showIndex) {
		this.showIndex = showIndex;
	}

	public String getRemark() {
		return remark;
	}

	public void setRemark(String remark) {
		this.remark = remark;
	}

	public boolean isChangeValue() {
		return changeValue;
	}

	public void setChangeValue(boolean changeValue) {
		this.changeValue = changeValue;
	}
}
