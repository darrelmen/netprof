package mitll.langtest.client.user;

import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.PropertyHandler;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 10/10/13
 * Time: 2:53 PM
 * To change this template use File | Settings | File Templates.
 */
public class LoginDialog extends UserDialog {
  public LoginDialog(LangTestDatabaseAsync service, PropertyHandler props) {
    super(service, props);    //To change body of overridden methods use File | Settings | File Templates.
  }

/*  public void onClick() {
    final String userID = user.box.getText();
    if (userID.length() > USER_ID_MAX_LENGTH) {
      markError(user, "Please enter a user id of reasonable length.");
    } else if (userID.length() == 0) {
      markError(user, "Please enter a user id.");
    } else {
      service.userExists(userID, new AsyncCallback<Integer>() {
        public void onFailure(Throwable caught) {
          Window.alert("userExists : Couldn't contact server");
        }

        public void onSuccess(Integer result) {
          boolean exists = result != -1;
          if (exists) {
            if (!checkPassword(password)) {
              markError(password, "Please use password from the email.");
            } else if (checkAudioSelection(regular, fastThenSlow)) {
              markError(recordingStyle, regular, "Try again", "Please choose either regular or regular then slow audio recording.");
            } else {
              dialogBox.hide();
              String audioType = fastThenSlow.getValue() ? Result.AUDIO_TYPE_FAST_AND_SLOW : Result.AUDIO_TYPE_REGULAR;
              storeAudioType(audioType);
              userManager.storeUser(result, audioType, userID, PropertyHandler.LOGIN_TYPE.DATA_COLLECTOR);
            }
          } else {
            System.out.println(userID + " doesn't exist");
            if (checkPassword(password)) {
              doRegistration(user, password, recordingStyle,
                regular, fastThenSlow, registrationInfo,
                dialogBox, login, disclosurePanel);
            } else {
              markError(password, "Please use password from the email.");
            }
          }
        }
      });
  }*/
}
