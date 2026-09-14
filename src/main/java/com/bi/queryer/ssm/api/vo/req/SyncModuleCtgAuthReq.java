package com.bi.queryer.ssm.api.vo.req;

import com.bi.queryer.sys.enums.Enabled;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class SyncModuleCtgAuthReq {

    /**
     * 模块id
     */
    private String moduleId;

    /**
     * 是否继承
     */
    private Integer isInherited = Enabled.YES.getId();

    /**
     * 授权用户
     */
    private List<String> authUserList = new ArrayList<>();

    /**
     * 授权部门
     */
    private List<String> authDeptList = new ArrayList<>();

    /**
     * 删除的授权id
     */
    private List<String> deleteDataAuthIdList = new ArrayList<>();

}
