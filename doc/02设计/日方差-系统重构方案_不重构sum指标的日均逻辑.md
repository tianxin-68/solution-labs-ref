# 日方差（var_pop_by_d）系统重构方案

## 1. 背景与目标

当前多维查询引擎支持用户选择维度、指标、聚合方式、分析计算后，生成 Doris/Trino SQL 并返回查询结果。现有“日均值”能力主要由 `com.bi.queryer.ssm.engine.aggregation` 下的 `AverageByDayAggregator`、`AverageByRealDayAggregator` 以及多个 `*AvgWrapper` 特殊分支支撑。

本次需要新增聚合方式“日方差”：`AggExpressionType.var_pop_by_d`，Doris 二次聚合函数为 `var_pop`。

日方差计算口径：

```sql
先按日对明细数据按原始聚合方式计算，得到 daily_value；
再对 daily_value 做 var_pop(daily_value)。
```

示例：指标原始聚合为 `sum(amount)`，查询粒度为月，日方差应等价于：

```sql
select
    month_dt,
    var_pop(daily_amount) as amount_var_pop_by_d
from (
    select
        day_dt,
        month_dt,
        sum(amount) as daily_amount
    from fact
    where ...
    group by day_dt, month_dt
) d
group by month_dt
```

重构目标：

1. 新增 `AggExpressionType.Var_Pop_By_Day`，code 为 `var_pop_by_d`。
2. 保持现有 `avg_by_d`、`avg_by_d_r` 行为兼容，不改变老查询配置和老模板结果。
3. 将“日均值专用逻辑”重构为“按日预聚合 + 二次聚合”通用能力，后续可扩展日标准差、日最大值、日分位数等类似场景。
4. 消除散落在系统中的 `Avg_By_Day` / `avg_by_d` 硬编码判断，改为聚合类型能力判断。
5. 重点保证兼容性、可扩展性、可维护性。

## 2. 当前问题

### 2.1 聚合类型语义和实现耦合

`AggExpressionType` 目前只定义了枚举值和基础表达式，`Avg_By_Day`、`Avg_By_Day_Real` 是特殊枚举，但“是否需要按日处理”“按自然日还是非空日”“二次聚合函数是什么”等能力没有在枚举或策略中表达。

现状代码通过如下方式判断日均：

```java
aggExpressionType == AggExpressionType.Avg_By_Day
aggExpressionType == AggExpressionType.Avg_By_Day_Real
aggExpressionType.isAvgByDay()
id.endsWith(AggExpressionType.Avg_By_Day.getCode())
code.replace("_" + AggExpressionType.Avg_By_Day.getCode(), "")
```

新增 `var_pop_by_d` 后，如果继续复制这些判断，会导致遗漏风险和长期维护成本继续上升。

### 2.2 当前日均实现不是完整的二阶段聚合抽象

`AverageByDayAggregator` 当前核心逻辑是：先生成原始聚合表达式，再除以天数；对 count distinct 等场景通过 `SingleModelBuilderAvgWrapper` / `DefaultDataSourceSqlBuilderAvgWrapper` 做额外包装。

这能覆盖现有日均历史口径，但不能直接表达日方差。日方差必须拿到“每日聚合后的 daily_value”再执行 `var_pop(daily_value)`，不能通过最终聚合值除以天数推导。

因此，本次不能只在 `AggregatorFactory` 里新增：

```java
case Var_Pop_By_Day:
    return new VarPopByDayAggregator(cxt);
```

如果只加聚合器，外层 SQL 缺少日粒度预聚合结果，方差口径会错误。

### 2.3 日均硬编码散落范围较广

已发现的主要路径包括：

