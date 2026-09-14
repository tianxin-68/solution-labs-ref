package com.bi.queryer.ssm.engine.config.ui.normalizer;

import com.bi.queryer.ssm.engine.config.ui.UIQueryConfigure;
import com.bi.queryer.ssm.enums.QueryConfigureType;
import com.bi.queryer.sys.config.SC;
import com.bi.queryer.util.BIUtil;
import com.alibaba.fastjson.JSONObject;

/**
 * @Author contributor
 * @Date 17:05 2024/11/13
 * @Description 前端查询配置规范化处理器：用于前端查询配置的兼容性处理
 **/
public class UIQueryConfigureNormalizer {
    protected String configString = null;

    protected UIQueryConfigure uiQueryConfigure = null;

    protected QueryConfigureType cfgType = null;
    public UIQueryConfigureNormalizer(String configString, QueryConfigureType cfgType){
        this.configString = configString;
        this.cfgType = cfgType;
    }

    public String normalize(){
        try {
            if (SC.v("ui.config.normalize.enable", "false").equalsIgnoreCase("false")) {
                return this.configString;
            }
            if (cfgType != QueryConfigureType.Table && cfgType != QueryConfigureType.GridHeader) {
                return this.configString;
            }
            if (BIUtil.isNotEmpty(configString)) {
                this.uiQueryConfigure = JSONObject.parseObject(configString, UIQueryConfigure.class);
            }

            // 规范化：自定义计算字段
            UICalcFieldNormalizer calcFieldNormalizer = new UICalcFieldNormalizer(this.uiQueryConfigure);
            calcFieldNormalizer.normalize();

            // 规范化：查询字段
            /*
            UIQueryFieldNormalizer queryFieldNormalizer = new UIQueryFieldNormalizer(this.uiQueryConfigure);
            queryFieldNormalizer.normalize();
             */

            // 规范化：聚合字段
            UIAggregatorNormalizer aggregatorNormalizer = new UIAggregatorNormalizer(this.uiQueryConfigure);
            aggregatorNormalizer.normalize();

            //规范化：lod字段
            UILodNormalizer lodNormalizer = new UILodNormalizer(this.uiQueryConfigure);
            lodNormalizer.normalize();

            //实时数据集处理
            UIRtDatasetNormalizer rtDatasetNormalizer = new UIRtDatasetNormalizer(this.uiQueryConfigure);
            rtDatasetNormalizer.normalize();

        }catch (Exception e){
            e.printStackTrace();
        }
        String newConfigString = JSONObject.toJSONString(this.uiQueryConfigure);
        return newConfigString;
    }
}
