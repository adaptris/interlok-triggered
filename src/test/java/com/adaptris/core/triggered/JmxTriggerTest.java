/*
 * $RCSfile: TriggeredChannelTest.java,v $
 * $Revision: 1.7 $
 * $Date: 2009/03/10 13:44:39 $
 * $Author: lchan $
 */
package com.adaptris.core.triggered;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;
import org.junit.Test;
import com.adaptris.core.BaseCase;
import com.adaptris.core.stubs.MockMessageListener;
import com.adaptris.core.util.LifecycleHelper;

public class JmxTriggerTest extends BaseCase {
  @Override
  public boolean isAnnotatedForJunit4() {
    return true;
  }
  @Test
  public void testStart() throws Exception {
    MockMessageListener stub = new MockMessageListener();
    JmxConsumer jmx = new JmxConsumer();
    jmx.setUniqueId("testJmxTrigger");
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

  @Test
  public void testUnavailableWhenStopped() throws Exception {
    MockMessageListener stub = new MockMessageListener();
    JmxConsumer jmx = new JmxConsumer();
    jmx.setUniqueId("testJmxTrigger");
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

  @Test
  public void testLifecycle() throws Exception {
    MockMessageListener stub = new MockMessageListener();
    JmxConsumer jmx = new JmxConsumer();
    jmx.registerAdaptrisMessageListener(stub);
    try {
      LifecycleHelper.initAndStart(jmx);
      fail();
    } catch (Exception expected) {
      // had no unique-id
    }
    finally {
      LifecycleHelper.stopAndClose(jmx);
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
