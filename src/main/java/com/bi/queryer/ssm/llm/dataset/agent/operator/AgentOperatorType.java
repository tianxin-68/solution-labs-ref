package com.bi.queryer.ssm.llm.dataset.agent.operator;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;

import java.util.List;

public enum AgentOperatorType {

    UNKNOWN("", "未知"),
    BI_AGENT_MID("BI_AGENT_MID","中位数");

    AgentOperatorType(String code, String desc){
        this.code = code;
        this.desc = desc;
    }

    private String code;

    private String desc;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public static AgentOperatorType getByCodeList(List<String> codeList) {

        if (CollUtil.isEmpty(codeList)) {
            return UNKNOWN;
        }

        for (AgentOperatorType type : AgentOperatorType.values()) {

            if(AgentOperatorType.UNKNOWN == type){
                continue;
            }

            boolean isExist = codeList.stream().filter(n -> n.toLowerCase().contains(type.getCode().toLowerCase())).findAny().isPresent();
            if (isExist) {
                return type;
            }
        }

        return UNKNOWN;
    }

    public static AgentOperatorType getByCode(String code) {

        if (StrUtil.isEmpty(code)) {
            return UNKNOWN;
        }

        for (AgentOperatorType type : AgentOperatorType.values()) {

            if(AgentOperatorType.UNKNOWN == type){
                continue;
            }

            boolean isExist = code.toLowerCase().contains(type.getCode().toLowerCase());
            if (isExist) {
                return type;
            }
        }

        return UNKNOWN;
    }

}
