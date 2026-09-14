package com.bi.queryer.ssm.portal.template.entity;

import lombok.*;

import java.sql.Timestamp;

/**
 * @Auther: contributor
 * @Date: 2024/6/18 09:42
 * @Description:
 */

@Setter
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AnalysisTplCfgLocalLockEntity {

    private String analysisTplId;

    private String userName;

    private Timestamp createdTime;
}
