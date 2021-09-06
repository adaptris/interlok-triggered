package com.adaptris.core.triggered;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.adaptris.annotation.AdapterComponent;
import com.adaptris.annotation.AutoPopulated;
import com.adaptris.annotation.ComponentProfile;
import com.adaptris.core.AdaptrisMessage;
import com.adaptris.core.AdaptrisMessageFactory;
import com.adaptris.core.AdaptrisMessageListener;
import com.adaptris.core.AdaptrisPollingConsumer;
import com.adaptris.core.Channel;
import com.adaptris.core.ClosedState;
import com.adaptris.core.CoreException;
import com.adaptris.core.DefaultMessageFactory;
import com.adaptris.core.EventHandler;
import com.adaptris.core.Poller;
import com.adaptris.core.ProcessingExceptionHandler;
import com.adaptris.core.ProduceException;
import com.adaptris.core.Workflow;
import com.adaptris.core.licensing.License;
import com.adaptris.core.licensing.License.LicenseType;
import com.adaptris.core.licensing.LicenseChecker;
import com.adaptris.core.licensing.LicensedComponent;
import com.adaptris.core.util.LifecycleHelper;
import com.adaptris.core.util.LoggingHelper;
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
@AdapterComponent
@ComponentProfile(summary = "A Channel whose lifecycle is determined by an external configurable trigger", tag = "triggered")
public final class TriggeredChannel extends Channel implements
AdaptrisMessageListener, LicensedComponent {

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
    LicenseChecker.newChecker().checkLicense(this);
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
    startTime = new Date();
  }

  /**
   *
   * @see com.adaptris.core.ChannelList#stop()
   */
  @Override
  public void stop() {
    stopTime = new Date();
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
  public boolean isEnabled(License license) {
    return license.isEnabled(LicenseType.Standard);
  }


  // Trigger doesn't care about onSuccess / onFailure, it's job is just to start workflows when it
  // receives a msg.
  @Override
  public void onAdaptrisMessage(AdaptrisMessage msg, Consumer<AdaptrisMessage> onSuccess,
      Consumer<AdaptrisMessage> onFailure) {
    List<Thread> threads = new ArrayList<>();
    // Capture the last Starttime (because we stop/close).
    Date lastStartTime = lastStartTime();
    Date lastStopTime = lastStopTime();
    try {
      LifecycleHelper.initAndStart(retrieveActiveMsgErrorHandler());
      LifecycleHelper.initAndStart(getEventHandlerForMessages());
      super.init();
      LifecycleHelper.start(getProduceConnection());
      // It's valid to start the consume connection before the workflow
      // as the docs explicitly state you should be using a PollingConsumer.
      LifecycleHelper.start(getConsumeConnection());
      for (Iterator<Workflow> i = getWorkflowList().getWorkflows().iterator(); i
          .hasNext();) {
        WorkflowStarter wfs = new WorkflowStarter(i.next());
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
      LifecycleHelper.stopAndClose(getEventHandlerForMessages());
      startTime = lastStartTime;
      stopTime = lastStopTime;
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
    log.trace("Waiting for the MessageErrorHandler to process any failed messages");
    TriggeredProcessor t = (TriggeredProcessor) retrieveActiveMsgErrorHandler();
    while (!t.processingCompleted()) {
      try {
        Thread.sleep(ThreadLocalRandom.current().nextInt(1000));
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

  @Override
  public String friendlyName() {
    return LoggingHelper.friendlyName(this);
  }


  private class WorkflowStarter implements Runnable {
    private Workflow workflow;

    WorkflowStarter(Workflow w) {
      workflow = w;
    }

    @Override
    public void run() {
      try {
        LifecycleHelper.start(workflow);
        if (workflow.getConsumer() instanceof AdaptrisPollingConsumer) {
          Poller p = ((AdaptrisPollingConsumer) workflow.getConsumer()).getPoller();
          if (p instanceof OneTimePoller) {
            ((OneTimePoller) p).processMessages();
          }
        }
      }
      catch (CoreException e) {
        log.error("Failure to start workflow", e);
      }
    }

    public String createFriendlyThreadName() {
      return workflow.friendlyName();
    }
  }


}
