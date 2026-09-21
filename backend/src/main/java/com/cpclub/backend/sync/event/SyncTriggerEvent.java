package com.cpclub.backend.sync.event;

import com.cpclub.backend.sync.entity.SyncJob;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class SyncTriggerEvent extends ApplicationEvent {
    private final SyncJob job;

    public SyncTriggerEvent(Object source, SyncJob job) {
        super(source);
        this.job = job;
    }
}
