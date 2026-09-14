package com.bi.queryer.util.download;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.bi.queryer.sys.db.DataType;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface FileField {
	String title();
	
	/**
	 * 数据类型
	 * @return
	 */
	DataType datatype() default DataType.String; 
	
	int order() default 0;
}
