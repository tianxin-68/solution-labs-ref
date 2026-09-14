package com.bi.queryer.ssm.engine.lod;

import cn.hutool.core.collection.CollUtil;
import com.bi.queryer.ssm.engine.QueryContext;
import com.bi.queryer.ssm.engine.QuerySqlFragments;
import com.bi.queryer.ssm.engine.analysis.AnalysisSingleModelSqlBuilder;
import com.bi.queryer.ssm.engine.config.QueryConfigure;
import com.bi.queryer.ssm.engine.config.field.QueryField;
import com.bi.queryer.ssm.engine.model.StarModel;
import com.bi.queryer.ssm.enums.AggExpressionType;
import com.bi.queryer.ssm.meta.MetaField;
import com.bi.queryer.ssm.meta.SSDMetaCacheManager;
import com.bi.queryer.util.BIConsts;
import com.bi.queryer.util.BIUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @Author contributor
 * @Date 11:44 2023-12-21
 * @Description
 **/
public class LodSingleModelSqlBuilder extends AnalysisSingleModelSqlBuilder {

    protected LodQueryConfigureItem item = null;

    public LodSingleModelSqlBuilder(QueryConfigure config, QueryContext cxt) {
        super(config, cxt);
    }

    public LodSingleModelSqlBuilder(QueryConfigure config, QueryContext cxt, LodQueryConfigureItem item){
        this(config, cxt);
        this.item = item;
    }

    @Override
    public String build(StarModel model) {
        // 若是日均值，此处因获取日均值相关sql
        String sql = super.build(model);
        item.setSql(sql);
        item.getFragments().setSelectFragments(super.buildSelectFragments(model));

        String aggSql = this.buildLodAggSql(item);

        // 再次设置其sql
        item.setSql(aggSql);
        /**不能删除不交叉的维度：因若删除导致sql第一次聚合是将丢失这些维度*/
        /**
        if(item.isAggToMainLevel()){
            // 行维度中只保留最终的聚合维度
            QueryConfigure queryConfigure = item.getConfig();
            List<QueryField> rowDimensions = queryConfigure.getResult().getRowDimensions();

            // 无效维度设置为虚拟维度
            List<QueryField> inValidDimensions = rowDimensions.stream().filter(f-> !item.getAggToMainLevelDimensions().contains(f)).collect(Collectors.toList());

            // 此处不能设置为序列字段，因会导致同环比时，对比sql中的聚合维度丢失
            //inValidDimensions.stream().forEach(f -> f.setVirtual(true));

            queryConfigure.getResult().remove(inValidDimensions, QueryArea.RowDimension);
        }
         */
        return aggSql;
    }

    public LodQueryConfigureItem getItem() {
        return item;
    }

    public void setItem(LodQueryConfigureItem item) {
        this.item = item;
    }

    /**
     * 二次聚合sql
     * @return
     */
    public String buildLodAggSql(LodQueryConfigureItem configureItem){
        if(!configureItem.isAggToMainLevel()){
            return configureItem.getSql();
        }
        StringBuilder aggSql = new StringBuilder();
        QuerySqlFragments fragments = configureItem.getFragments();
        if(fragments == null || BIUtil.isEmpty(fragments.getSelectFragments())) {
            return configureItem.getSql();
        }

        String measureId = configureItem.getLodField().getCustomFieldConfigure().getLodConfig().getMeasureId();
        MetaField metaMeasure = SSDMetaCacheManager.getField(measureId);
        if(metaMeasure == null){
            return configureItem.getSql();
        }

        List<String> aggDimensionCodes = configureItem.getAggToMainLevelDimensions().stream().map(QueryField::getCode).collect(Collectors.toList());
        List<String> dimensionCodes = configureItem.getConfig().getResult().getRowDimensions().stream().map(QueryField::getCode).collect(Collectors.toList());
        List<QueryField> lodMeasureFields = configureItem.getConfig().getResult().getMeasures();
        if(BIUtil.isEmpty(lodMeasureFields)){
            return configureItem.getSql();
        }
        QueryField lodMeasureField = lodMeasureFields.get(0);
        String viewAlias = "v_" + configureItem.getCode();

        /**select**/
        List<String> aggSelectFragments = new ArrayList<>();
        List<String> aggGroupByFragments = new ArrayList<>();
        // 添加维度
        for(String dimCode : dimensionCodes){
            if(aggDimensionCodes.contains(dimCode)) {
                aggSelectFragments.add(String.format("%s.%s as %s", viewAlias, dimCode, dimCode));
                aggGroupByFragments.add(String.format("%s.%s", viewAlias, dimCode));
            }else {
                aggSelectFragments.add(String.format("null as %s", dimCode));
            }
        }
        // 添加指标:此处指标必须用其原始指标code
        String measureCode = metaMeasure.getCode();

        //aggSelectFragments.add(String.format("%s(%s.%s) as %s", lodMeasureField.getAggExpressionType(), viewAlias, measureCode, measureCode));
        LodUIConfigure lodUIConfigure = configureItem.getLodField().getCustomFieldConfigure().getLodConfig();
        String aggExpr = BIUtil.isEmpty(lodUIConfigure.getAggExpressionType()) ? AggExpressionType.Avg.getCode() : lodUIConfigure.getAggExpressionType();

        //lod视图与主视图聚合维度一个都不匹配时，聚合计算不算汇总列
        if(CollUtil.isEmpty(aggDimensionCodes)){
            aggSelectFragments.add(String.format("%s(case when %s.%s = 0 then %s.%s else 0 end ) as %s", aggExpr,
                    viewAlias,BIConsts.GROUPING_VALUE,
                    viewAlias, measureCode, measureCode));
        }else{
            aggSelectFragments.add(String.format("%s(%s.%s) as %s", aggExpr, viewAlias, measureCode, measureCode));
        }

        // 权限不在sql层处理，在最后结果输出时做掩码，避免后续sql构建时数据类型不一致导致sql查询错误
        /*
        if(this.cxt.getAclFields().contains(measureCode)) {
            aggSelectFragments.add(String.format("%s(%s.%s) as %s", lodMeasureField.getAggExpressionType(), viewAlias, measureCode, measureCode));
        }else {
            aggSelectFragments.add(String.format("'***' as %s", measureCode));
        }
         */

        // 添加常量
        aggSelectFragments.add(String.format("max(%s.%s) as %s",
                viewAlias,
                BIConsts.GROUPING_VALUE,
                BIConsts.GROUPING_VALUE));

        aggSql.append(" select ").append(BIUtil.listToStr(aggSelectFragments));

        /**from*/
        aggSql.append(" from ").append(String.format("(%s) %s", configureItem.getSql(), viewAlias));

        /**group by*/
        if(BIUtil.isNotEmpty(aggGroupByFragments)){
            aggSql.append(" group by ").append(BIUtil.listToStr(aggGroupByFragments));
        }

        // 重置item相关信息
        configureItem.getFragments().setSelectFragments(aggSelectFragments);
        configureItem.setSql(aggSql.toString());

        return aggSql.toString();
    }
}
