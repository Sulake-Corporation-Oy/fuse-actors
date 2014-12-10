/**
 * Copyright 2012 Sulake Oy.
 */
package com.sulake.common.actors.spi;

import org.springframework.beans.factory.annotation.Required;
import org.springframework.jmx.export.annotation.ManagedResource;

import java.util.concurrent.Executor;

/**
 * Reference implementation of {@link ActorsRuntime} using {@link Executor}.
 *
 * @author dmitrym
 */
@ManagedResource
public class ExecutorBasedActorsRuntime extends SimpleActorsRuntime {

    private Executor executor;

    public ExecutorBasedActorsRuntime(int maxMessages, Executor executor) {
        super(maxMessages);
        this.executor = executor;
    }

    /**
     * Constructor for Spring.
     */
    public ExecutorBasedActorsRuntime() {
    }

    @Required
    public void setExecutorService(Executor executor) {
        this.executor = executor;
    }

    @Override
    public void scheduleHandleNextMessageCall(Runnable target) {
        executor.execute(target);
    }

}
