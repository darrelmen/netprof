package mitll.langtest.client.user;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.ListBox;
import com.github.gwtbootstrap.client.ui.Modal;
import com.github.gwtbootstrap.client.ui.TextBox;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.github.gwtbootstrap.client.ui.constants.ControlGroupType;
import com.github.gwtbootstrap.client.ui.event.HiddenEvent;
import com.github.gwtbootstrap.client.ui.event.HiddenHandler;
import com.github.gwtbootstrap.client.ui.event.ShowEvent;
import com.github.gwtbootstrap.client.ui.event.ShowHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.PropertyHandler;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 9/10/13
 * Time: 5:33 PM
 * To change this template use File | Settings | File Templates.
 */
public class StudentDialog extends UserDialog {
  private final UserManager userManager;

  public StudentDialog(LangTestDatabaseAsync service,PropertyHandler props, UserManager userManager) {
    super(service,props);
    this.userManager = userManager;
  }

  /**
   * @see UserManager#login()
   */
  public void displayLoginBox() {
    final Modal dialogBox = getDialog();
    dialogBox.setTitle("Login Questions");
    final FormField dialectGroup, ageEntryGroup;
    final ListBoxFormField genderGroup/*, experienceGroup*/;
    ageEntryGroup = addControlFormField(dialogBox, "Your age");
    genderGroup = getListBoxFormField(dialogBox, "Gender", getGenderBox());

/*    ControlGroup expGroup = new ControlGroup();
    expGroup.add(new ControlLabel("Select months of experience"));
    final ListBox experienceBox = getExperienceBox();
    expGroup.add(experienceBox);
    dialogBox.add(expGroup);*/

    dialectGroup = addControlFormField(dialogBox, "Dialect");

    dialectGroup.box.addKeyUpHandler(new KeyUpHandler() {
      public void onKeyUp(KeyUpEvent event) {
        if (dialectGroup.box.getText().length() > 0) {
          dialectGroup.group.setType(ControlGroupType.NONE);
        }
      }
    });

    final Button closeButton = makeCloseButton();
    dialogBox.add(closeButton);
    closeButton.setFocus(true);

    // Create a handler for the sendButton and nameField
    class MyHandler implements ClickHandler {
      /**
       * Do validation.
       * Fired when the user clicks on the sendButton.
       */
      public void onClick(ClickEvent event) {
        if (highlightAgeBox(ageEntryGroup)) {
          if (dialectGroup.getText().isEmpty()) {
            markError(dialectGroup, "Please enter a language dialect.");
          } else {
            dialogBox.hide();
            sendNameToServer();
          }
        }
        else {
          markError(ageEntryGroup,"Please enter age between " + MIN_AGE + " and " + MAX_AGE+".");
        }
      }

      /**
       * Send the name from the nameField to the server and wait for a response.
       */
      private void sendNameToServer() {
      /*  int monthsOfExperience = experienceBox.getSelectedIndex() * 3;
        if (experienceBox.getSelectedIndex() == EXPERIENCE_CHOICES.size() - 1) {
          monthsOfExperience = NATIVE_MONTHS;
        }*/
        addUser(0, ageEntryGroup.box, genderGroup.box, dialectGroup.box);
      }
    }

    // Add a handler to send the name to the server
    MyHandler handler = new MyHandler();
    closeButton.addClickHandler(handler);


    dialogBox.addHiddenHandler(new HiddenHandler() {
      @Override
      public void onHidden(HiddenEvent hiddenEvent) {
        removeKeyHandler();
      }
    });

    dialogBox.addShowHandler(new ShowHandler() {
      @Override
      public void onShow(ShowEvent showEvent) {
        addKeyHandler(closeButton);
      }
    });

    dialogBox.show();
  }



  /**
   * @see #displayLoginBox()
   * @param monthsOfExperience
   * @param ageEntryBox
   * @param genderBox
   * @param dialectBox
   */
  private void addUser(int monthsOfExperience, TextBox ageEntryBox, ListBox genderBox, TextBox dialectBox) {
    int age = getAge(ageEntryBox);
    String gender = genderBox.getValue(genderBox.getSelectedIndex());
    addUser(age, gender, monthsOfExperience, dialectBox.getText(), PropertyHandler.LOGIN_TYPE.STUDENT);
  }

  /**
   * @see UserManager#addAnonymousUser()
   * @param age
   * @param gender
   * @param monthsOfExperience
   */
  public void addUser(int age, String gender, int monthsOfExperience) {
    addUser(age, gender, monthsOfExperience, "", PropertyHandler.LOGIN_TYPE.ANONYMOUS);
  }

  /**
   * @see #addUser(int, String, int)
   * @param age
   * @param gender
   * @param monthsOfExperience
   * @param dialect
   * @param loginType
   */
  private void addUser(int age, String gender, int monthsOfExperience, String dialect,
                       final PropertyHandler.LOGIN_TYPE loginType) {
    service.addUser(age,
      gender,
      monthsOfExperience, dialect, new AsyncCallback<Long>() {
      public void onFailure(Throwable caught) {
        Window.alert("addUser : Couldn't contact server.");
      }

      public void onSuccess(Long result) {
        System.out.println("addUser : server result is " + result);
        userManager.storeUser(result, "", "" + result, loginType);
      }
    });
  }

  private Button makeCloseButton() {
    final Button closeButton = new Button("Login");
    closeButton.setType(ButtonType.PRIMARY);

    // We can set the id of a widget by accessing its Element
    closeButton.getElement().setId("closeButton");
    return closeButton;
  }

  private boolean highlightAgeBox(FormField ageEntryGroup) {
    String text = ageEntryGroup.box.getText();
    boolean validAge = false;
    if (text.length() == 0) {
      ageEntryGroup.group.setType(ControlGroupType.WARNING);
    } else {
      try {
        int age = Integer.parseInt(text);
        validAge = (age > MIN_AGE && age < MAX_AGE) || age == TEST_AGE;
        ageEntryGroup.group.setType(validAge ? ControlGroupType.NONE : ControlGroupType.ERROR);
      } catch (NumberFormatException e) {
        ageEntryGroup.group.setType(ControlGroupType.ERROR);
      }
    }

    return validAge;
  }
}
