package com.bi.queryer.ssm.engine.analysis.cfg.total;

import com.bi.queryer.ssm.enums.AnalysisTotalAggType;
import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @Auther: contributor
 * @Date: 2025/10/15 14:27
 * @Description:
 */
public class AnalysisTotalAggConfig {
//    {
//        "mode":"custom",
//            "config" : [
//        {
//            "id": "7017d48ce1fc4bcb8afe1637e712ccf8",
//                "code": "O_ORD_00336",
//                "totalAggType":"avg"
//        },
//        {
//            "id": "lod:e5fa33bfd7b84049b38ba9db47fd121c",
//                "code": "",
//                "totalAggType":"avg"
//        }
//                ]
//    }

    private String configType;
    private List<AnalysisZbTotalAggItemConfig> configs = new ArrayList<>();

    public List<AnalysisZbTotalAggItemConfig> getConfigs() {
        return configs;
    }

    public void setConfigs(List<AnalysisZbTotalAggItemConfig> configs) {
        this.configs = configs;
    }

    public String getConfigType() {
        return configType;
    }

    public void setConfigType(String configType) {
        this.configType = configType;
    }

    public boolean isActive() {
        if (CollectionUtils.isEmpty(configs)) {
            return false;
        }

        return configs.stream().anyMatch(v -> !AnalysisTotalAggType.DEFAULT.getCode().equals(v.getAggType()));
    }

    public String getAggTypeDesc() {
        if ("custom".equals(configType)) {
            Set<String> aggTypes = configs.stream().map(AnalysisZbTotalAggItemConfig::getAggType).collect(Collectors.toSet());
//            if (aggTypes.size() == 1) {
//                return AnalysisTotalAggType.get(aggTypes.iterator().next()).getDesc();
//            } else {
//                return "自定义";
//            }

            return "自定义";
        } else if (configs.size() > 0) {
            return AnalysisTotalAggType.get(configs.get(0).getAggType()).getDesc();
        } else {
            return AnalysisTotalAggType.DEFAULT.getDesc();
        }
    }

    public static class AnalysisZbTotalAggItemConfig {
        //指标id
        private String id;
        //指标code
        private String code;
        //指标聚合方式
        private String aggType;


        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getAggType() {
            return aggType;
        }

        public void setAggType(String aggType) {
            this.aggType = aggType;
        }
    }
}
