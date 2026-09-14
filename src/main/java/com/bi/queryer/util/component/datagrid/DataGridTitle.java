package com.bi.queryer.util.component.datagrid;

import org.dom4j.Element;

import com.bi.queryer.util.BIUtil;

public class DataGridTitle {
	private String text = "";
	
	private int fontSize = 12;
	
	private String fontColor = "";
	
	private String fontStyle = "normal";
	
	private String align = "";
	
	private String valign = "";
	
	public void load(Element titleEle){
		String str = titleEle.getTextTrim();
		if(BIUtil.isNotEmpty(str)) {
			text = str;
		}
		
		str = titleEle.attributeValue("fontSize");
		if(BIUtil.isNotEmpty(str)) {
			fontSize = Integer.valueOf(str);
		}
		
		str = titleEle.attributeValue("fontColor");
		if(BIUtil.isNotEmpty(str)) {
			fontColor = str;
		}
		
		str = titleEle.attributeValue("fontStyle");
		if(BIUtil.isNotEmpty(str)) {
			fontStyle = str;
		}
		
		str = titleEle.attributeValue("align");
		if(BIUtil.isNotEmpty(str)) {
			align = str;
		}
		
		str = titleEle.attributeValue("valign");
		if(BIUtil.isNotEmpty(str)) {
			valign = str;
		}
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public int getFontSize() {
		return fontSize;
	}

	public void setFontSize(int fontSize) {
		this.fontSize = fontSize;
	}

	public String getFontColor() {
		return fontColor;
	}

	public void setFontColor(String fontColor) {
		this.fontColor = fontColor;
	}

	public String getFontStyle() {
		return fontStyle;
	}

	public void setFontStyle(String fontStyle) {
		this.fontStyle = fontStyle;
	}

	public String getAlign() {
		return align;
	}

	public void setAlign(String align) {
		this.align = align;
	}

	public String getValign() {
		return valign;
	}

	public void setValign(String valign) {
		this.valign = valign;
	}
}
