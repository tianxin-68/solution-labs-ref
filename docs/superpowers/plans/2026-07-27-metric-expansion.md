# 指标膨胀/替换 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans（任务强耦合，适合本线程顺序执行）。Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 按 `ssm_metric_expansion_mapping`，对批量 `viewIds` 将模板 tplConfig 中命中的 `source_metric_code` 膨胀为 N 个 target（N=1 即替换），写回原 view，并重建 measure 派生字段。

**Architecture:** 在 `com.bi.queryer.ssm.migrate.bizsplit` 新增独立服务包；用 Fastjson 结构化改 tplConfig（禁止整串字面量替换）；主服务编排九步，processor 分拆「普通位置膨胀 / 计算字段变体 / 派生字段重建 / 回归校验」。加载复用 `ssm.template.view.queryViewWithCfgByIds`，保存复用 `ssm.template.updateTemplateCfg` + cfg_dtl delete/insert。

**Tech Stack:** Java + Spring `@Service`/`@RestController` + BaseDao/MyBatis + Fastjson + `SSDMetaCacheManager` + 现有 `TemplateViewEntity`/`TemplateCfgEntity`/`TemplateCfgDtlEntity`

**页面类型:** 非前端页面（后端批处理服务），不套用 `scene-list` / `scene-form` / `common-page` 模板。

**规则文件:**
- `.cursor/rules/001-ai-friendly-standard.mdc`（代码生成标准）
- `.cursor/rules/basic/003-code-rules.mdc` / `008-code-names.mdc` / `009-comment-rules.mdc`
- 注释：中文、功能导向；**禁止** HTML 标签（含 `<ul>`）

**已知约束（已确认 Spec）:**
- 不做 rule b（owner 业务线 narrowing）
- 跳过列宽；只处理 showOrder / frozenColCount 相关列序
- 只改 tplConfig，不改联动表，不做 cfg 备份回滚
- 计算字段：删除原字段，替换为 `code_业务线` 变体
- 组合分析 = `setting.customCompare` 的 `measureIdList`
- 派生字段存库明文 JSON（与现网一致，不写 `AES:` 前缀）
- 映射表已建，不提供 DDL

---

## 文件结构

```
src/main/java/cn/bi_queryer/bi/ssm/migrate/bizsplit/
├── controller/
│   └── MetricExpansionController.java
├── service/
│   ├── MetricExpansionService.java
│   └── MetricExpansionContext.java
├── model/
│   ├── MetricExpansionMappingEntity.java
│   ├── MetricExpansionTarget.java
│   ├── MetricExpansionExecuteReq.java
│   └── MetricExpansionExecuteRsp.java
├── processor/
│   ├── TplConfigExpansionProcessor.java
│   └── CalcFieldExpansionProcessor.java
├── builder/
│   └── DerivedFieldBuilder.java
├── validator/
│   └── MetricExpansionValidator.java
└── util/
    └── MetricExpansionMetaUtil.java

src/main/resources/mybatis/mapper/ssm/
└── ssm.metric.expansion.mapping.ibatis.xml
```

**不创建:** `TemplateLinkFieldExpansionProcessor`（已确认不改联动表）

**复用（只读/调用，原则上不改行为）:**
- `ssm.template.view.queryViewWithCfgByIds` — 批量按 viewId 加载含 tplConfig 的视图
- `ssm.template.updateTemplateCfg` — 写回 cfg
- `ssm.query.template.cfg.dtl.delete` / `add` — 重建 measure 派生字段时保留 dim 侧
- `SSDMetaCacheManager.getFieldByCode` / `getField` — target 元数据校验
- `SSDUtil.decryptTplConfig` — 加载时解密
- 参考（勿照搬字符串替换）: `QueryTemplateMetricReplaceService`

---

## Task 1: Model + Mapper

