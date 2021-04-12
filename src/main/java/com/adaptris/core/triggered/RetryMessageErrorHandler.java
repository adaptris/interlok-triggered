/*
 * $RCSfile: RetryMessageErrorHandler.java,v $
 * $Revision: 1.5 $
 * $Date: 2008/09/11 10:02:51 $
 * $Author: lchan $
 */
package com.adaptris.core.triggered;

import java.util.List;
import java.util.concurrent.TimeUnit;

import com.adaptris.annotation.AdapterComponent;
import com.adaptris.annotation.ComponentProfile;
import com.adaptris.annotation.DisplayOrder;
import com.adaptris.core.RetryMessageErrorHandlerImp;
import com.adaptris.util.TimeInterval;
import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * MessageErrorHandler implementation that allows automatic retries for a problem message and is intended for use within a
 * TriggeredChannel
 *
 * @config triggered-retry-message-error-handler
 */
@XStreamAlias("triggered-retry-message-error-handler")
@AdapterComponent
@ComponentProfile(summary = "An exception handler instance that allows automated retries and is intended for use within a TriggeredChannel", tag = "error-handling,triggered")
@DisplayOrder(order = { "retryLimit" })
public class RetryMessageErrorHandler extends RetryMessageErrorHandlerImp implements TriggeredProcessor {

  /**
   * DefaultConstructor
   * <p>
   * This modifies the standard {@link RetryMessageErrorHandlerImp} defaults to be
   * </p>
   * <ul>
   * <li>RetryLimit = 0</li>
   * <li>RetryInterval = 30 Seconds</li>
   * </ul>
   */
  public RetryMessageErrorHandler() {
    super();
    setRetryLimit(0);
    setRetryInterval(new TimeInterval(30L, TimeUnit.SECONDS));
  }

  /**
   *
   * @see com.adaptris.core.triggered.TriggeredProcessor#processingCompleted()
   */
  @Override
  public boolean processingCompleted() {
    return assertEmpty(retryList, inProgress) || executorShutdown();
  }

  private boolean assertEmpty(List<?>... lists) {
    int rc = 0;
    for (List<?> list : lists) {
      rc += list.isEmpty() ? 1 : 0;
    }
    return rc == lists.length;
  }

  private boolean executorShutdown() {
    return executor != null ? executor.isShutdown() : true;
  }
}
