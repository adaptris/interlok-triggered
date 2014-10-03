/*
 * $RCSfile: OneTimePoller.java,v $
 * $Revision: 1.2 $
 * $Date: 2008/04/29 12:17:48 $
 * $Author: lchan $
 */
package com.adaptris.core.triggered;

import com.adaptris.core.CoreException;
import com.adaptris.core.PollerImp;
import com.adaptris.util.license.License;
import com.adaptris.util.license.License.LicenseType;
import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * <p>
 * Implementation of <code>Poller</code> which only polls once upon start and never again.
 * </p>
 * 
 * @config triggered-one-time-poller
 * @license STANDARD
 */
@XStreamAlias("triggered-one-time-poller")
public class OneTimePoller extends PollerImp {

  @Override
  public boolean isEnabled(License license) throws CoreException {
    return license.isEnabled(LicenseType.Standard);
  }

  /**
   * <p>
   * Creates a new instance.
   * </p>
   */
  public OneTimePoller() {
  }

  /** @see com.adaptris.core.AdaptrisComponent#init() */
  @Override
  public void init() {
    // do nothing...
  }

  /** @see com.adaptris.core.AdaptrisComponent#start() */
  @Override
  public void start() {
    log.trace("Processing Messages");
    processMessages();
  }

  /** @see com.adaptris.core.AdaptrisComponent#stop() */
  @Override
  public void stop() {
    ;
  }

  /** @see com.adaptris.core.AdaptrisComponent#close() */
  @Override
  public void close() {
    // na...
  }

}
