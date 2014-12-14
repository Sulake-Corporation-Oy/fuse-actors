/**
 * Copyright 2012 Sulake Oy.
 */
package com.sulake.common.spring;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Helper to ease classpath scanning.
 *
 * @author dmitrym
 */
public class ClasspathScanningHelper implements BeanFactoryAware {

    private AutowireCapableBeanFactory autowireCapableBeanFactory;

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        autowireCapableBeanFactory = (AutowireCapableBeanFactory) beanFactory;
    }

    @SuppressWarnings("unchecked")
    public <T> Collection<T> autowire(Collection<Class<?>> componentClasses) {
        List<Object> components = new ArrayList<Object>(componentClasses.size());
        for (Class<?> componentClass : componentClasses) {
            components.add(autowireCapableBeanFactory.createBean(componentClass));
        }
        return (List<T>) components;
    }
}
