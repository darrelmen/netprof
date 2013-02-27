package mitll.langtest.client.user;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 5/8/12
 * Time: 5:59 PM
 * To change this template use File | Settings | File Templates.
 */
public interface UserFeedback {
  void showErrorMessage(String title, String msg);
  void showStatus(String msg);
  void login();

  void setEmailSubject(String emailSubject);

  void setEmailMessage(String emailMessage);

  void setEmailToken(String e);

  void showEmail(String subject, String linkTitle, String token);

  //void setMailtoWithHistory(String type, String section, String historyToken);
}
