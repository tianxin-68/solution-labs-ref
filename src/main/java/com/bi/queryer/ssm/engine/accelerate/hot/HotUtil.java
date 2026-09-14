package com.bi.queryer.ssm.engine.accelerate.hot;

import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import com.bi.queryer.ssm.engine.accelerate.hot.etl.model.EtlTableDdl;
import com.bi.queryer.ssm.meta.accelerate.hot.HotTableInfo;
import com.bi.queryer.ssm.meta.accelerate.hot.HotTableSource;
import com.bi.queryer.sys.config.SC;
import com.bi.queryer.sys.db.DBType;
import com.bi.queryer.util.BIUtil;
import com.bi.queryer.util.StringUtil;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @Author contributor
 * @Date 14:57 2024/8/21
 * @Description 热化相关工具类
 **/
public abstract class HotUtil {
    public static String logFormat(String title, boolean isEnd){
        if(!isEnd) {
            return String.format("%s：%s[开始○]", DateUtil.now(), title);
        }else {
            return String.format("%s：%s[结束●]", DateUtil.now(), title);
        }
    }

    /**
     * 将sql中unicode转义为中文
     * @param sql
     * @return
     */
    public static String escape(String sql){
        Pattern pattern = Pattern.compile("\\\\[0-9A-Fa-f]{4}");
        Matcher matcher = pattern.matcher(sql);
        Map<String, String> unicodeValues = new HashMap<>();
        while (matcher.find()) {
            String unicode = matcher.group();
            if(BIUtil.isEmpty(unicode) || unicodeValues.containsKey(unicode)){
                continue;
            }
            String unicodeLowerCase = unicode.replace("\\", "\\U").toLowerCase();
            String value = StringEscapeUtils.unescapeJava(unicodeLowerCase);
            unicodeValues.put(unicode, value);
        }

        String newSql = sql;
        for(String unicode : unicodeValues.keySet()){
            newSql = newSql.replace(unicode, unicodeValues.get(unicode));
        }
        newSql = newSql.replace("U&", "");
        return newSql;
    }

    public static String getHotTableAuthorityToken(){
        // ds_api
        String token = SC.v("hot.table.api.token", "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJhdWQiOiJkc19hcGkiLCJlbWFpbCI6ImRzX2FwaUB0dWh1LmNuIn0.YvsVRStIJghKFJxAxNcmg434cGTh9VcTBOxr5vgHgJA");
        return token;
    }

    public static String getHotTableOwner(){
        String owner = SC.v("hot.table.owner", "contributor");
        return owner;
    }

    public static String getDataStudioServerApiUrl(){
        String url = SC.v("datastudio.server.api", "");
        return url;
    }

    /**
     * 纠正语法
     * @param sql
     * @return
     */
    public static String rectifySyntax(String sql, String rectifySyntaxRule) {
        if(StringUtil.isEmpty(sql) || BIUtil.isEmpty(rectifySyntaxRule)){
            return sql;
        }
        String rules[] = rectifySyntaxRule.split(";");
        for(String rule : rules){
            String[] items = rule.split("=");
            String regexp = items[0];
            String replacement = items[items.length - 1];
            if(items.length < 2){
                replacement = "";
            }
            sql = sql.replaceAll("(?i)" + regexp, replacement);
        }
        return sql;
    }

    public static List<String> buildDataFileListByMonth(String hotTableName, String startDateStr, String endDateStr) {
        DateTime startDate = DateUtil.parseDate(startDateStr);
        DateTime endDate = DateUtil.parseDate(endDateStr);
        List<String> yearMonths = new ArrayList<>();
        while(startDate.getTime() <= endDate.getTime()){
            String yearMonth = startDate.toString("yyyy-MM");
            if(!yearMonths.contains(yearMonth)){
                yearMonths.add(yearMonth);
            }
            startDate = DateUtil.offsetDay(startDate, 1);
        }

        List<String> fileList = new ArrayList<>();
        if(BIUtil.isEmpty(yearMonths)){
            return fileList;
        }

        List<HotTableInfo> hotTables = HotTableCacheManager.getHotTablesByName(hotTableName);
        if(BIUtil.isEmpty(hotTables)){
            return fileList;
        }
        String partitionFieldName = "";
        HotTableSource source = hotTables.get(0).getHotTableSource();
        if(source != null){
            partitionFieldName = source.getPartitionBy();
        }
        if(BIUtil.isEmpty(partitionFieldName)){
            return fileList;
        }
        String[] tableInfos = hotTableName.split("\\.");
        String hotTableSchema = tableInfos[0];
        String hotTableShortName = tableInfos[tableInfos.length - 1];
        for(String yearMonth : yearMonths){
            String file = String.format("hdfs://ahdpns/user/hive/warehouse/%s.db/%s/%s=%s-*/*", hotTableSchema, hotTableShortName, partitionFieldName, yearMonth);
            fileList.add(file);
        }
        return fileList;
    }

