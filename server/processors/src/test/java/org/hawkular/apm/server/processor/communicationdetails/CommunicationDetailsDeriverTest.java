/*
 * Copyright 2015-2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hawkular.apm.server.processor.communicationdetails;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.hawkular.apm.api.model.Constants;
import org.hawkular.apm.api.model.Property;
import org.hawkular.apm.api.model.events.CommunicationDetails;
import org.hawkular.apm.api.model.events.SourceInfo;
import org.hawkular.apm.api.model.trace.Component;
import org.hawkular.apm.api.model.trace.Consumer;
import org.hawkular.apm.api.model.trace.CorrelationIdentifier;
import org.hawkular.apm.api.model.trace.CorrelationIdentifier.Scope;
import org.hawkular.apm.api.model.trace.Producer;
import org.hawkular.apm.api.model.trace.Trace;
import org.hawkular.apm.server.api.task.RetryAttemptException;
import org.junit.Test;

/**
 * @author gbrown
 */
public class CommunicationDetailsDeriverTest {

    /**  */
    private static final String BTXN_NAME = "traceName";

    @Test
    public void testInitialise() throws RetryAttemptException {
        TestSourceInfoCache cache=new TestSourceInfoCache();

        CommunicationDetailsDeriver deriver = new CommunicationDetailsDeriver();
        deriver.setSourceInfoCache(cache);

        List<Trace> traces = new ArrayList<Trace>();

        Trace trace1 = new Trace();
        trace1.setId("trace1");
        trace1.setStartTime(System.currentTimeMillis());

        traces.add(trace1);

        Consumer c1 = new Consumer();
        c1.setBaseTime(System.nanoTime());

        CorrelationIdentifier cid1 = new CorrelationIdentifier();
        cid1.setScope(Scope.Interaction);
        cid1.setValue("cid1");
        c1.getCorrelationIds().add(cid1);

        trace1.getNodes().add(c1);

        Producer p1 = new Producer();
        p1.setBaseTime(System.nanoTime());

        CorrelationIdentifier pid1 = new CorrelationIdentifier();
        pid1.setScope(Scope.Interaction);
        pid1.setValue("pid1");
        p1.getCorrelationIds().add(pid1);

        c1.getNodes().add(p1);

        deriver.initialise(null, traces);

        assertNotNull(deriver.getSourceInfoCache().get(null, "pid1"));
        assertNull(deriver.getSourceInfoCache().get(null, "cid1"));
    }

    @Test
    public void testInitialiseClientFragment() throws RetryAttemptException {
        TestSourceInfoCache cache=new TestSourceInfoCache();

        CommunicationDetailsDeriver deriver = new CommunicationDetailsDeriver();
        deriver.setSourceInfoCache(cache);

        Trace trace1 = new Trace();
        trace1.setId("trace1");
        trace1.setStartTime(System.currentTimeMillis());

        Component c1 = new Component();
        trace1.getNodes().add(c1);

        Producer p1 = new Producer();
        p1.setUri("p1");
        p1.setBaseTime(System.nanoTime());

        CorrelationIdentifier pid1 = new CorrelationIdentifier();
        pid1.setScope(Scope.Interaction);
        pid1.setValue("pid1");
        p1.getCorrelationIds().add(pid1);

        c1.getNodes().add(p1);

        Producer p2 = new Producer();
        p2.setUri("p2");
        p2.setBaseTime(System.nanoTime());

        CorrelationIdentifier pid2 = new CorrelationIdentifier();
        pid2.setScope(Scope.Interaction);
        pid2.setValue("pid2");
        p2.getCorrelationIds().add(pid2);

        c1.getNodes().add(p2);

        deriver.initialise(null, Collections.singletonList(trace1));

        SourceInfo si1 = deriver.getSourceInfoCache().get(null, "pid1");
        SourceInfo si2 = deriver.getSourceInfoCache().get(null, "pid2");

        assertNotNull(si1);
        assertNotNull(si2);

        assertEquals(Constants.URI_CLIENT_PREFIX + "p1", si1.getFragmentUri());

        // Check that source info 2 has same origin URI as p1, as they
        // are from the same fragment (without a consumer) so are being identified
        // as a client of the first producer URI found (see HWKBTM-353).
        assertEquals(Constants.URI_CLIENT_PREFIX + "p1", si2.getFragmentUri());
    }

