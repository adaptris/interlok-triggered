package com.adaptris.core.triggered;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;
import javax.validation.Valid;
import com.adaptris.validation.constraints.ConfigDeprecated;
import com.adaptris.core.AdaptrisMessageConsumerImp;
import com.adaptris.core.ConsumeDestination;
import com.adaptris.core.CoreException;
import com.adaptris.core.licensing.License;
import com.adaptris.core.licensing.License.LicenseType;
import com.adaptris.core.licensing.LicenseChecker;
import com.adaptris.core.licensing.LicensedComponent;
import com.adaptris.core.util.DestinationHelper;
import com.adaptris.core.util.LoggingHelper;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
@NoArgsConstructor
public class JmxConsumer extends AdaptrisMessageConsumerImp implements LicensedComponent {

  public static final String JMX_OBJECT_NAME_PREFIX = "Adaptris:type=TriggeredChannel, uid=";
  /**
   * The consume destination is used to build up the JMX object name.
   *
   */
  @Deprecated
  @Valid
  @ConfigDeprecated(removalVersion = "4.0.0", message = "use the 'unique-id' instead", groups = Deprecated.class)
  @Getter
  @Setter
  private ConsumeDestination destination;

  private transient String jmxUid = null;
  private transient boolean destinationWarningLogged;

  @Override
  public void prepare() throws CoreException {
    DestinationHelper.logConsumeDestinationWarning(destinationWarningLogged,
        () -> destinationWarningLogged = true, getDestination(),
        "{} uses destination, remove it and default to the unique-id instead",
        LoggingHelper.friendlyName(this));
    DestinationHelper.mustHaveEither(getUniqueId(), getDestination());
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
    return DestinationHelper.consumeDestination(getUniqueId(), getDestination());
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

  @Override
  protected String newThreadName() {
    return DestinationHelper.threadName(retrieveAdaptrisMessageListener(), getDestination());
  }
}
