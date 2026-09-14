package com.bi.queryer.ssm.query.template.view.event;

import org.springframework.context.ApplicationEvent;

/**
 * 视图 UPDATE 完成后发布的事件，携带 viewId 供下游消费
 */
public class TemplateViewUpdatedEvent extends ApplicationEvent {

    private final String viewId;

    public TemplateViewUpdatedEvent(Object source, String viewId) {
        super(source);
        this.viewId = viewId;
    }

    public String getViewId() {
        return viewId;
    }
}
