package com.bi.queryer.util;

/**
 * @Author contributor
 * @Date 17:48 2026/1/12
 * @Description sql聚合表达式解析器
 **/

import java.util.*;

public class SqlAggregationParser {
    public static Result analyze(String expr) {
        if (expr == null || expr.trim().isEmpty()) {
            return new Result(false, Collections.emptyList());
        }

        List<AggCall> aggCalls = extractAggregationCalls(expr);
        if (aggCalls.isEmpty()) {
            return new Result(false, Collections.emptyList());
        }

        // 使用 LinkedHashSet 保留顺序并去重
        Set<String> seen = new LinkedHashSet<>();
        for (AggCall call : aggCalls) {
            seen.add(call.method);
        }
        List<String> aggMethods = new ArrayList<>(seen);

        // 判断是否为单聚合表达式：整个表达式 ≈ 一个聚合调用
        boolean isSingle = (aggCalls.size() == 1) &&
                expr.trim().regionMatches(true, 0,
                        expr.substring(aggCalls.get(0).start, aggCalls.get(0).end).trim(), 0,
                        expr.substring(aggCalls.get(0).start, aggCalls.get(0).end).trim().length());

        return new Result(isSingle, aggMethods);
    }

    // 聚合调用信息
    private static class AggCall {
        final String method;
        final int start, end;

        AggCall(String method, int start, int end) {
            this.method = method;
            this.start = start;
            this.end = end;
        }
    }

    private static List<AggCall> extractAggregationCalls(String expr) {
        List<AggCall> calls = new ArrayList<>();
        char[] chars = expr.toCharArray();
        int i = 0;
        int n = chars.length;

        while (i < n) {
            char c = chars[i];

            if (Character.isWhitespace(c)) {
                i++;
                continue;
            }

            if (Character.isLetter(c)) {
                int start = i;
                StringBuilder nameBuilder = new StringBuilder();
                while (i < n && (Character.isLetterOrDigit(chars[i]) || chars[i] == '_')) {
                    nameBuilder.append(Character.toLowerCase(chars[i]));
                    i++;
                }
                String funcName = nameBuilder.toString();

                if (isAggFunction(funcName)) {
                    while (i < n && Character.isWhitespace(chars[i])) i++;

                    if (i < n && chars[i] == '(') {
                        int openPos = i;
                        i++; // skip '('

                        int parenDepth = 1;
                        boolean inString = false;
                        char stringQuote = 0;
                        int contentStart = i;

                        while (i < n && parenDepth > 0) {
                            char ch = chars[i];
                            if (!inString) {
                                if (ch == '\'' || ch == '"') {
                                    inString = true;
                                    stringQuote = ch;
                                } else if (ch == '(') {
                                    parenDepth++;
                                } else if (ch == ')') {
                                    parenDepth--;
                                }
                            } else {
                                if (ch == stringQuote && (i == 0 || chars[i - 1] != '\\')) {
                                    inString = false;
                                }
                            }
                            i++;
                        }

                        if (parenDepth == 0) {
                            String method = funcName;
                            if ("count".equals(funcName)) {
                                String content = expr.substring(contentStart, i - 1).trim();
                                if (content.toLowerCase().startsWith("distinct")) {
                                    method = "count(distinct)";
                                }
                            }
                            calls.add(new AggCall(method, start, i));
                        } else {
                            i = openPos + 1;
                        }
                    } else {
                        i = start + 1;
                    }
                } else {
                    i = start + 1;
                }
            } else {
                i++;
            }
        }
        return calls;
    }

    private static boolean isAggFunction(String name) {
        return "sum".equals(name) ||
                "count".equals(name) ||
                "max".equals(name) ||
                "min".equals(name) ||
                "avg".equals(name);
    }

    /**
     * @param expr
     * @return
     */
    public static boolean isSingleAggregation(String expr){

        try {

            Result result = analyze(expr);
            return result.isSingleAggregation;

        }catch (Exception e){
            return false;
        }

    }

    public static boolean isSingleAndNotCountDistinctAggregation(String expr){
        Result result = analyze(expr);
        return result.isSingleAggregation && BIUtil.isNotEmpty(result.aggregationMethods) && !result.aggregationMethods.contains("count(distinct)");
    }

    // ===== Result 类：使用 List<String> =====
    public static class Result {
        public final boolean isSingleAggregation;
        public final List<String> aggregationMethods;

        public Result(boolean isSingle, List<String> methods) {
            this.isSingleAggregation = isSingle;
            this.aggregationMethods = Collections.unmodifiableList(new ArrayList<>(methods));
        }

        // 为方便打印，提供 toString（按你要求的格式）
        @Override
        public String toString() {
            String methodsStr = String.join("/", aggregationMethods);
            return "单聚合表达式：" + (isSingleAggregation ? "是" : "否") +
                    "\n聚合方式:" + methodsStr;
        }
    }

    // ===== 测试 =====
    public static void main(String[] args) {
        String[] cases = {
                null,
                "x/c",
                "",
                "abcde",
                "abc/123",
                "avg(1sd23",
                "COUNT(DISTINCT IF([fa8148dea32145a092ae6c925375aeec] = '刹车片/刹车盘问题', [0029ad374a0a4f69af6a319f5fdf87e3], NULL))",
                "sum( [c8aecbf41ea14b86a037939ceff69138] )  * 1.00 /  ( sum( [bdd1dccdcb5447429521d0d4bd713c1c] ) * 1.00/ 30 )",
                "count(distinct [164a618dee3e44b9ad9650b3501102f1] )   * 1.00 /  count(distinct [92f7b4ef6c33485996a8b6a9dc52796d] )",
                "sum(case when  [007e3fe0c4db4664927650ddc07d0c0b]  = 'BI_QUERYER_POP_MERCHANT' then   [192c146678eb49c18b380ae9ce7687ce]/1.13  else  [a1b800c7345341c5babd2d9dd55eba61]  end)",
                "sum( [807128cc802b4c43b88baa4d8870b65e] ) /count(distinct  [a25740c0d4d74c5a82f07a58ef6405c7] )"
        };

        for (int i = 0; i < cases.length; i++) {
            System.out.println("=== 案例" + (i + 1) + " ===");
            System.out.println(cases[i]);
            Result result = analyze(cases[i]);
            System.out.println(result);

            // 展示 List 内容（实际开发中可直接使用）
            System.out.println("→ 聚合方式列表: " + result.aggregationMethods);
            System.out.println(isSingleAggregation(cases[i]));
            System.out.println(isSingleAndNotCountDistinctAggregation(cases[i]));

            System.out.println("------");
        }
    }
}
