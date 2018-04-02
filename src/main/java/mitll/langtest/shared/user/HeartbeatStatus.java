package mitll.langtest.shared.user;

import com.google.gwt.user.client.rpc.IsSerializable;

public class HeartbeatStatus implements IsSerializable {
  private boolean hasSession;
  private boolean codeHasUpdated;

  public HeartbeatStatus() {}
  public HeartbeatStatus(boolean hasSession, boolean codeHasUpdated) {
    this.hasSession = hasSession;
    this.codeHasUpdated = codeHasUpdated;
  }

  public boolean isHasSession() {
    return hasSession;
  }

  public boolean isCodeHasUpdated() {
    return codeHasUpdated;
  }
}