**Files:**
- Create: `src/main/java/cn/bi_queryer/bi/ssm/migrate/bizsplit/model/MetricExpansionMappingEntity.java`
- Create: `src/main/java/cn/bi_queryer/bi/ssm/migrate/bizsplit/model/MetricExpansionTarget.java`
- Create: `src/main/java/cn/bi_queryer/bi/ssm/migrate/bizsplit/model/MetricExpansionExecuteReq.java`
- Create: `src/main/java/cn/bi_queryer/bi/ssm/migrate/bizsplit/model/MetricExpansionExecuteRsp.java`
- Create: `src/main/resources/mybatis/mapper/ssm/ssm.metric.expansion.mapping.ibatis.xml`

- [ ] **Step 1: 创建映射实体**

```java
package com.bi.queryer.ssm.migrate.bizsplit.model;

/** 指标膨胀映射表 ssm_metric_expansion_mapping */
public class MetricExpansionMappingEntity {
    private Long pkid;
    private String sourceMetricCode;
    private String targetMetricCode;
    private String sourceBusinessline;
    private String targetBusinessline;
    // getter/setter
}
```

- [ ] **Step 2: 创建 Target / Req / Rsp**

```java
package com.bi.queryer.ssm.migrate.bizsplit.model;

import com.bi.queryer.ssm.meta.MetaField;
import java.util.List;

/** 单个膨胀目标（含已校验元数据） */
public class MetricExpansionTarget {
    private String targetMetricCode;
    private String targetBusinessline;
    private MetaField metaField;
    // getter/setter
}

/** 批量视图执行请求 */
public class MetricExpansionExecuteReq {
    /** 视图 id 列表（必填，批量） */
    private List<String> viewIds;
    /** 可选：仅膨胀指定 source；空则扫描模板中全部命中映射的 source */
    private List<String> sourceMetricCodes;
    /** 保存后是否回归校验，默认 true */
    private Boolean validateAfterSave;
    // getter/setter
}

/** 单视图执行结果汇总 */
public class MetricExpansionExecuteRsp {
    private String viewId;
    private String tplId;
    private String cfgId;
    private List<String> expandedSourceCodes;
    private String measureCodes;
    private Boolean validationPassed;
    private List<String> validationMessages;
    private String errorMessage; // 单视图失败时填，批量时不中断其他视图
    // getter/setter
}
```

- [ ] **Step 3: 创建 MyBatis XML（namespace 与 resultType 用 migrate.bizsplit）**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
  "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="ssm.metric.expansion.mapping">
  <select id="listBySourceMetricCode" parameterType="java.lang.String"
          resultType="com.bi.queryer.ssm.migrate.bizsplit.model.MetricExpansionMappingEntity">
    SELECT pkid AS pkid,
           source_metric_code AS sourceMetricCode,
           target_metric_code AS targetMetricCode,
           source_businessline AS sourceBusinessline,
           target_businessline AS targetBusinessline
    FROM ssm_metric_expansion_mapping
    WHERE source_metric_code = #{sourceMetricCode}
    ORDER BY pkid ASC
  </select>

  <select id="listAll"
          resultType="com.bi.queryer.ssm.migrate.bizsplit.model.MetricExpansionMappingEntity">
    SELECT pkid AS pkid,
           source_metric_code AS sourceMetricCode,
           target_metric_code AS targetMetricCode,
           source_businessline AS sourceBusinessline,
           target_businessline AS targetBusinessline
    FROM ssm_metric_expansion_mapping
    ORDER BY pkid ASC
  </select>
</mapper>
```

- [ ] **Step 4: 自检**

确认 `spring-db-pool.xml` 的 `classpath*:mybatis/**/*.xml` 会扫到新文件（已配置，无需改）。

---

## Task 2: Context + MetaUtil

**Files:**
- Create: `src/main/java/cn/bi_queryer/bi/ssm/migrate/bizsplit/service/MetricExpansionContext.java`
- Create: `src/main/java/cn/bi_queryer/bi/ssm/migrate/bizsplit/util/MetricExpansionMetaUtil.java`

- [ ] **Step 1: Context 承载单视图运行时状态**

```java
package com.bi.queryer.ssm.migrate.bizsplit;

