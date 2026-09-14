package com.bi.queryer.ssm.engine.model.creator;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.bi.queryer.ssm.engine.QueryContext;
import com.bi.queryer.ssm.engine.accelerate.hot.HotModelOptimizer;
import com.bi.queryer.ssm.engine.accelerate.route.TablePriSubCfgRouter;
import com.bi.queryer.ssm.engine.config.QueryConfigure;
import com.bi.queryer.ssm.engine.config.field.QueryField;
import com.bi.queryer.ssm.engine.model.QueryTable;
import com.bi.queryer.ssm.engine.model.StarModel;
import com.bi.queryer.ssm.meta.DorisTableInformation;
import com.bi.queryer.ssm.meta.MetaTable;
import com.bi.queryer.ssm.meta.SysEtlJobInfo;
import com.bi.queryer.ssm.query.template.enums.DataTypeEnum;
import com.bi.queryer.ssm.util.TableDataUpdateTimeUtil;
import com.bi.queryer.sys.base.BaseDao;
import com.bi.queryer.util.BIUtil;
import com.bi.queryer.util.SpringContextUtil;
import com.bi.queryer.util.period.DateUtil;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @Author contributor
 * @Date 18:48 2022-11-01
 * @Description full join模型创建者
 **/
public class JoinModelCreator extends BaseModelCreator {

    protected boolean isCreated = false;
    protected List<StarModel> models = new ArrayList<>();

    public JoinModelCreator(QueryConfigure config, QueryContext cxt) {
        super(config, cxt);
        models = new ArrayList<>();
    }

    @Override
    public List<StarModel> create() {
        if(BIUtil.isNotEmpty(models)){
            return models;
        }
        models = super.create();

        JoinModelOptimizer optimizer = new JoinModelOptimizer(config, models);
        optimizer.optimize();

        // 优化完后再次弱化维度表
        this.weakenDimTables(models);

        // 热化模型
        HotModelOptimizer.hot(config, models);

        //主子表替换
        TablePriSubCfgRouter.route(config,models);

        //构建模型实时表的更新时间
        buildModelDataUpdateTime();

        return models;
    }

    /**
     * 所有模型的原表均有相同维度，指标可以不同
     *
     * @return
     */
    public ModelValidateResult validate(boolean isNeedInvalidInfo) {
        ModelValidateResult result = new ModelValidateResult();
        List<StarModel> models = this.create();
        if (BIUtil.isEmpty(models)) {
            return result;
        }
        List<QueryField> queryDimFields = this.getAcceptFields();
        Set<String> queryDimFieldCodes = queryDimFields.stream().map(QueryField::getCode).collect(Collectors.toSet());

        for (StarModel model : models) {
            List<QueryTable> queryTables = model.getTables();
            Set<String> modelFieldCodes = new HashSet<>();
            for (QueryTable queryTable : queryTables) {
                modelFieldCodes.addAll(queryTable.getFields().stream().map(QueryField::getCode).collect(Collectors.toSet()));
            }

            if(!modelFieldCodes.containsAll(queryDimFieldCodes)) {
                result.setSuccess(false);
                if(isNeedInvalidInfo) {
                    InvalidModel invalidModel = new InvalidModel(model);

                    Set<String> invalidFieldCodes = new HashSet<>();
                    queryDimFieldCodes.stream().forEach(f -> {
                        if (!modelFieldCodes.contains(f)) {
                            invalidFieldCodes.add(f);
                        }
                    });
                    invalidModel.setFieldCodes(invalidFieldCodes);

                    model.getTables().stream().forEach(t ->{
                        MetaTable metaTable = t.getMeta();
                        String owner = metaTable.getTableOwner(); //BIUtil.isEmpty(metaTable.getUpdatedBy()) ? metaTable.getCreatedBy() : metaTable.getUpdatedBy();
                        result.getInvalidModelOwners().add(owner);
                    });

                    result.getInvalidModels().add(invalidModel);
                }else {
                    return result;
                }
            }
        }
        if(result.isSuccess()){
            result.setValidModels(models);
        }
        return result;
    }

    /**
     * 获取查询的维度字段
     *
     * @return
     */
    protected List<QueryField> getAcceptFields() {
        List<QueryField> acceptFields = new ArrayList<>();
        List<QueryField> dimFields = this.config.getResult().getRowDimensions();

        for (QueryField field : dimFields) {
            if (field.isAppend() || field.isCustom()) {
                // 不添加附加
                if(CollUtil.isNotEmpty(field.getCusCalcDependFields())){
                    acceptFields.addAll(field.getCusCalcDependFields());
                }else{
                    acceptFields.add(field);
                }

            } else {
                acceptFields.add(field);
            }
        }

        // 排除掉自定义计算维度
        // acceptFields = acceptFields.stream().filter(f->!f.isCustomDimension()).collect(Collectors.toList());
        // 添加计算字段的原子字段

        return acceptFields;
    }

    /**
     * 构建模型的数据更新时间
     */
    public void buildModelDataUpdateTime() {

        Map<String, String> tableDataUpdateTimeMap = config.getSettings().getTableDataUpdateTimeMap();
        //设置模型数据更新时间
        for (StarModel model : models) {
            MetaTable metaTable = model.getFactTable().getMeta();
            model.setDataUpdateTime(tableDataUpdateTimeMap.get(metaTable.getFullName()));
        }

    }

}
