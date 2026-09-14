package com.bi.queryer.ssm.llm.function;

import com.bi.queryer.ssm.llm.entity.LLMQueryConfig;
import com.bi.queryer.ssm.llm.entity.LLMQueryField;
import com.bi.queryer.ssm.llm.function.impl.*;

import java.util.List;
import java.util.stream.Collectors;

public class FunctionFactory {

    public static IFunction get(LLMQueryConfig config) {

        IFunction function = new BaseFunction(config);

        List<LLMQueryField> metrics = config.getResult().getMetrics();
        List<String> codeList = metrics
                .stream()
                .map(f -> f.getField_code()).collect(Collectors.toList());

        FunctionType type = FunctionType.getByCodeList(codeList);
        switch (type) {
            case TIRE_BRAND_GXD:
            case TIRE_BRAND_GXD_STRUCT:
            case TIRE_BRAND_GXD_TRANS:
                function = new TireBrandGxdFunction(config);
                break;
            case TIRE_SPEC_OPPORTUNITY_PAY_CNT:
                function = new TireSpecOpportunityPayCntFunction(config);
                break;
            case TIRE_BRAND_OPPORTUNITY_PAY_CNT:
            case TIRE_BRAND_OPPORTUNITY_PAY_CNT_GXZ:
                function = new TireBrandOpportunityPayCntGxzFunction(config);
                break;

            case TIRE_PID_GXD:
            case TIRE_PID_GXD_STRUCT:
            case TIRE_PID_GXD_TRANS:
                function = new TirePidGxdFunction(config);
                break;
            case TIRE_PID_OPPORTUNITY_PAY_CNT:
            case TIRE_PID_OPPORTUNITY_PAY_CNT_GXZ:
                function = new TirePidOpportunityPayCntGxzFunction(config);
                break;

            case TIRE_PNAME_GXD:
            case TIRE_PNAME_GXD_STRUCT:
            case TIRE_PNAME_GXD_TRANS:
                function = new TirePNameGxdFunction(config);
                break;

            case TIRE_PNAME_OPPORTUNITY_PAY_CNT:
            case TIRE_PNAME_OPPORTUNITY_PAY_CNT_GXZ:
                function = new TirePNameOpportunityPayCntGxzFunction(config);
                break;

        }

        return function;
    }

}
