package com.bi.queryer.ssm.engine.pivot.meta;

import com.bi.queryer.sys.enums.Enabled;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author: contributor
 * @CreateTime: 2024-05-21  10:13
 * @Description: 列总计的元信息
 */
@Data
public class ColTotalMeta {

    private Integer isActive = Enabled.NO.getId();

    //列维度信息
    private List<String> colDimCodeList= new ArrayList<>();

}
