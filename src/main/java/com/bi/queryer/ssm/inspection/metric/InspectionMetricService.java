package com.bi.queryer.ssm.inspection.metric;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import com.bi.queryer.ssm.enums.DateGranularity;
import com.bi.queryer.ssm.enums.TaskExecStatus;
import com.bi.queryer.ssm.inspection.metric.entity.InspectionMetricEntity;
import com.bi.queryer.ssm.inspection.metric.entity.InspectionMetricResultEntity;
import com.bi.queryer.ssm.inspection.metric.entity.InspectionMetricResultFieldEntity;
import com.bi.queryer.ssm.meta.MetaField;
import com.bi.queryer.ssm.meta.MetaTable;
import com.bi.queryer.ssm.meta.SSDMetaCacheManager;
import com.bi.queryer.sys.base.BaseDao;
import cn.hutool.core.util.StrUtil;
import com.bi.queryer.sys.db.DBUtil;
import com.bi.queryer.sys.db.DataSourceType;
import com.bi.queryer.sys.enums.Enabled;
import com.bi.queryer.sys.user.UserManager;
import com.bi.queryer.util.BIConsts;
import com.bi.queryer.util.BIUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.*;
import java.util.regex.Matcher;

@Service
@Scope("prototype")
public class InspectionMetricService {

    @Autowired
    private BaseDao dao;

    private static final String DEFAULT_DS_KEY = DataSourceType.Doris_Master.getKey();

    public void execute(String tableId, Integer isQueryTwoYears, String dsKey) {

        if (StrUtil.isBlank(dsKey)) {
            dsKey = DEFAULT_DS_KEY;
        }

        Map<String, String> param = new HashMap<>();
        param.put("tableId", tableId);
        List<InspectionMetricEntity> inspectionTableEntityList = (List<InspectionMetricEntity>) dao.queryObjectList("ssm.inspection.metric.queryAll", param);

        if (CollUtil.isEmpty(inspectionTableEntityList)) {
            return;
        }

        //按tableId进行分组
        Map<String, Set<String>> tableIdFieldCodeMap = new HashMap<>();
        Map<String, String> tableIdTableNameMap = new HashMap<>();

        //字段可以归属到多个表，key = table_id + field_code
        Map<String,InspectionMetricResultFieldEntity> fieldInfoMap = new HashMap<>();
        for (InspectionMetricEntity inspectionTableEntity : inspectionTableEntityList) {
            Set<String> fieldCodeList = tableIdFieldCodeMap.get(inspectionTableEntity.getTableId());
            if (CollUtil.isEmpty(fieldCodeList)) {
                fieldCodeList = new HashSet<>();
            }
            fieldCodeList.add(inspectionTableEntity.getFieldCode());
            tableIdFieldCodeMap.put(inspectionTableEntity.getTableId(), fieldCodeList);

            tableIdTableNameMap.put(inspectionTableEntity.getTableId(), inspectionTableEntity.getTableName());

            InspectionMetricResultFieldEntity inspectionMetricResultFieldEntity = new InspectionMetricResultFieldEntity();
            inspectionMetricResultFieldEntity.setFieldCode(inspectionTableEntity.getFieldCode());
            inspectionMetricResultFieldEntity.setFieldTitle(inspectionTableEntity.getFieldTitle());
            inspectionMetricResultFieldEntity.setFieldOwner(inspectionTableEntity.getFieldOwner());
            fieldInfoMap.put(inspectionTableEntity.getTableId()+inspectionTableEntity.getFieldCode(), inspectionMetricResultFieldEntity);
        }

        //巡检时间 = 从前天算的近7天+年同比
        List<String> checkTimeList = new ArrayList<>();

        int days = 8;
        if(Enabled.value(isQueryTwoYears)){
            days = 366;
        }

        for (int i = 1; i < days; i++) {
            DateTime offsetDate = DateUtil.offsetDay(DateUtil.yesterday(), -i);
            DateTime offsetDateYearTb = DateUtil.offset(offsetDate, DateField.YEAR, -1);
            checkTimeList.add(DateUtil.formatDate(offsetDate));
            checkTimeList.add(DateUtil.formatDate(offsetDateYearTb));
        }

        Collections.sort(checkTimeList);

        Long batchNo = System.currentTimeMillis();
        long t1 = System.currentTimeMillis();

        int totalCount = tableIdFieldCodeMap.keySet().size();
        System.out.println("开始巡检表，共有" + totalCount + "张表");
        for (String key : tableIdFieldCodeMap.keySet()) {
            long t2 = System.currentTimeMillis();
            System.out.println("开始巡检表" + key + "," + tableIdTableNameMap.get(key));
            inspectionMetric(key, tableIdFieldCodeMap.get(key), checkTimeList, batchNo, fieldInfoMap, dsKey);
            System.out.println("************巡检表" + key + "," + tableIdTableNameMap.get(key) + "耗时：" + (System.currentTimeMillis() - t2) + "毫秒");
        }

        long t3 = System.currentTimeMillis();
        System.out.println("************巡检表指标是否掉0耗时：" + (t3 - t1) + "毫秒");

    }

