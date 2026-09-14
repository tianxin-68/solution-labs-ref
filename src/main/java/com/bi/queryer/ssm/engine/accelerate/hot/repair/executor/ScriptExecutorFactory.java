package com.bi.queryer.ssm.engine.accelerate.hot.repair.executor;

import com.bi.queryer.sys.db.DBType;

/**
 * @Author contributor
 * @Date 19:16 2024/10/14
 * @Description TODO
 **/
public abstract class ScriptExecutorFactory {
    public static BaseScriptExecutor createScriptExecutor(String scriptType) {
        DBType type = DBType.getType(scriptType);
        BaseScriptExecutor executor = null;
        if(type == DBType.Hive){
            executor = new HiveScriptExecutor();
        }
        if(type == DBType.Doris){
            executor = new DorisScriptExecutor();
        }
        return executor;
    }
}
