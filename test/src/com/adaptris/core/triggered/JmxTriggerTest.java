/*
 * $RCSfile: TriggeredChannelTest.java,v $
 * $Revision: 1.7 $
 * $Date: 2009/03/10 13:44:39 $
 * $Author: lchan $
 */
package com.adaptris.core.triggered;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;

import com.adaptris.core.BaseCase;
import com.adaptris.core.ConfiguredConsumeDestination;
import com.adaptris.core.CoreException;
import com.adaptris.core.stubs.MockMessageListener;
import com.adaptris.core.util.LifecycleHelper;

public class JmxTriggerTest extends BaseCase {

  public JmxTriggerTest(java.lang.String testName) {
    super(testName);
  }

  @Override
  protected void setUp() throws Exception {
  }

  public void testStart() throws Exception {
    MockMessageListener stub = new MockMessageListener();
    JmxConsumer jmx = new JmxConsumer(new ConfiguredConsumeDestination("testJmxTrigger"));
    jmx.registerAdaptrisMessageListener(stub);
    try {
      start(jmx);
      MBeanServer server = findMBeanServer();
      ObjectName jmxTrigger = ObjectName.getInstance(JmxConsumer.JMX_OBJECT_NAME_PREFIX + "testJmxTrigger");
      server.invoke(jmxTrigger, JmxChannelTrigger.TRIGGER_OPERATION, null, null);
      assertEquals(1, stub.getMessages().size());
    }
    finally {
      stop(jmx);
    }
  }

  public void testUnavailableWhenStopped() throws Exception {
    MockMessageListener stub = new MockMessageListener();
    JmxConsumer jmx = new JmxConsumer(new ConfiguredConsumeDestination("testJmxTrigger"));
    jmx.registerAdaptrisMessageListener(stub);
    try {
      start(jmx);
      MBeanServer server = findMBeanServer();
      ObjectName jmxTrigger = ObjectName.getInstance(JmxConsumer.JMX_OBJECT_NAME_PREFIX + "testJmxTrigger");
      server.invoke(jmxTrigger, JmxChannelTrigger.TRIGGER_OPERATION, null, null);
      assertEquals(1, stub.getMessages().size());
      try {
        stop(jmx);
        server.invoke(jmxTrigger, JmxChannelTrigger.TRIGGER_OPERATION, null, null);
        fail("success unexpected");
      }
      catch (InstanceNotFoundException expected) {

      }
      assertEquals(1, stub.getMessages().size());
    }
    finally {
      stop(jmx);
    }
  }

  public void testInit() throws Exception {
    MockMessageListener stub = new MockMessageListener();
    JmxConsumer jmx = new JmxConsumer();
    jmx.registerAdaptrisMessageListener(stub);
    LifecycleHelper.init(jmx);
    try {
      LifecycleHelper.start(jmx);
      fail("Started with no consume Destination");
    }
    catch (CoreException expected) {

    }
    finally {
      LifecycleHelper.close(jmx);
    }
  }

  private MBeanServer findMBeanServer() {
    MBeanServer server = null;
    if (MBeanServerFactory.findMBeanServer(null).size() > 0) {
      server = MBeanServerFactory.findMBeanServer(null).get(0);
    }
    else {
      fail("No JMX mbeanServer.");
    }
    return server;
  }
}