    @Test
    public void testInitialiseServerFragment() throws RetryAttemptException {
        TestSourceInfoCache cache=new TestSourceInfoCache();

        CommunicationDetailsDeriver deriver = new CommunicationDetailsDeriver();
        deriver.setSourceInfoCache(cache);

        Trace trace1 = new Trace();
        trace1.setId("trace1");
        trace1.setStartTime(System.currentTimeMillis());

        Consumer c1 = new Consumer();
        c1.setUri("consumerURI");
        trace1.getNodes().add(c1);

        Producer p1 = new Producer();
        p1.setUri("p1");
        p1.setBaseTime(System.nanoTime());

        CorrelationIdentifier pid1 = new CorrelationIdentifier();
        pid1.setScope(Scope.Interaction);
        pid1.setValue("pid1");
        p1.getCorrelationIds().add(pid1);

        c1.getNodes().add(p1);

        Producer p2 = new Producer();
        p2.setUri("p2");
        p2.setBaseTime(System.nanoTime());

        CorrelationIdentifier pid2 = new CorrelationIdentifier();
        pid2.setScope(Scope.Interaction);
        pid2.setValue("pid2");
        p2.getCorrelationIds().add(pid2);

        c1.getNodes().add(p2);

        deriver.initialise(null, Collections.singletonList(trace1));

        SourceInfo si1 = deriver.getSourceInfoCache().get(null, "pid1");
        SourceInfo si2 = deriver.getSourceInfoCache().get(null, "pid2");

        assertNotNull(si1);
        assertNotNull(si2);

        assertEquals("consumerURI", si1.getFragmentUri());
        assertEquals("consumerURI", si2.getFragmentUri());
    }

    @Test
    public void testProcessSingleNoProducer() {
        TestSourceInfoCache cache=new TestSourceInfoCache();

        CommunicationDetailsDeriver deriver = new CommunicationDetailsDeriver();
        deriver.setSourceInfoCache(cache);

        List<Trace> traces = new ArrayList<Trace>();

        Trace trace1 = new Trace();
        traces.add(trace1);

        Consumer c1 = new Consumer();

        CorrelationIdentifier cid1 = new CorrelationIdentifier();
        cid1.setScope(Scope.Interaction);
        cid1.setValue("cid1");
        c1.getCorrelationIds().add(cid1);

        trace1.getNodes().add(c1);

        Producer p1 = new Producer();

        CorrelationIdentifier pid1 = new CorrelationIdentifier();
        pid1.setScope(Scope.Interaction);
        pid1.setValue("pid1");
        p1.getCorrelationIds().add(pid1);

        c1.getNodes().add(p1);

        try {
            deriver.processOneToOne(null, trace1);
            fail("Should have thrown exception");
        } catch (Exception e) {
        }
    }

