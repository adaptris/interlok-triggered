/*
 * $RCSfile: TriggeredProcessor.java,v $
 * $Revision: 1.2 $
 * $Date: 2008/07/14 09:57:10 $
 * $Author: lchan $
 */
package com.adaptris.core.triggered;


/**
 * Interface specifying components that may be hanlded using
 * {@link com.adaptris.core.triggered.TriggeredChannel}.
 * 
 * @author lchan
 * @author $Author: lchan $
 */
public interface TriggeredProcessor {

  /**
   * Report whether this component has finished is processing tasks.
   * 
   * @return true if the component has completed it's processing tasks.
   */
  boolean processingCompleted();

}
