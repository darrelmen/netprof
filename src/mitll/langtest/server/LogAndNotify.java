/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

package mitll.langtest.server;

/**
 * Created by go22670 on 8/27/14.
 */
public interface LogAndNotify {
  void logAndNotifyServerException(Exception e);
  void logAndNotifyServerException(Exception e, String additionalMessage);
}