- `src/main/java/cn/bi_queryer/bi/ssm/enums/AggExpressionType.java`
- `src/main/java/cn/bi_queryer/bi/ssm/engine/aggregation/AggregatorFactory.java`
- `src/main/java/cn/bi_queryer/bi/ssm/engine/aggregation/AverageByDayAggregator.java`
- `src/main/java/cn/bi_queryer/bi/ssm/engine/aggregation/AverageByRealDayAggregator.java`
- `src/main/java/cn/bi_queryer/bi/ssm/engine/aggregation/AnalysisAverageByDayAggregator.java`
- `src/main/java/cn/bi_queryer/bi/ssm/engine/aggregation/AnalysisAverageByRealDayAggregator.java`
- `src/main/java/cn/bi_queryer/bi/ssm/engine/SingleModelBuilderAvgWrapper.java`
- `src/main/java/cn/bi_queryer/bi/ssm/engine/DefaultDataSourceSqlBuilderAvgWrapper.java`
- `src/main/java/cn/bi_queryer/bi/ssm/engine/analysis/AnalysisSingleModelSqlBuilderAvgWrapper.java`
- `src/main/java/cn/bi_queryer/bi/ssm/engine/analysis/AnalysisSingleModelAvgTotalSqlBuilder.java`
- `src/main/java/cn/bi_queryer/bi/ssm/engine/lod/LodSingleModelSqlBuilderAvgWrapper.java`
- `src/main/java/cn/bi_queryer/bi/ssm/engine/lod/AnalysisSqlBuilderLodWrapper.java`
- `src/main/java/cn/bi_queryer/bi/ssm/engine/config/QueryConfigureNormalizer.java`
- `src/main/java/cn/bi_queryer/bi/ssm/engine/config/ui/normalizer/UIAggregatorNormalizer.java`
- `src/main/java/cn/bi_queryer/bi/ssm/engine/config/ui/normalizer/UIQueryFieldNormalizer.java`
- `src/main/java/cn/bi_queryer/bi/ssm/engine/config/ui/normalizer/UICalcFieldNormalizer.java`
- `src/main/java/cn/bi_queryer/bi/ssm/engine/ext/MeasureFieldAutoExtender.java`
- `src/main/java/cn/bi_queryer/bi/ssm/engine/acl/AclManager.java`
- `src/main/java/cn/bi_queryer/bi/ssm/engine/analysis/TargetAnalysisMultiModelSqlBuilder.java`
- `src/main/java/cn/bi_queryer/bi/ssm/util/FieldUtil.java`
- `src/main/java/cn/bi_queryer/bi/ssm/util/SSDUtil.java`
- `src/main/java/cn/bi_queryer/bi/ssm/engine/QueryEngine.java`
- `src/main/java/cn/bi_queryer/bi/ssm/llm/LLMAgentApiService.java`
- `src/main/java/cn/bi_queryer/bi/ssm/llm/config/LLMConfigService.java`
- `src/main/java/cn/bi_queryer/bi/ssm/query/field/QueryFieldService.java`
- `src/main/java/cn/bi_queryer/bi/ssm/mgr/targetValue/TargetValueController.java`

这些位置需要从“日均专用判断”迁移为“按日二次聚合能力判断”。

## 3. 推荐设计

### 3.1 引入按日二次聚合能力模型

在 `AggExpressionType` 中新增能力字段或能力方法，让聚合类型自己表达行为。

建议新增枚举值：

```java
Var_Pop_By_Day("var_pop_by_d", "日方差", "日方差", "")
```

建议新增方法：

```java
public boolean isByDayReAggregation();
public boolean isAvgByDay(); // 保留兼容，内部委托 isByDayReAggregation 的 avg 子集
public boolean isCalendarDayAvg();
public boolean isRealDayAvg();
public boolean isDailyVariance();
public String getByDaySecondAggFunction();
public boolean needByDayPreAggregation();
public static boolean isByDayReAggregationCode(String code);
public static AggExpressionType resolveByDayAggBySuffix(String idOrCode);
public static String stripByDayAggSuffix(String idOrCode);
public static String appendByDayAggSuffix(String idOrCode, AggExpressionType aggType);
public static List<AggExpressionType> getByDayReAggregationTypes();
```

推荐能力映射：

| 聚合类型 | code | 是否按日预聚合 | 二次计算 | 日期口径 | 兼容说明 |
| --- | --- | --- | --- | --- | --- |
| Avg_By_Day | `avg_by_d` | 部分场景需要 | `sum(daily_value) / calendar_days` | 自然日，含无数据日 | 保持现有口径 |
| Avg_By_Day_Real | `avg_by_d_r` | 需要 | `avg(daily_value)` 或 `sum(daily_value) / count(non_empty_day)` | 非空日 | 保持现有口径 |
| Var_Pop_By_Day | `var_pop_by_d` | 需要 | `var_pop(daily_value)` | 非空日 | 新增口径 |

关键点：`avg_by_d` 的历史语义是“多日均值”，包含自然日天数逻辑；`avg_by_d_r` 是“非空日均值”。`var_pop_by_d` 按需求描述更接近“对存在明细数据的日聚合值做方差”，默认不补零日。如果产品后续要求无数据日按 0 参与方差，需要额外引入日期日历表补齐，不建议混入本次实现。

### 3.2 引入策略对象，避免枚举膨胀

