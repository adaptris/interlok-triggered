package com.adaptris.core.triggered;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.adaptris.annotation.AutoPopulated;
import com.adaptris.core.AdaptrisMessage;
import com.adaptris.core.AdaptrisMessageFactory;
import com.adaptris.core.AdaptrisMessageListener;
import com.adaptris.core.AdaptrisPollingConsumer;
import com.adaptris.core.Channel;
import com.adaptris.core.ClosedState;
import com.adaptris.core.ComponentState;
import com.adaptris.core.ConsumeDestination;
import com.adaptris.core.CoreException;
import com.adaptris.core.DefaultMessageFactory;
import com.adaptris.core.EventHandler;
import com.adaptris.core.ProcessingExceptionHandler;
import com.adaptris.core.ProduceException;
import com.adaptris.core.Workflow;
import com.adaptris.core.util.LifecycleHelper;
import com.adaptris.util.license.License;
import com.adaptris.util.license.License.LicenseType;
import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * A Channel whose lifecycle is determined by an external configurable trigger.
 * <p>
 * Workflows configured as part of a TriggeredChannel do not participate in the normal adapter lifecycle until such time as its
 * trigger consumer consumes a message. At that point, the underlying connections and workflows are initialised, and started.
 * </p>
 * <p>
 * After starting the workflows, the channel blocks and waits for the workflows to terminate, so that it can accept the next
 * trigger. As a result of this behaviour, it only makes sense for the workflows to contain consumers that actively poll the
 * back-end system such as {@link AdaptrisPollingConsumer}, rather than consumers that passively wait for activity. Failure to
 * follow these guidelines may lead to undefined behaviour.
 * </p>
 * <p>
 * Even when using an {@link AdaptrisPollingConsumer} implementation the {@link com.adaptris.core.Poller} mechanism that should be
 * used should have a finite lifespan such as {@link OneTimePoller} so that the workflows terminate as expected. If you opt not to
 * use a poller with a finite lifespan then behaviour may be undefined.
 * </p>
 * <p>
 * This type of channel also handles errors and events differently. A specific EventHandler may be specified to physically handle
 * MessageLifecycleEvents that are created within the channel. If this is unspecified then the Adapter's EventHandler is used. The
 * rationale behind this behaviour is to treat events as standard AdaptrisMessage objects which then allows message error handling
 * logic to take place.
 * </p>
 * <p>
 * Any {@link com.adaptris.core.ProcessingExceptionHandler} implementation that is used with this Channel implementation must also
 * implement the {@link TriggeredProcessor} interface, which simply allows this Channel to interrogate whether processing has been
 * completed by the component or not. This ensures that the channel is not stopped prior to any messages that need to be retried
 * from being retried, thus failing the messages before their time.
 * </p>
 * 
 * @config triggered-channel
 * 
 * @license STANDARD
 * @see OneTimePoller
 * @see AdaptrisPollingConsumer
 * @see TriggeredProcessor
 * @author lchan
 * @author $Author: lchan $
 */