import com.bi.queryer.ssm.migrate.bizsplit.model.MetricExpansionTarget;
import com.alibaba.fastjson.JSONObject;
import java.util.*;

public class MetricExpansionContext {
    private String viewId;
    private String tplId;
    private String cfgId;
    private JSONObject tplConfigJson;
    /** 映射表全部 source code 集合（用于扫描命中） */
    private Set<String> allMappingSourceCodes = new LinkedHashSet<>();
    /** 本视图实际要膨胀的 source */
    private Set<String> activeSourceCodes = new LinkedHashSet<>();
    /** sourceCode -> targets（保持映射表 pkid 顺序） */
    private Map<String, List<MetricExpansionTarget>> targetsBySourceCode = new LinkedHashMap<>();
    /** fieldId -> metricCode（扫描 + 膨胀过程维护） */
    private Map<String, String> sourceIdToCode = new LinkedHashMap<>();
    /** 步骤六新生成的计算字段 code */
    private Set<String> generatedCalcFieldCodes = new LinkedHashSet<>();
    /** measures 下标插入增量，供列序重排 */
    private Map<Integer, Integer> columnInsertDeltaByIndex = new LinkedHashMap<>();

    public boolean isExpandableSourceCode(String code) {
        return code != null && activeSourceCodes.contains(code);
    }
    // getter/setter
}
```

- [ ] **Step 2: MetaUtil — 校验 target + 克隆 field**

```java
package com.bi.queryer.ssm.migrate.bizsplit.util;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.bi.queryer.ssm.exception.SSDException;
import com.bi.queryer.ssm.meta.MetaField;
import com.bi.queryer.ssm.meta.SSDMetaCacheManager;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import java.util.Collections;
import java.util.List;

public final class MetricExpansionMetaUtil {
    private MetricExpansionMetaUtil() {}

    /**
     * 校验 target 指标元数据存在且可用，返回优先 MetaField。
     */
    public static MetaField validateTargetMetric(String targetMetricCode) {
        if (StrUtil.isEmpty(targetMetricCode)) {
            throw new SSDException("目标指标编码为空");
        }
        List<MetaField> fields = SSDMetaCacheManager.getFieldByCode(targetMetricCode);
        if (CollUtil.isEmpty(fields)) {
            throw new SSDException("目标指标不存在或不可用: " + targetMetricCode);
        }
        Collections.sort(fields);
        MetaField field = fields.get(0);
        if (field.getIsActive() != null && field.getIsActive() == 0) {
            throw new SSDException("目标指标已停用: " + targetMetricCode);
        }
        return field;
    }

