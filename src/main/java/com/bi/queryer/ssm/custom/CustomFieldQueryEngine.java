package com.bi.queryer.ssm.custom;

import com.bi.queryer.ssm.engine.QueryContext;
import com.bi.queryer.ssm.engine.QueryEngine;
import com.bi.queryer.ssm.engine.config.QueryConfigure;
import com.bi.queryer.ssm.engine.config.field.QueryField;
import com.bi.queryer.ssm.meta.SSDQueryTemplate;
import com.bi.queryer.sys.enums.Enabled;
import com.bi.queryer.util.BIUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.util.TypeUtils;

import java.util.Iterator;

public class CustomFieldQueryEngine extends QueryEngine {

    public CustomFieldQueryEngine(QueryConfigure template, QueryContext cxt){
        super(template, cxt);
    }

    /**
    public void prepare(){
        // 构建模型
        this.models = this.createModels();
    }
     */

    /**
     * 先去掉自定义字段后sql查询，若没有错误，再继续有自定义字段的查询
     * @return
     */
    /*
    public boolean checkCustomFields(QueryField queryCustomField){
        List<QueryField> resultFields = config.getResult().getFields();
        List<QueryField> customFields = resultFields.stream()
                .filter(f-> f.getCustomFieldConfigure() != null && !f.getCustomFieldConfigure().isEmpty())
                .filter(f-> !CustomFieldType.isLod(f.getCustomFieldConfigure().getType()))
                .collect(Collectors.toList());
        if(BIUtil.isEmpty(customFields)) {
            return true;
        }

        SSDQueryTemplate template = this.getConfig().getTemplateEntity();
        this.replaceRawConfigMeasures(template, queryCustomField);
        this.config = this.config.clone();
        this.config.load();

        // 不校验权限
        this.config.getSettings().setAclCheck(false);

        BaseDao dao = (BaseDao) SpringContextUtil.getBean("baseDao");

        QueryContext customCxt = new QueryContext(this.getCxt().getRequest());

        List<String> aclCodeList = this.getConfig().getAllFields().stream().map(QueryField::getCode).collect(Collectors.toList());
        Map<String, String> aclMap = new HashMap<>();
        aclCodeList.stream().forEach(c->aclMap.put(c,c));
        customCxt.setAclFields(aclMap);

        // 包含自定义字段的sql校验
        try {
            QueryEngine engine =  new CustomFieldQueryEngine(this.config, customCxt);
            String sql = engine.buildSql();
            sql = this.getSqlTips() + " select count(1) as f_count from (" + sql + ") _custom_field_check_ where 1=2";
            DataSourceType dsType = DBUtil.getDataSourceType();

            Integer count = dao.queryCountBySQL(sql, dsType);
        }catch (Throwable e) {
            e.printStackTrace();
            String msg = e.getMessage() + "";
            if(msg.toLowerCase().contains("group by")) {
                msg = "计算字段可能需要设置为维度类型，" + msg;
            }
            throw new SSDException("存在计算字段表达式不合法：" + msg , SSDException.Custom_Field_Check_Error);
        }

        return true;
    }
     */

    private void replaceRawConfigMeasures(SSDQueryTemplate template, QueryField queryCustomField) {
        JSONObject configJSON = JSONObject.parseObject(template.getConfig());
        JSONObject resultObject = configJSON.getJSONObject("result");
        if (resultObject == null || resultObject.isEmpty()) {
            return;
        }
        JSONArray measures = resultObject.getJSONArray("measures");
        if (BIUtil.isEmpty(measures)) {
            return;
        }


        if (queryCustomField.getCustomFieldConfigure() != null && Enabled.value(queryCustomField.getCustomFieldConfigure().getIsMeasure())) {
            measures.clear();
            measures.add(JSONObject.toJSON(queryCustomField));
        } else {
            Iterator<Object> iterator = measures.iterator();
            while (iterator.hasNext()) {
                QueryField field = TypeUtils.castToJavaBean(iterator.next(), QueryField.class);
                field.init();
                if (field.isLodField()) {
                    iterator.remove();
                }
            }
        }
        template.setConfig(JSONObject.toJSONString(configJSON));
    }
}
