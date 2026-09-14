package com.bi.queryer.sys.authapply;

import com.bi.queryer.ssm.enums.DataSensitiveLevel;

/**
 * 工单审批链路：上级 → 数据owner → 数据owner上级 → (C4时)信安
 */
public class SsmWorkOrderApprovalRoute {

    private String directLeader;
    private String dataOwner;
    private String dataOwnerLeader;
    private String dataSensitiveLevel;
    private String securityOwner;
    private String businessOwner;

    public String buildMergeKey() {
        boolean isC4 = isC4Level(dataSensitiveLevel);
        return normalize(directLeader) + "#"
                + normalize(businessOwner) + "#"
                + normalize(dataOwner) + "#"
                + normalize(dataOwnerLeader) + "#"
                + (isC4 ? "C4" : "NON_C4") + "#"
                + (isC4 ? normalize(securityOwner) : "");
    }

    public static boolean isC4Level(String sensitiveLevel) {
        return DataSensitiveLevel.C4.getCode().equalsIgnoreCase(sensitiveLevel)
                || "C4".equalsIgnoreCase(sensitiveLevel);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }

    public String getDirectLeader() {
        return directLeader;
    }

    public void setDirectLeader(String directLeader) {
        this.directLeader = directLeader;
    }

    public String getDataOwner() {
        return dataOwner;
    }

    public void setDataOwner(String dataOwner) {
        this.dataOwner = dataOwner;
    }

    public String getDataOwnerLeader() {
        return dataOwnerLeader;
    }

    public void setDataOwnerLeader(String dataOwnerLeader) {
        this.dataOwnerLeader = dataOwnerLeader;
    }

    public String getDataSensitiveLevel() {
        return dataSensitiveLevel;
    }

    public void setDataSensitiveLevel(String dataSensitiveLevel) {
        this.dataSensitiveLevel = dataSensitiveLevel;
    }

    public String getSecurityOwner() {
        return securityOwner;
    }

    public void setSecurityOwner(String securityOwner) {
        this.securityOwner = securityOwner;
    }

    public String getBusinessOwner() { return businessOwner; }

    public void setBusinessOwner(String businessOwner) { this.businessOwner = businessOwner; }
}
