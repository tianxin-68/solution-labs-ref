package com.bi.queryer.sys.interceptor;

import java.lang.annotation.*;

/**
 * 数据权限同步
 * @author contributor
 */
@Documented
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface FlushUserAuthorityCache {


}
