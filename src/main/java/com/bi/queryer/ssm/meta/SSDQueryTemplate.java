package com.bi.queryer.ssm.meta;

import com.bi.queryer.sys.enums.Enabled;
import com.bi.queryer.util.BIUtil;
import com.bi.queryer.util.JSONSerializable;
import com.alibaba.fastjson.JSONObject;

public class SSDQueryTemplate implements JSONSerializable {

	private String id;

	private String name;
	
	private String description;
	
	private String type = "template";

	/**
	 * 最大导出数量
	 */
	private Double tplExpMaxRows;
	
	private String config;

	private String createdBy;
	
	private String updatedBy;
	
	private String createdTime;
	
	private String updatedTime;

	/**
	 * 是否为模板所有者
	 */
	private boolean isTemplateOwner = false;

	/**
	 * 是否可直接保存
	 */
	private Integer canSave = 0;

	/**
	 * 是否可保存视图
	 */
	private Integer canSaveView = 0;

	/**
	 * 是否是公共空间下的模版
	 */
	private Integer isSpaceTpl = 0;

    private String ctgId;

	private String ctgName;

	/**
	 * 数据集id
	 */
	private String datasetId;

	/**
	 * 模版所有者
	 */
	private String tplOwner;

	/**
	 * 模版视图id
	 */
	private String viewId;

	/**
	 * 模版视图名称
	 */
	private String viewName;

	/**
	 * 模版视图类型 公共视图=public 个人视图=personal
	 */
	private String viewType;

	/**
	 * 视图创建人
	 */
	private String viewCreatedBy;

	/**
	 * 视图状态 生效 active 临时 temp
	 */
	private String viewStatus;

	/**
	 * 模版视图是否有效
	 */
	private Integer isViewValid = Enabled.YES.getId();

	/**
	 * 模版字段维度编码集合
	 */
	private String tplConfigFieldDimCodes;

	/**
	 * 模版字段指标编码集合
	 */
	private String tplConfigFieldMeasureCodes;

	/**
	 * 模版字段维度资产
	 */
	private String tplConfigFieldDimAsset;

	/**
	 * 模版字段指标资产
	 */
	private String tplConfigFieldMeasureAsset;

	/**
	 * 模版字段编码是否保存过
	 */
	private Integer isTplFieldCodeSaved = Enabled.NO.getId();

	/**
	 * 是不是公域模版
	 */
	private Integer isInPublicDomain = Enabled.NO.getId();

	/**
	 * 模版类型
	 */
	private String tplType;

	/**
	 * 是不是快照模版
	 */
	private Integer isSnapshotTpl = Enabled.NO.getId();

	// 是否被收藏
	private Integer isFav = Enabled.NO.getId();
	//收藏的目录
	private String favCtgId;

	/**
	 * 是否轮询模板资产变更
	 */
	private Integer isPollTemplateAssetChange = Enabled.NO.getId();

	/**
	 * 轮询模板资产变更时间间隔(单位秒)
	 */
	private Integer pollTemplateAssetChangeInterval = 30;

	/**
	 * 模版资产是否改变
	 */
	private Integer isAssetChange = Enabled.NO.getId();

	/**
	 * 数据集类型：实时数据集=rt 离线数据集=offline
	 */
	private String datasetType;

	/**
	 * 模板级数据集id（视图覆盖 datasetId 后仍保留，供前端对比展示）
	 */
	private String tplDatasetId;

	/**
	 * 模板级数据集名称
	 */
	private String tplDatasetName;

    public String getCtgId() {
        return ctgId;
    }

    public void setCtgId(String ctgId) {
        this.ctgId = ctgId;
    }

	public String getCtgName() {
		return ctgName;
	}

	public void setCtgName(String ctgName) {
		this.ctgName = ctgName;
	}


	public String getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(String createdBy) {
		this.createdBy = createdBy;
	}

	public String getUpdatedBy() {
		return updatedBy;
	}

