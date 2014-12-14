/**
 * Copyright 2012 Sulake Oy.
 */
package com.sulake.common.spring;

import com.sulake.common.util.ComponentClassesProvider;
import com.sulake.common.util.ComponentsProvider;
import org.springframework.beans.factory.annotation.Required;

import java.util.Collection;

/**
 * Provides list of initialized component instances by auto-wiring classes delivered by {@link ComponentClassesProvider}.
 *
 * @author dmitrym
 */
public class AutowiringComponentsProvider implements ComponentsProvider {

    private ClasspathScanningHelper classpathScanningHelper;
    private ComponentClassesProvider componentClassesProvider;

    @Required
    public void setComponentClassesProvider(ComponentClassesProvider componentClassesProvider) {
        this.componentClassesProvider = componentClassesProvider;
    }

    @Required
    public void setClasspathScanningHelper(ClasspathScanningHelper classpathScanningHelper) {
        this.classpathScanningHelper = classpathScanningHelper;
    }

    @Override
    public <T> Collection<T> getComponents() {
        return classpathScanningHelper.autowire(componentClassesProvider.getComponentClasses());
    }
}
