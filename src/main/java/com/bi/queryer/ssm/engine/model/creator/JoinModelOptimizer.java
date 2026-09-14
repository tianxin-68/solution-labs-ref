package com.bi.queryer.ssm.engine.model.creator;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.bi.queryer.ssm.custom.CustomFieldExpressionIdMapping;
import com.bi.queryer.ssm.engine.config.QueryConfigure;
import com.bi.queryer.ssm.engine.config.field.QueryField;
import com.bi.queryer.ssm.engine.model.QueryTable;
import com.bi.queryer.ssm.engine.model.StarModel;
import com.bi.queryer.ssm.meta.MetaField;
import com.bi.queryer.ssm.meta.SSDMetaCacheManager;
import com.bi.queryer.util.BIUtil;
import org.apache.commons.collections4.ListUtils;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * @Author contributor
 * @Date 15:17 2022-10-26
 * @Description full join模型优化器：保证所有模型维度一致
 **/
public class JoinModelOptimizer extends BaseModelOptimizer {

    public JoinModelOptimizer(QueryConfigure config, List<StarModel> models){
        super(config, models);
    }

    public void optimize(){
        // 先标准化
        models.forEach(m->this.normalize(m));

        // 若没有指标，且有一个模型的字段覆盖另外一个，则删除被覆盖的模型
        this.optimizeOnlyDimModel();

        // 多个模型，删除无指标的模型
        this.optimizeNoneMeasureModel();

        // 删除冗余模型（按模型是否覆盖删除）
        this.optimizeRedundancyModel();

        /**
         * 补齐度量字段:将所有模型表的度量字段都保持一致（个数+编码），方便子查询和主查询的select子句使用。
         */
        this.supplementFullMeasure();

        //修正模型中的计算维度字段
        this.optimizeCustomDimFields();

    }

    /**
     * 若没有指标，且有一个模型的字段覆盖另外一个，则删除被覆盖的模型
     * 示例：T1(dim1/dim2),T2(dim1)，需删除T2
     * @return
     */
    protected void optimizeOnlyDimModel(){
        if(BIUtil.isEmpty(models)) {
            return ;
        }
        List<StarModel> discardModels = new ArrayList<>();
        for(int i = 0; i < models.size(); i++){
            StarModel model1 = models.get(i);
            if (BIUtil.isNotEmpty(model1.getMeasureFields())) {
                // 有指标，则不优化
                return ;
            }
            for(int j = i+1; j < models.size(); j++) {
                StarModel model2 = models.get(j);
                Set<String> dimFieldCodes1 = model1.getDimFields().stream().map(f -> f.getCode()).collect(Collectors.toSet());
                Set<String> dimFieldCodes2 = model2.getDimFields().stream().map(f -> f.getCode()).collect(Collectors.toSet());
                if(dimFieldCodes1.containsAll(dimFieldCodes2)) {
                    discardModels.add(models.get(j));
                }else if(dimFieldCodes2.containsAll(dimFieldCodes1)) {
                    discardModels.add(models.get(i));
                }
            }
        }
        models.removeAll(discardModels);
    }

    /**
     * 多个模型，删除无指标的模型，查询配置中有指标，但模型表无指标
     * 示例:T1(dim1/dim2/measure1)、T2(dim1/dim2)，需删除T2
     */
    protected void optimizeNoneMeasureModel(){
        if(models.size() < 2) {
            return;
        }
        List<QueryField> queryMeasures = config.getAllFields().stream().filter(f->f.isMeasure()).collect(Collectors.toList());
        if(BIUtil.isEmpty(queryMeasures)){
            return;
        }

        List<StarModel> discardModels = new ArrayList<>();
        // 多个模型时去掉无指标的表
        for(int i = 0; i < models.size(); i++) {
            List<QueryField> queryMeasureFields = models.get(i).getMeasureFields();
            if (queryMeasureFields.size() == 0) {
                discardModels.add(models.get(i));
            }
        }

        // 若有lod字段的配置，需要保留一个主模型
        // 原因：主视图中只有维度无指标时，lod字段也可以查询
        if(config.isLodConfig()){
            if(discardModels.size() > 1) {
                List<String> queryDimFieldCodes = config.getResult().getRowDimensions().stream().map(QueryField::getCode).collect(Collectors.toList());

                // 优先保留维度全覆盖的模型
                StarModel reserveModel = null;
                for(StarModel discardModel : discardModels){
                    List<String> dimFieldCodes = discardModel.getDimFields().stream().filter(f->!f.isAppend()).map(f -> f.getCode()).collect(Collectors.toList());
                    List<String> dimIntersection = ListUtils.intersection(queryDimFieldCodes, dimFieldCodes);
                    if(dimIntersection.size() == queryDimFieldCodes.size()){
                        // 维度全部覆盖，则保留
                        reserveModel = discardModel;
                    }
                }
                if(reserveModel == null){
                    reserveModel = discardModels.get(0);
                }
                discardModels.remove(reserveModel);
            }else{
                return;
            }
        }

        models.removeAll(discardModels);
    }