@XStreamAlias("triggered-channel")
public final class TriggeredChannel extends Channel implements
    AdaptrisMessageListener {

  @NotNull
  @Valid
  @AutoPopulated
  private Trigger trigger;
  @NotNull
  @Valid
  @AutoPopulated
  private AdaptrisMessageFactory messageFactory;
  private EventHandler eventHandlerForMessages;

  /**
   * Default Constructor.
   * <ol>
   * <li>message-error-handler is {@link RetryMessageErrorHandler}</li>
   * </ol>
   */
  public TriggeredChannel() {
    super();
    setTrigger(new Trigger());
    setMessageFactory(new DefaultMessageFactory());
    ProcessingExceptionHandler meh = new RetryMessageErrorHandler();
    setMessageErrorHandler(meh);
    registerActiveMsgErrorHandler(meh);
    super.changeState(ClosedState.getInstance());
  }

  @Override
  public void changeState(ComponentState s) {
    // Do nothing, as this is called from super lifecycle methods
    // and our own lifecycle takes precedence...
  }

  /**
   *
   * @see com.adaptris.core.ChannelList#init()
   */
  @Override
  public void init() throws CoreException {
    if (!(retrieveActiveMsgErrorHandler() instanceof TriggeredProcessor)) {
      throw new CoreException("TriggeredChannel may only contain "
          + TriggeredProcessor.class
          + " implementations as its MessageErrorHandler");
    }
    trigger.getConsumer().registerAdaptrisMessageListener(this);
    LifecycleHelper.init(trigger);

  }

  @Override
  public void prepare() throws CoreException {
    if (getEventHandlerForMessages() != null) {
      log.info("EventHandler configured, bypassing Adapters event handler");
      registerEventHandler(getEventHandlerForMessages());
    }
    super.prepare();
  }

  /**
   *
   * @see com.adaptris.core.ChannelList#start()
   */
  @Override
  public void start() throws CoreException {
    LifecycleHelper.start(trigger);
  }

  /**
   *
   * @see com.adaptris.core.ChannelList#stop()
   */
  @Override
  public void stop() {
    LifecycleHelper.stop(trigger);
  }

  /**
   *
   * @see com.adaptris.core.ChannelList#close()
   */
  @Override
  public void close() {
    LifecycleHelper.close(trigger);
  }

  @Override
  public boolean isEnabled(License license) throws CoreException {
    return license.isEnabled(LicenseType.Standard) && super.isEnabled(license) && trigger.isEnabled(license)
        && (getEventHandlerForMessages() != null ? getEventHandlerForMessages().isEnabled(license) : true);
  }

  /**
   *
   * @see AdaptrisMessageListener#onAdaptrisMessage(AdaptrisMessage)
   */
  public synchronized void onAdaptrisMessage(AdaptrisMessage msg) {
    List<Thread> threads = new ArrayList<Thread>();
    try {
      if (getEventHandlerForMessages() != null) {
        LifecycleHelper.start(eventHandler);
      }
      super.init();
      LifecycleHelper.start(getProduceConnection());
      // It's valid to start the consume connection before the workflow
      // as the docs explicitly state you should be using a PollingConsumer.
      LifecycleHelper.start(getConsumeConnection());
      for (Iterator<Workflow> i = getWorkflowList().getWorkflows().iterator(); i
          .hasNext();) {
        WorkflowStarter wfs = new WorkflowStarter((Workflow) i.next());
        Thread t = new Thread(wfs);
        t.setName(wfs.createFriendlyThreadName());
        threads.add(t);
        log.trace("Starting " + t.getName());
        t.start();
      }
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
    finally {
      waitForThreads(threads);
      waitForErrorHandler();
      super.stop();
      super.close();
      if (getEventHandlerForMessages() != null) {
        LifecycleHelper.stop(eventHandler);
        LifecycleHelper.close(eventHandler);
      }
    }
    log.trace("Trigger processing complete");
    try {
      getTrigger().getProducer().produce(msg);
    }
    catch (ProduceException e) {
      throw new RuntimeException(e);
    }
  }

  private void waitForErrorHandler() {
    log.trace("Waiting for the MessageErrorHandler to "
        + "process any failed messages");
    TriggeredProcessor t = (TriggeredProcessor) retrieveActiveMsgErrorHandler();
    while (!t.processingCompleted()) {
      try {
        Thread.sleep(new Random().nextInt(1000));
      }
      catch (InterruptedException e) {
        ;
      }
    }
  }

  private void waitForThreads(List<Thread> threads) {
    log.trace("Waiting for Processing to complete");
    for (Thread t : threads) {
      if (t.isAlive()) {
        try {
          t.join();
        }
        catch (InterruptedException e) {
          throw new RuntimeException(e);
        }
      }
    }
  }


  /**
   * @return the triggerComponent
   */
  public Trigger getTrigger() {
    return trigger;
  }

  /**
   * @param t the triggerComponent
   */
  public void setTrigger(Trigger t) {
    trigger = t;
  }

  private static String getFriendlyClassName() {
    String className = TriggeredChannel.class.getName();
    int dot = className.lastIndexOf(".");
    if (dot > 0) {
      className = className.substring(dot + 1);
    }
    return className;
  }

  /**
   * @return the messageFactory
   */
  public AdaptrisMessageFactory getMessageFactory() {
    return messageFactory;
  }

  /**
   * Set the message factory used when creating AdaptrisMessage.
   *
   * @param f the messageFactory to set
   */
  public void setMessageFactory(AdaptrisMessageFactory f) {
    messageFactory = f;
  }

  private class WorkflowStarter implements Runnable {
    private Workflow workflow;

    WorkflowStarter(Workflow w) {
      workflow = w;
    }

    public void run() {
      try {
        LifecycleHelper.start(workflow);
      }
      catch (CoreException e) {
        log.error("Failure to start workflow", e);
      }
    }

    public String createFriendlyThreadName() {
      ConsumeDestination cd = workflow.getConsumer().getDestination();
      if (cd != null) {
        return cd.getDeliveryThreadName();
      }
      return getFriendlyClassName() + "@"
          + Integer.toHexString(hashCode());
    }
  }

  /**
   * @return the eventHandlerForMessages
   */
  public EventHandler getEventHandlerForMessages() {
    return eventHandlerForMessages;
  }

  /**
   * @param eh the eventHandlerForMessages to set
   */
  public void setEventHandlerForMessages(EventHandler eh) {
    eventHandlerForMessages = eh;
  }
}
