package com.bi.queryer.ssm.api.vo.rsp;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author contributor
 * @Date 15:58 2026/4/8
 * @Description TODO
 **/
public class AnalysisDataRsp {
    /**
     * 分析过程代码
     */
    private List<String> analysisProcessPythonCodes = new ArrayList<>();

    /**
     * 分析结果
     */
    private String analysisResult = "";

    public List<String> getAnalysisProcessPythonCodes() {
        return analysisProcessPythonCodes;
    }

    public void setAnalysisProcessPythonCodes(List<String> analysisProcessPythonCodes) {
        this.analysisProcessPythonCodes = analysisProcessPythonCodes;
    }

    public String getAnalysisResult() {
        return analysisResult;
    }

    public void setAnalysisResult(String analysisResult) {
        this.analysisResult = analysisResult;
    }
}
