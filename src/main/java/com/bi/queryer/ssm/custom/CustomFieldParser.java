package com.bi.queryer.ssm.custom;

import com.bi.queryer.ssm.engine.config.QueryConfigure;
import com.bi.queryer.ssm.engine.config.field.QueryField;
import com.bi.queryer.ssm.enums.AggExpressionType;
import com.bi.queryer.ssm.exception.SSDException;
import com.bi.queryer.ssm.meta.MetaField;
import com.bi.queryer.ssm.meta.SSDMetaCacheManager;
import com.bi.queryer.sys.enums.Enabled;
import com.bi.queryer.util.BIUtil;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 字段解析器
 * 表达式select子句的sql表达式:指标id用英文中括号包裹。如：[指标1_id]/[指标2_id]
 */
public abstract class CustomFieldParser {

    public static final String expressionPattern = "(?<=\\[)(.+?)(?=\\])";

    /**
     * 解析自定义字段，返回原生字段列表
     *  步骤：
     *  1、判断字段是否存在
     *  2、判断表达式的合法性
     *  3、判断字段互斥关系
     *  4、判断字段是否有权限（移到查询引擎校验）
     *  5、最后返回字段列表
     * @param customField
     * @return
     */
    public static List<QueryField> parseExpression(QueryConfigure config, QueryField customField) {
        List<QueryField> expressionFields = new ArrayList<>();
        if(customField == null
                || customField.getCustomFieldConfigure() == null
                ||!Enabled.value(customField.getCustomFieldConfigure().getEnable())
                || BIUtil.isEmpty(customField.getCustomFieldConfigure().getExpression())){
            return expressionFields;
        }

        // 判断字段是否存在
        CustomFieldConfigure customCfg = customField.getCustomFieldConfigure();
        String expression = customCfg.getExpression();
        expressionFields = parseExpression(expression);
        Pattern pattern = Pattern.compile(expressionPattern);
        Matcher matcher = pattern.matcher(expression);
        List<String> notExistFieldTitles = new ArrayList<>();
        String expressionTips = customCfg.getExpression(); // 用于将表达式还原为中文便于错误提示，和前端显示一致
        while (matcher.find()){
            String fieldId = matcher.group();
            expressionTips = expressionTips.replaceAll("\\[" + fieldId + "\\]", customCfg.getMappingTitle(fieldId));
            boolean isExists = expressionFields.stream().filter(f-> f.getId().equals(fieldId)).count() > 0;
            String title = customCfg.getMappingTitle(fieldId);
            if(!isExists && !notExistFieldTitles.contains(title)) {
                notExistFieldTitles.add(title);
            }
        }
        if(BIUtil.isNotEmpty(notExistFieldTitles)) {
            throw new SSDException("字段不存在:" + BIUtil.listToStr(notExistFieldTitles));
        }


        // 敏感字段
        /**
         * 敏感数据都可查询明文：下载时走审批：20250904 by contributor
        if(BIUtil.isNotEmpty(expressionFields)) {
            expressionFields.forEach(qf->{
                if (Enabled.value(qf.getMeta().getIsSensitive())) {
                    throw new SSDException("敏感字段[" + qf.getTitle() + "]不能参与计算");
                }
            });
        }*/

        // 判断合法性:调整到engine中通过全量sql校验
        try{
            //validExpression(customField);
        }catch (Exception e) {
            throw new SSDException("表达式不合法:" + expressionTips + "，原因：" + e.getMessage());
        }

        // 判断互斥
        List<String> exceptionExcludeNames = CustomFieldManager.checkExclude(config, customField);
        if(BIUtil.isNotEmpty(exceptionExcludeNames)) {
            throw new SSDException("计算字段[" + customField.getTitle() + "]与字段[" + BIUtil.listToStr(exceptionExcludeNames, ",", "") + "]不能同时使用");
        }
        if(BIUtil.isEmpty(expressionFields)) {
            throw new SSDException("计算字段[" + customField.getTitle() + "]无法找到对应的原始字段");
        }
        return expressionFields;
    }


    /**
     * 解析表达式(select子句的sql表达式)，聚合函数中添加[]包裹字段id,表达式示例：sum([id1])/count([id2])
     * @param expression
     * @return
     */
    public static List<QueryField> parseExpression(String expression) {
        List<QueryField> expressionFields = new ArrayList<QueryField>();
        Set<String> idSet = new HashSet<String>();
        Pattern pattern = Pattern.compile(expressionPattern);
        Matcher matcher = pattern.matcher(expression);
        while (matcher.find()){
            idSet.add(matcher.group());
        }

        for(String fieldId : idSet){
            if(fieldId.contains(CustomFieldType.LOD.getIdentifier()) || fieldId.contains(CustomFieldType.ANALYSIS.getIdentifier())) {
                continue;
            }
            MetaField meta = SSDMetaCacheManager.getField(fieldId);
            QueryField qf = new QueryField(meta);
            qf.setId(meta.getId());
            qf.setAppend(true);	// 表达式附加字段
            qf.setIsResult(true);
            if(!expressionFields.contains(qf)) {
                expressionFields.add(qf);
            }
        }

        return expressionFields;
    }

    /**
     * 将表达式转换为计算表达式，仅适用于度量表达式
     * @param expression
     * @return
     */
    public static String convertCalcExpression(QueryField customField, String expression){
        List<QueryField> expressionFields = parseExpression(expression);
        String calcExpression = expression;
        if(customField.getCustomFieldConfigure() == null
                || customField.getCustomFieldConfigure().isEmpty()
                || BIUtil.isEmpty(expressionFields)) {
            return calcExpression;
        }
        for(QueryField qf : expressionFields){
            String replacement = "";
            if(Enabled.value(customField.getCustomFieldConfigure().getIsMeasure())) { // 度量字段
                String aggExpression = qf.getMeta().getAggExpression();
                if(BIUtil.isEmpty(aggExpression)) {
                    aggExpression = "";
                }
                if(qf.getMeta() != null && BIUtil.isNotEmpty(qf.getMeta().getAggExpression()) && qf.getMeta().getAggExpression().indexOf("[") != -1) {// 原子字段本身是后台配置的计算字段
                    replacement = aggExpression;
                }else{
                    replacement = AggExpressionType.get(aggExpression).getExpression(String.format("[%s]",qf.getCode()));
                    /*
                    if(AggExpressionType.get(aggExpression) == AggExpressionType.Count_Distinct) {
                        replacement = aggExpression + "(distinct [" + qf.getCode() + "])";
                    }else {
                        replacement = aggExpression + "([" + qf.getCode() + "])";
                    }
                     */
                }
            }else{
                // 若是后台配置了表达式，则直接替换
                if(qf.getMeta() != null && BIUtil.isNotEmpty(qf.getMeta().getAggExpression()) && qf.isMeasure()){
                    replacement = qf.getMeta().getAggExpression();
                }else {
                    replacement = "[" + qf.getCode() + "]";
                }
            }
            calcExpression = calcExpression.replaceAll("\\[" + qf.getId() + "\\]",  replacement);
        }
        return calcExpression;
    }
}
