package com.bi.queryer.util.component;

public abstract class DataComponent extends Component{

	private static final long serialVersionUID = 1L;
	
	public DataComponent(String code){
		this.code = code;
		this.isDataComponent = true;
	}
}
