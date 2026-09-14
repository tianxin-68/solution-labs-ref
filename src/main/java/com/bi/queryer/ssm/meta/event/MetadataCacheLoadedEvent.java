package com.bi.queryer.ssm.meta.event;

import org.springframework.context.ApplicationEvent;

/**
 * @Author contributor
 * @Date 18:20 2025/12/31
 * @Description TODO
 **/
public class MetadataCacheLoadedEvent extends ApplicationEvent {
    public MetadataCacheLoadedEvent(Object source) {
        super(source);
    }
}
