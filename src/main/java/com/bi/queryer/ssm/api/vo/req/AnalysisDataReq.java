package com.bi.queryer.ssm.api.vo.req;

import com.bi.queryer.util.BIUtil;

/**
 * @Author contributor
 * @Date 19:57 2026/4/7
 * @Description TODO
 **/
public class AnalysisDataReq extends QueryOlapDataReq {
    /**
     * 模型名称
     */
    private String modelName = "pai-glm-4.7";

    /**
     * 提示词
     */
    private String prompt = "";

    /**
     * skill token：用于标识是通过agent平台的skill调用的，避免直接调用此接口，长度32位
     */
    private String skillToken = "";

    public String getModelName() {
        if(BIUtil.isEmpty(modelName)) {
            modelName = "pai-glm-4.7";
        }
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public String getSkillToken() {
        return skillToken;
    }

    public void setSkillToken(String skillToken) {
        this.skillToken = skillToken;
    }
}
