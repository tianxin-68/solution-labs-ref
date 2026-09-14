package com.bi.queryer.ssm.enums;

import com.bi.queryer.sys.base.BaseController;
import com.bi.queryer.sys.common.ResponseMessage;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Scope;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 表管理
 * @author contributor
 */
@RestController
@Scope("prototype")
@RequestMapping("enums")
public class EnumsController extends BaseController {

    /**
     * 查询所有枚举类
     * @param EnumType
     * @return
     */
    @RequestMapping("getAllEnums")
    public ResponseMessage getAllEnums(@RequestParam(value = "EnumType", required = false) String EnumType){
        ResponseMessage result = new ResponseMessage();
        Map<String, List<Map<String, String>>> EnumsMap = new HashMap<>();

        //AggExpressionType
        List<Map<String, String>> aggExpressionType = AggExpressionType.toListMap();
        EnumsMap.put("AggExpressionType", aggExpressionType);

        //AutoExtendType
        List<Map<String, String>> autoExtendType = AutoExtendType.toListMap();
        EnumsMap.put("AutoExtendType", autoExtendType);

        //CategoryType
        List<Map<String, String>> categoryType = CategoryType.toListMap();
        EnumsMap.put("CategoryType", categoryType);

        //ExportAppendStringType
        List<Map<String, String>> exportAppendStringType = ExportAppendStringType.toListMap();
        EnumsMap.put("ExportAppendStringType", exportAppendStringType);

        //FieldDataType
        List<Map<String, String>> fieldDataType = FieldDataType.toListMap();
        EnumsMap.put("FieldDataType", fieldDataType);

        //FieldFilterMode
        List<Map<String, String>> fieldFilterMode = FieldFilterMode.toListMap();
        EnumsMap.put("FieldFilterMode", fieldFilterMode);

        //FieldFilterType
        List<Map<String, String>> fieldFilterType = FieldFilterType.toListMap();
        EnumsMap.put("FieldFilterType", fieldFilterType);

        //FieldJoinType
        List<Map<String, String>> fieldJoinType = FieldJoinType.toListMap();
        EnumsMap.put("FieldJoinType", fieldJoinType);

        //FieldSortType
        List<Map<String, String>> fieldSortType = FieldSortType.toListMap();
        EnumsMap.put("FieldSortType", fieldSortType);

        //FieldUseType
        List<Map<String, String>> fieldUseType = FieldUseType.toListMap();
        EnumsMap.put("FieldUseType", fieldUseType);

        //QueryFieldType
        List<Map<String, String>> queryFieldType = QueryFieldType.toListMap();
        EnumsMap.put("QueryFieldType", queryFieldType);

        //SensitiveDataType
        List<Map<String, String>> sensitiveDataType = SensitiveDataType.toListMap();
        EnumsMap.put("SensitiveDataType", sensitiveDataType);

        //ShowFormatExpressionType
        List<Map<String, String>> showFormatExpressionType = ShowFormatExpressionType.toListMap();
        EnumsMap.put("ShowFormatExpressionType", showFormatExpressionType);

        //ShowFormatExpressionType
        List<Map<String, String>> dateGranularity = DateGranularity.toListMap();
        EnumsMap.put("DateGranularity", dateGranularity);

        if (StringUtils.isNotBlank(EnumType) && EnumsMap.containsKey(EnumType)){
            Map<String, List<Map<String, String>>> EnumsFilterMap = new HashMap<>();
            EnumsFilterMap.put(EnumType, EnumsMap.get(EnumType));
            result.setData(EnumsFilterMap);
            return result;
        }

        result.setData(EnumsMap);
        return result;
    }



}
