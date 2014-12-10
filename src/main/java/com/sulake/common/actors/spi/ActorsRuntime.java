/**
 * Copyright 2012 Sulake Oy.
 */
package com.sulake.common.actors.spi;

import com.sulake.common.actors.Actor;
import com.sulake.common.actors.ActorRef;

/**
 * Actual runtime for handling actors.
 *
 * @author dmitrym
 */
public interface ActorsRuntime {

    ActorRef getSupportFor(Actor actor);

    void scheduleHandleNextMessageCall(Runnable actorSupport);
}
