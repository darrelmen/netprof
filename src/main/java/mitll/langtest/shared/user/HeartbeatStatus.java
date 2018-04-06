package mitll.langtest.shared.user;

import com.google.gwt.user.client.rpc.IsSerializable;

public class HeartbeatStatus implements IsSerializable {
  private boolean hasSession;
  private boolean codeHasUpdated;

  public HeartbeatStatus() {}

  /**
   * @see mitll.langtest.server.services.OpenUserServiceImpl#setCurrentUserToProject
   * @param hasSession
   * @param codeHasUpdated
   */
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
