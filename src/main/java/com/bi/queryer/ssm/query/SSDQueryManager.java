package com.bi.queryer.ssm.query;

import com.bi.queryer.ssm.engine.QueryEngine;

/**
 * 查询管理
 */

public class SSDQueryManager {

	private static final ThreadLocal<QueryEngine> queryEngine = new ThreadLocal<>();

	public static void setQueryEngine(QueryEngine engine){
		queryEngine.set(engine);
	}

	public static QueryEngine getQueryEngine(){
		return queryEngine.get();
	}

	public static void remove() {
		queryEngine.remove();
	}

}
