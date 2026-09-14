package com.bi.queryer.ssm.schedule;

import cn.hutool.core.collection.CollUtil;
import com.bi.queryer.ssm.schedule.model.SSDQueryFieldStatsTotal;
import com.bi.queryer.sys.base.BaseDao;
import com.bi.queryer.sys.common.ResponseMessage;
import com.bi.queryer.sys.db.DataSourceType;
import com.bi.queryer.sys.enums.Enabled;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author contributor
 */
@Service
public class ScheduleInterfaceService{

    @Autowired
    private BaseDao dao;

    @Transactional
    public ResponseMessage refreshSSDQueryFieldStats(){
        ResponseMessage responseMessage = new ResponseMessage();
        try{
            //1、查询ssd_query_field_stats_total表获取总体统计数据
            String sqlId1 = "ssm.schedule.queryFieldStatsTotalList";
            List<SSDQueryFieldStatsTotal> fieldStatsTotalList = (List<SSDQueryFieldStatsTotal>) dao.queryObjectList(sqlId1, null, DataSourceType.Default);

            //2、按目录去掉不符合要求ctgId，转成map
            Map<String,List<SSDQueryFieldStatsTotal>> fieldStatsTotalMap =
                    fieldStatsTotalList.stream().collect(Collectors.groupingBy(
                            SSDQueryFieldStatsTotal::getCtgId
                    ));

/*            //3、查询有效字段数大于20个（已经判断了）
            String sqlId2 = "ssm.schedule.queryMeetConditionCtgIdList";
            List<String> ctgIdList = (List<String>) dao.queryObjectList(sqlId2, null, DataSourceType.Default);*/

            //4、确认归档模块
            // 确定需要归档的模块：模块有效字段数大于20个；（已经过滤了）
            // 模块月度查询次数大于10（排除bi_dept角色组的人）（在第一步调度任务sql中完成）；
            // 月度使用字段数大于10；
            List<String> archiveCtgIdList = new ArrayList<>();
            for (Map.Entry<String, List<SSDQueryFieldStatsTotal>> entry : fieldStatsTotalMap.entrySet()) {
                if(CollUtil.isEmpty(entry.getValue()) || entry.getValue().size() <= 10){
                    //月度使用字段数小于等于10
                    continue;
                }

                //记录可以归档ctgId
                archiveCtgIdList.add(entry.getKey());
            }

            //4、先清空ssd_query_field_stats_archive_ctg表
            dao.delete("ssm.schedule.deleteQueryFieldStatsArchiveCtg", null);

            //5、插入ssd_query_field_stats_archive_ctg表
            if(CollUtil.isNotEmpty(archiveCtgIdList)){
                Map<String, Object> archiveParam = new HashMap<>();
                archiveParam.put("archiveCtgIdList", archiveCtgIdList);
                dao.insert("ssm.schedule.insertQueryFieldStatsArchiveCtg",archiveParam);
            }

            //6、先清空字段表表ssd_query_field_stats
            dao.delete("ssm.schedule.deleteQueryFieldStats", null);

            //7、确认归档模块中的字段是否归档
            for(String ctgId : archiveCtgIdList){
                if(!fieldStatsTotalMap.containsKey(ctgId)){
                    continue;
                }

                List<SSDQueryFieldStatsTotal> finalUsedFieldStatsList = new ArrayList<>();

                //区分维度和指标,按下载量倒排序
                List<SSDQueryFieldStatsTotal> ctgFieldList = fieldStatsTotalMap.get(ctgId);
                List<SSDQueryFieldStatsTotal> dimList = ctgFieldList.stream()
                        .filter(e -> !Enabled.value(e.getIsMeasure()))
                        .sorted(Comparator.comparing(SSDQueryFieldStatsTotal::getQueryCount).reversed())
                        .collect(Collectors.toList());
                List<SSDQueryFieldStatsTotal> measureList = ctgFieldList.stream()
                        .filter(e -> Enabled.value(e.getIsMeasure()))
                        .sorted(Comparator.comparing(SSDQueryFieldStatsTotal::getQueryCount).reversed())
                        .collect(Collectors.toList());

                if(CollUtil.isNotEmpty(dimList) && dimList.size() > 0){
                    Integer endIndex  = (dimList.size() == 1 ? 2 : (dimList.size() + 1)) / 2 ;
                    List<SSDQueryFieldStatsTotal> dimTempList = dimList.subList(0, endIndex);
                    finalUsedFieldStatsList.addAll(dimTempList);
                }

                if(CollUtil.isNotEmpty(measureList) && measureList.size() > 0){
                    Integer endIndex  = (measureList.size() == 1 ? 2 : (measureList.size() + 1)) / 2 ;
                    List<SSDQueryFieldStatsTotal> measureTempList = measureList.subList(0, endIndex);
                    finalUsedFieldStatsList.addAll(measureTempList);
                }

                //8、归档字段插入数据库
                if(CollUtil.isNotEmpty(finalUsedFieldStatsList)){
                    try{
                        Map<String, Object> param = new HashMap<>();
                        param.put("finalUsedFieldStatsList", finalUsedFieldStatsList);
                        dao.insert("ssm.schedule.insertQueryFieldStats",param);
                    }catch (Exception e){
                       e.printStackTrace();
                       continue;
                    }
                }
            }

        }catch (Exception e){
            return new ResponseMessage(false,e.getMessage());
        }
        return responseMessage;
    }


}
