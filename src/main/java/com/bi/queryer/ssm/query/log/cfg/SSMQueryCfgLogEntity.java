package com.bi.queryer.ssm.query.log.cfg;

import lombok.*;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @Auther: contributor
 * @Date: 2024/9/3 15:39
 * @Description:
 */
@Setter
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SSMQueryCfgLogEntity {
    private String userName;
    private String sessionId;
    private String templateId;
    //for 更新
    private Map<String, Object> logContent;
    //for 插入
    List<String> logFields;
    List<Object> logValues;
    private String env;
    private Date createdTime;
    private Date updatedTime;
}
