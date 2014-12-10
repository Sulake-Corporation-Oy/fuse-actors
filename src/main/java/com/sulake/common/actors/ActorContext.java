/**
 * Copyright 2012 Sulake Oy.
 */
package com.sulake.common.actors;

/**
 * Actor context, available only from {@link Actor#processMessage(Object)}.
 *
 * @author dmitrym
 */
public class ActorContext {

    private static final ThreadLocal<ActorContext> currentThreadContext = new ThreadLocal<ActorContext>();

    /**
     * Returns {@link ActorRef} to an actor which processes message in current thread.
     *
     * @throws IllegalStateException if called not from {@link Actor#processMessage(Object)}
     */
    public static ActorRef getCurrentActorRef() {
        return getCurrentContext().actorRef;
    }

    /**
     * Returns an actor which processes message in current thread.
     *
     * @throws IllegalStateException if called not from {@link Actor#processMessage(Object)}
     */
    @SuppressWarnings("unchecked")
    public static <T> T getCurrentActor() {
        return (T) getCurrentContext().actor;
    }

    private static ActorContext getCurrentContext() {
        ActorContext context = currentThreadContext.get();
        if (context == null) {
            throw new IllegalStateException("ActorContext.getCurrentContext() called not from actor handling method");
        }
        return context;
    }

    public static void setCurrentContext(ActorRef actorRef, Actor actor) {
        currentThreadContext.set(new ActorContext(actorRef, actor));
    }

    public static void removeCurrentContext() {
        currentThreadContext.remove();
    }

    private final ActorRef actorRef;

    private final Actor actor;

    private ActorContext(ActorRef actorRef, Actor actor) {
        this.actorRef = actorRef;
        this.actor = actor;
    }
}
