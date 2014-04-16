package mitll.langtest.client.user;

import com.github.gwtbootstrap.client.ui.AccordionGroup;
import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.Column;
import com.github.gwtbootstrap.client.ui.ControlGroup;
import com.github.gwtbootstrap.client.ui.ControlLabel;
import com.github.gwtbootstrap.client.ui.Controls;
import com.github.gwtbootstrap.client.ui.Fieldset;
import com.github.gwtbootstrap.client.ui.FluidRow;
import com.github.gwtbootstrap.client.ui.Form;
import com.github.gwtbootstrap.client.ui.Modal;
import com.github.gwtbootstrap.client.ui.RadioButton;
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
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.PropertyHandler;
import mitll.langtest.client.custom.KeyStorage;
import mitll.langtest.client.flashcard.ControlState;
import mitll.langtest.shared.DLIUser;
import mitll.langtest.shared.Result;

import java.util.ArrayList;
import java.util.Arrays;
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
  private static final int MIN_WEEKS = 0;
  private static final int MAX_WEEKS = 104;
  public static final int ILR_CHOICE_WIDTH = 80;
  public static final int MIN_LENGTH_USER_ID = 4;
  private static final String PRACTICE = "Practice";
  private static final String DEMO = "Demo";
  private static final String DATA_COLLECTION = "Data Collection";
  private static final String REVIEW = "Review";
  private static final Map<String, String> displayToRoles = new TreeMap<String, String>();
  private static final String STUDENT = "Student";
 // private static final String STUDENT_DATA_COLLECTION = "Student - Data Collection";
  private static final String TEACHER_REVIEWER = "Reviewer";
  private static final String TEACHER = "Teacher";
  private static final String RECORDER = "Recorder";
  private static final List<String> ROLES = Arrays.asList(STUDENT, TEACHER, TEACHER_REVIEWER, RECORDER);
  public static final String ARE_YOU_A = "Are you a";
  public static final String USER_ID = "User ID";
  public static final String USER_ID_TOOLTIP = "New users can choose any id and login.";
  public static final String PASSWORD = "Password";
  public static final String DIALECT = "Dialect";
  public static final String CHOOSE_A_GENDER = "Choose a gender.";

  private final UserManager userManager;
  private final UserNotification langTest;

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
    displayToRoles.put(TEACHER, PRACTICE);
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

    final FormField user = addControlFormField(fieldset, USER_ID, MIN_LENGTH_USER_ID);
    user.setVisible(isDataCollection(purpose) || isPractice(purpose));
    addTooltip(user.box, USER_ID_TOOLTIP);
    final FormField password = addControlFormField(fieldset, PASSWORD, true, 30, USER_ID_MAX_LENGTH);
    password.setVisible(false);
    purpose.box.setWidth("150px");

    Panel register = new FlowPanel();
    final RegistrationInfo registrationInfo = new RegistrationInfo(register);
    final AccordionGroup accordion = getAccordion(dialogBox, register);
    accordion.setVisible(!canSkipRegister(purpose));

    purpose.box.addChangeHandler(new ChangeHandler() {
      @Override
      public void onChange(ChangeEvent event) {
        boolean b = canSkipRegister(purpose);
        boolean needUserID = isDataCollection(purpose) || isReview(purpose) || isPractice(purpose);
        user.setVisible(needUserID);
        accordion.setVisible(!canSkipRegister(purpose));
        registrationInfo.showOrHideILR(!isReview(purpose)/* && !getRole(purpose).equals(RECORDER)*/);
        password.setVisible(isReview(purpose));

        if (b) {
          accordion.hide();
        } else {
          final String userID = user.box.getText();
          if (!userID.isEmpty()
            && (!isReview(purpose) || checkValidPassword(password))
            ) {
            service.userExists(userID, new AsyncCallback<Integer>() {
              @Override
              public void onFailure(Throwable caught) {}

              @Override
              public void onSuccess(Integer result) {
                boolean exists = result != -1;
                if (!exists) {
                  accordion.show();
                }
              }
            });
          }
        }
      }
    });

    final Button closeButton = addLoginButton(dialogBox);
    closeButton.addClickHandler(makeCloseHandler(dialogBox, registrationInfo, user, password, purpose, accordion));

    configureKeyHandler(dialogBox, closeButton);

    dialogBox.addShownHandler(new ShownHandler() {

      @Override
      public void onShown(ShownEvent shownEvent) {
        Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
          public void execute() {
            user.box.setFocus(true);
          }
        });

      }
    });
 /*   Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
      public void execute() {
        user.box.setFocus(true);
      }
    });
*/
    dialogBox.show();
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
   * @see #displayLoginBox()
   */
  private ClickHandler makeCloseHandler(final Modal dialogBox,
                                        final RegistrationInfo registrationInfo,
                                        final FormField user,
                                        final FormField password,
                                        final ListBoxFormField purpose,
                                        final AccordionGroup accordion) {
    return new ClickHandler() {
      /**
       * Do validation.
       * Fired when the user clicks on the sendButton.
       */
      public void onClick(ClickEvent event) {
        //System.out.println("makeCloseHandler : got click " + event);

        String purposeSetting = getRole(purpose);
        final String audioType = getAudioTypeFromPurpose(purposeSetting);
        if (canLoginAnonymously(purpose)) {
          System.out.println("\tcan login anonymously for " + purposeSetting);
          dialogBox.hide();
          langTest.rememberAudioType(audioType);
          addAnonymousUser(audioType);
        } else {
          System.out.println("\tcheckUserAndMaybeRegister for " + purposeSetting);

          checkUserAndMaybeRegister(audioType, user, password, dialogBox, accordion, registrationInfo, purposeSetting);
        }
      }
    };
  }

  private boolean canLoginAnonymously(ListBoxFormField purpose) {
    String purposeSetting = getRole(purpose);
    return purposeSetting.equalsIgnoreCase(DEMO);
  }

  private void checkUserAndMaybeRegister(final String audioType, FormField user,
                                         final FormField password,
                                         final Modal dialogBox,
                                         final AccordionGroup accordion,
                                         final RegistrationInfo registrationInfo,
                                         final String purposeSetting) {
    final String userID = user.box.getText();
    boolean isReview = purposeSetting.equals(REVIEW);

    System.out.println("checkUserAndMaybeRegister " + purposeSetting + " review " + isReview);
    if (checkValidUser(user) &&
      (!isReview || checkValidPassword(password))
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
          } else {
            System.out.println("checkUserAndMaybeRegister for " + purposeSetting + " user does not exist id=" + result);

            if (!canSkipRegister(purposeSetting)) {
              accordion.show();
            }
            checkThenRegister(audioType, registrationInfo, dialogBox, userID);
          }
          setRecordingOrder();
        }
      });
    } else {
      System.out.println("checkUserAndMaybeRegister for " + purposeSetting + " user name " + userID + " is invalid?");
    }
  }

  /**
   * Order of validity checks is important - marks errors moving top to bottom in dialog.
   * @param audioType
   * @param registrationInfo
   * @param dialogBox
   * @param userID
   */
  private void checkThenRegister(String audioType, RegistrationInfo registrationInfo, Modal dialogBox, String userID) {
    boolean skipWeekCheck = audioType.toLowerCase().contains(REVIEW.toLowerCase());
    boolean skipChecks = audioType.toLowerCase().contains(PRACTICE.toLowerCase());
    if (skipChecks) {
      System.out.println("checkThenRegister : skipChecks " + audioType + " user  " + userID);

      hideAndSend(audioType, registrationInfo, dialogBox, userID);
    } else if (highlightIntegerBox(registrationInfo.ageEntryGroup)) {
      System.out.println("\tcheckThenRegister : age OK skipChecks " + audioType + " skipWeekCheck " + skipWeekCheck);

      if (skipWeekCheck || highlightIntegerBox(registrationInfo.weeks, MIN_WEEKS, MAX_WEEKS)) {
        // order is important here --
         if (registrationInfo.checkValidGender()) {
          if (registrationInfo.dialectGroup.getText().isEmpty()) {
            System.out.println("\tcheckThenRegister : dialectGroup ");

            markError(registrationInfo.dialectGroup, "Enter a language dialect.");
          } else if (registrationInfo.checkValidity() && registrationInfo.checkValidity2()
            ) {
            System.out.println("\tcheckThenRegister : hideAndSend");

            hideAndSend(audioType, registrationInfo, dialogBox, userID);
          } else {
            System.out.println("\tcheckThenRegister : skipChecks " + audioType + " user  " + userID);
          }
        }
      } else {
        System.out.println("\tcheckThenRegister : markError weeks ");

        markError(registrationInfo.weeks, "Enter weeks between " + MIN_WEEKS + " and " + MAX_WEEKS + ".");
      }
    } else {
      markError(registrationInfo.ageEntryGroup.group, registrationInfo.ageEntryGroup.box, "",
        "Enter age between " + MIN_AGE + " and " + MAX_AGE + ".");
    }
  }

  private void hideAndSend(String audioType, RegistrationInfo registrationInfo, Modal dialogBox, String userID) {
    System.out.println("hideAndSend : audioType " + audioType + " user  " + userID);

    dialogBox.hide();
    sendNameToServer(registrationInfo, audioType, userID);
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
  private void sendNameToServer(RegistrationInfo registrationInfo, String audioType, String userID) {
    int weeksValue = registrationInfo.weeks.getText().isEmpty() ? 0 : Integer.parseInt(registrationInfo.weeks.getText());
    addUser(0, weeksValue, registrationInfo, audioType, userID);
  }

  /**
   * @param monthsOfExperience
   * @param userID
   * @see #sendNameToServer(mitll.langtest.client.user.StudentDialog.RegistrationInfo, String, String)
   */
  private void addUser(int monthsOfExperience, final int weeksOfExperience,
                       final RegistrationInfo registrationInfo,
                       final String audioType, String userID) {
    int age = getAge(registrationInfo.ageEntryGroup.box);
    String gender = registrationInfo.genderGroup.getValue();

    AsyncCallback<Long> async = getAddDLIUserCallback(weeksOfExperience, registrationInfo, audioType, userID);
    addUser(age, gender, monthsOfExperience, registrationInfo.dialectGroup.getText(), "", userID, async);
  }

  private AsyncCallback<Long> getAddDLIUserCallback(final int weeksOfExperience, final RegistrationInfo registrationInfo,
                                                    final String audioType, final String userChosenID) {
    return new AsyncCallback<Long>() {
      public void onFailure(Throwable caught) {
        Window.alert("addUser : Couldn't contact server.");
      }

      public void onSuccess(Long result) {
        System.out.println("addUser : server result is " + result);
        userManager.storeUser(result, audioType, userChosenID, PropertyHandler.LOGIN_TYPE.STUDENT);

        DLIUser dliUser = new DLIUser(result, weeksOfExperience,
          new DLIUser.ILRLevel(registrationInfo.reading.getValue(), registrationInfo.getValue("rilr")),
          new DLIUser.ILRLevel(registrationInfo.listening.getValue(), registrationInfo.getValue("lilr")),
          new DLIUser.ILRLevel(registrationInfo.speaking.getValue(), registrationInfo.getValue("silr")),
          new DLIUser.ILRLevel(registrationInfo.writing.getValue(), registrationInfo.getValue("wilr"))
        );

        service.addDLIUser(dliUser, new AsyncCallback<Void>() {
          @Override
          public void onFailure(Throwable caught) {
            Window.alert("addDLIUser : Couldn't contact server.");
          }

          @Override
          public void onSuccess(Void result) {
          }
        });
      }
    };
  }

  private void addAnonymousUser(String audioType) {
    addUser(89, "male", 0, audioType);
  }

  /**
   * @param age
   * @param gender
   * @param monthsOfExperience
   * @see UserManager#addAnonymousUser()
   */
  public void addUser(int age, String gender, int monthsOfExperience, String audioType) {
    addUser(age, gender, monthsOfExperience, "", PropertyHandler.LOGIN_TYPE.ANONYMOUS, audioType);
  }

  /**
   * @param age
   * @param gender
   * @param monthsOfExperience
   * @param dialect
   * @param loginType
   * @see #addUser(int, String, int, String)
   */
  private void addUser(int age, String gender, int monthsOfExperience, String dialect,
                       final PropertyHandler.LOGIN_TYPE loginType,
                       final String audioType) {
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
    addUser(age, gender, monthsOfExperience, dialect, "", "", async);
  }

  private void addUser(int age, String gender, int monthsOfExperience, String dialect, String nativeLang, String userID,
                       AsyncCallback<Long> async) {
    System.out.println("addUser : userID is " + userID);

    service.addUser(age,
      gender,
      monthsOfExperience, nativeLang, dialect, userID, async);
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
    private final FormField weeks;
    private ListBoxFormField reading;
    private ListBoxFormField listening;
    private ListBoxFormField speaking;
    private ListBoxFormField writing;
    private final FormField dialectGroup;
    final FluidRow ilrLevels;
    final FluidRow estimating;
    final Widget label;


    public RegistrationInfo(Panel dialogBox) {
      Form form = new Form();
      DOM.setStyleAttribute(form.getElement(), "marginBottom", "0px");

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

      ageEntryGroup = addControlFormField(fieldsetLeft, "Your age");
      genderGroup = getListBoxFormField(fieldsetRight, "Gender", getGenderBox());
      weeks = addControlFormField(fieldsetLeft, "Weeks of Experience");
      dialectGroup = getDialect(fieldsetRight);

      final ControlGroup ilrLevel = new ControlGroup();
      ilrLevel.addStyleName("leftFiveMargin");
      ControlLabel label = new ControlLabel("Select your ILR level (check boxes below if estimating)");
      ilrLevel.add(label);
      this.label = label;
      dialogBox.add(ilrLevel);
      ilrLevel.addStyleName("floatLeft");

      FluidRow row = getILRLevels();
      row.addStyleName("floatLeft");

      dialogBox.add(row);
      ilrLevels = row;
      FluidRow row2 = getEstimating2();
      dialogBox.add(row2);
      estimating = row2;
    }

    public void showOrHideILR(boolean show) {
      ilrLevels.setVisible(show);
      estimating.setVisible(show);
      label.setVisible(show);
      weeks.setVisible(show);
    }

    public boolean checkValidity() {
      if (!ilrLevels.isVisible()) return true;
      for (ListBoxFormField f : Arrays.asList(reading,listening,speaking,writing)) {
         if (f.box.getValue().equals(UNSET)) {
           f.markSimpleError("Choose a level", Placement.TOP);
           return false;
         }
      }
      return true;
    }

    public boolean checkValidity2() {
      if (!estimating.isVisible()) return true;

      for (YesNo f : ilrs) {
        if (!f.markSimpleError(Placement.TOP)) {
          return false;
        }
      }
      return true;
    }


    public boolean checkValidGender() {
      boolean valid = !genderGroup.getValue().equals(UNSET);
      if (!valid) {
        genderGroup.markSimpleError(CHOOSE_A_GENDER,Placement.LEFT);
      }
      return valid;
    }


    public boolean getValue(String ilr) {
      for (YesNo yn : ilrs) {
        if (yn.getName().equals(ilr)) return yn.getValue();
      }
      return false;
    }

    /**
     * @return
     * @see #RegistrationInfo(com.google.gwt.user.client.ui.Panel)
     */
    private FluidRow getILRLevels() {
      FluidRow row = new FluidRow();
      Column column = new Column(2, new HTML());
      row.add(column);

      Column c1 = new Column(2);
      row.add(c1);
      Column c2 = new Column(2);
      row.add(c2);
      Column c3 = new Column(2);
      row.add(c3);
      Column c4 = new Column(2);
      row.add(c4);

      List<String> levels = Arrays.asList("Unset", "0+", "1", "1+", "2", "2+", "3", "3+", "4");
      reading = getListBoxFormField(c1, "Reading", getListBox2(levels));
      listening = getListBoxFormField(c2, "Listening", getListBox2(levels));
      speaking  = getListBoxFormField(c3, "Speaking", getListBox2(levels));
      writing   = getListBoxFormField(c4, "Writing", getListBox2(levels));
      return row;
    }

    private final List<YesNo> ilrs = new ArrayList<YesNo>();

    private FluidRow getEstimating2() {
      FluidRow row2 = new FluidRow();

      Column cc0 = new Column(2, new HTML("Estimating:"));
      row2.add(cc0);

      for (String ilr : Arrays.asList("rilr", "lilr", "silr", "wilr")) {
        YesNo e = new YesNo(ilr);
        e.group.addStyleName("leftThirtyMargin");

        Column cc1 = new Column(2, e.group);
        row2.add(cc1);
        ilrs.add(e);
      }

      return row2;
    }
  }

  private class YesNo {
    public final RadioButton yes, no;
    private final String name;
    public final ControlGroup group;

    public YesNo(String name) {
      this.name = name;
      yes = new RadioButton(name, "Y");
      yes.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          hidePopovers();
        }
      });
      no = new RadioButton(name, "N");
      no.addClickHandler(
        new ClickHandler() {
          @Override
          public void onClick(ClickEvent event) {
            hidePopovers();
          }
        }
      );

      group = new ControlGroup();

      yes.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          group.setType(ControlGroupType.NONE);
        }
      });
      no.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          group.setType(ControlGroupType.NONE);
        }
      });

      Controls controls = new Controls();
      controls.add(yes);
      controls.add(no);
      group.add(controls);
    }

    public boolean markSimpleError(Placement placement) {
      if (!yes.getValue() && !no.getValue()) {
        markError(group, yes, "Please choose", "Click yes or no.", placement);
        return false;
      } else return true;
    }

    public String getName() {
      return name;
    }
    public boolean getValue() {
      return yes.getValue();
    }
  }
}
