package com.bi.queryer.ssm.engine.accelerate.hot.etl.doris;

import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import com.bi.queryer.ssm.engine.accelerate.hot.HotUtil;
import com.bi.queryer.ssm.engine.accelerate.hot.etl.model.EtlTableDdl;
import com.bi.queryer.ssm.enums.DateGranularity;
import com.bi.queryer.ssm.meta.accelerate.hot.HotTableInfo;
import com.bi.queryer.util.BIUtil;
import com.bi.queryer.util.period.WeekDateUtil;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @Author: contributor
 * @CreateTime: 2024-09-10  10:38
 * @Description:  doris增量脚本构建
 */
public class DorisIncrementalScriptBuilder extends DorisFullScriptBuilder {
    protected DateGranularity dateGranularity = null;
    public DorisIncrementalScriptBuilder(HotTableInfo hotTable) {
        super(hotTable);
        this.dateGranularity = DateGranularity.get(hotTable.getHotTableSource().getDateGranularity());
    }

    public EtlTableDdl buildScheduleScript(){
        // 建表
        DorisTableCreator tableCreator = new DorisTableCreator(hotTable);
        EtlTableDdl ddl = tableCreator.create();

        // 拼接按增量日期清理数据+bulk load
        List<String> fragments = new ArrayList<>();

        fragments.add(String.format("use %s; \n", hotTableSchema));

        fragments.add("\n-- ==========前置api==========\n");
        fragments.add(buildPreApiSql()+ ";");

        fragments.add(buildPreSql(ddl) + ";");
        fragments.add(" \n--splitflag=sql \n");

        fragments.add("\n-- ==========加载热表数据==========\n");
        // 构建插入sql
        String loadSql = buildBulkLoadScript(ddl);
        fragments.add(loadSql+ ";");

        fragments.add(String.format(" \n--splitflag=load,%s,%s \n", hotTableSchema, hotTableShortName));

        String scriptContent = BIUtil.listToStr(fragments, "");
        ddl.setEtlScriptContent(scriptContent);

        return ddl;
    }


    protected String buildBulkLoadScript(EtlTableDdl ddl) {
        return super.buildBulkLoadScript(ddl);
    }

    protected List<String> getSetParameters() {
        return super.getSetParameters();
    }

    protected String buildWhereSql(String partitionFieldName) {

        String whereSql = String.format(" where %s ",
                buildDateRangeSql(partitionFieldName));
        return whereSql;

    }

    /**
     * 构建前置sql
     * @return
     */
    public String buildPreSql(EtlTableDdl ddl) {

        String partitionFieldName = ddl.getPartitionField().getName();

        String preSql = "\n-- ==========前置sql处理==========\n";
        preSql += String.format(" delete from %s where %s ",
                hotTableShortName
                , buildDateRangeSql(partitionFieldName)
        );

        return preSql;
    }

    public String buildDateRangeSql(String partitionFieldName) {
        List<String> dateRange = this.getIncrementalDateRange();
        String startDate = dateRange.get(0);
        String endDate = dateRange.get(1);
        String dataRangeSql = String.format(" %s between '%s' and '%s' ",partitionFieldName
                ,startDate
                ,endDate);

        return dataRangeSql;
    }

    protected  String buildPreApiSql() {
        return super.buildPreApiSql();
    }

