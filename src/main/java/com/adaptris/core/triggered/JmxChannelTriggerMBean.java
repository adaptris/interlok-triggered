package com.adaptris.core.triggered;

/**
 * <p>
 * <code>MBean</code> which acts as a trigger for a {@link TriggeredChannel} using JMX.
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