    @Test
    public void testProcessSingleInteraction() throws RetryAttemptException {
        TestSourceInfoCache cache=new TestSourceInfoCache();

        CommunicationDetailsDeriver deriver = new CommunicationDetailsDeriver();
        deriver.setSourceInfoCache(cache);

        Trace trace1 = new Trace();
        trace1.setStartTime(1000000);

        trace1.setBusinessTransaction(BTXN_NAME);
        trace1.setId("trace1");
        trace1.setHostName("host1");
        trace1.setHostAddress("addr1");
        trace1.setPrincipal("p1");

        Consumer c1 = new Consumer();
        c1.setUri("FirstURI");
        c1.setBaseTime(0);

        CorrelationIdentifier cid1 = new CorrelationIdentifier();
        cid1.setScope(Scope.Interaction);
        cid1.setValue("cid1");
        c1.getCorrelationIds().add(cid1);

        trace1.getNodes().add(c1);

        Producer p1 = new Producer();
        p1.setBaseTime(1000000);
        p1.setDuration(2000000000);
        p1.getProperties().add(new Property("prop1", "value1"));

        CorrelationIdentifier pid1 = new CorrelationIdentifier();
        pid1.setScope(Scope.Interaction);
        pid1.setValue("pid1");
        p1.getCorrelationIds().add(pid1);

        c1.getNodes().add(p1);

        Trace trace2 = new Trace();
        trace2.setStartTime(2000000);

        trace2.setBusinessTransaction(BTXN_NAME);
        trace2.setId("trace2");
        trace2.setHostName("host2");
        trace2.setHostAddress("addr2");
        trace2.setPrincipal("p1");

        Consumer c2 = new Consumer();
        c2.setUri("SecondURI");
        c2.setDuration(1200000000);
        c2.getProperties().add(new Property("prop2", "value2"));

        CorrelationIdentifier cid2 = new CorrelationIdentifier();
        cid2.setScope(Scope.Interaction);
        cid2.setValue("pid1");
        c2.getCorrelationIds().add(cid2);

        trace2.getNodes().add(c2);

        deriver.initialise(null, Collections.singletonList(trace1));
        deriver.initialise(null, Collections.singletonList(trace2));
        CommunicationDetails details = deriver.processOneToOne(null, trace2);

        assertNotNull(details);

        assertEquals("pid1", details.getLinkId());
        assertEquals(BTXN_NAME, details.getBusinessTransaction());
        assertEquals("FirstURI", details.getSource());
        assertEquals("SecondURI", details.getTarget());

        assertFalse(details.isMultiConsumer());

        assertTrue(c2.getDuration() == details.getConsumerDuration());
        assertTrue(p1.getDuration() == details.getProducerDuration());
        assertTrue(400 == details.getLatency());
        assertTrue(details.hasProperty("prop1"));
        assertTrue(details.hasProperty("prop2"));
        assertEquals("trace1", details.getSourceFragmentId());
        assertEquals("host1", details.getSourceHostName());
        assertEquals("addr1", details.getSourceHostAddress());
        assertEquals("trace2", details.getTargetFragmentId());
        assertEquals("host2", details.getTargetHostName());
        assertEquals("addr2", details.getTargetHostAddress());
        assertEquals("p1", details.getPrincipal());

        long timestamp = trace1.getStartTime() + TimeUnit.MILLISECONDS.convert(p1.getBaseTime() -
                c1.getBaseTime(), TimeUnit.NANOSECONDS);
        assertEquals(timestamp, details.getTimestamp());

        long timestampOffset = trace2.getStartTime() - details.getTimestamp() - details.getLatency();

        assertEquals(timestampOffset, details.getTimestampOffset());
    }

    @Test
    public void testProcessSingleControlFlow() throws RetryAttemptException {
        TestSourceInfoCache cache=new TestSourceInfoCache();

        CommunicationDetailsDeriver deriver = new CommunicationDetailsDeriver();
        deriver.setSourceInfoCache(cache);

        Trace trace1 = new Trace();
        trace1.setStartTime(1000000);

        trace1.setBusinessTransaction(BTXN_NAME);
        trace1.setId("trace1");
        trace1.setHostName("host1");
        trace1.setHostAddress("addr1");
        trace1.setPrincipal("p1");

        Consumer c1 = new Consumer();
        c1.setUri("FirstURI");
        c1.setBaseTime(0);

        CorrelationIdentifier cid1 = new CorrelationIdentifier();
        cid1.setScope(Scope.ControlFlow);
        cid1.setValue("cid1");
        c1.getCorrelationIds().add(cid1);

        trace1.getNodes().add(c1);

        Producer p1 = new Producer();
        p1.setBaseTime(1000000);
        p1.setDuration(2000000000);
        p1.getProperties().add(new Property("prop1", "value1"));

        CorrelationIdentifier pid1 = new CorrelationIdentifier();
        pid1.setScope(Scope.ControlFlow);
        pid1.setValue("pid1");
        p1.getCorrelationIds().add(pid1);

        c1.getNodes().add(p1);

        Trace trace2 = new Trace();
        trace2.setStartTime(2000000);

        trace2.setBusinessTransaction(BTXN_NAME);
        trace2.setId("trace2");
        trace2.setHostName("host2");
        trace2.setHostAddress("addr2");
        trace2.setPrincipal("p1");

        Consumer c2 = new Consumer();
        c2.setUri("SecondURI");
        c2.setDuration(1200000000);
        c2.getProperties().add(new Property("prop2", "value2"));

        CorrelationIdentifier cid2 = new CorrelationIdentifier();
        cid2.setScope(Scope.ControlFlow);
        cid2.setValue("pid1");
        c2.getCorrelationIds().add(cid2);

        trace2.getNodes().add(c2);

        deriver.initialise(null, Collections.singletonList(trace1));
        deriver.initialise(null, Collections.singletonList(trace2));
        CommunicationDetails details = deriver.processOneToOne(null, trace2);

        assertNotNull(details);

        assertEquals("pid1", details.getLinkId());
        assertEquals(BTXN_NAME, details.getBusinessTransaction());
        assertEquals("FirstURI", details.getSource());
        assertEquals("SecondURI", details.getTarget());

        assertFalse(details.isMultiConsumer());

        assertTrue(c2.getDuration() == details.getConsumerDuration());
        assertTrue(p1.getDuration() == details.getProducerDuration());
        assertTrue(400 == details.getLatency());
        assertTrue(details.hasProperty("prop1"));
        assertTrue(details.hasProperty("prop2"));
        assertEquals("trace1", details.getSourceFragmentId());
        assertEquals("host1", details.getSourceHostName());
        assertEquals("addr1", details.getSourceHostAddress());
        assertEquals("trace2", details.getTargetFragmentId());
        assertEquals("host2", details.getTargetHostName());
        assertEquals("addr2", details.getTargetHostAddress());
        assertEquals("p1", details.getPrincipal());

        long timestamp = trace1.getStartTime() + TimeUnit.MILLISECONDS.convert(p1.getBaseTime() -
                c1.getBaseTime(), TimeUnit.NANOSECONDS);
        assertEquals(timestamp, details.getTimestamp());

        long timestampOffset = trace2.getStartTime() - details.getTimestamp() - details.getLatency();

        assertEquals(timestampOffset, details.getTimestampOffset());
    }

