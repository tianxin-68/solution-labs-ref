package com.bi.queryer.ssm.engine.config;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.bi.queryer.ssm.engine.analysis.cfg.thb.AnalysisThbConfig;
import com.bi.queryer.ssm.engine.analysis.cfg.thb.AnalysisThbItemConfig;
import com.bi.queryer.ssm.engine.analysis.cfg.total.AnalysisTotalAggConfig;
import com.bi.queryer.ssm.engine.analysis.cfg.total.AnalysisTotalConfig;
import com.bi.queryer.ssm.engine.analysis.cfg.total.AnalysisTotalItemConfig;
import com.bi.queryer.ssm.engine.analysis.initializer.AnalysisMeasureInitializer;
import com.bi.queryer.ssm.engine.config.field.QueryField;
import com.bi.queryer.ssm.engine.lod.LodUIConfigure;
import com.bi.queryer.ssm.enums.*;
import com.bi.queryer.ssm.meta.MetaField;
import com.bi.queryer.ssm.meta.MetaFieldCategory;
import com.bi.queryer.ssm.meta.SSDMetaCacheManager;
import com.bi.queryer.ssm.util.FieldUtil;
import com.bi.queryer.sys.config.SC;
import com.bi.queryer.sys.enums.Enabled;
import com.bi.queryer.util.BIConsts;
import com.bi.queryer.util.BIUtil;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @Author contributor
 * @Date 14:04 2024-07-03
 * @Description 查询配置规范化处理器
 **/
public class QueryConfigureNormalizer {
    private QueryConfigure config = null;
    private QueryFilter filter = null;
    private QueryResult result = null;
    private Map<String, QueryField> resultFields = null;
    private List<QueryField> appendResultFields = new ArrayList<>();
    public QueryConfigureNormalizer(){

    }

    public void normalize(QueryConfigure configure){

        this.config = configure;
        this.filter = this.config.getFilter();
        this.result = this.config.getResult();
        this.resultFields = result.getFields().stream().collect(Collectors.toMap(QueryField::getCode, f -> f, (f1, f2) -> f1));

        // 先清空，避免重复调用导致重复记录
        this.clear();

        //业务日历属性
        this.normalizeBusinessConfig();

        //公共日期
        this.normalizeCommonDate();

        // 分析相关字段
        this.normalizeAnalysis();

        // 排序
        this.normalizeSort();

        // 字段
        this.normalizeFilterFields();

        // 结果字段
        this.normalizeResultFields();
    }

    /**
     * 标准化公共日期
     */
    private void normalizeCommonDate() {

        //配置区（行维度、列维度、筛选）的公共日期
        boolean isEnable = "true".equalsIgnoreCase(SC.v("ssm.normalize.common.date.enable", "false"));
        if(isEnable){
            this.normalizeConfigCommonDate();
        }

        this.normalizeCommonDateFilterValue();

        this.normalizeCommonDateGranularity();

        //列小计日期
        this.normalizeColSubtotalCommonDate();

    }

    /**
     * 业务日历，日期粒度需要设置为天
     * */
    public void normalizeBusinessConfig() {

        if (!config.getSettings().isBusinessCalendar()) {
            return;
        }

        config.getSettings().setDateGranularity(DateGranularity.DAY.getCode());
        config.getSettings().setIsAggQuery(Enabled.NO.getId());

        for (QueryField qf : this.config.getAllFields()) {
            qf.setQueryDateGranularity(DateGranularity.DAY.getCode());
        }

        //判断是否有农历年同比
        AnalysisThbConfig thbConfig = this.config.getAnalysis().getThb();
        if (!thbConfig.isActive()) {
            return;
        }

        List<String> calcModeList = new ArrayList<>();
        for (AnalysisThbItemConfig item : thbConfig.getItems()) {
            for (String calcMode : item.getCalcModes()) {
                calcModeList.add(calcMode);
            }
        }

        for (String calcMode : calcModeList) {
            AnalysisCalcMode analysisCalcMode = AnalysisCalcMode.get(calcMode);
            if (analysisCalcMode.isTblny()) {
                config.getSettings().setShowLunarDate(Enabled.YES.getId());
                return;
            }
        }

    }

    /**
     * 日期过滤只有一个值时，补齐成2个
     */
    public void normalizeCommonDateFilterValue() {

        for (QueryField qf : config.getFilter().getFields()) {

            if (!qf.isCommonDate()) {
                continue;
            }

            if(CollUtil.isEmpty(qf.getValues())){
                continue;
            }

            if (qf.getValues().size() == 1) {
                qf.getValues().add(qf.getValues().get(0));
            }
        }
    }

