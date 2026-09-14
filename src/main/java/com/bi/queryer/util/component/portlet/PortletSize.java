package com.bi.queryer.util.component.portlet;

public class PortletSize {
	public int width = -99;
	public int height = -99;
	public PortletMargin margin = new PortletMargin();
	
	public boolean resized(){
		return width != -99 && height != -99;
	}

	public PortletMargin getMargin() {
		return margin;
	}

	public void setMargin(PortletMargin margin) {
		this.margin = margin;
	}

	public int getWidth() {
		return width;
	}

	public void setWidth(int width) {
		this.width = width;
	}

	public int getHeight() {
		return height;
	}

	public void setHeight(int height) {
		this.height = height;
	}

}
