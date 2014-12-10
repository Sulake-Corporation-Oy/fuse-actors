/**
 * Copyright 2012 Sulake Oy.
 */
package com.sulake.common.actors;

import com.sulake.common.actors.spi.PassthroughActorRuntime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.beans.factory.ObjectFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

/**
 * Tests {@link SmartActors} and {@link SmartActor}.
 *
 * @author dmitrym
 */
@RunWith(MockitoJUnitRunner.class)
public class SmartActorsTest {

    public interface TestActorLogic {

        void init();

        boolean canBeRemoved();

        boolean processMessage(Object message);

        void processPing();

        void preDestroy();

        void destroy();
    }

    public static class TestActor extends SmartActor<Integer> {

        private final TestActorLogic logic;

        public TestActor(TestActorLogic logic) {
            this.logic = logic;
        }

        @Override
        protected void initActor(Integer actorId) {
            super.initActor(actorId);
            logic.init();
        }

        @Override
        protected boolean canBeRemoved() {
            return logic.canBeRemoved();
        }

        @Override
        public boolean processMessage(Object message) {
            return super.processMessage(message) || logic.processMessage(message);
        }

        @Override
        protected void processPing() {
            logic.processPing();
            super.processPing();
        }

        @Override
        protected void preDestroy() {
            logic.preDestroy();
            super.preDestroy();
        }

        @Override
        protected void destroyActor() {
            logic.destroy();
            super.destroyActor();
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("TestActor{");
            sb.append("actorId=").append(getActorId());
            sb.append('}');
            return sb.toString();
        }
    }

    @Mock
    private ObjectFactory<SmartActor<Integer>> actorsFactory;

    @Mock
    private TestActorLogic logic1;

    private TestActor actor1;

    @Mock
    private TestActorLogic logic2;

    private TestActor actor2;

    private SmartActors<Integer> actors;

    @Before
    public void setUp() {
        actors = new SmartActors<Integer>();
        actors.setActorsRuntime(new PassthroughActorRuntime());
        actors.setActorsFactory(actorsFactory);

        actor1 = new TestActor(logic1);
        actor2 = new TestActor(logic2);
    }

    @Test
    public void testNormalOperations() throws Exception {
        when(actorsFactory.getObject()).thenReturn(actor1);
        actors.sendMessage(1, "MSG1");
        verify(actor1.logic).init();
        verify(actor1.logic).processMessage("MSG1");

        actors.sendMessage(1, "MSG2");
        verify(actor1.logic).processMessage("MSG2");

        when(actorsFactory.getObject()).thenReturn(actor2);
        actors.sendMessage(2, "MSG3");
        verify(actor2.logic).init();
        verify(actor2.logic).processMessage("MSG3");
    }

    @Test
    public void testPingBatchParameters() throws Exception {
        actors.setPingPeriodInMillis(10000);
        actors.setPreferredPingBatchSize(4);

        actors.calculateNextPingBatch();
        assertEquals(0, actors.getLastPingBatchSize());
        assertEquals(10000, actors.getLastPingBatchPeriod());

        createTestActors(10);

        actors.calculateNextPingBatch();
        assertEquals(4, actors.getLastPingBatchSize());
        assertEquals(10000 * 4 / 10, actors.getLastPingBatchPeriod());

        actors.setPingPeriodInMillis(1000);
        actors.calculateNextPingBatch();
        assertEquals(10, actors.getLastPingBatchSize());
        assertEquals(1000, actors.getLastPingBatchPeriod());
    }

    @Test
    public void testPingBatchSending() throws Exception {
        final TestActor[] testActors = createTestActors(5);

        actors.setPreferredPingBatchSize(4);
        actors.setPingPeriodInMillis(10000);
        actors.calculateNextPingBatch();
        assertEquals(4, actors.getLastPingBatchSize());

        for (TestActor testActor : testActors) {
            when(testActor.logic.canBeRemoved()).thenReturn(false);
        }

        actors.sendPingToNextBatch();
        actors.sendPingToNextBatch();
        actors.sendPingToNextBatch();
        actors.sendPingToNextBatch();
        actors.sendPingToNextBatch();

        for (TestActor testActor : testActors) {
            verify(testActor.logic, times(4)).processPing();
            verify(testActor.logic, times(4)).canBeRemoved();
        }
    }

    private TestActor[] createTestActors(int num) {
        final TestActor[] testActors = new TestActor[num];
        testActors[0] = actor1;
        testActors[1] = actor2;
        for (int i = 2; i < testActors.length; i++) {
            testActors[i] = new TestActor(mock(TestActorLogic.class, "actor" + (i + 1)));
        }
        for (int i = 0; i < testActors.length; i++) {
            TestActor testActor = testActors[i];
            when(actorsFactory.getObject()).thenReturn(testActor);
            actors.sendMessage(i, "MSG1");
            verify(testActor.logic).init();
            verify(testActor.logic).processMessage("MSG1");
        }
        return testActors;
    }

    @Test
    public void testActorRemoval() throws Exception {
        actors.setPingPeriodInMillis(1000);

        when(actorsFactory.getObject()).thenReturn(actor1);
        actors.sendMessage(1, "MSG1");
        verify(actor1.logic).init();
        verify(actor1.logic).processMessage("MSG1");

        when(actor1.logic.canBeRemoved()).thenReturn(false);
        actors.calculateNextPingBatch();
        actors.sendPingToNextBatch();
        verify(actor1.logic).processPing();
        verify(actor1.logic).canBeRemoved();
        verify(actor1.logic, times(0)).preDestroy();
        verify(actor1.logic, times(0)).destroy();

        when(actor1.logic.canBeRemoved()).thenReturn(true);
        actors.calculateNextPingBatch();
        actors.sendPingToNextBatch();
        verify(actor1.logic, times(2)).processPing();
        verify(actor1.logic, times(2)).canBeRemoved();
        verify(actor1.logic).preDestroy();
        verify(actor1.logic).destroy();

        when(actorsFactory.getObject()).thenReturn(actor2);
        actors.sendMessage(1, "MSG1");
        verify(actor2.logic).init();
        verify(actor2.logic).processMessage("MSG1");
    }

    @Test(expected = IllegalStateException.class)
    public void testStop() throws Exception {
        when(actorsFactory.getObject()).thenReturn(actor1);
        actors.sendMessage(1, "MSG1");
        verify(actor1.logic).init();
        verify(actor1.logic).processMessage("MSG1");

        actors.stop();
        verify(actor1.logic).preDestroy();
        verify(actor1.logic).destroy();

        actors.sendMessage(1, "MSG1");
        fail("Expected IllegalStateException here");
    }
}
