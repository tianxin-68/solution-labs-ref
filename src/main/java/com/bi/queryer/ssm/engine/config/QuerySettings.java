package com.bi.queryer.ssm.engine.config;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.bi.queryer.ssm.engine.config.settings.style.QuerySortItem;
import com.bi.queryer.ssm.engine.config.settings.style.QuerySortSettings;
import com.bi.queryer.ssm.engine.config.settings.style.QueryStyleSettings;
import com.bi.queryer.ssm.enums.*;
import com.bi.queryer.ssm.query.template.enums.DataTypeEnum;
import com.bi.queryer.ssm.util.SSDUtil;
import com.bi.queryer.sys.config.SC;
import com.bi.queryer.sys.db.DataSourceType;
import com.bi.queryer.sys.enums.Enabled;
import com.bi.queryer.sys.user.UserManager;
import com.bi.queryer.sys.user.vo.User;
import com.bi.queryer.util.BIConsts;
import com.bi.queryer.util.BIUtil;
import com.alibaba.fastjson.JSON;

import java.util.*;

/**
 * @Author contributor
 * @Date 20:57 2023-08-14
 * @Description 查询设置
 **/
public class QuerySettings {

    /**
     * 最多查询行数, 前台会穿过来，默认是当前用户的值
     */
    private Integer queryRowLimit = -1;

    private Integer apiQueryRowLimit;

    /**
     * 是否隐藏空行
     */
    private Integer isHideNullColumn = Enabled.NO.getId();

    /**
     * 时间粒度
     */
    private String dateGranularity = DateGranularity.DAY.getCode();

    /**
     * 是否为时间汇总
     */
    private Integer isAggQuery = Enabled.NO.getId();

    /**
     * 查询超时时间（秒）
     */
    private Integer queryTimeoutSec = null;

    /**
     * 查询超时检测频率（秒）
     */
    private Integer queryTimeoutDetectionInterval = 10;

    /**
     * 查询数据源key
     */
    private String queryDatasourceKey = "";

    /**
     * 显示农历日期
     */
    private Integer showLunarDate = Enabled.NO.getId();

    /**
     * 是否需要排序
     */
    private Boolean isNeedSort = true;

    /**
     * 排序模式
     */
    private SortMode sortMode = SortMode.NORMAL;

    /**
     * 配置开始时间
     */
    private String configBeginTime = null;

    /**
     * 是否需要进行权限校验（构建的模型用于非数据查询场景时，不需要进行目录、字段、行级权限校验，提升查询效率）
     */
    private Boolean aclCheck = true;

    /**
     * 数据集id
     */
    private String datasetId;

    /**
     * 数据集类型 实时还是离线
     */
    private DataTypeEnum datasetType = DataTypeEnum.OFFLINE;

    /**
     * 查询模式
     */
    private QueryModeType queryModeType = QueryModeType.ALL;

    /**
     * 查询session属性
     */
    private Map<String, String> sessionProperties = new HashMap<>();

    /**
     * 查询所有日期数据：用于自定义对比日期长度与基准日期长度不一致时，查询最大长度的数据
     */
    private boolean queryAllDate = false;

    /**
     * 是否开启热表查询
     */
    private boolean enableHotTableQuery = true;

    /**
     * 是否开启主子表路由
     */
    private boolean enableTablePriSubRouter = true;

    /**
     * 是否开启数据路由
     */
    private boolean enableDataSourceRoute = true;

    /**
     * 构建模型时是否退化维度表
     */
    private boolean enableWeakenDimTableOnCreateStarModel = true;

    /**
     * 构建模型时是否补齐全量度量
     */
    private boolean enableSupplementFullMeasureOnCreateStarModel = true;

    /**
     * 最晚数据可用时间
     */
    private String lastDateAvailableTime;

    /**
     * 模型使用的作业的完成时间
     */
    private Map<String, Date> etlJobUpdateTimeMap = new HashMap<>();

    /**
     * 目录数据类型  离线 offline,实时 rt, 预测 predict
     */
    private String ctgDataType;

    /**
     * 查询来源
     */
    private String querySource = QuerySourceType.Query.getCode();

    // 查询返回格式
    private String responseFormat = "map";

