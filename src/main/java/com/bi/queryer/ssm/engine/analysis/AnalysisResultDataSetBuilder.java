package com.bi.queryer.ssm.engine.analysis;

import com.bi.queryer.ssm.engine.QueryContext;
import com.bi.queryer.ssm.engine.analysis.cfg.ctr.AnalysisContributionRateItemConfig;
import com.bi.queryer.ssm.engine.analysis.cfg.thb.AnalysisZbThbItemConfig;
import com.bi.queryer.ssm.engine.config.QueryConfigure;
import com.bi.queryer.ssm.engine.config.field.QueryField;
import com.bi.queryer.ssm.engine.result.ResultDataSet;
import com.bi.queryer.ssm.engine.result.ResultDataSetColumn;
import com.bi.queryer.ssm.engine.result.ResultDataSetTargetConfig;
import com.bi.queryer.ssm.enums.AnalysisCalcMode;
import com.bi.queryer.ssm.enums.QueryArea;
import com.bi.queryer.sys.enums.Enabled;
import com.bi.queryer.util.BIConsts;
import com.bi.queryer.util.BIUtil;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @Author contributor
 * @Date 14:46 2023-08-22
 * @Description 分析结果数据集构建器：用于构建数据集列的层级关系
 **/
public class AnalysisResultDataSetBuilder {

    protected QueryConfigure config ;

    protected ResultDataSet dataSet ;

    protected QueryContext cxt;

    protected Map<String, ResultDataSetColumn> leafColumns = new LinkedHashMap<>();

    protected Map<String, ResultDataSetColumn> allColumns = new HashMap<>();

    public AnalysisResultDataSetBuilder(QueryConfigure config, ResultDataSet dataSet, QueryContext cxt){
        this.config = config;
        this.dataSet = dataSet;
        this.cxt = cxt;
    }

