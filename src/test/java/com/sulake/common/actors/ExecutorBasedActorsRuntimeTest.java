/**
 * Copyright 2012 Sulake Oy.
 */
package com.sulake.common.actors;

import com.sulake.common.actors.spi.ExecutorBasedActorsRuntime;
import com.sulake.common.actors.spi.SimpleActorsRuntime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

/**
 * Tests {@link ExecutorBasedActorsRuntime}.
 *
 * @author dmitrym
 */
public class ExecutorBasedActorsRuntimeTest {

    protected TestingActor actor1;
    protected TestingActor actor2;
    protected SimpleActorsRuntime runtime;
    public static final int DEFAULT_MESSAGE_LIMIT = 16;
    public static final int DEFAULT_THREADS = 2;
    public static final String MESSAGE1 = "message1";
    public static final String MESSAGE2 = "message2";
    public static final String MESSAGE3 = "message3";
    public static final String MESSAGE4 = "message4";

    @Before
    public void setUp() {
        actor1 = new TestingActor();
        actor2 = new TestingActor();

        runtime = createRuntime(DEFAULT_MESSAGE_LIMIT, DEFAULT_THREADS);
        runtime.setShutdownWaitTimeInMillis(0);
        runtime.start();
    }

    protected SimpleActorsRuntime createRuntime(int maxMessages, int nThreads) {
        return new ExecutorBasedActorsRuntime(maxMessages, new ThreadPoolExecutor(nThreads, nThreads, 0, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>()));
    }

    @After
    public void tearDown() throws InterruptedException {
        runtime.stop();
    }

    @Test
    public void testSerialMessagePassing() throws Exception {
        ActorRef ref1 = runtime.getSupportFor(actor1);
        ActorRef ref2 = runtime.getSupportFor(actor2);

        ref1.sendMessage(MESSAGE1);
        assertEquals(MESSAGE1, actor1.waitForNextMessage());
        ref1.sendMessage(MESSAGE2);
        assertEquals(MESSAGE2, actor1.waitForNextMessage());
        ref2.sendMessage(MESSAGE3);
        assertEquals(MESSAGE3, actor2.waitForNextMessage());
    }

    @Test
    public void testParallelMessagePassing() throws Exception {
        ActorRef ref1 = runtime.getSupportFor(actor1);
        ActorRef ref2 = runtime.getSupportFor(actor2);

        actor1.pause();
        ref1.sendMessage(MESSAGE1);
        assertEquals(MESSAGE1, actor1.waitForNextMessage());
        ref1.sendMessage(MESSAGE2);
        ref1.sendMessage(MESSAGE3);
        ref2.sendMessage(MESSAGE4);
        assertEquals(MESSAGE4, actor2.waitForNextMessage());
        assertNull(actor1.waitForNextMessage());
        actor1.resume();
        assertEquals(MESSAGE2, actor1.waitForNextMessage());
        assertEquals(MESSAGE3, actor1.waitForNextMessage());
    }

    @Test(expected = IllegalStateException.class)
    public void testNoMessageAfterRuntimeStop() throws Exception {
        ActorRef ref1 = runtime.getSupportFor(actor1);
        runtime.stop();

        ref1.sendMessage(MESSAGE1);
        fail("Expected IllegalStateException here");
    }

    @Test(expected = IllegalStateException.class)
    public void testNoMessageAfterRefRelease() throws Exception {
        ActorRef ref1 = runtime.getSupportFor(actor1);
        ref1.release();

        ref1.sendMessage(MESSAGE1);
        fail("Expected IllegalStateException here");
    }

    @Test
    public void testMessagesOverLimit() throws Exception {
        ActorRef ref1 = runtime.getSupportFor(actor1);

        actor1.pause();
        for (int i = 0; i < DEFAULT_MESSAGE_LIMIT; i++) {
            ref1.sendMessage(MESSAGE1);
        }

        try {
            ref1.sendMessage(MESSAGE2);
            fail("Expected TooManyPendingMessagesException here");
        }
        catch (TooManyPendingMessagesException ignored) {
        }

        actor1.resume();
        assertEquals(MESSAGE1, actor1.waitForNextMessage());
        assertEquals(MESSAGE1, actor1.waitForNextMessage());

        ref1.sendMessage(MESSAGE2);
        for (int i = 2; i < DEFAULT_MESSAGE_LIMIT; i++) {
            assertEquals(MESSAGE1, actor1.waitForNextMessage());
        }
    }
}
