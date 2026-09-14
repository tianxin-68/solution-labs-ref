package com.bi.queryer.ssm.mgr.exportImport;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author contributor
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface SSDExcel {

    /**
     * 标注该属性的顺序
     *
     * @return 该属性的顺序
     */
    int order();

    /**
     * 是否是0,1
     *
     * @return
     */
    boolean trueOrFalse() default  false;
}
