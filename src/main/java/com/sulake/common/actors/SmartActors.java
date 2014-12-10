/**
 * Copyright 2012 Sulake Oy.
 */
package com.sulake.common.actors;

import com.sulake.common.actors.spi.ActorsRuntime;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

/**
 * "Smart" actors:
 * <ul>
 * <li>Individual actors are addressed by {@code actorId}.
 * <li>Actor instances are created on on first message to for
 * {@code actorId} (via {@link #sendMessage(Object, Object)}.
 * <li>Actor instances are removed from the system ("expired") based on result
 * returned by {@link SmartActor#canBeRemoved()}
 * <li>There is support for periodic ping message ({@link #setPingPeriodInMillis(long)}.
 * </ul>
 *
 * @param <T> type of {@code actorId}
 * @author dmitrym
 * @see SmartActor
 */
@ManagedResource
public class SmartActors<T> {

    /**
     * System message - ping
     */
    public static final Object PING_MESSAGE = "<PING>";

    /**
     * System message - signals actor to destroy self
     */
    public static final Object DESTROY_MESSAGE = "<DIE!!!!>";

    public static final long SHUTDOWN_WAIT_TIME = TimeUnit.SECONDS.toMillis(30);

    public static final int PREFERRED_PING_BATCH_SIZE = 100;

    public static final int MIN_PING_BATCH_PERIOD_IN_MILLIS = 1000;

    private static final Logger logger = Logger.getLogger(SmartActors.class);

    private ActorsRuntime actorsRuntime;

    private ObjectFactory<SmartActor<T>> actorsFactory;

    private int preferredPingBatchSize = PREFERRED_PING_BATCH_SIZE;

    private long pingPeriodInMillis;

    private final ConcurrentMap<T, ActorRef> actorRefById = new ConcurrentHashMap<T, ActorRef>();

    private Iterator<ActorRef> pingIterator = actorRefById.values().iterator();

    private long pingBatchPeriod;

    private int pingBatchSize;

    private volatile boolean stopped;

    @Required
    public void setActorsRuntime(ActorsRuntime actorsRuntime) {
        this.actorsRuntime = actorsRuntime;
    }

    @Required
    public void setActorsFactory(ObjectFactory<SmartActor<T>> actorsFactory) {
        this.actorsFactory = actorsFactory;
    }

    public void setPreferredPingBatchSize(int preferredPingBatchSize) {
        this.preferredPingBatchSize = preferredPingBatchSize;
    }

    @Required
    public void setPingPeriodInMillis(long pingPeriodInMillis) {
        this.pingPeriodInMillis = pingPeriodInMillis;
    }

    @PostConstruct
    public synchronized void start() {
        Thread pingThread = new Thread(new Runnable() {
            @Override
            public void run() {
                pingLoop();
            }
        }, toString());
        pingThread.setDaemon(true);
        pingThread.start();
    }

    private void pingLoop() {
        while (!stopped) {
            calculateNextPingBatch();

            sendPingToNextBatch();

            try {
                Thread.sleep(pingBatchPeriod);
            }
            catch (InterruptedException ex) {
                logger.error("Unexpected interruption, ignoring", ex);
            }
        }

    }

    void calculateNextPingBatch() {
        int size = actorRefById.size();
        if (size == 0) {
            pingBatchPeriod = pingPeriodInMillis;
            pingBatchSize = 0;
        }
        else {
            pingBatchSize = Math.min(preferredPingBatchSize, size);
            pingBatchPeriod = pingPeriodInMillis * pingBatchSize / size;
            if (pingBatchPeriod < MIN_PING_BATCH_PERIOD_IN_MILLIS) {
                pingBatchPeriod = MIN_PING_BATCH_PERIOD_IN_MILLIS;
                pingBatchSize = (int) (pingBatchPeriod * size / pingPeriodInMillis);
            }
            else if (pingBatchPeriod > pingPeriodInMillis) {
                pingBatchPeriod = pingPeriodInMillis;
            }
        }
    }

    void sendPingToNextBatch() {
        for (int i = 0; i < pingBatchSize; i++) {
            if (!pingIterator.hasNext()) {
                pingIterator = actorRefById.values().iterator();
                if (!pingIterator.hasNext()) {
                    return;
                }
            }

            try {
                pingIterator.next().sendMessage(PING_MESSAGE);
            }
            catch (ActorRefReleasedException ex) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Got " + ex + " when sending ping message, ignoring");
                }
            }
            catch (RuntimeException ex) {
                logger.error("Unexpected exception when sending ping message, ignoring", ex);
            }
        }
    }

    @PreDestroy
    public synchronized void stop() throws InterruptedException {
        stopped = true;

        for (ActorRef actorRef : actorRefById.values()) {
            try {
                actorRef.sendMessage(DESTROY_MESSAGE);
                actorRef.release();
            }
            catch (ActorRefReleasedException ignored) {
            }
        }

        if (!actorRefById.isEmpty()) {
            wait(SHUTDOWN_WAIT_TIME);
        }
    }

    @ManagedAttribute
    public long getLastPingBatchPeriod() {
        return pingBatchPeriod;
    }

    @ManagedAttribute
    public int getLastPingBatchSize() {
        return pingBatchSize;
    }

    @ManagedAttribute
    public int getEstimatedSize() {
        return actorRefById.size();
    }

    boolean tryToRemoveActor(T actorId, ActorRef actorRef) {
        if (actorRef.releaseIfIdle()) {
            actorRefById.remove(actorId, actorRef);
            return true;
        }
        return false;
    }

    void removeActor(T actorId, ActorRef actorRef) {
        actorRefById.remove(actorId, actorRef);
        if (actorRefById.isEmpty()) {
            synchronized (this) {
                notify();
            }
        }
    }

    /**
     * Sends {@code message} to actor addressed by given
     * {@code actorId}. Concrete actor instance will be created on first
     * message and removed from the system ("expired") some time later.
     *
     * @see SmartActor#canBeRemoved()
     */
    public void sendMessage(T actorId, Object message) {
        boolean sent = false;
        do {
            if (stopped) {
                throw new IllegalStateException("SmartActors are stopped");
            }

            ActorRef actorRef = getActorRefFor(actorId);

            try {
                actorRef.sendMessage(message);
                sent = true;
            }
            catch (ActorRefReleasedException ignored) {
                // may happen in rare concurrency condition
                // remove "broken" actorRef, recreate on next cycle
                actorRefById.remove(actorId, actorRef);
            }
        }
        while (!sent); // Hello, Jonas! :-) 
    }

    private ActorRef getActorRefFor(T actorId) {
        ActorRef actorRef = actorRefById.get(actorId);
        if (actorRef != null) {
            return actorRef;
        }
        SmartActor<T> actor = actorsFactory.getObject();
        actor.bind(actorId, this);
        ActorRef actorRuntime = actorsRuntime.getSupportFor(actor);
        ActorRef concurrentActorRef = actorRefById.putIfAbsent(actorId, actorRuntime);
        return concurrentActorRef != null ? concurrentActorRef : actorRuntime;
    }

}
