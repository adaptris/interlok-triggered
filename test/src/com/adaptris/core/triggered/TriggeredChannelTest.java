/*
 * $RCSfile: TriggeredChannelTest.java,v $
 * $Revision: 1.7 $
 * $Date: 2009/03/10 13:44:39 $
 * $Author: lchan $
 */
package com.adaptris.core.triggered;

import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;

import com.adaptris.core.Adapter;
import com.adaptris.core.AdaptrisMessage;
import com.adaptris.core.AdaptrisMessageConsumer;
import com.adaptris.core.AdaptrisMessageFactory;
import com.adaptris.core.AdaptrisMessageProducer;
import com.adaptris.core.ChannelList;
import com.adaptris.core.ClosedState;
import com.adaptris.core.ComponentState;
import com.adaptris.core.ConfiguredConsumeDestination;
import com.adaptris.core.ConfiguredProduceDestination;
import com.adaptris.core.CoreException;
import com.adaptris.core.DefaultEventHandler;
import com.adaptris.core.ExampleChannelCase;
import com.adaptris.core.PollingTrigger;
import com.adaptris.core.StandaloneProducer;
import com.adaptris.core.StandardWorkflow;
import com.adaptris.core.Workflow;
import com.adaptris.core.XStreamMarshaller;
import com.adaptris.core.fs.FsConsumer;
import com.adaptris.core.jms.JmsConnection;
import com.adaptris.core.jms.PtpProducer;
import com.adaptris.core.jms.jndi.StandardJndiImplementation;
import com.adaptris.core.stubs.FailFirstMockMessageProducer;
import com.adaptris.core.stubs.MockEventHandlerWithState;
import com.adaptris.core.stubs.MockMessageConsumer;
import com.adaptris.core.stubs.MockMessageProducer;
import com.adaptris.core.util.LifecycleHelper;
import com.adaptris.util.GuidGenerator;
import com.adaptris.util.TimeInterval;

public class TriggeredChannelTest extends ExampleChannelCase {

  private TriggeredChannel channel;
  private Adapter adapter;
  private String triggeredWorkflowKey;
  private MockEventHandlerWithState adapterEventHandler;

  public TriggeredChannelTest(java.lang.String testName) {
    super(testName);
  }

  @Override
  protected void setUp() throws Exception {
    adapterEventHandler = new MockEventHandlerWithState("AdapterEventHandler");
    adapter = new Adapter();
    adapter.setEventHandler(adapterEventHandler);
    adapter.setUniqueId(new GuidGenerator().getUUID());
    channel = createTriggeredChannel(new MockMessageConsumer());
    adapter.getChannelList().addChannel(channel);
  }

  public void testJmxTrigger() throws Exception {
    JmxConsumer jmx = new JmxConsumer(new ConfiguredConsumeDestination("testJmxTrigger"));
    channel.getTrigger().setConsumer(jmx);
    adapter.requestStart();
    StandardWorkflow twf = findWorkflow(channel.getWorkflowList().getWorkflows(), triggeredWorkflowKey);
    MockMessageProducer tp = (MockMessageProducer) twf.getProducer();
    MockMessageProducer ep = (MockMessageProducer) ((DefaultEventHandler) channel.getEventHandlerForMessages()).getProducer();
    MBeanServer mBeanServer = null;
    if (MBeanServerFactory.findMBeanServer(null).size() > 0) {
      mBeanServer = MBeanServerFactory.findMBeanServer(null).get(0);
    }
    else {
      fail("No JMX mbeanServer.");
    }
    ObjectName jmxTrigger = ObjectName.getInstance(JmxConsumer.JMX_OBJECT_NAME_PREFIX + "testJmxTrigger");
    assertWorkflowState(channel.getWorkflowList().getWorkflows(), ClosedState.getInstance());
    assertEquals(ClosedState.getInstance(), channel.getEventHandlerForMessages().retrieveComponentState());

    mBeanServer.invoke(jmxTrigger, JmxChannelTrigger.TRIGGER_OPERATION, null, null);
    assertEquals("Number of messages produced", 1, tp.getMessages().size());
    assertEquals("Number of lifecycle events", 1, ep.getMessages().size());
    assertEquals("Trigger message produced", 1, ((MockMessageProducer) channel.getTrigger().getProducer()).getMessages().size());
    checkMessagePayloads(tp.getMessages());

    assertWorkflowState(channel.getWorkflowList().getWorkflows(), ClosedState.getInstance());
    assertEquals(ClosedState.getInstance(), channel.getEventHandlerForMessages().retrieveComponentState());
    adapter.requestClose();
  }