    @Test
    public void testProcessSingleMultiConsumerInteraction() throws RetryAttemptException {
        TestSourceInfoCache cache=new TestSourceInfoCache();

        CommunicationDetailsDeriver deriver = new CommunicationDetailsDeriver();
        deriver.setSourceInfoCache(cache);

        List<Trace> traces1 = new ArrayList<Trace>();

        Trace trace1 = new Trace();
        trace1.setStartTime(1000000);

        traces1.add(trace1);

        trace1.setBusinessTransaction(BTXN_NAME);
        trace1.setId("trace1");
        trace1.setHostName("host1");
        trace1.setHostAddress("addr1");

        Consumer c1 = new Consumer();
        c1.setUri("FirstURI");
        c1.setBaseTime(0);

        CorrelationIdentifier cid1 = new CorrelationIdentifier();
        cid1.setScope(Scope.Interaction);
        cid1.setValue("cid1");
        c1.getCorrelationIds().add(cid1);

        trace1.getNodes().add(c1);

        Producer p1 = new Producer();
        p1.setBaseTime(1000000);
        p1.setDuration(2000000000);
        p1.getDetails().put(Producer.DETAILS_PUBLISH, "true");

        CorrelationIdentifier pid1 = new CorrelationIdentifier();
        pid1.setScope(Scope.Interaction);
        pid1.setValue("pid1");
        p1.getCorrelationIds().add(pid1);

        c1.getNodes().add(p1);

        List<Trace> traces2 = new ArrayList<Trace>();

        Trace trace2 = new Trace();
        trace2.setStartTime(2000000);

        traces2.add(trace2);

        trace2.setBusinessTransaction(BTXN_NAME);
        trace2.setId("trace2");
        trace2.setHostName("host2");
        trace2.setHostAddress("addr2");

        Consumer c2 = new Consumer();
        c2.setUri("SecondURI");
        c2.setDuration(1200000000);
        c2.getDetails().put(Consumer.DETAILS_PUBLISH, "true");
        c2.getProperties().add(new Property("prop1", "value1"));

        CorrelationIdentifier cid2 = new CorrelationIdentifier();
        cid2.setScope(Scope.Interaction);
        cid2.setValue("pid1");
        c2.getCorrelationIds().add(cid2);

        trace2.getNodes().add(c2);

        deriver.initialise(null, traces1);
        deriver.initialise(null, traces2);
        CommunicationDetails details = deriver.processOneToOne(null, trace2);

        assertNotNull(details);

        assertTrue(details.isMultiConsumer());
    }