为了后续扩展，建议不要把所有 SQL 拼接细节都塞进 `AggExpressionType`。枚举只表达“是什么”，策略对象表达“怎么生成 SQL”。

建议新增：

```java
com.bi.queryer.ssm.engine.aggregation.day.DayReAggregationSpec
com.bi.queryer.ssm.engine.aggregation.day.DayReAggregationSpecFactory
```

`DayReAggregationSpec` 建议字段：

```java
private AggExpressionType aggType;
private String secondAggFunction;      // avg / var_pop / stddev_pop / percentile 等
private DayParticipationMode dayMode;  // CALENDAR_DAY / NON_EMPTY_DAY
private boolean requiresDailySubquery;
private boolean supportsDistinct;
private boolean supportsCalcField;
private boolean keepLegacyAvgAlgorithm;
```

`DayParticipationMode`：

```java
CALENDAR_DAY     // 自然日，当前 avg_by_d 口径
NON_EMPTY_DAY    // 非空日，avg_by_d_r 和 var_pop_by_d 口径
```

这样新增下一个类似聚合时，只需新增枚举值和策略映射，不需要在全链路复制硬编码分支。

### 3.3 两阶段 SQL 构建抽象

建议将当前 `*AvgWrapper` 逐步重构为 `*ByDayReAggWrapper`，保留旧类名作为兼容入口或过渡子类。

目标 SQL 构建分为三层：

1. 明细数据源层：沿用 `DefaultDataSourceSqlBuilder` 生成基础明细/预处理数据源。
2. 日预聚合层：按查询维度、最细日期 `BIConsts.MIN_DATE_GRANULARITY_CODE`、必要 grouping 字段进行 group by，计算每日 `daily_value`。
3. 二次聚合层：按最终查询维度进行二次聚合，输出最终指标。

抽象示意：

```sql
select
    final_dims,
    <second_stage_expr>(daily_value) as metric_var_pop_by_d
from (
    select
        final_dims,
        min_date_granularity_code,
        <origin_agg_expr> as daily_value
    from (<base_datasource_sql>) v
    group by final_dims, min_date_granularity_code
) d
group by final_dims
```

不同聚合类型的二次表达式：

```sql
-- avg_by_d：保持历史自然日口径
sum(daily_value) * 1.0000 / calendar_days

-- avg_by_d_r：非空日均
avg(daily_value)
-- 或等价：sum(daily_value) * 1.0000 / count(distinct day)

-- var_pop_by_d：日方差
var_pop(daily_value)
```

注意：现有 `avg_by_d` 对普通 sum/count 类指标可继续走旧算法，降低风险；但建议框架上统一识别为 ByDayReAggregation。第一期实现可以对 `avg_by_d` 保留旧 SQL 生成路径，对 `var_pop_by_d` 走新二阶段路径。第二期再评估是否完全迁移 `avg_by_d` 到二阶段实现。

### 3.4 聚合器层改造

当前聚合器职责是把 `QueryField` 转成聚合表达式。建议拆分为：

- `DefaultAggregator`：普通聚合，保持不变。
- `ByDayFirstStageAggregator`：生成日预聚合层表达式，即按指标原始聚合方式计算 `daily_value`。
- `ByDaySecondStageAggregator`：生成二次聚合表达式，如 `var_pop(alias)`、`avg(alias)`、`sum(alias)/days`。
- `AverageByDayAggregator` / `AverageByRealDayAggregator`：保留为兼容类，内部委托新策略。
- `VarPopByDayAggregator`：新增，内部使用 `ByDaySecondStageAggregator`。
- `AnalysisAverageByDayAggregator` / `AnalysisAverageByRealDayAggregator`：保留分析场景日期天数、同环比、自定义对比、业务日历特殊逻辑，但抽取公共能力供 ByDay 策略复用。

`AggregatorFactory` 改造方向：

```java
if (aggExpressionType.needByDayPreAggregation()) {
    return new ByDaySecondStageAggregator(cxt, DayReAggregationSpecFactory.get(aggExpressionType));
}
return new DefaultAggregator(cxt);
```

分析场景：

```java
if (aggExpressionType.needByDayPreAggregation()) {
    return new AnalysisByDaySecondStageAggregator(cxt, DayReAggregationSpecFactory.get(aggExpressionType));
}
return new DefaultAggregator(cxt);
```

这样以后新增日标准差时，不再需要改多处 switch。

## 4. 关键实现方案

### 4.1 新增 `var_pop_by_d` 元数据和枚举

修改 `AggExpressionType.java`：

