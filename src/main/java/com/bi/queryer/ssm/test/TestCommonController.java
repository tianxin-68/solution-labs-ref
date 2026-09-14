package com.bi.queryer.ssm.test;

import cn.hutool.core.io.FileUtil;
import com.bi.queryer.ssm.custom.CustomFieldParser;
import com.bi.queryer.ssm.engine.accelerate.hot.mq.AdhocMQReceiver;
import com.bi.queryer.ssm.engine.accelerate.hot.mq.action.ActionByMQ;
import com.bi.queryer.ssm.meta.MetaField;
import com.bi.queryer.ssm.meta.MetaTable;
import com.bi.queryer.ssm.meta.SSDMetaCacheManager;
import com.bi.queryer.ssm.mgr.fieldDef.FieldDefService;
import com.bi.queryer.ssm.mgr.fieldDef.model.ManualIndexWhitePaperEntity;
import com.bi.queryer.ssm.util.AggregatorItem;
import com.bi.queryer.ssm.util.SqlExpressionUtil;
import com.bi.queryer.sys.common.ResponseMessage;
import com.bi.queryer.sys.db.DBUtil;
import com.bi.queryer.sys.db.DataSourceType;
import com.bi.queryer.sys.enums.Enabled;
import com.bi.queryer.util.BIUtil;
import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @Author contributor
 * @Date 11:15 2024/10/26
 * @Description TODO
 **/

@Controller
@Scope("prototype")
@RequestMapping("test/common")
public class TestCommonController {

    @Autowired
    private FieldDefService fieldDefService = null;
    @RequestMapping("parse/field/ref")
    @ResponseBody
    public ResponseMessage parseFieldRef() {
        List<TestCalcField> fields = this.parseCalcField();
        System.out.println("----------引用表达式替换-------------");
        for(TestCalcField f : fields){
            List<String> list = new ArrayList<>();
            list.add(f.getFieldId());
            list.add(f.getOldExpression());
            list.add(f.getNewExpression());
            System.out.println(BIUtil.listToStr(list, "\t"));
        }

        System.out.println("----------引用字段列表-------------");
        for(TestCalcField f : fields){
            List<String> refIdList = f.getRefIdList();
            if(BIUtil.isEmpty(refIdList)){
                continue;
            }
            for(String refId : refIdList){
                List<String> list = new ArrayList<>();
                list.add(f.getFieldId());
                list.add(refId);
                System.out.println(BIUtil.listToStr(list, "\t"));
            }
        }
        return new ResponseMessage();
    }