    @Test
    public void testProcessSingleMultiConsumerControlFlow() throws RetryAttemptException {
        TestSourceInfoCache cache=new TestSourceInfoCache();

        CommunicationDetailsDeriver deriver = new CommunicationDetailsDeriver();
        deriver.setSourceInfoCache(cache);

        List<Trace> traces1 = new ArrayList<Trace>();

        Trace trace1 = new Trace();
        trace1.setStartTime(1000000);

        traces1.add(trace1);

        trace1.setBusinessTransaction(BTXN_NAME);
        trace1.setId("trace1");
        trace1.setHostName("host1");
        trace1.setHostAddress("addr1");

        Consumer c1 = new Consumer();
        c1.setUri("FirstURI");
        c1.setBaseTime(0);

        CorrelationIdentifier cid1 = new CorrelationIdentifier();
        cid1.setScope(Scope.ControlFlow);
        cid1.setValue("cid1");
        c1.getCorrelationIds().add(cid1);

        trace1.getNodes().add(c1);

        Producer p1 = new Producer();
        p1.setBaseTime(1000000);
        p1.setDuration(2000000000);
        p1.getDetails().put(Producer.DETAILS_PUBLISH, "true");

        CorrelationIdentifier pid1 = new CorrelationIdentifier();
        pid1.setScope(Scope.ControlFlow);
        pid1.setValue("pid1");
        p1.getCorrelationIds().add(pid1);

        c1.getNodes().add(p1);

        List<Trace> traces2 = new ArrayList<Trace>();

        Trace trace2 = new Trace();
        trace2.setStartTime(2000000);

        traces2.add(trace2);

        trace2.setBusinessTransaction(BTXN_NAME);
        trace2.setId("trace2");
        trace2.setHostName("host2");
        trace2.setHostAddress("addr2");

        Consumer c2 = new Consumer();
        c2.setUri("SecondURI");
        c2.setDuration(1200000000);
        c2.getDetails().put(Consumer.DETAILS_PUBLISH, "true");
        c2.getProperties().add(new Property("prop1", "value1"));

        CorrelationIdentifier cid2 = new CorrelationIdentifier();
        cid2.setScope(Scope.ControlFlow);
        cid2.setValue("pid1");
        c2.getCorrelationIds().add(cid2);

        trace2.getNodes().add(c2);

        deriver.initialise(null, traces1);
        deriver.initialise(null, traces2);
        CommunicationDetails details = deriver.processOneToOne(null, trace2);

        assertNotNull(details);

        assertTrue(details.isMultiConsumer());
    }

    @Test
    public void testProcessSingleWithClient() throws RetryAttemptException {
        TestSourceInfoCache cache=new TestSourceInfoCache();

        CommunicationDetailsDeriver deriver = new CommunicationDetailsDeriver();
        deriver.setSourceInfoCache(cache);

        List<Trace> traces1 = new ArrayList<Trace>();

        Trace trace1 = new Trace();
        trace1.setStartTime(System.currentTimeMillis());

        traces1.add(trace1);

        trace1.setBusinessTransaction(BTXN_NAME);
        trace1.setId("trace1");
        trace1.setHostName("host1");
        trace1.setHostAddress("addr1");
        trace1.setPrincipal("p1");

        Producer p1 = new Producer();
        p1.setUri("TheURI");
        p1.setBaseTime(System.nanoTime());
        p1.setDuration(2000000000);

        CorrelationIdentifier pid1 = new CorrelationIdentifier();
        pid1.setScope(Scope.Interaction);
        pid1.setValue("pid1");
        p1.getCorrelationIds().add(pid1);

        trace1.getNodes().add(p1);

        List<Trace> traces2 = new ArrayList<Trace>();

        Trace trace2 = new Trace();
        traces2.add(trace2);

        trace2.setBusinessTransaction(BTXN_NAME);
        trace2.setId("trace2");
        trace2.setHostName("host2");
        trace2.setHostAddress("addr2");
        trace2.setPrincipal("p1");

        Consumer c2 = new Consumer();
        c2.setUri("TheURI");
        c2.setDuration(1200000000);
        c2.getProperties().add(new Property("prop1", "value1"));

        CorrelationIdentifier cid2 = new CorrelationIdentifier();
        cid2.setScope(Scope.Interaction);
        cid2.setValue("pid1");
        c2.getCorrelationIds().add(cid2);

        trace2.getNodes().add(c2);

        deriver.initialise(null, traces1);
        deriver.initialise(null, traces2);
        CommunicationDetails details = deriver.processOneToOne(null, trace2);

        assertNotNull(details);

        assertEquals("pid1", details.getLinkId());
        assertEquals(BTXN_NAME, details.getBusinessTransaction());
        assertEquals(Constants.URI_CLIENT_PREFIX + "TheURI", details.getSource());
        assertEquals("TheURI", details.getTarget());
        assertTrue(c2.getDuration() == details.getConsumerDuration());
        assertTrue(p1.getDuration() == details.getProducerDuration());
        assertTrue(400 == details.getLatency());
        assertTrue(details.hasProperty("prop1"));
        assertEquals("trace1", details.getSourceFragmentId());
        assertEquals("host1", details.getSourceHostName());
        assertEquals("addr1", details.getSourceHostAddress());
        assertEquals("trace2", details.getTargetFragmentId());
        assertEquals("host2", details.getTargetHostName());
        assertEquals("addr2", details.getTargetHostAddress());
        assertEquals("p1", details.getPrincipal());
    }

