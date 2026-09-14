package com.bi.queryer.ssm.engine.function;

import com.bi.queryer.ssm.engine.function.impl.*;
import com.bi.queryer.ssm.util.SSDUtil;
import com.bi.queryer.sys.db.DBType;
import com.bi.queryer.sys.db.DBUtil;

/**
 * 函数管理类：用于动态获取函数接口
 * @author contributor
 *
 */
public class FunctionManager {

	public static IFunction getFunction(){
		DBType dbType = DBUtil.getDBType();
		return getFunction(dbType);
	}

	public static IFunction getFunction(DBType dbType){
		IFunction function = null;
		switch(dbType){
			case Presto:
				function = new PrestoFunction();
				break;
			case SQLServer:
				function = new SQLServerFunction();
				break;
			case MySQL:
				function = new MySQLFunction();
				break;
			case Doris:
				function = new DorisFunction();
				break;
			case Trino:
				default:
				function = new TrinoFunction();
				break;
		}

		return function;
	}
}