    // 对于一些日期汇总且依赖日期的计算维度,前面在处理依赖字段的时候，回丢失时间粒度，这里补齐
    private void normalizeCommonDateGranularity() {
        String dateGranularity = this.config.getSettings().getDateGranularity();
        if (BIUtil.isEmpty(dateGranularity)) {
            return;
        }

        List<QueryField> fields = this.config.getResult().getFields();
        for (QueryField qf : fields) {
            if (qf == null) {
                continue;
            }
            if (qf.isAppend() && qf.isCommonDate() && !StringUtils.equalsIgnoreCase(qf.getQueryDateGranularity(), dateGranularity)) {
                qf.setQueryDateGranularity(dateGranularity);
            }
        }
    }

    /**
     * 标准化 配置区（行维度、列维度、筛选）的公共日期
     */
    public void normalizeConfigCommonDate() {

        List<QueryField> fieldList = new ArrayList<>();

        List<QueryField> configFieldList = new ArrayList<>();
        configFieldList.addAll(this.result.getFields());
        configFieldList.addAll(this.filter.getFields());

        for (QueryField qf : configFieldList) {

            if (qf.getMeta() == null) {
                continue;
            }

            //排除公共日期
            if (Enabled.value(qf.getMeta().getIsCommonDate())) {
                continue;
            }

            //排除不显示的字段
            if (!Enabled.value(qf.getIsShow())) {
                continue;
            }

            fieldList.add(qf);
        }

        MetaField dataField = getCommonDateFieldId(fieldList);
        updateConfigCommonDateField(this.config.getResult().getFields(),dataField);
        updateConfigCommonDateField(this.config.getFilter().getFields(),dataField);

    }

    public void updateConfigCommonDateField(List<QueryField> fieldList,MetaField dataField ) {
        for (QueryField qf : fieldList) {
            if (qf.getMeta() == null) {
                continue;
            }

            //公共日期
            if (Enabled.value(qf.getMeta().getIsCommonDate())) {
                qf.setMeta(dataField);
            }
        }
    }

    /**
     * 标准化列小计日期
     */
    public void normalizeColSubtotalCommonDate() {

        AnalysisTotalConfig total = this.config.getAnalysis().getTotal();

        if (!total.isActive()) {
            return;
        }

        Optional<AnalysisTotalItemConfig> optionalColSubtotalItemConfig = total.getItems().stream().filter(t -> AnalysisTotalType.COL_SUBTOTAL == t.getTotalType()).findAny();
        if (!optionalColSubtotalItemConfig.isPresent()) {
            return;
        }

        //结果没有公共日期不处理
        QueryField commonDateField = this.config.getResultCommonDateField();
        if (commonDateField == null) {
            return;
        }

        AnalysisTotalItemConfig colSubtotalItemConfig = optionalColSubtotalItemConfig.get();

        for (int i = 0; i < colSubtotalItemConfig.getDimIdList().size(); i++) {

            String dimId = colSubtotalItemConfig.getDimIdList().get(i);
            MetaField queryField = SSDMetaCacheManager.getField(dimId);
            if (queryField == null) {
                continue;
            }

            //非公共日期不处理
            if (!Enabled.value(queryField.getIsCommonDate())) {
                continue;
            }

            colSubtotalItemConfig.getDimIdList().set(i, commonDateField.getId());
        }

    }

    /**
     * 获取公共日期的字段id
     * @param fieldList
     * @return
     */
    public MetaField getCommonDateFieldId(List<QueryField> fieldList) {

        MetaField dateField = null ;
        if (CollUtil.isNotEmpty(fieldList)) {
            for(QueryField qf : fieldList){

                List<MetaField> commonDateFieldList = new ArrayList<>();
                List<String> ctgIdList = new ArrayList<>();

                if (StrUtil.isNotEmpty(qf.getModuleCtgId())) {
                    ctgIdList.add(qf.getModuleCtgId());
                } else {
                    MetaField metaField = SSDMetaCacheManager.getField(qf.getId());
                    if(metaField == null){
                        continue;
                    }
                    if (CollUtil.isEmpty(metaField.getCategoryIdList())) {
                        continue;
                    }
                    ctgIdList.addAll(metaField.getCategoryIdList());
                }

                for (String ctgId : ctgIdList) {
                    MetaFieldCategory metaFieldCategory = SSDMetaCacheManager.getCategoryById(ctgId);
                    FieldUtil.getCtgCommonDateField(metaFieldCategory, commonDateFieldList);

                    if (CollUtil.isNotEmpty(commonDateFieldList)) {
                        dateField = commonDateFieldList.get(0);
                        break;
                    }
                }

                if (dateField != null) {
                    break;
                }
            }
        }

        //如果字段的目录中一个公共日期都没有，从所有字段中找一个
        if (dateField == null) {
            for (MetaField mf : SSDMetaCacheManager.getFieldsCache().values()) {
                if (Enabled.value(mf.getIsCommonDate())) {
                    dateField = mf;
                    break;
                }
            }
        }

        return dateField;
    }

