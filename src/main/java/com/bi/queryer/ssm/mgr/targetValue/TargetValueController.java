package com.bi.queryer.ssm.mgr.targetValue;

import com.bi.queryer.ssm.enums.AggExpressionType;
import com.bi.queryer.ssm.meta.targetValue.TargetTableField;
import com.bi.queryer.ssm.meta.targetValue.TargetValueAdaptiveInfo;
import com.bi.queryer.ssm.meta.targetValue.TargetValueCacheManager;
import com.bi.queryer.ssm.mgr.targetValue.rsp.TargetValueMetaResp;
import com.bi.queryer.sys.common.ResponseMessage;
import com.bi.queryer.util.BIUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @Author contributor
 * @Date 17:31 2024/12/3
 * @Description TODO
 **/

@RestController
@Scope("prototype")
@RequestMapping("target/value")
public class TargetValueController {
    private static final String ALL = "整体";

    @Autowired
    private TargetValueCacheManager targetValueCacheManager;

    /**
     * 获取指标目标值支持日期粒度
     *
     * @param request
     * @return
     */
    @RequestMapping("measure/adaptive/list")
    public ResponseMessage getMeasureAdaptiveList(@RequestBody final TargetValueRequest request) {
        ResponseMessage result = new ResponseMessage();
        if (BIUtil.isEmpty(request.getMeasureCodes())) {
            result.setData(new ArrayList<>());
            return result;
        }

        List<String> measureCodes = request.getMeasureCodes();
        Map<String, List<TargetValueAdaptiveInfo>> measureAdaptiveMap = TargetValueCacheManager.getMeasureAdaptiveMap();
        List<TargetValueMetaResp> res = new ArrayList<>();
        for (String measureCode : measureCodes) {
            TargetValueMetaResp targetValueMetaResp = new TargetValueMetaResp();
            targetValueMetaResp.setMeasureCode(measureCode);
            String replacedCode = measureCode.replaceAll("_" + AggExpressionType.Avg_By_Day_Real.getCode() + "|" + "_" + AggExpressionType.Avg_By_Day.getCode(), "");
            List<TargetValueAdaptiveInfo> adaptiveInfoList = measureAdaptiveMap.get(replacedCode);
            if (BIUtil.isEmpty(adaptiveInfoList)) {
                continue;
            }

            Function<TargetValueAdaptiveInfo, Boolean> filter = v -> {
                if (StringUtils.isEmpty(request.getDateGranularity())) {
                    return Objects.equals(v.getMeasureField().getDateType(), request.getDateType());
                } else {
                    return Objects.equals(v.getMeasureField().getDateType(), request.getDateType())
                            && v.getDateGranularityList().contains(request.getDateGranularity());
                }
            };

            List<String> supportDims = adaptiveInfoList.stream()
                    .filter(filter::apply)
                    .map(TargetValueAdaptiveInfo::getDimensionList)
                    .map(v -> {
                        if (BIUtil.isEmpty(v)) {
                            return ALL;
                        } else {
                            return v.stream().map(TargetTableField::getWhitePaperName)
                                    .collect(Collectors.joining("*"));
                        }
                    }).distinct().collect(Collectors.toList());

            if (BIUtil.isEmpty(supportDims)) {
                // 一个维度也不支持的，去掉
                continue;
            }
            if (supportDims.remove(ALL)) {
                supportDims.add(0, ALL);
            }

            targetValueMetaResp.setDateType(request.getDateType());
            targetValueMetaResp.setDateGranularity(request.getDateGranularity());
            targetValueMetaResp.setSupportDims(supportDims);

            List<String> targetTypes = adaptiveInfoList.stream().map(TargetValueAdaptiveInfo::getMeasureField)
                    .map(TargetTableField::getTargetTypeList)
                    .flatMap(Collection::stream).distinct()
                    .collect(Collectors.toList());

            targetValueMetaResp.setTargetTypes(targetTypes);
            res.add(targetValueMetaResp);
        }

        result.setData(res);
        return result;
    }

    @RequestMapping("measure/meta/refresh")
    public ResponseMessage refreshTargetMetaCache(@RequestBody TargetValueRequest targetValueRequest) {
        return new ResponseMessage(targetValueCacheManager.refreshTargetMetaCache());
    }
}
