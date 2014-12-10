/**
 * Copyright 2012 Sulake Oy.
 */
package com.sulake.common.actors.spi;

import com.sulake.common.actors.Actor;
import com.sulake.common.actors.ActorRef;
import com.sulake.common.actors.TooManyPendingMessagesException;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Abstract implementation of {@link ActorsRuntime}, creates
 * {@link SimpleActorSupport}s, deals with pending message tracking.
 *
 * @author dmitrym
 */
@ManagedResource
public abstract class SimpleActorsRuntime implements ActorsRuntime {

    private static final long DEFAULT_WAIT_TIME_IN_MILLIS = TimeUnit.SECONDS.toMillis(30);

    protected int maxMessages;

    protected Semaphore messagePermits;

    protected volatile boolean closed = true;

    protected long shutdownWaitTimeInMillis = DEFAULT_WAIT_TIME_IN_MILLIS;

    public SimpleActorsRuntime(int maxMessages) {
        setMaxMessages(maxMessages);
    }

    /**
     * Constructor for Spring.
     */
    public SimpleActorsRuntime() {
    }

    @Required
    public void setMaxMessages(int maxMessages) {
        this.maxMessages = maxMessages;
        messagePermits = new Semaphore(maxMessages);
    }

    public void setShutdownWaitTimeInMillis(long setShutdownWaitTimeInMillis) {
        shutdownWaitTimeInMillis = setShutdownWaitTimeInMillis;
    }

    @ManagedAttribute
    public int getPendingMessages() {
        return maxMessages - messagePermits.availablePermits();
    }

    public void acquirePermitForMessage() {
        if (closed) {
            throw new IllegalStateException("Actor runtime is closing");
        }
        if (!messagePermits.tryAcquire()) {
            // TODO implement policy
            throw new TooManyPendingMessagesException();
        }
    }

    public void releasePermitForMessage() {
        messagePermits.release();
    }

    @Override
    public ActorRef getSupportFor(Actor actor) {
        if (closed) {
            throw new IllegalStateException("Actor runtime is closing");
        }
        return new SimpleActorSupport(this, actor);
    }

    @PostConstruct
    public void start() {
        if (!closed) {
            throw new IllegalStateException("Runtime already started");
        }

        if (messagePermits == null) {
            throw new IllegalStateException("maxMessages has not been set");
        }

        closed = false;
    }

    @PreDestroy
    public void stop() throws InterruptedException {
        if (closed) {
            return;
        }
        closed = true;
        messagePermits.tryAcquire(maxMessages, shutdownWaitTimeInMillis, TimeUnit.MILLISECONDS);
    }

    public void forcedStop() {
        closed = true;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("SimpleActorsRuntime{");
        sb.append("maxMessages=").append(maxMessages);
        sb.append(", messagePermits=").append(messagePermits);
        sb.append(", closed=").append(closed);
        sb.append(", shutdownWaitTimeInMillis=").append(shutdownWaitTimeInMillis);
        sb.append('}');
        return sb.toString();
    }
}
