package com.bi.queryer.ssm.engine.model.creator;

import com.bi.queryer.ssm.engine.config.QueryConfigure;
import com.bi.queryer.ssm.engine.config.field.QueryField;
import com.bi.queryer.ssm.engine.model.QueryTable;
import com.bi.queryer.ssm.engine.model.StarModel;
import com.bi.queryer.ssm.meta.MetaField;
import com.bi.queryer.ssm.meta.SSDMetaCacheManager;
import com.bi.queryer.util.BIUtil;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @Author contributor
 * @Date 17:30 2022-11-01
 * @Description 模型优化器
 **/
public abstract class BaseModelOptimizer {
    protected List<StarModel> models = null;
    protected QueryConfigure config = null;

    // 用于字段快速匹配，提升字段查找性能
    protected Map<String, QueryField> resultFieldMap = new HashMap<>(10);
    protected Map<String, QueryField> filterFieldMap = new HashMap<>(10);

    public BaseModelOptimizer(QueryConfigure config, List<StarModel> models){
        this.models = models;
        this.config = config;

        List<QueryField> resultFields = config.getResult().getFields();
        resultFields.forEach(f->resultFieldMap.put(f.getCode(), f));

        List<QueryField> filterFields = config.getFilter().getFields();
        filterFields.forEach(f->filterFieldMap.put(f.getCode(), f));
    }

    public List<StarModel> getModels() {
        return models;
    }

    public void setModels(List<StarModel> models) {
        this.models = models;
    }

    public QueryConfigure getConfig() {
        return config;
    }

    public void setConfig(QueryConfigure config) {
        this.config = config;
    }

    abstract public void optimize();