    /**
     * 是否为lod查询
     */
    private Integer isLodQuery = Enabled.NO.getId();

    /**
     * 表格样式
     */
    private QueryStyleSettings tableStyle;

    /**
     * 是否过滤空行
     */
    private Boolean filterEmptyLine = false;

    /**
     * 查询原始值：即最终结果中返回原始值（格式化之前）
     */
    private Boolean queryRawValue = false;

    /**
     * 是否为大模型查询
     */
    private Integer isAgentQuery = Enabled.NO.getId();

    /**
     * 日历类型
     */
    private String calendarType = CalendarType.NATURAL.getCode();

    /**
     * 业务日历的年份
     */
    private Integer promoYear;

    /**
     * 数据截止时间
     */
    private String dataSnapshotDate;

    /**
     * 用户设备agent信息
     */
    private String userAgent;

    /**
     * 是否显示对比日期
     */
    private Integer showDateRemark = Enabled.YES.getId();

    // 是否使用相同code获取所有表
    private boolean enableCreateAllTableBySameCodeOpt = false;

    /**
     * 数据分片粒度
     */
    private String dataSliceGranularity = "";

    /**
     * 表数据更新时间
     */
    private Map<String,String> tableDataUpdateTimeMap = new HashMap<>();

    /**
     * 对外提供api的鉴权码
     */
    private String olapApiKey;

    // 是否系统级 olap-api key。系统 key 在 QuerySessionManager 跳过 Redis 分布式限流锁。
    // 值由 OlapApiQueryConfigureNormalizer 根据 olap_api_key 表 is_system 字段写入。
    private Integer isSystemOlapApiKey = Enabled.NO.getId();

    /**
     * 客户端机器名
     */
    private String hostname;

    /**
     * 是否模板使用在AI配置
     */
    private Integer isTplUseInAIConfig = Enabled.NO.getId();

    /**
     * 巡检数据源标识
     */
    private String inspectDsKey;

    public Integer getQueryRowLimit() {
        return queryRowLimit;
    }

    public void setQueryRowLimit(Integer queryRowLimit) {
        this.queryRowLimit = queryRowLimit;
    }

    public Integer getIsHideNullColumn() {
        return isHideNullColumn;
    }

    public void setIsHideNullColumn(Integer isHideNullColumn) {
        this.isHideNullColumn = isHideNullColumn;
    }

    public String getDateGranularity() {
        return dateGranularity;
    }

    public void setDateGranularity(String dateGranularity) {
        this.dateGranularity = dateGranularity;
    }

    public Integer getIsAggQuery() {
        return isAggQuery;
    }

    public void setIsAggQuery(Integer isAggQuery) {
        this.isAggQuery = isAggQuery;
    }

    public Integer getQueryTimeoutSec() {
        if(queryTimeoutSec == null || queryTimeoutSec == 0) {
            queryTimeoutSec = BIConsts.QUERY_TIME_OUT_SEC;
        }
        if(queryTimeoutSec >= BIConsts.QUERY_MAX_TIME_OUT_SEC){
            queryTimeoutSec = BIConsts.QUERY_MAX_TIME_OUT_SEC;
        }
        return queryTimeoutSec;
    }

    public void setQueryTimeoutSec(Integer queryTimeoutSec) {
        this.queryTimeoutSec = queryTimeoutSec;
    }

    public Integer getShowLunarDate() {
        return showLunarDate;
    }

    public void setShowLunarDate(Integer showLunarDate) {
        this.showLunarDate = showLunarDate;
    }

    public Boolean getNeedSort() {
        return isNeedSort;
    }

    public void setNeedSort(Boolean needSort) {
        isNeedSort = needSort;
    }

    public String getConfigBeginTime() {
        return configBeginTime;
    }

    public void setConfigBeginTime(String configBeginTime) {
        this.configBeginTime = configBeginTime;
    }

    public Boolean getAclCheck() {
        return aclCheck;
    }

    public void setAclCheck(Boolean aclCheck) {
        this.aclCheck = aclCheck;
    }

    public Map<String, String> getSessionProperties() {
        return sessionProperties;
    }

    public void setSessionProperties(Map<String, String> sessionProperties) {
        this.sessionProperties = sessionProperties;
    }

    public SortMode getSortMode() {
        return sortMode;
    }

