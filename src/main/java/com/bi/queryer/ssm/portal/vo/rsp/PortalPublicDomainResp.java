package com.bi.queryer.ssm.portal.vo.rsp;

/**
 * @Auther: contributor
 * @Date: 2025/9/15 11:12
 * @Description:
 */
public class PortalPublicDomainResp {
    private String ctgId;

    private String queryTplId;

    private String analysisTplMenuId;

    private String portalId;

    private String portalName;

    private String portalMenuId;

    private String portalMenuName;

    private String portalMenuDesc;

    private String portalMenuPath;

    private String spaceCtgId;

    private String spaceCtgName;

    private Integer isInPublicDomain;

    private String analysisTplOwners ;

    public static PortalPublicDomainResp copy(PortalPublicDomainResp ori) {
        PortalPublicDomainResp copy = new PortalPublicDomainResp();
        copy.setCtgId(ori.getCtgId());
        copy.setQueryTplId(ori.getQueryTplId());
        copy.setAnalysisTplMenuId(ori.getAnalysisTplMenuId());
        copy.setPortalId(ori.getPortalId());
        copy.setPortalName(ori.getPortalName());
        copy.setPortalMenuId(ori.getPortalMenuId());
        copy.setPortalMenuName(ori.getPortalMenuName());
        copy.setPortalMenuPath(ori.getPortalMenuPath());
        copy.setPortalMenuDesc(ori.getPortalMenuDesc());
        copy.setSpaceCtgId(ori.getSpaceCtgId());
        copy.setSpaceCtgName(ori.getSpaceCtgName());
        copy.setIsInPublicDomain(ori.getIsInPublicDomain());
        return copy;
    }

    public String getCtgId() {
        return ctgId;
    }

    public void setCtgId(String ctgId) {
        this.ctgId = ctgId;
    }

    public String getQueryTplId() {
        return queryTplId;
    }

    public void setQueryTplId(String queryTplId) {
        this.queryTplId = queryTplId;
    }
    public String getAnalysisTplMenuId() {
        return analysisTplMenuId;
    }

    public void setAnalysisTplMenuId(String analysisTplMenuId) {
        this.analysisTplMenuId = analysisTplMenuId;
    }

    public String getPortalId() {
        return portalId;
    }

    public void setPortalId(String portalId) {
        this.portalId = portalId;
    }

    public String getPortalName() {
        return portalName;
    }

    public void setPortalName(String portalName) {
        this.portalName = portalName;
    }

    public String getPortalMenuId() {
        return portalMenuId;
    }

    public void setPortalMenuId(String portalMenuId) {
        this.portalMenuId = portalMenuId;
    }

    public String getPortalMenuName() {
        return portalMenuName;
    }

    public void setPortalMenuName(String portalMenuName) {
        this.portalMenuName = portalMenuName;
    }

    public String getPortalMenuDesc() {
        return portalMenuDesc;
    }

    public void setPortalMenuDesc(String portalMenuDesc) {
        this.portalMenuDesc = portalMenuDesc;
    }

    public String getPortalMenuPath() {
        return portalMenuPath;
    }

    public void setPortalMenuPath(String portalMenuPath) {
        this.portalMenuPath = portalMenuPath;
    }

    public String getSpaceCtgId() {
        return spaceCtgId;
    }

    public void setSpaceCtgId(String spaceCtgId) {
        this.spaceCtgId = spaceCtgId;
    }

    public String getSpaceCtgName() {
        return spaceCtgName;
    }

    public void setSpaceCtgName(String spaceCtgName) {
        this.spaceCtgName = spaceCtgName;
    }

    public Integer getIsInPublicDomain() {
        return isInPublicDomain;
    }

    public void setIsInPublicDomain(Integer isInPublicDomain) {
        this.isInPublicDomain = isInPublicDomain;
    }

    public String getAnalysisTplOwners() {
        return analysisTplOwners;
    }

    public void setAnalysisTplOwners(String analysisTplOwners) {
        this.analysisTplOwners = analysisTplOwners;
    }
}
