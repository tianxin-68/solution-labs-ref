package com.bi.queryer.ssm.engine.function.impl;

import com.bi.queryer.ssm.engine.accelerate.route.DataSourceRouter;
import com.bi.queryer.ssm.engine.config.QuerySettings;
import com.bi.queryer.ssm.engine.function.FunctionInfo;
import com.bi.queryer.ssm.engine.function.FunctionParser;
import com.bi.queryer.ssm.engine.function.IFunction;
import com.bi.queryer.ssm.engine.function.impl.model.QueryProcess;
import com.bi.queryer.ssm.engine.session.QuerySessionProperty;
import com.bi.queryer.ssm.engine.session.QuerySessionSettingManager;
import com.bi.queryer.ssm.enums.DateGranularity;
import com.bi.queryer.ssm.util.SSDUtil;
import com.bi.queryer.sys.config.SC;
import com.bi.queryer.sys.db.DBUtil;
import com.bi.queryer.sys.db.DataSourceType;
import com.bi.queryer.sys.enums.DateType;
import com.bi.queryer.sys.enums.Enabled;
import com.bi.queryer.util.BIConsts;
import com.bi.queryer.util.BIUtil;
import com.bi.queryer.util.Guid;
import com.bi.queryer.util.network.HttpUtil;
import com.alibaba.fastjson.JSONObject;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @Author contributor
 * @Date 09:47 2022-11-02
 * @Description TODO
 **/
public class DorisFunction implements IFunction {

    protected String getDbFormatString(String format){

        String dbFormatStr = "";
        if(Format_DateTime.equalsIgnoreCase(format)){
            dbFormatStr = "%Y-%m-%d %H:%i:%S";
        }
        if(Format_Date.equalsIgnoreCase(format)){
            dbFormatStr = "%Y-%m-%d";
        }
        if(Format_Month.equalsIgnoreCase(format)){
            dbFormatStr = "%Y%m";
        }
        if(Format_Year.equalsIgnoreCase(format)){
            dbFormatStr = "%Y";
        }

        return dbFormatStr;
    }

    @Override
    public String date2Char(String field, String format) {
        String formatField = field;

        if (Format_Week.equalsIgnoreCase(format)) {
            //formatField = String.format("bi_get_week_id(%s)", field);
            formatField = String.format("yearweek(%s, 1)", field);
            return formatField;
        }

        //季度日期格式化
        if(Format_Quarter.equalsIgnoreCase(format)){
            formatField = String.format("concat(substring(%s, 1, 4),'-Q',quarter(str_to_date(REPLACE(SUBSTR(%s, 1, 7), '-', ''), '%%Y%%m')))", field, field);
            return formatField;
        }

        int subLength = 0;
        if(Format_DateTime.equalsIgnoreCase(format)){
            subLength = 19;
        }
        if(Format_Date.equalsIgnoreCase(format)){
            subLength = 10;
        }
        if(Format_Month.equalsIgnoreCase(format)){
            subLength = 7;
        }
        if(Format_Year.equalsIgnoreCase(format)){
            subLength = 4;
        }

        formatField = "substring(" + field + ",1" + "," + subLength + ")";
        // 月份去掉中划线
        if(Format_Month.equalsIgnoreCase(format)){
            formatField = "replace(" + formatField + "," + "'-'" + ",'')";
        }
        return formatField;
    }

    public String appendPagination(String sql, Integer pageNum , Integer pageSize){
        StringBuilder newSql = new StringBuilder(sql);
        newSql.append(" limit ").append(pageSize * pageNum);
        return newSql.toString();
    }

    @Override
    public String parseDate(String field, String format) {
        String dbFormatStr = getDbFormatString(format);
        String formatField = field;
        formatField = "str_to_date(" + field + ", '" + dbFormatStr + "')";
        return formatField;
    }

    @Override
    public String mask(String field, String mask) {
        String fieldFullName = "case when " + field + " is null or " + field + " = '' then '' " +
                "when length(" + field + ")<4 then concat(left(" + field + "," + "1) ,'" + mask + "') " +
                "else concat(left(" + field + "," + "1),'" + mask + "') end";
        return fieldFullName;
    }

    @Override
    public String coalesce(List<String> values) {
        if(BIUtil.isEmpty(values)) {
            return "";
        }
        if(values.size() == 1) {
            return values.get(0);
        }
        String coalesce = "coalesce(" + BIUtil.listToStr(values) + ")";
        return coalesce;
    }

