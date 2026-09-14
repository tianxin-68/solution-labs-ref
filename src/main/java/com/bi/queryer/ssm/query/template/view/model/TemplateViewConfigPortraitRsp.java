package com.bi.queryer.ssm.query.template.view.model;

import java.util.ArrayList;
import java.util.List;

/**
 * 视图配置画像：基本信息 + 数据结构 + 默认参数
 */
public class TemplateViewConfigPortraitRsp {

    private TemplateViewConfigPortraitBasic basic;

    private List<TemplateViewConfigPortraitMetadataRow> metadata = new ArrayList<>();

    private TemplateViewConfigPortraitDefaults defaults;

    public TemplateViewConfigPortraitBasic getBasic() {
        return basic;
    }

    public void setBasic(TemplateViewConfigPortraitBasic basic) {
        this.basic = basic;
    }

    public List<TemplateViewConfigPortraitMetadataRow> getMetadata() {
        return metadata;
    }

    public void setMetadata(List<TemplateViewConfigPortraitMetadataRow> metadata) {
        this.metadata = metadata;
    }

    public TemplateViewConfigPortraitDefaults getDefaults() {
        return defaults;
    }

    public void setDefaults(TemplateViewConfigPortraitDefaults defaults) {
        this.defaults = defaults;
    }
}