    /**
     * 优化原因：前期做规范化时，保障了所有模型的维度一致性
     * 1、多个模型，存在模型维度不全且有其他模型相同指标时，则需要删除此模型
     * 示例:T1(dim1/dim2/measure1/measure2)、T2(dim1/measure1)，需删除T2
     *
     * 2、多个模型且有查询维度，但存在无维度有指标的模型，则需删除此模型
     * 示例:T1(dim1/measure1/measure2)、T2(measure2)，需删除T2
     *
     * 3、多个模型维度一致，但指标有包含关系，则需删除指标被包含的模型
     * 示例：T1(dim1/measure1/measures2), T2(dim1/measure1)，需删除T2
     */
    protected void optimizeRedundancyModel(){
        if(models.size() < 2) {
            return;
        }
        /*
        Set<String> calcDimDependCodes = Stream.of(config.getResult().getColDimensions(), config.getResult().getRowDimensions())
                .flatMap(Collection::stream).filter(Objects::nonNull)
                .map(QueryField::getCalcAtomFields)
                .flatMap(Collection::stream)
                .filter(QueryField::isDimension)
                .map(QueryField::getRawCode).collect(Collectors.toSet());
        */

        List<StarModel> discardModels = new ArrayList<>();
        for(int i = 0; i < models.size(); i++){
            StarModel model1 = models.get(i);
            for(int j = i+1; j < models.size(); j++) {
                StarModel model2 = models.get(j);

                // 排除掉附加字段， 分两种情况：
                // 1、计算维度依赖的原子维度，不能删除；解决把原子维度隐藏，查计算维度报错的问题；
                // 2、计算指标依赖的原子维度，需要删除，解决和原子指标选择模型不一样的问题；
                Predicate<QueryField> predicate = f -> !f.isAppend();// || calcDimDependCodes.contains(f.getRawCode());
                List<String> dimFieldCodes1 = model1.getDimFields().stream().filter(predicate).map(f -> f.getCode()).distinct().collect(Collectors.toList());
                List<String> dimFieldCodes2 = model2.getDimFields().stream().filter(predicate).map(f -> f.getCode()).distinct().collect(Collectors.toList());
                List<String> dimIntersection = ListUtils.intersection(dimFieldCodes1, dimFieldCodes2);
                if(dimIntersection.size() == dimFieldCodes1.size() && dimFieldCodes1.size() == dimFieldCodes2.size()){
                    // 维度全部覆盖，则不处理
                    // 此处去掉：多个模型有相同维度和相同指标时，不只需要从一个模型查询即可   by contributor 2024-09-25
                    // continue;
                }

                // 排除掉附加字段
                List<String> measureFieldCodes1 = model1.getMeasureFields().stream().filter(f->!f.isAppend()).map(f -> f.getCode()).distinct().collect(Collectors.toList());
                List<String> measureFieldCodes2 = model2.getMeasureFields().stream().filter(f->!f.isAppend()).map(f -> f.getCode()).distinct().collect(Collectors.toList());
                List<String> measureIntersection = ListUtils.intersection(measureFieldCodes1, measureFieldCodes2);

                // 若有指标且指标编码一致，则不处理
                if(measureIntersection.size() == 0 && BIUtil.isNotEmpty(measureFieldCodes1) && BIUtil.isNotEmpty(measureFieldCodes2)){
                    // 无指标交接，则不处理
                    continue;
                }

                // 是否是相同维度
                boolean isSameDims = (dimIntersection.size() == dimFieldCodes1.size() && dimIntersection.size() == dimFieldCodes2.size())
                                        || (dimFieldCodes1.isEmpty() && dimFieldCodes2.isEmpty());

                // 是否是相同指标
                boolean isSameMeasure = (measureIntersection.size() == measureFieldCodes1.size() && measureIntersection.size() == measureFieldCodes2.size())
                                        || (measureFieldCodes1.isEmpty() && measureFieldCodes2.isEmpty());

                if(isSameDims) {
                    if(isSameMeasure) {
                        /** 维度一致且指标一致 : 去掉任意一个模型 **/
                        /*
                        List<String> measureAppendFieldCodes1 = model1.getMeasureFields().stream().filter(f->f.isAppend()).map(f -> f.getCode()).collect(Collectors.toList());
                        List<String> measureAppendFieldCodes2 = model2.getMeasureFields().stream().filter(f->f.isAppend()).map(f -> f.getCode()).collect(Collectors.toList());
                        // 特殊处理：2个模型的字段都是附加字段，则保留
                        if(measureFieldCodes1.isEmpty() && measureFieldCodes2.isEmpty() && !measureAppendFieldCodes1.isEmpty() && !measureAppendFieldCodes2.isEmpty()){
                            // do nothing
                        }
                         */
                        if (measureFieldCodes1.containsAll(measureFieldCodes2)) { // 判断： 1覆盖2，则删除2
                            //去掉字段少的模型
                            if(models.get(i).getFields().size() < models.get(j).getFields().size()){
                                discardModels.add(models.get(i));
                            }else{
                                discardModels.add(models.get(j));
                            }
                        }
                    }else {
                        /** 维度一致但指标不一致 : 判断指标是否可覆盖 **/
                        // 判断： 1覆盖2，则删除2
                        if (measureFieldCodes1.containsAll(measureFieldCodes2)) {
                            discardModels.add(models.get(j));
                        }
                        // 判断：2覆盖1，则删除1
                        if (measureFieldCodes2.containsAll(measureFieldCodes1)) {
                            discardModels.add(models.get(i));
                        }
                    }
                }else {
                    /*** 维度不一致 : 判断维度和指标是否均可覆盖 ****/
                    // 判断： 1覆盖2，则删除2
                    if((dimIntersection.size() == dimFieldCodes2.size() || dimFieldCodes2.isEmpty())
                            && measureFieldCodes1.containsAll(measureFieldCodes2)){
                        discardModels.add(models.get(j));
                    }
                    // 判断：2覆盖1，则删除1
                    if((dimIntersection.size() == dimFieldCodes1.size() || dimFieldCodes1.isEmpty())
                            && measureFieldCodes2.containsAll(measureFieldCodes1)){
                        discardModels.add(models.get(i));
                    }
                }
            }
        }
        models.removeAll(discardModels);
    }

