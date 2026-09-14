package com.bi.queryer.ssm.query.template.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author: contributor
 * @CreateTime: 2024-07-02  17:47
 * @Description: 获取模板更新时间
 */
@Data
public class TemplateDataUpdateTimeReq {

    private List<String> tplIdList = new ArrayList<>();

}