    protected List<TestCalcField> parseCalcField(){
        List<TestCalcField> calcFields = new ArrayList<>();
        Pattern pattern = Pattern.compile(CustomFieldParser.expressionPattern);

        // 白皮书编码列表
        Map<String, ManualIndexWhitePaperEntity> manualIndexWhitePaperMap = fieldDefService.getManualIndexWhitePaperMap(null);

        // 只处理V2字段的表达式
        Map<String, MetaField> fields = SSDMetaCacheManager.getFieldsCache();
        List<String> notExistRefInfos = new ArrayList<>();
        for(MetaField metaField : fields.values()){
            MetaTable table = SSDMetaCacheManager.getTable(metaField.getTableId());
            if(table == null){
                continue;
            }
            String tableFullName = table.getFullName();
            if(!tableFullName.toLowerCase().contains("bi_olap.")){
                continue;
            }
            String expression = metaField.getRawAggExpression();
            if(BIUtil.isEmpty(expression) || !expression.contains("[")){
                continue;
            }
            // 未归属目录的，不处理
            if(BIUtil.isEmpty(metaField.getCategoryId())){
               // continue;
            }

            if(Enabled.isFalse(metaField.getIsShow())){
              //  continue;
            }

            // 日均不处理
            if(metaField.getCode().contains("avg_by_d")){
                continue;
            }

            List<String> refIdList = new ArrayList<>();
            Map<String, ManualIndexWhitePaperEntity> refFieldWhitePapers = new HashMap<>();
            String newExpression = expression;
            Matcher matcher = pattern.matcher(newExpression);
            while (matcher.find()){
                String refFieldCode = matcher.group();
                if(BIUtil.isEmpty(refFieldCode)){
                    continue;
                }
                MetaField refField = SSDMetaCacheManager.getFieldByCode(metaField.getTableId(), refFieldCode);
                if(refField == null){
                    refField = SSDMetaCacheManager.getFieldByName(metaField.getTableId(), refFieldCode);
                }
                if(refField == null && refFieldCode.contains(".")){
                    refField = SSDMetaCacheManager.getFieldByFullName(refFieldCode);
                }
                if(refField == null){
                    List<String> list = new ArrayList<>();
                    list.add(metaField.getId());
                    list.add(table.getFullName());
                    list.add(metaField.getCode());
                    list.add(metaField.getTitle());
                    list.add(expression);
                    list.add(refFieldCode);
                    list.add(metaField.getCreatedBy());
                    notExistRefInfos.add(BIUtil.listToStr(list, "\t"));
                    continue;
                }
                newExpression = newExpression.replaceAll("\\[" + refFieldCode + "\\]",  "[" + refField.getId() + "]");
                refIdList.add(refField.getId());

                ManualIndexWhitePaperEntity whitePaper = manualIndexWhitePaperMap.get(refField.getKpiNo());
                if(whitePaper != null) {
                    refFieldWhitePapers.put(refField.getCode(), whitePaper);
                }
            }
            TestCalcField calcField = new TestCalcField(metaField);
            calcField.setTableId(table.getId());
            calcField.setTableName(tableFullName);
            calcField.setNewExpression(newExpression);
            calcField.setRefIdList(refIdList.stream().distinct().collect(Collectors.toList()));
            calcField.setRefWhitePapers(refFieldWhitePapers);
            calcFields.add(calcField);
        }
        System.out.println("----------无效引用清单-------------");
        notExistRefInfos.forEach(s -> System.out.println(s));
        return calcFields;
    }

    @RequestMapping("disassemble/compound/field")
    @ResponseBody
    public ResponseMessage disassembleCompoundField() {
        List<TestCalcField> calcFields = this.parseCalcField();
        List<TestCompoundField> compoundFields = new ArrayList<>();
        for(TestCalcField f : calcFields){
            String expression = f.getOldExpression();
            if(!SqlExpressionUtil.isCompound(expression)){
                continue;
            }
            List<AggregatorItem> aggregatorItems = new ArrayList<>();
            try {
                aggregatorItems = SqlExpressionUtil.parseAggregators(expression);
            }catch (Exception e){
                System.out.println(expression);
                e.printStackTrace();
                return new ResponseMessage();
            }
            if(BIUtil.isEmpty(aggregatorItems)){
                continue;
            }
            TestCompoundField compoundField = TestCompoundField.copy(f);
            int index = 2;
            String newExpression = f.getOldExpression();
            for(AggregatorItem aggItem : aggregatorItems) {
                TestCompoundFieldOperator operator = new TestCompoundFieldOperator();
                operator.setOperatorFieldCode(String.format("%s_%s", f.getFieldCode(), index));
                operator.setOperatorFieldName(String.format("%s_%s", f.getFieldName(), index));

                String operatorAggExpr = String.format("%s(%s)", aggItem.getAggregator() , aggItem.getContent());
                operator.setOperatorAggExpression(operatorAggExpr);

                compoundField.getOperators().add(operator);

                String replacement = String.format("[%s]", operator.getOperatorFieldCode());
                newExpression = newExpression.replace(operatorAggExpr, replacement);
                index++;
            }
            compoundField.setNewExpression(newExpression);
            compoundFields.add(compoundField);
        }
        for(TestCompoundField c : compoundFields){
            for(TestCompoundFieldOperator op : c.getOperators()){
                List<String> list = new ArrayList<>();
                list.add(c.getTableId());
                list.add(c.getTableName());
                list.add(c.getFieldId());
                list.add(c.getFieldCode());
                list.add(c.getFieldName());
                list.add(c.getFieldTitle());
                list.add(c.getOldExpression());
                list.add(c.getNewExpression());
                list.add(c.getOwner());
                list.add(op.getOperatorFieldCode());
                list.add(op.getOperatorFieldName());
                list.add(op.getOperatorAggExpression());
                String s = BIUtil.listToStr(list, "\t");
                System.out.println(s);
            }
        }
        return new ResponseMessage();
    }

