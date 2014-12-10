/**
 * Copyright 2012 Sulake Oy.
 */
package com.sulake.common.actors;

import com.sulake.common.actors.spi.SimpleActorsRuntime;

/**
 * Indicates that there are too many pending requests for actors.
 *
 * @author dmitrym
 * @see SimpleActorsRuntime#setMaxMessages(int)
 * @see ActorRef#sendMessage(Object)
 */
@SuppressWarnings("serial")
public class TooManyPendingMessagesException extends ActorException {
}
