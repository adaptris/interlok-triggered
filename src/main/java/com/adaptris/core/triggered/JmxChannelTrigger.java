package com.adaptris.core.triggered;

import static com.adaptris.core.AdaptrisMessageFactory.defaultIfNull;
import com.adaptris.core.AdaptrisMessage;

/**
 * Implementation of {@link JmxChannelTriggerMBean} that triggers the underlying
 * channel.
 *
 * @author lchan
 *
 */
public class JmxChannelTrigger implements JmxChannelTriggerMBean {

  private transient JmxConsumer owner;
  public static final String TRIGGER_OPERATION = "trigger";

  public JmxChannelTrigger(JmxConsumer listener) {
    owner = listener;
  }

  @Override
  public void trigger() {
    String oldName = Thread.currentThread().getName();

    try {
      Thread.currentThread().setName(owner.newThreadName());
      AdaptrisMessage msg = defaultIfNull(owner.getMessageFactory()).newMessage();
      owner.retrieveAdaptrisMessageListener().onAdaptrisMessage(msg);
    }
    finally {
      Thread.currentThread().setName(oldName);
    }
  }
}
