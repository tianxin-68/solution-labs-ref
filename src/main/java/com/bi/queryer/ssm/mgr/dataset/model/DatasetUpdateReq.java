package com.bi.queryer.ssm.mgr.dataset.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: contributor
 * @CreateTime: 2024-04-15  15:20
 * @Description: 数据集修改接口
 */
@Data
public class DatasetUpdateReq extends DatasetAddReq{

    private String datasetId;

}
