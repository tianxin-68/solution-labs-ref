package com.bi.queryer.ssm.engine.view;

import cn.hutool.core.util.StrUtil;
import com.bi.queryer.ssm.engine.QueryContext;
import com.bi.queryer.ssm.engine.config.QueryConfigure;
import com.bi.queryer.ssm.engine.model.StarModel;
import com.bi.queryer.ssm.engine.view.impl.*;
import com.bi.queryer.ssm.enums.DataSourceViewMeasureType;
import com.bi.queryer.ssm.meta.MetaField;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @Author contributor
 * @Date 16:13 2025/3/17
 * @Description TODO
 **/
public abstract class DataSourceViewBuilderFactory {
    public static IDataSourceViewBuilder create(StarModel model, QueryConfigure config, QueryContext cxt) {

        IDataSourceViewBuilder builder = null;

        //含有白皮书编码的字段
        List<String> containKpiNoFieldNames = model.getFactTable().getMeta().getFields()
                .stream().filter(f -> StrUtil.isNotEmpty(f.getKpiNo()))
                .map(MetaField::getName)
                .collect(Collectors.toList());

        List<String> nameList = model.getFields()
                .stream()
                .filter(f -> !f.isVirtual() && containKpiNoFieldNames.contains(f.getName()))
                .map(f -> f.getName()).collect(Collectors.toList());

        DataSourceViewMeasureType type = DataSourceViewMeasureType.getByName(nameList);
        String analysisBusinessLine = type.getAnalysisBusinessLine();
        switch (type) {
            case AVG_CAR_YEAR:
                builder = new CarYearDataSourceViewBuilder(model, config, cxt);
                break;
            case AVG_CAR_PRICE:
                builder = new CarPriceDataSourceViewBuilder(model, config, cxt);
                break;
            case AVG_ITEM_INDEX:
                builder = new ItemIndexDataSourceViewBuilder(model, config, cxt);
                break;
            case BATTERY_AVG_ITEM_INDEX:
                builder = new BatteryAvgItemIndexDataSourceViewBuilder(model, config, cxt);
                break;
            case TIRE_PAY_AVG_CAR_YEAR:
                builder = new TirePayAvgCarYearDataSourceViewBuilder(model, config, cxt);
                break;
            case MAINTENANCE_KEYPAGE_LISTING_AVG_CAR_YEAR:
            case MAINTENANCE_OIL_KEYPAGE_LISTING_AVG_CAR_YEAR:
            case MAINTENANCE_PARTS_KEYPAGE_LISTING_AVG_CAR_YEAR:
                builder = new MaintenanceCarYearDataSourceViewBuilder(model, config, cxt, analysisBusinessLine);
                break;
            case MAINTENANCE_KEYPAGE_LISTING_AVG_CAR_PRICE:
            case MAINTENANCE_OIL_KEYPAGE_LISTING_AVG_CAR_PRICE:
            case MAINTENANCE_PARTS_KEYPAGE_LISTING_AVG_CAR_PRICE:
                builder = new MaintenanceCarPriceDataSourceViewBuilder(model, config, cxt, analysisBusinessLine);
                break;
            case MAINTENANCE_PAY_AVG_CAR_YEAR:
            case MAINTENANCE_OIL_PAY_AVG_CAR_YEAR:
            case MAINTENANCE_PARTS_PAY_AVG_CAR_YEAR:
                builder = new MaintenancePayAvgCarYearDataSourceViewBuilder(model, config, cxt, analysisBusinessLine);
                break;
            case MAINTENANCE_PAY_AVG_CAR_PRICE:
            case MAINTENANCE_OIL_PAY_AVG_CAR_PRICE:
            case MAINTENANCE_PARTS_PAY_AVG_CAR_PRICE:
                builder = new MaintenancePayAvgCarPriceDatasourceViewBuilder(model, config, cxt, analysisBusinessLine);
                break;
            case MAINTENANCE_FINISH_ORDER_AVG_CAR_YEAR:
            case MAINTENANCE_OIL_FINISH_ORDER_AVG_CAR_YEAR:
            case MAINTENANCE_PARTS_FINISH_ORDER_AVG_CAR_YEAR:
                builder = new MaintenanceFinishOrderAvgCarYearDatasourceViewBuilder(model, config, cxt, analysisBusinessLine);
                break;
            case MAINTENANCE_FINISH_ORDER_AVG_CAR_PRICE:
            case MAINTENANCE_OIL_FINISH_ORDER_AVG_CAR_PRICE:
            case MAINTENANCE_PARTS_FINISH_ORDER_AVG_CAR_PRICE:
                builder = new MaintenanceFinishOrderAvgCarPriceDatasourceViewBuilder(model, config, cxt, analysisBusinessLine);
                break;
        }

        return builder;
    }
}