    @Test
    public void testProcessSinglePropertyNullValue() throws RetryAttemptException {
        TestSourceInfoCache cache=new TestSourceInfoCache();

        CommunicationDetailsDeriver deriver = new CommunicationDetailsDeriver();
        deriver.setSourceInfoCache(cache);

        List<Trace> traces1 = new ArrayList<Trace>();

        Trace trace1 = new Trace();
        trace1.setStartTime(1000000);

        traces1.add(trace1);

        trace1.setBusinessTransaction(BTXN_NAME);
        trace1.setId("trace1");

        Consumer c1 = new Consumer();
        c1.setUri("FirstURI");
        c1.setBaseTime(0);

        CorrelationIdentifier cid1 = new CorrelationIdentifier();
        cid1.setScope(Scope.Interaction);
        cid1.setValue("cid1");
        c1.getCorrelationIds().add(cid1);

        trace1.getNodes().add(c1);

        Producer p1 = new Producer();
        p1.setBaseTime(1000000);
        p1.setDuration(2000000000);
        p1.getProperties().add(new Property("prop1", null));

        CorrelationIdentifier pid1 = new CorrelationIdentifier();
        pid1.setScope(Scope.Interaction);
        pid1.setValue("pid1");
        p1.getCorrelationIds().add(pid1);

        c1.getNodes().add(p1);

        List<Trace> traces2 = new ArrayList<Trace>();

        Trace trace2 = new Trace();
        trace2.setStartTime(2000000);

        traces2.add(trace2);

        trace2.setId("trace2");

        Consumer c2 = new Consumer();
        c2.setUri("SecondURI");
        c2.setDuration(1200000000);
        c2.getProperties().add(new Property("prop2", null));

        CorrelationIdentifier cid2 = new CorrelationIdentifier();
        cid2.setScope(Scope.Interaction);
        cid2.setValue("pid1");
        c2.getCorrelationIds().add(cid2);

        trace2.getNodes().add(c2);

        deriver.initialise(null, traces1);
        deriver.initialise(null, traces2);
        CommunicationDetails details = deriver.processOneToOne(null, trace2);

        assertNotNull(details);

        assertEquals("pid1", details.getLinkId());
        assertEquals("FirstURI", details.getSource());
        assertEquals("SecondURI", details.getTarget());

        assertTrue(details.hasProperty("prop1"));
        assertEquals(1, details.getProperties("prop1").size());
        assertNull(details.getProperties("prop1").iterator().next().getValue());
        assertTrue(details.hasProperty("prop2"));
        assertEquals(1, details.getProperties("prop2").size());
        assertNull(details.getProperties("prop2").iterator().next().getValue());
    }

