package com.bi.queryer.ssm.engine.accelerate.hot.etl.doris;

import com.bi.queryer.ssm.engine.accelerate.hot.etl.model.EtlTableDdl;
import com.bi.queryer.ssm.meta.accelerate.hot.HotTableInfo;
import com.bi.queryer.util.BIUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author contributor
 * @Date 17:49 2024/9/18
 * @Description TODO
 **/
public class DorisRepairScriptBuilder extends DorisIncrementalScriptBuilder{
    public DorisRepairScriptBuilder(HotTableInfo hotTable) {
        super(hotTable);
    }

    public String build(EtlTableDdl ddl){

        // 先删除分区
        List<String> fragments = new ArrayList<>();

        fragments.add(String.format("use %s; \n", hotTableSchema));

        String partitionFieldName = ddl.getPartitionField().getName();
        String deleteSql = "\n-- ==========前置sql处理：删除分区数据==========\n";
        deleteSql += String.format(" delete from %s where %s between '%s' and '%s'"
                , hotTableShortName
                , partitionFieldName
                ,"${v_date}"
                ,"${v_date}"
        );

        fragments.add(deleteSql + " ; ");

        fragments.add(" \n--splitflag=sql \n");

        fragments.add("\n-- ==========加载热表数据==========\n");

        String loadScript = this.buildBulkLoadScript(ddl);
        fragments.add(loadScript + ";");

        fragments.add(String.format(" \n--splitflag=load,%s,%s \n", hotTableSchema, hotTableShortName));

        String script = BIUtil.listToStr(fragments, "");
        return script;
    }

    @Override
    public String buildWhereSql(String partitionFieldName) {
        String whereSql = String.format(" where %s between '%s' and '%s' "
                                            , partitionFieldName
                                            ,"${v_date}"
                                            ,"${v_date}");
        return whereSql;
    }
}
