package com.bi.queryer.util.component.text;

import org.apache.commons.lang3.StringUtils;
import org.dom4j.Element;

import com.bi.queryer.util.component.Component;
import com.bi.queryer.util.component.ComponentType;

public class Text extends Component{

	private static final long serialVersionUID = 1L;

	private String text = "";
	
	private int fontSize = 12;
	
	private String fontColor = "#000000";
	
	private String fontStyle = "normal";
	
	private String align = "center";
	
	private String valign = "middle";
	
	private String bgColor = "transport";
	
	public Text(){
		
	}
	
	public Text(String text){
		this.text = text;
	}
	
	public void load(Element textEle){
		super.load(textEle);
		String str = textEle.getTextTrim();
		if(StringUtils.isNotEmpty(str)) {
			text = str;
		}
		
		str = textEle.attributeValue("fontSize");
		if(StringUtils.isNotEmpty(str)) {
			fontSize = Integer.valueOf(str);
		}
		
		str = textEle.attributeValue("fontColor");
		if(StringUtils.isNotEmpty(str)) {
			fontColor = str;
		}
		
		str = textEle.attributeValue("bgColor");
		if(StringUtils.isNotEmpty(str)) {
			bgColor = str;
		}
		
		str = textEle.attributeValue("fontStyle");
		if(StringUtils.isNotEmpty(str)) {
			fontStyle = str;
		}
		
		str = textEle.attributeValue("align");
		if(StringUtils.isNotEmpty(str)) {
			align = str;
		}
		
		str = textEle.attributeValue("valign");
		if(StringUtils.isNotEmpty(str)) {
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

	@Override
	public ComponentType getType() {
		return ComponentType.Text;
	}

	public String getBgColor() {
		return bgColor;
	}

	public void setBgColor(String bgColor) {
		this.bgColor = bgColor;
	}
}
