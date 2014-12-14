/**
 * Copyright 2011 Sulake Oy.
 */
package com.sulake.common.actors.dispatcher;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marker annotation for handlers of {@link com.sulake.common.actors.Actor} requests.
 *
 * @author dmitrym
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ActorMessageHandler {
}