    @Test
    public void testProcessSingleCausedBy() throws RetryAttemptException {
        TestSourceInfoCache cache=new TestSourceInfoCache();

        CommunicationDetailsDeriver deriver = new CommunicationDetailsDeriver();
        deriver.setSourceInfoCache(cache);

        Trace trace1 = new Trace();
        trace1.setStartTime(1000000);

        trace1.setBusinessTransaction(BTXN_NAME);
        trace1.setId("trace1");
        trace1.setHostName("host1");
        trace1.setHostAddress("addr1");
        trace1.setPrincipal("p1");

        Consumer c1 = new Consumer();
        c1.setUri("FirstURI");
        c1.setBaseTime(0);

        CorrelationIdentifier cid1 = new CorrelationIdentifier();
        cid1.setScope(Scope.Interaction);
        cid1.setValue("cid1");
        c1.getCorrelationIds().add(cid1);

        trace1.getNodes().add(c1);

        Component comp1 = new Component();
        comp1.setBaseTime(1000000);
        comp1.setDuration(2000000000);
        comp1.getProperties().add(new Property("prop1", "value1"));

        c1.getNodes().add(comp1);

        Trace trace2 = new Trace();
        trace2.setStartTime(2000000);

        trace2.setBusinessTransaction(BTXN_NAME);
        trace2.setId("trace2");
        trace2.setHostName("host2");
        trace2.setHostAddress("addr2");
        trace2.setPrincipal("p1");

        Consumer c2 = new Consumer();
        c2.setUri("SecondURI");
        c2.setDuration(1200000000);
        c2.getProperties().add(new Property("prop2", "value2"));

        CorrelationIdentifier cid2 = new CorrelationIdentifier();
        cid2.setScope(Scope.CausedBy);
        cid2.setValue("trace1:0:0");
        c2.getCorrelationIds().add(cid2);

        trace2.getNodes().add(c2);

        deriver.initialise(null, Collections.singletonList(trace1));
        deriver.initialise(null, Collections.singletonList(trace2));
        CommunicationDetails details = deriver.processOneToOne(null, trace2);

        assertNotNull(details);

        assertEquals(BTXN_NAME, details.getBusinessTransaction());
        assertEquals("FirstURI", details.getSource());
        assertEquals("SecondURI", details.getTarget());

        assertTrue(details.isMultiConsumer());

        assertTrue(c2.getDuration() == details.getConsumerDuration());
        assertTrue(comp1.getDuration() == details.getProducerDuration());

        long latency = trace2.getStartTime() - (trace1.getStartTime()
                + TimeUnit.MILLISECONDS.convert(comp1.getBaseTime(),  TimeUnit.NANOSECONDS));

        assertEquals(latency, details.getLatency());
        assertTrue(details.hasProperty("prop1"));
        assertTrue(details.hasProperty("prop2"));
        assertEquals("trace1", details.getSourceFragmentId());
        assertEquals("host1", details.getSourceHostName());
        assertEquals("addr1", details.getSourceHostAddress());
        assertEquals("trace2", details.getTargetFragmentId());
        assertEquals("host2", details.getTargetHostName());
        assertEquals("addr2", details.getTargetHostAddress());
        assertEquals("p1", details.getPrincipal());

        long timestamp = trace1.getStartTime() + TimeUnit.MILLISECONDS.convert(comp1.getBaseTime() -
                c1.getBaseTime(), TimeUnit.NANOSECONDS);
        assertEquals(timestamp, details.getTimestamp());
    }