    //完成率
    //bi_completion_rate(currentValue, targetValue, precision , isPositiveValue) ->double
    //入参：
    //currentValue：当前值，数值类型
    //targetValue：目标值，数值类型
    // precision：精度（小数位），必填，integer类型（正整数）
    //isPositiveValue：是否是正向值，整型 1=正向，0=负向
    //
    //出参：
    //当前值/目标值
    //
    //示例：
    //bi_completion_rate(88,100,2,1) -> 0.88
    @Override
    public String completionRate(String currentValue, String targetValue, String isPositiveValue) {
        //return "bi_completion_rate(" + currentValue + "," + targetValue + "," + "6" + "," + isPositiveValue + ")";
        if (Enabled.value(isPositiveValue)) {
            return String.format("CASE WHEN %s = 0 THEN CASE WHEN %s >= %s THEN 1.0 ELSE 0.0 END " +
                            "WHEN %s > 0 THEN CASE WHEN %s >= 0 THEN %s / %s ELSE 0.0 END " +
                            "WHEN %s < 0 THEN 2.0 - (%s / %s) END", targetValue, currentValue, targetValue, targetValue,
                    currentValue, currentValue, targetValue, targetValue, currentValue, targetValue);
        } else {
            return String.format("CASE  WHEN %s = 0 THEN CASE WHEN %s <= %s THEN 1.0 ELSE 0.0 END " +
                            "WHEN %s < 0 THEN CASE WHEN %s >= 0 THEN 0.0 ELSE %s / %s END " +
                            "WHEN %s > 0 THEN 2.0 - (%s / %s) END", targetValue, currentValue, targetValue, targetValue,
                    currentValue, currentValue, targetValue, targetValue, currentValue, targetValue);
        }
    }

    @Override
    public String getDateProgress(String dateGranularity, String dateFieldName) {
        DateGranularity dg = DateGranularity.get(dateGranularity);
        switch (dg) {
            case MONTH:
                return String.format("CASE WHEN %s < DATE_FORMAT(CURDATE(), '%%Y%%m') THEN 1.0 " +
                        "WHEN %s > DATE_FORMAT(CURDATE(), '%%Y%%m') THEN 0.0 " +
                        "ELSE CAST(DAY(CURDATE()) AS DOUBLE) / CAST(DAY(LAST_DAY(STR_TO_DATE(%s, '%%Y%%m'))) AS DOUBLE)" +
                        "END", dateFieldName, dateFieldName, dateFieldName);
            case YEAR:
                return String.format("CASE WHEN CAST(%s AS INT) < YEAR(CURDATE()) THEN 1.0 " +
                                "WHEN CAST(%s AS INT) > YEAR(CURDATE()) THEN 0.0 " +
                                "ELSE CAST(DAYOFYEAR(CURDATE()) AS DOUBLE) / " +
                                "CAST(CASE WHEN YEAR(CURDATE()) %% 4 = 0 AND (YEAR(CURDATE()) %% 100 != 0 OR YEAR(CURDATE()) %% 400 = 0) THEN 366 ELSE 365 END AS DOUBLE )" +
                                "END",
                        dateFieldName, dateFieldName);
            case  DAY:
                return "CAST(NULL AS DOUBLE)";
            default:
                return IFunction.super.getDateProgress(dateGranularity, dateFieldName);
        }
    }

    @Override
    public String formatDate(String field, String format) {
        String dbFormatStr = getDbFormatString(format);
        String formatField = field;
        formatField = "date_format(" + field + ", '" + dbFormatStr + "')";
        return formatField;
    }

    @Override
    public String addDate(String dateField, DateGranularity dg, Integer interval) {
        return String.format("date_add(%s, interval %s %s)", dateField, interval, dg.toString());
    }

    @Override
    public String division(String molecule, String denominator, boolean resultIsZeroByDenominatorIsZero) {
        //String result = String.format("bi_division(%s, %s, %s)", molecule, denominator, resultIsZeroByDenominatorIsZero ? 1 : 0);
        String result = String.format("((%s) * %s /%s)", molecule, BIConsts.INT_TO_DOUBLE_PRECISION, denominator);
        return result;
    }

