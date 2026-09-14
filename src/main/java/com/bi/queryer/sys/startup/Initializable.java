package com.bi.queryer.sys.startup;

import java.util.Map;

import com.bi.queryer.sys.exception.BIException;

/**
 * 初始化接口
 * @author contributor
 *
 */
public interface Initializable {
	public void initialize(Map<String, ?> initParams) throws BIException;
}
