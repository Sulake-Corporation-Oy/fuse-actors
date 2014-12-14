/**
 * Copyright 2012 Sulake Oy.
 */
package com.sulake.common.actors.dispatcher;

import com.sulake.common.actors.Actor;
import com.sulake.common.util.ComponentsProvider;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Required;

import javax.annotation.PostConstruct;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

/**
 * Dispatches requests to matching {@link ActorMessageHandler}.
 *
 * @author dmitrym
 */
public class ActorMessageDispatcher {

    private static final Logger logger = Logger.getLogger(ActorMessageDispatcher.class);

    private final class HandlerMethodInvoker {

        private final Object handler;

        private final Method method;

        private final Class<?> messageType;

        private final boolean takesActor;

        private HandlerMethodInvoker(Object handler, Method method) {
            this.handler = handler;
            this.method = method;

            if (!method.getReturnType().equals(Void.TYPE)) {
                throw new IllegalArgumentException(method.toGenericString() + ": must be void");
            }

            if (method.getParameterTypes().length == 1) {
                takesActor = false;
                messageType = method.getParameterTypes()[0];
            }
            else if (method.getParameterTypes().length == 2) {
                takesActor = true;
                messageType = method.getParameterTypes()[1];
                if (!method.getParameterTypes()[0].isAssignableFrom(actorClass)) {
                    throw new IllegalArgumentException(method.toGenericString() + ": first parameter must be subclass of "
                            + Actor.class.getName());
                }
            }
            else {
                throw new IllegalArgumentException(method.toGenericString() + ": must take 2 parameters");
            }
        }

        private void invokeHandlingMethod(Object actor, Object message) {
            Throwable error;
            try {
                if (takesActor) {
                    method.invoke(handler, actor, message);
                }
                else {
                    method.invoke(handler, message);
                }
                return;
            }
            catch (IllegalArgumentException e) {
                error = e;
            } catch (IllegalAccessException e) {
                error = e;
            }
            catch (InvocationTargetException e) {
                error = e.getCause() != null ? e.getCause() : e;
            }
            logger.error("Unexpected exception handling " + message + " for " + actor, error);
        }

    }

    protected Class<?> actorClass;
    private Collection<?> handlers;
    private final Map<Class<?>, List<HandlerMethodInvoker>> invokersByMessageClass = new HashMap<Class<?>, List<HandlerMethodInvoker>>();

    @Required
    public void setActorClass(Class<?> actorClass) {
        this.actorClass = actorClass;
    }

    public void setHandlers(Collection<?> handlers) {
        if (this.handlers != null) {
            throw new IllegalStateException("Only one of handlers or handlersProvider must be specified");
        }
        this.handlers = handlers;
    }

    public void setHandlersProvider(ComponentsProvider provider) {
        setHandlers(provider.getComponents());
    }

    @PostConstruct
    public void init() {
        if (handlers == null) {
            throw new IllegalStateException("Either handlers or handlersProvider must be specified");
        }
        for (Object handler : handlers) {
            if (!addHandler(handler)) {
                throw new IllegalArgumentException("No idea how to deliver requests to " + handler);
            }
        }
    }

    /**
     * Parses add adds given handler to internal mapping. Sub-classes CAN
     * override this to add support for custom mappings, but MUST always call
     * super implementation and MUST return {@code false} only if handler
     * type is not supported both by sub-class and super-class.
     *
     * @param handler handler object
     * @return {@code true} if handler has been successfully added to
     * internal mappings; {@code false} if handler type is not
     * supported
     */
    protected boolean addHandler(Object handler) {
        boolean supported = false;

        for (Method method : handler.getClass().getMethods()) {
            if (!method.isAnnotationPresent(ActorMessageHandler.class)) {
                continue;
            }

            HandlerMethodInvoker invoker = new HandlerMethodInvoker(handler, method);
            List<HandlerMethodInvoker> invokersForMessageType = invokersByMessageClass.get(invoker.messageType);
            if (invokersForMessageType == null) {
                invokersForMessageType = new ArrayList<HandlerMethodInvoker>();
                invokersByMessageClass.put(invoker.messageType, invokersForMessageType);
            }
            invokersForMessageType.add(invoker);

            supported = true;
        }

        return supported;
    }

    /**
     * Dispatches message to handlers that are interested in it.
     *
     * @param actor   current actor
     * @param message message to dispatch
     * @return {@code true} if message was dispatched; {@code false}
     * if there are no handlers for it
     */
    public boolean dispatchMessage(Object actor, Object message) {
        Collection<HandlerMethodInvoker> invokers = invokersByMessageClass.get(message.getClass());
        if (invokers == null || invokers.isEmpty()) {
            return false;
        }

        for (HandlerMethodInvoker invoker : invokers) {
            invoker.invokeHandlingMethod(actor, message);
        }
        return true;
    }
}
