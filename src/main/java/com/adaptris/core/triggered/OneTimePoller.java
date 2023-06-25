/*
 * $RCSfile: OneTimePoller.java,v $
 * $Revision: 1.2 $
 * $Date: 2008/04/29 12:17:48 $
 * $Author: lchan $
 */
package com.adaptris.core.triggered;

import com.adaptris.core.CoreException;
import com.adaptris.core.PollerImp;
import com.adaptris.core.licensing.License;
import com.adaptris.core.licensing.License.LicenseType;
import com.adaptris.core.licensing.LicenseChecker;
import com.adaptris.core.licensing.LicensedComponent;
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
public class OneTimePoller extends PollerImp implements LicensedComponent {

  @Override
  public boolean isEnabled(License license) {
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
  public void init() throws CoreException {
    LicenseChecker.newChecker().checkLicense(this);
    super.init();
  }

  @Override
  public void processMessages() {
    log.trace("Processing Messages");
    super.processMessages();
  }

}
