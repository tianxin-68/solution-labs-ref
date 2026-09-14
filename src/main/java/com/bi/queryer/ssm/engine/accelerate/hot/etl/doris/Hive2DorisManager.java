package com.bi.queryer.ssm.engine.accelerate.hot.etl.doris;

import com.bi.queryer.ssm.engine.accelerate.hot.enums.DataUpdateMode;
import com.bi.queryer.ssm.engine.accelerate.hot.etl.HotEtlManager;
import com.bi.queryer.ssm.engine.accelerate.hot.etl.HotEtlScriptBuilder;
import com.bi.queryer.ssm.engine.accelerate.hot.etl.HotEtlType;
import com.bi.queryer.ssm.meta.accelerate.hot.HotTableInfo;

/**
 * @Author contributor
 * @Date 16:51 2024/8/19
 * @Description 处理hive同步到doris相关逻辑
 **/
public class Hive2DorisManager  extends HotEtlManager{
    @Override
    public HotEtlType getType() {
        return HotEtlType.Doris;
    }

    @Override
    public HotEtlScriptBuilder createScriptBuilder(HotTableInfo hotTable) {

        DataUpdateMode dataUpdateMode = DataUpdateMode.get(hotTable.getHotTableSource().getDataUpdateMode());
        if (DataUpdateMode.INCREMENTAL == dataUpdateMode) {
            return new DorisIncrementalScriptBuilder(hotTable);
        }
        return new DorisFullScriptBuilder(hotTable);
    }
}