1. 新增枚举 `Var_Pop_By_Day("var_pop_by_d", "日方差", "日方差", "")`。
2. 新增 `isByDayReAggregation()`，返回 `Avg_By_Day`、`Avg_By_Day_Real`、`Var_Pop_By_Day`。
3. 保留 `isAvgByDay()`，只返回两个日均类型，避免显示格式等历史逻辑误伤 `var_pop_by_d`。
4. 新增 suffix 处理方法，统一替代各处 `replace("_avg_by_d", "")`。
5. `toListMap()` 当前跳过 `isAvgByDay()`；建议改成按展示策略控制：
   - 默认普通聚合下拉仍可隐藏 ByDay 派生类型。
   - 如果前端已有派生指标方式，不直接展示 `var_pop_by_d`，只由扩展字段提供。
   - 如产品要求直接展示，则通过配置开关灰度展示。

原因：枚举是全链路最稳定的识别入口，先把能力沉淀到枚举/策略，后续代码才能逐步去硬编码。

### 4.2 字段自动扩展

修改 `MeasureFieldAutoExtender.java`。

当前只扩展：

```java
Avg_By_Day
Avg_By_Day_Real
```

建议改为：

```java
AggExpressionType.getAutoExtendAggTypes()
```

初始返回：

```java
Avg_By_Day
Avg_By_Day_Real
Var_Pop_By_Day
```

格式化策略需区分：

- `avg_by_d` / `avg_by_d_r`：继续追加 `BIConsts.AVG_FORMAT_SUFFIX`，保持历史日均展示。
- `var_pop_by_d`：不应追加日均格式后缀，使用数值格式；如产品要求，可新增方差专属格式后缀或默认小数格式。

原因：自动扩展是前端/元数据能否选择到派生指标的入口，必须从硬编码列表改为策略列表。

### 4.3 UI 配置规范化

涉及：

- `UIQueryFieldNormalizer.java`
- `UICalcFieldNormalizer.java`
- `UIAggregatorNormalizer.java`

改造点：

1. 所有识别字段后缀的逻辑改为 `AggExpressionType.resolveByDayAggBySuffix(idOrCode)`。
2. 所有去后缀逻辑改为 `AggExpressionType.stripByDayAggSuffix(idOrCode)`。
3. 所有追加后缀逻辑改为 `AggExpressionType.appendByDayAggSuffix(idOrCode, aggType)`。
4. `UICalcFieldNormalizer` 中引用字段打标从日均专用改为 ByDay 通用：引用字段 id 以 `var_pop_by_d` 结尾时，设置 `aggExpressionType=var_pop_by_d`。
5. `UIAggregatorNormalizer` 中“按日去重后 sum”逻辑仍是日均历史特殊逻辑，不应自动套到 `var_pop_by_d`；但其判断应使用能力方法，避免误删/误改新后缀。

原因：计算字段、LOD 字段、前端传入字段经常使用 id/code 后缀识别派生聚合字段，这里遗漏会导致字段找不到、表达式替换错误、引用原字段错误。

### 4.4 SQL 构建包装器重构

涉及：

- `DefaultDataSourceSqlBuilderAvgWrapper.java`
- `SingleModelBuilderAvgWrapper.java`
- `AnalysisSingleModelSqlBuilderAvgWrapper.java`
- `LodSingleModelSqlBuilderAvgWrapper.java`
- `AnalysisSingleModelAvgTotalSqlBuilder.java`

建议新增通用类并保留旧类兼容：

```java
DefaultDataSourceSqlBuilderByDayReAggWrapper
SingleModelBuilderByDayReAggWrapper
AnalysisSingleModelSqlBuilderByDayReAggWrapper
LodSingleModelSqlBuilderByDayReAggWrapper
AnalysisSingleModelByDayReAggTotalSqlBuilder
```

过渡做法：

```java
public class SingleModelBuilderAvgWrapper extends SingleModelBuilderByDayReAggWrapper { ... }
```

核心改造：

1. `distinctFields` 命名调整为 `byDayReAggFields` 或拆为 `dailyPreAggFields`，因为 `var_pop_by_d` 不一定是 distinct 指标。
2. `prepare()` 中判断从：

```java
aggType == Avg_By_Day || aggType == Avg_By_Day_Real
```

改为：

```java
aggType.needByDayPreAggregation()
```

