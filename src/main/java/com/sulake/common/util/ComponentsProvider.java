/**
 * Copyright 2012 Sulake Oy.
 */
package com.sulake.common.util;

import java.util.Collection;

/**
 * Generic interface to obtain list of initialized component instances.
 *
 * @author dmitrym
 */
public interface ComponentsProvider {
    <T> Collection<T> getComponents();
}
