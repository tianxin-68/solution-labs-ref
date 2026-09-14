package com.bi.queryer.util.component;

/**
 * 设计器服务类工厂
 * @author contributor
 *
 */
public abstract class DesignerServiceFactory {
	
	/**
	 * 获取服务
	 * @param type
	 * @return
	 */
	public static BaseDesignerService getService(ComponentType type){
		BaseDesignerService service = null;
//		switch(type){
//		case DataGrid:
//			service = new DataGridDesignerService();
//			break;
//		case Filter:
//			service = new FilterDesignerService();
//			break;
//		case Portal:
//			service = new PortalDesignerService();
//			break;
//		default:
//			break;
//		}
		return service;
	}
}