3. 对 `var_pop_by_d`，无论原始聚合是否 count distinct，都要进入日预聚合路径。
4. 对 `avg_by_d`，为了兼容可保留旧路径，仅当原始聚合为 count distinct 或配置要求时进入预聚合路径。
5. 对 `avg_by_d_r`，建议统一走日预聚合路径，保持非空日口径。
6. 外层 select 对 `var_pop_by_d` 使用 `var_pop(daily_alias)`，不要复用 `sum/days`。
7. group by 必须包含 `BIConsts.MIN_DATE_GRANULARITY_CODE`，保证 daily_value 是日级别。
8. 行列总计、整表总计、grouping sets 的处理需要使用通用 ByDay 判断。

原因：`var_pop_by_d` 的正确性依赖每日值集合，必须进入预聚合路径；同时不能破坏日均历史行为。

### 4.5 分析计算和总计逻辑

涉及：

- `AnalysisSingleModelSqlBuilder.java`
- `AnalysisSingleModelSqlBuilderAvgWrapper.java`
- `AnalysisSingleModelAvgTotalSqlBuilder.java`
- `AnalysisMultiModelSqlBuilder.java`
- `AnalysisCrossDimensionSqlBuilder.java`
- `TargetAnalysisMultiModelSqlBuilder.java`

改造点：

1. `AnalysisSingleModelAvgTotalSqlBuilder` 中 `alias.endsWith(avg_by_d)` 判断改为解析 ByDay 聚合类型。
2. 当前 `buildSelectFragmentsOnDayNotAggQuery()` 是“日粒度不汇总但总计需要计算日均”的特殊逻辑。对 `var_pop_by_d` 不应简单除以天数，而应基于明细日值集合重新聚合。因此：
   - `avg_by_d`：继续使用天数除法逻辑。
   - `avg_by_d_r`：可使用非空日天数或 `avg(daily_value)`。
   - `var_pop_by_d`：必须走日预聚合 + 总计二次聚合，不能在总计层套 `/% daysExpr`。
3. 同环比、自定义对比、业务日历的日期范围修正逻辑应沉淀到日期参与策略中，供 `avg_by_d` 使用；`var_pop_by_d` 默认只使用筛选范围内实际存在的日值。
4. `TargetAnalysisMultiModelSqlBuilder.isAvgByDay()` 改为 `isByDayReAggregationField()`，但目标值展示格式仍区分日均和方差。

原因：分析场景有大量总计和日期对齐逻辑，直接把日方差当日均处理会产生明显错误结果。

### 4.6 LOD 逻辑

涉及：

- `LodSingleModelSqlBuilderAvgWrapper.java`
- `AnalysisSqlBuilderLodWrapper.java`
- `LodQuerySqlBuilder.java`
- `LodCalcManager.java`
- `QueryConfigureNormalizer.java`

改造点：

1. `item.getAggExpressionType().isAvgByDay()` 改为 `item.getAggExpressionType().isByDayReAggregation()`。
2. LOD 指标引用 id 自动追加后缀时，使用当前聚合类型 code，而不是固定 `avg_by_d`。
3. LOD 的日预聚合维度必须包含 LOD 声明维度 + 最细日期，否则 `var_pop_by_d` 会在错误维度上计算。
4. LOD 四则计算中解析字段 id/code 后缀时，统一调用 suffix 工具。

原因：LOD 本身就是多层聚合，和“按日预聚合 + 二次聚合”叠加后最容易出现聚合层级错误。

### 4.7 权限、字段存在性和黑白名单

涉及：

- `AclManager.java`
- `QueryFieldService.java`
- `TargetAnalysisMultiModelSqlBuilder.java`
- `SSDUtil.java`

改造点：

1. `isAvgByDay(QueryField)` 泛化为 `isByDayReAggregationField(QueryField)`。
2. 权限判断时，对派生字段统一剥离 ByDay 后缀后回到原始字段鉴权。
3. 黑白名单扩展当前只加 `avg_by_d`、`avg_by_d_r`，需要决定是否自动加 `var_pop_by_d`：
   - 推荐：对已有原字段权限自动继承到所有 ByDay 派生字段。
   - 如果权限表物理存储必须展开，则通过 `AggExpressionType.getPermissionInheritedAggTypes()` 生成。

原因：派生聚合字段本质上还是原指标的展示口径，不应绕过权限，也不应因为新增后缀导致误判无权限。

### 4.8 展示标题和格式化

涉及：

- `FieldUtil.java`
- `QueryEngine.java`
- `ResultDataSetColumn` 相关构建逻辑

改造点：

