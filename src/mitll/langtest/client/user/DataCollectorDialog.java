package mitll.langtest.client.user;

import com.github.gwtbootstrap.client.ui.AccordionGroup;
import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.ControlGroup;
import com.github.gwtbootstrap.client.ui.ControlLabel;
import com.github.gwtbootstrap.client.ui.Controls;
import com.github.gwtbootstrap.client.ui.ListBox;
import com.github.gwtbootstrap.client.ui.Modal;
import com.github.gwtbootstrap.client.ui.RadioButton;
import com.github.gwtbootstrap.client.ui.base.TextBoxBase;
import com.github.gwtbootstrap.client.ui.constants.ControlGroupType;
import com.github.gwtbootstrap.client.ui.constants.Placement;
import com.github.gwtbootstrap.client.ui.event.HiddenEvent;
import com.github.gwtbootstrap.client.ui.event.HiddenHandler;
import com.github.gwtbootstrap.client.ui.event.ShowEvent;
import com.github.gwtbootstrap.client.ui.event.ShowHandler;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Panel;
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
  public void displayTeacherLogin(String loginTitle) {
    final Modal dialogBox = getDialog(loginTitle);

    final FormField user = addControlFormField(dialogBox, "User ID");
    final FormField password = addControlFormField(dialogBox, "Password", true, 0);

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

    final RegistrationInfo registrationInfo = getRegistrationInfo();
    Panel register = registrationInfo.getRegister();

    dialogBox.setMaxHeigth(Window.getClientHeight() * 0.95 + "px");
    AccordionGroup accordion = null;
    if (!props.isDataCollectAdminView()) {
      accordion = getAccordion(dialogBox, register);
    }
    final AccordionGroup accordionFinal = accordion;

    final Button login = addLoginButton(dialogBox);
    login.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
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
                  markError(recordingStyle, regular, "Try again", "Please choose either regular or regular then slow audio recording.", Placement.BOTTOM);
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
                    dialogBox, login, accordionFinal);
                } else {
                  markError(password, "Please use password from the email.");
                }
              }
            }
          });
        }
      }
    });
    dialogBox.addHiddenHandler(new HiddenHandler() {
      @Override
      public void onHidden(HiddenEvent hiddenEvent) {
        removeKeyHandler();
      }
    });
    dialogBox.addShowHandler(new ShowHandler() {
      @Override
      public void onShow(ShowEvent showEvent) {
        addKeyHandler(login);
      }
    });

    Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
      public void execute() {
        user.box.setFocus(true);
      }
    });

    dialogBox.show();
  }

  protected RegistrationInfo getRegistrationInfo() {
    return new RegistrationInfo().invoke();
  }

  /**
   *
   * @param user
   * @param password
   * @param regular
   * @param fastThenSlow
   * @param dialogBox
   * @param login
   * @param accordion
   * @see #displayTeacherLogin
   */
  private void doRegistration(FormField user, FormField password, ControlGroup audioGroup,
                              RadioButton regular,
                              RadioButton fastThenSlow,
                              RegistrationInfo registrationInfo,
                              Modal dialogBox,
                              Button login, AccordionGroup accordion) {

    boolean valid = user.box.getText().length() > 0;
    if (!valid) {
      markError(user, "Please enter a userid.");
    } else {
      valid = password.box.getText().length() > 0;
      if (!valid) {
        markError(user, "Please enter a userid.");
      }
    }
    FormField nativeLangGroup = registrationInfo.getNativeLangGroup();
    FormField dialectGroup = registrationInfo.getDialectGroup();
    if (valid) {
      valid = checkPassword(password);
      if (!valid) {
        markError(password, "Please use password from the email sent to you.");
        valid = false;
      } else if (!props.isDataCollectAdminView() && checkAudioSelection(regular, fastThenSlow)) {
        markError(audioGroup, regular, "Try Again", "Please choose a style", Placement.BOTTOM);// either regular or regular then slow audio recording.");
        valid = false;
      } else {
        if (!props.isDataCollectAdminView() && nativeLangGroup.getText().isEmpty()) {
          accordion.show();
          markError(nativeLangGroup, "Language is empty");
          valid = false;
        } else {
          if (!props.isDataCollectAdminView() && dialectGroup.getText().isEmpty()) {
            markError(dialectGroup, "Dialect is empty");
            valid = false;
          }
        }
      }

      FormField ageEntryGroup = registrationInfo.getAgeEntryGroup();
      if (valid) {
        try {
          int age = getAge(ageEntryGroup.box);
          if (!props.isDataCollectAdminView() && (age < MIN_AGE) || (age > MAX_AGE && age != TEST_AGE)) {
            valid = false;
            markError(ageEntryGroup, "age '" + age + "' is too young or old.");
          }
        } catch (NumberFormatException e) {
          markError(ageEntryGroup, "age '" + ageEntryGroup.getText() + "' is invalid.");
          valid = false;
        }
      }
      if (valid) {
        int enteredAge = getAge(ageEntryGroup.box);
        checkUserOrCreate(enteredAge, user,
          registrationInfo.getExperienceGroup().box, registrationInfo.getGenderGroup().box,
          nativeLangGroup.box, dialectGroup.box, dialogBox,
          login, fastThenSlow.getValue());
      }
    }
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
   * @seex #displayTeacherLogin()
   */
  private void checkUserOrCreate(final int enteredAge, final FormField user, final ListBox experienceBox,
                                 final ListBox genderBox,
                                 final TextBoxBase nativeLang, final TextBoxBase dialect,
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
                          TextBoxBase nativeLang,
                          TextBoxBase dialect, final TextBoxBase user,
                          final Modal dialogBox,
                          final Button closeButton,
                          final boolean isFastAndSlow) {
    int monthsOfExperience = experienceBox.getSelectedIndex() * 3;
    if (experienceBox.getSelectedIndex() == EXPERIENCE_CHOICES.size() - 1) {
      monthsOfExperience = NATIVE_MONTHS;
    }

    final String userName = user.getText();
    String gender = genderBox.getValue(genderBox.getSelectedIndex());
    String nativeLangValue = nativeLang.getText();
    String dialectValue = dialect.getText();
    addFullUser(dialogBox, closeButton, userManager,
      userName, gender, nativeLangValue, dialectValue, age, monthsOfExperience, isFastAndSlow);
  }

  protected void addFullUser(final Modal dialogBox, final Button closeButton,
                             final UserManager userManager,
                             final String userName, String gender, String nativeLangValue, String dialectValue, int age,
                             int monthsOfExperience, final boolean isFastAndSlow) {
    service.addUser(age,
      gender,
      monthsOfExperience,
      nativeLangValue, dialectValue,
      userName,

      new AsyncCallback<Long>() {
        public void onFailure(Throwable caught) {
          // Show the RPC error message to the user
          Window.alert("addUser : Can't contact server.");
          closeButton.setFocus(true);
        }

        public void onSuccess(Long result) {
          System.out.println("addUser : server result is " + result);
          dialogBox.hide();
          String audioType = isFastAndSlow ? Result.AUDIO_TYPE_FAST_AND_SLOW : Result.AUDIO_TYPE_REGULAR;
          storeAudioType(audioType);
          userManager.storeUser(result, audioType, userName, PropertyHandler.LOGIN_TYPE.DATA_COLLECTOR);
        }
      });
  }

  protected void storeAudioType(String type) {
    if (props.isCollectAudio()) {
      userNotification.rememberAudioType(type);
    }
  }

  private boolean checkAudioSelection(RadioButton regular, RadioButton fastThenSlow) {
    return props.isCollectAudio() && !regular.getValue() && !fastThenSlow.getValue();
  }

  private class RegistrationInfo {
    private FormField nativeLangGroup;
    private FormField dialectGroup;
    private FormField ageEntryGroup;
    private ListBoxFormField genderGroup;
    private ListBoxFormField experienceGroup;
    private VerticalPanel register;

    public FormField getNativeLangGroup() {
      return nativeLangGroup;
    }

    public FormField getDialectGroup() {
      return dialectGroup;
    }

    public FormField getAgeEntryGroup() {
      return ageEntryGroup;
    }

    public ListBoxFormField getGenderGroup() {
      return genderGroup;
    }

    public ListBoxFormField getExperienceGroup() {
      return experienceGroup;
    }

    public VerticalPanel getRegister() {
      return register;
    }

    public RegistrationInfo invoke() {
      register = new VerticalPanel();
      nativeLangGroup = addControlFormField(register, "Native Lang (L1)");
      dialectGroup = addControlFormField(register, "Dialect");
      ageEntryGroup = addControlFormField(register, "Your age");
      genderGroup = getListBoxFormField(register, "Select gender", getGenderBox());
      experienceGroup = getListBoxFormField(register, "Select months of experience", getExperienceBox());
      return this;
    }
  }
}
