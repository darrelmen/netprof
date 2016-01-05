package mitll.langtest.shared.exercise;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Created by go22670 on 1/4/16.
 */
public interface Shell extends IsSerializable {
  String getID();
  STATE getState();
  void setState(STATE state);
}
