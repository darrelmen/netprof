package mitll.langtest.client.user;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 5/16/12
 * Time: 12:54 PM
 * To change this template use File | Settings | File Templates.
 */
public interface UserNotification {
  void gotUser(long userID);
  void rememberAudioType(String audioType);
  void setShowUnansweredFirst(boolean v);
}
