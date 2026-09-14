package com.bi.queryer.util;

import java.util.UUID;

/**
 * id生成器
 * @author contributor
 *
 */
public class Guid {
	public static String id(){
		String str = UUID.randomUUID().toString();
		str = str.replaceAll("\\-", "");
		return str;
	}
}
