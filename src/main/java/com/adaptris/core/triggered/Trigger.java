/*
 * $RCSfile: Trigger.java,v $
 * $Revision: 1.3 $
 * $Date: 2008/05/20 11:11:59 $
 * $Author: lchan $
 */
package com.adaptris.core.triggered;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.adaptris.annotation.AdapterComponent;
import com.adaptris.annotation.ComponentProfile;
import com.adaptris.core.AdaptrisMessageProducer;
import com.adaptris.core.CoreException;
import com.adaptris.core.NullConnection;
import com.adaptris.core.NullMessageConsumer;
import com.adaptris.core.NullMessageProducer;
import com.adaptris.core.StandaloneConsumer;
import com.adaptris.core.licensing.License;
import com.adaptris.core.licensing.License.LicenseType;
import com.adaptris.core.licensing.LicenseChecker;
import com.adaptris.core.licensing.LicensedComponent;
import com.adaptris.core.util.LifecycleHelper;
import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * Component that triggers an instance of TriggeredChannel
 *
 * @config trigger
 *
 * @license STANDARD
 * @author lchan
 * @author $Author: lchan $
 */
@XStreamAlias("trigger")
@AdapterComponent
@ComponentProfile(summary = "Standalone wrapper for a consumer and connection and is intended for use within a TriggeredChannel", tag = "consumer,triggered")
public class Trigger extends StandaloneConsumer implements LicensedComponent {

  @NotNull
  @Valid
  private AdaptrisMessageProducer producer;

  public Trigger() {
    setConsumer(new NullMessageConsumer());
    setProducer(new NullMessageProducer());
    setConnection(new NullConnection());
  }

  @Override
  public void close() {
    LifecycleHelper.close(getConsumer());
    LifecycleHelper.close(getProducer());
    LifecycleHelper.close(getConnection());
  }

  @Override
  public void init() throws CoreException {
    getConnection().addMessageProducer(getProducer());
    getConnection().addMessageConsumer(getConsumer());
    LifecycleHelper.init(getConnection());
    LifecycleHelper.init(getProducer());
    LifecycleHelper.init(getConsumer());
  }

  @Override
  public void prepare() throws CoreException {
    LicenseChecker.newChecker().checkLicense(this);
    super.prepare();
  }

  @Override
  public boolean isEnabled(License license) {
    return license.isEnabled(LicenseType.Standard);
  }

  @Override
  public void start() throws CoreException {
    LifecycleHelper.start(getConnection());
    LifecycleHelper.start(getProducer());
    LifecycleHelper.start(getConsumer());
  }

  @Override
  public void stop() {
    LifecycleHelper.stop(getConsumer());
    LifecycleHelper.stop(getProducer());
    LifecycleHelper.stop(getConnection());
  }

  /**
   * @return the producer
   */
  public AdaptrisMessageProducer getProducer() {
    return producer;
  }

  /**
   * @param p
   *          the producer to set
   */
  public void setProducer(AdaptrisMessageProducer p) {
    producer = p;
  }

}
