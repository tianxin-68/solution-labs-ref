package com.bi.queryer.ssm.migrate.bizsplit.processor;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.bi.queryer.ssm.migrate.bizsplit.model.MetricCodeMappingEntity;
import com.bi.queryer.sys.base.BaseDao;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 看板/查询模板复制时的内容改写：
 * 1. 名称/筛选值：把配置里所有字符串中出现的老业务线子串替换成新业务线（覆盖看板名、widget 标题、
 *    筛选器 title/展示值，以及"业务线"维度筛选条件的取值本身——它们在 JSON 里都只是普通字符串，统一按子串替换处理）。
 * 2. 指标 code/id/展示名：配置里 {@code code} 键、{@code id} 键、"裸 id 数组"键（selectedFields/
 *    measureIdList/dimIdList）、LOD 计算字段的 {@code measureId}，以及同一层的展示名键（name/title/
 *    displayTitle），统一按 {@code ssm_metric_expansion_field_mapping}（{@link MetricCodeMappingEntity}，
 *    见 {@link #loadMetricCodeMapping(String, String, String, String)}）查出的**同一行**记录一起替换。
 *    只信这张表：查不到映射（维度、与业务线无关的通用指标）保持原样，不做任何元数据反查/猜测兜底
 *    （不查 {@code SSDMetaCacheManager}，也不读旧的 {@code ssm_metric_expansion_mapping} 表、不读本表的
 *    {@code source_metric_code}/{@code target_metric_code} 两列）。
 * 3. 复合/带前缀的 code/id（如 AggField 的 {@code ${code}_avg_by_d}、LOD 的 {@code lod:${id}}、同环比的
 *    {@code ${id}___${calcMode}}）先剥离前后缀、查表、再把前后缀原样拼回去；AggField 额外携带的
 *    {@code originCode}/{@code originId}（未加后缀的原始值）优先按这两个字段查表，查到后把复合
 *    code/id 里对应的那一段原地替换掉，不影响后缀部分。
 * 4. 看板内部的结构性引用（不是指标 code/id，是这次复制过程自己生成的新 id）：storyline 的
 *    {@code targetWidgetId}、AI 解读依赖的 {@code widgetId}+{@code tplId}+{@code value} 组合、
 *    查询模板 widget 的 {@code viewId}，按调用方传入的 {@link WidgetRefMapping} 重新映射；不传时
 *    （或查不到映射时，widgetId/tplId 沿用旧值，viewId 置空）保持原样。
 * 5. AI 解读的 {@code scriptId}：脚本是针对老业务线的字段生成的，复制后不能继续沿用，一律清空
 *    scriptId 并把 scriptStatus 置为 outdated，让前端提示重新生成，而不是静默复用一个不存在/过期的脚本。
 * <p>
 * 之所以用"通用 JSON 树遍历 + 原样重新序列化"而不是走 {@code QueryConfigure}/{@code QueryField} 领域模型解析，
 * 是因为 {@code QueryConfigure#toJSON()} 只回写 filter/result/setting 三段，会丢失 globalFilter/meso/analysis/
 * sessionId 等字段——领域模型是为查询执行设计的，不是可靠的无损序列化器，用它来"解析再序列化"会悄悄丢配置。
 */
@Component
public class MetricMigrateFilterRewriteProcessor {

    /** 值是字段 code，触发指标映射查找的键 */
    private static final String CODE_KEY = "code";

    /** 值是字段 id，触发指标映射查找的键 */
    private static final String ID_KEY = "id";

    /** AggField 等聚合/派生字段携带的"未加后缀原始 code/id"，比复合 code/id 更适合直接查表 */
    private static final String ORIGIN_CODE_KEY = "originCode";
    private static final String ORIGIN_ID_KEY = "originId";

    /** LOD 计算字段 customFieldConfigure.lodConfig 里"固定的度量"引用，值是裸字段 id，没有兄弟 code/id 键 */
    private static final String LOD_MEASURE_ID_KEY = "measureId";

    /** LOD/计算字段 customFieldConfigure.expressionIdMapping（数组，元素自带 code/id）+ 兄弟 expression（公式串，形如 "[oldId]+[oldId2]"） */
    private static final String EXPRESSION_ID_MAPPING_KEY = "expressionIdMapping";
    private static final String EXPRESSION_KEY = "expression";

    /**
     * "裸 id/code 数组"类键：前端直接喂给 el-tree/multi-select 的 checked-keys，元素是裸字符串，没有
     * code/id 包裹，走不到上面按 code/id 触发的分支，必须单独按键名识别处理。selectedFields/dimIdList
     * 存的是字段 id，measureIdList 视具体 widget 不同可能存 code 也可能存 id，统一按"先按 id 查、查不到
     * 再按 code 查"处理。哨兵值 "all"（表示"全部"）天然查不到映射，原样保留。
     */
    private static final Set<String> ID_ARRAY_KEYS = new HashSet<>(Arrays.asList("selectedFields", "measureIdList", "dimIdList"));

    /** code/id 命中映射时，同一层里代表该字段展示名的键，一起换成映射表给出的 new_field_title */
    private static final Set<String> FIELD_NAME_KEYS = new HashSet<>(Arrays.asList("name", "title", "displayTitle"));

    /** storyline 节点指向同一看板内另一个 widget 的 id；随本次复制生成的 widgetId 映射一起换 */
    private static final String TARGET_WIDGET_ID_KEY = "targetWidgetId";

    /** 查询模板 widget 当前展示的视图 id；随 queryTplId 一起换，查不到映射置空（不保留旧视图 id） */
    private static final String VIEW_ID_KEY = "viewId";

    /**
     * AI 解读"依赖图表"配置项的指纹：同一层同时出现 widgetId + tplId 才当作依赖引用处理，避免误伤
     * widgetOptions 里其它场景下出现的同名字段。value 是 {@code widgetId + AI_DEPEND_SPLIT_CHAR + tplId}
     * 的组合编码（分隔符取自前端 aiInterpreter/const.js 的 SPLIT_CHAR）。
     */
    private static final String WIDGET_ID_KEY = "widgetId";
    private static final String TPL_ID_KEY = "tplId";
    private static final String VALUE_KEY = "value";
    private static final String AI_DEPEND_SPLIT_CHAR = "-#-";

    /** AI 解读脚本配置项的指纹：同一层同时出现 scriptId + scriptStatus 才当作脚本状态处理 */
    private static final String SCRIPT_ID_KEY = "scriptId";
    private static final String SCRIPT_STATUS_KEY = "scriptStatus";
    private static final String SCRIPT_STATUS_OUTDATED = "outdated";

    /**
     * 特例（业务线拆分专用，不限定 code）：老取值 {@link #BIZLINE_SPLIT_SOURCE_VALUE}（"超市改装"）
     * 迁移到 {@link #BIZLINE_SPLIT_TARGET_NEW_BIZLINE}（"改装超市"）时，id/title 直接换成
     * {@link #BIZLINE_SPLIT_TARGET_VALUE}（"改装升级与车品超市"，是一个词，不是两个并列取值），
     * 不走老业务线子串替换、也不查指标映射表。
     */
    private static final String BIZLINE_SPLIT_SOURCE_VALUE = "改装超市";
    private static final String BIZLINE_SPLIT_TARGET_NEW_BIZLINE = "超市改装";
    private static final String BIZLINE_SPLIT_TARGET_VALUE = "改装升级与车品超市";

    /** 已知的复合 code/id 前缀（剥离顺序：先试前缀），对应前端 constant/index.js LOD_FIELD_ID_PREFIX 等 */
    private static final String LOD_PREFIX = "lod:";
    private static final String ANALYSIS_PREFIX = "analysis:";
    private static final List<String> KNOWN_PREFIXES = Arrays.asList(LOD_PREFIX, ANALYSIS_PREFIX);

    /** 已知的复合 code/id 分隔符/后缀（剥离顺序：先试更具体的 ___t_ 目标同环比分隔符，再试 ___ 同环比分隔符，最后试聚合后缀） */
    private static final String TARGET_THB_SEP = "___t_";
    private static final String ZB_THB_SEP = "___";
    private static final List<String> AGG_SUFFIXES = Arrays.asList("_avg_by_d_r", "_avg_by_d");

    @Autowired
    private BaseDao dao;

    /** 一条 {@code ssm_metric_expansion_field_mapping} 记录里新字段的 code/id/展示名，三者始终来自同一行 */
    @Getter
    public static class FieldMappingRow {
        private final String newCode;
        private final String newId;
        private final String newTitle;

        FieldMappingRow(String newCode, String newId, String newTitle) {
            this.newCode = newCode;
            this.newId = newId;
            this.newTitle = newTitle;
        }
    }

    /**
     * 指标 code/id 映射结果：按老 code、老 id 两个维度各建一份索引，指向同一批 {@link FieldMappingRow}，
     * 保证不管从 code 还是 id 找进来，拿到的新 code/新 id/新展示名都是同一行数据，不会互相矛盾。
     */
    @Getter
    public static class MetricFieldMapping {
        private final Map<String, FieldMappingRow> byOldCode;
        private final Map<String, FieldMappingRow> byOldId;

        MetricFieldMapping(Map<String, FieldMappingRow> byOldCode, Map<String, FieldMappingRow> byOldId) {
            this.byOldCode = byOldCode;
            this.byOldId = byOldId;
        }

        public boolean isEmpty() {
            return byOldCode.isEmpty() && byOldId.isEmpty();
        }
    }

    /**
     * 看板自身结构性引用的重新映射：这次复制过程里生成的 widgetId、以及既有的 queryTplId/viewId
     * 迁移映射表查询——这些不是"指标 code/id"，是看板/查询模板/视图自己的 id，不走
     * {@link MetricFieldMapping}。三个函数查不到映射时的兜底行为由调用方决定（通常 widgetId/tplId
     * 保留旧值，viewId 返回 null 表示置空，见类注释第4条）。
     */
    public static class WidgetRefMapping {
        private final Map<String, String> widgetIdMapping;
        private final Function<String, String> tplIdRemapper;
        private final Function<String, String> viewIdRemapper;

        public WidgetRefMapping(Map<String, String> widgetIdMapping, Function<String, String> tplIdRemapper,
                                 Function<String, String> viewIdRemapper) {
            this.widgetIdMapping = widgetIdMapping;
            this.tplIdRemapper = tplIdRemapper;
            this.viewIdRemapper = viewIdRemapper;
        }

        private String remapWidgetId(String oldWidgetId) {
            return widgetIdMapping.getOrDefault(oldWidgetId, oldWidgetId);
        }

        private String remapTplId(String oldTplId) {
            return tplIdRemapper.apply(oldTplId);
        }

        private String remapViewId(String oldViewId) {
            return viewIdRemapper.apply(oldViewId);
        }
    }

    /** 不限定 tplId/viewId 的全局映射（兜底用，见 {@link #loadMetricCodeMapping(String, String, String, String)}） */
    public MetricFieldMapping loadMetricCodeMapping(String oldBizLine, String newBizLine) {
        return loadMetricCodeMapping(oldBizLine, newBizLine, null, null);
    }

    /**
     * 按来源/目标业务线（以及可选的 tplId/viewId）加载指标 code/id/展示名映射。
     * 传入 tplId/viewId 时查出的是这次具体迁移实例（同一个老查询模板/老视图）产出的精确对应关系；
     * 不传时是该业务线对下全部的映射（用于取不到精确范围时的兜底，或非查询模板类 widget）。
     * 只取 {@code old_field_code}/{@code old_field_id}/{@code new_field_code}/{@code new_field_id}/
     * {@code new_field_title} 这几列，不读 {@code source_metric_code}/{@code target_metric_code}
     * （那是旧 {@code ssm_metric_expansion_mapping} 表遗留下来的字段，不作为映射来源）。
     */
    public MetricFieldMapping loadMetricCodeMapping(String oldBizLine, String newBizLine, String tplId, String viewId) {
        Map<String, Object> params = new HashMap<>();
        params.put("sourceBusinessline", oldBizLine);
        params.put("targetBusinessline", newBizLine);
        params.put("tplId", tplId);
        params.put("viewId", viewId);
        List<MetricCodeMappingEntity> rows = dao.queryObjectList(
                "ssm.migrate.bizsplit.queryMetricCodeMapping", params, MetricCodeMappingEntity.class);
        Map<String, FieldMappingRow> byOldCode = new HashMap<>();
        Map<String, FieldMappingRow> byOldId = new HashMap<>();
        if (CollUtil.isNotEmpty(rows)) {
            for (MetricCodeMappingEntity row : rows) {
                FieldMappingRow mapped = new FieldMappingRow(row.getNewFieldCode(), row.getNewFieldId(), row.getNewFieldTitle());
                if (StrUtil.isNotEmpty(row.getOldFieldCode())) {
                    byOldCode.put(row.getOldFieldCode(), mapped);
                }
                if (StrUtil.isNotEmpty(row.getOldFieldId())) {
                    byOldId.put(row.getOldFieldId(), mapped);
                }
            }
        }
        return new MetricFieldMapping(byOldCode, byOldId);
    }

    /** 名称类纯文本字段（看板名/描述、widget 标题/描述等）的老业务线子串替换 */
    public String rewriteText(String text, String oldBizLine, String newBizLine) {
        if (text == null || StrUtil.isEmpty(oldBizLine) || oldBizLine.equals(newBizLine)) {
            return text;
        }
        return text.replace(oldBizLine, newBizLine);
    }

    /**
     * 看板 widgetOptions / 查询模板 tplConfig 的整体改写：解析成通用 JSON 树，逐节点处理后原样重新序列化。
     * 传入的 json 可能是 JSONObject 也可能是 JSONArray 结构（视具体 widget 类型而定）。
     * {@code widgetRefMapping} 为 null 时跳过 targetWidgetId/viewId/AI依赖引用的重新映射（调用方没有这个上下文，
     * 比如未来只处理查询模板自身内容、不涉及看板 widget 结构的场景）。
     */
    public String rewriteConfigJson(String json, String oldBizLine, String newBizLine,
                                     MetricFieldMapping fieldMapping, WidgetRefMapping widgetRefMapping) {
        if (StrUtil.isEmpty(json)) {
            return json;
        }
        Object parsed;
        try {
            parsed = JSON.parse(json);
        } catch (Exception e) {
            // 不是合法 JSON（极少数遗留数据/纯文本配置），当普通文本做子串替换，不做 code 映射
            return rewriteText(json, oldBizLine, newBizLine);
        }
        Object rewritten = rewriteNode(parsed, oldBizLine, newBizLine, fieldMapping, widgetRefMapping);
        // fastjson 默认序列化会整个丢掉值为 null 的 key（而不是写成 "key":null）——scriptId/viewId 故意置空
        // 就是要保留这个 key（前端按 key 是否存在/是否为 null 判断，不是按 key 缺失判断），必须显式带上这个开关。
        return JSON.toJSONString(rewritten, SerializerFeature.WriteMapNullValue);
    }

    /** 逗号分隔的 code 列表（{@code tplConfigFieldDimCodes}/{@code tplConfigFieldMeasureCodes}）按同一份指标映射做逐个替换 */
    public String rewriteCodeList(String commaSeparatedCodes, MetricFieldMapping fieldMapping) {
        if (StrUtil.isEmpty(commaSeparatedCodes)) {
            return commaSeparatedCodes;
        }
        return Arrays.stream(commaSeparatedCodes.split(","))
                .map(String::trim)
                .filter(StrUtil::isNotEmpty)
                .map(code -> {
                    ResolvedRef resolved = resolveCodeOrId(code, true, fieldMapping);
                    return resolved != null ? resolved.newValue : code;
                })
                .collect(Collectors.joining(","));
    }

    private Object rewriteNode(Object node, String oldBizLine, String newBizLine,
                                MetricFieldMapping fieldMapping, WidgetRefMapping widgetRefMapping) {
        if (node instanceof JSONObject) {
            JSONObject src = (JSONObject) node;

            String codeValue = src.getString(CODE_KEY);
            String idValue = src.getString(ID_KEY);
            String originCodeValue = codeValue;
            String originIdValue = idValue;

            // AggField 等聚合/派生字段：originCode/originId 是未加后缀的真实值，优先按它们查表；
            // 普通字段没有 origin* 键，直接按 code/id 查（内置前后缀剥离兜底，见 resolveCodeOrId）。
            ResolvedRef originCodeResolved = originCodeValue != null ? resolveCodeOrId(originCodeValue, true, fieldMapping) : null;
            ResolvedRef originIdResolved = originIdValue != null ? resolveCodeOrId(originIdValue, false, fieldMapping) : null;
            ResolvedRef codeResolved = originCodeValue == null && codeValue != null
                    ? resolveCodeOrId(codeValue, true, fieldMapping) : null;
            ResolvedRef idResolved = originIdValue == null && idValue != null
                    ? resolveCodeOrId(idValue, false, fieldMapping) : null;

            FieldMappingRow fieldRow = originCodeResolved != null ? originCodeResolved.row
                    : originIdResolved != null ? originIdResolved.row
                    : codeResolved != null ? codeResolved.row
                    : idResolved != null ? idResolved.row
                    : null;
            // 不管这一行映射是通过 code/id/originCode/originId 哪一个键查到的，code 和 id 都要一起换成
            // 同一行的新值——见 MetricFieldMapping 类注释"不管从 code 还是 id 找进来，拿到的新 code/新 id/
            // 新展示名都是同一行数据"。之前 code 只认 codeResolved、id 只认 idResolved，各自独立查表，
            // 命中率取决于"这个 widget 里的 code/id 字符串是否恰好也出现在映射表对应列里"——同一个字段
            // 常见只把 code 或只把 id 精确对上映射表（另一个可能是别的粒度/别的写法），导致查到了 code
            // 却查不到 id（或反过来），此时应该直接用查到的这一行的新 id，而不是因为 id 单独查不到就放弃。
            String newBareCode = fieldRow != null && StrUtil.isNotEmpty(fieldRow.getNewCode()) ? fieldRow.getNewCode() : null;
            String newBareId = fieldRow != null && StrUtil.isNotEmpty(fieldRow.getNewId()) ? fieldRow.getNewId() : null;

            // LOD/计算字段：customFieldConfigure 对象上若有 expressionIdMapping 数组，收集老 id -> 新 id，
            // 供同一层的 expression 公式串做 token 替换（[oldId] -> [newId]）
            Object expressionIdMappingValue = src.get(EXPRESSION_ID_MAPPING_KEY);
            Map<String, String> exprIdSwaps = expressionIdMappingValue instanceof JSONArray
                    ? collectExpressionIdSwaps((JSONArray) expressionIdMappingValue, fieldMapping)
                    : null;

            // AI 解读"依赖图表"条目指纹：widgetId + tplId 同时出现
            String widgetIdValue = src.getString(WIDGET_ID_KEY);
            String tplIdValue = src.getString(TPL_ID_KEY);
            boolean isDependEntry = widgetRefMapping != null && widgetIdValue != null && tplIdValue != null;
            String newWidgetIdForDepend = isDependEntry ? widgetRefMapping.remapWidgetId(widgetIdValue) : null;
            String newTplIdForDepend = isDependEntry ? widgetRefMapping.remapTplId(tplIdValue) : null;
            String oldDependValue = isDependEntry ? widgetIdValue + AI_DEPEND_SPLIT_CHAR + tplIdValue : null;

            // AI 解读脚本条目指纹：scriptId + scriptStatus 同时出现，且原来确实有脚本（scriptId 非空）
            boolean isAiScriptEntry = src.containsKey(SCRIPT_ID_KEY) && src.containsKey(SCRIPT_STATUS_KEY)
                    && StrUtil.isNotEmpty(src.getString(SCRIPT_ID_KEY));

            JSONObject result = new JSONObject(true);
            for (Map.Entry<String, Object> entry : src.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                if (CODE_KEY.equals(key) && value instanceof String) {
                    if (originCodeValue != null) {
                        result.put(key, newBareCode != null
                                ? codeValue.replace(originCodeValue, newBareCode)
                                : rewriteText((String) value, oldBizLine, newBizLine));
                    } else {
                        result.put(key, newBareCode != null
                                ? newBareCode : rewriteText((String) value, oldBizLine, newBizLine));
                    }
                } else if (ID_KEY.equals(key) && value instanceof String) {
                    // 指标映射表查不到时（比如这个 id 存的根本不是指标 id，是"业务线"维度筛选条件的取值本身，
                    // 如 values[].id = "保养"），要按普通文本子串替换兜底，不能原样留着老业务线名——
                    // 这正是类注释第1条说的"筛选器...取值本身"，只是之前这里查不到映射就直接原样返回，漏了兜底。
                    String bizLineSplitOverride = resolveBizLineSplitOverride((String) value, newBizLine);
                    if (bizLineSplitOverride != null) {
                        result.put(key, bizLineSplitOverride);
                    } else if (originIdValue != null) {
                        result.put(key, newBareId != null
                                ? idValue.replace(originIdValue, newBareId)
                                : rewriteText((String) value, oldBizLine, newBizLine));
                    } else {
                        result.put(key, newBareId != null
                                ? newBareId : rewriteText((String) value, oldBizLine, newBizLine));
                    }
                } else if (ORIGIN_CODE_KEY.equals(key) && value instanceof String) {
                    result.put(key, newBareCode != null
                            ? newBareCode : rewriteText((String) value, oldBizLine, newBizLine));
                } else if (ORIGIN_ID_KEY.equals(key) && value instanceof String) {
                    result.put(key, newBareId != null
                            ? newBareId : rewriteText((String) value, oldBizLine, newBizLine));
                } else if (LOD_MEASURE_ID_KEY.equals(key) && value instanceof String) {
                    ResolvedRef lodResolved = resolveCodeOrId((String) value, false, fieldMapping);
                    result.put(key, lodResolved != null ? lodResolved.newValue : value);
                } else if (ID_ARRAY_KEYS.contains(key) && value instanceof JSONArray) {
                    JSONArray ids = (JSONArray) value;
                    JSONArray mapped = new JSONArray(ids.size());
                    for (Object id : ids) {
                        mapped.add(id instanceof String ? remapBareFieldRef((String) id, fieldMapping) : id);
                    }
                    result.put(key, mapped);
                } else if (EXPRESSION_KEY.equals(key) && value instanceof String && CollUtil.isNotEmpty(exprIdSwaps)) {
                    String newExpr = (String) value;
                    for (Map.Entry<String, String> swap : exprIdSwaps.entrySet()) {
                        newExpr = newExpr.replace("[" + swap.getKey() + "]", "[" + swap.getValue() + "]");
                    }
                    result.put(key, rewriteText(newExpr, oldBizLine, newBizLine));
                } else if (isDependEntry && WIDGET_ID_KEY.equals(key) && value instanceof String) {
                    result.put(key, newWidgetIdForDepend);
                } else if (isDependEntry && TPL_ID_KEY.equals(key) && value instanceof String) {
                    result.put(key, newTplIdForDepend);
                } else if (isDependEntry && VALUE_KEY.equals(key) && value instanceof String && value.equals(oldDependValue)) {
                    result.put(key, newWidgetIdForDepend + AI_DEPEND_SPLIT_CHAR + newTplIdForDepend);
                } else if (isAiScriptEntry && SCRIPT_ID_KEY.equals(key)) {
                    result.put(key, null);
                } else if (isAiScriptEntry && SCRIPT_STATUS_KEY.equals(key)) {
                    result.put(key, SCRIPT_STATUS_OUTDATED);
                } else if (FIELD_NAME_KEYS.contains(key) && value instanceof String) {
                    String bizLineSplitOverride = resolveBizLineSplitOverride((String) value, newBizLine);
                    if (bizLineSplitOverride != null) {
                        result.put(key, bizLineSplitOverride);
                    } else if (fieldRow != null && StrUtil.isNotEmpty(fieldRow.getNewTitle())) {
                        result.put(key, fieldRow.getNewTitle());
                    } else {
                        result.put(key, rewriteText((String) value, oldBizLine, newBizLine));
                    }
                } else if (widgetRefMapping != null && TARGET_WIDGET_ID_KEY.equals(key) && value instanceof String) {
                    result.put(key, widgetRefMapping.remapWidgetId((String) value));
                } else if (widgetRefMapping != null && VIEW_ID_KEY.equals(key) && value instanceof String) {
                    result.put(key, widgetRefMapping.remapViewId((String) value));
                } else {
                    result.put(key, rewriteNode(value, oldBizLine, newBizLine, fieldMapping, widgetRefMapping));
                }
            }
            return result;
        }
        if (node instanceof JSONArray) {
            JSONArray src = (JSONArray) node;
            JSONArray result = new JSONArray(src.size());
            for (Object item : src) {
                result.add(rewriteNode(item, oldBizLine, newBizLine, fieldMapping, widgetRefMapping));
            }
            return result;
        }
        if (node instanceof String) {
            return rewriteText((String) node, oldBizLine, newBizLine);
        }
        return node;
    }

    /**
     * 特例覆盖：{@code value} 精确等于 {@link #BIZLINE_SPLIT_SOURCE_VALUE}（"超市改装"）且这次目标
     * 业务线正好是 {@link #BIZLINE_SPLIT_TARGET_NEW_BIZLINE}（"改装超市"）时返回
     * {@link #BIZLINE_SPLIT_TARGET_VALUE}（一个词，不是拆成两个取值）；不满足则返回 null，
     * 调用方按原来的逻辑（指标映射表查值/老业务线子串替换）继续处理。
     */
    private String resolveBizLineSplitOverride(String value, String newBizLine) {
        return BIZLINE_SPLIT_SOURCE_VALUE.equals(value) && BIZLINE_SPLIT_TARGET_NEW_BIZLINE.equals(newBizLine)
                ? BIZLINE_SPLIT_TARGET_VALUE : null;
    }

    /** 从 expressionIdMapping 数组的原始（改写前）内容里收集 老id -> 新id，供 expression 公式串做 token 替换 */
    private Map<String, String> collectExpressionIdSwaps(JSONArray expressionIdMapping, MetricFieldMapping fieldMapping) {
        Map<String, String> swaps = new HashMap<>();
        for (Object item : expressionIdMapping) {
            if (!(item instanceof JSONObject)) {
                continue;
            }
            JSONObject obj = (JSONObject) item;
            String oldId = obj.getString(ID_KEY);
            if (oldId == null) {
                continue;
            }
            String oldCode = obj.getString(CODE_KEY);
            ResolvedRef resolved = oldCode != null ? resolveCodeOrId(oldCode, true, fieldMapping)
                    : resolveCodeOrId(oldId, false, fieldMapping);
            if (resolved != null && StrUtil.isNotEmpty(resolved.row.getNewId())) {
                swaps.put(oldId, resolved.row.getNewId());
            }
        }
        return swaps;
    }

    /** "裸 id/code 数组"元素重映射：先按 id 查，查不到按 code 查，都查不到原样保留（含哨兵值 "all"） */
    private String remapBareFieldRef(String rawValue, MetricFieldMapping fieldMapping) {
        if (rawValue == null) {
            return null;
        }
        ResolvedRef byId = resolveCodeOrId(rawValue, false, fieldMapping);
        if (byId != null) {
            return byId.newValue;
        }
        ResolvedRef byCode = resolveCodeOrId(rawValue, true, fieldMapping);
        return byCode != null ? byCode.newValue : rawValue;
    }

    /** 一次 code/id 查表的结果：命中的映射行 + 拼回前后缀后的最终字符串 */
    private static class ResolvedRef {
        final FieldMappingRow row;
        final String newValue;

        ResolvedRef(FieldMappingRow row, String newValue) {
            this.row = row;
            this.newValue = newValue;
        }
    }

    /**
     * 按 code 或 id 查表：先精确匹配；查不到时剥离已知前后缀（lod:/analysis: 前缀，___t_.../___...
     * 分隔符，_avg_by_d 系聚合后缀）用剥离后的裸值再查一次，命中则把同样的前后缀拼回新值上。
     * 两次都查不到返回 null（调用方保留原值）。
     */
    private ResolvedRef resolveCodeOrId(String rawValue, boolean isCode, MetricFieldMapping fieldMapping) {
        if (rawValue == null) {
            return null;
        }
        Map<String, FieldMappingRow> map = isCode ? fieldMapping.getByOldCode() : fieldMapping.getByOldId();
        FieldMappingRow direct = map.get(rawValue);
        if (direct != null) {
            String newBare = isCode ? direct.getNewCode() : direct.getNewId();
            return new ResolvedRef(direct, StrUtil.isNotEmpty(newBare) ? newBare : rawValue);
        }
        Affix affix = stripAffix(rawValue);
        if (affix.bare.equals(rawValue)) {
            return null;
        }
        FieldMappingRow affixRow = map.get(affix.bare);
        if (affixRow == null) {
            return null;
        }
        String newBare = isCode ? affixRow.getNewCode() : affixRow.getNewId();
        if (StrUtil.isEmpty(newBare)) {
            return new ResolvedRef(affixRow, rawValue);
        }
        return new ResolvedRef(affixRow, affix.prefix + newBare + affix.suffix);
    }

    private static class Affix {
        final String prefix;
        final String bare;
        final String suffix;

        Affix(String prefix, String bare, String suffix) {
            this.prefix = prefix;
            this.bare = bare;
            this.suffix = suffix;
        }
    }

    private Affix stripAffix(String raw) {
        String prefix = "";
        String working = raw;
        for (String knownPrefix : KNOWN_PREFIXES) {
            if (working.startsWith(knownPrefix)) {
                prefix = knownPrefix;
                working = working.substring(knownPrefix.length());
                break;
            }
        }

        String suffix = "";
        int targetSepIdx = working.indexOf(TARGET_THB_SEP);
        if (targetSepIdx > 0) {
            suffix = working.substring(targetSepIdx);
            working = working.substring(0, targetSepIdx);
        } else {
            int zbSepIdx = working.indexOf(ZB_THB_SEP);
            if (zbSepIdx > 0) {
                suffix = working.substring(zbSepIdx);
                working = working.substring(0, zbSepIdx);
            } else {
                for (String aggSuffix : AGG_SUFFIXES) {
                    if (working.endsWith(aggSuffix) && working.length() > aggSuffix.length()) {
                        suffix = aggSuffix;
                        working = working.substring(0, working.length() - aggSuffix.length());
                        break;
                    }
                }
            }
        }
        return new Affix(prefix, working, suffix);
    }
}
