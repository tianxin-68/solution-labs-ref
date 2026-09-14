package com.bi.queryer.ssm.util;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import com.bi.queryer.ssm.engine.QueryContext;
import com.bi.queryer.ssm.engine.QueryEngine;
import com.bi.queryer.ssm.engine.QueryFactory;
import com.bi.queryer.ssm.engine.accelerate.hot.HotUtil;
import com.bi.queryer.ssm.engine.accelerate.route.DataSourceRouter;
import com.bi.queryer.ssm.engine.analysis.AnalysisUtil;
import com.bi.queryer.ssm.engine.analysis.cfg.AnalysisItemConfig;
import com.bi.queryer.ssm.engine.analysis.cfg.compare.AnalysisCompareConfig;
import com.bi.queryer.ssm.engine.analysis.cfg.compare.AnalysisCompareItemConfig;
import com.bi.queryer.ssm.engine.analysis.cfg.total.AnalysisTotalConfig;
import com.bi.queryer.ssm.engine.config.QueryConfigure;
import com.bi.queryer.ssm.engine.config.QuerySettings;
import com.bi.queryer.ssm.engine.config.field.FieldValue;
import com.bi.queryer.ssm.engine.config.field.QueryField;
import com.bi.queryer.ssm.engine.config.settings.style.QuerySortItem;
import com.bi.queryer.ssm.engine.config.ui.UIQueryConfigure;
import com.bi.queryer.ssm.engine.config.ui.UIQueryField;
import com.bi.queryer.ssm.engine.config.ui.normalizer.UIQueryFieldNormalizer;
import com.bi.queryer.ssm.engine.model.QueryTable;
import com.bi.queryer.ssm.engine.model.StarModel;
import com.bi.queryer.ssm.enums.*;
import com.bi.queryer.ssm.meta.*;
import com.bi.queryer.ssm.promotion.PromotionManager;
import com.bi.queryer.ssm.query.field.QueryFieldService;
import com.bi.queryer.sys.base.BaseDao;
import com.bi.queryer.sys.config.SC;
import com.bi.queryer.sys.db.DBType;
import com.bi.queryer.sys.db.DataSourceType;
import com.bi.queryer.sys.enums.Enabled;
import com.bi.queryer.sys.enums.RuntimeEnv;
import com.bi.queryer.sys.exception.BIException;
import com.bi.queryer.util.BIConsts;
import com.bi.queryer.util.BIUtil;
import com.bi.queryer.util.SpringContextUtil;
import com.bi.queryer.util.encryption.AES;
import com.bi.queryer.util.period.WeekDateUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.github.luben.zstd.Zstd;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.MurmurHash3;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * User: contributor
 * Date: 2020/2/11
 * Time: 11:25
 * Description:
 */
public abstract class SSDUtil {

    /**
     * 字段权限的所属模块
     */
    public static final String Field_Auth_Module = "ssd_field";

    /**
     * 字段权限所属维度
     */
    public static final String Field_Auth_Dim = "ssm_field";

    /**
     * 字段权限的所属模块
     */
    public static final String Ctg_Auth_Module = "ssd_ctg";

    /**
     * 字段权限所属维度
     */
    public static final String Ctg_Auth_Dim = "ssm_ctg";

    /**
     * 字段权限所属维度(财务)
     */
    public static final String Ctg_Auth_Dim_Fms = "ssd_fms_ctg";

    /**
     * 敏感字段权限维度
     */
    public static final String Field_Sensitive_Auth_Dim = "ssd_sensitive_field";


    /**
     * 空值标识
     */
    public static final String Null_String = "[null]";

