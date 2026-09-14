package com.bi.queryer.ssm.query.log;

import com.bi.queryer.sys.enums.Enabled;
import com.bi.queryer.util.BIUtil;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

/**
 * @Auther: contributor
 * @Date: 2024/8/22 10:42
 * @Description:
 */
@Setter
@Getter
public class SSMFilterQueryLogEntity {
    private int logId;
    private String userName;
    private String queryFieldId;
    private String queryFieldCode;
    private String queryFieldTitle;
    private String querySql;
    private Integer isRetry = Enabled.NO.getId();
    private Integer isHitCache = Enabled.NO.getId();
    private String querySuccess;
    private Integer queryRows;
    private String queryInfo;
    private Date queryBeginTime;
    private Date queryEndTime;
    private String env =  BIUtil.getRuntimeEnv().getCode();
    private String dsKey;

    private String uiBeginTime;
    private String uiEndTime;
    private String sessionId;

    public SSMFilterQueryLogEntity() {
    }

    public SSMFilterQueryLogEntity(String userName, String queryFieldId, String queryFieldCode, String queryFieldTitle,
                                   Date queryBeginTime, String dsKey) {
        this.userName = userName;
        this.queryFieldId = queryFieldId;
        this.queryFieldCode = queryFieldCode;
        this.queryFieldTitle = queryFieldTitle;
        this.queryBeginTime = queryBeginTime;
        this.dsKey = dsKey;
    }
}
