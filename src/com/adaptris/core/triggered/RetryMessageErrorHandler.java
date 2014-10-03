/*
 * $RCSfile: RetryMessageErrorHandler.java,v $
 * $Revision: 1.5 $
 * $Date: 2008/09/11 10:02:51 $
 * $Author: lchan $
 */
package com.adaptris.core.triggered;

import java.util.concurrent.TimeUnit;

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
    return retryList.size() == 0 && inProgress.size() == 0;
  }

}
