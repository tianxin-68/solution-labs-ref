package com.bi.queryer.sys.interceptor;

import java.lang.annotation.*;

/**
 * 记录权限处理的日志
 * @author contributor
 */
@Documented
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface OperLog {
    String operModule() default "";
}
