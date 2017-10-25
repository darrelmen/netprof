package mitll.langtest.shared.user;

import java.io.Serializable;

public class ChoosePasswordResult implements Serializable {
  public ResultType getResultType() {
    return resultType;
  }

  public User getUser() {
    return user;
  }

  public enum ResultType {
    Success,
    Failed,
    NotExists,
    AlreadySet
  }
  private ResultType resultType;
  private User user;

  public ChoosePasswordResult() {}
  public ChoosePasswordResult(User user, ResultType resultType) {
    this.user = user;
    this.resultType = resultType;
  }
}
