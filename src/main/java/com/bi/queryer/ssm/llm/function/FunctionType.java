package com.bi.queryer.ssm.llm.function;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;

import java.util.List;

public enum FunctionType {

    UNKNOWN("", "未知"),
    TIRE_BRAND_GXD("TIRE_BRAND_GXD","轮胎_品牌_贡献度"),
    TIRE_BRAND_GXD_STRUCT("TIRE_BRAND_GXD_STRUCT","轮胎_品牌_结构效应"),
    TIRE_BRAND_GXD_TRANS("TIRE_BRAND_GXD_TRANS","轮胎_品牌_转化效应"),

    TIRE_SPEC_OPPORTUNITY_PAY_CNT("TIRE_SPEC_OPPORTUNITY_PAY_CNT","轮胎规格_机会_支付件数"),
    TIRE_BRAND_OPPORTUNITY_PAY_CNT("TIRE_BRAND_OPPORTUNITY_PAY_CNT","轮胎_品牌_机会_支付件数"),
    TIRE_BRAND_OPPORTUNITY_PAY_CNT_GXZ("TIRE_BRAND_OPPORTUNITY_PAY_CNT_GXZ","轮胎_品牌_机会_支付件数_贡献值"),

    TIRE_PID_GXD("TIRE_PID_GXD","轮胎_商品_贡献度"),
    TIRE_PID_GXD_STRUCT("TIRE_PID_GXD_STRUCT","轮胎_商品_结构效应"),
    TIRE_PID_GXD_TRANS("TIRE_PID_GXD_TRANS","轮胎_商品_转化效应"),

    TIRE_PID_OPPORTUNITY_PAY_CNT("TIRE_PID_OPPORTUNITY_PAY_CNT","轮胎_商品_机会_支付件数"),
    TIRE_PID_OPPORTUNITY_PAY_CNT_GXZ("TIRE_PID_OPPORTUNITY_PAY_CNT_GXZ","轮胎_商品_机会_支付件数_贡献值"),

    TIRE_PNAME_GXD("TIRE_PNAME_GXD","轮胎_商品_贡献度"),
    TIRE_PNAME_GXD_STRUCT("TIRE_PNAME_GXD_STRUCT","轮胎_商品_结构效应"),
    TIRE_PNAME_GXD_TRANS("TIRE_PNAME_GXD_TRANS","轮胎_商品_转化效应"),

    TIRE_PNAME_OPPORTUNITY_PAY_CNT("TIRE_PNAME_OPPORTUNITY_PAY_CNT","轮胎_商品_机会_支付件数"),
    TIRE_PNAME_OPPORTUNITY_PAY_CNT_GXZ("TIRE_PNAME_OPPORTUNITY_PAY_CNT_GXZ","轮胎_商品_机会_支付件数_贡献值");


    FunctionType(String code, String desc){
        this.code = code;
        this.desc = desc;
    }

    private String code;

    private String desc;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public static FunctionType getByCodeList(List<String> codeList) {

        if (CollUtil.isEmpty(codeList)) {
            return UNKNOWN;
        }

        for (FunctionType type : FunctionType.values()) {

            boolean isExist = codeList.stream().filter(n -> type.getCode().equalsIgnoreCase(n)).findAny().isPresent();
            if (isExist) {
                return type;
            }
        }

        return UNKNOWN;
    }

    public static FunctionType getByCode(String code) {

        if (StrUtil.isEmpty(code)) {
            return UNKNOWN;
        }

        for (FunctionType type : FunctionType.values()) {
            boolean isExist = type.getCode().equalsIgnoreCase(code);
            if (isExist) {
                return type;
            }
        }

        return UNKNOWN;
    }

}