    public void setSortMode(SortMode sortMode) {
        this.sortMode = sortMode;
    }

    public Integer getQueryTimeoutDetectionInterval() {
        if(queryTimeoutDetectionInterval == null || queryTimeoutDetectionInterval <= 0){
            queryTimeoutDetectionInterval = 10;
        }
        return queryTimeoutDetectionInterval;
    }

    public void setQueryTimeoutDetectionInterval(Integer queryTimeoutDetectionInterval) {
        this.queryTimeoutDetectionInterval = queryTimeoutDetectionInterval;
    }

    public String getQueryDatasourceKey() {
        if(BIUtil.isEmpty(queryDatasourceKey)){
            queryDatasourceKey = DataSourceType.Trino_Master.getKey();
        }
        return queryDatasourceKey;
    }

    public void setQueryDatasourceKey(String queryDatasourceKey) {
        this.queryDatasourceKey = queryDatasourceKey;
    }

    public String getDatasetId() {
        if(StrUtil.isEmpty(datasetId)){
            return SC.v("ssm.default.datasetId", "");
        }
        return datasetId;
    }

    public void setDatasetId(String datasetId) {
        this.datasetId = datasetId;
    }

    public QueryModeType getQueryModeType() {
        return queryModeType;
    }

    public void setQueryModeType(QueryModeType queryModeType) {
        this.queryModeType = queryModeType;
    }

    public boolean isQueryAllDate() {
        return queryAllDate;
    }

    public void setQueryAllDate(boolean queryAllDate) {
        this.queryAllDate = queryAllDate;
    }

    public boolean isEnableHotTableQuery() {
        return enableHotTableQuery;
    }

    public void setEnableHotTableQuery(boolean enableHotTableQuery) {
        this.enableHotTableQuery = enableHotTableQuery;
    }

    public boolean isEnableDataSourceRoute() {
        return enableDataSourceRoute;
    }

    public void setEnableDataSourceRoute(boolean enableDataSourceRoute) {
        this.enableDataSourceRoute = enableDataSourceRoute;
    }

    public boolean isEnableWeakenDimTableOnCreateStarModel() {
        return enableWeakenDimTableOnCreateStarModel;
    }

    public void setEnableWeakenDimTableOnCreateStarModel(boolean enableWeakenDimTableOnCreateStarModel) {
        this.enableWeakenDimTableOnCreateStarModel = enableWeakenDimTableOnCreateStarModel;
    }

    public boolean isEnableSupplementFullMeasureOnCreateStarModel() {
        return enableSupplementFullMeasureOnCreateStarModel;
    }

    public void setEnableSupplementFullMeasureOnCreateStarModel(boolean enableSupplementFullMeasureOnCreateStarModel) {
        this.enableSupplementFullMeasureOnCreateStarModel = enableSupplementFullMeasureOnCreateStarModel;
    }

    public String getLastDateAvailableTime() {
        return lastDateAvailableTime;
    }

    public void setLastDateAvailableTime(String lastDateAvailableTime) {
        this.lastDateAvailableTime = lastDateAvailableTime;
    }

    public Map<String, Date> getEtlJobUpdateTimeMap() {
        return etlJobUpdateTimeMap;
    }

    public void setEtlJobUpdateTimeMap(Map<String, Date> etlJobUpdateTimeMap) {
        this.etlJobUpdateTimeMap = etlJobUpdateTimeMap;
    }

    public String getCtgDataType() {
        return ctgDataType;
    }

    public void setCtgDataType(String ctgDataType) {
        this.ctgDataType = ctgDataType;
    }

    public String getQuerySource() {
        return querySource;
    }

    public void setQuerySource(String querySource) {
        this.querySource = querySource;
    }


    public boolean isEnableTablePriSubRouter() {
        return enableTablePriSubRouter;
    }

    public void setEnableTablePriSubRouter(boolean enableTablePriSubRouter) {
        this.enableTablePriSubRouter = enableTablePriSubRouter;
    }

    public Integer getIsLodQuery() {
        return isLodQuery;
    }

    public void setIsLodQuery(Integer isLodQuery) {
        this.isLodQuery = isLodQuery;
    }

    public QueryStyleSettings getTableStyle() {
        return tableStyle;
    }

