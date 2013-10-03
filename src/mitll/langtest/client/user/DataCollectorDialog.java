package mitll.langtest.client.user;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.ControlGroup;
import com.github.gwtbootstrap.client.ui.ControlLabel;
import com.github.gwtbootstrap.client.ui.Controls;
import com.github.gwtbootstrap.client.ui.ListBox;
import com.github.gwtbootstrap.client.ui.Modal;
import com.github.gwtbootstrap.client.ui.RadioButton;
import com.github.gwtbootstrap.client.ui.TextBox;
import com.github.gwtbootstrap.client.ui.constants.ControlGroupType;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.event.logical.shared.OpenEvent;
import com.google.gwt.event.logical.shared.OpenHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.DisclosurePanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.PropertyHandler;
import mitll.langtest.shared.Result;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 9/10/13
 * Time: 4:24 PM
 * To change this template use File | Settings | File Templates.
 */
public class DataCollectorDialog extends UserDialog {
  public DataCollectorDialog(LangTestDatabaseAsync service, PropertyHandler props,
                             UserNotification langTest,
                             UserManager userManager) {
    super(service, props, userManager, langTest);
  }

  /**
   * Really should be named data collector (audio recorder) login
   */
  public void display(String title) {
    final Modal dialogBox = getDialog();

    dialogBox.setTitle(title);

    final FormField user = addControlFormField(dialogBox, "User ID");
    final FormField password = addControlFormField(dialogBox, "Password", true);
    final RadioButton regular = new RadioButton("AudioType", "Regular Audio Recording");
    final RadioButton fastThenSlow = new RadioButton("AudioType", "Record Regular Speed then Slow");

    final ControlGroup recordingStyle = new ControlGroup();

    regular.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        recordingStyle.setType(ControlGroupType.NONE);   // clear any error markings
      }
    });
    fastThenSlow.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        recordingStyle.setType(ControlGroupType.NONE);
      }
    });

    if (props.isCollectAudio()) {
      recordingStyle.add(new ControlLabel("<b>Audio Recording Style</b>"));
      Controls controls = new Controls();
      controls.add(regular);
      controls.add(fastThenSlow);
      recordingStyle.add(controls);
      dialogBox.add(recordingStyle);
    }

    SimplePanel spacer = new SimplePanel();
    spacer.setSize("20px", "5px");
    dialogBox.add(spacer);
    dialogBox.add(new HTML("<i>New users : click on Registration below and fill in the fields.</i>"));
    SimplePanel spacer2 = new SimplePanel();
    spacer2.setSize("20px", "5px");
    dialogBox.add(spacer2);

    final FormField nativeLangGroup, dialectGroup, ageEntryGroup;
    final ListBoxFormField genderGroup, experienceGroup;
    VerticalPanel register = new VerticalPanel();
    nativeLangGroup = addControlFormField(register, "Native Lang (L1)");
    dialectGroup = addControlFormField(register, "Dialect");
    ageEntryGroup = addControlFormField(register, "Your age");
    genderGroup = getListBoxFormField(register, "Select gender", getGenderBox());
    experienceGroup = getListBoxFormField(register, "Select months of experience", getExperienceBox());

    dialogBox.setMaxHeigth(Window.getClientHeight() * 0.8 + "px");
    if (!props.isDataCollectAdminView()) {
      dp = new DisclosurePanel("Registration");
      dp.setContent(register);
      dp.addOpenHandler(new OpenHandler<DisclosurePanel>() {
        @Override
        public void onOpen(OpenEvent<DisclosurePanel> event) {
          centerVertically(dialogBox.getElement()); // need to resize the dialog when reveal hidden widgets
        }
      });

      dp.addCloseHandler(new CloseHandler<DisclosurePanel>() {
        @Override
        public void onClose(CloseEvent<DisclosurePanel> event) {
          centerVertically(dialogBox.getElement());
        }
      });
      dialogBox.add(dp);
    }

    final Button login = addLoginButton(dialogBox);

    login.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        // System.out.println("login button got click " + event);

        if (isUserIDValid(user) && isPasswordValid(password) && isValidAudio(regular, fastThenSlow, recordingStyle)) {
          final String userID = user.box.getText();
          service.userExists(userID, new AsyncCallback<Integer>() {
            public void onFailure(Throwable caught) {
              Window.alert("userExists : Couldn't contact server");
            }

            public void onSuccess(Integer result) {
              boolean exists = result != -1;
              if (exists) {
                String audioType = fastThenSlow.getValue() ? Result.AUDIO_TYPE_FAST_AND_SLOW : Result.AUDIO_TYPE_REGULAR;
                userExists(result, userID, dialogBox, audioType, PropertyHandler.LOGIN_TYPE.DATA_COLLECTOR);
              } else {
                doRegistration(user, password, recordingStyle,
                  regular, fastThenSlow, nativeLangGroup, dialectGroup, ageEntryGroup,
                  experienceGroup, genderGroup, dialogBox, login);
              }
            }
          });
        }
      }
    });

    addKeyHandler(login, dialogBox);

    dialogBox.show();
  }

  protected boolean isValidAudio(RadioButton regular, RadioButton fastThenSlow,ControlGroup recordingStyle) {
    if (checkAudioSelection(regular, fastThenSlow)) {
      markError(recordingStyle, regular, "Try again", "Please choose either regular or regular then slow audio recording.");
      return false;
    }
    return true;
  }

  /**
   * @param user
   * @param password
   * @param regular
   * @param fastThenSlow
   * @param nativeLang
   * @param dialect
   * @param ageEntryBox
   * @param experienceBox
   * @param genderBox
   * @param dialogBox
   * @param login
   * @see #display
   */
  private void doRegistration(FormField user, FormField password, ControlGroup audioGroup,
                              RadioButton regular,
                              RadioButton fastThenSlow,
                              FormField nativeLang, FormField dialect, FormField ageEntryBox,
                              ListBoxFormField experienceBox, ListBoxFormField genderBox,
                              Modal dialogBox,
                              Button login) {
    boolean valid = true;
    if (!props.isDataCollectAdminView() && checkAudioSelection(regular, fastThenSlow)) {
      markError(audioGroup, regular, "Try Again", "Please choose either regular or regular then slow audio recording.");
      valid = false;
    } else if (!props.isDataCollectAdminView() && nativeLang.getText().isEmpty()) {
      if (!dp.isOpen()) {
        dp.setOpen(true);   // reveal registration fields
      } else {
        markError(nativeLang, "Language is empty");
      }
      valid = false;
    } else if (!props.isDataCollectAdminView() && dialect.getText().isEmpty()) {
      markError(dialect, "Dialect is empty");
      valid = false;
    }

    if (valid) {
      try {
        int age = getAge(ageEntryBox.box);
        if (!props.isDataCollectAdminView() && (age < MIN_AGE) || (age > MAX_AGE && age != TEST_AGE)) {
          valid = false;
          markError(ageEntryBox, "age '" + age + "' is too young or old.");
        }
      } catch (NumberFormatException e) {
        markError(ageEntryBox, "age '" + ageEntryBox.getText() + "' is invalid.");
        valid = false;
      }
    }
    if (valid) {
      int enteredAge = getAge(ageEntryBox.box);
      checkUserOrCreate(enteredAge, user, experienceBox.box, genderBox.box, nativeLang.box, dialect.box, dialogBox,
        login, fastThenSlow.getValue());
    } else {
      //System.out.println("not valid ------------ ?");
    }

  }

  private boolean checkAudioSelection(RadioButton regular, RadioButton fastThenSlow) {
    return props.isCollectAudio() && !regular.getValue() && !fastThenSlow.getValue();
  }

  /**
   * @param enteredAge
   * @param user
   * @param experienceBox
   * @param genderBox
   * @param nativeLang
   * @param dialect
   * @param dialogBox
   * @param closeButton
   * @param isFastAndSlow
   * @seex #display()
   */
  private void checkUserOrCreate(final int enteredAge, final FormField user, final ListBox experienceBox,
                                 final ListBox genderBox,
                                 final TextBox nativeLang, final TextBox dialect,
                                 final Modal dialogBox,
                                 final Button closeButton,
                                 final boolean isFastAndSlow) {
    service.userExists(user.getText(), new AsyncCallback<Integer>() {
      public void onFailure(Throwable caught) {
        Window.alert("Couldn't contact server.");
      }

      public void onSuccess(Integer result) {
        System.out.println("user '" + user.getText() + "' exists " + result);
        if (result == -1) {
          addTeacher(enteredAge,
            experienceBox, genderBox, nativeLang, dialect, user.box, dialogBox, closeButton, isFastAndSlow);
        } else {
          markError(user, "User " + user.getText() + " already registered, click login.");
        }
      }
    });
  }

  /**
   * @param age
   * @param experienceBox
   * @param genderBox
   * @param nativeLang
   * @param dialect
   * @param user
   * @param dialogBox
   * @param closeButton
   * @param isFastAndSlow
   * @see #checkUserOrCreate
   */
  private void addTeacher(int age, ListBox experienceBox, ListBox genderBox,
                          TextBox nativeLang,
                          TextBox dialect, final TextBox user,
                          final Modal dialogBox,
                          final Button closeButton,
                          final boolean isFastAndSlow) {
    int monthsOfExperience = experienceBox.getSelectedIndex() * 3;
    if (experienceBox.getSelectedIndex() == EXPERIENCE_CHOICES.size() - 1) {
      monthsOfExperience = NATIVE_MONTHS;
    }

    service.addUser(age,
      genderBox.getValue(genderBox.getSelectedIndex()),
      monthsOfExperience,
      nativeLang.getText(),
      dialect.getText(),
      user.getText(),

      new AsyncCallback<Long>() {
        public void onFailure(Throwable caught) {
          // Show the RPC error message to the user
          Window.alert("addUser : Can't contact server.");
          closeButton.setFocus(true);
        }

        public void onSuccess(Long result) {
          System.out.println("addUser : server result is " + result);
          String audioType = isFastAndSlow ? Result.AUDIO_TYPE_FAST_AND_SLOW : Result.AUDIO_TYPE_REGULAR;
          userExists(result.intValue(), user.getText(), dialogBox, audioType, PropertyHandler.LOGIN_TYPE.DATA_COLLECTOR);
        }
      });
  }
}
