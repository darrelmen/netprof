package mitll.langtest.client.user;

import com.github.gwtbootstrap.client.ui.AccordionGroup;
import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.CheckBox;
import com.github.gwtbootstrap.client.ui.Fieldset;
import com.github.gwtbootstrap.client.ui.Form;
import com.github.gwtbootstrap.client.ui.Modal;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.constants.ControlGroupType;
import com.github.gwtbootstrap.client.ui.constants.Placement;
import com.github.gwtbootstrap.client.ui.event.HiddenEvent;
import com.github.gwtbootstrap.client.ui.event.HiddenHandler;
import com.github.gwtbootstrap.client.ui.event.ShowEvent;
import com.github.gwtbootstrap.client.ui.event.ShowHandler;
import com.github.gwtbootstrap.client.ui.event.ShownEvent;
import com.github.gwtbootstrap.client.ui.event.ShownHandler;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.VerticalPanel;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.PropertyHandler;
import mitll.langtest.client.custom.KeyStorage;
import mitll.langtest.client.flashcard.ControlState;
import mitll.langtest.shared.Result;
import mitll.langtest.shared.User;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 9/10/13
 * Time: 5:33 PM
 * To change this template use File | Settings | File Templates.
 */
class StudentDialog extends UserDialog {
  public static final int ILR_CHOICE_WIDTH = 80;
  public static final int MIN_LENGTH_USER_ID = 8;
  private static final String PRACTICE = "Practice";
  private static final String DEMO = "Demo";
  private static final String DATA_COLLECTION = "Data Collection";
  public static final String REVIEW = "Review";
  private static final Map<String, String> displayToRoles = new TreeMap<String, String>();
  private static final String STUDENT = "Student";
  private static final String TEACHER_REVIEWER = "Reviewer";
  private static final String TEACHER = "Teacher";
  private static final String RECORDER = "Recorder";
  private static final List<String> ROLES = Arrays.asList(STUDENT, TEACHER);
  public static final String ARE_YOU_A = "Are you a";
  public static final String USER_ID = "User ID";
  public static final String USER_ID_TOOLTIP = "New users can choose any id and login.";
  public static final String PASSWORD = "Password";
  public static final String DIALECT = "Dialect";
  public static final String CHOOSE_A_GENDER = "Choose a gender.";
  private static final String DO_QUALITY_CONTROL = " Do Quality Control";
  private static final String MARK_AND_FIX_DEFECTS = "Mark and fix defects in text and audio";
  private static final String RECORD_REFERENCE_AUDIO = " Record Reference Audio";
  private static final String RECORD_REFERENCE_AUDIO_TOOLTIP = "Record reference audio for course content";

  boolean accordionHasBeenShown = false;
  private final UserManager userManager;
  private final UserNotification langTest;
  private CheckBox qcCheckBox, recordAudioCheckBox;
  private Button closeButton;
  RegistrationInfo registrationInfo;
  AccordionGroup accordion;

  public StudentDialog(LangTestDatabaseAsync service, PropertyHandler props, UserManager userManager,
                       UserNotification userNotification) {
    super(service, props);
    this.userManager = userManager;
    this.langTest = userNotification;
    populateRoles();
  }

  private void populateRoles() {
    displayToRoles.put(STUDENT, PRACTICE);
   // displayToRoles.put(STUDENT_DATA_COLLECTION, DATA_COLLECTION);
    displayToRoles.put(TEACHER_REVIEWER, REVIEW);
    displayToRoles.put(TEACHER, REVIEW);
    displayToRoles.put(RECORDER, RECORDER);
  }

  private String getRole(ListBoxFormField purpose) {
    return displayToRoles.get(purpose.getValue());
  }

  /**
   * @see UserManager#login
   */

