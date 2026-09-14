package com.bi.queryer.ssm.engine.pivot.meta;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author: contributor
 * @CreateTime: 2024-05-21  09:57
 * @Description: 列小计的元信息
 */
@Data
public class ColSubTotalMeta {

    /**
     * 小计的行维度
     */
    private String rowDimCode;

    /**
     * 子维度信息
     */
    private List<String> childDimCodeList = new ArrayList<>();

    /**
     * 父维度信息
     */
   private  List<String> parentDimCodeList = new ArrayList<>();

}
