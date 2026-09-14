package com.bi.queryer.ssm.inspection.table;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.bi.queryer.ssm.enums.DateGranularity;
import com.bi.queryer.ssm.enums.TaskExecStatus;
import com.bi.queryer.ssm.inspection.table.entity.InspectionTableEntity;
import com.bi.queryer.ssm.inspection.table.entity.InspectionTableResultEntity;
import com.bi.queryer.ssm.meta.MetaField;
import com.bi.queryer.ssm.meta.MetaTable;
import com.bi.queryer.ssm.meta.SSDMetaCacheManager;
import com.bi.queryer.sys.base.BaseDao;
import com.bi.queryer.sys.db.DBUtil;
import com.bi.queryer.sys.db.DataSourceType;
import com.bi.queryer.sys.user.UserManager;
import com.bi.queryer.util.BIConsts;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Scope("prototype")
public class InspectionTableService {

    @Autowired
    private BaseDao dao;

    private static final String DEFAULT_DS_KEY = DataSourceType.Doris_Master.getKey();

    public void execute(String tableName, String dsKey) {

        if (StrUtil.isBlank(dsKey)) {
            dsKey = DEFAULT_DS_KEY;
        }

       List<InspectionTableEntity> inspectionTableEntityList = (List<InspectionTableEntity>) dao.queryObjectList("ssm.inspection.table.queryAll",null);

       //按入参表名过滤
       if(StrUtil.isNotEmpty(tableName)){
           inspectionTableEntityList = inspectionTableEntityList
                   .stream()
                   .filter(inspectionTableEntity -> tableName.equalsIgnoreCase(inspectionTableEntity.getTableName())).collect(Collectors.toList());
       }

       if(CollUtil.isEmpty(inspectionTableEntityList)){
           return;
       }

       Long batchNo = System.currentTimeMillis();
       long t1 = System.currentTimeMillis();
       int totalCount = inspectionTableEntityList.size();
       System.out.println("开始巡检表，共有" + totalCount + "张表");
       int inspectionCount = 0;
       for(InspectionTableEntity inspectionTableEntity : inspectionTableEntityList){
           System.out.println("开始巡检表：" + inspectionTableEntity.getTableName());
           inspectionCount++;
           inspectionTable(inspectionTableEntity, batchNo, dsKey);
           System.out.println("巡检表完成：" + inspectionTableEntity.getTableName());
           System.out.println("巡检表进度：" + inspectionCount + " / " + totalCount);
       }
       long t2 = System.currentTimeMillis();
       System.out.println("************巡检表耗时：" + (t2 - t1) + "毫秒");

    }

    /**
     * 巡检表分区
     * @param inspectionTableEntity
     */
    public void inspectionTable(InspectionTableEntity inspectionTableEntity, Long batchNo, String dsKey) {

        //通过表名，找到多维缓存中的表
        MetaTable metaTable = SSDMetaCacheManager.getTableByFullName(inspectionTableEntity.getTableName());
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

        //构建查询sql
        /**
         * select
         *  t.dt,
         *  max(t.dt) over() as max_dt,
         *  min(t.dt) over() as min_dt,
         *  count(1) as cnt
         * from bi_olap.ads_tfc_deviceid_active_dtl_di t
         * group by t.dt;
         */
        StringBuilder querySQL = new StringBuilder();
        querySQL.append(buildSelectSQL(dtField));
        querySQL.append(buildFromSQL(metaTable));
        querySQL.append(buildGroupBySQL(dtField));

        Statement stmt = null;
        ResultSet rs = null;
        Connection conn = null;

        InspectionTableResultEntity inspectionTableResultEntity = new InspectionTableResultEntity();

        try {

            conn = DBUtil.getConn(DataSourceType.getType(dsKey));
            stmt = conn.createStatement();
            rs = stmt.executeQuery(querySQL.toString());

            //日期对应的数据量 key= dt, value = cnt
            Map<String, Integer> dtCntMap = new HashMap<>();
            String maxDt = null;
            String minDt = null;
            while (rs.next()) {
                String dt = rs.getString(BIConsts.DATE_CODE);

                if (StrUtil.isEmpty(maxDt)) {
                    maxDt = rs.getString("max_dt");
                    minDt = rs.getString("min_dt");
                }

                int cnt = rs.getInt("cnt");
                dtCntMap.put(dt, cnt);
            }

            //获取最小日期和最大日期之间的所有日期
            List<String> dateList = DateUtil.rangeToList(DateUtil.parseDate(minDt), DateUtil.parseDate(maxDt), DateField.DAY_OF_YEAR)
                    .stream().map(f -> f.toDateStr()).collect(Collectors.toList());

            //排序
            Collections.sort(dateList);

            List<String> missingDateList = new ArrayList<>();
            for (String dt : dateList) {
                Integer cnt = dtCntMap.get(dt);
                //分区缺失
                if (cnt == null || cnt == 0) {
                    missingDateList.add(dt);
                }
            }

            inspectionTableResultEntity.setTableName(metaTable.getFullName());
            inspectionTableResultEntity.setTableOwner(inspectionTableEntity.getTableOwner());
            inspectionTableResultEntity.setEmptyPartitionDateList(missingDateList);
            inspectionTableResultEntity.setBatchNo(batchNo);
            inspectionTableResultEntity.setCreatedBy(UserManager.get().getName());
            inspectionTableResultEntity.setMaxPartitionDate(maxDt);
            inspectionTableResultEntity.setMinPartitionDate(minDt);
            inspectionTableResultEntity.setDsKey(dsKey);
            inspectionTableResultEntity.setExecStatus(TaskExecStatus.SUCCESS.getCode());

        } catch (Exception e) {

            inspectionTableResultEntity.setExecStatus(TaskExecStatus.FAIL.getCode());
            inspectionTableResultEntity.setRemark(e.getMessage());

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



        if(TaskExecStatus.FAIL == TaskExecStatus.get(inspectionTableResultEntity.getExecStatus())){
            dao.insert("ssm.inspection.table.insertErrorResult", inspectionTableResultEntity);
        }else {
            //记录缺失的分区
            if (CollUtil.isNotEmpty(inspectionTableResultEntity.getEmptyPartitionDateList())) {
                dao.insert("ssm.inspection.table.insertInspectionTableResult", inspectionTableResultEntity);
            }
        }

    }

    public String buildSelectSQL( MetaField dtField) {
        StringBuilder whereSQL = new StringBuilder();
        whereSQL.append("select ");
        whereSQL.append(String.format("%s as %s ",dtField.getName(),dtField.getCode()));
        whereSQL.append(String.format(",max(%s) over() as max_dt ",dtField.getName()));
        whereSQL.append(String.format(",min(%s) over() as min_dt ",dtField.getName()));
        whereSQL.append(String.format(",count(1) as cnt "));
        return whereSQL.toString();
    }

    public String buildFromSQL(MetaTable metaTable) {
        StringBuilder fromSQL = new StringBuilder();
        fromSQL.append(" from ");
        fromSQL.append(metaTable.getFullName());
        return fromSQL.toString();
    }

    public String buildGroupBySQL( MetaField dtField) {
        StringBuilder groupSQL = new StringBuilder();
        groupSQL.append(" group by ");
        groupSQL.append(dtField.getName());
        return groupSQL.toString();
    }


}
