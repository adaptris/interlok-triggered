package com.adaptris.core.triggered;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;

import com.adaptris.core.AdaptrisMessageConsumerImp;
import com.adaptris.core.ConsumeDestination;
import com.adaptris.core.CoreException;
import com.adaptris.core.licensing.License;
import com.adaptris.core.licensing.License.LicenseType;
import com.adaptris.core.licensing.LicenseChecker;
import com.adaptris.core.licensing.LicensedComponent;
import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * Consumer type that can fires based on a JMX invocation.
 * 
 * <p>
 * The destination returned by {@link ConsumeDestination} is used as part of the {@link ObjectName}
 * </p>
 * 
 * @config triggered-jmx-consumer
 * 
 * @license STANDARD
 * @author lchan
 * 
 */
@XStreamAlias("triggered-jmx-consumer")
public class JmxConsumer extends AdaptrisMessageConsumerImp implements LicensedComponent {

  public static final String JMX_OBJECT_NAME_PREFIX = "Adaptris:type=TriggeredChannel, uid=";

  private transient String jmxUid = null;

  public JmxConsumer() {
    super();
  }

  public JmxConsumer(ConsumeDestination d) {
    this();
    setDestination(d);
  }

  @Override
  public void prepare() throws CoreException {
    LicenseChecker.newChecker().checkLicense(this);
  }

  @Override
  public boolean isEnabled(License license) {
    return license.isEnabled(LicenseType.Standard);
  }

  @Override
  public void close() {

  }

  private MBeanServer getMBeanServer() {
    if (MBeanServerFactory.findMBeanServer(null).size() > 0) {
      return MBeanServerFactory.findMBeanServer(null).get(0);
    }
    else {
      return MBeanServerFactory.createMBeanServer();
    }
  }

  private String getJmxUid() throws CoreException {
    if (getDestination() != null) {
      return getDestination().getDestination();
    }
    else {
      throw new CoreException("No Destination configured");
    }
  }

  @Override
  public void init() throws CoreException {

  }

  @Override
  public void start() throws CoreException {

    try {
      MBeanServer mBeanServer = getMBeanServer();

      ObjectName adapterName = ObjectName.getInstance(JMX_OBJECT_NAME_PREFIX + getJmxUid());
      mBeanServer.registerMBean(new JmxChannelTrigger(this), adapterName);
    }
    catch (Exception e) {
      if (e instanceof CoreException) {
        throw (CoreException) e;
      }
      else {
        throw new CoreException(e);
      }
    }
  }

  @Override
  public void stop() {
    MBeanServer mBeanServer = getMBeanServer();
    try {
      mBeanServer.unregisterMBean(ObjectName.getInstance(JMX_OBJECT_NAME_PREFIX + getJmxUid()));
    }
    catch (Exception e) {
      ;
    }
  }

  String threadName() {
    return retrieveAdaptrisMessageListener().friendlyName();
  }
}
