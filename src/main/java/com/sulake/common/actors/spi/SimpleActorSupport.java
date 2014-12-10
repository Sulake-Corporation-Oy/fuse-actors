/**
 * Copyright 2012 Sulake Oy.
 */
package com.sulake.common.actors.spi;

import com.sulake.common.actors.Actor;
import com.sulake.common.actors.ActorContext;
import com.sulake.common.actors.ActorRef;
import com.sulake.common.actors.ActorRefReleasedException;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.Semaphore;

/**
 * Simple implementation of {ActorSupport} using unidirectional linked
 * list for pending requests and {@link Semaphore} to guard access.
 *
 * @author dmitrym
 */
public final class SimpleActorSupport implements ActorRef, Runnable {

    private static final class Node {
        private final Object message;
        private Node next;

        private Node(Object message) {
            this.message = message;
        }
    }

    private static final Logger logger = Logger.getLogger(SimpleActorSupport.class);

    private final SimpleActorsRuntime runtime;

    private final Actor actor;

    private volatile boolean released;

    private volatile Node head;

    @Nullable
    private volatile Node tail;

    private final Semaphore guardSemaphore = new Semaphore(1);

    public SimpleActorSupport(SimpleActorsRuntime runtime, Actor actor) {
        this.runtime = runtime;
        this.actor = actor;
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public void sendMessage(Object message) {
        runtime.acquirePermitForMessage();

        guardSemaphore.acquireUninterruptibly();
        try {
            if (released) {
                throw new ActorRefReleasedException();
            }
            if (tail == null) {
                head = tail = new Node(message);
            }
            else {
                tail = tail.next = new Node(message);
                return;
            }
        }
        finally {
            guardSemaphore.release();
        }

        runtime.scheduleHandleNextMessageCall(this);
    }

    @SuppressWarnings("ReturnInsideFinallyBlock")
    @Override
    public void run() {
        ActorContext.setCurrentContext(this, actor);

        try {
            if (!actor.processMessage(head.message)) {
                logger.error(describeActorSafely() + " ignored " + head.message);
            }
        }
        catch (RuntimeException ex) {
            logger.error(describeActorSafely() + ": unexpected exception processing " + head.message, ex);
        }
        finally {
            ActorContext.removeCurrentContext();

            runtime.releasePermitForMessage();

            guardSemaphore.acquireUninterruptibly();
            try {
                head = head.next;
                if (head == null) {
                    tail = null;
                    return;
                }
            }
            finally {
                guardSemaphore.release();
            }

            runtime.scheduleHandleNextMessageCall(this);
        }
    }

    @Override
    public void release() {
        guardSemaphore.acquireUninterruptibly();
        try {
            if (released) {
                return;
            }
            released = true;
        }
        finally {
            guardSemaphore.release();
        }
    }

    @Override
    public boolean releaseIfIdle() {
        boolean fromProcessingMessage = equals(ActorContext.getCurrentActorRef());
        guardSemaphore.acquireUninterruptibly();
        try {
            if (released) {
                return true;
            }
            if (fromProcessingMessage) {
                if (!head.equals(tail)) {
                    return false;
                }
            }
            else {
                if (head != null) {
                    return false;
                }
            }
            released = true;
            return true;
        }
        finally {
            guardSemaphore.release();
        }
    }

    public String describeActorSafely() {
        try {
            return actor.toString();
        }
        catch (RuntimeException ex) {
            return String.format("%s@%x (toString() caused %s)", actor.getClass().getCanonicalName(),
                    System.identityHashCode(actor), ex);
        }
    }

}