    /**
     * 增量时按月构建文件列表
     * @return
     */
    @Override
    protected List<String> buildDataFileList() {
        /**
        Integer dataIncrementalDays = hotTable.getHotTableSource().getDataIncrementalDays();
        DateTime startDate = DateUtil.offsetDay(DateUtil.date(), -1 * dataIncrementalDays);
        DateTime endDate = DateUtil.offsetDay(DateUtil.date(), -1);
         */
        List<String> dateRange = this.getIncrementalDateRange();
        String startDateStr = dateRange.get(0);
        String endDateStr = dateRange.get(1);
        List<String> fileList = new ArrayList<>();
        if(isPartitionByDay){
            // 日：按月读取分区文件
            fileList = HotUtil.buildDataFileListByMonth(hotTable.getHotTableName(), startDateStr, endDateStr);
            return fileList;
        }

        String formatString = hotTable.getHotTableSource().getDateFormatType();
        if(BIUtil.isEmpty(formatString)){
            formatString = dateGranularity.getFormat();
        }

        // 周、月、年：按其原始分区读读取文件
        Date startDate = null;
        Date endDate = null;

        // 周
        if (DateGranularity.WEEK == dateGranularity) {
            startDate = WeekDateUtil.getWeekFirstDay(startDateStr);
            endDate = WeekDateUtil.getWeekLastDay(endDateStr);
        }

        // 月
        if (DateGranularity.MONTH == dateGranularity) {
            startDate = DateUtil.parse(startDateStr + "01",  formatString + "dd");
            endDate = DateUtil.offset(
                    DateUtil.offset(
                            DateUtil.parse(endDateStr + "01", formatString + "dd")
                            , DateField.MONTH, 1)
                    , DateField.DAY_OF_YEAR, -1);
        }

        // 年
        if (DateGranularity.YEAR == dateGranularity) {
            startDate = DateUtil.parseDate(startDateStr + "-01-01");
            endDate = DateUtil.parseDate(endDateStr + "-12-31");
        }
        List<DateTime> dayList = DateUtil.rangeToList(startDate, endDate, DateField.DAY_OF_YEAR);

        // 转为对应日期粒度列表（去重后）
        List<String> dateStrList = new ArrayList<>();
        for(DateTime day : dayList){
            String dateStr = "";
            if(dateGranularity == DateGranularity.WEEK){
                dateStr = WeekDateUtil.getWeekId(day);
            }else {
                dateStr = day.toString(formatString);
            }
            if(!dateStrList.contains(dateStr)){
                dateStrList.add(dateStr);
            }
        }
        String partitionFieldName = "";
        if(hotTable.getHotTableSource() != null){
            partitionFieldName = hotTable.getHotTableSource().getPartitionBy();
        }
        for(String dateStr : dateStrList){
            String file = String.format("hdfs://ahdpns/user/hive/warehouse/%s.db/%s/%s=%s/*", hotTableSchema, hotTableShortName, partitionFieldName, dateStr);
            fileList.add(file);
        }
        return fileList;
    }

    /**
     * 获取增量日期范围
     * @return
     */
    protected List<String> getIncrementalDateRange(){
        Integer dataIncrementalDays = hotTable.getHotTableSource().getDataIncrementalDays();
        String startDate = "";
        String endDate = "";
        String formatString = hotTable.getHotTableSource().getDateFormatType();
        if(BIUtil.isEmpty(formatString)){
            formatString = dateGranularity.getFormat();
        }
        if(dateGranularity != DateGranularity.WEEK){
            startDate = DateUtil.offsetDay(DateUtil.date(), -dataIncrementalDays).toString(formatString);
            endDate = DateUtil.offsetDay(DateUtil.date(), -1).toString(formatString);
        }else {
            // 周粒度特殊处理
            startDate = WeekDateUtil.getWeekId(DateUtil.offsetDay(DateUtil.date(), -dataIncrementalDays));
            endDate = WeekDateUtil.getWeekId(DateUtil.offsetDay(DateUtil.date(), -1));
            /*
            if("yyyy-WW".equalsIgnoreCase(formatString)){
                startDate = startDate.substring(0,4) + "-" + startDate.substring(4,6);
                endDate = endDate.substring(0,4) + "-" + endDate.substring(4,6);
            }
             */
        }
        List<String> range = new ArrayList<>();
        range.add(startDate);
        range.add(endDate);
        return range;
    }

    public static void main(String[] args) {
        String startDate = "2024-0101";
//        startDate = startDate.substring(0,4) + "-" + startDate.substring(4,6);
//        System.out.println(startDate);
        DateTime d = DateUtil.parse(startDate, "yyyy-MMdd");
        System.out.println(d.toDateStr());
    }


}
