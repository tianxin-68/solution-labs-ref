package com.bi.queryer.ssm.query.risk;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import com.bi.queryer.ssm.meta.MetaField;
import com.bi.queryer.ssm.query.risk.model.RiskDataContent;
import com.bi.queryer.ssm.query.risk.model.RiskDataField;
import com.bi.queryer.ssm.query.risk.model.RiskInfo;
import com.bi.queryer.ssm.engine.config.QueryConfigure;
import com.bi.queryer.ssm.engine.config.field.QueryField;
import com.bi.queryer.sys.config.SC;
import com.alibaba.fastjson.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 风控处理
 */
public class RiskEngine {

    private RiskInfo riskInfo;
    private QueryConfigure queryConfigure;

    public RiskEngine(RiskInfo riskInfo,QueryConfigure queryConfigure) {
        this.riskInfo = riskInfo;
        this.queryConfigure = queryConfigure;
    }

    public void process() {

        try {

            String url = SC.v("ssm.risk.submit.url", "");

            if (StrUtil.isEmpty(url)) {
                return;
            }

            buildRiskInfo();

            String token = SC.v("ssm.risk.submit.token", "");

            String resultBody = HttpRequest.post(url)
                    .header("u_token", token)
                    .body(JSONObject.toJSONString(riskInfo))
                    .execute().body();

            System.out.println(String.format("风控结果：%s", resultBody));

        } catch (Exception e) {

            System.out.println(String.format("风控处理异常", e.getMessage()));
            e.printStackTrace();
        }

    }

    /**
     * 构建上报风控的参数
     */
    public void buildRiskInfo(){
//        {
//            "username": "用户名",            //必填
//                "clientName": "接入系统名称",    //必填
//                "clientUrl": "接入系统请求url",
//                "clientAppId": "接入系统appid",  //必填
//                "clientLogId":"关联接入系统日志id",
//                "userIp": "用户访问ip",
//                "userBusinessLine": "用户所属业务线",
//                "dataBusinessLine":"数据业务线",
//                "dataContent": {                //必填
//            "dataFields":[
//            {"fieldName":"pid"},
//            {"fieldName":"predictprice"}
//    ]
//        },
//            "dataSensitiveLevel": "数据敏感级别",
//                "userBehavior": "访问/下载",   //必填
//                "dataNumber": 1000 //数据量    //必填
//        }


        List<QueryField> fields = queryConfigure.getResult().getFields();
        if(CollUtil.isNotEmpty(fields)) {

            List<RiskDataField> dataFields = new ArrayList<>();
            for (QueryField queryField : fields) {
                RiskDataField riskDataField = new RiskDataField();
                riskDataField.setFieldName(queryField.getName());

                MetaField metaField = queryField.getMeta();
                if (metaField != null) {
                    riskDataField.setSensitiveLevel(metaField.getSensitiveLevel());
                }

                dataFields.add(riskDataField);
            }

            RiskDataContent dataContent = new RiskDataContent();
            dataContent.setDataFields(dataFields);
            riskInfo.setDataContent(dataContent);
        }

        riskInfo.setOperateTime(new Date());

        riskInfo.setClientName(SC.v("ssm.risk.client.name", "ssm"));
        riskInfo.setClientAppId(SC.v("ssm.risk.client.appid", "tx-ssm-server"));

    }
}