    public void setTableStyle(QueryStyleSettings tableStyle) {
        this.tableStyle = tableStyle;
    }


    public List<QuerySortItem> getQuerySortItems() {

        List<QuerySortItem> querySortItems = new ArrayList<>();

        if (this.tableStyle == null) {
            return querySortItems;
        }

        //集团内部数据集，因为使用复杂视图，与当前排序规则max() over() 不兼容，暂不排序
        //String groupDatasetId = SC.v("ssm.group.internal.use.datasetId", "123c62ef6a914ba1bb4e6b28d92fde71");
        //if(groupDatasetId.equalsIgnoreCase(this.datasetId)){
        if(SSDUtil.isGroupInternalDataset(this.datasetId)){
            return querySortItems;
        }

        //判断是否开启后台上下排序
        boolean isEnableUpDownSort = "true".equalsIgnoreCase(SC.v("ssm.query.up.down.sort.enable", "true"));
        if(!isEnableUpDownSort) {
            return querySortItems;
        }

        QuerySortSettings querySortSettings = this.tableStyle.getUpDownSortData();
        if (querySortSettings == null || Enabled.isFalse(querySortSettings.getIsActive())) {
            return querySortItems;
        }

        if (CollUtil.isEmpty(querySortSettings.getSortItems())) {
            return querySortItems;
        }

        querySortItems.addAll( querySortSettings.getSortItems());
        return querySortItems;
    }

    /**
     * 获取排序的字段编码
     * @return
     */
    public List<String> getQuerySortFieldCodes(QueryConfigure configure) {

        List<String> orderByFieldCode = new ArrayList<>();

        Boolean isQueryPivot = SSDUtil.isQueryPivot(configure);

        List<QuerySortItem> querySortItems = this.getQuerySortItems();
        if (CollUtil.isNotEmpty(querySortItems)) {
            for (QuerySortItem querySortItem : querySortItems) {
                orderByFieldCode.add(querySortItem.getOrderBy());

                if (StrUtil.isNotEmpty(querySortItem.getOrderEntity()) && isQueryPivot) {
                    orderByFieldCode.add(querySortItem.getOrderEntity());
                }
            }
        }

        return orderByFieldCode;
    }

    /**
     * 获取最大可用数据时间
     * @return
     */
    public DateTime getLastAvailableDate() {

        CtgDataType ctgDataType = CtgDataType.get(this.getCtgDataType());

        //实时取今天
        if(CtgDataType.RT == ctgDataType ) {
            DateTime lastDay = DateUtil.parse(DateUtil.today());
            return lastDay;
        }

        //预测取2999-12-31
        if( CtgDataType.PREDICT == ctgDataType) {
            return DateUtil.parse(BIConsts.MAX_END_DATE);
        }

        DateTime lastDay = DateUtil.yesterday();

        //最晚数据可用时间为空，取昨天
        if(StrUtil.isEmpty(lastDateAvailableTime)){
            return lastDay;
        }

        //查询配置的最晚数据可用时间 小于昨天，取查询配置的最晚数据可用时间
        DateTime lastAvailableDate = DateUtil.parse(lastDateAvailableTime);
        if (lastAvailableDate.getTime() < lastDay.getTime()) {
            lastDay = lastAvailableDate;
        }

        return lastDay;
    }

    public Boolean getFilterEmptyRow() {
        return filterEmptyLine;
    }

    public void setFilterEmptyRow(Boolean filterEmptyRow) {
        this.filterEmptyLine = filterEmptyRow;
    }

    public Boolean getFilterEmptyLine() {
        return filterEmptyLine;
    }

    public void setFilterEmptyLine(Boolean filterEmptyLine) {
        this.filterEmptyLine = filterEmptyLine;
    }

    public QuerySettings clone() {
        QuerySettings querySettings = JSON.parseObject(JSON.toJSONString(this), QuerySettings.class);
        return querySettings;
    }

    public Boolean getQueryRawValue() {
        return queryRawValue;
    }

    public void setQueryRawValue(Boolean queryRawValue) {
        this.queryRawValue = queryRawValue;
    }

    public String getResponseFormat() {
        return responseFormat;
    }

    public void setResponseFormat(String responseFormat) {
        this.responseFormat = responseFormat;
    }

