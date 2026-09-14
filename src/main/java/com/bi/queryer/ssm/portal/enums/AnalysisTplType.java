package com.bi.queryer.ssm.portal.enums;
/**
 * @Author: contributor
 * @CreateTime: 2024-06-17  16:55
 * @Description: 看板类型
 */
public enum AnalysisTplType {

    NORMAL("normal","正式看板"),
    TMP("tmp","临时看板"),
    SNAPSHOT("snapshot", "快照");

    private String code;
    private String name;

    AnalysisTplType(String code, String name){
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

    public static AnalysisTplType get(String code) {
        for (AnalysisTplType menuType : values()) {
            if (menuType.getCode().equalsIgnoreCase(code)) {
                return menuType;
            }
        }
        return NORMAL;
    }

}