    /**
     * 构建逻辑：只处理指标
     * 前置条件：用同环占比时才需要构建
     * 1、从叶子节点列中找到对应的查询字段
     * 2、查询字段若是分析字段，则找到其对应的原生字段
     * 3、按原生字段进行分组，即原生字段列的下级包括：本期、同比、环比、对比、占比等字段
     * 4、若是交叉表，则需要将上一步的原生字段列挂载到维度项列，如：列维度=业务线，则：保养->支付GMV->本期值、环比、同比、占比
     */
    public void build(){
        this.prepare();
        List<ResultDataSetColumn> rootColumns = dataSet.getColumns();
        if(BIUtil.isEmpty(rootColumns)){
            return;
        }
        List<ResultDataSetColumn> leafMeasureColumns = leafColumns.values().stream()
                                                                .filter(f -> QueryArea.Measure ==  QueryArea.get(f.getRawQueryArea()))
                                                                .collect(Collectors.toList());
        this.setTotalColumnAuth(leafMeasureColumns);


        // 指标父列：若有分析字段则处理，否则不处理
        Map<String, ResultDataSetColumn> measureParentColumns = new LinkedHashMap<>();
        for(ResultDataSetColumn measureColumn : leafMeasureColumns){
            // 此处需要判断是否是交叉表查询：交叉表取rawCode，明细表取code，因明细时分析字段rawcode
            String code = measureColumn.getRawCode();
            QueryField queryField = config.getResult().getFieldByCode(code);
            if(queryField == null){
                continue;
            }

            // 当前字段是分析字段，不处理
            if(Enabled.isTrue(queryField.getIsAnalysis())){
                continue;
            }

            // 当前字段没有派生的分析字段，不处理
//            boolean hasAnalysisField = config.getResult().getMeasures().stream()
//                    .filter(f->{
//                        AnalysisCalcMode calcMode = AnalysisCalcMode.get(f.getAnalysisConfig().getCalcMode());
//                        return Enabled.isTrue(f.getIsAnalysis())
//                                && (calcMode.isCompare() || calcMode.isZb())
//                                && f.getAnalysisConfig().getMeasureCode().equalsIgnoreCase(code);
//                    }).count() > 0;

            if(config.isConfigTotalOnly()){
                continue;
            }

            // 1、创建指标父列
            ResultDataSetColumn measureParentColumn = measureColumn.clone();
            String id = this.getParentCodePath(measureColumn) + BIConsts.ANALYSIS_PATH_SEPARATOR + measureColumn.getRawCode();
            measureParentColumn.setCode(id);
            measureParentColumn.setId(id);
            measureParentColumn.setParentCode(measureColumn.getParentCode());
            measureParentColumns.put(measureParentColumn.getCode(), measureParentColumn);

            ResultDataSetColumn parentColumn = allColumns.get(measureParentColumn.getParentCode());
            if(parentColumn != null){
                // 确保字段顺序
                int idx = parentColumn.getChildren().indexOf(measureColumn);
                parentColumn.setChild(idx, measureParentColumn);
            }else{
                // 若无父列，则在root列上删除并添加
                // 确保字段顺序
                int idx = rootColumns.indexOf(measureColumn);
                if(idx >= 0){
                    rootColumns.set(idx, measureParentColumn);
                }else {
                    rootColumns.add(measureParentColumn);
                }
            }

            // 4、将源列添加到指标父列的子级
            measureColumn.setTitle(BIConsts.ANALYSIS_CURRENT_TITLE);
            measureColumn.setRawTitle(BIConsts.ANALYSIS_CURRENT_TITLE);
            // 修改本期字段的rawCode，因父级指标字段已经有其原生字段的rawcode，避免重复
            // rawcode用于前端标识列的原生编码，目前用于关联指标白皮书的内容
            // 以上逻辑废除20240718
            measureColumn.setRawCode(measureColumn.getRawCode());
            measureParentColumn.addChild(measureColumn);

            // 5、指标父列添加到总体列列表中
            allColumns.put(measureParentColumn.getCode(), measureParentColumn);

        }

        // 分析列: 若是分析字段，则找到对应分析字段的原生字段列并归属到原生字段列的子级中
        for(ResultDataSetColumn measureColumn : leafMeasureColumns){
            String code = measureColumn.getRawCode();
            QueryField queryField = config.getResult().getFieldByCode(code);
            if(queryField == null){
                continue;
            }

            if(Enabled.isFalse(queryField.getIsAnalysis())){
                continue;
            }

            // 此处需用原始计算方式，避免行总计的同环对比指标被处理
            AnalysisCalcMode calcMode = AnalysisCalcMode.get(queryField.getAnalysisConfig().getRawCalcMode());
            if(!calcMode.isCompare() && !calcMode.isZb() && AnalysisCalcMode.CONTRIBUTION_RATE != calcMode
                    && AnalysisCalcMode.ZB_THB != calcMode && !queryField.isTargetValue()){
                continue;
            }

            // 1、将源列从上级列中删除
            ResultDataSetColumn parentColumn = allColumns.get(measureColumn.getParentCode());
            if(parentColumn != null){
                parentColumn.removeChild(measureColumn);
            }else{
                // 若无父列，则在root列上删除
                rootColumns.remove(measureColumn);
            }

            // 2、将源列添加到指标父列的子级
            String rawMeasureCode = this.getParentCodePath(measureColumn) + BIConsts.ANALYSIS_PATH_SEPARATOR + queryField.getAnalysisConfig().getMeasureCode();
            ResultDataSetColumn measureParentColumn = measureParentColumns.get(rawMeasureCode);
            if(measureParentColumn != null){
                measureParentColumn.addChild(measureColumn);
            }
        }

        System.out.println();
    }

    protected void prepare(){
        List<ResultDataSetColumn> columns = dataSet.getColumns();
        init(columns);
    }

