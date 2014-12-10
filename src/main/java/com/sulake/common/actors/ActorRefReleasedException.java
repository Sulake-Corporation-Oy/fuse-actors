/**
 * Copyright 2012 Sulake Oy.
 */
package com.sulake.common.actors;

/**
 * Indicates an attempt to send a message via released {@link ActorRef}.
 *
 * @author dmitrym
 * @see ActorRef
 */
@SuppressWarnings("serial")
public class ActorRefReleasedException extends IllegalStateException {

}
