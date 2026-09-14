package com.bi.queryer.ssm.engine.aggregation;

import com.bi.queryer.ssm.engine.config.field.QueryField;
import com.bi.queryer.ssm.engine.function.FunctionManager;
import com.bi.queryer.ssm.engine.function.IFunction;
import com.bi.queryer.ssm.engine.model.QueryTable;
import com.bi.queryer.ssm.enums.AggExpressionType;
import com.bi.queryer.ssm.meta.MetaField;
import com.bi.queryer.ssm.meta.MetaTable;
import com.bi.queryer.ssm.meta.SSDMetaCacheManager;
import com.bi.queryer.sys.config.SC;
import com.bi.queryer.util.BIUtil;
import com.bi.queryer.util.StringUtil;

import java.util.Set;
import java.util.Stack;

/**
 * @Author contributor
 * @Date 14:21 2023-09-22
 * @Description 默认聚合器：用于生成指标的聚合表达式
 **/
public class DefaultAggregator {

    protected IFunction fx = FunctionManager.getFunction();

    protected AggregatorContext cxt = null;

    public DefaultAggregator(AggregatorContext cxt){
        this.cxt = cxt;
    }

    public String aggregate(QueryField field){
      //  this.cxt = cxt;
        if(field == null){
            return "";
        }
        MetaField meta = field.getMeta();
        if(!field.isMeasure()) {
            return "";
        }

        String finalExpression = "";

        if(field.isCalc()){
            // 计算字段，需对其计算表达式解析处理
            finalExpression = this.convertCalcExpression(field);
        }else{
            String fieldFullName = field.getTable().getAlias() + "." + field.getCode();
            String aggExpression = meta.getAggExpression();
            if (StringUtil.isEmpty(aggExpression)) {
                //throw new BIException(meta.getTitle() + "[" + meta.getName() + "]未配置聚合函数");
                finalExpression = "null";
            }else {
                finalExpression = AggExpressionType.get(aggExpression).getExpression(fieldFullName);
            }
            /*
            if (AggExpressionType.Count_Distinct == AggExpressionType.get(aggExpression)) {
                finalExpression = aggExpression + "(distinct " + fx.hash(fieldFullName) + ")";
            } else {
                finalExpression = aggExpression + "(" + fieldFullName + ")";
            }
             */
        }
        return finalExpression;
    }

    /**
     * 转换计算表达式:将计算表达式解析为可执行的sql计算表达式
     *
     * @return
     */
    protected String convertCalcExpression(QueryField calcField) {
        String expression = calcField.getMeta().getAggExpression();
        if (!calcField.isCalc()) {
            return expression;
        }
        Set<QueryField> atomFields = calcField.getCalcAtomFields();
        if (atomFields.isEmpty()) {
            return expression;
        }

        // 提取count distinct添加hash算法，提升查询性能
        try {
            expression = wrapHashExpression(expression);
        }catch (Exception e){
            e.printStackTrace();
            expression = calcField.getMeta().getAggExpression();
        }

        for (QueryField atomField : atomFields) {
            if (!StringUtil.isEmpty(expression) && expression.indexOf("[") != -1) {
                QueryTable atomTable = atomField.getTable() == null ? calcField.getTable() : atomField.getTable();
                String replacement = atomField.getCode();

                // 表内表达式
                String atomFieldFullName = atomTable.getAlias() + "." + atomField.getCode();
                expression = expression.replaceAll("\\[" + replacement + "\\]", atomFieldFullName);

                // 跨表表达式，示例：bi_test.ads_t1.biz_line
                MetaTable metaTable = SSDMetaCacheManager.getTable(atomField.getMeta().getTableId());
                if (metaTable != null) {
                    replacement = metaTable.getFullName(true) + "." + atomField.getMeta().getName();
                }
                expression = expression.replaceAll("\\[" + replacement + "\\]", atomFieldFullName);

                //兼容表达式使用字段id的场景
                replacement = atomField.getId();
                expression = expression.replaceAll("\\[" + replacement + "\\]", atomFieldFullName);
            }
        }
        expression = " " + fx.tryCatch(expression) + " ";
        return expression;
    }

    protected String wrapHashExpression(String expression){
        boolean enable = "true".equalsIgnoreCase(SC.v("distinct.hash.enable", "false"));
        if(!enable){
            return expression;
        }
        String[] countDistinctFlags = new String[]{"count(distinct", "COUNT(distinct", "count(DISTINCT", "COUNT(DISTINCT"};
        String newExpression = expression;
        for(String cdf : countDistinctFlags){
            newExpression = wrapHashExpression(newExpression, cdf);
        }
        return newExpression;
    }

    protected String wrapHashExpression(String expression, String countDistinctFlag){
        if(BIUtil.isEmpty(expression)){
            return expression;
        }
       // String countDistinctFlag = "count(distinct";
        int matchCount = StringUtil.countMatches(expression, countDistinctFlag);
        if(matchCount == 0){
            return expression;
        }

        String newExpression = expression;

        // 找到从flag开始后的第一个非对称的)所在的索引
        // 算法：若找到(入栈，若找到)且栈不为空则出栈，否则就获取)所在的索引
        for(int cnt = 0; cnt < matchCount; cnt++) {
            int foundIndex = -1;

            int startIndex = expression.indexOf(countDistinctFlag, foundIndex + 1) + countDistinctFlag.length();
            Stack<Integer> bracketStack = new Stack<>();
            for (int i = startIndex; i < expression.length(); i++) {
                Character c = expression.charAt(i);
                if (c == '(') {
                    bracketStack.push(i);
                    continue;
                }
                if (c == ')' && !bracketStack.isEmpty()) {
                    bracketStack.pop();
                    continue;
                }
                if (c == ')' && bracketStack.isEmpty()) {
                    foundIndex = i;
                    break;
                }
            }
            if(foundIndex < 0){
                break;
            }
            String distinctFieldExpression = expression.substring(startIndex, foundIndex);
            String oldChar = countDistinctFlag + distinctFieldExpression;
            String newChar = countDistinctFlag + " " + fx.hash(distinctFieldExpression);
            newExpression = newExpression.replace(oldChar, newChar);
        }
        return newExpression;
    }

    public static void main(String[] args) {
        DefaultAggregator aggregator = new DefaultAggregator(null);
        String expr = "case when count(distinct f1.id) = 0 then null else sum(f1.amt) /count(distinct f1.id) end";
        expr = "case when count(distinct(f1.id)) = 0 then null else sum(f1.amt) /count(distinct(f1.id)) end";

        expr = "COUNT(distinct CASE WHEN [dim_plat_type] = 'app' and [businessline] = '轮胎' THEN [key_page_deviceid] else null end)";

        expr = "count(distinct if([thirdcheck_finish_time]>'',[receiveid],null))";

        expr = "case when count(distinct [item_pay_fenmu])=0 then 0 else count(distinct [item_pay_category])*1.0000 /count(distinct [item_pay_fenmu]) end";

        String newExpr = aggregator.wrapHashExpression(expr);
        System.out.println(expr);
        System.out.println(newExpr);
    }
}