    /**
     * 巡检表指标是否掉0
     * @param tableId
     * @param fieldCodeList
     * @param batchNo
     */
    public void inspectionMetric(String tableId, Set<String> fieldCodeList, List<String> checkTimeList, Long batchNo, Map<String, InspectionMetricResultFieldEntity> fieldInfoMap, String dsKey) {

        MetaTable metaTable = SSDMetaCacheManager.getTable(tableId);
        if (metaTable == null) {
            return;
        }

        //判断粒度是不是日粒度
        DateGranularity dateGranularity = DateGranularity.get(metaTable.getDateGranularity());
        if (DateGranularity.DAY != dateGranularity) {
            return;
        }

        //找到表中的时间分区字段，code = dt
        List<MetaField> metaFields = SSDMetaCacheManager.getTableFields(metaTable.getId());
        Optional<MetaField> dtFieldOpt = metaFields.stream().filter(metaField -> metaField.getCode().equalsIgnoreCase(BIConsts.DATE_CODE)).findAny();
        if (!dtFieldOpt.isPresent()) {
            return;
        }

        MetaField dtField = dtFieldOpt.get();

        InspectionMetricResultEntity inspectionMetricResultEntity = new InspectionMetricResultEntity();
        inspectionMetricResultEntity.setTableId(tableId);
        inspectionMetricResultEntity.setTableName(metaTable.getFullName());
        inspectionMetricResultEntity.setBatchNo(batchNo);
        inspectionMetricResultEntity.setExecStatus(TaskExecStatus.SUCCESS.getCode());
        inspectionMetricResultEntity.setCreatedBy(UserManager.get().getName());
        inspectionMetricResultEntity.setDsKey(dsKey);

        //构建sql
        StringBuilder sql = new StringBuilder();
        sql.append(buildSelectSql(dtField, fieldCodeList, tableId));
        sql.append(buildFromSql(metaTable));
        sql.append(buildWhereSql(dtField, checkTimeList));
        sql.append(buildGroupSql(dtField));

        Statement stmt = null;
        ResultSet rs = null;
        Connection conn = null;

        try {

            conn = DBUtil.getConn(DataSourceType.getType(dsKey));
            stmt = conn.createStatement();
            rs = stmt.executeQuery(sql.toString());

            //获取查询结果的字段名，避免巡检的字段不存在，报错
            List<String> columnNameList = new ArrayList<>();
            ResultSetMetaData md = rs.getMetaData();
            for (int i = 1; i <= md.getColumnCount(); i++) {
                columnNameList.add(md.getColumnLabel(i)); // 别名
            }

            List<String> dtList = new ArrayList<>();
            while (rs.next()) {
                String dt = rs.getString(dtField.getCode());
                dtList.add(dt);
                for (String fieldCode : fieldCodeList) {

                    if(!columnNameList.contains(fieldCode)){
                        continue;
                    }

                    Object value = rs.getObject(fieldCode);
                    if (value == null || value.equals(0)) {
                        InspectionMetricResultFieldEntity inspectionMetricResultFieldEntity = fieldInfoMap.get(tableId+fieldCode);

                        InspectionMetricResultFieldEntity fieldEntity = new InspectionMetricResultFieldEntity();
                        BeanUtils.copyProperties(inspectionMetricResultFieldEntity, fieldEntity);
                        fieldEntity.setMetricEmptyPartitionDate(dt);
                        fieldEntity.setMetricEmptyRemark("指标值为空或者0");
                        inspectionMetricResultEntity.getInspectionMetricResultFieldEntityList().add(fieldEntity);
                    }
                }
            }

            //判断是否有确实的天数
            for(String checkTime : checkTimeList){
                if(dtList.contains(checkTime)){
                    continue;
                }

                for (String fieldCode : fieldCodeList) {
                    InspectionMetricResultFieldEntity inspectionMetricResultFieldEntity = fieldInfoMap.get(tableId+fieldCode);

                    InspectionMetricResultFieldEntity fieldEntity = new InspectionMetricResultFieldEntity();
                    BeanUtils.copyProperties(inspectionMetricResultFieldEntity, fieldEntity);
                    fieldEntity.setMetricEmptyPartitionDate(checkTime);
                    fieldEntity.setMetricEmptyRemark("分区为空");
                    inspectionMetricResultEntity.getInspectionMetricResultFieldEntityList().add(fieldEntity);
                }

            }

        } catch (Exception e) {
            inspectionMetricResultEntity.setExecStatus(TaskExecStatus.FAIL.getCode());
            String errorMsg = e.getMessage();
            errorMsg = errorMsg.replaceAll("'","");
            inspectionMetricResultEntity.setRemark(errorMsg);
            e.printStackTrace();
        } finally {
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
            } catch (Exception e) {
                e.printStackTrace();
            }
        }


