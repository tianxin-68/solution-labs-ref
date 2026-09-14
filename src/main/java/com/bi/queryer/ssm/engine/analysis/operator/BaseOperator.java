package com.bi.queryer.ssm.engine.analysis.operator;

import com.bi.queryer.ssm.engine.QueryContext;
import com.bi.queryer.ssm.engine.analysis.cfg.AnalysisItemConfig;
import com.bi.queryer.ssm.engine.config.QueryConfigure;
import com.bi.queryer.ssm.engine.config.field.QueryField;
import com.bi.queryer.ssm.engine.function.FunctionManager;
import com.bi.queryer.ssm.engine.function.IFunction;
import com.bi.queryer.ssm.engine.model.StarModel;
import com.bi.queryer.ssm.enums.QueryArea;
import com.bi.queryer.util.BIUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @Author contributor
 * @Date 17:13 2023-07-27
 * @Description 分析算子
 **/
public class BaseOperator {

    protected IFunction function = null;

    public BaseOperator(){
        this.function = FunctionManager.getFunction();
    }

    public String calc(OperatorContext cxt, QueryField measureField){
        return "null";
    }

    public String getCalcExpression(OperatorContext cxt,String measureValue,String tableAlias, String groupKey,String ratioUnit){
        return "null";
    }

    public String lodCalc(OperatorContext cxt,String measureName,String ratioUnit) {
        return "null";
    }

    /**
     * 预计算
     * @return
     */
    public String preCalc(QueryConfigure config, QueryField measureField, Map<String, String> measureExpressions, List<StarModel> models){
        return "null";
    }

    /**
     * 维度分类汇总
     * @param config
     * @param model
     * @return
     */
    public List<String> groupingSets(QueryConfigure config, StarModel model){
        return new ArrayList<>();
    }

    /**
     * 初始化查询配置：根据不同的分析算子，添加相关的查询所需的配置信息
     */
    public void initQueryConfig(QueryConfigure config, QueryContext cxt){

    }

    /**
     * 是否支持转置
     * @return
     */
    public boolean isSupportPivot(AnalysisItemConfig itemConfig){
        return false;
    }

    /**
     * 总计转置
     * @return
     */
    public String pivot(QueryConfigure config, QueryField measure){
        return String.format("null");
    }

    /**
     * 获取分组标识
     * @param config
     * @return
     */
    public List<Integer> getGroupingValues(QueryConfigure config){
        return new ArrayList<>();
    }

    /**
     * 获取转置字段名
     * @param config
     * @param measure
     * @return
     */
    public String getPivotFieldName(QueryConfigure config, QueryField measure){
        return measure.getCode();
    }

    /**
     * 是否是交叉表查询
     * @param config
     * @return
     */
    protected boolean isCrossQuery(QueryConfigure config){
        List<QueryField> colFields = this.getColumnDimensions(config);
        return BIUtil.isNotEmpty(colFields);
    }

    /**
     *  列维度字段编码列表：注意，此处必须通过字段所属原始查询区域获取列维度字段，因此处列维度已被转为行维度
     * @param config
     * @return
     */
    protected List<QueryField> getColumnDimensions(QueryConfigure config){
        List<QueryField> colDimFields = config.getResult().getFields().stream().filter(f->f.getRawQueryArea() == QueryArea.ColumnDimension).collect(Collectors.toList());
        // 去掉附加字段，场景：自定义维度放在列上时
        colDimFields = colDimFields.stream().filter(f -> !f.isAppend()).collect(Collectors.toList());
        return colDimFields;
    }

    protected List<QueryField> getRowDimensions(QueryConfigure config){
        List<QueryField> rowFields = config.getResult().getRowDimensions().stream().collect(Collectors.toList());
        return rowFields;
    }

}