    /* 调整到基类逻辑
    @Override
    public String groupingId(List<String> fields) {
        // udf 存在性能问题，偶发集群宕机
        // String groupingIdExpression = String.format("bi_grouping_id(array(%s))", BIUtil.listToStr(fields));

        // 原始grouping_id 在 grouping sets((dim1)) + sum(case when dim1='x')场景下，查询报错
        // String groupingIdExpression = String.format("grouping_id(%s)", BIUtil.listToStr(fields));

        String groupingIdExpression = "";
        int size = fields.size();
        List<String> gropingItemValues = new ArrayList<>();
        for (int i = 0; i < size; i++){
            String field = fields.get(i);
            String groupingItemValue = String.format("if(%s is null , pow(2,%s), 0)", field, size - i - 1);
            gropingItemValues.add(groupingItemValue);
        }
        groupingIdExpression = BIUtil.listToStr(gropingItemValues, " + ");
        return groupingIdExpression;
    }
     */

    @Override
    public String formatString(String formatStyle, String field) {
        String expr = String.format("concat('', %s)", field);
        return expr;
    }

    @Override
    public String castToString(String field) {
        String expr = String.format("concat('', %s)", field);
        boolean isEmptyCastToNull = SC.v("query.empty.cast.to.null", "true").equals("true");
        if(isEmptyCastToNull) {
            expr = String.format("if(coalesce(%s, '') in('','null'), '%s', %s)", expr, BIConsts.NULL_VALUE, expr);
        }else{
            expr = String.format("if(%s is null, '%s', %s)", field, BIConsts.NULL_VALUE, expr);
        }
        return expr;
    }

    @Override
    public QueryProcess monitor(Connection conn, Statement stmt, QuerySettings querySettings) {
        QueryProcess queryProcess = new QueryProcess();
        queryProcess.setQueryId(new AtomicReference(Guid.id()));
        try {
            // do nothing
            // 通过Statement设置超时时间无效，需要在hint中添加
            // stmt.setQueryTimeout(2);
            // test 手动 kill
            /*
            cachedThreadPool.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(5 * 1000);
                        killQuery("", "", querySettings);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
             */
        } catch (Exception e) {
            e.printStackTrace();
        }
        return queryProcess;
    }

    public String getDbEngineQueryId(QueryProcess process, String sessionId, DataSourceType dataSourceType){
        String queryId = "";
        try {
            /** 通过traceId获取queryId，去掉sql中的tips信息，提升缓存命中率 **/
            String url = this.getDorisApiBaseUrl(dataSourceType) + "/rest/v2/manager/query/trace_id/" + sessionId;
            String result = HttpUtil.doGet(url, new HashMap(), "application/json", this.getRequestHeaders(dataSourceType), 1500);
            if(BIUtil.isEmpty(result)){
                return "";
            }
            JSONObject resultJson = JSONObject.parseObject(result);
            queryId = resultJson.getString("data");
        } catch (Exception e) {
            System.out.println("getDbEngineQueryId: " + e.getMessage());
        }
        return queryId;
    }

    protected String getDorisApiBaseUrl(DataSourceType dataSourceType){
        String dorisServerBaseUrl = "";
        if(dataSourceType != null && dataSourceType.isHWDoris()){
            dorisServerBaseUrl = SC.v("hw_doris_master.api.base.url");
        }else {
            dorisServerBaseUrl = SC.v("doris_master.api.base.url");
        }
        return dorisServerBaseUrl;
    }

