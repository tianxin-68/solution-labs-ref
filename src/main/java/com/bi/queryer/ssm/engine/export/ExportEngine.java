package com.bi.queryer.ssm.engine.export;

import com.bi.queryer.ssm.engine.QueryEngine;
import com.bi.queryer.ssm.query.risk.RiskEngine;
import com.bi.queryer.ssm.query.risk.RiskQueueService;
import com.bi.queryer.ssm.query.risk.enums.UserBehaviorType;
import com.bi.queryer.ssm.query.risk.model.RiskInfo;
import com.bi.queryer.sys.user.UserManager;
import com.bi.queryer.sys.user.vo.User;
import com.bi.queryer.util.SpringContextUtil;

/**
 * @Auther: contributor
 * @Date: 2025/2/24 11:48
 * @Description:
 */
public abstract class ExportEngine extends QueryEngine {
    protected Integer totalSize = 0;
    protected QueryEngine queryEngine;

    public ExportEngine(QueryEngine queryEngine) {
        super(queryEngine.getConfig(), queryEngine.getCxt());
        this.queryEngine = queryEngine;
    }


    public abstract void export(String fileRealName, String querySql);

    public void submitRisk() {

        try {

            User user = UserManager.get();
            RiskQueueService riskQueueService = (RiskQueueService) SpringContextUtil.getBean("riskQueueService");
            RiskInfo riskInfo = new RiskInfo();
            riskInfo.setUsername(user.getName());
            riskInfo.setUserBehavior(UserBehaviorType.DOWNLOAD.getName());
            riskInfo.setDataNumber(totalSize);
            riskInfo.setBlackBox(this.cxt.getBlackBox());
            riskInfo.setClientLogId(this.cxt.getExportLogId());

            RiskEngine riskEngine = new RiskEngine(riskInfo, queryEngine.getConfig());
            riskQueueService.addQueue(riskEngine);

        } catch (Exception e) {
            System.out.println("导出上报风控异常:" + e.getMessage());
            e.printStackTrace();
        }

    }

    public Integer getTotalSize() {
        return totalSize;
    }
}
