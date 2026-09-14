package com.bi.queryer.ssm.test;

import cn.hutool.core.collection.CollUtil;
import com.bi.queryer.ssm.engine.QueryContext;
import com.bi.queryer.ssm.engine.QueryEngine;
import com.bi.queryer.ssm.engine.QueryFactory;
import com.bi.queryer.ssm.engine.accelerate.route.DataSourceRouter;
import com.bi.queryer.ssm.engine.config.QueryConfigure;
import com.bi.queryer.ssm.engine.config.field.QueryField;
import com.bi.queryer.ssm.engine.model.creator.JoinModelCreator;
import com.bi.queryer.ssm.enums.QueryArea;
import com.bi.queryer.ssm.meta.MetaField;
import com.bi.queryer.ssm.meta.SSDMetaCacheManager;
import com.bi.queryer.ssm.meta.SSDQueryTemplate;
import com.bi.queryer.ssm.query.QueryController;
import com.bi.queryer.sys.base.BaseDao;
import com.bi.queryer.sys.common.ResponseMessage;
import com.bi.queryer.sys.db.DBUtil;
import com.bi.queryer.sys.db.DataSourceType;
import com.bi.queryer.sys.enums.Enabled;
import com.bi.queryer.sys.user.UserManager;
import com.bi.queryer.sys.user.vo.User;
import com.bi.queryer.util.BIMap;
import com.bi.queryer.util.BIUtil;
import com.alibaba.fastjson.JSONObject;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @Author contributor
 * @Date 11:38 2024/9/13
 * @Description TODO
 **/
@Controller
@Scope("prototype")
@RequestMapping("test/tpl")
public class TestQueryTemplateController extends QueryController{
    public static int index = 0;
    @RequestMapping("check")
    @ResponseBody
    public ResponseMessage checkField() {
        index = 1;
        User user = UserManager.get();
        ResponseMessage result = new ResponseMessage();
        List<BIMap> templates = this.getTemplates();
        // 设置当前引擎查询默认数据源
        DataSourceRouter.setCurrentDataSourceType(DataSourceType.Trino_Master);
        int threadFieldCount = 500;
        List<List<BIMap>> threadMetaFieldList = BIUtil.splitList(templates, threadFieldCount);//BIUtil.splitList(compareFields, threadFieldCount);
        int threadCount = threadMetaFieldList.size();
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        List<CompletableFuture<List<Map>>> futures = new ArrayList<>();
        for(List<BIMap> tpls : threadMetaFieldList){
            CompletableFuture<List<Map>> future = CompletableFuture.supplyAsync(()->{
                List<Map> infos = new ArrayList<>();
                for(BIMap tpl : tpls){
                    String checkResult = checkTemplate(tpl);
                    if(BIUtil.isNotEmpty(checkResult)){
                        Map info = new HashMap();
                        info.putAll(tpl);
                        info.remove("tpl_config");
                        infos.add(info);
                        info.put("error", checkResult);
                        System.out.println(JSONObject.toJSONString(info));
                    }
                    System.out.println("--------------------->" + index++);
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
                return infos;
            }, executorService);

            futures.add(future);
        }
        List<Map> allInfos = new ArrayList<>();
        futures.forEach(f->{
            List<Map> threadResult = f.join();
            allInfos.addAll(threadResult);
        });
        executorService.shutdown();

        result.setData(allInfos);
        return result;
    }

    @Override
    protected QueryConfigure createQueryConfigure(SSDQueryTemplate queryTemplate) {
        return null;
    }

    public List<BIMap> getTemplates(){
        String sql = "select   tpl_id,     tpl_name,     tpl_owner,     cfg_id,     tpl_config,     cnt,     cnt_avg,     query_time_min,     query_time_max " +
                " from tmp_ssm_query_tpl"
              // + " where tpl_id = 'cf85f6e3034d41218cfccb8716f1eb67' "
                ;
        BaseDao dao = DBUtil.getBaseDao();
        List<BIMap> list = dao.queryMapListBySQL(sql, DataSourceType.Default);
        return list;
    }

    public String checkTemplate(BIMap tpl){
        String result = "";
        try {
            SSDQueryTemplate queryTemplate = new SSDQueryTemplate();
            queryTemplate.setConfig(tpl.get("tpl_config") + "");
            QueryConfigure queryConfigure = new QueryConfigure(queryTemplate);
            queryConfigure.load();
            queryConfigure.getSettings().setAclCheck(false);
            QueryContext cxt = new QueryContext(getRequest());
            QueryEngine engine = QueryFactory.createEngine(queryConfigure, cxt);
        }catch (Throwable e){
            //e.printStackTrace();
            result = e.getMessage();
            if(BIUtil.isNotEmpty(result) && result.contains("字段不存在")){
                result = "";
            }
        }
        return result;
    }

    @RequestMapping("filter/lod")
    @ResponseBody
    public ResponseMessage filterLod(){

        String sql = "\n" +
                "select\n" +
                "    t1.tpl_id,\n" +
                "    t2.tpl_config\n" +
                "from\n" +
                "    bi_ods.ssd_query_template t1\n" +
                "    inner join bi_ods.ssd_query_template_cfg t2 on t1.cfg_id = t2.cfg_id\n" +
                "    where t1.dt = '2025-03-09' and t2.dt = '2025-03-09'\n" +
                "    and t2.tpl_config like '%lod:%'\n" +
                "    and t1.tpl_type = 'normal'" ;

        BaseDao dao = DBUtil.getBaseDao();
        List<BIMap> list = dao.queryMapListBySQL(sql, DataSourceType.Trino_Master);

        for(BIMap map : list) {

            try {

                SSDQueryTemplate queryTemplate = new SSDQueryTemplate();
                queryTemplate.setConfig(map.get("tpl_config") + "");

                QueryConfigure queryConfigure = new QueryConfigure(queryTemplate);
                queryConfigure.load();

                if (!queryConfigure.isLodConfig()) {
                    continue;
                }


                boolean flag = false;
                List<QueryField> lodFields = queryConfigure.getLodFields();
                for (QueryField lodField : lodFields) {
                    List<QueryField> lodDimFields =  lodField.getCustomFieldConfigure().getLodConfig().getDimensionList();

                    for(QueryField lodDimField : lodDimFields){
                        Optional<QueryField> opt=   queryConfigure.getResult().getRowDimensions().stream().filter(f->f.getCode().equalsIgnoreCase(lodDimField.getCode())).findAny();
                        if(!opt.isPresent()){
                            flag = true;
                            break;
                        }

                    }

                    if(flag){
                        break;
                    }

                }

                if(flag){
                    System.out.println(map.get("tpl_id"));
                }

            }catch (Exception e){

            }

        }

        return new ResponseMessage();

    }

}