    public boolean killQuery(String sessionId, String killMessage, QuerySettings querySettings, DataSourceType dataSourceType) {
        Connection conn = null;
        ResultSet rs = null;
        Statement stmt = null;
        String queryId = "";
        try {
            /** 通过traceId获取queryId，去掉sql中的tips信息，提升缓存命中率 **/
            String url = this.getDorisApiBaseUrl(dataSourceType) + "/rest/v2/manager/query/trace_id/" + sessionId;
            String result = HttpUtil.doGet(url, new HashMap(), "application/json", this.getRequestHeaders(dataSourceType), 3 * 1000);
            if(BIUtil.isEmpty(result)){
                return true;
            }
            JSONObject resultJson = JSONObject.parseObject(result);
            if(!resultJson.get("msg").equals("success")){
                return true;
            }
            queryId = resultJson.getString("data");
            //System.out.println(result);

            /*
            // 无法真实在kill
            QueryProcess queryProcess = this.getQueryProcess(DBUtil.getConn(dataSourceType), querySettings);
            String url = SC.v("doris_master.api.base.url") + "/rest/v2/manager/query/kill/" + queryProcess.getQueryId();
            String result = HttpUtil.doPost(url, new HashMap(), "application/json", this.getRequestHeaders(), 3 * 1000);
            System.out.println(result);
             */

            /** 废弃：不通过sql列表获取对应的queryId
            QueryProcess queryProcess = this.getQueryProcess(conn, sessionId);
            if(queryProcess.getQueryId() != null && BIUtil.isNotEmpty(queryProcess.getQueryId().get())) {
                queryId = queryProcess.getQueryId().get();
            }
             */
            DataSourceType executeDataSourceType = null;
            if(dataSourceType != null && dataSourceType.isHWDoris()){
                executeDataSourceType = DataSourceType.HW_Doris_Master;
            }else {
                executeDataSourceType = DataSourceType.Doris_Master;
            }
            conn = DBUtil.getConn(executeDataSourceType);
            if(BIUtil.isNotEmpty(queryId)) {
                stmt = conn.createStatement();
                String sql = String.format("kill query '%s'", queryId);
                rs = stmt.executeQuery(sql);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }finally {
            try{
                if(rs != null){
                    rs.close();
                }
                if(stmt != null){
                    stmt.close();
                }
                if(conn != null){
                    conn.close();
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        return true;
    }

    /**
     * 获取查询进程，主要获取查询进程id，便于kill
     * @param conn
     * @param sessionId
     * @return
     */
    public QueryProcess getQueryProcess(Connection conn, String sessionId) {
        QueryProcess queryProcess = new QueryProcess();

        ResultSet rs = null;
        Statement queryListStmt = null;
        try{
            queryListStmt = conn.createStatement();

            String sql = "show full processlist";
            rs = queryListStmt.executeQuery(sql);
            while (rs.next()){
                String querySql = rs.getString("Info");
                if(querySql != null && BIUtil.isNotEmpty(sessionId) && querySql.contains(sessionId)) {
                    String queryId = rs.getString("Id");
                    queryProcess.setQueryId(new AtomicReference(queryId));
                    break;
                }
            }
        }catch (Throwable e){
            e.printStackTrace();
        }finally {
            try{
                if(rs != null){
                    rs.close();
                }
                if(queryListStmt != null){
                    queryListStmt.close();
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        return queryProcess;
    }

    @Override
    public void setSessionProperties(Connection conn, List<QuerySessionProperty> properties, String sessionId, String sql) {
        Statement stmt = null;
//        Connection traceIdConn = null;
        try{
            // 设置traceId，便于后续获取queryId
            //traceIdConn = DBUtil.getConn(DataSourceType.Doris_Master);
            stmt = conn.createStatement();
            String sessionSql = String.format("set session_context=\"trace_id:%s\"", sessionId);
            stmt.executeQuery(sessionSql);
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
//            if( traceIdConn != null){
//                try {
//                    traceIdConn.close();
//                } catch (SQLException e) {
//                    e.printStackTrace();
//                }
//            }
        }
    }

    @Override
    public String appendHint(List<QuerySessionProperty> properties, QuerySettings querySettings, String sql) {
        if(BIUtil.isEmpty(sql) || !sql.toLowerCase().contains("select ")){
            return sql;
        }
        Map<String, String> statementProperties = new HashMap<>();

        // 设置超时时间
        int queryTimeout = QuerySessionSettingManager.getQueryTimeoutSec(); //querySettings.getQueryTimeoutSec();
        statementProperties.put("query_timeout", queryTimeout + "");

        // 若是集团内部数据集，则添加默认参数
        if(SSDUtil.isGroupInternalDataset(querySettings.getDatasetId())){
            statementProperties.put("enable_fallback_to_original_planner", "false");
        }

        // 设置其他参数
        for(QuerySessionProperty property : properties){
            if(property.isSupportDoris()){
                statementProperties.put(property.getKey(), property.getValue());
            }
        }

        List<String> items = new ArrayList<>();
        for(String key : statementProperties.keySet()){
            items.add(String.format("%s=%s", key, statementProperties.get(key)));
        }

        String propertySql = String.format("/*+ SET_VAR(%s)*/ ", BIUtil.listToStr(items));
//        String propertySql = String.format("/*+ SET_VAR(%s)*/ sleep(120) as __s, ", BIUtil.listToStr(items));
        String replacement = String.format("select %s ", propertySql);
        // 测试慢sql
//        String replacement = String.format("select %s ", propertySql + " sleep(60) as __s,");
        sql = sql.replaceFirst("select ", replacement);

        return sql;
    }

    protected Map<String, String> getRequestHeaders(DataSourceType dataSourceType){
        Map<String, String> headers = new HashMap<>();
        String dsId = DataSourceRouter.getCurrentDataSourceType().getId();
        // DataSourceType.Doris_Master.getId(); DataSourceRouter.getCurrentDataSourceType().getId();
        String authString = "";
        if(dataSourceType != null && dataSourceType.isHWDoris()){
            authString =  SC.v("hw_doris.admin.user", "admin") + ":" + SC.v("hw_doris.admin.pwd", "Bigdata123!");
        }else {
            authString =  SC.v("doris.admin.user", "admin") + ":" + SC.v("doris.admin.pwd", "admin@Yh763@k8");// SC.v(dsId + ".jdbc.username") + ":" + DesEncryption.decrypt(SC.v(dsId + ".jdbc.password"));
        }

        String encodedAuth = Base64.getEncoder().encodeToString(authString.getBytes(StandardCharsets.UTF_8));
        headers.put("Authorization", "Basic " + encodedAuth);
        return headers;
    }


    @Override
    public boolean isManualKillMessage(String message) {
        if(BIUtil.isEmpty(message)) {
            return false;
        }
        String keywords[] = SC.v("doris.manual.kill.keyword", "milliseconds ago;cancelled;user cancel").split(";");
        for(String kw : keywords){
            if(message.contains(kw)){
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isQueryTimeoutMessage(String message) {
        if(BIUtil.isEmpty(message)) {
            return false;
        }
        if(message.contains("query timeout")) {
            return true;
        }
        return false;
    }

    @Override
    public boolean isBlockMessage(String message) {
        if(BIUtil.isEmpty(message)) {
            return false;
        }
        String[] keywords = SC.v("ssm.block.by.doris.keywords", "sql block rule,cancel query from fe,cancel top memory,query queue timeout,cancelled by workload policy").split(",");  //new String[]{"sql block rule", "cancel query from fe", "cancel top memory", "query queue timeout", "cancelled by workload policy"};
        for(String kw : keywords){
            if(message.toLowerCase().contains(kw)){
                return true;
            }
        }
        return false;
    }

    @Override
    public String hash(String field) {
        boolean enable = "true".equalsIgnoreCase(SC.v("distinct.hash.enable", "false"));
        if(enable){
            field = String.format("murmur_hash3_64(%s)", field);
        }
        return field;
    }

    @Override
    public String decode(String field, String rule) {
        // bi_decode在order by子句影响查询性能，故改造为case when
        if(BIUtil.isEmpty(rule) || !rule.contains(",")){
            return field;
        }
        String[] decodeItems = rule.split(",");
        Map<String, String> decodeMap = new LinkedHashMap<>();
        String otherKey = "(other_concat_raw)";
        for(int i = 0; i < decodeItems.length; i++){
            String key = decodeItems[i];
            String value = null;
            if((i+1) < decodeItems.length){
                value = decodeItems[i+1];
            }
            if(value != null){
                decodeMap.put(key, value);
            }
            i++;
        }
        List<String> whenExpressions = new ArrayList<>();
        for(String key : decodeMap.keySet()){
            if(otherKey.equals(key)){
                continue;
            }
            whenExpressions.add(String.format(" when '%s' then '%s' ", key, decodeMap.get(key)));
        }
        if(BIUtil.isEmpty(whenExpressions)) {
            return field;
        }
        String elseExpression = String.format(" else %s ", field);
        if(decodeMap.containsKey(otherKey)){
            elseExpression = String.format(" else concat('%s', %s) ", decodeMap.get(otherKey), field);
        }
        String caseWhenExpression = String.format("(case %s %s %s end)", field, BIUtil.listToStr(whenExpressions, ""), elseExpression);
        //return String.format("bi_decode(%s, '%s')", field, rule);
        return caseWhenExpression;
    }

    /**
     * 获取门店开业月份
     * 完整表达式 bi_shop_open_month(substring(vf.dt, 1, 10), 'd', vd1.online_month)
     * statsDateExpression 统计日期  substring(vf.dt, 1, 10)
     * dateGranularity 日期粒度 d
     * openMonthFieldFullName 门店开业月份字段名称 vd1.online_month
     * @return
     */
    public  String getShopOpenMonthExpression(String statsDateExpression,String dateGranularity,String openMonthFieldFullName) {

        String shopOpenMonthExpression = "";

        DateGranularity dateGranularityEnum = DateGranularity.get(dateGranularity);
        switch (dateGranularityEnum) {
            case DAY:
                shopOpenMonthExpression = String.format("months_diff(%s, str_to_date(str_to_date(%s,'-',''), '%%Y%%m'))"
                                                        ,statsDateExpression
                                                        , openMonthFieldFullName
                                                        );

                break;
            case MONTH:
                if(statsDateExpression != null && statsDateExpression.toLowerCase().contains("bi_add_month")){
                    // 去掉bi_add_month的udf，因为最终在group by子句出现，导致查询性能过慢
                    // 解决方案：将bi_add_month转换为原生doris函数months_add处理
                    // 解析入参:bi_add_month(原始日期，偏移类型，偏移量)
                    FunctionInfo functionInfo = FunctionParser.parse(statsDateExpression);
                    List<String> funcParameters = functionInfo.getParameters();
                    if(BIUtil.isNotEmpty(funcParameters) && funcParameters.size() == 3){
                        String rawDate = funcParameters.get(0);
                        String offsetType = funcParameters.get(1).replace("'", "");
                        String offsetValueString = funcParameters.get(2).replace("'", "");
                        DateType offsetDataType = DateType.get(offsetType);
                        Integer offsetValue = 0;
                        if(offsetDataType == DateType.Month){
                            offsetValue = Integer.valueOf(offsetValueString);
                        }
                        if(offsetDataType == DateType.Year){
                            offsetValue = Integer.valueOf(offsetValueString) * 12;
                        }
                        shopOpenMonthExpression = String.format(
                                "months_diff(months_add(str_to_date(%s,'%%Y%%m'), %s), str_to_date(replace(%s,'-',''), '%%Y%%m'))",
                                rawDate, offsetValue,
                                openMonthFieldFullName
                                );
                    }
                }
                // 兜底
                if(BIUtil.isEmpty(shopOpenMonthExpression)){
                    shopOpenMonthExpression = String.format("months_diff(str_to_date(%s,'%%Y%%m'), str_to_date(replace(%s,'-',''), '%%Y%%m'))",
                            statsDateExpression,
                            openMonthFieldFullName
                    );
                }
                break;
            case QUARTER:

                shopOpenMonthExpression = String.format("months_diff(str_to_date(replace(%s,'Q','0'),'%%Y%%m'), str_to_date(replace(%s,'-',''), '%%Y%%m')) ",
                        statsDateExpression,
                        openMonthFieldFullName
                        );
                break;
            case YEAR:

                shopOpenMonthExpression = String.format("months_diff(str_to_date(%s,'%%Y'), str_to_date(replace(%s,'-',''), '%%Y%%m')) ",
                        statsDateExpression,
                        openMonthFieldFullName
                        );
                break;
        }
        // 若为负数，则返回未null
        if(BIUtil.isNotEmpty(shopOpenMonthExpression)){
            shopOpenMonthExpression = String.format("if(%s < 0 , null, %s)", shopOpenMonthExpression, shopOpenMonthExpression);
        }
        return shopOpenMonthExpression;
    }

    @Override
    public String getDateRangeProgress(String startDate,String endDate,String currentDate){

        StringBuilder expression = new StringBuilder();

        expression.append(String.format("CASE WHEN DATEDIFF('%s', %s) < 0 THEN 0.0000  ",currentDate, startDate));
        expression.append(String.format("WHEN DATEDIFF(%s, '%s') < 0 THEN 1.0000   ",endDate,currentDate ));
        expression.append(String.format("ELSE ROUND(  (DATEDIFF('%s', %s) + 1) * 1.0000  / (DATEDIFF(%s, %s) + 1),  4) end",
                currentDate,startDate,endDate,startDate ));

        return expression.toString();
    }

    /**
     * 日期差值计算
     * @param startDate
     * @param endDate
     * @return
     */
    @Override
    public String getPromoDateDiff(String startDate,String endDate){
        return String.format("DATEDIFF('%s', %s)", endDate, startDate);
    }
}
