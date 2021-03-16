package com.adaptris.core.triggered;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;

import com.adaptris.annotation.AdapterComponent;
import com.adaptris.annotation.ComponentProfile;
import com.adaptris.core.AdaptrisMessageConsumerImp;
import com.adaptris.core.CoreException;
import com.adaptris.core.licensing.License;
import com.adaptris.core.licensing.License.LicenseType;
import com.adaptris.core.licensing.LicenseChecker;
import com.adaptris.core.licensing.LicensedComponent;
import com.adaptris.core.util.DestinationHelper;
import com.adaptris.interlok.util.Args;
import com.thoughtworks.xstream.annotations.XStreamAlias;

import lombok.NoArgsConstructor;

/**
 * Consumer type that can fires based on a JMX invocation.
 *
 * <p>
 * The unique id is used as part of the {@link ObjectName} {@value #JMX_OBJECT_NAME_PREFIX}
 * </p>
 *
 * @config triggered-jmx-consumer
 *
 * @license STANDARD
 * @author lchan
 *
 */
@XStreamAlias("triggered-jmx-consumer")
@AdapterComponent
@ComponentProfile(summary = "Consumer type that can fires based on a JMX invocation", tag = "consumer,jmx,triggered")
@NoArgsConstructor
public class JmxConsumer extends AdaptrisMessageConsumerImp implements LicensedComponent {

  public static final String JMX_OBJECT_NAME_PREFIX = "Adaptris:type=TriggeredChannel, uid=";

  @Override
  public void prepare() throws CoreException {
    Args.notNull(getUniqueId(), "unique-id");

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

  private String getJmxUid() {
    return getUniqueId();
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
    } catch (Exception e) {
      if (e instanceof CoreException) {
        throw (CoreException) e;
      } else {
        throw new CoreException(e);
      }
    }
  }

  @Override
  public void stop() {
    MBeanServer mBeanServer = getMBeanServer();
    try {
      mBeanServer.unregisterMBean(ObjectName.getInstance(JMX_OBJECT_NAME_PREFIX + getJmxUid()));
    } catch (Exception e) {
      ;
    }
  }

  @Override
  protected String newThreadName() {
    return DestinationHelper.threadName(retrieveAdaptrisMessageListener());
  }
}
