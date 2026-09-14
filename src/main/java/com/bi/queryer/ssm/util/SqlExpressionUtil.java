package com.bi.queryer.ssm.util;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @Author contributor
 * @Date 10:36 2024/11/8
 * @Description sql表达式解析相关工具类
 **/
public abstract class SqlExpressionUtil {

    /**
     * 将表达式解析为聚合函数列表
     * 示例：sum([f1])/count(distinct [f2])/max([dt])
     * 返回：[{"fx":"sum","content": "([f1])"},{"fx":"count","content": "(distinct [f2])"}, {"fx":"max","content": "([dt])"}]
     * @param expression
     * @return
     */
    public static List<AggregatorItem> parseAggregators(String expression){
        List<AggregatorItem> items = new ArrayList<>();
        if(expression == null || "".equals(expression)){
            return items;
        }
        String[] aggregatorFlags = new String[]{"sum(","avg(","count(", "max(", "min("};
        int leftBracketCount = StrUtil.count(expression, "(");
        int rightBracketCount = StrUtil.count(expression, ")");
        if(leftBracketCount != rightBracketCount){
            return items;
        }
        String expressionLowerCase = expression.toLowerCase();
        for(String aggregatorFlag : aggregatorFlags){
            int matchCount = countMatches(expressionLowerCase, aggregatorFlag);
            if(matchCount == 0){
                continue;
            }
            int maxCount = 50; // 最多循环遍历次数
            while(matchCount > 0 && maxCount-- > 0) {
                int foundIndex = -1;
                int startIndex = expressionLowerCase.indexOf(aggregatorFlag, foundIndex + 1) + aggregatorFlag.length();
                Stack<Integer> bracketStack = new Stack<>();
                for (int i = startIndex; i < expressionLowerCase.length(); i++) {
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
                if (foundIndex < 0) {
                    continue;
                }

                // 通过索引解构源表达式：不改变源聚合函数和聚合内容的大小写
                AggregatorItem aggregatorItem = new AggregatorItem();
                String aggregator = expression.substring(startIndex - aggregatorFlag.length(), startIndex - 1);
                aggregatorItem.setAggregator(aggregator);
                aggregatorItem.setContent(expression.substring(startIndex, foundIndex));

                items.add(aggregatorItem);

                String target = expressionLowerCase.substring(startIndex - aggregatorFlag.length(), foundIndex + 1);
                String replacement = createRepeatString(" ", target.length());
                expressionLowerCase = expressionLowerCase.replace(target, replacement);
                matchCount = countMatches(expressionLowerCase, aggregatorFlag);
            }
        }
        return items;
    }

    public static int countMatches(String str, String sub) {
        if (str == null)
            return 0;
        int count = 0;
        for (int idx = 0; (idx = str.indexOf(sub, idx)) != -1; idx += sub
                .length())
            count++;

        return count;
    }

    public static String createRepeatString(String value,Integer num){
        String result = "";
        for(int i=0;i<num;i++){
            result+=value;
        }
        return result;
    }

    public static boolean isCompound(String expression){
        // 先剔除单引号中的字符，避免单号中有四则运算符
        String newExpression = expression.replaceAll("'[^']*'", "");
        String[] operators = new String[]{"+", "-", "*", "/"};
        for(String operator : operators){
            if(newExpression.contains(operator)){
                return true;
            }
        }
        return false;
    }

    /**
     * 解析表达式中的函数
     * @return
     */
    public static List<String> parseFunction(String  expression) {
        String regex = "\\b(\\w+)\\s*\\(";

        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(expression);

        List<String> functions = new ArrayList<>();
        while (matcher.find()) {
            String value = matcher.group(1);
            if (StrUtil.isNotEmpty(value)) {
                functions.add(value.toLowerCase()); // 提取函数名称
            }
        }

        //去重
        if (CollUtil.isNotEmpty(functions)) {
            functions = functions.stream().distinct().collect(Collectors.toList());
        }

        return functions;
    }

    public static void main(String[] args) {
        // String expr = "IF(count(distinct IF([is_good] = 0, [ancestor_orderid], null)) = 0, 0, count(distinct IF([is_bad_add_good] = 1, [ancestor_orderid], null)) / count(distinct IF([is_good] = 0, [ancestor_orderid], null)))";
        String expr = "func1(func2())";
        List<String> items = parseFunction(expr);
        items.forEach(item -> System.out.println(item.toString()));
    }
}

