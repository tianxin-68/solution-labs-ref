package com.bi.queryer.ssm.api.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserCtgAuthEntity {

    private String ctgId;

    private String ctgName;

    private Integer isModule;

    private String userName;

    private String userRealName;

    private String deptName;

    private String authSource;

    private String authSourceDetail;

    private String authStartDate;

    private String authEndDate;

}