    protected void init(List<ResultDataSetColumn> columns){
        if(BIUtil.isEmpty(columns)){
            return;
        }
        for(ResultDataSetColumn column : columns){
            // 设置计算方式和计算类型
            String code = column.getRawCode();
            QueryField queryField = config.getResult().getFieldByCode(code);
            if(queryField != null && Enabled.isTrue(queryField.getIsAnalysis())) {

                column.setCalcMode(queryField.getAnalysisConfig().getCalcMode());
                column.setCalcType(queryField.getAnalysisConfig().getCalcType());
                column.setCompareIndex(queryField.getAnalysisConfig().getCompareIndex());

                if (queryField.isTargetValue()) {
                    ResultDataSetTargetConfig itemConfig = queryField.getAnalysisConfig().getTargetConfig();
                    itemConfig.setRawMeasureCode(queryField.getAnalysisConfig().getMeasureCode());
                    if (AnalysisCalcMode.CONTRIBUTION_RATE == AnalysisCalcMode.get(queryField.getAnalysisConfig().getCalcMode())) {
                        // 避免前端混淆
                        column.setCalcMode("");
                        column.setCalcType("");
                    }
                    column.setTargetConfig(itemConfig);
                }

                if (AnalysisCalcMode.CONTRIBUTION_RATE == AnalysisCalcMode.get(queryField.getAnalysisConfig().getCalcMode())) {
                    AnalysisContributionRateItemConfig itemConfig = (AnalysisContributionRateItemConfig) queryField.getAnalysisConfig();
                    column.setCtrCalcMode(itemConfig.getCtrCalcMode());
                }

                if (AnalysisCalcMode.ZB_THB == AnalysisCalcMode.get(queryField.getAnalysisConfig().getCalcMode())) {
                    AnalysisZbThbItemConfig itemConfig = (AnalysisZbThbItemConfig) queryField.getAnalysisConfig();
                    column.getZbThbConfig().setThbCalcMode(itemConfig.getThbCalcMode());
                    column.getZbThbConfig().setZbCalcMode(itemConfig.getZbCalcMode());
                }

            }

            if(BIUtil.isEmpty(column.getChildren())){
                leafColumns.put(column.getCode(), column);
            }else {
                init(column.getChildren());
            }
            allColumns.put(column.getCode(), column);
        }
    }

    /**
     * 获取父级的编码路径
     * @param column
     * @return
     */
    protected String getParentCodePath(ResultDataSetColumn column){
        String parentCode = column.getParentCode();
        if(BIUtil.isEmpty(parentCode)){
            return parentCode;
        }
        List<String> parentCodeList = new ArrayList<>();
        while(BIUtil.isNotEmpty(parentCode)){
            parentCodeList.add(parentCode);
            ResultDataSetColumn parentColumn = allColumns.get(parentCode);
            if(parentColumn == null){
                break;
            }
            parentCode = parentColumn.getParentCode();
        }

        return BIUtil.listToStr(parentCodeList, BIConsts.ANALYSIS_PATH_SEPARATOR);
    }

    /**
     * 设置行/整表总计列权限：若行总的计算指标无权限，则行总计无权限
     */
    protected void setTotalColumnAuth(List<ResultDataSetColumn> leafMeasureColumns){
        List<ResultDataSetColumn> totalColumns = leafMeasureColumns.stream().filter(c->c.isTotal()).collect(Collectors.toList());
        if(BIUtil.isEmpty(totalColumns)){
            return;
        }

        List<String> noAuthCodes = leafMeasureColumns.stream()
                                    .filter(c->!c.isTotal())
                                    .filter(c->!c.isHasAuth()).map(ResultDataSetColumn::getRawCode).collect(Collectors.toList());
        for(ResultDataSetColumn totalColumn : totalColumns){
            // 明细表行总计：计算的源指标无权限，则无权限
            if(totalColumn.getRawCode().contains(BIConsts.ROW_TOTAL_COLUMN_CODE)){
                totalColumn.setHasAuth(BIUtil.isEmpty(noAuthCodes));
            }else {
                // 交叉表行总计：对应指标无权限，则行总计列无权限
                String totalAuthCode = totalColumn.getRawCode().split("_" + AnalysisCalcMode.ROW_TOTAL.getCode())[0];
                totalColumn.setHasAuth(!noAuthCodes.contains(totalAuthCode));
            }
        }

        Set<String> noAuthTotalCodes = totalColumns.stream().filter(f->!f.isHasAuth()).map(ResultDataSetColumn::getCode).collect(Collectors.toSet());
        if(BIUtil.isEmpty(noAuthCodes)){
            return;
        }
        // 临时处理：将无权限的行总计数据替换为掩码
        this.dataSet.getRows().parallelStream().forEach(row -> {
            for(String noAuthTotalCode : noAuthTotalCodes){
                if(row.containsKey(noAuthTotalCode)){
                    row.put(noAuthTotalCode, BIConsts.NO_AUTH_CONTENT);
                }
            }
        });
    }

    public static void main(String[] args) {
        List<String> list = new ArrayList<>();
        list.add("abc");
        list.add("bcd");
        list.add("cde");

        list.set(0, "1");
        System.out.println(list);
    }
}