    /**
     * 标准化公共日期
     * 按实际查询内容，自动匹配公共日期
     */
//    public void normalizeCommonDate(){
//        QueryField filterCommonDateField = this.config.getFilterCommonDateField();
//        QueryField resultCommonDateField = this.config.getResultCommonDateField();
//        QueryField commonDate = filterCommonDateField != null ? filterCommonDateField : resultCommonDateField;
//        if(commonDate == null){
//            return;
//        }
//        // 按字段所属表获取其对应公共日期：哪个表所属指标多，就取哪个表的公共日期
//        if(BIUtil.isEmpty(resultFields)){
//            return;
//        }
//        Map<String, List<QueryField>> tableFieldMap = new HashMap<>();
//        for(QueryField field : resultFields.values()){
//            MetaField meta = field.getMeta();
//            if(meta == null){
//                continue;
//            }
//            String tableId = meta.getTableId();
//            MetaTable table = SSDMetaCacheManager.getTable(tableId);
//            if(table == null){
//                continue;
//            }
//            if(tableFieldMap.containsKey(tableId)){
//                tableFieldMap.get(tableId).add(field);
//            }else {
//                List<QueryField> tableFields = new ArrayList<>();
//                tableFields.add(field);
//                tableFieldMap.put(tableId, tableFields);
//            }
//        }
//
//        String targetTableId = null;
//        Integer size = -1;
//        for(String tableId : tableFieldMap.keySet()){
//            List<QueryField> fields = tableFieldMap.get(tableId);
//            if(size < fields.size()){
//                targetTableId = tableId;
//                size = fields.size();
//            }
//        }
//
//        if(BIUtil.isEmpty(targetTableId)){
//            return;
//        }
//        MetaField targetCommonDateMetaField = null;
//        MetaTable metaTable = SSDMetaCacheManager.getTable(targetTableId);
//        List<MetaField> metaFields = metaTable.getFields();
//        for(MetaField metaField : metaFields){
//            if(Enabled.isTrue(metaField.getIsCommonDate())){
//                targetCommonDateMetaField = metaField;
//                break;
//            }
//        }
//        if(targetCommonDateMetaField == null){
//            return;
//        }
//        if(filterCommonDateField != null){
//            filterCommonDateField.setMeta(targetCommonDateMetaField);
//        }
//        if(resultCommonDateField != null){
//            resultCommonDateField.setMeta(targetCommonDateMetaField);
//        }
//    }

    /**
     * 标准化分析配置：将分析配置分配到查询结果字段中
     */
    public void normalizeAnalysis() {

        normalizeTotal();

        //添加分析字段
        AnalysisMeasureInitializer analysisMeasureInitializer = new AnalysisMeasureInitializer(this.config);
        analysisMeasureInitializer.initialize();

    }

    public void normalizeTotal() {
        //将所有维度设置为具体的维度id
        AnalysisTotalConfig analysisTotalConfig = this.config.getAnalysis().getTotal();

        if (!analysisTotalConfig.isActive()) {
            return;
        }

        //如果只配置了列小计、且没有选择任何维度，将is_active设置为0
        if (analysisTotalConfig.getItems().size() == 1) {
            AnalysisTotalItemConfig analysisTotalItemConfig = analysisTotalConfig.getItem(AnalysisTotalType.COL_SUBTOTAL);
            if (analysisTotalItemConfig != null && CollUtil.isEmpty(analysisTotalItemConfig.getDimIdList())) {
                analysisTotalConfig.setIsActive(0);
                return;
            }
        }

        List<String> dimIdList = new ArrayList<>();
        List<QueryField> rowDimensions = config.getResult().getRowDimensions();
        for (int i = 0; i < rowDimensions.size() - 1; i++) {
            dimIdList.add(rowDimensions.get(i).getId());
        }

        for (AnalysisTotalItemConfig analysisItemConfig : analysisTotalConfig.getItems()) {
            if (AnalysisTotalType.COL_SUBTOTAL != analysisItemConfig.getTotalType()) {
                continue;
            }

            if (analysisItemConfig.getDimIdList().contains(BIConsts.ALL_DIM)) {
                analysisItemConfig.getDimIdList().clear();
                analysisItemConfig.getDimIdList().addAll(dimIdList);
            }

            //配置的列小计维度大于实际的维度数量，重新赋值
            if (analysisItemConfig.getDimIdList().size() > dimIdList.size()) {
                analysisItemConfig.getDimIdList().clear();
                analysisItemConfig.getDimIdList().addAll(dimIdList);
            }
        }

        // 汇总支持自定聚合方式的配置设置
        AnalysisTotalAggConfig aggConfig = analysisTotalConfig.getAggConfig();
        if (!aggConfig.isActive()) {
            return;
        }
        for (AnalysisTotalAggConfig.AnalysisZbTotalAggItemConfig itemConfig : aggConfig.getConfigs()) {
            QueryField field = config.getResult().getFieldById(itemConfig.getId());
            if (field == null) {
                field = config.getResult().getFieldByCode(itemConfig.getCode());
            }
            if (field != null) {
                field.setTotalAggType(itemConfig.getAggType());
            }
        }
    }

