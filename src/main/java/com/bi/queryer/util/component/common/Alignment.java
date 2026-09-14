package com.bi.queryer.util.component.common;

public enum Alignment {
	Left,
	Center,
	Right,
	Horizontal,
	Vertical,
	Response,
	Auto;
	
	public static Alignment get(String str){
		for(Alignment align : values()){
			if(align.toString().equalsIgnoreCase(str)){
				return align;
			}
		}
		return Auto;
	}
}
