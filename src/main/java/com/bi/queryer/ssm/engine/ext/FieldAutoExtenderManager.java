package com.bi.queryer.ssm.engine.ext;


import com.bi.queryer.ssm.meta.MetaField;

public class FieldAutoExtenderManager {
	
	public static IFieldAutoExtender getExtender(MetaField src) {
		IFieldAutoExtender extender = null;
		if(src == null) return extender;
		//extender = new DateFieldAutoExtender();
		extender = new MeasureFieldAutoExtender();
		return extender;
	}
}