  public void testTrigger() throws Exception {
    adapter.requestStart();
    StandardWorkflow twf = findWorkflow(channel.getWorkflowList().getWorkflows(), triggeredWorkflowKey);

    MockMessageConsumer mc = (MockMessageConsumer) channel.getTrigger().getConsumer();
    MockMessageProducer tp = (MockMessageProducer) twf.getProducer();
    MockMessageProducer ep = (MockMessageProducer) ((DefaultEventHandler) channel.getEventHandlerForMessages()).getProducer();

    assertWorkflowState(channel.getWorkflowList().getWorkflows(), ClosedState.getInstance());
    assertEquals(ClosedState.getInstance(), channel.getEventHandlerForMessages().retrieveComponentState());
    mc.submitMessage(AdaptrisMessageFactory.getDefaultInstance().newMessage());
    Thread.sleep(500);

    assertEquals("Number of messages produced", 1, tp.getMessages().size());
    assertEquals("Number of lifecycle events", 1, ep.getMessages().size());
    assertEquals("Trigger message produced", 1, ((MockMessageProducer) channel.getTrigger().getProducer())
        .getMessages().size());
    checkMessagePayloads(tp.getMessages());

    assertWorkflowState(channel.getWorkflowList().getWorkflows(), ClosedState.getInstance());
    assertEquals(ClosedState.getInstance(), channel.getEventHandlerForMessages().retrieveComponentState());
    adapter.requestClose();
  }

  public void testTriggerWithFailure() throws Exception {
    LifecycleHelper.initAndStart(adapter);
    LifecycleHelper.stopAndClose(adapter);
    StandardWorkflow twf = findWorkflow(channel.getWorkflowList().getWorkflows(), triggeredWorkflowKey);
    MockMessageConsumer mc = (MockMessageConsumer) channel.getTrigger().getConsumer();
    MockMessageProducer tp = new FailFirstMockMessageProducer();
    twf.setProducer(tp);
    LifecycleHelper.initAndStart(adapter);
    mc.submitMessage(AdaptrisMessageFactory.getDefaultInstance().newMessage());
    waitForMessages(tp, 1);
    assertEquals("Number of messages produced", 1, tp.getMessages().size());
    assertEquals("Trigger message produced", 1, ((MockMessageProducer) channel.getTrigger().getProducer()).getMessages().size());
    checkMessagePayloads(tp.getMessages());

    assertWorkflowState(channel.getWorkflowList().getWorkflows(), ClosedState.getInstance());
    assertEquals(ClosedState.getInstance(), channel.getEventHandlerForMessages().retrieveComponentState());
    LifecycleHelper.stopAndClose(adapter);
  }

  private void assertWorkflowState(List l, ComponentState state) {
    for (Iterator i = l.iterator(); i.hasNext();) {
      StandardWorkflow wf = (StandardWorkflow) i.next();
      assertEquals("State of workflow " + wf.obtainWorkflowId(), state, wf.retrieveComponentState());
    }
  }

  private void checkMessagePayloads(List l) {
    for (Iterator i = l.iterator(); i.hasNext();) {
      AdaptrisMessage m = (AdaptrisMessage) i.next();
      assertEquals("The quick brown fox", m.getStringPayload());
    }
  }

