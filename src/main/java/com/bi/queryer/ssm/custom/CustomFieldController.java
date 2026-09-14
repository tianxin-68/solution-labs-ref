package com.bi.queryer.ssm.custom;

import com.bi.queryer.ssm.engine.QueryContext;
import com.bi.queryer.ssm.engine.config.field.QueryField;
import com.bi.queryer.ssm.exception.SSDException;
import com.bi.queryer.ssm.meta.SSDQueryTemplate;
import com.bi.queryer.sys.base.BaseController;
import com.bi.queryer.sys.common.ResponseMessage;
import com.bi.queryer.util.BIUtil;
import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@Scope("prototype")
@RequestMapping("ssd/custom/field")
public class CustomFieldController  extends BaseController {

    @Autowired
    private CustomFieldService service = null;

    /**
     * 添加行区域自定义字段
     * @return
     */
    @RequestMapping("check")
    @ResponseBody
    public ResponseMessage check(){
        ResponseMessage result = new ResponseMessage();
        JSONObject data = new JSONObject();
        try{
            String customFieldCheckStr = stringValue("customFieldCheck");
            customFieldCheckStr = BIUtil.isEmpty(customFieldCheckStr) ? JSONObject.toJSONString(this.params) : customFieldCheckStr;
            if(BIUtil.isEmpty(customFieldCheckStr)) {
                throw new SSDException("自定义字段校验内容为空");
            }
            JSONObject checkJsonObject = JSONObject.parseObject(customFieldCheckStr);//BIUtil.toJSONObject(customFieldCheckStr);
            // 返回其原生字段和字段id
            String customFieldStr = checkJsonObject.getString("customField");
            if(BIUtil.isEmpty(customFieldStr)) {
                throw new SSDException("校验内容没有customField属性");
            }
            String templateStr = checkJsonObject.getString("template");
            if(BIUtil.isEmpty(templateStr)) {
                throw new SSDException("校验内容没有template属性");
            }

            // 查询的计算字段
            QueryField queryCustomField = JSONObject.parseObject(customFieldStr, QueryField.class);
            queryCustomField.init();

            // 查询模板&配置
            SSDQueryTemplate template = JSONObject.parseObject(templateStr, SSDQueryTemplate.class);
            /*
            QueryConfigure queryConfigure = new QueryConfigure(template);
            queryConfigure.load();
            List<QueryField> atomFields = service.check(queryConfigure, queryCustomField, new QueryContext(this.getRequest()));
            if(BIUtil.isNotEmpty(atomFields)) {
                List<JSONObject> atomFieldsJson = atomFields.stream().map(f->f.toJSON()).collect(Collectors.toList());
                data.put("atomFields", atomFieldsJson);
            }
            CustomFieldType customFieldType = CustomFieldType.get(queryCustomField.getCustomFieldConfigure().getType());
            String customFieldId = BIUtil.isEmpty(queryCustomField.getId()) ?  customFieldType.getIdentifier() + Guid.id() : queryCustomField.getId();
            data.put("customFieldId", customFieldId);
             */
            String customFieldId = service.check(template, queryCustomField, new QueryContext(this.getRequest()));
            data.put("customFieldId", customFieldId);
            result.setData(data);
            return result;
        }catch (Exception e) {
            e.printStackTrace();
            throw new SSDException(e.getMessage(), SSDException.Custom_Field_Check_Error);
        }
    }
}
