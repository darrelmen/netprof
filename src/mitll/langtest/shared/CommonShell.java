/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

package mitll.langtest.shared;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Created by GO22670 on 3/21/2014.
 */
public interface CommonShell extends IsSerializable {
  String getID();
  String getTooltip();
  STATE getState();
  void setState(STATE state);

  STATE getSecondState();
  void setSecondState(STATE state);

  String getEnglish();
  String getMeaning();
  String getForeignLanguage();
}