  @Override
  protected Object retrieveObjectForSampleConfig() {
    TriggeredChannel c = new TriggeredChannel();
    c.setUniqueId(UUID.randomUUID().toString());
    ChannelList cl = new ChannelList();
    try {
      c.setTrigger(createTriggerForConfig());
      c.getWorkflowList().add(createDefaultWorkflowForConfig());
      DefaultEventHandler sceh = new DefaultEventHandler();
      sceh.setMarshaller(new XStreamMarshaller());
      c.setEventHandlerForMessages(sceh);
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
    cl.addChannel(c);
    return cl;
  }

  private Workflow createDefaultWorkflowForConfig() throws CoreException {
    StandardWorkflow wf = new StandardWorkflow();
    wf.setUniqueId(UUID.randomUUID().toString());
    StandaloneProducer ep = new StandaloneProducer();
    ep.setConnection(new JmsConnection(new StandardJndiImplementation("MyJndiName")));
    PtpProducer ptp = new PtpProducer();
    ptp.setDestination(new ConfiguredProduceDestination("dest"));
    ep.setProducer(ptp);
    ConfiguredConsumeDestination ccd = new ConfiguredConsumeDestination();
    ccd.setDestination("file:./fs/in");
    FsConsumer fsc = new FsConsumer();
    fsc.setPoller(new OneTimePoller());
    fsc.setDestination(ccd);
    wf.setConsumer(fsc);
    wf.getServiceCollection().addService(ep);
    return wf;
  }

  private Trigger createTriggerForConfig() {
    JmxConsumer jmxConsumer = new JmxConsumer();
    jmxConsumer.setDestination(new ConfiguredConsumeDestination("jmx_trigger"));
    Trigger t = new Trigger();
    t.setConsumer(jmxConsumer);
    return t;
  }

  @Override
  protected String createBaseFileName(Object object) {
    return TriggeredChannel.class.getName();
  }

  private TriggeredChannel createTriggeredChannel(AdaptrisMessageConsumer consumer) throws CoreException {
    TriggeredChannel channel = new TriggeredChannel();
    channel.setUniqueId("TriggeredChannel");
    channel.setTrigger(createTestTrigger(consumer, new MockMessageProducer()));
    Workflow triggerWorkflow = createTriggeredWorkflow();
    triggeredWorkflowKey = triggerWorkflow.getUniqueId() + "@" + channel.getUniqueId();
    channel.getWorkflowList().add(triggerWorkflow);
    channel.setEventHandlerForMessages(new MockEventHandlerWithState("TriggeredChannelEventHandler"));
    RetryMessageErrorHandler rmeh = new RetryMessageErrorHandler();
    rmeh.setRetryInterval(new TimeInterval(1L, TimeUnit.SECONDS));
    channel.setMessageErrorHandler(rmeh);
    return channel;
  }

  private Trigger createTestTrigger(AdaptrisMessageConsumer consumer, AdaptrisMessageProducer producer) {
    Trigger sc = new Trigger();
    sc.setConsumer(consumer);
    sc.setProducer(producer);
    return sc;
  }

  private Workflow createTriggeredWorkflow() {
    MockMessageProducer triggeredProducer = new MockMessageProducer();
    StandardWorkflow triggeredWorkflow = new StandardWorkflow();
    PollingTrigger pt = new PollingTrigger();
    pt.setDestination(new ConfiguredConsumeDestination("Trigger"));
    pt.setPoller(new OneTimePoller());
    pt.setTemplate("The quick brown fox");
    triggeredWorkflow.setConsumer(pt);
    triggeredWorkflow.setProducer(triggeredProducer);
    triggeredWorkflow.setUniqueId("TriggeredWorkflow");
    return triggeredWorkflow;
  }

  private StandardWorkflow findWorkflow(List l, String key) {
    StandardWorkflow result = null;

    for (Iterator i = l.iterator(); i.hasNext();) {
      StandardWorkflow w = (StandardWorkflow) i.next();
      if (key.equals(w.obtainWorkflowId())) {
        result = w;
        break;
      }
    }
    return result;
  }
}