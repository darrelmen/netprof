package mitll.langtest.shared.user;

import com.github.gwtbootstrap.client.ui.Button;
import mitll.langtest.client.user.FormField;

import java.io.Serializable;

/**
 * @see mitll.langtest.client.user.ResetPassword#onChangePassword
 */
public class ChoosePasswordResult implements Serializable {
  public PasswordResultType getResultType() {
    return resultType;
  }

  public User getUser() {
    return user;
  }

  /**
   * @see mitll.langtest.server.services.OpenUserServiceImpl#changePasswordWithToken
   */
  public enum PasswordResultType {
    Success,
    Failed,
    NotExists,
    AlreadySet
  }

  private PasswordResultType resultType;
  private User user;

  public ChoosePasswordResult() {
  }

  public ChoosePasswordResult(User user, PasswordResultType resultType) {
    this.user = user;
    this.resultType = resultType;
  }
}