        if(TaskExecStatus.FAIL == TaskExecStatus.get(inspectionMetricResultEntity.getExecStatus())){
            dao.insert("ssm.inspection.metric.insertErrorResult",inspectionMetricResultEntity);
        }else{
            if (CollUtil.isNotEmpty(inspectionMetricResultEntity.getInspectionMetricResultFieldEntityList()) ) {

                //将inspectionMetricResultFieldEntityList按2000拆分插入，避免数据
                List<List<InspectionMetricResultFieldEntity>> metricResultFieldEntityList = BIUtil.splitList(inspectionMetricResultEntity.getInspectionMetricResultFieldEntityList(),2000);
                for(List<InspectionMetricResultFieldEntity> splitList : metricResultFieldEntityList){
                    if(CollUtil.isEmpty(splitList)){
                        continue;
                    }

                    InspectionMetricResultEntity insertInspectionMetricResult = new InspectionMetricResultEntity();
                    insertInspectionMetricResult.setTableId(inspectionMetricResultEntity.getTableId());
                    insertInspectionMetricResult.setTableName(inspectionMetricResultEntity.getTableName());
                    insertInspectionMetricResult.setBatchNo(inspectionMetricResultEntity.getBatchNo());
                    insertInspectionMetricResult.setExecStatus(inspectionMetricResultEntity.getExecStatus());
                    insertInspectionMetricResult.setRemark(inspectionMetricResultEntity.getRemark());
                    insertInspectionMetricResult.setInspectionMetricResultFieldEntityList(splitList);
                    dao.insert("ssm.inspection.metric.batchInsert", insertInspectionMetricResult);
                }

            }
        }

    }

    public StringBuilder buildSelectSql( MetaField dtField,Set<String> fieldCodeList,String tableId) {
        StringBuilder selectSql = new StringBuilder();

        List<String> selectFragments = new ArrayList<>();
        //添加时间字段查询
        selectFragments.add(String.format(" %s as %s ", dtField.getName(), dtField.getCode()));

        for (String fieldCode : fieldCodeList) {
            MetaField metaField = SSDMetaCacheManager.getFieldByCode(tableId, fieldCode);
            if (metaField == null) {
                continue;
            }

            //处理计算字段，替换[]中的内容
            if (CollUtil.isNotEmpty(metaField.getCalcAtomFields())) {
                String aggExpression = metaField.getAggExpression();
                for(MetaField atomField : metaField.getCalcAtomFields()){
                    aggExpression = aggExpression.replaceAll("\\[" + atomField.getId() + "\\]", Matcher.quoteReplacement(atomField.getName()));
                }

                //去掉distinct，提高查询效率
                aggExpression = aggExpression.replaceAll("distinct"," ");
                selectFragments.add(String.format(" %s as %s ", aggExpression, metaField.getCode()));

            } else {
                selectFragments.add(String.format(" count(%s) as %s ", metaField.getName(), metaField.getCode()));
            }
        }

        selectSql.append(String.format("select %s ", BIUtil.listToStr(selectFragments, ",")));
        return selectSql;
    }

    public StringBuilder buildFromSql(MetaTable metaTable) {
        StringBuilder fromSql = new StringBuilder();
        fromSql.append(String.format(" from %s ", metaTable.getFullName()));
        return fromSql;
    }

    public StringBuilder buildWhereSql(MetaField dtField,List<String> checkTimeList) {
        StringBuilder whereSql = new StringBuilder();
        whereSql.append(" where ");
        whereSql.append(String.format(" %s in (%s)", dtField.getName(), BIUtil.listToStr(checkTimeList, ",","'")));
        return whereSql;
    }

    public StringBuilder buildGroupSql(MetaField dtField){
        StringBuilder groupSql = new StringBuilder();
        groupSql.append(String.format(" group by %s ", dtField.getName()));
        return groupSql;
    }

}
