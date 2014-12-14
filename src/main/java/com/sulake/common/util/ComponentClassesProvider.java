/**
 * Copyright 2012 Sulake Oy.
 */
package com.sulake.common.util;

import java.util.Collection;

/**
 * Generic interface to obtain list of classes matching certain criteria.
 *
 * @author dmitrym
 */
public interface ComponentClassesProvider {
    Collection<Class<?>> getComponentClasses();
}
