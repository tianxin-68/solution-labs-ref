package com.bi.queryer.ssm.portal.template.vo;

import java.util.List;

/**
 * AI 分析模板配置 VO
 * @Auther: contributor
 * @Date: 2026/3/6 14:27
 * @Description:
 */
public class AnalysisTemplateAiCfgVO {
    //看板 ID
    private String analysisTplId;
    // 视图 ID
    private String viewId;

    /**
     * 分析解读模式
     */
    private String analysisMode;

    private Boolean isActive;

    /**
     * 分析配置列表
     */
    private List<AnalysisTplAiCfgItem> analysisTplAiCfgItems;

    public String getAnalysisTplId() {
        return analysisTplId;
    }

    public void setAnalysisTplId(String analysisTplId) {
        this.analysisTplId = analysisTplId;
    }

    public String getAnalysisMode() {
        return analysisMode;
    }

    public void setAnalysisMode(String analysisMode) {
        this.analysisMode = analysisMode;
    }

    public Boolean getActive() {
        return isActive;
    }

    public void setActive(Boolean active) {
        isActive = active;
    }

    public List<AnalysisTplAiCfgItem> getAnalysisTplAiCfgItems() {
        return analysisTplAiCfgItems;
    }

    public void setAnalysisTplAiCfgItems(List<AnalysisTplAiCfgItem> analysisTplAiCfgItems) {
        this.analysisTplAiCfgItems = analysisTplAiCfgItems;
    }

    public String getViewId() {
        return viewId;
    }

    public void setViewId(String viewId) {
        this.viewId = viewId;
    }

    /**
     * 分析配置项
     */
    public static class AnalysisTplAiCfgItem {

        /**
         * 项目 ID
         */
        private String itemId;

        /**
         * 类型：script/static
         */
        private String type;

        /**
         * 输出格式
         */
        private String outputFormat;

        /**
         * 依赖数据列表
         */
        private List<DependData> dependDataList;

        /**
         * 输出提示词
         */
        private String outputPrompt;

        /**
         * AI 模型名称
         */
        private String llmModel;

        /**
         * Python 脚本 ID
         */
        private String scriptId;

        // 脚本状态
        private String scriptStatus;

        public String getItemId() {
            return itemId;
        }

        public void setItemId(String itemId) {
            this.itemId = itemId;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getOutputFormat() {
            return outputFormat;
        }

        public void setOutputFormat(String outputFormat) {
            this.outputFormat = outputFormat;
        }

        public List<DependData> getDependDataList() {
            return dependDataList;
        }

        public void setDependDataList(List<DependData> dependDataList) {
            this.dependDataList = dependDataList;
        }

        public String getOutputPrompt() {
            return outputPrompt;
        }

        public void setOutputPrompt(String outputPrompt) {
            this.outputPrompt = outputPrompt;
        }

        public String getScriptId() {
            return scriptId;
        }

        public void setScriptId(String scriptId) {
            this.scriptId = scriptId;
        }

        public String getScriptStatus() {
            return scriptStatus;
        }

        public void setScriptStatus(String scriptStatus) {
            this.scriptStatus = scriptStatus;
        }

        public String getLlmModel() {
            return llmModel;
        }

        public void setLlmModel(String llmModel) {
            this.llmModel = llmModel;
        }
    }

    /**
     * 依赖数据
     */
    public static class DependData {

        /**
         * 查询模板 ID
         */
        private String tplId;

        /**
         * 视图 ID
         */
        private String viewId;

        /**
         * 查询模板名称
         */
        private String tplName;

        // 视图名称
        private String viewName;

        /**
         * 数据路径
         */
        private String dataPath;

        // 全局字段
        private List<String> dataRange;

        private List<ColumnsInfo> columnsInfo;

        // 日期粒度
        private String dateGranularity;

        public List<String> getDataRange() {
            return dataRange;
        }

        public void setDataRange(List<String> dataRange) {
            this.dataRange = dataRange;
        }

        public String getDateGranularity() {
            return dateGranularity;
        }

        public void setDateGranularity(String dateGranularity) {
            this.dateGranularity = dateGranularity;
        }

        public String getTplId() {
            return tplId;
        }

        public void setTplId(String tplId) {
            this.tplId = tplId;
        }

        public String getViewId() {
            return viewId;
        }

        public void setViewId(String viewId) {
            this.viewId = viewId;
        }

        public String getTplName() {
            return tplName;
        }

        public void setTplName(String tplName) {
            this.tplName = tplName;
        }

        public String getViewName() {
            return viewName;
        }

        public void setViewName(String viewName) {
            this.viewName = viewName;
        }

        public String getDataPath() {
            return dataPath;
        }

        public void setDataPath(String dataPath) {
            this.dataPath = dataPath;
        }

        public List<ColumnsInfo> getColumnsInfo() {
            return columnsInfo;
        }

        public void setColumnsInfo(List<ColumnsInfo> columnsInfo) {
            this.columnsInfo = columnsInfo;
        }
    }
}
