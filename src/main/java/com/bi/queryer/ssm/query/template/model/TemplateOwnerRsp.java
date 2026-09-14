package com.bi.queryer.ssm.query.template.model;

import com.bi.queryer.sys.enums.Enabled;
import com.bi.queryer.sys.user.vo.UserRsp;

import java.util.ArrayList;
import java.util.List;

/**
 * 模版负责人返回实体
 */
public class TemplateOwnerRsp {

    /**
     * 模版负责人
     */
    private List<UserRsp> tplOwnerList = new ArrayList<>();

    /**
     * 是否是公共空间下的模版
     */
    private Integer isSpaceTpl = Enabled.NO.getId();

    /**
     * 共享空间名称
     */
    private String spaceName;

    /**
     * 模版分类路径
     */
    private String templateCtgPath;

    /**
     * 共享空间负责人
     */
    private List<UserRsp> spaceOwnerList = new ArrayList<>();

    public List<UserRsp> getTplOwnerList() {
        return tplOwnerList;
    }

    public void setTplOwnerList(List<UserRsp> tplOwnerList) {
        this.tplOwnerList = tplOwnerList;
    }

    public Integer getIsSpaceTpl() {
        return isSpaceTpl;
    }

    public void setIsSpaceTpl(Integer isSpaceTpl) {
        this.isSpaceTpl = isSpaceTpl;
    }

    public String getSpaceName() {
        return spaceName;
    }

    public void setSpaceName(String spaceName) {
        this.spaceName = spaceName;
    }

    public List<UserRsp> getSpaceOwnerList() {
        return spaceOwnerList;
    }

    public void setSpaceOwnerList(List<UserRsp> spaceOwnerList) {
        this.spaceOwnerList = spaceOwnerList;
    }

    public String getTemplateCtgPath() {
        return templateCtgPath;
    }

    public void setTemplateCtgPath(String templateCtgPath) {
        this.templateCtgPath = templateCtgPath;
    }
}