    /**
     * 格式化批量补数脚本：按批次输出
     */
    public static String formatBatchRepairScript(EtlTableDdl ddl, HotTableInfo hotTableInfo){
        if(true){
            // 仅本地测试使用：用于生成补数脚本
            // return "";
        }
        StringBuilder html = new StringBuilder();
        DateTime startDate = DateUtil.offset(DateUtil.parseDate(hotTableInfo.getDataMinDate()), DateField.DAY_OF_YEAR, -1);
        Long hotDays = DateUtil.between(DateUtil.parseDate(hotTableInfo.getDataMinDate()), DateUtil.parseDate(hotTableInfo.getDataMaxDate()), DateUnit.DAY) + 1;
        if(hotDays >= hotTableInfo.getHotTableSource().getHotDataRequireDays()){
            return "";
        }
        Integer repairDays = hotTableInfo.getHotTableSource().getHotDataRequireDays() - hotDays.intValue();
        DateTime endDate =  DateUtil.offset(startDate, DateField.DAY_OF_YEAR, -1 * repairDays);//DateUtil.offset(DateUtil.date(), DateField.DAY_OF_YEAR, -750);

        DBType dbType = DBType.getType(hotTableInfo.getHotDbEngine());
        List<String> titles = new ArrayList<>();
        List<String> contents = new ArrayList<>();
        // 按30天进行分配补数
        while(startDate.getTime() > endDate.getTime()){
            String script = ddl.getRepairScriptContent();
            int batchDays = hotTableInfo.getHotTableSource().getRepairDataBatchDays();
            DateTime batchEndDate = DateUtil.offset(startDate,  DateField.DAY_OF_YEAR, -1 * batchDays);

            String startStr = startDate.toString("yyyy-MM-dd");
            String endStr = batchEndDate.toString("yyyy-MM-dd");

            String scriptType = DBType.getType(hotTableInfo.getHotDbEngine()) == DBType.Trino ? "hive" : "doris";
            String batchTitle = String.format("%s(%s~%s)", scriptType, endStr, startStr);
            titles.add(batchTitle);

            StringBuilder contentHtml = new StringBuilder();

            contentHtml.append(String.format("<h2 id='%s'>%s</h2>", batchTitle, batchTitle));
            if(dbType == DBType.Trino){
                // 替换where
                script = StringUtils.replaceOnce(script,"${hivevar:v_date}", endStr);
                script = StringUtils.replaceOnce(script,"${hivevar:v_date}", startStr);
            }
            if(dbType == DBType.Doris){
                // 替换delete
                script = StringUtils.replaceOnce(script,"${v_date}", endStr);
                script = StringUtils.replaceOnce(script,"${v_date}", startStr);

                // 替换where
                script = StringUtils.replaceOnce(script,"${v_date}", endStr);
                script = StringUtils.replaceOnce(script,"${v_date}", startStr);

                // v_label
                String loadLabel = hotTableInfo.getEtlJobName() + "_"+ batchEndDate.toString("yyyyMMdd") + "_" + startDate.toString("yyyyMMdd");
                script = StringUtils.replaceOnce(script,"${v_label}", loadLabel);

                // data infile
                String dataInFiles = BIUtil.listToStr(buildDataFileListByMonth(hotTableInfo.getHotTableName(), endStr, startStr), ",", "\"");
                String replacement = String.format(" infile(%s)", dataInFiles);
                String pattern = " infile\\([^\\)]*\\)";
                script = script.replaceAll(pattern, replacement);

                String showLoadScript = String.format("-- show load from bi_hot where label = '%s';", loadLabel);

                script = script + "\n" + showLoadScript;
            }
            //script = script.replace("\n","<br/>");
            // 输出
            contentHtml.append("<table style='width:100%'><tr><td style='width:100%'><textarea style='width:100%;height:300px;'>" + script + "</textarea></td></tr></table>");
            startDate = DateUtil.offset(batchEndDate,  DateField.DAY_OF_YEAR, -1);;

            contents.add(contentHtml.toString());
        }

        html.append("<!DOCTYPE html><html lang=\"en\"><head><meta charset=\"UTF-8\">");
        html.append("<style>html, body {margin: 0;padding: 0;height: 100%;font-size:12px;}  .main {display: flex;height: 100%;}  .dir {padding: 10px;flex: 1;overflow: auto;border-right: 1px solid #cccccc;display: flex;flex-direction: column;}  .dir > a {margin-bottom: 10px;}  .content {padding: 10px;flex: 6;overflow: auto;}</style>");
        html.append(String.format("<title>%s-%s(补数)</title>", dbType == DBType.Doris ? "doris":"hive" , hotTableInfo.getSourceTableName()));
        html.append("</head>");
        html.append("<body><div class=\"main\">");
        // title
        html.append("<div class=\"dir\">");
        titles.stream().forEach(t->html.append(String.format("<a href=\"#%s\">%s</a>", t, t)));
        html.append("</div>");

        // content
        html.append("<div class=\"content\">");
        html.append(String.format("<h1>%s</h1>",hotTableInfo.getSourceTableName() ));
        contents.stream().forEach(c->html.append(c));
        html.append("</div>");

        html.append("</div></body></html>");

        String fileName = String.format("/Users/contributor/Documents/dev/html/%s.html", hotTableInfo.getEtlJobName());
        FileUtil.writeUtf8String(html.toString(), fileName);

        System.out.println(String.format("===========补数脚本html输出成功：%s", fileName));
        return html.toString();
    }

    public static void main(String[] args) {
        String pattern = "infile\\([^\\)]*\\)";
        String sql = "load data infile(\"usr/d1\", \"usr/d2\")";
        sql = "load data infile(\"usr/d1/dt=*/*\")";

        sql = sql.replaceAll(pattern, "infile('abc/dt=2024-09-*')");
        System.out.println(sql);

        long repairDays = DateUtil.between(DateUtil.parseDate("2024-09-01"), DateUtil.parseDate("2024-09-20"), DateUnit.DAY);
        System.out.println(repairDays);
    }
}
