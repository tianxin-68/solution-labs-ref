package com.bi.queryer.util.component.portlet;

import java.io.Serializable;

import org.dom4j.Element;

import com.bi.queryer.util.component.XMLSerializable;

public class PortletMargin extends XMLSerializable implements Cloneable, Serializable{
	private static final long serialVersionUID = 1L;
	
	protected Integer left = 0;
	protected Integer right = 0;
	protected Integer top = 0;
	protected Integer bottom = 0;
	
	@Override
	protected PortletMargin clone() throws CloneNotSupportedException {
		return (PortletMargin) super.clone();
	}
	
	public PortletMargin(){
		
	}
	
	public PortletMargin(Integer top , Integer right, Integer bottom, Integer left){
		this.top = top;
		this.right = right;
		this.bottom = bottom;
		this.left = left;
	}
	
	@Override
	public void load(Element e) {
		if(e == null){
			return;
		}
		this.left = Integer.valueOf(e.attributeValue("left"));
		this.right = Integer.valueOf(e.attributeValue("right"));
		this.top = Integer.valueOf(e.attributeValue("top"));
		this.bottom = Integer.valueOf(e.attributeValue("bottom"));
	}
	
	@Override
	public void save(Element e) {
		e.addAttribute("left", left + "");
		e.addAttribute("right", right + "");
		e.addAttribute("top", top + "");
		e.addAttribute("bottom", bottom + "");
	}
	
	@Override
	public String toString() {
		return top + "px " + right + "px " + bottom + "px " + left + "px";
	}

	public Integer getLeft() {
		return left;
	}

	public void setLeft(Integer left) {
		this.left = left;
	}

	public Integer getRight() {
		return right;
	}

	public void setRight(Integer right) {
		this.right = right;
	}

	public Integer getTop() {
		return top;
	}

	public void setTop(Integer top) {
		this.top = top;
	}

	public Integer getBottom() {
		return bottom;
	}

	public void setBottom(Integer bottom) {
		this.bottom = bottom;
	}
	
	public static void main(String[] args) {
	}
}