    /**
     * map转String
     *
     * @param map
     * @return
     */
    public static String getMapToString(Map<String, String> map) {
        Set<String> keySet = map.keySet();
        //将set集合转换为数组
        String[] keyArray = keySet.toArray(new String[keySet.size()]);
        //给数组排序(升序)
        Arrays.sort(keyArray);
        //因为String拼接效率会很低的，所以转用StringBuilder
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < keyArray.length; i++) {
            sb.append(keyArray[i]).append(":").append(String.valueOf(map.get(keyArray[i])).trim());
            if (i != keyArray.length - 1) {
                sb.append(",");
            }
        }
        return sb.toString();
    }

    /**
     * 总体校验字段之间是否有互斥
     *
     * @param config
     * @return
     */
    public static Map<String, Object> checkFieldExclude(QueryConfigure config) {
        Map<String, Object> resultMap = new HashMap<>();

        //long start = System.currentTimeMillis();
        /*
        ValidateResult validateResult = QueryConfigureValidator.validate(config);
        if(!validateResult.isSuccess()){
            //resultMap.put("flag", false);
            //resultMap.put("message", validateResult.getMessage());
        }
        */
        /*
        //将config转为codeList
        List<QueryField> allFields = new ArrayList<>();
        allFields.addAll(config.getResult().getFields());
        allFields.addAll(config.getFilter().getFields());
        Set<String> fieldIdList = new HashSet<>();
        allFields.forEach(e -> {
            fieldIdList.add(e.getCode());
        });
        if (fieldIdList == null || fieldIdList.isEmpty() || fieldIdList.size() < 2) {
            return resultMap;
        }

        //外层循环
        for (int i = 0; i < fieldIdList.size(); i++) {
            //取字段互斥的所有字段
            Set<String> excludeCodes = SSDMetaCacheManager.getExcludeFieldCodes(fieldCodes.get(i));
            //内循环判断
            for (int j = i + 1; j < fieldCodes.size(); j++) {
                if (excludeCodes.contains(fieldCodes.get(j))) {
                    List<MetaField> fieldList1 = SSDMetaCacheManager.getFieldByCode(fieldCodes.get(i));
                    List<MetaField> fieldList2 = SSDMetaCacheManager.getFieldByCode(fieldCodes.get(j));
                    String resultStr = "";
                    if (!fieldList1.isEmpty()) {
                        resultStr = "'" + fieldList1.get(0).getTitle() + "'";
                    }
                    if (!fieldList2.isEmpty()) {
                        if (StringUtils.isNotBlank(resultStr)) {
                            resultStr += "与";
                        }
                        resultStr += "'" + fieldList2.get(0).getTitle() + "'";
                    }
                    long end1 = System.currentTimeMillis();
//                    throw new SSDException( resultStr + " 存在互斥关系，请检查！");
                    resultMap.put("flag", false);
                    resultMap.put("message", resultStr + " 存在互斥关系。请联系@数据产品技术支持 协助解决，不要重复点击哦~");
                    return resultMap;
                }
            }
        }

        // 自定义字段互斥校验
        Map<String, List<String>> exceptionExcludeTitles = CustomFieldManager.checkAllExclude(config);
        if (BIUtil.isNotEmpty(exceptionExcludeTitles)) {
            List<String> tipList = new ArrayList<>();
            exceptionExcludeTitles.forEach((k, v) -> {
                tipList.add(k + "不能与[" + BIUtil.listToStr(v) + "]一起查询");
            });
            String tips = BIUtil.listToStr(tipList, ";");
//            throw new SSDException("字段互斥错误：" + tips);
            resultMap.put("flag", false);
            resultMap.put("message", "字段互斥错误：" + tips);
            return resultMap;
        }
        */
        return resultMap;
    }

    /**
     * 总体校验字字段是否存在
     *
     * @param config
     * @return
     */
    public static Map<String, Object> checkFieldExist(QueryConfigure config) throws BIException {
        Map<String, Object> resultMap = new HashMap<>();

        if (!getCheckQueryRulesFlag()) {
            return resultMap;
        }

        //将config转为codeList
        List<QueryField> allFields = new ArrayList<>();
        HashMap<String, QueryField> allFieldsMap = new HashMap<>();
        allFields.addAll(config.getResult().getFields());
        allFields.addAll(config.getFilter().getFields());
        List<String> fieldIdsAll = new ArrayList<>();

        // 去掉分析字段
        allFields = allFields.stream().filter(f -> !Enabled.value(f.getIsAnalysis())).collect(Collectors.toList());

        allFields.forEach(e -> {
            //去掉计算字段字段
            if (!e.getCode().contains(BIConsts.Custom_Field_Name_Suffix)) {
                fieldIdsAll.add(e.getId());
                allFieldsMap.put(e.getId(), e);
            }
        });

        //去重
        List<String> fieldIds = (List<String>) fieldIdsAll.stream().distinct().collect(Collectors.toList());

        if (fieldIds == null || fieldIds.isEmpty() || fieldIds.size() < 1) {
            return resultMap;
        }

        //校验没有的字段
        String returnStr = "";
        for (int i = 0; i < fieldIdsAll.size(); i++) {
            //取字段信息
            MetaField metaField = SSDMetaCacheManager.getField(fieldIdsAll.get(i));
            if (metaField == null) {
                returnStr += allFieldsMap.get(fieldIdsAll.get(i)).getTitle() + ";";
            }

        }

        if (BIUtil.isNotEmpty(returnStr)) {
            resultMap.put("flag", false);
            resultMap.put("message", "查询失败：查询字段[" + returnStr + "]不存在！");
        }

        return resultMap;
    }

    /**
     * 总体校验模型是否超过3个
     *
     * @param config
     * @return
     */
    public static Map<String, Object> checkModelsSize(QueryConfigure config) throws BIException {

        Map<String, Object> resultMap = new HashMap<>();

        if (!getCheckQueryRulesFlag()) {
            return resultMap;
        }

        //将config转为codeList
        List<QueryField> allFields = new ArrayList<>();
        allFields.addAll(config.getResult().getFields());
        allFields.addAll(config.getFilter().getFields());
        List<String> fieldIdsAll = new ArrayList<>();
        allFields.forEach(e -> {
            fieldIdsAll.add(e.getId());
        });

        //去重
        List<String> fieldIds = (List<String>) fieldIdsAll.stream().distinct().collect(Collectors.toList());

        if (fieldIds == null || fieldIds.isEmpty() || fieldIds.size() < 1) {
            return resultMap;
        }

        Set<String> categorySet = new HashSet<>();
        for (int i = 0; i < fieldIdsAll.size(); i++) {
            //取字段信息
            MetaField metaField = SSDMetaCacheManager.getField(fieldIdsAll.get(i));
            if (metaField != null && StringUtils.isNotBlank(metaField.getCategoryId())) {
                categorySet.add(metaField.getCategoryId());
            }
        }

        if (BIUtil.isNotEmpty(categorySet) && categorySet.size() > 3) {
            resultMap.put("flag", false);
            resultMap.put("message", "查询字段不能超过3个模块目录，请联系@数据产品技术支持，协助查看！");
        }

        return resultMap;
    }

    /**
     * 校验过滤时间内，数据是否准备好
     *
     * @return
     */
    public static DataAvailableTimeResp checkDataAvailableTime(QueryEngine engine) {

        DataAvailableTimeResp dataAvailableTimeResp = new DataAvailableTimeResp();

        if (!getCheckDataAvailableTimeFlag()) {
            return dataAvailableTimeResp;
        }

        QuerySettings querySettings = engine.getConfig().getSettings();
        Map<String, Date> etlJobUpdateTimeMap = querySettings.getEtlJobUpdateTimeMap();
        String lastDateAvailableTime = querySettings.getLastDateAvailableTime();

        //数据可用时间为空，不处理
        if (lastDateAvailableTime == null) {
            return dataAvailableTimeResp;
        }

        dataAvailableTimeResp.setLastDateAvailableTime(lastDateAvailableTime);

        DateGranularity dateGranularity = DateGranularity.get(engine.getConfig().getSettings().getDateGranularity());
        if (DateGranularity.MONTH == dateGranularity ||  DateGranularity.QUARTER == dateGranularity || DateGranularity.YEAR == dateGranularity ) {
            return dataAvailableTimeResp;
        }

        //4. 查询条件中最大的日期
        Date filterDateTime = getMaxFilterDateTime(engine.getConfig());

        if (filterDateTime == null) {
            return dataAvailableTimeResp;
        }

        //格式化日期
        filterDateTime = DateUtil.parseDate(new SimpleDateFormat("yyyy-MM-dd").format(filterDateTime));
        dataAvailableTimeResp.setMaxFilterDateTime(com.bi.queryer.util.period.DateUtil.toDateStr(filterDateTime));

        //5. 查询未就绪的字段
        List<JSONObject> unAvailableFieldList = new ArrayList<>();
        for (StarModel starModel : engine.getModels()) {
            if (CollUtil.isEmpty(starModel.getFactTable().getMeta().getEtlJobs())) {
                continue;
            }

            for (String etljob : starModel.getFactTable().getMeta().getEtlJobs()) {

                Date lastEndTime = etlJobUpdateTimeMap.get(etljob);
                if (lastEndTime == null) {
                    continue;
                }

                if (lastEndTime.compareTo(filterDateTime) < 0) {
                    for (QueryField queryField : starModel.getFields()) {

                        if (queryField.isVirtual()) {
                            continue;
                        }

                        if(queryField.isCommonDate()){
                            continue;
                        }

                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put("code", queryField.getCode());
                        jsonObject.put("title", queryField.getTitle());
                        String rptDevOwner = "";
                        if (queryField.getMeta() != null) {
                            rptDevOwner = SSDMetaCacheManager.getOwnerByMetaField(queryField.getMeta());
                        }
                        jsonObject.put("dataDevOwner", rptDevOwner);

                        if (queryField.getTable() != null && queryField.getTable().getMeta() != null) {
                            jsonObject.put("tableOwner", queryField.getTable().getMeta().getTableOwner());
                        }

                        unAvailableFieldList.add(jsonObject);
                    }

                    break;
                }
            }
        }

        dataAvailableTimeResp.setUnAvailableFieldList(unAvailableFieldList);
        return dataAvailableTimeResp;
    }

    /**
     * 获取数据最大可用时间
     * @param engine
     * @return
     */
    public static String getLastDateAvailableTime(QueryEngine engine,Map<String, Date> etlJobUpdateTimeMap) {

        //目录数据类型不是离线数据，不处理
        if (CtgDataType.OFFLINE != CtgDataType.get(engine.getConfig().getSettings().getCtgDataType())) {
            return null;
        }

        Date lastDateAvailableTime = null;

        //1. 查询模块所有的依赖作业
        List<String> etlJobs = new ArrayList<>();
        for (StarModel starModel : engine.getModels()) {
            if (CollUtil.isEmpty(starModel.getFactTable().getMeta().getEtlJobs())) {
                continue;
            }
            etlJobs.addAll(starModel.getFactTable().getMeta().getEtlJobs());
        }

        if (CollUtil.isEmpty(etlJobs)) {
            return null;
        }

        // 若查询时走trino，则需过滤掉doris作业
        DataSourceType dataSourceType = DataSourceRouter.getCurrentDataSourceType();
        if (dataSourceType != null && DBType.getType(dataSourceType.getDialect()) == DBType.Trino) {
            // TODO
        }

        //2. 查询依赖作业的更新时间
        BaseDao dao = (BaseDao) SpringContextUtil.getBean("baseDao");
        List<SysEtlJobInfo> etlJobInfoList = (List<SysEtlJobInfo>) dao.queryObjectList("ssm.query.queryEtlJobUpdateTime", etlJobs, DataSourceType.ETL);
        if (CollUtil.isEmpty(etlJobInfoList)) {
            return null;
        }

        //3. 查询数据可用时间
        for (SysEtlJobInfo sysEtlJobInfo : etlJobInfoList) {

            JobType jobType = JobType.get(sysEtlJobInfo.getJobType());

            //准实时作业不处理
            if (JobType.R == jobType) {
                continue;
            }

            //结束时间为空，此处设置任务结束时间为昨日
            if (StrUtil.isEmpty(sysEtlJobInfo.getLastEndTime())) {
                String lastEndTime = new SimpleDateFormat("yyyy-MM-dd").format(DateUtil.offset(new Date(), DateField.DAY_OF_YEAR, -1));
                sysEtlJobInfo.setLastEndTime(lastEndTime);
            }

            //因为数据为T+1更新，需要往前减一天
            Date lastEndTime = DateUtil.offset(DateUtil.parse(sysEtlJobInfo.getLastEndTime()), DateField.DAY_OF_YEAR, -1);
            etlJobUpdateTimeMap.put(sysEtlJobInfo.getEtlJob(), lastEndTime);

            if (lastDateAvailableTime == null) {
                lastDateAvailableTime = lastEndTime;
            } else {
                if (lastEndTime.compareTo(lastDateAvailableTime) < 0) {
                    lastDateAvailableTime = lastEndTime;
                }
            }

        }

        String lastDateAvailableTimeStr = "";
        //日期格式化
        if (lastDateAvailableTime != null) {
            lastDateAvailableTimeStr = com.bi.queryer.util.period.DateUtil.toDateStr(lastDateAvailableTime);
        }

        return lastDateAvailableTimeStr;
    }
    
    /**
     * 获取过滤条件中最大的日期
     *
     * @param config
     * @return
     */
    public static Date getMaxFilterDateTime(QueryConfigure config) {

        Date filterDateTime = null;
        QueryField dateField = config.getFilterCommonDateField();
        if (dateField == null) {
            return null;
        }

        List<FieldValue> baseDates = FieldUtil.getFilterRealValues(dateField);
        if (BIUtil.isEmpty(baseDates)) {
            return null;
        }

        DateGranularity dateGranularity = DateGranularity.get(config.getSettings().getDateGranularity());
        filterDateTime = dateFormat(baseDates.get(baseDates.size() - 1).getId(), dateGranularity);

        //如果有自定义对比，需要判断自定义对比的时间
        AnalysisCompareConfig compare = config.getAnalysis().getCompare();
        if (!compare.isActive()) {
            return filterDateTime;
        }

        for (AnalysisCompareItemConfig item : compare.getItems()) {

            if (CollUtil.isEmpty(item.getCompareDates())) {
                continue;
            }

            Date compareMaxDate = dateFormat(item.getCompareDates().get(item.getCompareDates().size() - 1), dateGranularity);
            if (compareMaxDate.compareTo(filterDateTime) > 0) {
                filterDateTime = compareMaxDate;
            }
        }

        return filterDateTime;
    }

    public static Date dateFormat(String dateStr, DateGranularity dateGranularity) {
        Date date = null;

        if (DateGranularity.WEEK == dateGranularity) {
            date = WeekDateUtil.getWeekLastDay(dateStr);
        } else {
            date = DateUtil.parse(dateStr);
        }

        return date;
    }

    /**
     * 是否检验查询规则
     *
     * @return true检验，false不检验
     */
    public static Boolean getCheckDataAvailableTimeFlag() {
        String checkDataAvailableTimeFlag = SC.v("isCheck.query.dataAvailableTime","1");
        if (StringUtils.isBlank(checkDataAvailableTimeFlag) || !Enabled.value(checkDataAvailableTimeFlag)) {
            return false;
        }
        return true;
    }

    /**
     * 是否检验数据查询时间
     *
     * @return true检验，false不检验
     */
    public static Boolean getCheckQueryRulesFlag() {
        String checkQueryRulesFlag = SC.v("isCheck.query.rules");
        if (StringUtils.isBlank(checkQueryRulesFlag) || !Enabled.value(checkQueryRulesFlag)) {
            return false;
        }
        return true;
    }

    public static String compress(String str) {
        byte[] compressArray = Zstd.compress(str.getBytes());
        Base64 base64 = new Base64();
        String compressStr = base64.encodeAsString(compressArray);
        return compressStr;
    }

    /**
     * 解压字符串
     *
     * @param compressStr BASE64编码字符串
     * @return
     */
    public static String decompress(String compressStr) {
        if (StrUtil.isBlank(compressStr)) {
            return compressStr;
        }
        Base64 base64 = new Base64();
        byte[] compressArray = base64.decode(compressStr);
        int size = (int) Zstd.decompressedSize(compressArray);
        byte[] decompressArray = new byte[size];
        Zstd.decompress(decompressArray, compressArray);
        return new String(decompressArray);
    }

    /**
     * 返回过滤日期的范围：最小日期、最大日期（粒度：日）
     * @param configure
     * @return
     */
    public static List<String> getFilterDateRange(QueryConfigure configure) {
        List<String> range = new ArrayList<>();
        QueryField commonDateField = configure.getFilterCommonDateField();
        if (commonDateField == null) {
            return range;
        }

        List<FieldValue> valueList = new ArrayList<>();
        //兼容业务日历
        if(configure.getSettings().isBusinessCalendar()){
           List<String> promoFilterDateList =  PromotionManager.getFilterDateList(commonDateField,configure);
           if(CollUtil.isNotEmpty(promoFilterDateList)){
               valueList.add(new FieldValue(promoFilterDateList.get(0),promoFilterDateList.get(0)));
               valueList.add(new FieldValue(promoFilterDateList.get(promoFilterDateList.size()-1),promoFilterDateList.get(promoFilterDateList.size()-1)));
           }
        }else {
            valueList = commonDateField.getValues();
        }

        List<FieldValue> newValues = FieldUtil.getDateFieldFilterValues(commonDateField,valueList,configure);
        newValues.forEach(v -> {
            range.add(v.getId());
        });

        // 非分析场景：直接获取过滤值
        if (!configure.hasAnalysis()) {
            return range;
        }

        // 分析场景
        List<QueryField> measures = configure.getResult().getMeasures();
        if (BIUtil.isEmpty(measures)) {
            return range;
        }

        Set<String> calcModes = new HashSet<>();
        for (QueryField measure : measures) {
            AnalysisItemConfig itemConfig = measure.getAnalysisConfig();
            if (itemConfig == null || Enabled.isFalse(measure.getIsAnalysis())) {
                continue;
            }

            AnalysisCalcMode calcMode = itemConfig.getRawThbCalcMode();

            if (calcMode.isZb() || calcMode.isTotal()) {
                continue;
            }

            Integer compareIndex = itemConfig.getCompareIndex();

            String calcModeKey = String.format("%s_%s", calcMode.getCode(), compareIndex);
            if (calcModes.contains(calcModeKey)) {
                // 避免重复计算
                continue;
            }

            List<String> compareDateRange = AnalysisUtil.getCompareDateRange(configure, calcMode, compareIndex);
            if (CollUtil.isNotEmpty(compareDateRange) && compareDateRange.size() > 1) {

                //AnalysisUtil.getCompareDateRange 不同粒度时间格式不同
                //此处统一转化时间格式为日粒度
                List<FieldValue> fieldValueList = new ArrayList<>();
                FieldValue fieldValueStart = new FieldValue(compareDateRange.get(0), "");
                FieldValue fieldValueEnd = new FieldValue(compareDateRange.get(1), "");
                fieldValueList.add(fieldValueStart);
                fieldValueList.add(fieldValueEnd);

                List<FieldValue> dateValues = FieldUtil.getDateFieldFilterValues(commonDateField, fieldValueList,configure);
                dateValues.forEach(v -> {
                    range.add(v.getId());
                });
            }
            calcModes.add(calcModeKey);
        }

        //获取时间范围的最小日期、最大日期
        if (CollUtil.isNotEmpty(range) && range.size() > 1) {
            Collections.sort(range);

            String queryMinDate = range.get(0);
            String queryMaxDate = range.get(range.size() - 1);

            range.clear();
            range.add(queryMinDate);
            range.add(queryMaxDate);
        }

        return range;
    }

    /**
     * 获取数据环境
     * @return
     */
    public static DataEnv getDataEnv() {

        try {

            HttpServletRequest request = WebUtil.getRequest();
            String dataEnv = request.getHeader("dataEnv");

            return DataEnv.get(dataEnv);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return DataEnv.OLD_SSM;
    }

    /**
     * 获取环境
     * @return
     */
    public static Env getEnv() {
        try {

            HttpServletRequest request = WebUtil.getRequest();
            String env = "";

            if (request != null) {
                env = request.getHeader("env");
            }

            //请求头没有env参数，使用环境变量中的值
            if (StrUtil.isEmpty(env)) {
                RuntimeEnv runtimeEnv = BIUtil.getRuntimeEnv();
                if (RuntimeEnv.Product == runtimeEnv) {
                    return Env.PROD;
                } else if (RuntimeEnv.UT == runtimeEnv) {
                    return Env.UT;
                }
            }

            return Env.get(env);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return Env.PROD;
    }

    /**
     * 获取数据环境对应的数据源
     * @return
     */
    public static DataSourceType getDataEnvDataSourceType() {

        DataEnv dataEnv = getDataEnv();

        switch (dataEnv) {
            case OLD_SSM:
                return DataSourceType.Default;
            case NEW_MGP:
                //通过环境区分
                Env env = getEnv();
                if (Env.PROD == env) {
                    return DataSourceType.OLAP;
                } else {
                    return DataSourceType.OLAP_UT;
                }

        }

        return DataSourceType.Default;
    }

    /**
     * 获取MGP环境对应的数据源
     * @return
     */
    public static DataSourceType getMgpDataSourceType() {
        Env env = getEnv();
        if (Env.UT == env) {
            return DataSourceType.OLAP_UT;
        }

        return DataSourceType.OLAP;
    }


    /**
     * 获取最终查询的所有字段
     * @param configure
     * @param models
     * @return
     */
    public static List<QueryField> getFinalQueryAllFields(QueryConfigure configure, List<StarModel> models) {
        /**
        List<QueryField> fieldList = configure.getAllQueryOriginFields();

        if (BIUtil.isEmpty(fieldList)) {
            return Collections.emptyList();
        }

        // 设置字段的模块id和所属目录id
        Map<String, String> allAtomFieldModuleIds = new HashMap<>();// key=字段id，value=模块id
        for(QueryField f : fieldList){
            String ctgId = SSDMetaCacheManager.getCategoryIdByFieldIdAndModuleCtgId(f.getId(), f.getModuleCtgId());
            f.setCtgId(ctgId);
            // 若是计算字段，则先通过id mapping获取对应原子字段的模块id
            CustomFieldConfigure customFieldConfigure = f.getCustomFieldConfigure();
            if(customFieldConfigure != null && BIUtil.isNotEmpty(customFieldConfigure.getExpression())){
                Map<String, String> atomFieldModuleIds = new HashMap<>();
                List<CustomFieldExpressionIdMapping> mappings = customFieldConfigure.getExpressionIdMapping();
                if(BIUtil.isNotEmpty(mappings)){
                    mappings.stream().filter(m -> m.getId() != null && m.getModuleCtgId() != null).forEach(m -> atomFieldModuleIds.put(m.getId(), m.getModuleCtgId()));
                }
                allAtomFieldModuleIds.putAll(atomFieldModuleIds);
            }
        }
         */
        List<QueryField> fieldList = new ArrayList<>();
        if(BIUtil.isNotEmpty(models)){
            for(StarModel model : models){
                fieldList.addAll(model.getFields());
            }
        }

        fieldList.stream().forEach(f -> {
                    f.setCtgId(SSDMetaCacheManager.getCategoryIdByFieldIdAndModuleCtgId(f.getId(), f.getModuleCtgId()));
                });

        /**
        // 设置计算字段的原生字段的目录id和模块id
        fieldList.stream().filter(f -> BIUtil.isEmpty(f.getCtgId()))
                .forEach(f -> {
                    if(BIUtil.isEmpty(f.getModuleCtgId())) {
                        f.setModuleCtgId(allAtomFieldModuleIds.get(f.getId()));
                    }
                    f.setCtgId(SSDMetaCacheManager.getCategoryIdByFieldIdAndModuleCtgId(f.getId(), allAtomFieldModuleIds.get(f.getId())));
                });
        */

        // 拆分所有结果字段和过滤字段
        Map<String, QueryField> resultFields = new HashMap<>(16);
        Map<String, QueryField> filterFields = new HashMap<>(16);
        for(QueryField f : fieldList){
            if(f.getIsResult()){
                resultFields.put(f.getCode(), f);
            }
            if(f.getIsFilter()){
                filterFields.put(f.getCode(), f);
            }
        }

        // 再同步字段的结果和过滤属性
        Map<String, QueryField> allFields = new LinkedHashMap<>(16);
        for(QueryField f : fieldList){
            QueryField cp = f.clone();
            if(resultFields.containsKey(cp.getCode())){
                cp.setIsResult(true);
            }
            if(filterFields.containsKey(cp.getCode())){
                cp.setValues(filterFields.get(cp.getCode()).getValues());
                cp.setIsFilter(true);
            }
            allFields.put(cp.getId(), cp);
        }
        return new ArrayList<>(allFields.values());
    }

    /**
     * 语法修正：若是doris热引擎（从trino -> doris)时，会出现语法不兼容问题：因后台配置的自定义表达式或自定义字段可能存在，如：try(xx)，需要替换为doris语法
     * @param sql
     * @param dsType
     * @return
     */
    public static String rectifySqlBySyntaxRule(String sql, DataSourceType dsType){
        String newSql = sql;
        if(DBType.Doris == DBType.getType(dsType.getDialect())) {
            String rectifySyntaxRule = SC.v("hot.table.doris.syntax.rectify.rule", "try\\(=\\(");
            newSql = HotUtil.rectifySyntax(sql, rectifySyntaxRule);
        }
        return newSql;
    }

    /**
     * order by 使用开窗函数，存在语法问题，需要进行sql修正
     * @param sql
     * @return
     */
    public static String rectifyOrderBySql(String sql,String sqlTips) {

        boolean isEnableRectifyOrderBySql = "true".equalsIgnoreCase(SC.v("ssm.rectify.order.by.sql.enable", "true"));
        if(!isEnableRectifyOrderBySql) {
            return sql;
        }

        if (StrUtil.isEmpty(sql)) {
            return sql;
        }

        /**
         * 已经处理过了，不再处理
         */
        if(sql.contains("_sort_rn1")){
            return sql;
        }

        //没有排序不处理
        int sortByIndex = sql.indexOf(BIConsts.ORDER_BY);
        if (sortByIndex == -1) {
            return sql;
        }

        String subQueryOrderBySql = sql.substring(sortByIndex+BIConsts.ORDER_BY.length());
        if(StrUtil.isEmpty(subQueryOrderBySql)) {
            return sql;
        }

        String subQuerySql = sql.substring(0, sortByIndex);
        String orderBySql = subQueryOrderBySql;

        List<String> orderByItemList = splitOrderBy(orderBySql);

        List<String> selectOrderByItemList = new ArrayList<>();

        int idx = 1;
        for (String orderByItemSql : orderByItemList) {

            //判断是否有max() over() 排序关键字
            if (!orderByItemSql.contains("max(") || !orderByItemSql.contains("over(")) {
                continue;
            }

            //按照 desc asc 截取出排序的字段体
            String[] orderFieldNameArray = orderByItemSql.split(" desc nulls | asc nulls | DESC nulls | ASC nulls");
            if (orderFieldNameArray.length != 2) {
                continue;
            }

            //设置别名
            String fieldAlias = BIConsts.ORDER_BY_ITEM_PREFIX + idx;
            String selectItemSql = String.format("%s as %s", orderFieldNameArray[0], fieldAlias);

            selectOrderByItemList.add(selectItemSql);
            orderBySql = orderBySql.replace(orderFieldNameArray[0], fieldAlias);

            idx++;
        }

        if (CollUtil.isEmpty(selectOrderByItemList)) {
            return sql;
        }

        subQuerySql = subQuerySql.replace(sqlTips," ");

        // 获取子查询列
        /**
        List<ResultDataSetColumn> columns = DBUtil.getMetadata(subQuerySql, DataSourceRouter.getCurrentDataSourceType());
        List<String> selectClause = new ArrayList<>();
        columns.stream().forEach(c -> selectClause.add("tmp." + c.getCode()));
         */

        //重新包装sql
        String resultSql = String.format(
                " %s select tmp.*, %s from (%s) tmp %s %s",
                //" %s select %s, %s from (%s) tmp %s %s",
                sqlTips,
                //BIUtil.listToStr(selectClause),
                BIUtil.listToStr(selectOrderByItemList),
                subQuerySql,
                BIConsts.ORDER_BY,
                orderBySql
        );

        return resultSql;
    }

    public static List<String> splitOrderBy(String input) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int parenthesisLevel = 0;

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);

            if (c == '(') {
                parenthesisLevel++;
            } else if (c == ')') {
                parenthesisLevel--;
            }

            if (c == ',' && parenthesisLevel == 0) {
                // 当遇到不在括号内的逗号时，添加当前段到结果并重置
                result.add(current.toString().trim());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }

        // 添加最后一段
        if (current.length() > 0) {
            result.add(current.toString().trim());
        }

        return result;
    }


    /**
     * hash字符串
     * @param sql
     * @return
     */
    public static String hash(String sql){
        try {
            // 获取MessageDigest实例，使用SHA-256算法
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            // 计算哈希值
            byte[] hashBytes = digest.digest(sql.getBytes());
            // 将哈希值转换为十六进制字符串
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256算法不可用", e);
        }
    }

    /**
     * 是否是查询转置：通过sql查询转置
     * @param config
     * @return
     */
    public static boolean isQueryPivot(QueryConfigure config) {

        boolean isEnableQueryPivot = "true".equalsIgnoreCase(SC.v("ssm.query.pivot.enable", "true"));
        if (!isEnableQueryPivot) {
            return false;
        }

        List<QuerySortItem> metricSortItems = new ArrayList<>(8);
        for (QuerySortItem sortItem : config.getSettings().getQuerySortItems()) {
            String orderBy = sortItem.getColumnField();
            QueryField field = config.getResult().getFieldByCode(orderBy);
            if (field == null || QueryArea.Measure.equals(field.getRawQueryArea())) {
                metricSortItems.add(sortItem);
            }

        }
        boolean needSort = config.getSettings().getNeedSort();
        boolean hasMetricSortItems = BIUtil.isNotEmpty(metricSortItems);
        boolean hasColumnItems = BIUtil.isNotEmpty(config.getResult().getColDimensions());
        return needSort && hasMetricSortItems && hasColumnItems;
    }

    /**
     * 计算行列转置的grouping_id
     * @param fields
     * @return
     */
    public static String calcPivotGroupingKey(List<String> fields){
        // 注意：此处不用grouping原生函数，因为存在没有grouping sets场景下需要调用此函数，如：行列转置后需要重新计算grouping值
        //return String.format("grouping(%s)", BIUtil.listToStr(fields));
        // udf 存在性能问题，偶发集群宕机
        // String groupingIdExpression = String.format("bi_grouping_id(array(%s))", BIUtil.listToStr(fields));

        // 原始grouping_id 在 grouping sets((dim1)) + sum(case when dim1='x')场景下，查询报错
        // String groupingIdExpression = String.format("grouping_id(%s)", BIUtil.listToStr(fields));

        String groupingIdExpression = "";
        int size = fields.size();
        List<String> gropingItemValues = new ArrayList<>();
        for (int i = 0; i < size; i++){
            String field = fields.get(i);
            String groupingItemValue = String.format("if(%s is null , pow(2,%s), 0)", field, size - i );
            gropingItemValues.add(groupingItemValue);
        }
        groupingIdExpression = BIUtil.listToStr(gropingItemValues, " + ");
        if(BIUtil.isEmpty(groupingIdExpression)){
            groupingIdExpression = "0";
        }
        return groupingIdExpression;
    }

    /**
     * 是否有行总计或列总计
     * @param config
     * @return
     */
    public static boolean hasRowTotalOrColumnTotal(QueryConfigure config){
        if(!config.hasAnalysis()){
            return false;
        }
        if(!config.getAnalysis().getTotal().isActive()){
            return false;
        }
        AnalysisTotalConfig totalConfig = config.getAnalysis().getTotal();
        if(totalConfig.getItem(AnalysisTotalType.ROW_TOTAL) != null || totalConfig.getItem(AnalysisTotalType.COL_TOTAL) != null){
            return true;
        }
        return false;
    }

    /**
     * 是否有日粒度且日期不汇总时，有行列总计需要取日均值
     * @param config
     * @return
     */
    public static boolean hasTotalNeedToAvgOnDayNotAgg(QueryConfigure config) {
        QueryField commonDateField = config.getFilterCommonDateField();
        if (commonDateField == null) {
            return false;
        }
        if (DateGranularity.DAY != DateGranularity.get(commonDateField.getQueryDateGranularity()) || config.isAggQuery()) {
            return false;
        }

        //包含-按日去重后再sum聚合字段code列表，不处理
        String distinctByDayThenSumCodes = SC.v("distinct.by.day.then.sum.code.list", "");
        if (StrUtil.isNotEmpty(distinctByDayThenSumCodes)) {

            boolean isExist = false;
            List<String> distinctByDayThenSumCodeList = Arrays.asList(distinctByDayThenSumCodes.split(","));
            for (QueryField field : config.getResult().getMeasures()) {

                String code = field.getCode();
                code = code.replace("_" + AggExpressionType.Avg_By_Day.getCode(), "").replace("_" + AggExpressionType.Avg_By_Day_Real.getCode(), "");
                if (distinctByDayThenSumCodeList.contains(code)) {
                    isExist = true;
                    break;
                }
            }

            if (isExist) {
                return false;
            }
        }

        return hasRowTotalOrColumnTotal(config);
    }

    /**
     * 通过配置获取etl任务
     * @param configList
     * @return
     */
    public static List<MetaTable> getEtlJobByConfig(List<String> configList, String datasetId, Integer isNeedNormalize) {
        // LinkedHashSet：MetaTable.equals 基于 id 去重，同时保留插入顺序
        Set<MetaTable> result = new LinkedHashSet<>();

        Map<String, MetaField> fieldCodeMap = new HashMap<>();
        Map<String, MetaField> fieldIdMap = new HashMap<>();

        //查询字段树元信息
        if (Enabled.value(isNeedNormalize)) {
            QueryFieldService queryFieldService = (QueryFieldService) SpringContextUtil.getBean("queryFieldService");
            List treeNodes = queryFieldService.buildFieldTree(CategoryType.Front, datasetId);
            fetchFieldFromTreeNodes(treeNodes, fieldCodeMap, fieldIdMap);
        }

        for (String config : configList) {

            //兼容换绑的场景
            if (Enabled.value(isNeedNormalize)) {
                config = normalizeConfig(config, fieldCodeMap, fieldIdMap);
            }

            SSDQueryTemplate queryTemplate = new SSDQueryTemplate();
            queryTemplate.setConfig(config);

            QueryConfigure queryConfigure = new QueryConfigure(queryTemplate);
            queryConfigure.load();

            queryConfigure.getSettings().setAclCheck(false);
            queryConfigure.getSettings().setEnableCreateAllTableBySameCodeOpt(true);

            QueryContext cxt = new QueryContext();
            QueryEngine engine = QueryFactory.createEngine(queryConfigure, cxt);

            //收集所有事实表和维表的 MetaTable（按 id 去重）
            for (StarModel starModel : engine.getModels()) {

                MetaTable factMeta = starModel.getFactTable().getMeta();
                if (factMeta != null) {
                    result.add(factMeta);
                }

                for (QueryTable dimTable : starModel.getDimTables()) {
                    MetaTable dimMeta = dimTable.getMeta();
                    if (dimMeta != null) {
                        result.add(dimMeta);
                    }
                }
            }
        }

        //销毁字段map
        fieldCodeMap.clear();
        fieldIdMap.clear();

        return new ArrayList<>(result);
    }

    /**
     * 1 判断前端传递的字段id在字段树中是否存在
     * 2 不存在，通过code 在字段数中获取一个
     * @param config
     * @param fieldCodeMap
     * @param fieldIdMap
     * @return
     */
    public static String normalizeConfig(String config, Map<String, MetaField> fieldCodeMap, Map<String, MetaField> fieldIdMap) {

        UIQueryConfigure uiQueryConfigure = JSONObject.parseObject(config, UIQueryConfigure.class);

        UIQueryFieldNormalizer uiQueryFieldNormalizer = new UIQueryFieldNormalizer(uiQueryConfigure,fieldIdMap,fieldCodeMap);
        uiQueryFieldNormalizer.normalize();

        //20260423兼容前端保存的config中，value为空的场景
        //https://ssd-admin.example.com/ssm/#/template/2cdfebbc8b674d8d9c71354654469358?viewId=378588ce019d4df2ab42439a025f5f15
        normalizeFilterDateField(uiQueryConfigure);

        String newConfigString = JSONObject.toJSONString(uiQueryConfigure);
        return newConfigString;
    }

    /**
     * 如果配置的过滤日期范围为空，则设置默认的过滤日期范围为当前时间
     * @param uiQueryConfigure
     */
    public static void normalizeFilterDateField(UIQueryConfigure uiQueryConfigure) {

        List<UIQueryField> filterFields = uiQueryConfigure.getFilter();
        if (CollUtil.isEmpty(filterFields)) {
            return;
        }

        Optional<UIQueryField> optional = filterFields.stream()
                .filter(f -> BIConsts.DATE_CODE.equalsIgnoreCase(f.getCode()))
                .findAny();

        if (optional.isPresent()) {
            UIQueryField uiQueryField = optional.get();
            if (CollUtil.isNotEmpty(uiQueryField.getValues())) {
                return;
            }

            List<FieldValue> valueList = new ArrayList<>();
            String dateStr = buildDateByDateGranularity(uiQueryConfigure.getSetting().getDateGranularity());
            for (int i = 0; i < 2; i++) {
                FieldValue fieldValue = new FieldValue();
                fieldValue.setId(dateStr);
                fieldValue.setTitle(dateStr);
                fieldValue.setFilterShowType(FieldFilterType.DateRange.getCode());
                valueList.add(fieldValue);
            }
            uiQueryField.setValues(valueList);
        }
    }

    /**
     * 通过日期粒度获取日期
     * @param dateGranularity
     * @return
     */
    public static String buildDateByDateGranularity(String dateGranularity ){

        String date = DateUtil.today();
        DateGranularity dg = DateGranularity.get(dateGranularity);
        switch (dg){
            case WEEK:
                date = "202501";
                break;
            case MONTH:
                date = "202501";
                break;
            case QUARTER:
                date = "2025-Q1";
                break;
        }

        return date;
    }

    public static void fetchFieldFromTreeNodes(List treeNodes, Map<String, MetaField> fieldCodeMap, Map<String, MetaField> fieldIdMap){
        if(BIUtil.isEmpty(treeNodes)){
            return;
        }
        for(Object node : treeNodes){
            JSONObject nodeObject = (JSONObject) node;
            String type = nodeObject.getString("type");
            if("field".equalsIgnoreCase(type)){
                MetaField mf = JSON.parseObject(nodeObject.toJSONString(), MetaField.class);
                if(mf != null){
                    fieldCodeMap.put(mf.getCode(), mf);
                    fieldIdMap.put(mf.getId(), mf);
                }

                for(MetaField metaField:mf.getSameCodeFieldList()){
                    fieldIdMap.put(metaField.getId(), metaField);
                }

            }
            JSONArray children = nodeObject.getJSONArray("children");
            fetchFieldFromTreeNodes(children, fieldCodeMap,fieldIdMap);
        }
    }

    /**
     * 获取自定义计算指标的code和expression的map
     * @param selectFragments
     * @param config
     * @return
     */
    public static Map<String,String> buildCustomMeasureCodeExpressionMap(List<String> selectFragments,QueryConfigure config) {

        Map<String, String> customMeasureCodeExpressionMap = new HashMap<>();

        for (String selectFragment : selectFragments) {

            String code = "";
            String expression = "";

            String[] expressionArray = selectFragment.split(" as | AS ");
            if (expressionArray.length == 2) {
                code = expressionArray[1];
                expression = expressionArray[0];
            } else if (expressionArray.length == 1) {
                //没有别名的
                expressionArray = selectFragment.split("\\.");
                if (expressionArray.length == 2) {
                    code = StringUtils.trim(expressionArray[1]);
                    expression = selectFragment;
                }
            }

            //判断是否为自定义计算指标
            String customMeasureCode = code;
            Optional<QueryField> customMeasureOpt = config.getResult().getMeasures()
                    .stream()
                    .filter(measureField -> measureField.getCode().equals(customMeasureCode))
                    .filter(measureField -> measureField.isCustomMeasure())
                    .findAny();

            if (customMeasureOpt.isPresent()) {
                customMeasureCodeExpressionMap.put(customMeasureCode, expression);
            }

        }

        return customMeasureCodeExpressionMap;
    }

    /**
     * 是否为结果过滤
     * @return
     */
    public static boolean isAggFilter(QueryField field) {

        if (!field.isActive()) {
            return false;
        }

        if (!field.getIsFilter()) {
            return false;
        }

        //不可度量字段直接跳过
        if (!field.isMeasure()) {
            return false;
        }

        //由于模板可能存在没有FilterValueMode的情况
        if (FieldFilterMode.detail == FieldFilterMode.get(field.getFilterValueMode())) {
            return false;
        }

        return true;
    }

    public static String encryptStr = "AES:";

    public static String keyByteStr = "pYyPs7JhbzPpd7Hf";
    public static String decryptTplConfig(String config) {
        try {
            String tplConfig = config;
            if (tplConfig.contains(encryptStr)) {
                tplConfig = tplConfig.replace(encryptStr, "");
                byte[] keyByte = keyByteStr.getBytes("utf-8");
                tplConfig = new String(AES.decrypt(tplConfig, keyByte), "utf-8");
            }
            return tplConfig;
        } catch (Exception e) {
            return "{}";
        }
    }

    public static String decryptTplConfigFieldAsset(String fieldAsset) {
        try {
            String asset = fieldAsset;
            if (asset.contains(encryptStr)) {
                asset = asset.replace(encryptStr, "");
                byte[] keyByte = keyByteStr.getBytes("utf-8");
                asset = new String(AES.decrypt(asset, keyByte), "utf-8");
            }
            return asset;
        } catch (Exception e) {
            return "[]";
        }
    }

    public static String getQueryConfigFilterDesc(QueryConfigure config){
        List<QueryField> fields = config.getFilter().getFields();
        if(BIUtil.isEmpty(fields)){
            return null;
        }

        // 日期粒度取自查询 settings
        String dateGranularity = config.getSettings() != null
                ? config.getSettings().getDateGranularity() : null;

        JSONArray array = new JSONArray();
        for(QueryField field : fields){
            JSONObject json = new JSONObject();
            List<FieldValue> values = field.getValues();
            if(BIUtil.isEmpty(values)){
                continue;
            }
            json.put("id", field.getId());
            json.put("name", field.getName());
            json.put("code", field.getCode());
            json.put("isDim", field.isMeasure() ? 0 : 1);
            json.put("filterValueType", field.getFilterValueType());
            json.put("filterValues", field.getValues().stream().map(FieldValue::getId).collect(Collectors.toList()));
            json.put("dateGranularity", dateGranularity);
            array.add(json);
        }
        return array.toString();
    }

    public static boolean isGroupInternalDataset(String datasetId){
        String groupDatasetId = SC.v("ssm.group.internal.use.datasetId", "123c62ef6a914ba1bb4e6b28d92fde71");
        return groupDatasetId.equalsIgnoreCase(datasetId);
    }

    public static boolean isSsmDataset(String datasetId) {
        String[] ssmDatasetIds = SC.v("ssm.dataset.ids", "3a1416400ab3405badefc558edfdadbd,123c62ef6a914ba1bb4e6b28d92fde71").split(",");
        return Arrays.asList(ssmDatasetIds).contains(datasetId);
    }

    /**
     * 发送BI Queryer通消息
     * @param sign 签名
     * @param messageText
     * @param receivers
     */
    public static void sendNotification(String sign,String messageText,List<String> receivers) {

        String notification_url = "http://localhost:9010/WeiXinWork/Push/Text";
        String notification_requestID = "change-me";

        try {

            JSONObject jsonObject = new JSONObject();
            jsonObject.put("Content", messageText);
            jsonObject.put("Sign", sign);
            jsonObject.put("Users", receivers);

            String result = HttpRequest.post(notification_url)
                    .header("RequestID", notification_requestID)
                    .body(jsonObject.toJSONString())
                    .execute().body();

            System.out.println("BI Queryer通结果：" + result);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    // 判断是否包含 c1是否包含c2
    public static boolean isSuperset(List<String> c1, List<String> c2) {
        if (c1 == null || c2 == null || c1.isEmpty() || c2.isEmpty()) {
            return false;
        }

        Set<String> setA = new HashSet<>(c1);
        boolean isSuperset = c2.stream().allMatch(setA::contains);
        return isSuperset;
    }

    /**
     * 获取多个集合的交集，返回 Set（去重）
     */
    public static Set<String> getIntersection(List<List<String>> collections) {
        if (collections == null || collections.size() == 0) {
            return new HashSet<>();
        }

        List<String> first = collections.get(0);
        Set<String> intersection = new HashSet<>(first);
        for (int i = 1; i < collections.size(); i++) {
            List<String> current = collections.get(i);
            intersection.retainAll(current);
        }

        return intersection;
    }

    /**
     * 判断用户是否命中限流（在限流的阈值内）
     * @param userName
     * @param rateLimitThreshold
     * @param whiteList 白名单：永远返回false
     * @param blackList 黑名称：永远返回true
     * @return
     */
    public static boolean isHitRateLimitByUserName(String userName, Double rateLimitThreshold, List<String> whiteList, List<String> blackList){
        boolean isHit = false;
        if(BIUtil.isEmpty(userName)){
            return isHit;
        }
        if(BIUtil.isNotEmpty(whiteList) && whiteList.contains(userName)){
            return false;
        }
        if(BIUtil.isNotEmpty(blackList) &&  blackList.contains(userName)){
            return true;
        }
        Integer hashCode = Math.abs(MurmurHash3.hash32(userName.getBytes(StandardCharsets.UTF_8)));
        Integer remainder = hashCode % 100;
        Double threshold = rateLimitThreshold * 100;
        if(remainder < threshold.intValue()){
            isHit = true;
        }
        return isHit;
    }

    /**
     * 判断是否是准实时数据集
     * @param datasetId
     * @return
     */
    public static boolean isNearRealTimeDataset(String datasetId) {
        String nearRealTimeDatasetIds = SC.v("ssm.near.realtime.dataset.id.list", "");
        if (StrUtil.isNotEmpty(nearRealTimeDatasetIds)) {
            List<String> nearRealTimeDatasetIdList = Arrays.asList(nearRealTimeDatasetIds.split(","));
            if (nearRealTimeDatasetIdList.contains(datasetId)) {
                return true;
            }
        }

        return false;
    }

    public static void main(String[] args) {
        List<String> useNames = Arrays.asList("bojun", "yuelinhui","linfeng2","zhujiaojiao", "linfeng2");
        for (String name : useNames) {
            boolean f = isHitRateLimitByUserName(name, 1.0, null, null);
            System.out.println(name + "=" + f);
        }
    }

}