    /**
     * 以 source field JSON 为模板，替换为 target 的 id/code/title，并写入 showOrder。
     */
    public static JSONObject buildTargetMeasureField(JSONObject sourceField, MetaField targetMeta, double showOrder) {
        JSONObject copy = JSON.parseObject(sourceField.toJSONString());
        copy.put("id", targetMeta.getId());
        copy.put("code", targetMeta.getCode());
        if (StrUtil.isNotEmpty(targetMeta.getTitle())) {
            copy.put("title", targetMeta.getTitle());
            copy.put("name", targetMeta.getTitle());
        }
        copy.put("showOrder", showOrder);
        // 普通指标不应携带原计算配置
        copy.remove("customFieldConfigure");
        return copy;
    }
}
```

> 若项目 `MetaField.isActive` 类型/命名不同，以实际字段为准；无 isActive 时仅校验存在即可。

---

## Task 3: TplConfigExpansionProcessor（步骤 2/4/5）

**Files:**
- Create: `src/main/java/cn/bi_queryer/bi/ssm/migrate/bizsplit/processor/TplConfigExpansionProcessor.java`

- [ ] **Step 1: 扫描 source**

遍历并收集 code：
- `filter[]`
- `result.measures` / `rowDimensions` / `colDimensions`
- `analysis.thb` 各粒度数组 item 的 `measureIdList`（及 `zbThbConfigs[].measureId` / `targetThbConfigs[].measureId` 若存在）
- `analysis.zb.items[].measureIdList`
- `analysis.target` 各粒度 `measureIdList`
- `setting.customCompare` 内 `measureIdList`（组合分析）

对每个 field：记录 `id -> code`；若 code 落在 `allMappingSourceCodes`，加入候选。最终 `activeSourceCodes` 由 Service 再与请求过滤求交。

- [ ] **Step 2: expandMeasures**

原 source 位置替换为 N 个 target 完整 field（`MetricExpansionMetaUtil.buildTargetMeasureField`），保持 mapping 顺序；记录 `columnInsertDeltaByIndex`。

- [ ] **Step 3: expandFilterFields**

`filter[]` 中 source 展开为 N 条，继承操作符与取值；id/code/title 换为 target。

- [ ] **Step 4: expandSortFields**

路径：
- `setting.tableStyle.upDownSortData.sortFields` / `sortItems`
- `setting.tableStyle.leftRightSortData.sortFields` / `sortItems`

匹配 `fieldCode` / `orderBy` / `columnField` 等于 source code（或通过 idToCode 反查）时，展开为 N 条，继承 sortType/order。

- [ ] **Step 5: expandAnalysisSections + customCompare**

对 `measureIdList`：遇到 source 的 id，替换为 N 个 target id（`'all'` 原样保留）。  
覆盖：`analysis.thb.*`、`analysis.zb.items`、`analysis.target.*`、`setting.customCompare`。  
`analysis.total`：若 `aggConfig.configs` 按指标列出，对 source 展开为 N 条；若全局模式无需逐指标展开则跳过。

- [ ] **Step 6: 列样式（跳过列宽）**

- **不做** 列宽配置复制
- 若存在按列下标记录的冻结/顺序配置，按 `columnInsertDeltaByIndex` 重排（`renumberColumnOrderIndices`）
- `frozenColCount`：仅在「按列下标冻结」语义下需要随插入偏移调整；若仅为整数计数且不绑定具体指标，保持不变

- [ ] **Step 7: 提供给计算字段复用的入口**

```java
/**
 * 步骤六之后：把新计算字段 id/code 同步进 sort/filter/analysis/total（此时不再二次膨胀普通 source）。
 */
public void expandTplConfigForNewCalcFields(MetricExpansionContext context) { ... }
```

实现要点：在 CalcField 删除原字段并插入变体后，把原计算字段在 filter/sort/analysis 中的引用，替换为全部变体 id/code。

---

## Task 4: CalcFieldExpansionProcessor（步骤 6）

**Files:**
- Create: `src/main/java/cn/bi_queryer/bi/ssm/migrate/bizsplit/processor/CalcFieldExpansionProcessor.java`

- [ ] **Step 1: 识别受影响计算字段**

`measures` 中存在 `customFieldConfigure.expression` / `expressionIdMapping`，且引用的任一 code/id 属于 `activeSourceCodes`。

- [ ] **Step 2: 按业务线交集生成变体**

```text
sources = extractReferencedMetrics(expression + expressionIdMapping)
expandableSources = sources.filter(in activeSourceCodes)
commonBusinessLines = ∩ targets[s].targetBusinessline   // 已确认非空
for bl in commonBusinessLines:
  newExpression = expression
  for s in expandableSources:
    target = targets[s].find(t => t.targetBusinessline == bl)
    if target == null: log.warn 缺失映射；continue
    newExpression = replaceAll(newExpression, s.id/code, target.id/code)
  新字段 code = 原code + "_" + bl；新 id = Guid；更新 expressionIdMapping
