package com.bi.queryer.ssm.meta;

import cn.hutool.core.collection.CollectionUtil;
import com.bi.queryer.ssm.enums.DataEnv;
import com.bi.queryer.ssm.enums.DataSensitiveLevel;
import com.bi.queryer.ssm.mgr.fieldCtg.model.CtgNeedAuthEntity;
import com.bi.queryer.sys.base.BaseModel;
import com.bi.queryer.sys.enums.Enabled;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 字段分类
 *
 * @author contributor
 */
public class MetaFieldCategory extends BaseModel implements Comparable,Cloneable {
    // 类别id
    private String id;

    // 类别名称
    private String name;

    // 类别显示顺序
    private Double showOrder;

    // 父id
    private String parentId;

    // 父Name
    private String parentName;

    private List<MetaFieldCategory> children = new ArrayList<MetaFieldCategory>();

    private MetaFieldCategory parent = null;

    private List<MetaField> fields = new ArrayList<MetaField>();

    /**
     * 分类类型:front=前台，back=后台
     */
    private String type;

    /**
     * 分析师
     */
    private String rptDevOwner;

    private String rptDevOwnerDesc;

    /**
     * 业务负责人
     */
    private String bizOwner;

    private String bizOwnerDesc;

    /**
     * 是否对其下的内容自动归档
     */
    private Integer isAutoArchive;

    /**
     * 开发负责人
     */
    private String dataDevOwner;

    private String dataDevOwnerDesc;

    /**
     * 是否含超大表
     */
    private Integer oversized;

    /**
     * 是否是模块
     */
    private Integer isModule;

    /**
     * 内容承载时间
     */
    private String dataDate;

    /**
     * 内容承载说明
     */
    private String dataDesc;

    /**
     * 申请该目录所需数据权限
     */
    private Map<String, List<String>> authMap;

    private String ctgDesc;

    // 模块内测人员（会赋该目录权限）
    private String moduleBetaMember;


    /**
     * 对应作业
     */
    private String etlJob;

    /**
     * 数据集id
     */
    private String datasetId;

    /**
     * 节点类型 dataset 数据集  ctg 目录
     */
    private String nodeType = "ctg";

    /**
     * 是否继承
     */
    private Integer isInherited = Enabled.YES.getId();

    /**
     * 是否是测试目录，是=0（UT环境显示、线上环境不显示），否=0（线上和UT环境都显示）
     */
    private Integer isTestCtg = Enabled.NO.getId();

    //目录对应的模块目录id
    private String moduleCtgId;

    private String dataEnv = DataEnv.OLD_SSM.getCode();

    //数据权限
    private List<CtgNeedAuthEntity> ctgDataAuthList = new ArrayList<>();

    /**
     * 离线 offline,实时 rt, 预测 predict
     */
    private String dataType;

    /**
     * 目录敏感等级（聚合自身字段及子孙目录后的最高等级）
     */
    private String sensitiveLevel = DataSensitiveLevel.C1.getCode();

    public List<MetaField> getFields() {
        return fields;
    }