    /**
     * 标准化排序：按统一规则设置默认排序
     */
    public void normalizeSort(){
        //添加默认排序
        QuerySort querySort = new QuerySort(this.config);
        querySort.load();
    }

    /**
     * 标准化过滤字段
     */
    public void normalizeFilterFields(){
        // 修复结果和过滤的字段属性
        // 1、重置聚合类型：以结果字段为主，避免对指标结果过滤时，聚合方式丢失（不重置会以默认聚合方式过滤）
        // 2、重置过滤字段的结果查询类型：以结果字段为主
        List<QueryField> filterFields = filter.getFields();
        for (QueryField filterField : filterFields) {
            QueryField resultField = resultFields.get(filterField.getCode());
            if (resultField != null) {
                filterField.setAggExpressionType(resultField.getAggExpressionType());
                filterField.setQueryArea(resultField.getQueryArea());
                filterField.setRawQueryArea(resultField.getRawQueryArea());
                filterField.setShowOrder(resultField.getShowOrder()); // 字段顺序已结果字段为主
            }
        }
    }

    /**
     * 标准化结果字段
     */
    public void normalizeResultFields() {
        List<QueryField> filterFields = filter.getFields();
      for (QueryField filterField : filterFields) {
            //修复结果筛选字段不存在于指标区，日均计算异常的问题 。场景示例：指标选择【支付用户数（日均）】，未选择【支付用户数】原始值，筛选区选择【支付用户数】结果过滤
            //将结果筛选字段默认加入指标区，避免日均sql异常
            if (filterField.isMeasure() && FieldFilterMode.agg == FieldFilterMode.get(filterField.getFilterValueMode())) {
                //判断筛选指标是否存在与指标区
                Long measureCount = result.getFields().stream()
                        .filter(f -> f.getCode().equalsIgnoreCase(filterField.getCode()))
                        .filter(f -> !f.isAppend())
                        .count();
                if (measureCount == 0) {
                    QueryField queryField = filterField.clone();
                    queryField.setIsResult(true);
                    queryField.setAppend(true);
                    //修改过滤计算字段的原子指标，查询异常的问题
                    queryField.setFilter(true);
                    appendResultFields.add(queryField);
                }
            }
        }
        for (QueryField appendField : appendResultFields) {
            result.add(appendField, QueryArea.Measure);
        }

        // lod字段均值：设置lod指标改为其均值字段
        for (QueryField resultField : resultFields.values()) {
            if (!resultField.isLodField()) {
                continue;
            }
            AggExpressionType aggExpressionType = AggExpressionType.get(resultField.getAggExpressionType());
            if (!aggExpressionType.isAvgByDay()) {
                continue;
            }
            LodUIConfigure uiConfigure = resultField.getCustomFieldConfigure().getLodConfig();
            if (uiConfigure == null || BIUtil.isEmpty(uiConfigure.getMeasureId())) {
                continue;
            }
            // 日均值需改修改其引用的指标id
            if (!uiConfigure.getMeasureId().endsWith(aggExpressionType.getCode())) {
                uiConfigure.setMeasureId(String.format("%s_%s", uiConfigure.getMeasureId(), aggExpressionType.getCode()));
            }
        }
    }

    public void clear(){
        for(QueryField appendField : appendResultFields){
            result.remove(appendField, QueryArea.Measure);
        }
        this.appendResultFields.clear();
    }

}
