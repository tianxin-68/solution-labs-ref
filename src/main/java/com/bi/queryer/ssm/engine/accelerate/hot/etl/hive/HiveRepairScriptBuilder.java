package com.bi.queryer.ssm.engine.accelerate.hot.etl.hive;

import com.bi.queryer.ssm.engine.accelerate.hot.etl.model.EtlTableDdl;
import com.bi.queryer.ssm.meta.accelerate.hot.HotTableInfo;

/**
 * @Author contributor
 * @Date 16:54 2024/9/18
 * @Description 补数脚本构建器
 **/
public class HiveRepairScriptBuilder extends HiveFullScriptBuilder {
    public HiveRepairScriptBuilder(HotTableInfo hotTable) {
        super(hotTable);
    }

    public String build(EtlTableDdl ddl){
        String script = this.buildInsertOverwrite(ddl);
        script = script + ";";
        return script;
    }

    @Override
    protected String buildInsertOverwrite(EtlTableDdl ddl) {
        return super.buildInsertOverwrite(ddl);
    }

    @Override
    public String buildWhereSql(String partitionFieldName) {
        String whereSql = String.format(" where t1.%s between '%s' and '%s' ", partitionFieldName, "${hivevar:v_date}", "${hivevar:v_date}");
        return whereSql;
    }
}
