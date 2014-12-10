/**
 * Copyright 2012 Sulake Oy.
 */
package com.sulake.common.actors;

/**
 * Base class for "smart" {@link Actor}s. Life-cycle methods:
 * <ul>
 * <li>{@link #initActor(Object)}
 * <li>{@link #processPing()}
 * <li>{@link #canBeRemoved()}
 * <li>{@link #preDestroy()}
 * <li>{@link #destroyActor()}
 * </ul>
 *
 * @param <T> type of {@code actorId}
 * @author dmitrym
 * @see SmartActors
 */
public abstract class SmartActor<T> implements Actor {

    private T actorId;

    private SmartActors<T> parentService;

    private boolean initialized;

    void bind(T actorId, SmartActors<T> parentService) {
        this.actorId = actorId;
        this.parentService = parentService;
    }

    public T getActorId() {
        return actorId;
    }

    /**
     * Handles system requests.
     *
     * @return {@code true} if {@code message} was system message
     */
    @Override
    public boolean processMessage(Object message) {
        if (!initialized) {
            initialized = true;
            initActor(actorId);
            return false;
        }

        if (message.equals(SmartActors.PING_MESSAGE)) {
            processPing();
            return true;
        }

        if (message.equals(SmartActors.DESTROY_MESSAGE)) {
            processDestroy();
            return true;
        }

        return false;
    }

    /**
     * Allows actor to perform initialization before handling business message.
     */
    protected void initActor(T actorId) {
    }

    /**
     * Called on periodic ping message. Desired period is set via
     * {@link SmartActors#setPingPeriodInMillis(long)}, but system doesn't
     * guarantee exact intervals.
     */
    protected void processPing() {
        if (!canBeRemoved()) {
            return;
        }

        tryToRemoveSelf();
    }

    /**
     * Checks if this actor has "expired" and can be removed from the system.
     *
     * @return {@code true} if this actor has expired and can be removed
     * from the system
     */
    protected abstract boolean canBeRemoved();

    /**
     * Attempts to remove this actor instance from the system.
     */
    protected final void tryToRemoveSelf() {
        preDestroy();
        if (parentService.tryToRemoveActor(actorId, ActorContext.getCurrentActorRef())) {
            destroyActor();
        }
    }

    private void processDestroy() {
        preDestroy();
        parentService.removeActor(actorId, ActorContext.getCurrentActorRef());
        destroyActor();
    }

    /**
     * Called before attempting to remove this actor from the system. Good place
     * for persisting state to DB.
     * <p/>
     * <b>Note:</b> the system doesn't guarantee that actor will be actually
     * destroyed, so this method MUST leave actor instance in working state.
     */
    protected void preDestroy() {
    }

    /**
     * Called after successfully removing this actor from the system.
     * <p/>
     * <b>Note:</b> this method is for final clean-up. It is <b>not</b> a good
     * place for persisting state to DB, as there might already be a new
     * instance of actor created for the same {@code actorId}.
     */
    protected void destroyActor() {
    }

}
