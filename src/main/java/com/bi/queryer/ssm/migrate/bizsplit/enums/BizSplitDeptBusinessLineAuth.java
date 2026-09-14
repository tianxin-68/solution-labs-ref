package com.bi.queryer.ssm.migrate.bizsplit.enums;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 组织与默认业务线行级权限映射。
 *
 * 用于业务线拆分后，为各业务中心用户初始化 ssm_row / ssm_dim_bizline 权限。
 * 每个用户仅命中一个映射项，业务线取该项配置，不做跨项合并。
 *
 * @see com.bi.queryer.ssm.migrate.bizsplit.service.BizSplitDataAuthInitService
 */
public enum BizSplitDeptBusinessLineAuth {

    /** 保养油液业务中心 → 保养油液 */
    OIL("701485", "保养油液业务中心", "保养油液"),

    /** 保养配件业务中心 → 保养配件、改装升级与车品超市 */
    PARTS("701490", "保养配件业务中心", "保养配件", "改装升级与车品超市"),

    /** 汽车改装业务中心 → 改装升级与车品超市 */
    CAR_MODIFY("100290", "汽车改装业务中心", "改装升级与车品超市"),

    /** 电子改装业务中心 → 电子改装 */
    ELECTRONIC_MODIFY("701484", "电子改装业务中心", "电子改装"),

    /** 二轮车与轮毂业务组 → 电瓶车、二轮车、轮毂 */
    TWO_WHEEL("701424", "二轮车与轮毂业务组", "电瓶车", "二轮车", "轮毂");

    /** 组织 id */
    private final String deptId;

    /** 组织名称，用于结果展示 */
    private final String deptName;

    /** 该组织下用户默认授权的业务线列表 */
    private final List<String> businessLines;

    BizSplitDeptBusinessLineAuth(String deptId, String deptName, String... businessLines) {
        this.deptId = deptId;
        this.deptName = deptName;
        this.businessLines = Collections.unmodifiableList(Arrays.asList(businessLines));
    }

    public String getDeptId() {
        return deptId;
    }

    public String getDeptName() {
        return deptName;
    }

    public List<String> getBusinessLines() {
        return businessLines;
    }
}
