package com.bi.queryer.util.component;

import com.bi.queryer.util.SpringContextUtil;
import com.bi.queryer.util.StringUtil;

public abstract class ComponentManagerServiceFactory {
	public static IComponentManagerService getMgrService(ComponentType type) {
		IComponentManagerService service = null;
		String beanName = "";
		switch(type) {
		case Filter:
			beanName = "filterManagerService";
			break;
		case DataGrid:
			beanName = "dataGridManagerService";
			break;
		case Portal:
			beanName = "portalManagerService";
			break;
        case OlapChart:
			beanName = "chartManagerService";
			break;
		default:
			break;
		}
		if(!StringUtil.isEmpty(beanName)) {
			service = (IComponentManagerService) SpringContextUtil.getBean(beanName);
		}
		return service;
	}
}