    /**
     * 规范化表：将表中缺少的维度补齐，若有相关度量指标也需补齐
     * @param model
     */
    protected void normalize(StarModel model) {
        List<QueryField> queryFields = config.getAllFields();
        if(BIUtil.isEmpty(queryFields)) {
            return;
        }

        // 合并config重复字段：将重复字段的属性相互copy
        // 场景：过滤区选择模块1.日期，结果区选择模块2.日期，需要将这2个日期的属性同步
        Map<String, List<QueryField>> queryFieldMap = queryFields.stream().collect(Collectors.groupingBy(QueryField::getCode));
        Collection<List<QueryField>> sameCodeFieldList = queryFieldMap.values();
        for(List<QueryField> list : sameCodeFieldList){
            for(int i = 0; i < list.size(); i++){
                for(int j = i; j < list.size(); j++){
                    copyFieldProperty(list.get(i), list.get(j));
                    copyFieldProperty(list.get(j), list.get(i));
                }
            }
        }




        List<QueryTable> queryTables = model.getTables();
        for(QueryTable queryTable : queryTables){

            List<QueryField> queryTableFields = queryTable.getFields();

            // 手动设置map容量，提升查询性能
            Map<String, QueryField> tableQueryFieldMap = new HashMap<>(5);
            for(QueryField f : queryTableFields){
                tableQueryFieldMap.put(f.getCode(), f);
            }

            // 手动设置map容量，提升查询性能
            Map<String, MetaField> tableMetaFieldMap = new HashMap<>(100);
            List<MetaField> metaFields = SSDMetaCacheManager.getTableFields(queryTable.getMeta().getId());
            metaFields.stream().forEach(f->{tableMetaFieldMap.put(f.getCode(), f);});

            for(QueryField queryField : queryFields){

                // 若查询是过滤字段，则需要将过滤信息同步更新到表格字段中
                // 场景：T1(dim1/measure1)、T2(dim1/measure1)，T2.dim1作为结果字段，T1.dim1作为过滤字段
                QueryField tableField = tableQueryFieldMap.get(queryField.getCode());
                MetaField tableMetaField = tableMetaFieldMap.get(queryField.getCode());

                if(queryField.getIsFilter() && tableField != null){
                    this.copyFieldProperty(tableField, queryField);
                }

                // 查询表中没有相应的查询字段，但元表表中有，则添加到该表的查询字段中
                // 场景：config(result:[dim1/measure1])，queryTable(dim1)，则需添加measure1到queryTable中
                if(tableField == null && tableMetaField != null) {
                    // 克隆一个并添加到表中
                    QueryField cloneField = queryField.clone();

                    cloneField.setMeta(tableMetaFieldMap.get(queryField.getCode()));
                    this.copyFieldProperty(cloneField, queryField);
                    queryTable.addField(cloneField);

                    // 此处需要添加到map中，避免后续遍历时遗漏
                    tableQueryFieldMap.put(cloneField.getCode(), cloneField);
                }

                // copy用户自定义维度字段，确保不同starModel中有相同维度
                // 说明：自定义指标字段不可能再此出现，因为自定义指标字段在sql最外层计算
                // 场景：用户自定义字段:substr(日期,1,7)，日期在多个模块中同时存在，此字段需要copy到其他模块中，避免最后join时条件缺失
                if(tableField == null && tableMetaField == null && queryField.isCustomDimension()){
                    // 模型中不存在，则添加
                    if(model.getFieldByCode(queryField.getCode()) == null){
                        QueryField cloneField = queryField.clone();
                        cloneField.getCalcAtomFields().clear();
                        this.copyFieldProperty(cloneField, queryField);

                        // 修改克隆字段用户自定义表达式中的引用字段id
                        // 此处不能在字段的clone方法中处理，因为需要隶属表id:queryTable.getId()
                        String expression = cloneField.getCustomFieldConfigure().getExpression();
                        // 设置clone字段原子字段:通过编码获取
                        Set<QueryField> srcAtomFields = queryField.getCalcAtomFields();
                        for(QueryField srcAtomField : srcAtomFields){
                            MetaField targetAtomMetaField = SSDMetaCacheManager.getFieldByCode(queryTable.getId(), srcAtomField.getCode());
                            if(targetAtomMetaField != null) {
                                // 修改计算表达式：将其中的id替换为clone字段的id
                                expression = expression.replaceAll("\\[" + srcAtomField.getId() + "\\]", "[" + targetAtomMetaField.getId() + "]");
                            }
                        }
                        cloneField.getCustomFieldConfigure().setExpression(expression);

                        queryTable.addField(cloneField);
                        tableQueryFieldMap.put(cloneField.getCode(), cloneField);
                    }
                }
            }
            // 重新格式化：处理计算字段的附加字段，将附加字段添加到表中
            queryTable.format();
        }
    }

    protected void copyFieldProperty(QueryField target, QueryField src) {
//        target.setIsResult(config.getResult().getFieldByCode(target.getCode()) != null);
//        target.setIsFilter(config.getFilter().getFieldByCode(target.getCode()) != null);

        target.setIsResult(this.resultFieldMap.containsKey(target.getCode()));
        target.setIsFilter(this.filterFieldMap.containsKey(target.getCode()));

        target.setFilterValueType(src.getFilterValueType());
        target.setFilterValueMode(src.getFilterValueMode());
        target.setFilterQueryRule(src.getFilterQueryRule());
        target.setValuesTitle(src.getValuesTitle());
        target.setValues(src.getValues());

//        src.setIsResult(config.getResult().getFieldByCode(src.getCode()) != null);
//        src.setIsFilter(config.getFilter().getFieldByCode(src.getCode()) != null);

        src.setIsResult(this.resultFieldMap.containsKey(src.getCode()));
        src.setIsFilter(this.filterFieldMap.containsKey(src.getCode()));

        // 分类维度信息copy
        if(src.isCategoryAggregation()){
            target.setCategoryAggregation(src.getCategoryAggregation());
        }
        if(target.isCategoryAggregation()){
            src.setCategoryAggregation(target.getCategoryAggregation());
        }
    }
}
