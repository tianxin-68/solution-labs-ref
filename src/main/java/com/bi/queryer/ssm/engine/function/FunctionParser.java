package com.bi.queryer.ssm.engine.function;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @Author contributor
 * @Date 11:27 2025/7/1
 * @Description TODO
 **/
public class FunctionParser {
    public static void main(String[] args) {
        String input = "bi_add_month(replace('2024-05','-',''), 'year', 1)";
        FunctionInfo FunctionInfo = parse(input);

        System.out.println("函数名: " + FunctionInfo.getName());
        System.out.println("参数:");
        for (String param : FunctionInfo.getParameters()) {
            System.out.println("  " + param);
        }
    }

    public static FunctionInfo parse(String input) {
        // 移除所有空白字符
        String normalized = input.replaceAll("\\s+", "");

        // 匹配函数名和参数部分
        Pattern pattern = Pattern.compile("([a-zA-Z_]+)\\((.*)\\)");
        Matcher matcher = pattern.matcher(normalized);

        if (!matcher.matches()) {
            throw new IllegalArgumentException("无效的函数调用格式");
        }

        String functionName = matcher.group(1);
        String paramsStr = matcher.group(2);

        List<String> parameters = parseParameters(paramsStr);

        return new FunctionInfo(functionName, parameters);
    }

    private static List<String> parseParameters(String paramsStr) {
        List<String> parameters = new ArrayList<>();
        int depth = 0;
        StringBuilder currentParam = new StringBuilder();

        for (char c : paramsStr.toCharArray()) {
            if (c == ',' && depth == 0) {
                parameters.add(currentParam.toString().trim());
                currentParam = new StringBuilder();
            } else {
                if (c == '(') depth++;
                if (c == ')') depth--;
                currentParam.append(c);
            }
        }

        if (currentParam.length() > 0) {
            parameters.add(currentParam.toString().trim());
        }

        return parameters;
    }
}
