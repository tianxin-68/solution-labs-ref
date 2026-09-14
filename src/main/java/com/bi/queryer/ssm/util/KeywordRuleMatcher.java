package com.bi.queryer.ssm.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 关键字规则匹配工具类
 * 源列表格式：["门店名称", "销售数量", "库存商品名称"]
 * 规则格式：[门店名称] and ([%商品名称%] or [%产品名称%]) and ([%数量%] or [%金额%] or [%GMV%])
 */
public class KeywordRuleMatcher {

    private static final Pattern AND_PATTERN = Pattern.compile("\\s+and\\s+", Pattern.CASE_INSENSITIVE);
    private static final Pattern OR_PATTERN = Pattern.compile("\\s+or\\s+", Pattern.CASE_INSENSITIVE);

    // 内部类：记录规则关键词及其匹配模式
    private static class RuleKeyword {
        String keyword;
        boolean isFuzzy; // true 表示模糊匹配（含 %），false 表示精确匹配

        RuleKeyword(String keyword, boolean isFuzzy) {
            this.keyword = keyword;
            this.isFuzzy = isFuzzy;
        }
    }

    /**
     * 判断源关键字列表（不带[]）是否匹配规则表达式（带[]）
     *
     * @param sourceKeywords 源关键字列表，如 ["门店名称", "销售数量", "库存商品名称"]
     * @param ruleExpression 规则表达式，如 "[门店名称] and ([%商品名称%] or [%产品名称%]) and ([%数量%] or [%金额%] or [%GMV%])"
     * @return 是否匹配
     */
    public static boolean matches(Collection<String> sourceKeywords, String ruleExpression) {
        if (sourceKeywords == null || sourceKeywords.isEmpty() ||
                ruleExpression == null || ruleExpression.trim().isEmpty()) {
            return false;
        }

        // 1. 标准化规则表达式
        String normalizedRule = ruleExpression
                .replaceAll("（", "(")
                .replaceAll("）", ")")
                .replaceAll("\\s+", " ")
                .trim();

        // 2. 按顶层 "and" 拆分条件组（跳过括号内）
        List<String> ruleGroupsStr = splitByTopLevelAnd(normalizedRule);

        // 3. 解析每组规则为 RuleKeyword 列表
        List<List<RuleKeyword>> ruleGroups = new ArrayList<>();
        for (String groupStr : ruleGroupsStr) {
            List<RuleKeyword> groupKeywords = parseGroup(groupStr);
            if (!groupKeywords.isEmpty()) {
                ruleGroups.add(groupKeywords);
            }
        }

        // 4. 对每组进行匹配（OR 逻辑），组间是 AND
        for (List<RuleKeyword> ruleGroup : ruleGroups) {
            boolean groupMatched = false;
            for (RuleKeyword ruleKeyword : ruleGroup) {
                for (String sourceKeyword : sourceKeywords) {
                    if (ruleKeyword.isFuzzy) {
                        // 模糊匹配：源关键词包含规则关键词
                        if (sourceKeyword.contains(ruleKeyword.keyword)) {
                            groupMatched = true;
                            break;
                        }
                    } else {
                        // 精确匹配：源关键词必须完全等于规则关键词
                        if (sourceKeyword.equals(ruleKeyword.keyword)) {
                            groupMatched = true;
                            break;
                        }
                    }
                }
                if (groupMatched) break;
            }
            if (!groupMatched) {
                return false;
            }
        }

        return true;
    }

    /**
     * 按顶层 and 分割规则（不拆分括号内的 and）
     */
    private static List<String> splitByTopLevelAnd(String rule) {
        List<String> groups = new ArrayList<>();
        int start = 0;
        int parenCount = 0;

        for (int i = 0; i < rule.length(); i++) {
            char c = rule.charAt(i);
            if (c == '(') {
                parenCount++;
            } else if (c == ')') {
                parenCount--;
            } else if (parenCount == 0 && rule.regionMatches(true, i, " and ", 0, 5)) {
                groups.add(rule.substring(start, i).trim());
                i += 4; // 跳过 " and"
                start = i + 1;
            }
        }
        groups.add(rule.substring(start).trim());

        return groups;
    }

    /**
     * 解析单个规则组（如 "(%商品名称% or %产品名称%)"），提取 RuleKeyword 列表
     */
    private static List<RuleKeyword> parseGroup(String group) {
        List<RuleKeyword> keywords = new ArrayList<>();
        group = group.trim();

        // 去掉外层括号（如果有）
        if (group.startsWith("(") && group.endsWith(")")) {
            group = group.substring(1, group.length() - 1).trim();
        }

        // 按 "or" 分割
        String[] orParts = OR_PATTERN.split(group);
        for (String part : orParts) {
            part = part.trim();
            if (part.isEmpty()) continue;

            // 提取核心关键词和匹配模式
            RuleKeyword ruleKeyword = extractRuleKeyword(part);
            if (ruleKeyword != null) {
                keywords.add(ruleKeyword);
            }
        }

        return keywords;
    }

    /**
     * 从形如 "[ %商品名称% ]" 的字符串中提取 RuleKeyword
     * isFuzzy = true 如果有 %，否则 false
     */
    private static RuleKeyword extractRuleKeyword(String keywordStr) {
        if (keywordStr == null || !keywordStr.startsWith("[") || !keywordStr.endsWith("]")) {
            return null;
        }

        String inner = keywordStr.substring(1, keywordStr.length() - 1).trim();

        boolean isFuzzy = false;
        // 检查首尾是否有 %
        if (inner.startsWith("%")) {
            isFuzzy = true;
            inner = inner.substring(1);
        }
        if (inner.endsWith("%")) {
            isFuzzy = true;
            inner = inner.substring(0, inner.length() - 1);
        }

        String core = inner.trim();
        if (core.isEmpty()) {
            return null;
        }

        return new RuleKeyword(core, isFuzzy);
    }

    public static void main(String[] args) {
        List<String> source1 = Arrays.asList("用户姓名", "身份证号", "收件人手机号");

        // 示例1：匹配
        String rule1 = "([用户ID] or [用户姓名])  and [%手机号%]  and [%身份证号%]";
        System.out.println(matches(source1, rule1)); // true
        // 门店名称 匹配 [门店名称]（精确）
        // 库存商品名称 包含 "商品名称"（模糊）
        // 销售数量 包含 "数量"（模糊）

        // 示例2：不匹配
        String rule2 = "[门店名称] and ([商品名称] or [%产品名称%]) and ([%数量%] or [%金额%] or [%GMV%])";
        System.out.println(matches(source1, rule2)); // false
        // 因为 "库存商品名称" 不等于 "商品名称"（精确匹配）

        // 示例2：不匹配
        String rule3 = "[门店名称] and ([%商品名称%] or [%产品名称%]) and ([%数量%] or [%金额%] or [%GMV%])";
        System.out.println(matches(source1, rule3)); // false
    }
}
