/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

package mitll.langtest.client.exercise;

/**
 * For panels that could be busy recording or playing audio and want to veto moving to another exercise.
 * User: GO22670
 * Date: 12/27/12
 * Time: 2:48 PM
 * To change this template use File | Settings | File Templates.
 */
public interface BusyPanel {
  boolean isBusy();
  void setBusy(boolean v);
}
