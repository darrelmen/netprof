package mitll.langtest.shared;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Created by GO22670 on 3/21/2014.
 */
public interface CommonShell extends IsSerializable {
  public String getID();
  public String getTooltip();
  void setTooltip(String tooltip);
}
