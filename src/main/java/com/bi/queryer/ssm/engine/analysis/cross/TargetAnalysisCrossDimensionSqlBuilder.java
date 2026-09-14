package com.bi.queryer.ssm.engine.analysis.cross;

import com.bi.queryer.ssm.engine.QueryEngine;
import com.bi.queryer.ssm.engine.analysis.cfg.AnalysisItemConfig;
import com.bi.queryer.ssm.engine.config.field.QueryField;
import com.bi.queryer.ssm.enums.AnalysisCalcMode;
import org.apache.commons.lang3.StringUtils;

/**
 * @Author contributor
 * @Date 19:49 2024-07-24
 * @Description 分析场景的交叉表sql构建器
 **/
public class TargetAnalysisCrossDimensionSqlBuilder extends AnalysisCrossDimensionSqlBuilder {

    public TargetAnalysisCrossDimensionSqlBuilder(QueryEngine engine) {
        super(engine);
    }


    @Override
    protected String buildSelectMeasureExpression(QueryField measureField) {
        AnalysisItemConfig analysisItemConfig = measureField.getAnalysisConfig();
        if (analysisItemConfig != null && analysisItemConfig.isTargetValue()) {
            if (!AnalysisCalcMode.CONTRIBUTION_RATE.getCode().equals(analysisItemConfig.getCalcMode()) &&
                    StringUtils.isNotEmpty(analysisItemConfig.getCalcMode())) {
                return "null";
            } else {
                return tableAlias + "." + measureField.getCode();
            }
        } else {
            return super.buildSelectMeasureExpression(measureField);
        }
    }
}
