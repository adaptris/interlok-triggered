/*
 * $RCSfile: AdapterControllerMBean.java,v $
 * $Revision: 1.7 $
 * $Date: 2008/02/19 12:03:59 $
 * $Author: lchan $
 */
package com.adaptris.core.triggered;


/**
 * <p>
 * <code>MBean</code> which acts as a trigger for a {@link TriggeredChannel}
 * using JMX.
 * </p>
 */
public interface JmxChannelTriggerMBean {

  /**
   * <p>
   * Triggers the channel to start.
   * </p>
   *
   */
  void trigger();

}