1. `FieldUtil.getTitle()` 中日均标题逻辑保留给 `isAvgByDay()`，新增 `var_pop_by_d` 标题展示为“日方差”。
2. `FieldUtil.getFormatString()` 中 `AVG_FORMAT_SUFFIX` 只适用于日均，不适用于日方差。
3. `QueryEngine.unifyColumnFormat()` 当前 `avgFlags` 只有日均。建议改为：
   - 日均仍走日均格式统一。
   - 日方差走基础数值格式或新增 `byDayReAggFormatFlags`。
4. 导出列标题、原始列 code、分析真实值/差值格式要区分日均和日方差。

原因：日方差是方差值，不是均值。复用日均显示后缀会造成含义错误。

### 4.9 LLM 和 API 配置

涉及：

- `LLMAgentApiService.java`
- `LLMConfigService.java`
- `OlapApiQueryConfigureNormalizer.java` 如有聚合枚举校验也需检查

改造点：

1. 字段后缀过滤从日均硬编码改为 ByDay 通用。
2. LLM 配置生成时，如果需要暴露 `var_pop_by_d`，应以派生指标方式输出。
3. 对表达式替换、字段 id/code 生成统一走 `AggExpressionType` suffix 工具。

原因：LLM/API 场景通常不经过完整前端 normalizer，必须保证配置生成和解析一致。

## 5. 建议分阶段实施

### 阶段一：能力收口和兼容改造

目标：不改变现有 SQL 结果，先收口识别能力。

修改内容：

1. `AggExpressionType` 新增 `Var_Pop_By_Day` 和 ByDay 能力方法。
2. 新增 suffix 工具方法。
3. 把散落的 `Avg_By_Day` / `Avg_By_Day_Real` 后缀判断逐步替换为工具方法。
4. `isAvgByDay()` 保留，确保旧逻辑不受影响。
5. `MeasureFieldAutoExtender` 支持从配置化列表生成扩展字段，但 `var_pop_by_d` 可先通过开关控制是否启用。

收益：降低后续新增 SQL 能力时的遗漏风险。

### 阶段二：实现 `var_pop_by_d` 二阶段 SQL

目标：日方差结果正确。

修改内容：

1. 新增 `DayReAggregationSpec` / `DayReAggregationSpecFactory`。
2. 新增或重构 `ByDayReAggWrapper`，支持所有需要日预聚合的字段。
3. `var_pop_by_d` 强制进入日预聚合路径。
4. 日预聚合层使用原始聚合方式生成 daily_value。
5. 二次聚合层使用 Doris `var_pop(daily_value)`。
6. 分析、总计、LOD 场景接入通用 ByDay wrapper。

收益：满足新增需求，同时建立可扩展骨架。

### 阶段三：逐步迁移日均到统一框架

目标：减少历史特殊逻辑，统一维护。

修改内容：

1. `avg_by_d_r` 优先迁移到二阶段 `avg(daily_value)`。
2. `avg_by_d` 由于涉及自然日补足和业务日历，建议保留天数策略，但接入统一 ByDay spec。
3. 清理 `*AvgWrapper` 命名，保留兼容类，内部委托新类。
4. 增加 SQL 快照测试，确认历史日均 SQL 和结果不变。

收益：长期降低维护成本。

## 6. 涉及修改清单与原因