    @Test
    public void testProcessSingleOutboundInteraction() throws RetryAttemptException {
        TestSourceInfoCache cache=new TestSourceInfoCache();

        CommunicationDetailsDeriver deriver = new CommunicationDetailsDeriver();
        deriver.setSourceInfoCache(cache);

        Trace trace1 = new Trace();
        trace1.setId("trace1");
        trace1.setStartTime(1000000);

        Consumer c1 = new Consumer();
        c1.setUri("FirstURI");
        c1.setBaseTime(0);
        trace1.getNodes().add(c1);

        Producer p1a = new Producer();
        p1a.setBaseTime(1000000);
        p1a.setDuration(2000000000);

        CorrelationIdentifier pid1 = new CorrelationIdentifier();
        pid1.setScope(Scope.Interaction);
        pid1.setValue("pid1");
        p1a.getCorrelationIds().add(pid1);

        c1.getNodes().add(p1a);

        Trace trace2 = new Trace();
        trace2.setId("trace2");
        trace2.setStartTime(2000000);

        Consumer c2 = new Consumer();
        c2.setUri("SecondURI");
        c2.setDuration(1200000000);

        CorrelationIdentifier cid2 = new CorrelationIdentifier();
        cid2.setScope(Scope.Interaction);
        cid2.setValue("pid1");
        c2.getCorrelationIds().add(cid2);

        trace2.getNodes().add(c2);

        Producer p2 = new Producer();
        p2.setBaseTime(1000000);
        p2.setDuration(2000000000);

        CorrelationIdentifier pid2 = new CorrelationIdentifier();
        pid2.setScope(Scope.Interaction);
        pid2.setValue("pid2");
        p2.getCorrelationIds().add(pid2);

        c2.getNodes().add(p2);

        Producer p3 = new Producer();
        p3.getDetails().put(Producer.DETAILS_PUBLISH, "true");
        p3.setBaseTime(1000000);
        p3.setDuration(2000000000);

        CorrelationIdentifier pid3 = new CorrelationIdentifier();
        pid3.setScope(Scope.ControlFlow);
        pid3.setValue("pid3");
        p3.getCorrelationIds().add(pid3);

        c2.getNodes().add(p3);

        deriver.initialise(null, Collections.singletonList(trace1));
        deriver.initialise(null, Collections.singletonList(trace2));
        CommunicationDetails details = deriver.processOneToOne(null, trace2);

        assertNotNull(details);

        assertEquals(5, details.getOutbound().size());
        assertTrue(details.getOutbound().get(0).getLinkIds().contains("trace2:0"));
        assertTrue(details.getOutbound().get(1).getLinkIds().contains("trace2:0:0"));
        assertTrue(details.getOutbound().get(2).getLinkIds().contains("pid2"));
        assertTrue(details.getOutbound().get(3).getLinkIds().contains("trace2:0:1"));
        assertTrue(details.getOutbound().get(4).getLinkIds().contains("pid3"));
        assertTrue(details.getOutbound().get(0).isMultiConsumer());
        assertTrue(details.getOutbound().get(1).isMultiConsumer());
        assertFalse(details.getOutbound().get(2).isMultiConsumer());
        assertTrue(details.getOutbound().get(3).isMultiConsumer());
        assertTrue(details.getOutbound().get(4).isMultiConsumer());
        assertEquals(0, details.getOutbound().get(0).getProducerOffset());
        assertEquals(1, details.getOutbound().get(1).getProducerOffset());
        assertEquals(1, details.getOutbound().get(2).getProducerOffset());
        assertEquals(1, details.getOutbound().get(3).getProducerOffset());
        assertEquals(1, details.getOutbound().get(4).getProducerOffset());
    }

    @Test
    public void testCalculateP2PLatency() {
        SourceInfo si = new SourceInfo();
        si.setDuration(TimeUnit.NANOSECONDS.convert(2000, TimeUnit.MILLISECONDS));

        Trace item = new Trace();

        Consumer consumer = new Consumer();
        consumer.setDuration(TimeUnit.NANOSECONDS.convert(1000, TimeUnit.MILLISECONDS));
        item.getNodes().add(consumer);

        long latency = TimeUnit.MILLISECONDS.convert(si.getDuration() - consumer.getDuration(), TimeUnit.NANOSECONDS) / 2;

        assertEquals(latency, CommunicationDetailsDeriver.calculateLatency(si, item, consumer));
    }

    @Test
    public void testCalculateP2PAsyncLatency() {
        SourceInfo si = new SourceInfo();
        si.setTimestamp(1000);
        si.setDuration(TimeUnit.NANOSECONDS.convert(2000, TimeUnit.MILLISECONDS));

        Trace item = new Trace();
        item.setStartTime(2000);

        Consumer consumer = new Consumer();
        consumer.setDuration(TimeUnit.NANOSECONDS.convert(3000, TimeUnit.MILLISECONDS));
        item.getNodes().add(consumer);

        long latency = item.getStartTime() - si.getTimestamp();

        assertEquals(latency, CommunicationDetailsDeriver.calculateLatency(si, item, consumer));
    }

    @Test
    public void testCalculateMultiConsumerLatency() {
        SourceInfo si = new SourceInfo();
        si.setMultipleConsumers(true);
        si.setTimestamp(1000);

        Trace item = new Trace();
        item.setStartTime(2000);

        Consumer consumer = new Consumer();
        item.getNodes().add(consumer);

        long latency = item.getStartTime() - si.getTimestamp();

        assertEquals(latency, CommunicationDetailsDeriver.calculateLatency(si, item, consumer));
    }

}
