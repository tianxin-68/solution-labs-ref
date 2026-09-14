package com.bi.queryer.ssm.engine.accelerate.route.balance;

/**
 * @Author contributor
 * @Date 19:41 2024-04-10
 * @Description 集群资源
 **/
public class TrinoClusterResource {

    private String clusterName = "";

    // 查询数
    private Integer queryCount = 0 ;

    // 查询split数
    private Integer splitCount = 0;

    // 是否是主集群
    private Boolean isMasterCluster = false;


    public TrinoClusterResource() {
    }

    public TrinoClusterResource(String clusterName, Integer queryCount, Integer splitCount) {
        this.clusterName = clusterName;
        this.queryCount = queryCount;
        this.splitCount = splitCount;
    }

    public TrinoClusterResource(String clusterName, Integer queryCount, Integer splitCount, Boolean isMasterCluster) {
        this.clusterName = clusterName;
        this.queryCount = queryCount;
        this.splitCount = splitCount;
        this.isMasterCluster = isMasterCluster;
    }

    public Integer getQueryCount() {
        return queryCount;
    }

    public void setQueryCount(Integer queryCount) {
        this.queryCount = queryCount;
    }

    public Integer getSplitCount() {
        return splitCount;
    }

    public void setSplitCount(Integer splitCount) {
        this.splitCount = splitCount;
    }

    public String getClusterName() {
        return clusterName;
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    public Boolean getMasterCluster() {
        return isMasterCluster;
    }

    public void setMasterCluster(Boolean masterCluster) {
        isMasterCluster = masterCluster;
    }
}