  public void displayLoginBox() {
    final Modal dialogBox = getDialog("Login");
    dialogBox.setWidth("700px");
    dialogBox.setAnimation(false);
    dialogBox.setMaxHeigth("760px");

    Element element = dialogBox.getElement();
    element.setId("Student_LoginBoxDialog");
    DOM.setStyleAttribute(element, "top", "1%");
    Form form = new Form();
    form.addStyleName("form-horizontal");
    DOM.setStyleAttribute(form.getElement(), "marginBottom", "0px");

    Fieldset fieldset = new Fieldset();
    form.add(fieldset);
    dialogBox.add(form);
    final ListBoxFormField purpose = getListBoxFormField(fieldset, ARE_YOU_A, getListBox2(ROLES));
    purpose.box.setWidth("150px");

    final FormField user = addControlFormField(fieldset, USER_ID, MIN_LENGTH_USER_ID,"min 8 characters, 2 numbers like \"jlebron23\"");
    user.setVisible(isDataCollection(purpose) || isPractice(purpose));
    addTooltip(user.box, USER_ID_TOOLTIP);
    user.box.setFocus(true);
    final FormField password = addControlFormField(fieldset, PASSWORD, true, 30, USER_ID_MAX_LENGTH, "");
    password.setVisible(false);

    langTest.getPermissions().clear();

    Panel permissions = makePermissions();

    Panel register = new FlowPanel();
    registrationInfo = new RegistrationInfo(register, permissions);
    showDemographicFields(false);

    accordion = getAccordion(dialogBox, register);
    // only show the accordion if logging in for the first time
    accordion.setVisible(false);

    purpose.box.addChangeHandler(new ChangeHandler() {
      @Override
      public void onChange(ChangeEvent event) {
        boolean skipRegister = canSkipRegister(purpose);
        boolean needUserID = isDataCollection(purpose) || isReview(purpose) || isPractice(purpose);
        user.setVisible(needUserID);
       // registrationInfo.showOrHideILR(!isReview(purpose));
        password.setVisible(isReview(purpose));

        if (skipRegister) {
          accordion.hide();
        } else {
          System.out.println("can't skip register...");
          final String userID = user.box.getText();
          if (shouldCheckForExistingUser(userID, purpose, password, true)
            ) {
            service.userExists(userID, new AsyncCallback<Integer>() {
              @Override
              public void onFailure(Throwable caught) {}

              @Override
              public void onSuccess(final Integer result) {
                boolean exists = result != -1;
                accordion.setVisible(!exists);
                if (!exists) {
                  System.out.println("user doesn't exist...");
                  showAccordion();
                }
                else {
                  System.out.println("user exists...");

                }
              }
            });
          }
        }
      }
    });

    closeButton = addLoginButton(dialogBox);
    closeButton.addClickHandler(makeCloseHandler(closeButton,dialogBox, registrationInfo, user, password, purpose, accordion, langTest.getPermissions()));

    configureKeyHandler(dialogBox, closeButton);

    dialogBox.addShownHandler(new ShownHandler() {

      @Override
      public void onShown(ShownEvent shownEvent) {
        getFocusOnUserID(user);

      }
    });

    dialogBox.show();
    getFocusOnUserID(user);
  }

  private void showAccordion() {
    accordion.setVisible(true);
    accordion.show();
    accordionHasBeenShown = true;
  }

