package com.bi.queryer.util.component.menu;

import com.bi.queryer.util.component.Component;
import com.bi.queryer.util.component.ComponentType;

/**
 * 入口容器
 * @author contributor
 *
 */
public class EntryPointContainer extends Component{

	private static final long serialVersionUID = 1L;
	
	
	/**
	 * 每页入口个数
	 */
	protected int capacity = 9;

	public EntryPointContainer(){
		
	}
	
	public void addEntryPoint(EntryPoint point){
		addChild(point);
	}
	
	@Override
	public ComponentType getType() {
		return ComponentType.EntryPointContainer;
	}

	public int getCapacity() {
		return capacity;
	}

	public void setCapacity(int capacity) {
		this.capacity = capacity;
	}


}