    /**
     * 补齐度量字段:将所有模型表的度量字段都保持一致（个数+编码），方便子查询和主查询的select子句使用。
     * 说明：补充的度量字段为空字段
     * 示例：T1(dim1/measure1/measure2)、T1(dim1/measure1/measure3) => T1(dim1/measure1/measure2/measure3)、T1(dim1/measure1/measure2/measure3)
     */
    protected void supplementFullMeasure(){
        if(!config.getSettings().isEnableSupplementFullMeasureOnCreateStarModel()){
            return;
        }
        if(models.size() < 2) {
            return;
        }
        Map<String, QueryField> allMeasureFields = config.getAllFields().stream().filter(f->f.isMeasure() && !f.isAppend()).collect(Collectors.toMap(QueryField::getCode, QueryField->QueryField, (entity1, entity2) -> entity1));
       // List<QueryTable> queryTables = models.stream().map(StarModel::getFactTable).collect(Collectors.toList());
        for(StarModel model : models){
            Map<String, QueryField> tableMeasures = model.getMeasureFields().stream().collect(Collectors.toMap(QueryField::getCode, QueryField->QueryField));

            List<QueryField> supplementFields = new ArrayList<>();
            for(QueryField m : allMeasureFields.values()){
                QueryField tableField = tableMeasures.get(m.getCode());
                if(tableField == null) {
                    QueryField clone = m.clone();
                    clone.setVirtual(true);
                    clone.setIsResult(true);
                    supplementFields.add(clone);
                }
                if(tableField != null && tableField.isAppend()) {
                    // 查询字段在表格中存在，但是附加字段，则此类字段也为虚拟字段
                    tableField.setVirtual(true);
                }
            }
            supplementFields.forEach(f->{model.getFactTable().addField(f);});

            //queryTable.format(); // 此处不要格式化表格，避免计算字段错乱
        }
    }

    /**
     * 修正原因：模型中计算字段引用的字段与事实表中的字段不一致，导致异常
     * 计算字段引用的字段A,编码=AAK,id = 111,表的原始字段 编码=AAK ,id =222
     * 修正：计算字段引用的字段A,编码=AAK,id = 222,表的原始字段 编码=AAK ,id =222
     */
    protected void optimizeCustomDimFields() {

        for (StarModel model : models) {
            List<QueryField> allFields = model.getFields();
            if (CollUtil.isEmpty(allFields)) {
                continue;
            }

            List<QueryField> customDimensionFieldList = allFields
                    .stream().filter(f->f.isCustomDimension())
                    .collect(Collectors.toList());

            if(CollUtil.isEmpty(customDimensionFieldList)){
                continue;
            }

            for (QueryField qf : customDimensionFieldList) {
                if (CollUtil.isEmpty(qf.getCalcAtomFields())) {
                    continue;
                }

                Set<QueryField> calcAtomFields = new HashSet<>();

                for (QueryField calcAtomField : qf.getCalcAtomFields()) {

                    String fieldCode = calcAtomField.getCode();
                    Optional<QueryField> opt = allFields
                            .stream().filter(f -> f.getCode().equals(fieldCode))
                            .findAny();

                    if (!opt.isPresent()) {
                        calcAtomFields.add(calcAtomField);
                        continue;
                    }

                    QueryField queryField = opt.get();

                    if (queryField.getId().equalsIgnoreCase(calcAtomField.getId())) {
                        calcAtomFields.add(calcAtomField);
                        continue;
                    }

                    calcAtomFields.add(queryField);

                    // 修改字段用户自定义表达式中的引用字段id
                    String expression = qf.getCustomFieldConfigure().getExpression();
                    if(StrUtil.isNotEmpty(expression)){
                        // 修改计算表达式：将其中的id替换为clone字段的id
                        expression = expression.replaceAll("\\[" + calcAtomField.getId() + "\\]", "[" + queryField.getId() + "]");
                    }

                    qf.getCustomFieldConfigure().setExpression(expression);
                }

                qf.setCalcAtomFields(calcAtomFields);

            }

        }

    }

}
