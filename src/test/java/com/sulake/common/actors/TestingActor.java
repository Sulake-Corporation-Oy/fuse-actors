/**
 * Copyright 2012 Sulake Oy.
 */
package com.sulake.common.actors;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class TestingActor implements Actor {

    public static final int WAIT_TIME = 5000;

    private final List<Object> receivedMessages = new LinkedList<Object>();

    private boolean paused;

    @Override
    public boolean processMessage(Object message) {
        synchronized (receivedMessages) {
            receivedMessages.add(message);
            receivedMessages.notifyAll();
        }
        synchronized (this) {
            while (paused) {
                try {
                    wait();
                }
                catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            }
        }
        return true;
    }

    public List<Object> drainReceivedMessages() {
        synchronized (receivedMessages) {
            ArrayList<Object> result = new ArrayList<Object>(receivedMessages);
            receivedMessages.clear();
            return result;
        }
    }

    @Nullable
    public Object waitForNextMessage() throws InterruptedException {
        synchronized (receivedMessages) {
            if (receivedMessages.isEmpty()) {
                receivedMessages.wait(WAIT_TIME);
            }
            return receivedMessages.isEmpty() ? null : receivedMessages.remove(0);
        }
    }

    public synchronized void pause() {
        paused = true;
    }

    public synchronized void resume() {
        paused = false;
        notifyAll();
    }
}