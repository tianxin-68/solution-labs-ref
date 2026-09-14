package com.bi.queryer.ssm.portal.enums;

/**
 * AI 脚本状态枚举
 * @Auther: contributor
 * @Date: 2026/3/6
 * @Description: Python 代码状态机
 */
public enum AiScriptStatus {

    /**
     * 未生成 - 初始状态，模版块刚创建，尚未生成 Python 代码
     */
    NOT_GENERATED("not_generated", "未生成"),

    /**
     * 生成中 - 正在调用大模型生成 Python 代码
     */
    GENERATING("generating", "生成中"),

    /**
     * 已生成 - Python 代码已由大模型生成，且输入未发生变更
     */
    GENERATED("generated", "已生成"),

    /**
     * 已过期 - Python 代码已生成，但用户修改了输入要素（输出模版/使用数据/计算逻辑）
     */
    OUTDATED("outdated", "已过期"),

    /**
     * 手动修改 - 用户在 Python 编辑器中手动修改并保存了代码
     */
    MANUAL_EDITED("manual_edited", "手动修改"),

    /**
     * 生成失败 - 大模型调用失败或生成的代码有语法错误
     */
    ERROR("error", "生成失败");

    private final String code;
    private final String name;

    AiScriptStatus(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    /**
     * 根据 code 获取枚举
     * @param code 状态码
     * @return 枚举对象
     */
    public static AiScriptStatus valueOfCode(String code) {
        for (AiScriptStatus status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知的脚本状态：" + code);
    }

    /**
     * 判断状态是否可以转换到生成中
     * @return 是否可以转换
     */
    public boolean canTransitionToGenerating() {
        return this == NOT_GENERATED 
            || this == OUTDATED 
            || this == ERROR
            || this == MANUAL_EDITED;
    }

    /**
     * 判断状态是否可以转换到已过期
     * @return 是否可以转换
     */
    public boolean canTransitionToOutdated() {
        return this == GENERATED || this == MANUAL_EDITED;
    }
}
