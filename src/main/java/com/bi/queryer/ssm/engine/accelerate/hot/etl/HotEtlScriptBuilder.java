package com.bi.queryer.ssm.engine.accelerate.hot.etl;

import com.bi.queryer.ssm.engine.accelerate.hot.etl.model.EtlTableDdl;
import com.bi.queryer.ssm.meta.accelerate.hot.HotTableInfo;

/**
 * @Author contributor
 * @Date 18:07 2024/8/22
 * @Description 热化etl脚本构建器
 **/
public abstract class HotEtlScriptBuilder {
    protected HotTableInfo hotTable = null;

    public HotEtlScriptBuilder(HotTableInfo hotTable) {
        this.hotTable = hotTable;
    }

    /**
     * 构建调度脚本
     * @return
     */
    abstract  public EtlTableDdl buildScheduleScript();

    /**
     * 构建补数脚本
     * @param ddl
     * @return
     */
    abstract  public EtlTableDdl buildRepairScript(EtlTableDdl ddl);
}