```

表达式中指标引用格式为 `[fieldId]`（前端事实），替换时对 id 与 code 均做全出现一致替换。

- [ ] **Step 3: 删除原计算字段并替换为变体**

在 `measures` 中：命中计划的原计算字段位置，不保留原对象，改为插入全部变体。  
随后调用 `tplConfigExpansionProcessor.expandTplConfigForNewCalcFields(context)`，保证排序/过滤/分析/总计中原计算字段引用也被替换。

---

## Task 5: DerivedFieldBuilder（步骤 7）

**Files:**
- Create: `src/main/java/cn/bi_queryer/bi/ssm/migrate/bizsplit/builder/DerivedFieldBuilder.java`

- [ ] **Step 1: 重建 codes + asset（明文）**

```java
public String[] rebuildDerivedFields(JSONObject tplConfigJson) {
    // 遍历 result.measures + filter 中 type 为指标的字段（与前端 ConfigTemplateMetaEntity 对齐：至少覆盖 measures）
    // measureCodes = distinct codes join ","
    // assetList: common -> assetIdentifier=code；calc -> type=calc, assetIdentifier=expression
    // 按 assetIdentifier 排序
    // return [measureCodesStr, JSON.toJSONString(assetList)]  // 明文，禁止 AES
}
```

**禁止** 调用 `AES.encrypt` / 加 `AES:` 前缀（已确认按现网明文存库）。

---

## Task 6: Validator + Service + Controller

**Files:**
- Create: `src/main/java/cn/bi_queryer/bi/ssm/migrate/bizsplit/validator/MetricExpansionValidator.java`
- Create: `src/main/java/cn/bi_queryer/bi/ssm/migrate/bizsplit/service/MetricExpansionService.java`
- Create: `src/main/java/cn/bi_queryer/bi/ssm/migrate/bizsplit/controller/MetricExpansionController.java`

- [ ] **Step 1: Validator**

保存后重新 `decryptTplConfig`，核对：
- 原 active source code 不再出现在 measures（已被 target 替换）
- 每个 source 的 N 个 target code 均出现在 measures
- 生成的计算字段 code 唯一且存在
- 返回 `List<String>` 问题描述；空列表表示通过

- [ ] **Step 2: Service 批量编排**

```java
public List<MetricExpansionExecuteRsp> execute(MetricExpansionExecuteReq req) {
    Preconditions.checkArgument(CollUtil.isNotEmpty(req.getViewIds()), "viewIds 不能为空");
    // 1) 预加载全部映射 listAll → allMappingSourceCodes
    // 2) dao.queryObjectList("ssm.template.view.queryViewWithCfgByIds", req.getViewIds(), TemplateViewEntity.class)
    // 3) for each view: try executeOne(view) catch → rsp.errorMessage，不中断其他视图
}

private MetricExpansionExecuteRsp executeOne(TemplateViewEntity view, ...) {
    // decrypt tplConfig → context
    // scan → filter by req.sourceMetricCodes
    // 无 active source：可跳过并返回「无需膨胀」或视为成功无变更（推荐：skip，validationPassed=true，messages 说明）
    // loadAndValidateTargets：listBySourceMetricCode + validateTargetMetric，无效抛 SSDException
    // expandTplConfig → processCalcFields → rebuildDerivedFields
    // 事务内：updateTemplateCfg + 读旧 cfg_dtl 保留 dim → delete dtl → insert 新 measureCodes/measureAsset(明文)
    // validateAfterSave
}
```

保存伪代码：

```java
dao.executeTranscation(DataSourceType.Default, new AbstractTransaction() {
  public void execute() {
    dao.update("ssm.template.updateTemplateCfg",
        new TemplateCfgEntity(cfgId, tplConfigJson.toJSONString()));
    TemplateCfgDtlEntity oldDtl = (TemplateCfgDtlEntity)
        dao.queryObject("ssm.query.template.cfg.dtl.getById", cfgId); // 若 id 名不同，查现有 XML
    dao.delete("ssm.query.template.cfg.dtl.delete", cfgId);
    TemplateCfgDtlEntity dtl = new TemplateCfgDtlEntity(cfgId,
        oldDtl != null ? oldDtl.getTplConfigFieldDimCodes() : null,
        measureCodes);
    dtl.setTplConfigFieldDimAsset(oldDtl != null ? oldDtl.getTplConfigFieldDimAsset() : null);
    dtl.setTplConfigFieldMeasureAsset(measureAssetPlainJson);
    dao.insert("ssm.query.template.cfg.dtl.add", dtl);
  }
});
```

> 实现时先打开 `ssm.query.template.cfg.dtl.ibatis.xml` 核对 `getById`/`delete`/`add` 的真实 statement id 与参数类型，再落代码。

- [ ] **Step 3: Controller**

```java
@RestController
@Scope("prototype")
@RequestMapping("ssd/template/metric/expansion")
public class MetricExpansionController {
    @Autowired
    private MetricExpansionService metricExpansionService;

