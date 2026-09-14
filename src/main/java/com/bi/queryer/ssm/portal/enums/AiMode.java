package com.bi.queryer.ssm.portal.enums;

/**
 * @Auther: contributor
 * @Date: 2026/3/9 15:27
 * @Description:
 */
public enum AiMode {

    PYTHON_SCRIPT("pythonScript","python解读"),
    AI("ai","ai自由解读");

    private String code;

    private String name;

    AiMode(String code,String name){
        this.code = code;
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public static AiMode get(String code) {
        for (AiMode aiMode : values()) {
            if (aiMode.getCode().equalsIgnoreCase(code)) {
                return aiMode;
            }
        }
        return AI;
    }
}