	public void setUpdatedBy(String updatedBy) {
		this.updatedBy = updatedBy;
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

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getConfig() {
		return config;
	}

	public void setConfig(String config) {
		this.config = config;
	}

	public String getCreatedTime() {
		return createdTime;
	}

	public void setCreatedTime(String createdTime) {
		this.createdTime = createdTime;
	}

	public String getUpdatedTime() {
		return updatedTime;
	}

	public void setUpdatedTime(String updatedTime) {
		this.updatedTime = updatedTime;
	}

	public Double getTplExpMaxRows() {
		return tplExpMaxRows;
	}

	public void setTplExpMaxRows(Double tplExpMaxRows) {
		this.tplExpMaxRows = tplExpMaxRows;
	}

	public boolean isTemplateOwner() {
		return isTemplateOwner;
	}

	public void setTemplateOwner(boolean templateOwner) {
		isTemplateOwner = templateOwner;
	}

	@Override
	public JSONObject toJSON() {
		return BIUtil.toJSONObject(this);
	}

	public Integer getCanSave() {
		return canSave;
	}

	public void setCanSave(Integer canSave) {
		this.canSave = canSave;
	}

	public Integer getIsSpaceTpl() {
		return isSpaceTpl;
	}

	public void setIsSpaceTpl(Integer isSpaceTpl) {
		this.isSpaceTpl = isSpaceTpl;
	}

	public String getDatasetId() {
		return datasetId;
	}

	public void setDatasetId(String datasetId) {
		this.datasetId = datasetId;
	}

	public String getTplOwner() {
		return tplOwner;
	}

	public void setTplOwner(String tplOwner) {
		this.tplOwner = tplOwner;
	}

	public String getViewId() {
		return viewId;
	}

	public void setViewId(String viewId) {
		this.viewId = viewId;
	}

	public String getViewName() {
		return viewName;
	}

	public void setViewName(String viewName) {
		this.viewName = viewName;
	}

	public Integer getIsViewValid() {
		return isViewValid;
	}

	public void setIsViewValid(Integer isViewValid) {
		this.isViewValid = isViewValid;
	}

	public String getTplConfigFieldDimCodes() {
		return tplConfigFieldDimCodes;
	}

	public void setTplConfigFieldDimCodes(String tplConfigFieldDimCodes) {
		this.tplConfigFieldDimCodes = tplConfigFieldDimCodes;
	}

	public String getTplConfigFieldMeasureCodes() {
		return tplConfigFieldMeasureCodes;
	}

	public void setTplConfigFieldMeasureCodes(String tplConfigFieldMeasureCodes) {
		this.tplConfigFieldMeasureCodes = tplConfigFieldMeasureCodes;
	}

	public Integer getIsTplFieldCodeSaved() {
		return isTplFieldCodeSaved;
	}

	public void setIsTplFieldCodeSaved(Integer isTplFieldCodeSaved) {
		this.isTplFieldCodeSaved = isTplFieldCodeSaved;
	}

	public String getViewType() {
		return viewType;
	}

	public void setViewType(String viewType) {
		this.viewType = viewType;
	}

	public String getTplConfigFieldDimAsset() {
		return tplConfigFieldDimAsset;
	}

	public void setTplConfigFieldDimAsset(String tplConfigFieldDimAsset) {
		this.tplConfigFieldDimAsset = tplConfigFieldDimAsset;
	}

	public String getTplConfigFieldMeasureAsset() {
		return tplConfigFieldMeasureAsset;
	}

	public void setTplConfigFieldMeasureAsset(String tplConfigFieldMeasureAsset) {
		this.tplConfigFieldMeasureAsset = tplConfigFieldMeasureAsset;
	}

	public Integer getCanSaveView() {
		return canSaveView;
	}

	public void setCanSaveView(Integer canSaveView) {
		this.canSaveView = canSaveView;
	}

	public Integer getIsInPublicDomain() {
		return isInPublicDomain;
	}

	public void setIsInPublicDomain(Integer isInPublicDomain) {
		this.isInPublicDomain = isInPublicDomain;
	}

	public Integer getIsFav() {
		return isFav;
	}

	public void setIsFav(Integer isFav) {
		this.isFav = isFav;
	}

	public String getFavCtgId() {
		return favCtgId;
	}

	public void setFavCtgId(String favCtgId) {
		this.favCtgId = favCtgId;
	}

	public Integer getIsSnapshotTpl() {
		return isSnapshotTpl;
	}

	public void setIsSnapshotTpl(Integer isSnapshotTpl) {
		this.isSnapshotTpl = isSnapshotTpl;
	}

	public String getTplType() {
		return tplType;
	}

	public void setTplType(String tplType) {
		this.tplType = tplType;
	}

	public Integer getIsPollTemplateAssetChange() {
		return isPollTemplateAssetChange;
	}

	public void setIsPollTemplateAssetChange(Integer isPollTemplateAssetChange) {
		this.isPollTemplateAssetChange = isPollTemplateAssetChange;
	}

	public Integer getPollTemplateAssetChangeInterval() {
		return pollTemplateAssetChangeInterval;
	}

	public void setPollTemplateAssetChangeInterval(Integer pollTemplateAssetChangeInterval) {
		this.pollTemplateAssetChangeInterval = pollTemplateAssetChangeInterval;
	}

	public Integer getIsAssetChange() {
		return isAssetChange;
	}

	public void setIsAssetChange(Integer isAssetChange) {
		this.isAssetChange = isAssetChange;
	}

	public String getViewStatus() {
		return viewStatus;
	}

	public void setViewStatus(String viewStatus) {
		this.viewStatus = viewStatus;
	}

	public String getDatasetType() {
		return datasetType;
	}

	public void setDatasetType(String datasetType) {
		this.datasetType = datasetType;
	}

	public String getTplDatasetId() {
		return tplDatasetId;
	}

	public void setTplDatasetId(String tplDatasetId) {
		this.tplDatasetId = tplDatasetId;
	}

	public String getTplDatasetName() {
		return tplDatasetName;
	}

	public void setTplDatasetName(String tplDatasetName) {
		this.tplDatasetName = tplDatasetName;
	}

	public String getViewCreatedBy() {
		return viewCreatedBy;
	}

	public void setViewCreatedBy(String viewCreatedBy) {
		this.viewCreatedBy = viewCreatedBy;
	}
}
