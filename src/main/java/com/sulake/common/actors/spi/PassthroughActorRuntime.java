/**
 * Copyright 2012 Sulake Oy.
 */
package com.sulake.common.actors.spi;

import com.sulake.common.actors.*;

/**
 * Execute-in-calling-thread implementation of {@link ActorsRuntime}, suitable
 * only for unit testing.
 *
 * @author dmitrym
 */
public class PassthroughActorRuntime implements ActorsRuntime {

    private static class PassthroughActorSupport implements ActorRef {

        private final Actor actor;

        private boolean released;

        private PassthroughActorSupport(Actor actor) {
            this.actor = actor;
        }

        @Override
        public synchronized void release() {
            if (released) {
                return;
            }
            released = true;
        }

        @Override
        public boolean releaseIfIdle() {
            release();
            return true;
        }

        @Override
        public synchronized void sendMessage(Object message) throws TooManyPendingMessagesException,
                ActorRefReleasedException {
            if (released) {
                throw new ActorRefReleasedException();
            }

            ActorContext.setCurrentContext(this, actor);
            try {
                actor.processMessage(message);
            }
            finally {
                ActorContext.removeCurrentContext();
            }
        }

    }

    @Override
    public ActorRef getSupportFor(Actor actor) {
        return new PassthroughActorSupport(actor);
    }

    @Override
    public void scheduleHandleNextMessageCall(Runnable actorSupport) {
        throw new UnsupportedOperationException();
    }

}