| 文件 | 修改原因 |
| --- | --- |
| `AggExpressionType.java` | 新增 `var_pop_by_d`，沉淀 ByDay 能力、二次聚合函数、后缀工具，替代硬编码判断。 |
| `AggregatorFactory.java` | 从 switch 日均枚举改为按 ByDay 能力创建聚合器，支持 `var_pop_by_d`。 |
| `DefaultAggregator.java` | 保持普通聚合；必要时抽取原始聚合表达式能力供日预聚合层复用。 |
| `AverageByDayAggregator.java` | 保留历史日均自然日口径，抽取天数计算和白名单逻辑到可复用策略。 |
| `AverageByRealDayAggregator.java` | 保留非空日均口径，迁移到 ByDay 策略。 |
| `AnalysisAverageByDayAggregator.java` | 保留同环比、自定义对比、业务日历日期修正，供分析场景日均复用。 |
| `AnalysisAverageByRealDayAggregator.java` | 接入通用非空日策略。 |
| `DefaultDataSourceSqlBuilderAvgWrapper.java` | 改造为数据源日预聚合包装器，支持非 distinct 的 `var_pop_by_d`。 |
| `SingleModelBuilderAvgWrapper.java` | 改造字段分组逻辑，`distinctFields` 泛化为 ByDay 字段集合。 |
| `AnalysisSingleModelSqlBuilderAvgWrapper.java` | 分析场景接入 ByDay 预聚合，处理总计和 count distinct 兼容。 |
| `AnalysisSingleModelAvgTotalSqlBuilder.java` | 总计逻辑从日均除天数改为按聚合类型选择二次聚合方式；`var_pop_by_d` 不可除天数。 |
| `LodSingleModelSqlBuilderAvgWrapper.java` | LOD 日均判断改为 ByDay 判断，确保 `var_pop_by_d` 使用正确聚合层级。 |
| `AnalysisSqlBuilderLodWrapper.java` | 创建 LOD wrapper 时使用 ByDay 能力判断。 |
| `QueryConfigureNormalizer.java` | LOD 引用指标自动补后缀时支持 `var_pop_by_d`。 |
| `UIQueryFieldNormalizer.java` | 前端字段 id/code 后缀解析支持所有 ByDay 聚合。 |
| `UICalcFieldNormalizer.java` | 计算字段引用的隐藏指标创建和聚合打标支持 `var_pop_by_d`。 |
| `UIAggregatorNormalizer.java` | 按日去重后 sum 的历史逻辑保持，但后缀处理改为通用工具，避免误伤新增类型。 |
| `MeasureFieldAutoExtender.java` | 自动扩展 `var_pop_by_d` 派生指标，格式化策略区分日均和方差。 |
| `AclManager.java` | 派生字段鉴权统一剥离 ByDay 后缀，避免新增字段无权限或绕权限。 |
| `TargetAnalysisMultiModelSqlBuilder.java` | 目标值/分析字段识别从日均泛化为 ByDay，同时展示格式区分日方差。 |
| `FieldUtil.java` | 标题、格式化、字段展示逻辑支持日方差，不复用日均格式后缀。 |
| `SSDUtil.java` | 字段 code 后缀剥离支持所有 ByDay 类型。 |
| `QueryEngine.java` | 结果列格式统一逻辑支持日方差，保留日均历史格式。 |
| `LLMAgentApiService.java` | LLM 字段过滤和派生指标识别支持 `var_pop_by_d`。 |
| `LLMConfigService.java` | LLM 配置生成、表达式替换、后缀处理支持 `var_pop_by_d`。 |
| `QueryFieldService.java` | 字段权限黑白名单扩展支持 ByDay 派生字段。 |
| `TargetValueController.java` | 目标值字段 code 剥离支持 `var_pop_by_d`。 |

## 7. SQL 正确性约束

### 7.1 `var_pop_by_d` 必须满足

1. 第一阶段 group by 必须包含最细日期字段 `BIConsts.MIN_DATE_GRANULARITY_CODE`。
2. 第一阶段 daily_value 必须使用指标元数据中的原始聚合方式。
3. 第二阶段必须使用 `var_pop(daily_value)`。
4. 无数据日期默认不参与方差计算。
5. count distinct 指标必须先按日 distinct，再对每日 distinct 结果做 `var_pop`。
6. 计算字段如果引用多个原子指标，需要先得到每日原子聚合值，再计算每日表达式，最后对每日表达式做 `var_pop`。不能先对全周期计算字段求值再做方差。

### 7.2 与现有日均的兼容边界

1. `avg_by_d` 继续使用自然日天数口径。
2. `avg_by_d_r` 继续使用非空日口径。
3. 当前日均对业务日历、月/季/年未完结日期剔除、分析对比日期修正的逻辑不能被 `var_pop_by_d` 误用。
4. `var_pop_by_d` 不追加 `AVG_FORMAT_SUFFIX`。
5. 老模板中已有 `avg_by_d`、`avg_by_d_r` 的 id/code 不做迁移。

## 8. 测试计划

### 8.1 单元测试

1. `AggExpressionType`：
   - `get("var_pop_by_d")` 返回 `Var_Pop_By_Day`。
   - `isByDayReAggregation()` 对三个 ByDay 类型返回 true。
   - `isAvgByDay()` 只对两个日均类型返回 true。
   - suffix append/strip/resolve 覆盖 `avg_by_d`、`avg_by_d_r`、`var_pop_by_d`。

2. `DayReAggregationSpecFactory`：
   - `var_pop_by_d` 的 second function 为 `var_pop`。
   - `var_pop_by_d` 的 day mode 为 `NON_EMPTY_DAY`。
   - `avg_by_d` 保持 calendar day 策略。

3. 聚合器：
   - 普通 sum/count 不受影响。
   - `var_pop_by_d` 生成二次聚合表达式 `var_pop(daily_alias)`。
   - count distinct 原始聚合在第一阶段执行。

