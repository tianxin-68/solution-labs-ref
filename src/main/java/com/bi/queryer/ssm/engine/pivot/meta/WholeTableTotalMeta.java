package com.bi.queryer.ssm.engine.pivot.meta;

import com.bi.queryer.ssm.engine.result.ResultDataSetColumn;
import lombok.Data;

/**
 * @Author: contributor
 * @CreateTime: 2024-05-24  17:56
 * @Description: 整表总计元数据
 */
@Data
public class WholeTableTotalMeta {

    private ResultDataSetColumn column = new ResultDataSetColumn();

    private String value;

}