    public Integer getIsAgentQuery() {
        return isAgentQuery;
    }

    public void setIsAgentQuery(Integer isAgentQuery) {
        this.isAgentQuery = isAgentQuery;
    }

    public String getCalendarType() {
        return calendarType;
    }

    public void setCalendarType(String calendarType) {
        this.calendarType = calendarType;
    }

    public Integer getPromoYear() {
        return promoYear;
    }

    public void setPromoYear(Integer promoYear) {
        this.promoYear = promoYear;
    }

    public String getDataSnapshotDate() {
        return dataSnapshotDate;
    }

    public void setDataSnapshotDate(String dataSnapshotDate) {
        this.dataSnapshotDate = dataSnapshotDate;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public Integer getShowDateRemark() {
        return showDateRemark;
    }

    public void setShowDateRemark(Integer showDateRemark) {
        this.showDateRemark = showDateRemark;
    }

    public DataTypeEnum getDatasetType() {
        return datasetType;
    }

    public void setDatasetType(DataTypeEnum datasetType) {
        this.datasetType = datasetType;
    }

    public String getDataSliceGranularity() {
        return dataSliceGranularity;
    }

    public void setDataSliceGranularity(String dataSliceGranularity) {
        this.dataSliceGranularity = dataSliceGranularity;
    }

    /**
     * 是否是业务日历
     * @return
     */
    public boolean isBusinessCalendar() {

        if (CalendarType.BUSINESS == CalendarType.get(this.calendarType)) {
            return true;
        }

        return false;
    }

    public boolean isEnableCreateAllTableBySameCodeOpt() {
        return enableCreateAllTableBySameCodeOpt;
    }

    public boolean enableCreateAllTableBySameCodeOpt() {
        User user = UserManager.get();
        boolean getAllTableBySameCodeOpt = "true".equalsIgnoreCase(SC.v("create.all.table.by.same.code", "true"));
        String blacklist = SC.v("create.all.table.by.same.code.blacklist", "");
        List<String> blackUserList = Arrays.asList(blacklist.split(","));
        return isEnableCreateAllTableBySameCodeOpt() && getAllTableBySameCodeOpt && user != null && !blackUserList.contains(user.getName());
    }

    public void setEnableCreateAllTableBySameCodeOpt(boolean enableCreateAllTableBySameCodeOpt) {
        this.enableCreateAllTableBySameCodeOpt = enableCreateAllTableBySameCodeOpt;
    }
    /**
     * 是否是实时数据集
     * @return
     */
    public boolean isRtDataset(){
        return DataTypeEnum.REAL_TIME == this.getDatasetType();
    }

    public Map<String, String> getTableDataUpdateTimeMap() {
        return tableDataUpdateTimeMap;
    }

    public void setTableDataUpdateTimeMap(Map<String, String> tableDataUpdateTimeMap) {
        this.tableDataUpdateTimeMap = tableDataUpdateTimeMap;
    }

    public String getOlapApiKey() {
        return olapApiKey;
    }

    public void setOlapApiKey(String olapApiKey) {
        this.olapApiKey = olapApiKey;
    }

    public Integer getIsSystemOlapApiKey() {
        return isSystemOlapApiKey;
    }

    public void setIsSystemOlapApiKey(Integer isSystemOlapApiKey) {
        this.isSystemOlapApiKey = isSystemOlapApiKey;
    }

    // 判断当前查询是否使用系统级 olap-api key
    public boolean isSystemOlapApiKey() {
        return Enabled.value(isSystemOlapApiKey);
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public Integer getIsTplUseInAIConfig() {
        return isTplUseInAIConfig;
    }

    public void setIsTplUseInAIConfig(Integer isTplUseInAIConfig) {
        this.isTplUseInAIConfig = isTplUseInAIConfig;
    }

    public Integer getApiQueryRowLimit() {
        return apiQueryRowLimit;
    }

    public void setApiQueryRowLimit(Integer apiQueryRowLimit) {
        this.apiQueryRowLimit = apiQueryRowLimit;
    }

    public String getInspectDsKey() {
        return inspectDsKey;
    }

    public void setInspectDsKey(String inspectDsKey) {
        this.inspectDsKey = inspectDsKey;
    }
}
