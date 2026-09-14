package com.bi.queryer.sys.base.proxy;

import cn.hutool.db.ds.pooled.ConnectionWraper;
import com.bi.queryer.sys.db.DBUtil;
import com.bi.queryer.sys.db.DataSourceType;
import com.bi.queryer.util.BIUtil;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Auther: contributor
 * @Date: 2024/8/2 10:17
 * @Description:
 */
public class ConnectionProxy extends ConnectionWraper {
    private final String sessionId;
    private static final Map<String, List<StatementProxy>> QUERY_STATEMENTS_MAP = new ConcurrentHashMap<>(16);

    public static ConnectionProxy getConn(DataSourceType dsType, String sessionId) throws SQLException {
        Connection conn = DBUtil.getConn(dsType);
        return new ConnectionProxy(conn, sessionId);
    }

    private ConnectionProxy(Connection conn, String sessionId) {
        this.raw = conn;
        this.sessionId = sessionId;
    }

    @Override
    public Statement createStatement() throws SQLException {
        Statement statement = raw.createStatement();
        List<StatementProxy> statementContainer = QUERY_STATEMENTS_MAP.computeIfAbsent(sessionId, key -> Collections.synchronizedList(new ArrayList<>(4)));
        StatementProxy proxy = new StatementProxy(statement);
        statementContainer.add(proxy);
        return proxy;
    }

    @Override
    public void close() throws SQLException {
        raw.close();
        QUERY_STATEMENTS_MAP.remove(sessionId);
    }

    public static void killQuery(String sessionId) {
        List<StatementProxy> statements = QUERY_STATEMENTS_MAP.remove(sessionId);
        if (BIUtil.isEmpty(statements)) {
            return;
        }

        for (StatementProxy statement : statements) {
            try {
                statement.cancel();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public boolean isClosed() throws SQLException {
        return raw.isClosed();
    }

}
