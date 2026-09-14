package com.bi.queryer.ssm.engine.function.impl;


import com.bi.queryer.ssm.engine.config.QuerySettings;
import com.bi.queryer.ssm.engine.function.impl.model.QueryProcess;
import com.bi.queryer.ssm.engine.session.QuerySessionProperty;
import com.bi.queryer.ssm.engine.session.QuerySessionSettingManager;
import com.bi.queryer.ssm.exception.SSDException;
import com.bi.queryer.sys.config.SC;
import com.bi.queryer.util.BIConsts;
import com.bi.queryer.util.BIUtil;
import com.bi.queryer.util.StringUtil;
import io.trino.jdbc.TrinoConnection;
import io.trino.jdbc.TrinoStatement;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

public class TrinoFunction extends PrestoFunction {

    public QueryProcess getQueryProcess(Connection conn, Statement stmt, QuerySettings querySettings) throws SQLException {
        QueryProcess queryProcess = new QueryProcess();
        TrinoStatement ts = stmt.unwrap(TrinoStatement.class);
//        TrinoConnection tc = conn.unwrap(TrinoConnection.class);
//        tc.setSessionProperty("use_mark_distinct", "false");
//        tc.setSessionProperty("query_max_stage_count", "10000");
        ts.setProgressMonitor(queryStats -> {
            queryProcess.getQueryId().set(queryStats.getQueryId());
            queryProcess.getQueryState().set(queryStats.getState());

        });
        return  queryProcess;
    }

    @Override
    public void setSessionProperties(Connection conn, List<QuerySessionProperty> properties, String sessionId, String sql) {
        TrinoConnection tc = null;
        try {
            tc = conn.unwrap(TrinoConnection.class);
//            tc.setSessionProperty("use_mark_distinct", "false");
//            tc.setSessionProperty("mark_distinct_strategy", "none");
//            tc.setSessionProperty("query_max_stage_count", "10000");
            tc.setSessionProperty("query_max_run_time", QuerySessionSettingManager.getQueryTimeoutSec() + "s");
            int distinctStrategyCount = Integer.valueOf(SC.v("trino.distinct.strategy.none.count", "10"));
            if(BIUtil.isNotEmpty(sql)){
                int matchCount = StringUtil.countMatches(sql.toLowerCase(), "distinct ");
                if(matchCount > distinctStrategyCount){
                    tc.setSessionProperty("mark_distinct_strategy", "NONE");
                }else{
                    tc.setSessionProperty("mark_distinct_strategy", "AUTOMATIC");
                }
            }
            if(BIUtil.isEmpty(properties)){
                return;
            }
            for(QuerySessionProperty p : properties){
                if(p.isSupportTrino()) {
                    if("query_max_run_time".equals(p.getKey())){
                        // 查询超时时间已单独设置
                        continue;
                    }
                    if("mark_distinct_strategy".equals(p.getKey())){
                        // 已单独设置
                        continue;
                    }
                    tc.setSessionProperty(p.getKey(), p.getValue());
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String division(String molecule, String denominator, boolean resultIsZeroByDenominatorIsZero) {
        //String result = String.format("bi_devision(%s, %s, %s)", molecule, denominator, resultIsZeroByDenominatorIsZero ? 1 : 0);
        String result = String.format("((%s) * %s /%s)", molecule, BIConsts.INT_TO_DOUBLE_PRECISION, denominator);
        return tryCatch(result);
    }

    @Override
    public String formatString(String formatStyle, String field) {
        String expr = String.format("format('%s', %s)", formatStyle, field);
        return expr;
    }

    @Override
    public String castToString(String field) {
        String expr = String.format("format('%s', %s)", "%s", field);
        boolean isEmptyCastToNull = SC.v("query.empty.cast.to.null", "true").equals("true");
        if(isEmptyCastToNull) {
            expr = String.format("if(coalesce(%s, '') in('','null'), '%s', %s)", expr, BIConsts.NULL_VALUE, expr);
        }else{
            expr = String.format("if(%s is null, '%s', %s)", field, BIConsts.NULL_VALUE, expr);
        }
        return expr;
    }

    @Override
    public boolean isManualKillMessage(String message) {
        if(BIUtil.isEmpty(message)) {
            return false;
        }
        if(message.contains(SSDException.Manual_Kill_Error)) {
            return true;
        }
        return false;
    }

    @Override
    public String quote(String fieldName) {
        return String.format("\"%s\"", fieldName);
    }

    @Override
    public boolean isQueryTimeoutMessage(String message) {
        if(BIUtil.isEmpty(message)) {
            return false;
        }
        if (message.contains(BIConsts.SSM_ERROR_TIMEOUT_Prefix) || message.toLowerCase().contains("error fetching results")  || message.toLowerCase().contains("maximum time limit")) {
            return true;
        }
        return false;
    }

    /**
     * 日期差值计算
     * @param startDate
     * @param endDate
     * @return
     */
    @Override
    public String getPromoDateDiff(String startDate,String endDate){
        return String.format("date_diff('day', cast(%s as DATE),DATE '%s')", startDate,endDate );
    }

    /**
     * 获取日期进度
     * @param startDate
     * @param endDate
     * @param currentDate
     * @return
     */
    @Override
    public  String getDateRangeProgress(String startDate,String endDate,String currentDate){
        StringBuilder expression = new StringBuilder();

        expression.append(String.format("CASE WHEN DATE('%s') < DATE(%s) THEN 0.0000  ",currentDate, startDate));
        expression.append(String.format("WHEN DATE('%s') > DATE(%s) THEN 1.0000   ",currentDate,endDate ));
        expression.append(String.format("ELSE ROUND(  (date_diff('day', DATE(%s), DATE('%s')) + 1) * 1.0000 / (date_diff('day', DATE(%s), DATE(%s)) + 1), 4) END ",
                startDate,currentDate,startDate,endDate ));

        return expression.toString();
    }
}
