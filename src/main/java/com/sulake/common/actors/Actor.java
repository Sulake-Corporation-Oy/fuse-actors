/**
 * Copyright 2012 Sulake Oy.
 */
package com.sulake.common.actors;

/**
 * Interface for actor implementation.
 *
 * @author dmitrym
 */
public interface Actor {

    /**
     * Processes message. Actors runtime guarantees that for each instance this
     * method can be called only from one thread at a time.
     *
     * @return {@code true} if message was processed; {@code false} if
     * message type is unsupported
     * @see ActorRef#sendMessage(Object)
     */
    boolean processMessage(Object message);
}