    public void setFields(List<MetaField> fields) {
        this.fields = fields;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Double getShowOrder() {
        return showOrder;
    }

    public void setShowOrder(Double showOrder) {
        this.showOrder = showOrder;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public List<MetaFieldCategory> getChildren() {
        return children;
    }

    public void setChildren(List<MetaFieldCategory> children) {
        this.children = children;
    }

    public MetaFieldCategory getParent() {
        return parent;
    }

    public void setParent(MetaFieldCategory parent) {
        this.parent = parent;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getRptDevOwner() {
        return rptDevOwner;
    }

    public void setRptDevOwner(String rptDevOwner) {
        this.rptDevOwner = rptDevOwner;
    }

    public String getBizOwner() {
        return bizOwner;
    }

    public void setBizOwner(String bizOwner) {
        this.bizOwner = bizOwner;
    }

    public String getBizOwnerDesc() {
        return bizOwnerDesc;
    }

    public void setBizOwnerDesc(String bizOwnerDesc) {
        this.bizOwnerDesc = bizOwnerDesc;
    }

    public String getDataDevOwner() {
        return dataDevOwner;
    }

    public void setDataDevOwner(String dataDevOwner) {
        this.dataDevOwner = dataDevOwner;
    }

    public Integer getIsAutoArchive() {
        return isAutoArchive;
    }

    public void setIsAutoArchive(Integer isAutoArchive) {
        this.isAutoArchive = isAutoArchive;
    }

    public Integer getOversized() {
        return oversized;
    }

    public void setOversized(Integer oversized) {
        this.oversized = oversized;
    }

    public String getCtgDesc() {
        return ctgDesc;
    }

    public void setCtgDesc(String ctgDesc) {
        this.ctgDesc = ctgDesc;
    }

    public String getEtlJob() {
        return etlJob;
    }

    public void setEtlJob(String etlJob) {
        this.etlJob = etlJob;
    }

    public String getParentName() {
        return parentName;
    }

    public void setParentName(String parentName) {
        this.parentName = parentName;
    }

    @Override
    public int compareTo(Object o) {
        if (o == null) {
            return -1;
        }
        Double num = (this.showOrder - ((MetaFieldCategory) o).getShowOrder());
        if(num == 0){
            return 0;
        }
        return num > 0 ? 1 : -1;
    }

    public Integer getIsModule() {
        return isModule;
    }

    public void setIsModule(Integer isModule) {
        this.isModule = isModule;
    }

    public Map<String, List<String>> getAuthMap() {
        return authMap;
    }

    public void setAuthMap(Map<String, List<String>> authMap) {
        this.authMap = authMap;
    }

    public String getDataDate() {
        return dataDate;
    }

    public void setDataDate(String dataDate) {
        this.dataDate = dataDate;
    }

    public String getDataDesc() {
        return dataDesc;
    }

    public void setDataDesc(String dataDesc) {
        this.dataDesc = dataDesc;
    }

    // 判断是否是叶子模块（可申请模块）
    public Boolean isLeafModule() {
        return this.getIsModule() == Enabled.YES.getId() && (CollectionUtil.isEmpty(this.getChildren()) || this.getChildren().get(0).getIsModule() != Enabled.YES.getId());
    }

    //获取路径信息
    public String getCategoryPath() {
        if (this.getParent() == null) {
            return this.getName();
        } else {
            return this.getParent().getName() + "/" + this.getName();
        }
    }

    public String getTopParentCtgId(){
        if(this.parent == null){
            return this.getId();
        }
        else{
            return this.parent.getTopParentCtgId();
        }
    }

    public String getModuleBetaMember() {
        return moduleBetaMember;
    }

    public void setModuleBetaMember(String moduleBetaMember) {
        this.moduleBetaMember = moduleBetaMember;
    }

    public String getDatasetId() {
        return datasetId;
    }

    public void setDatasetId(String datasetId) {
        this.datasetId = datasetId;
    }

    public String getNodeType() {
        return nodeType;
    }

    public void setNodeType(String nodeType) {
        this.nodeType = nodeType;
    }

    public Integer getIsInherited() {
        return isInherited;
    }

    public void setIsInherited(Integer isInherited) {
        this.isInherited = isInherited;
    }

    public String getModuleCtgId() {
        return moduleCtgId;
    }

    public void setModuleCtgId(String moduleCtgId) {
        this.moduleCtgId = moduleCtgId;
    }

    public List<CtgNeedAuthEntity> getCtgDataAuthList() {
        return ctgDataAuthList;
    }

    public void setCtgDataAuthList(List<CtgNeedAuthEntity> ctgDataAuthList) {
        this.ctgDataAuthList = ctgDataAuthList;
    }

    public Integer getIsTestCtg() {
        return isTestCtg;
    }

    public void setIsTestCtg(Integer isTestCtg) {
        this.isTestCtg = isTestCtg;
    }

    public String getDataEnv() {
        return dataEnv;
    }

    public void setDataEnv(String dataEnv) {
        this.dataEnv = dataEnv;
    }

    public String getDataType() {
        return dataType;
    }

    public void setDataType(String dataType) {
        this.dataType = dataType;
    }

    public MetaFieldCategory clone() {
        MetaFieldCategory clone = new MetaFieldCategory();
        try {
            clone = (MetaFieldCategory) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
        return clone;
    }

    public String getRptDevOwnerDesc() {
        return rptDevOwnerDesc;
    }

    public void setRptDevOwnerDesc(String rptDevOwnerDesc) {
        this.rptDevOwnerDesc = rptDevOwnerDesc;
    }

    public String getDataDevOwnerDesc() {
        return dataDevOwnerDesc;
    }

    public void setDataDevOwnerDesc(String dataDevOwnerDesc) {
        this.dataDevOwnerDesc = dataDevOwnerDesc;
    }

    public String getSensitiveLevel() {
        return sensitiveLevel;
    }

    public void setSensitiveLevel(String sensitiveLevel) {
        this.sensitiveLevel = sensitiveLevel;
    }
}
