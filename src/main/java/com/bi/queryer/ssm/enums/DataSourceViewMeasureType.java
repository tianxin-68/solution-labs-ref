package com.bi.queryer.ssm.enums;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public enum DataSourceViewMeasureType {

    UNKNOWN("", "", "未知", "", "", null),

    AVG_CAR_YEAR("avg_dim_car_year", "D_TFC_01454", "轮胎-关键页曝光平均车龄",
            "bi_olap.ads_tfc_conv_platform_goods_sum_di",
            "keypage_prd_listing_deviceid,dim_tid,dim_car_year,dim_analysis_business", null),

    AVG_CAR_PRICE("avg_guide_price", "D_TFC_01455", "轮胎-关键页曝光平均车价",
            "bi_olap.ads_tfc_conv_platform_goods_sum_di",
            "keypage_prd_listing_deviceid,dim_tid,guide_price,dim_analysis_business", null),

    AVG_ITEM_INDEX("avg_item_index", "D_TFC_01456", "轮胎-列表商品曝光平均坑位",
            "bi_olap.ads_tfc_conv_platform_goods_sum_di",
            "item_index,prd_listing_deviceid,dim_url,dim_analysis_business", null),

    BATTERY_AVG_ITEM_INDEX("battery_avg_item_index", "D_TFC_01763", "蓄电池-列表商品曝光平均坑位",
            "bi_olap.ads_tfc_conv_platform_goods_sum_di",
            "item_index,prd_listing_deviceid,dim_url,dim_analysis_business", null),

    TIRE_PAY_AVG_CAR_YEAR("tire_pay_avg_car_year", "D_ORD_06005", "轮胎-支付平均车龄",
            "bi_olap.ads_ord_order_detail_dtl_di",
            "pay_userid,dim_tid,dim_car_year,dim_analysis_business", null),

    MAINTENANCE_KEYPAGE_LISTING_AVG_CAR_YEAR("maintenance_keypage_listing_avg_car_year", "D_TFC_01592", "保养-关键页曝光平均车龄",
            "bi_olap.ads_tfc_conv_platform_goods_sum_di",
            "keypage_prd_listing_deviceid,dim_tid,dim_car_year,dim_analysis_business", "保养,保养油液,保养配件"),

    MAINTENANCE_OIL_KEYPAGE_LISTING_AVG_CAR_YEAR("maintenance_oil_keypage_listing_avg_car_year", "D_TFC_02086", "保养油液-关键页曝光平均车龄",
            "bi_olap.ads_tfc_conv_platform_goods_sum_di",
            "keypage_prd_listing_deviceid,dim_tid,dim_car_year,dim_analysis_business", "保养油液"),

    MAINTENANCE_PARTS_KEYPAGE_LISTING_AVG_CAR_YEAR("maintenance_parts_keypage_listing_avg_car_year", "D_TFC_02087", "保养配件-关键页曝光平均车龄",
            "bi_olap.ads_tfc_conv_platform_goods_sum_di",
            "keypage_prd_listing_deviceid,dim_tid,dim_car_year,dim_analysis_business", "保养配件"),

    MAINTENANCE_KEYPAGE_LISTING_AVG_CAR_PRICE("maintenance_keypage_listing_avg_car_price", "D_TFC_01593", "保养-关键页曝光平均车价",
            "bi_olap.ads_tfc_conv_platform_goods_sum_di",
            "keypage_prd_listing_deviceid,dim_tid,guide_price,dim_analysis_business", "保养,保养油液,保养配件"),

    MAINTENANCE_OIL_KEYPAGE_LISTING_AVG_CAR_PRICE("maintenance_oil_keypage_listing_avg_car_price", "D_TFC_02090", "保养油液-关键页曝光平均车价",
            "bi_olap.ads_tfc_conv_platform_goods_sum_di",
            "keypage_prd_listing_deviceid,dim_tid,guide_price,dim_analysis_business", "保养油液"),

    MAINTENANCE_PARTS_KEYPAGE_LISTING_AVG_CAR_PRICE("maintenance_parts_keypage_listing_avg_car_price", "D_TFC_02091", "保养配件-关键页曝光平均车价",
            "bi_olap.ads_tfc_conv_platform_goods_sum_di",
            "keypage_prd_listing_deviceid,dim_tid,guide_price,dim_analysis_business", "保养配件"),

    MAINTENANCE_PAY_AVG_CAR_YEAR("maintenance_pay_avg_car_year", "D_ORD_06462", "保养-支付平均车龄",
            "bi_olap.ads_ord_order_detail_dtl_di",
            "pay_userid,dim_tid,dim_car_year,dim_analysis_business", "保养,保养油液,保养配件"),

    MAINTENANCE_OIL_PAY_AVG_CAR_YEAR("maintenance_oil_pay_avg_car_year", "D_ORD_08871", "保养油液-支付平均车龄",
            "bi_olap.ads_ord_order_detail_dtl_di",
            "pay_userid,dim_tid,dim_car_year,dim_analysis_business", "保养油液"),

    MAINTENANCE_PARTS_PAY_AVG_CAR_YEAR("maintenance_parts_pay_avg_car_year", "D_ORD_08872", "保养配件-支付平均车龄",
            "bi_olap.ads_ord_order_detail_dtl_di",
            "pay_userid,dim_tid,dim_car_year,dim_analysis_business", "保养配件"),

    MAINTENANCE_PAY_AVG_CAR_PRICE("maintenance_pay_avg_car_price", "D_ORD_06464", "保养-支付平均车价",
            "bi_olap.ads_ord_order_detail_dtl_di",
            "pay_userid,dim_tid,tid_guide_price,dim_analysis_business", "保养,保养油液,保养配件"),

    MAINTENANCE_OIL_PAY_AVG_CAR_PRICE("maintenance_oil_pay_avg_car_price", "D_ORD_08875", "保养油液-支付平均车价",
            "bi_olap.ads_ord_order_detail_dtl_di",
            "pay_userid,dim_tid,tid_guide_price,dim_analysis_business", "保养油液"),

    MAINTENANCE_PARTS_PAY_AVG_CAR_PRICE("maintenance_parts_pay_avg_car_price", "D_ORD_08876", "保养配件-支付平均车价",
            "bi_olap.ads_ord_order_detail_dtl_di",
            "pay_userid,dim_tid,tid_guide_price,dim_analysis_business", "保养配件"),

    MAINTENANCE_FINISH_ORDER_AVG_CAR_YEAR("maintenance_finish_order_avg_car_year", "D_ORD_06461", "保养-履约平均车龄",
            "bi_olap.ads_ord_order_detail_dtl_di",
            "finish_order_user,dim_tid,dim_car_year,dim_analysis_business", "保养,保养油液,保养配件"),

    MAINTENANCE_OIL_FINISH_ORDER_AVG_CAR_YEAR("maintenance_oil_finish_order_avg_car_year", "D_ORD_08869", "保养油液-履约平均车龄",
            "bi_olap.ads_ord_order_detail_dtl_di",
            "finish_order_user,dim_tid,dim_car_year,dim_analysis_business", "保养油液"),

    MAINTENANCE_PARTS_FINISH_ORDER_AVG_CAR_YEAR("maintenance_parts_finish_order_avg_car_year", "D_ORD_08870", "保养配件-履约平均车龄",
            "bi_olap.ads_ord_order_detail_dtl_di",
            "finish_order_user,dim_tid,dim_car_year,dim_analysis_business", "保养配件"),

    MAINTENANCE_FINISH_ORDER_AVG_CAR_PRICE("maintenance_finish_order_avg_car_price", "D_TFC_01593", "保养-履约平均车价",
            "bi_olap.ads_ord_order_detail_dtl_di",
            "finish_order_user,dim_tid,tid_guide_price,dim_analysis_business", "保养,保养油液,保养配件"),

    MAINTENANCE_OIL_FINISH_ORDER_AVG_CAR_PRICE("maintenance_oil_finish_order_avg_car_price", "D_ORD_08873", "保养油液-履约平均车价",
            "bi_olap.ads_ord_order_detail_dtl_di",
            "finish_order_user,dim_tid,tid_guide_price,dim_analysis_business", "保养油液"),

    MAINTENANCE_PARTS_FINISH_ORDER_AVG_CAR_PRICE("maintenance_parts_finish_order_avg_car_price", "D_ORD_08874", "保养配件-履约平均车价",
            "bi_olap.ads_ord_order_detail_dtl_di",
            "finish_order_user,dim_tid,tid_guide_price,dim_analysis_business", "保养配件");

    DataSourceViewMeasureType(String name, String code, String desc, String tableNames,
                              String additionalFieldNames, String analysisBusinessLine) {
        this.name = name;
        this.code = code;
        this.desc = desc;
        this.tableNames = tableNames;
        this.additionalFieldNames = additionalFieldNames;
        this.analysisBusinessLine = analysisBusinessLine;
    }

    private String name;

    private String code;

    private String desc;

    private String tableNames;

    private String additionalFieldNames;

    /** 保养类指标对应的 dim_analysis_business 过滤值；非保养类为 null */
    private String analysisBusinessLine;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

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

    public String getTableNames() {
        return tableNames;
    }

    public void setTableNames(String tableNames) {
        this.tableNames = tableNames;
    }

    public String getAdditionalFieldNames() {
        return additionalFieldNames;
    }

    public void setAdditionalFieldNames(String additionalFieldNames) {
        this.additionalFieldNames = additionalFieldNames;
    }

    public String getAnalysisBusinessLine() {
        return analysisBusinessLine;
    }

    public static DataSourceViewMeasureType getByName(List<String> nameList) {

        if (CollUtil.isEmpty(nameList)) {
            return UNKNOWN;
        }

        for (DataSourceViewMeasureType type : DataSourceViewMeasureType.values()) {
            boolean isExist = nameList.stream().filter(n -> type.getName().equals(n)).findAny().isPresent();
            if (isExist) {
                return type;
            }
        }

        return UNKNOWN;
    }

    public static List<String> getTableNameList() {

        List<String> tableNames = new ArrayList<>();
        for (DataSourceViewMeasureType type : DataSourceViewMeasureType.values()) {
            if (DataSourceViewMeasureType.UNKNOWN == type) {
                continue;
            }
            if (StrUtil.isNotEmpty(type.getTableNames())) {

                String[] tableNameArray = type.getTableNames().split(",");
                for (String tableName : tableNameArray) {

                    if (StrUtil.isEmpty(tableName)) {
                        continue;
                    }

                    tableNames.add(tableName);
                }
            }
        }

        if (CollUtil.isNotEmpty(tableNames)) {
            tableNames = tableNames.stream().distinct().collect(Collectors.toList());
        }

        return tableNames;
    }

}
