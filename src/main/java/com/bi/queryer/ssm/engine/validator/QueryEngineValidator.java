package com.bi.queryer.ssm.engine.validator;

import cn.hutool.core.util.StrUtil;
import com.bi.queryer.ssm.engine.config.QueryConfigure;
import com.bi.queryer.ssm.engine.config.field.QueryField;
import com.bi.queryer.ssm.engine.function.FunctionManager;
import com.bi.queryer.ssm.engine.function.IFunction;
import com.bi.queryer.ssm.engine.model.StarModel;
import com.bi.queryer.ssm.engine.model.creator.InvalidModel;
import com.bi.queryer.ssm.engine.model.creator.ModelValidateResult;
import com.bi.queryer.ssm.engine.session.QuerySessionSettingManager;
import com.bi.queryer.ssm.meta.MetaTable;
import com.bi.queryer.sys.config.SC;
import com.bi.queryer.util.BIConsts;
import com.bi.queryer.util.BIUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @Author contributor
 * @Date 11:02 2024-04-16
 * @Description 查询校验器
 **/
public class QueryEngineValidator {

    /**
     * 获取无需模型提示信息
     * @param result
     * @return
     */
    public static String getInvalidModelMessage(QueryConfigure config, ModelValidateResult result){
        String msg = "";
        if(result.isSuccess()){
            return msg;
        }

        Map<String, QueryField> allConfigFields = config.getAllFields().stream().collect(Collectors.toMap(QueryField::getCode, f->f, (f1,f2)->f1));

        List<String> infos = new ArrayList<>();
        for(InvalidModel invalidModel : result.getInvalidModels()){
            StarModel model = invalidModel.getModel();

            List<String> tableNames = new ArrayList<>();
            model.getTables().stream().forEach(t ->{
                MetaTable metaTable = t.getMeta();
                tableNames.add(metaTable.getFullName(true));
            });

            List<String> fieldNames = new ArrayList<>();
            if(BIUtil.isNotEmpty(invalidModel.getFieldCodes())){
                invalidModel.getFieldCodes().forEach(c->{
                    QueryField configField = allConfigFields.get(c);
                    if(configField != null){
                        String fieldTitle = BIUtil.isEmpty(configField.getDisplayTitle()) ? configField.getMeta().getTitle() : configField.getDisplayTitle();
                        fieldNames.add(fieldTitle);
                    }else {
                        fieldNames.add(c);
                    }
                });
            }

            String info = String.format("请联系模型负责人[%s]，原因：字段[%s]在模型[%s]中不存在",
                    BIUtil.listToStr(result.getInvalidModelOwners()),
                    BIUtil.listToStr(fieldNames),
                    BIUtil.listToStr(tableNames)
                    );

            infos.add(info);
        }
        msg = BIUtil.listToStr(infos, ";");
        return msg;
    }

