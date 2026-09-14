package com.bi.queryer.ssm.portal.template.vo;

import com.bi.queryer.ssm.query.template.model.TemplateViewMapping;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * @Auther: contributor
 * @Date: 2024/6/17 19:09
 * @Description:
 */

@Data
public class WidgetNodeVO {
    /**
     * 组件ID，前端构造
     */
    private String widgetId;
    /**
     * 组件类型编码
     */
    private String widgetTypeCode;
    /**
     * 组件标题
     */
    private String widgetTitle;
    /**
     * 组件描述
     */
    private String widgetDesc;
    /**
     * 组件基本设置(长宽，位置等设置)
     */
    private String widgetSettings;
    /**
     * 组件配置, 格式和查询模板的配置一样
     */
    private String widgetOptions;
    /**
     * 查询模板id
     */
    private String queryTplId;
    /**
     * 草稿版本的查询模板id
     */
    private String localQueryTplId;

    private List<TemplateViewMapping> queryTplViewIdMappingList = new ArrayList<>();

    /**
     * 子组件
     */
    private List<WidgetNodeVO> children;
}
