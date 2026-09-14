package com.bi.queryer.ssm.engine.accelerate.hot.etl.hive;

import com.bi.queryer.ssm.engine.accelerate.hot.enums.DataUpdateMode;
import com.bi.queryer.ssm.engine.accelerate.hot.etl.HotEtlManager;
import com.bi.queryer.ssm.engine.accelerate.hot.etl.HotEtlScriptBuilder;
import com.bi.queryer.ssm.engine.accelerate.hot.etl.HotEtlType;
import com.bi.queryer.ssm.meta.accelerate.hot.HotTableInfo;

/**
 * @Author contributor
 * @Date 16:50 2024/8/19
 * @Description 处理trino到hive相关逻辑
 **/
public class Trino2HiveManager extends HotEtlManager {
    @Override
    public HotEtlType getType() {
        return HotEtlType.Hive;
    }

    @Override
    public HotEtlScriptBuilder createScriptBuilder(HotTableInfo hotTable) {

        DataUpdateMode dataUpdateMode = DataUpdateMode.get(hotTable.getHotTableSource().getDataUpdateMode());
        if (DataUpdateMode.INCREMENTAL == dataUpdateMode) {
            return new HiveIncrementalScriptBuilder(hotTable);
        }
        return new HiveFullScriptBuilder(hotTable);
    }
}
