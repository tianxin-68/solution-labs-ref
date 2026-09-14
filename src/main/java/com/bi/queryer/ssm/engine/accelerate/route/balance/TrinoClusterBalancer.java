package com.bi.queryer.ssm.engine.accelerate.route.balance;

import com.bi.queryer.ssm.engine.config.QuerySettings;
import com.bi.queryer.ssm.engine.function.FunctionManager;
import com.bi.queryer.ssm.engine.function.IFunction;
import com.bi.queryer.sys.base.BaseDao;
import com.bi.queryer.sys.config.SC;
import com.bi.queryer.sys.db.DBUtil;
import com.bi.queryer.sys.db.DataSourceType;
import com.bi.queryer.util.BIUtil;
import com.bi.queryer.util.SpringContextUtil;
import com.bi.queryer.util.network.HttpUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.dbcp2.BasicDataSource;

import java.net.URI;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

/**
 * @Author contributor
 * @Date 19:35 2024-04-10
 * @Description 集群平衡器
 **/
public class TrinoClusterBalancer {
    private BaseDao dao = null;

    public TrinoClusterBalancer(){
        this.dao = DBUtil.getBaseDao();
    }

    /**
     * 获取最佳数据源
     * @return
     */
    public DataSourceType getBesetDataSourceType(){
        DataSourceType bestDataSource = DataSourceType.Trino_Master;
        try {
            boolean isEnable = "true".equalsIgnoreCase(SC.v("route.trino.balance.enable", "false"));
            if (!isEnable) {
                return bestDataSource;
            }

            String slaveListStr = SC.v("route.trino.balance.slave.list", "");
            if (BIUtil.isEmpty(slaveListStr)) {
                return bestDataSource;
            }

            List<DataSourceType> dataSources = new ArrayList<>();
            dataSources.add(DataSourceType.Trino_Master);

            String[] slaveKeyList = slaveListStr.split(",");
            for (String slaveKey : slaveKeyList) {
                dataSources.add(DataSourceType.getType(slaveKey));
            }

            List<TrinoClusterResource> allClusterResources = this.getAllClusterResources(dataSources);
            if (BIUtil.isEmpty(allClusterResources)) {
                return bestDataSource;
            }

            Integer maxQueryCount = Integer.valueOf(SC.v("route.trino.balance.slave.max.query.count", "5"));
            allClusterResources = allClusterResources.stream().filter(r -> r.getQueryCount() <= maxQueryCount).collect(Collectors.toList());

            // 优先转移到master：master的queryCount<=2
            Optional<TrinoClusterResource> masterResource = allClusterResources.stream().filter(r -> r.getMasterCluster()).findFirst();
            if (masterResource.isPresent() && masterResource.get().getQueryCount() <= 1) {
                return bestDataSource;
            }

            // queryCount数<= 7且splitCount/queryCount最小/集群节点数
            Collections.sort(allClusterResources, (r1, r2) -> {
                if (r1.getQueryCount() == 0) {
                    return 1;
                }
                if (r2.getQueryCount() == 0) {
                    return -1;
                }
                double rate1 = r1.getQueryCount(); //r1.getSplitCount() * 1.00 / r1.getQueryCount();
                double rate2 = r2.getQueryCount(); //r2.getSplitCount() * 1.00 / r2.getQueryCount();
                if (rate1 < rate2) {
                    return 1;
                } else if (rate1 > rate2) {
                    return -1;
                } else {
                    return 0;
                }
            });

            if (BIUtil.isNotEmpty(allClusterResources)) {
                TrinoClusterResource bestResource = allClusterResources.get(allClusterResources.size() - 1);
                bestDataSource = DataSourceType.getType(bestResource.getClusterName());
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return bestDataSource;
    }

    protected List<TrinoClusterResource> getAllClusterResources(List<DataSourceType> dataSources){
        ExecutorService executorService = Executors.newFixedThreadPool(dataSources.size());
        List<TrinoClusterResource> resourceList = new ArrayList<>();
        try {
            // 提交多个任务到线程池
            ArrayList<Future<TrinoClusterResource>> futuresList = new ArrayList<>();
            for(DataSourceType ds : dataSources) {
                futuresList.add(executorService.submit(() -> {
                    TrinoClusterResource resource = getClusterResourceByApi(ds);
                    return resource;
                }));
            }

            for (Future<TrinoClusterResource> future : futuresList) {
                    TrinoClusterResource resource = future.get();
                    if(resource != null) {
                        resourceList.add(resource);
                    }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        // 关闭线程池
        executorService.shutdown();

        return resourceList;
    }

    protected TrinoClusterResource getClusterResourceByApi(DataSourceType dataSourceType) {
        TrinoClusterResource clusterResource = new TrinoClusterResource(dataSourceType.getKey(), 0, 0);
        clusterResource.setMasterCluster(dataSourceType == DataSourceType.Trino_Master);
        try {
            IFunction fx = FunctionManager.getFunction();
            BasicDataSource dataSource = (BasicDataSource) SpringContextUtil.getBean(dataSourceType.getName());
            String jdbcUrl = dataSource.getUrl().replace("jdbc:", "");
            URI uri = URI.create(jdbcUrl);

            String url = String.format("https://%s:%d/v1/query?state=running", uri.getHost(), uri.getPort());
            Request request = new Request.Builder()
                    .url(url)
                    .header("Authorization", Credentials.basic(dataSource.getUsername(), dataSource.getPassword()))
                    .build();
            OkHttpClient client = HttpUtil.getUnsafeOkHttpClient(3);
            try {
                Response response = client.newCall(request).execute();
                if(response.isSuccessful()){
                   JSONArray array = JSON.parseArray(response.body().string());
                    clusterResource.setQueryCount(array.size());
                }
                response.close();
            } catch (Exception e) {
                System.out.println(jdbcUrl);
                e.printStackTrace();
            }
            return clusterResource;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    protected TrinoClusterResource getClusterResourceBySql(DataSourceType dataSourceType){
        TrinoClusterResource clusterResource = new TrinoClusterResource(dataSourceType.getKey(), 0, 0);
        clusterResource.setMasterCluster(dataSourceType == DataSourceType.Trino_Master);
        Statement stmt = null;
        ResultSet rs = null;
        Connection conn = null;
        try {
            // 调整负载最小的集群数据源
            conn = DBUtil.getConn(dataSourceType); //DBUtil.getConn(DBUtil.getDataSourceType());
            stmt = conn.createStatement();

            IFunction fx = FunctionManager.getFunction();
            // 监控，超时kill
            QuerySettings settings = new QuerySettings();
            settings.setQueryTimeoutSec(3);
            settings.setQueryTimeoutDetectionInterval(1);
            settings.setQueryDatasourceKey(dataSourceType.getKey());
            fx.monitor(conn, stmt, settings);

            String sql = dao.getMyBatisSql("ssm.session.getClusterResource", new HashMap<>());
            rs = stmt.executeQuery(sql);

            if(rs.next()) {
                String value = rs.getObject("queryCount") + "";
                if(BIUtil.isNotEmpty(value)) {
                    clusterResource.setQueryCount(Integer.valueOf(value));
                }

                value = rs.getObject("splitCount") + "";
                if(BIUtil.isNotEmpty(value)) {
                    clusterResource.setSplitCount(Integer.valueOf(value));
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
            clusterResource = null;
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
        return clusterResource;
    }
}
