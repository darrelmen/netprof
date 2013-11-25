package mitll.langtest.client.user;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.Modal;
import com.github.gwtbootstrap.client.ui.TextBox;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.PropertyHandler;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 10/3/13
 * Time: 11:59 AM
 * To change this template use File | Settings | File Templates.
 * @deprecated
 */
public class SimpleDialog extends UserDialog {
  public SimpleDialog(LangTestDatabaseAsync service, PropertyHandler props, UserManager userManager, UserNotification userNotification) {
    super(service, props, userManager, userNotification);    //To change body of overridden methods use File | Settings | File Templates.
  }

  public void display(String title) {
    final Modal dialogBox = getDialog(title);

    dialogBox.setTitle(title);

    final FormField user = addControlFormField(dialogBox, "User ID");
    final FormField password = addControlFormField(dialogBox, "Password", true,1);
    final Button login = addLoginButton(dialogBox);

    login.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        // System.out.println("login button got click " + event);

       /* if (isUserIDValid(user) && isPasswordValid(password)) {
          final String userID = user.box.getText();
          service.userExists(userID, new AsyncCallback<Integer>() {
            public void onFailure(Throwable caught) {
              Window.alert("userExists : Couldn't contact server");
            }

            public void onSuccess(Integer result) {
              boolean exists = result != -1;
              if (exists) {
                userExists(result, userID, dialogBox, Result.AUDIO_TYPE_FAST_AND_SLOW, PropertyHandler.LOGIN_TYPE.SIMPLE);
              } else {
                addUser(user.box, dialogBox, login);
              }
            }
          });
        }*/
      }
    });

    addKeyHandler(login, dialogBox);

    dialogBox.show();
  }

  /**
   * @param user
   * @param dialogBox
   * @param closeButton
   */
  private void addUser(final TextBox user,
                       final Modal dialogBox,
                       final Button closeButton) {
    service.addUser(89,
      "male",
      0,
      "Unknown",
      "Unknown",
      user.getText(),

      new AsyncCallback<Long>() {
        public void onFailure(Throwable caught) {
          // Show the RPC error message to the user
          Window.alert("addUser : Can't contact server.");
          closeButton.setFocus(true);
        }

        public void onSuccess(Long result) {
          System.out.println("addUser : server result is " + result);
          //userExists(result.intValue(), user.getText(), dialogBox, Result.AUDIO_TYPE_FAST_AND_SLOW, PropertyHandler.LOGIN_TYPE.SIMPLE);
        }
      });
  }
}