    /**
     * 复合字段中引用了绑定白皮书编码的指标为不无效
     * @return
     */
    @RequestMapping("get/invalid/compound/field")
    @ResponseBody
    public ResponseMessage getInvalidCompoundField() {
        List<TestCalcField> calcFields = this.parseCalcField();
        System.out.println("----------无效复合指标-------------");
        List<TestCalcField> invalidCompoundFields = new ArrayList<>();
        for(TestCalcField f : calcFields){
            String expression = f.getOldExpression();
            if(!SqlExpressionUtil.isCompound(expression)){
                continue;
            }
            // 无聚合方式不处理
            List<AggregatorItem> aggregatorItems = SqlExpressionUtil.parseAggregators(expression);
            if(BIUtil.isEmpty(aggregatorItems)){
                continue;
            }
            Map<String, ManualIndexWhitePaperEntity> whitePaperMap = f.getRefWhitePapers();
            if(BIUtil.isNotEmpty(whitePaperMap)){
                for(ManualIndexWhitePaperEntity wp : whitePaperMap.values()){
                    if(Enabled.isTrue(wp.getIsMeasure())){
                        invalidCompoundFields.add(f);
                    }
                }
            }
        }
        for(TestCalcField c : invalidCompoundFields){
            List<String> list = new ArrayList<>();
            list.add(c.getTableId());
            list.add(c.getTableName());
            list.add(c.getFieldId());
            list.add(c.getFieldCode());
            list.add(c.getFieldName());
            list.add(c.getFieldTitle());
            list.add(c.getOldExpression());

            List<String> wpCodes = new ArrayList<>();
            // 获取白皮书编码列表
            if(BIUtil.isNotEmpty(c.getRefWhitePapers())) {
                wpCodes = c.getRefWhitePapers().values().stream().map(wp -> wp.getIndexNo()).collect(Collectors.toList());
            }
            list.add(BIUtil.listToStr(wpCodes, "|"));

            list.add(c.getOwner());

            String s = BIUtil.listToStr(list, "\t");

            System.out.println(s);
        }

        return new ResponseMessage();
    }


    public static void main(String[] args) {
    }

    @RequestMapping("test/doris/cache")
    @ResponseBody
    public ResponseMessage testDorisCache(){
        byte[] bytes = FileUtil.readBytes("/Users/contributor/Documents/workspace/bi_queryer/src/main/java/cn/bi_queryer/bi/ssm/test/doris_query.sql");
        String sql = new String(bytes);
        sql = "/*uid=contributor*/ " + sql;
        Connection conn = null;
        PreparedStatement stmt =  null;
        ResultSet rs = null;
        int size = 0;
        long t1 = System.currentTimeMillis();
        // 查询sql
        try{
            conn = DBUtil.getConn(DataSourceType.Doris_Master);
            stmt = conn.prepareStatement(sql);
            rs = stmt.executeQuery();
            while (rs.next()){
                size++;
            }
        }catch(Exception e){
            e.printStackTrace();
        }finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (stmt != null) {
                    stmt.close();
                }
                if (conn != null) {
                    conn.close();
                }
            }catch(Exception e){
                e.printStackTrace();
            }
        }
        ResponseMessage result = new ResponseMessage();
        long t2 = System.currentTimeMillis();
        Map<String, Object> data = new HashMap<>();
        data.put("size", size);
        data.put("consume", (t2-t1)/1000.0);
        result.setData(data);
        return result;
    }

    @RequestMapping("test/adhoc/mq")
    @ResponseBody
    public ResponseMessage testAdhocMq(){
        AdhocMQReceiver adhocMQReceiver = new AdhocMQReceiver();
        JSONObject msg = new JSONObject();
        msg.put("tableName", "bi_view.t1");
        msg.put("operation", "insert");
        msg.put("operator", "contributor");
        List<ActionByMQ> actionList =  adhocMQReceiver.accept(msg);
        ResponseMessage result = new ResponseMessage();
        result.setData(actionList.size());
        return result;
    }
}