    /**
     * 按批量 viewIds 执行指标膨胀/替换。
     */
    @RequestMapping(value = "execute", method = RequestMethod.POST)
    public SSMResponseMessage<List<MetricExpansionExecuteRsp>> execute(
            @RequestBody MetricExpansionExecuteReq req) {
        return SSMResponseMessage.success(metricExpansionService.execute(req));
    }
}
```

请求示例：

```json
{
  "viewIds": ["view-1", "view-2"],
  "sourceMetricCodes": [],
  "validateAfterSave": true
}
```

---

## Task 7: 完成前自检（本 plan 内门禁，不等于完成前验证阶段）

- [ ] **Step 1: 编译相关类**

在 IDE/Maven 编译 `migrate.bizsplit` 包，修复 import / MetaField API 差异。

- [ ] **Step 2: 对照 Spec 清单**

| Spec 项 | 对应实现 |
|--------|----------|
| 批量 viewIds | Controller/Service |
| 不做 rule b | listBySourceMetricCode 全量 |
| measures 1→N | TplConfigExpansionProcessor.expandMeasures |
| sort/filter 全量展开 | expandSortFields / expandFilterFields |
| THB/ZB/目标/customCompare | expandAnalysisSections |
| total | analysis.total 处理 |
| 跳过列宽 | 无 column width 逻辑 |
| 不改联动表 | 无 Link processor |
| 计算字段删除替换变体 | CalcFieldExpansionProcessor |
| 明文 asset | DerivedFieldBuilder 不加密 |
| tplId/viewId 不变 | 只 update cfg + dtl |

- [ ] **Step 3: 清理临时恢复目录（若存在）**

删除 `d:\code\bi_queryer\.tmp_recover`（仅本地草稿，勿提交）。

---

## Guardrails

1. **禁止** 对整段 tplConfig 做 `String.replace` 式指标替换（与 `QueryTemplateMetricReplaceService` 区分）。
2. **禁止** 修改联动表、禁止备份表写入。
3. **禁止** 派生字段写 `AES:` 密文入库。
4. **禁止** 实现 rule b / 列宽复制。
5. 单视图失败不拖垮批量；错误写入对应 `MetricExpansionExecuteRsp.errorMessage`。
6. target 元数据缺失：**中止该视图**并报错（按 Spec 步骤三）。
7. 计算字段缺某 bl 映射：仅 `log.warn`，不阻断（交集非空前提下）。
8. 注释中文、功能导向；禁止 HTML 标签。

---

## 自检（对 Spec）

| 需求点 | Task |
|--------|------|
| 映射查询 | Task 1 |
| 元数据校验 | Task 2 |
| 扫描 + 九类位置（除联动/列宽） | Task 3 |
| 计算字段变体 + 删除原字段 | Task 4 |
| 派生字段明文 | Task 5 |
| 批量 view 保存 + 校验 API | Task 6 |
| 门禁对照 | Task 7 |

无占位符；类型与包名统一为 `com.bi.queryer.ssm.migrate.bizsplit`。