  protected void getFocusOnUserID(final FormField user) {
    Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
      public void execute() {
        user.box.setFocus(true);
      }
    });
  }

  protected boolean shouldCheckForExistingUser(String userID, ListBoxFormField purpose, FormField password, boolean markError) {
    return !userID.isEmpty() && (!isReview(purpose) || checkValidPassword(password, markError));
  }

  /**
   * @see #displayLoginBox()
   * @return
   */
  private Panel makePermissions() {
    Panel permissions = new VerticalPanel();

    qcCheckBox = new CheckBox(DO_QUALITY_CONTROL);
    qcCheckBox.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        langTest.setPermission(User.Permission.QUALITY_CONTROL, qcCheckBox.getValue());
        showDemographicFields(qcCheckBox.getValue());
      }
    });
    addTooltip(qcCheckBox, MARK_AND_FIX_DEFECTS);
    permissions.add(qcCheckBox);

    Node child = qcCheckBox.getElement().getChild(1);
    com.google.gwt.dom.client.Element as = SpanElement.as(child);
    as.setClassName("icon-edit");

    recordAudioCheckBox = new CheckBox(RECORD_REFERENCE_AUDIO);
    recordAudioCheckBox.getHTML();
    child = recordAudioCheckBox.getElement().getChild(1);
    as = SpanElement.as(child);
    as.setClassName("icon-microphone");
    addTooltip(recordAudioCheckBox, RECORD_REFERENCE_AUDIO_TOOLTIP);
    recordAudioCheckBox.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        langTest.setPermission(User.Permission.RECORD_AUDIO, recordAudioCheckBox.getValue());
        showDemographicFields(recordAudioCheckBox.getValue());
      }
    });
    permissions.add(recordAudioCheckBox);
    return permissions;
  }

  private void showDemographicFields(boolean vis) {
    registrationInfo.ageEntryGroup.setVisible(vis);
    registrationInfo.genderGroup.setVisible(vis);
    registrationInfo.dialectGroup.setVisible(vis);
  }

  private boolean isDataCollection(ListBoxFormField purpose) {
    String role = getRole(purpose);
    return role.equals(DATA_COLLECTION) || role.equals(RECORDER);
  }

  private boolean isPractice(ListBoxFormField purpose) {
    return getRole(purpose).equals(PRACTICE);
  }

  private boolean isReview(ListBoxFormField purpose) {
    String role = getRole(purpose);
    return role.equals(REVIEW) || role.equals(RECORDER);
  }

  /**
   * @see #displayLoginBox()
   * @param dialogBox
   * @param closeButton
   */
  private void configureKeyHandler(Modal dialogBox, final Button closeButton) {
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
  }

  /**
   * @param dialogBox
   * @param registrationInfo
   * @param user
   * @param purpose
   * @param accordion
   * @param permissions
   * @see #displayLoginBox()
   */
  private ClickHandler makeCloseHandler(final Button closeButton,
                                        final Modal dialogBox,
                                        final RegistrationInfo registrationInfo,
                                        final FormField user,
                                        final FormField password,
                                        final ListBoxFormField purpose,
                                        final AccordionGroup accordion,
                                        final Collection<User.Permission> permissions) {
    return new ClickHandler() {
      /**
       * Do validation.
       * Fired when the user clicks on the sendButton.
       */
      public void onClick(ClickEvent event) {
       // closeButton.setEnabled(false);
        String purposeSetting = getRole(purpose);
        final String audioType = getAudioTypeFromPurpose(purposeSetting);
        System.out.println("\tmakeCloseHandler.onClick for " + purposeSetting);
        checkUserAndMaybeRegister(audioType, user, password, dialogBox, registrationInfo, purposeSetting, permissions);
      }
    };
  }

  private void checkUserAndMaybeRegister(final String audioType,
                                         final FormField user,
                                         final FormField password,
                                         final Modal dialogBox,
                                         final RegistrationInfo registrationInfo,
                                         final String purposeSetting,
                                         final Collection<User.Permission> permissions) {
    final String userID = user.box.getText();
    boolean needsPassword = purposeSetting.equals(REVIEW) || purposeSetting.equals(RECORDER);

    System.out.println("checkUserAndMaybeRegister " + purposeSetting + " review " + needsPassword);
    if ((!needsPassword || checkValidPassword(password, true))
      ) {
      service.userExists(userID, new AsyncCallback<Integer>() {
        @Override
        public void onFailure(Throwable caught) {}

        @Override
        public void onSuccess(Integer result) {
          boolean exists = result != -1;
          if (exists) {
            System.out.println("checkUserAndMaybeRegister for " + purposeSetting + " user exists id=" + result);

            dialogBox.hide();
            langTest.rememberAudioType(audioType);
            userManager.storeUser(result, audioType, userID, PropertyHandler.LOGIN_TYPE.STUDENT);
          } else if (checkValidUser(user)) {
            System.out.println("checkUserAndMaybeRegister for " + purposeSetting + " user does not exist id=" + result);

            checkThenRegister(audioType, registrationInfo, dialogBox, userID, permissions);
          }
        }
      });
    }
  }

  /**
   * Order of validity checks is important - marks errors moving top to bottom in dialog.
   * @param audioType
   * @param registrationInfo
   * @param dialogBox
   * @param userID
   * @see #checkUserAndMaybeRegister(String, mitll.langtest.client.user.BasicDialog.FormField, mitll.langtest.client.user.BasicDialog.FormField, com.github.gwtbootstrap.client.ui.Modal, mitll.langtest.client.user.StudentDialog.RegistrationInfo, String, java.util.Collection)
   */
  private void checkThenRegister(String audioType, RegistrationInfo registrationInfo, Modal dialogBox, String userID,
                                 Collection<User.Permission> permissions) {
    String s = audioType.toLowerCase();
    boolean skipWeekCheck = s.contains(REVIEW.toLowerCase()) || s.contains(RECORDER.toLowerCase());
    boolean skipChecks    = s.contains(PRACTICE.toLowerCase()); // i.e. you are a student
    if (skipChecks) {
      System.out.println("checkThenRegister : skipChecks " + audioType + " user  " + userID);

      hideAndSend(audioType, registrationInfo, dialogBox, userID, permissions);
    } else {
      boolean shownBefore = accordionHasBeenShown;
      showAccordion();

      if (qcCheckBox.getValue() || recordAudioCheckBox.getValue()) {
        if (highlightIntegerBox(registrationInfo.ageEntryGroup)) {
          System.out.println("\tcheckThenRegister : age OK skipChecks " + audioType + " skipWeekCheck " + skipWeekCheck);

          // order is important here --
          if (registrationInfo.checkValidGender()) {
            if (registrationInfo.dialectGroup.getText().isEmpty()) {
              System.out.println("\tcheckThenRegister : dialectGroup ");

              markError(registrationInfo.dialectGroup, "Enter a language dialect.");
            } else {
              System.out.println("\tcheckThenRegister : hideAndSend");

              hideAndSend(audioType, registrationInfo, dialogBox, userID, permissions);
            }
          }

        } else {
          markError(registrationInfo.ageEntryGroup.group, registrationInfo.ageEntryGroup.box, "",
              "Enter age between " + MIN_AGE + " and " + MAX_AGE + ".");
        }
      } else if (shownBefore) {
        System.out.println("checkThenRegister : shownBefore - skipChecks " + audioType + " user  " + userID);

        hideAndSend(audioType, registrationInfo, dialogBox, userID, permissions);
      }
    }
  }

  protected void markError(FormField dialectGroup, String message) {
    super.markError(dialectGroup, message);
    closeButton.setEnabled(true);
  }

  /**
   * @see #checkThenRegister(String, mitll.langtest.client.user.StudentDialog.RegistrationInfo, com.github.gwtbootstrap.client.ui.Modal, String, java.util.Collection)
   * @param audioType
   * @param registrationInfo
   * @param dialogBox
   * @param userID
   * @param permissions
   */
  private void hideAndSend(String audioType, RegistrationInfo registrationInfo, Modal dialogBox, String userID, Collection<User.Permission> permissions) {
    System.out.println("hideAndSend : audioType " + audioType + " user  " + userID);

    dialogBox.hide();
    sendNameToServer(registrationInfo, audioType, userID, permissions);
  }

  private String getAudioTypeFromPurpose(String purposeValue) {
    if (purposeValue.equalsIgnoreCase(PRACTICE)) return Result.AUDIO_TYPE_PRACTICE;
    else if (purposeValue.equalsIgnoreCase(DEMO)) return Result.AUDIO_TYPE_DEMO;
    else if (purposeValue.equalsIgnoreCase(DATA_COLLECTION)) return Result.AUDIO_TYPE_REGULAR;
    else if (purposeValue.equalsIgnoreCase(RECORDER)) return Result.AUDIO_TYPE_RECORDER;
    else return Result.AUDIO_TYPE_REVIEW;
  }

  private boolean canSkipRegister(ListBoxFormField field) { return canSkipRegister(getRole(field));  }

  private boolean canSkipRegister(String purposeValue) {
    return purposeValue.equalsIgnoreCase(PRACTICE) ||
      purposeValue.equalsIgnoreCase(DEMO);
  }

  private FormField getDialect(Panel dialogBox) {
    final FormField dialectGroup = addControlFormField(dialogBox, DIALECT);
    dialectGroup.group.addStyleName("topTwentyMargin");

    dialectGroup.box.addKeyUpHandler(new KeyUpHandler() {
      public void onKeyUp(KeyUpEvent event) {
        if (dialectGroup.box.getText().length() > 0) {
          dialectGroup.group.setType(ControlGroupType.NONE);
        }
      }
    });
    return dialectGroup;
  }


  /**
   * Send the name from the nameField to the server and wait for a response.
   *
   * @see #makeCloseHandler
   */
  private void sendNameToServer(RegistrationInfo registrationInfo, String audioType, String userID, Collection<User.Permission> permissions) {
    //int weeksValue = registrationInfo.weeks.getText().isEmpty() ? 0 : Integer.parseInt(registrationInfo.weeks.getText());
    addUser(0, registrationInfo, audioType, userID, permissions);
  }

  /**
   * @param monthsOfExperience
   * @param userID
   * @param permissions
   * @see #sendNameToServer(mitll.langtest.client.user.StudentDialog.RegistrationInfo, String, String, java.util.Collection)
   */
  private void addUser(int monthsOfExperience,
                       final RegistrationInfo registrationInfo,
                       final String audioType, String userID, Collection<User.Permission> permissions) {
    int age = getAge(registrationInfo.ageEntryGroup.box);
    String gender = registrationInfo.genderGroup.getValue();

    AsyncCallback<Long> async = getAddDLIUserCallback(audioType, userID);
    addUser(age, gender, monthsOfExperience, registrationInfo.dialectGroup.getText(), "", userID, permissions, async);
  }

  private AsyncCallback<Long> getAddDLIUserCallback(final String audioType, final String userChosenID) {
    return new AsyncCallback<Long>() {
      public void onFailure(Throwable caught) {
        Window.alert("addUser : Couldn't contact server.");
      }

      public void onSuccess(Long result) {
        userManager.storeUser(result, audioType, userChosenID, PropertyHandler.LOGIN_TYPE.STUDENT);
      }
    };
  }

  /**
   * @param age
   * @param gender
   * @param monthsOfExperience
   * @param permissions
   * @see UserManager#addAnonymousUser()
   */
  public void addUser(int age, String gender, int monthsOfExperience, String audioType, Collection<User.Permission> permissions) {
    addUser(age, gender, monthsOfExperience, "", PropertyHandler.LOGIN_TYPE.ANONYMOUS, audioType, permissions);
  }

  /**
   * @param age
   * @param gender
   * @param monthsOfExperience
   * @param dialect
   * @param loginType
   * @param permissions
   * @see #addUser(int, String, int, String, java.util.Collection)
   */
  private void addUser(int age, String gender, int monthsOfExperience, String dialect,
                       final PropertyHandler.LOGIN_TYPE loginType,
                       final String audioType, Collection<User.Permission> permissions) {
    AsyncCallback<Long> async = new AsyncCallback<Long>() {
      public void onFailure(Throwable caught) {
        Window.alert("addUser : Couldn't contact server.");
      }

      public void onSuccess(Long result) {
        System.out.println("addUser : server result is " + result);
        setDefaultControlValues(result.intValue());
        userManager.storeUser(result, audioType, "" + result, loginType);
      }
    };
    addUser(age, gender, monthsOfExperience, dialect, "", "", permissions, async);
  }

  private void addUser(int age, String gender, int monthsOfExperience, String dialect, String nativeLang, String userID,
                       Collection<User.Permission> permissions, AsyncCallback<Long> async) {
    System.out.println("addUser : userID is " + userID);

    service.addUser(age,
        gender,
        monthsOfExperience, nativeLang, dialect, userID, permissions, async);
  }

  private void setDefaultControlValues(int user) {
    ControlState controlState = new ControlState();
    controlState.setStorage(new KeyStorage(props.getLanguage(),user));
    controlState.setAudioOn(true);
    controlState.setAudioFeedbackOn(true);
  }

  private boolean highlightIntegerBox(FormField ageEntryGroup) {
    return highlightIntegerBox(ageEntryGroup, MIN_AGE, MAX_AGE+1, TEST_AGE);
  }

  private class RegistrationInfo {
    private final FormField ageEntryGroup;
    private final ListBoxFormField genderGroup;
    private final FormField dialectGroup;

    /**
     * @see StudentDialog#displayLoginBox()
     * @param dialogBox
     * @param lowerLeft
     */
    public RegistrationInfo(Panel dialogBox, Panel lowerLeft) {
      Form form = new Form();
      form.getElement().getStyle().setMarginBottom(0, Style.Unit.PX);

      form.addStyleName("form-horizontal");

      Fieldset fieldsetLeft = new Fieldset();
      Fieldset fieldsetRight = new Fieldset();

      DivWidget divLeft = new DivWidget();
      divLeft.addStyleName("floatLeft");

      DivWidget divRight = new DivWidget();
      divRight.addStyleName("floatRight");

      divLeft.add(fieldsetLeft);
      form.add(divLeft);

      divRight.add(fieldsetRight);
      form.add(divRight);

      dialogBox.add(divLeft);
      dialogBox.add(divRight);

      genderGroup = getListBoxFormField(fieldsetRight, "Gender", getGenderBox());

      addControlGroupEntrySimple(fieldsetLeft, "Permissions", lowerLeft);
      ageEntryGroup = addControlFormField(fieldsetLeft, "Your age");
      dialectGroup = getDialect(fieldsetRight);
    }


    /**
     * @see #checkThenRegister(String, mitll.langtest.client.user.StudentDialog.RegistrationInfo, com.github.gwtbootstrap.client.ui.Modal, String, java.util.Collection)
     * @return
     */
    public boolean checkValidGender() {
      boolean valid = !genderGroup.getValue().equals(UNSET);
      if (!valid) {
        genderGroup.markSimpleError(CHOOSE_A_GENDER,Placement.LEFT);
      }
      return valid;
    }
  }
}
