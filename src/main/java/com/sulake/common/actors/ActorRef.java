/**
 * Copyright 2012 Sulake Oy.
 */
package com.sulake.common.actors;

import com.sulake.common.actors.spi.SimpleActorsRuntime;

/**
 * A reference to a particular {@link Actor} instance.
 *
 * @author dmitrym
 */
public interface ActorRef {

    /**
     * Sends message to referenced actor instance.
     *
     * @param message message to send
     * @throws TooManyPendingMessagesException if there are too many requests
     *                                         pending delivery and actors runtime has been configured to throw
     *                                         exceptions
     * @throws ActorRefReleasedException       if this actor reference is already released
     * @see Actor#processMessage(Object)
     * @see SimpleActorsRuntime#setMaxMessages(int)
     * @see #release()
     * @see #releaseIfIdle()
     */
    void sendMessage(Object message) throws TooManyPendingMessagesException, ActorRefReleasedException;

    /**
     * Releases this instance. Pending requests will be delivered to actor
     * instance, further calls to {@link #sendMessage(Object)} will throw
     * {@link ActorRefReleasedException}.
     */
    void release();

    /**
     * Releases this instance, but only if there are no pending requests. If
     * succeeded, further calls to {@link #sendMessage(Object)} will throw
     * {@link ActorRefReleasedException}.
     *
     * @return {@code true} if released; {@code false} if there are requests pending
     */
    boolean releaseIfIdle();

}
