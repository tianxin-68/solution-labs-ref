package com.bi.queryer.ssm.custom;

import com.bi.queryer.ssm.engine.QueryContext;
import com.bi.queryer.ssm.engine.QueryEngine;
import com.bi.queryer.ssm.engine.QueryFactory;
import com.bi.queryer.ssm.engine.accelerate.route.DataSourceRouter;
import com.bi.queryer.ssm.engine.config.QueryConfigure;
import com.bi.queryer.ssm.engine.config.QueryResult;
import com.bi.queryer.ssm.engine.config.field.QueryField;
import com.bi.queryer.ssm.enums.AnalysisCalcMode;
import com.bi.queryer.ssm.enums.QueryArea;
import com.bi.queryer.ssm.exception.SSDException;
import com.bi.queryer.ssm.meta.SSDQueryTemplate;
import com.bi.queryer.sys.base.BaseDao;
import com.bi.queryer.sys.db.DBUtil;
import com.bi.queryer.sys.db.DataSourceType;
import com.bi.queryer.sys.enums.Enabled;
import com.bi.queryer.sys.config.SC;
import com.bi.queryer.util.BIUtil;
import com.bi.queryer.util.Guid;
import com.bi.queryer.util.SpringContextUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Scope("prototype")
public class CustomFieldService {
    /**
     * 校验自定义字段表达式合法性，并返回该字段的稳定 id。
     * <p>
     * 新建/编辑中的字段在 check 期间可能尚无持久化 id。若 id 为空，CustomFieldManager 会现场 Guid 生成，
     * QueryConfigure.clone() 每次从 template.config 反序列化又会再生成一次，导致 code 不稳定，
     * LodQueryConfigureCreator 按 code 删除 lod 字段失败，进而让 QueryFactory.createEngine 无限递归。
     * 因此必须在构建 QueryConfigure 之前生成并写回同一个 id。
     * </p>
     * <p>
     * LOD 类型直接跳过 buildSql 校验，仅返回 id。
     * lod_calc / 表达式含 lod 标识是否跳过由开关控制：ssm.custom.field.lod.check.enable（默认 true）。
     * 设为 false 时：上述相关字段跳过 buildSql 校验，仅返回 id。
     * </p>
     *
     * @param template 前端传入的临时模板（仅内存对象，setConfig 不落库）
     * @param queryCustomField 当前正在校验的自定义字段
     * @param cxt 查询上下文
     * @return 稳定的自定义字段 id（新建则生成，已有则沿用）
     */
    public String check(SSDQueryTemplate template, QueryField queryCustomField, QueryContext cxt) {
        // 1. 方法一开始就确定唯一 id，并回填到字段对象，保证全程只有一个 id
        CustomFieldType customFieldType = CustomFieldType.get(queryCustomField.getCustomFieldConfigure().getType());
        String customFieldId = BIUtil.isEmpty(queryCustomField.getId()) ? customFieldType.getIdentifier() + Guid.id() : queryCustomField.getId();
        queryCustomField.setId(customFieldId);

        // 2. 仅 LOD 类型直接跳过 SQL 校验
        if (CustomFieldType.LOD == customFieldType) {
            return customFieldId;
        }

        // 3. lod 校验开关关闭时，保留旧逻辑：lod 字段或表达式含 lod 标识的字段跳过 SQL 校验
        String expression = queryCustomField.getCustomFieldConfigure().getExpression();
        boolean isLodRelated = CustomFieldType.isLod(queryCustomField.getCustomFieldConfigure().getType())
                || (expression != null && expression.contains(CustomFieldType.LOD.getIdentifier()));
        boolean lodCheckEnable = "true".equalsIgnoreCase(SC.v("ssm.custom.field.lod.check.enable", "true"));
        if (isLodRelated && !lodCheckEnable) {
            return customFieldId;
        }

        // 4. 写回 template.config，使后续 clone()/reload 反序列化读到同一份固定 id，code 保持一致
        writeBackFieldId(template, customFieldId, queryCustomField);

        // 5. 普通字段，或 lod 开关开启时，统一走 buildSql 校验（依赖稳定 id，删除 lod 字段才能按 code 匹配）
        QueryConfigure queryConfigure = new QueryConfigure(template);
        queryConfigure.load();

        // 不校验权限
        queryConfigure.getSettings().setAclCheck(false);
        // 不排序
        queryConfigure.getSettings().setNeedSort(false);

        // 若有列维度，转为行维度，提升性能
        QueryResult result = queryConfigure.getResult();
        List<QueryField> colDimFields = result.getColDimensions();
        if(BIUtil.isNotEmpty(colDimFields)){
            colDimFields.forEach(c -> {
                c.setQueryArea(QueryArea.RowDimension);
                c.setRawQueryArea(QueryArea.RowDimension);
                result.add(c, QueryArea.RowDimension);
            });
        }
        result.setColDimensions(new ArrayList<>());
        // 前面把列维度转为行维度后，占整表总计会报错，校验场景下先移除
        queryConfigure.getAnalysis().getZb().getItems().removeIf(v-> Objects.equals(v.getCalcMode(), AnalysisCalcMode.ZB_WHOLE_TABLE_TOTAL.getCode()));
        queryConfigure.getResult().getFields().removeIf(v-> Objects.equals(v.getAnalysisConfig().getCalcMode(), AnalysisCalcMode.ZB_WHOLE_TABLE_TOTAL.getCode()));
        queryConfigure.getResult().getMeasures().removeIf(v-> Objects.equals(v.getAnalysisConfig().getCalcMode(), AnalysisCalcMode.ZB_WHOLE_TABLE_TOTAL.getCode()));

        // config 中已包含待 check 的计算字段，只需补齐 queryArea，无需再 add 到 result
        if(Enabled.isTrue(queryCustomField.getCustomFieldConfigure().getIsMeasure())) {
            queryCustomField.setRawQueryArea(QueryArea.Measure);
            //result.add(queryCustomField, QueryArea.Measure);
        }else {
            queryCustomField.setRawQueryArea(QueryArea.RowDimension);
            //result.add(queryCustomField, QueryArea.RowDimension);
        }

        QueryEngine engine = QueryFactory.createEngine(queryConfigure, cxt);
        // 设置当前引擎查询默认数据源
        DataSourceRouter.setQueryEngineDefaultDataSource(engine);

        BaseDao dao = (BaseDao) SpringContextUtil.getBean("baseDao");

        List<String> aclCodeList = queryConfigure.getAllFields().stream().map(QueryField::getCode).collect(Collectors.toList());
        Map<String, String> aclMap = new HashMap<>();
        aclCodeList.stream().forEach(c->aclMap.put(c,c));
        cxt.setAclFields(aclMap);

        // 用 where 1=2 做语法级校验，不真正取数（lod 在开关开启时也会走到这里）
        try {
            String sql = engine.buildSql();
            sql = " select count(1) as f_count from (" + sql + ") _custom_field_check_ where 1=2";
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

        return customFieldId;
    }

    /**
     * 将稳定 id 写回 template.config 的 result.measures。
     * <p>
     * QueryConfigure 从 template.getConfig() 反序列化；clone() 时也会再次反序列化。
     * 只有把 id 写进这份 JSON，CustomFieldManager.getMetaField() 才不会因空 id 重新 Guid，
     * 从而保证 lod 字段按 code 删除成功、createEngine 不会误判递归。
     * </p>
     * <p>
     * 匹配策略：在 measures 中找 id 为空且 customFieldConfigure.expression 与当前校验字段一致的条目再写入，
     * 避免仅凭空 id 误改写其它未保存字段。
     * </p>
     *
     * @param template 当前请求的临时模板对象（仅改内存 config，不落库）
     * @param customFieldId 已生成的自定义字段 id
     * @param queryCustomField 当前正在校验的自定义字段（用于表达式比对）
     */
    private void writeBackFieldId(SSDQueryTemplate template, String customFieldId, QueryField queryCustomField) {
        JSONObject configJson = JSONObject.parseObject(template.getConfig());
        if (configJson == null) {
            return;
        }
        JSONObject resultObj = configJson.getJSONObject("result");
        if (resultObj == null) {
            return;
        }
        JSONArray measuresArr = resultObj.getJSONArray("measures");
        if (BIUtil.isEmpty(measuresArr)) {
            return;
        }

        String targetExpression = queryCustomField.getCustomFieldConfigure() == null
                ? null
                : queryCustomField.getCustomFieldConfigure().getExpression();
        for (int i = 0; i < measuresArr.size(); i++) {
            JSONObject f = measuresArr.getJSONObject(i);
            if (f == null || BIUtil.isNotEmpty(f.getString("id"))) {
                continue;
            }
            JSONObject cfg = f.getJSONObject("customFieldConfigure");
            String fieldExpression = cfg == null ? null : cfg.getString("expression");
            if (!Objects.equals(targetExpression, fieldExpression)) {
                continue;
            }
            f.put("id", customFieldId);
            template.setConfig(configJson.toJSONString());
            break;
        }
    }

    /*
    public String check2(SSDQueryTemplate template, QueryField queryCustomField, QueryContext cxt) {
        String expression = queryCustomField.getCustomFieldConfigure().getExpression();
        if(CustomFieldType.isLod(queryCustomField.getCustomFieldConfigure().getType()) || expression.contains(CustomFieldType.LOD.getIdentifier())) {
            // lod不校验：lod字段或lod字段参与的四则运算字段
        }else {
            QueryConfigure queryConfigure = new QueryConfigure(template);
            queryConfigure.load();
            // 先解析字段
            List<QueryField> fieldList = CustomFieldParser.parseExpression(queryConfigure, queryCustomField);

            // 再通过全量sql校验
            queryConfigure.getSettings().setAclCheck(false); // 不校验权限
            CustomFieldQueryEngine engine = new CustomFieldQueryEngine(queryConfigure, cxt);
            // 设置当前引擎查询默认数据源
            DataSourceRouter.setQueryEngineDefaultDataSource(engine);
            engine.checkCustomFields(queryCustomField);
        }
        CustomFieldType customFieldType = CustomFieldType.get(queryCustomField.getCustomFieldConfigure().getType());
        String customFieldId = BIUtil.isEmpty(queryCustomField.getId()) ?  customFieldType.getIdentifier() + Guid.id() : queryCustomField.getId();
        return customFieldId;
    }
     */

    /*
    public List<QueryField> check(QueryConfigure queryConfigure,  QueryField queryField, QueryContext cxt) {

        // 先解析字段
        List<QueryField> fieldList = CustomFieldParser.parseExpression(queryConfigure, queryField);

        // lod字段合法性，不需要通过sql校验
        if(CustomFieldType.isLod(queryField.getCustomFieldConfigure().getType())){
            return fieldList;
        }

        // 再通过全量sql校验
        CustomFieldQueryEngine engine = new CustomFieldQueryEngine(queryConfigure, cxt);
        engine.checkCustomFields();
        return fieldList;
    }
     */
}
