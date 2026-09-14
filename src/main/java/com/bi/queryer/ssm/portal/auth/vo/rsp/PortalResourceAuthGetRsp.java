package com.bi.queryer.ssm.portal.auth.vo.rsp;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 获取文件夹/看板授权响应
 */
@Data
public class PortalResourceAuthGetRsp {

    private String resId;

    private String resType;

    // ---------- 权限继承 ----------

    /** 是否开启继承 1=开启 0=关闭 */
    private Integer inheritEnabled = 1;

    private String parentResId;

    private String parentResType;

    /** 父级资源名称（用于展示"继承自 XXX"） */
    private String parentResName;

    /** 父级有效用户数（"66人"） */
    private Integer parentAuthUserCount;

    // ---------- 本层直接授权 ----------

    private List<String> userNameList = new ArrayList<>();

    private List<String> deptIdList = new ArrayList<>();

    /** 本资源有效用户总数（含继承），用于「查看有权限的用户清单（N人）」 */
    private Integer effectiveUserCount;
}