### 8.2 SQL 快照测试

覆盖场景：

1. 单模型，维度 + `sum` 指标 + `var_pop_by_d`。
2. 单模型，维度 + `count(distinct)` 指标 + `var_pop_by_d`。
3. 无维度整体查询 + `var_pop_by_d`。
4. 日期粒度为 day/week/month/quarter/year。
5. 日期在行维度、列维度、无日期维度。
6. 行总计、列总计、整表总计。
7. LOD 指标 + `var_pop_by_d`。
8. 计算字段引用 `var_pop_by_d` 派生指标。
9. 同环比、占比、自定义对比中的 `var_pop_by_d`。
10. 老 `avg_by_d` / `avg_by_d_r` SQL 快照不变或结果不变。

### 8.3 结果正确性测试

构造小数据集：

| dt | dim | amount |
| --- | --- | --- |
| 2026-05-01 | A | 10 |
| 2026-05-01 | A | 20 |
| 2026-05-02 | A | 40 |
| 2026-05-04 | A | 70 |

原始聚合 `sum(amount)`，每日值为 `[30, 40, 70]`，`var_pop_by_d` 应等于 `var_pop(30, 40, 70)`。2026-05-03 无数据，默认不参与。

再构造 count distinct：

| dt | dim | user_id |
| --- | --- | --- |
| 2026-05-01 | A | u1 |
| 2026-05-01 | A | u1 |
| 2026-05-01 | A | u2 |
| 2026-05-02 | A | u1 |
| 2026-05-04 | A | u3 |

每日 distinct 值为 `[2, 1, 1]`，`var_pop_by_d` 应等于 `var_pop(2, 1, 1)`。

### 8.4 回归测试

1. 老日均模板无需迁移，查询成功。
2. 老日均在普通查询、分析查询、LOD 查询、导出中的结果与当前一致。
3. 权限黑白名单对派生字段仍生效。
4. 前端传入 `id_xxx_var_pop_by_d` 能被 normalizer 找回原始元字段。
5. LLM/API 生成配置不会丢失或误改 `var_pop_by_d` 后缀。

## 9. 风险与处理建议

| 风险 | 说明 | 建议 |
| --- | --- | --- |
| 日方差误用日均除天数逻辑 | 方差无法由总值/天数推导 | `var_pop_by_d` 强制走日预聚合二阶段 SQL。 |
| `avg_by_d` 历史口径变化 | 老报表对日均结果敏感 | 第一阶段不强制迁移 `avg_by_d` SQL，只做能力收口。 |
| 计算字段方差层级错误 | 先全周期计算再方差会错误 | 每日先计算原子聚合和表达式，再二次 `var_pop`。 |
| 总计场景结果错误 | grouping sets 和日预聚合层级叠加复杂 | 对总计建立独立 SQL 快照和结果测试。 |
| LOD 场景重复聚合 | LOD 已有聚合层级 | 明确 LOD 维度 + day 为第一阶段 group by。 |
| 权限遗漏 | 新派生字段后缀导致找不到原字段 | 所有鉴权统一 strip ByDay suffix。 |
| 格式误导 | 方差使用日均格式后缀 | 日均格式和方差格式分离。 |

## 10. 推荐落地顺序

1. 先改 `AggExpressionType`，新增 `var_pop_by_d` 和 ByDay 能力方法。
2. 新增 suffix 工具，并替换 UI normalizer、权限、LLM、目标值中的后缀硬编码。
3. 改 `MeasureFieldAutoExtender`，让元数据能生成 `var_pop_by_d` 派生字段。
4. 新增 ByDay 二阶段 SQL wrapper，先只让 `var_pop_by_d` 使用。
5. 接入分析、总计、LOD 场景。
6. 补 SQL 快照和小数据集结果测试。
7. 验证老 `avg_by_d` / `avg_by_d_r` 不变。
8. 稳定后再逐步重命名 `*AvgWrapper` 为 `*ByDayReAggWrapper`，旧类保留兼容。

## 11. 最终建议

本次不要把“日方差”实现成“第三个日均分支”。正确方向是把系统中已经存在但没有显式建模的能力抽象出来：

```text
按日预聚合 -> 基于每日值二次聚合
```

`avg_by_d`、`avg_by_d_r` 是这个能力的历史特例，`var_pop_by_d` 是新的二次聚合类型。通过 `AggExpressionType + DayReAggregationSpec + ByDayReAggWrapper` 三层收口，可以在兼容老日均的同时，避免后续每新增一个日级二次聚合都要全链路复制硬编码判断。