    /**
     * 构建配置错误
     * @param errorContent
     * @param models
     * @return
     */
    public static QueryMessage validateErrorMessage(String errorContent, List<StarModel> models, QueryConfigure config) {
        String defaultDetailContent = "";
        if(BIUtil.isNotEmpty(errorContent)){
            defaultDetailContent = "[详情]：" +  StrUtil.subSufByLength(errorContent, 200);
        }
        QueryMessage errorMessage = new QueryMessage(BIConsts.QUERY_ERROR_TIP + defaultDetailContent, QueryMessageType.ERROR_OTHER);
        if(BIUtil.isEmpty(errorContent)){
            return errorMessage;
        }
        IFunction fx = FunctionManager.getFunction();
        // 查询超时
        if (fx.isQueryTimeoutMessage(errorContent)){
            String timeoutError = String.format(BIConsts.SSM_ERROR_TIMEOUT, QuerySessionSettingManager.getQueryTimeoutSec() / 60);
            // 列维度
            Optional<QueryField> colDimField = config.getResult().getColDimensions().stream().findFirst();
            if(colDimField.isPresent()){
                timeoutError = String.format(BIConsts.SSM_ERROR_TIMEOUT_COLUMN_DIMENSION, QuerySessionSettingManager.getQueryTimeoutSec() / 60, colDimField.get().getTitle());
            }
            errorMessage = new QueryMessage(timeoutError, QueryMessageType.ERROR_TIMEOUT);
            return errorMessage;
        }

        // 规则熔断
        if(fx.isBlockMessage(errorContent)){
            errorMessage = new QueryMessage(createBlockMessage(errorContent), QueryMessageType.ERROR_BLOCK);
            return errorMessage;
        }

        // 限流
        if(errorContent.contains(BIConsts.SSM_ERROR_RATE_LIMIT) ){
            errorMessage = new QueryMessage(errorContent, QueryMessageType.ERROR_RATE_LIMIT);
            return errorMessage;
        }

        //olap_api查询限流
        if(errorContent.contains(BIConsts.OLAP_API_ERROR_RATE_LIMIT_PREFIX)){
            errorMessage = new QueryMessage(errorContent, QueryMessageType.ERROR_OLAP_API_RATE_LIMIT);
            return errorMessage;
        }

        // 分区限制
        if(errorContent.contains(BIConsts.SSM_ERROR_TOO_MANY_PARTITION)){
            errorMessage = new QueryMessage(errorContent, QueryMessageType.ERROR_TOO_MANY_PARTITION);
            return errorMessage;
        }

        // 查询视图限制
        if(errorContent.contains(BIConsts.SSM_ERROR_VIEW_LIMIT)){
            errorMessage = new QueryMessage(errorContent, QueryMessageType.ERROR_VIEW_LIMIT);
            return errorMessage;
        }

        // 手动kill，不处理错误：不发邮件
        // 注意：必须放到最后
        if (fx.isManualKillMessage(errorContent)) {
            errorMessage = new QueryMessage(errorContent, QueryMessageType.ERROR_MANUAL_KILL);
            return errorMessage;
        }

        // 通过关键字判断是否为配置错误
        if(BIUtil.isEmpty(models)){
            return errorMessage;
        }
        String configErrorKeywordStr = SC.v("config.error.keywords", "does not exist,cannot be resolved");
        String[] configErrorKeywords = configErrorKeywordStr.toLowerCase().split(",");
        boolean isConfigError = false;
        for(String kw : configErrorKeywords){
            if(errorContent.toLowerCase().contains(kw)){
                isConfigError = true;
                break;
            }
        }
        if(isConfigError){
            // 简化信息，只取第一个
            MetaTable metaTable = models.get(0).getTables().get(0).getMeta();
            String owner = metaTable.getTableOwner(); //BIUtil.isEmpty(metaTable.getUpdatedBy()) ? metaTable.getCreatedBy() : metaTable.getUpdatedBy();
            String configErrorInfo = String.format("请联系模型负责人[%s]，模型配置错误：%s", owner, errorContent);
            errorMessage = new QueryMessage(configErrorInfo, QueryMessageType.ERROR_CONFIG);
            return errorMessage;
        }
        return errorMessage;
    }

    protected static String createBlockMessage(String errorContent){
        if(BIUtil.isEmpty(errorContent)){
            return errorContent;
        }
        String defaultMessage = "因集群资源紧张，查询被阻断。请减少维度/指标或修改筛选条件，缩小查询范围。";
        String[] infos = errorContent.split(",");
        String blockMessage = "";
        for(String info : infos){
            if(!info.toLowerCase().trim().contains("rule_")){
                continue;
            }
            String[] items = info.trim().split(":");
            String ruleCode = items[items.length - 1].trim();
            if(BIUtil.isEmpty(ruleCode)){
                continue;
            }
            blockMessage = SC.v("doris.block.rule." + ruleCode, defaultMessage);
            break;
        }
        // 匹配其他阻断关键字
        if(BIUtil.isEmpty(blockMessage)){
            if(errorContent.toLowerCase().contains("cancel query from fe") || errorContent.toLowerCase().contains("cancel top memory")){
                blockMessage = "查询被终止：由于集群整体内存资源不足，当前查询内存占用过高。请减少维度/指标或修改筛选条件，缩小查询范围。";
            }
            if(errorContent.toLowerCase().contains("query queue timeout")){
                blockMessage = "查询被终止：当前集群查询排队队列已经满，当前查询已经排队超过30秒，已被主动取消以释放资源，请稍后再试。";
            }
        }
        if(BIUtil.isEmpty(blockMessage)){
            blockMessage = defaultMessage;
        }
        return blockMessage;
    }

}
