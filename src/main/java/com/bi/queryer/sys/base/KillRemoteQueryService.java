package com.bi.queryer.sys.base;

import com.bi.queryer.ssm.engine.session.QuerySession;
import com.bi.queryer.ssm.engine.session.QuerySessionManager;
import com.bi.queryer.sys.base.proxy.ConnectionProxy;
import com.bi.queryer.sys.rmi.RMIServer;
import com.bi.queryer.sys.rmi.impl.QueryKillRmiService;
import com.bi.queryer.util.BIUtil;
import com.bi.queryer.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Objects;

/**
 * @Auther: contributor
 * @Date: 2024/9/2 20:16
 * @Description:
 */
public class KillRemoteQueryService implements Serializable {
    private final static Logger LOG = LoggerFactory.getLogger(KillRemoteQueryService.class);

    public static boolean killQuery(String sessionId) {
        QuerySession querySession = QuerySessionManager.get(sessionId);
        if (querySession == null || StringUtil.isEmpty(querySession.getServerIp())) {
            return true;
        }

        try {
            Thread.sleep(500);
            String currentIp = BIUtil.getServerIP();
            if (Objects.equals(querySession.getServerIp(), currentIp)) {
                ConnectionProxy.killQuery(sessionId);
            } else {
                LOG.info("kill query {} on {}...", sessionId, querySession.getServerIp());
                Object param = new HashMap<String, String>() {
                    {
                        put("sessionId", sessionId);
                    }
                };
                RMIServer.syncInvoke(QueryKillRmiService.class, querySession.getServerIp(), param);
            }
            return true;
        } catch (Throwable t) {
            LOG.error("kill query {} failed", sessionId, t);
            return false;
        } finally {
            QuerySessionManager.delete(sessionId);
        }
    }
}
