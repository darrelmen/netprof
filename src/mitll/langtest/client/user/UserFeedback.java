package mitll.langtest.client.user;

import mitll.langtest.client.DialogHelper;

import java.util.List;

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

  void showEmail(String subject, String linkTitle, String token);

  void showErrorMessage(String title, List<String> msgs, String buttonName, DialogHelper.CloseListener listener);
}
