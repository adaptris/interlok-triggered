/*
 * $RCSfile: OneTimePoller.java,v $
 * $Revision: 1.2 $
 * $Date: 2008/04/29 12:17:48 $
 * $Author: lchan $
 */
package com.adaptris.core.triggered;

import com.adaptris.core.PollerImp;
import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * <p>
 * Implementation of <code>Poller</code> which only polls once upon start and never again.
 * </p>
 *
 * @config triggered-one-time-poller
 */
@XStreamAlias("triggered-one-time-poller")
public class OneTimePoller extends PollerImp {

  /**
   * <p>
   * Creates a new instance.
   * </p>
   */
  public OneTimePoller() {
  }

  @Override
  public void processMessages() {
    log.trace("Processing Messages");
    super.processMessages();
  }

}
