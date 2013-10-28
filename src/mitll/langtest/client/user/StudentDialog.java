package mitll.langtest.client.user;

import com.github.gwtbootstrap.client.ui.AccordionGroup;
import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.CheckBox;
import com.github.gwtbootstrap.client.ui.Column;
import com.github.gwtbootstrap.client.ui.ControlGroup;
import com.github.gwtbootstrap.client.ui.ControlLabel;
import com.github.gwtbootstrap.client.ui.Fieldset;
import com.github.gwtbootstrap.client.ui.FluidRow;
import com.github.gwtbootstrap.client.ui.Form;
import com.github.gwtbootstrap.client.ui.ListBox;
import com.github.gwtbootstrap.client.ui.Modal;
import com.github.gwtbootstrap.client.ui.constants.ControlGroupType;
import com.github.gwtbootstrap.client.ui.event.HiddenEvent;
import com.github.gwtbootstrap.client.ui.event.HiddenHandler;
import com.github.gwtbootstrap.client.ui.event.ShowEvent;
import com.github.gwtbootstrap.client.ui.event.ShowHandler;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.PropertyHandler;
import mitll.langtest.shared.DLIUser;
import mitll.langtest.shared.Result;

import java.util.Arrays;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 9/10/13
 * Time: 5:33 PM
 * To change this template use File | Settings | File Templates.
 */
public class StudentDialog extends UserDialog {
  private static final int MIN_WEEKS = 0;
  private static final int MAX_WEEKS = 104;
  private final UserManager userManager;
  private final UserNotification langTest;

  public StudentDialog(LangTestDatabaseAsync service, PropertyHandler props, UserManager userManager, UserNotification langTest) {
    super(service, props);
    this.userManager = userManager;
    this.langTest = langTest;
  }

  /**
   * @see UserManager#login()
   */
  public void displayLoginBox() {
    final Modal dialogBox = getDialog("Login Questions");
    dialogBox.setAnimation(false);
    dialogBox.setMaxHeigth("760px");
    DOM.setStyleAttribute(dialogBox.getElement(), "top", "4%");
    Form form = new Form();
    form.addStyleName("form-horizontal");
    DOM.setStyleAttribute(form.getElement(), "marginBottom", "0px");

    Fieldset fieldset = new Fieldset();
    form.add(fieldset);
    dialogBox.add(form);
    final FormField user = addControlFormField(fieldset, "User ID");
    final FormField password = addControlFormField(fieldset, "Password", true);
    final ListBoxFormField purpose = getListBoxFormField(fieldset, "Purpose",
      getListBox2(Arrays.asList("Data Collection", "Practice", "Demo")));

    purpose.box.setWidth("150px");

    Panel register = new FlowPanel();
    RegistrationInfo registrationInfo = new RegistrationInfo(register);
    final AccordionGroup accordion = getAccordion(dialogBox, register);

    purpose.box.addChangeHandler(new ChangeHandler() {
      @Override
      public void onChange(ChangeEvent event) {
        boolean b = canSkipRegister(purpose.getValue());
        if (b) accordion.hide();
        else accordion.show();
      }
    });

    final Button closeButton = addLoginButton(dialogBox);
    closeButton.addClickHandler(makeCloseHandler(dialogBox, registrationInfo, user, password, purpose, accordion));

    configureKeyHandler(dialogBox, closeButton);

    Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
      public void execute() {
        user.box.setFocus(true);
      }
    });

    dialogBox.show();
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
   * @param password
   * @param purpose
   * @param accordion
   * @see #displayLoginBox()
   */
  private ClickHandler makeCloseHandler(final Modal dialogBox,
                                        final RegistrationInfo registrationInfo,
                                        final FormField user, final FormField password, final ListBoxFormField purpose,
                                        final AccordionGroup accordion) {
    // Create a handler for the sendButton and nameField
    return new ClickHandler() {
      /**
       * Do validation.
       * Fired when the user clicks on the sendButton.
       */
      public void onClick(ClickEvent event) {

        final String userID = user.box.getText();
        if (checkValidUser(user) && checkValidPassword(password)) {
          service.userExists(userID, new AsyncCallback<Integer>() {
            @Override
            public void onFailure(Throwable caught) {
            }

            @Override
            public void onSuccess(Integer result) {
              boolean exists = result != -1;
              String audioType = getAudioTypeFromPurpose(purpose.getValue());
              if (exists) {
                dialogBox.hide();
                langTest.rememberAudioType(audioType);
                userManager.storeUser(result, audioType, userID, PropertyHandler.LOGIN_TYPE.STUDENT);
              } else {
              /*  if (canSkipRegister(purposeValue)) {
                  dialogBox.hide();
                  sendNameToServer(registrationInfo, audioType, userID);
                } else {*/
                System.out.println("reveal!");
                accordion.show();
                checkThenRegister(audioType, registrationInfo, dialogBox, userID);
              }
            }
          });
        }
      }
    };
  }

  private void checkThenRegister(String audioType, RegistrationInfo registrationInfo, Modal dialogBox, String userID) {
    if (highlightIntegerBox(registrationInfo.ageEntryGroup)) {
      if (highlightIntegerBox(registrationInfo.weeks, MIN_WEEKS, MAX_WEEKS)) {
        if (registrationInfo.dialectGroup.getText().isEmpty()) {
          markError(registrationInfo.dialectGroup, "Please enter a language dialect.");
        } else {
          dialogBox.hide();
          sendNameToServer(registrationInfo, audioType, userID);
        }
      } else {
        markError(registrationInfo.weeks, "Please enter weeks between " + MIN_WEEKS + " and " + MAX_WEEKS + ".");
      }
    } else {
      markError(registrationInfo.ageEntryGroup, "Please enter age between " + MIN_AGE + " and " + MAX_AGE + ".");
    }
  }

  private String getAudioTypeFromPurpose(String purposeValue) {
    return purposeValue.equalsIgnoreCase("Practice") ?
      Result.AUDIO_TYPE_PRACTICE : (purposeValue.equalsIgnoreCase("Demo") ?
      Result.AUDIO_TYPE_DEMO :
      Result.AUDIO_TYPE_FAST_AND_SLOW);
  }

  private boolean canSkipRegister(String purposeValue) {
    return purposeValue.equalsIgnoreCase("Practice") || purposeValue.equalsIgnoreCase("Demo");
  }

  private boolean checkValidUser(FormField user) {
    final String userID = user.box.getText();
    if (userID.length() > USER_ID_MAX_LENGTH) {
      markError(user, "Please enter a user id of reasonable length.");
      return false;
    } else if (userID.length() == 0) {
      markError(user, "Please enter a user id.");
      return false;
    }
    return true;
  }

  private boolean checkValidPassword(FormField password) {
    final String userID = password.box.getText();
    if (userID.length() > USER_ID_MAX_LENGTH) {
      markError(password, "Please enter a password of reasonable length.");
      return false;
    } else if (userID.length() == 0) {
      markError(password, "Please enter the password that you've been told.");
      return false;
    }
    return true;
  }

  private FormField getDialect(Panel dialogBox) {
    final FormField dialectGroup = addControlFormField(dialogBox, "Dialect");
    dialectGroup.box.addStyleName("topMargin");

    dialectGroup.box.addKeyUpHandler(new KeyUpHandler() {
      public void onKeyUp(KeyUpEvent event) {
        if (dialectGroup.box.getText().length() > 0) {
          dialectGroup.group.setType(ControlGroupType.NONE);
        }
      }
    });
    return dialectGroup;
  }

  protected ListBox getListBox2(List<String> values) {
    final ListBox genderBox = new ListBox(false);
    for (String s : values) {
      genderBox.addItem(s);
    }
    genderBox.setWidth("60px");
    return genderBox;
  }

  /**
   * Send the name from the nameField to the server and wait for a response.
   *
   * @see #makeCloseHandler(com.github.gwtbootstrap.client.ui.Modal, mitll.langtest.client.user.StudentDialog.RegistrationInfo, mitll.langtest.client.user.UserDialog.FormField, mitll.langtest.client.user.UserDialog.FormField, mitll.langtest.client.user.UserDialog.ListBoxFormField, com.github.gwtbootstrap.client.ui.AccordionGroup)
   */
  private void sendNameToServer(RegistrationInfo registrationInfo, String audioType, String userID) {
    int weeksValue = registrationInfo.weeks.getText().isEmpty() ? 0 : Integer.parseInt(registrationInfo.weeks.getText());
    addUser(0, weeksValue, registrationInfo, audioType, userID);
  }

  /**
   * @param monthsOfExperience
   * @param userID
   * @see #displayLoginBox()
   */
  private void addUser(int monthsOfExperience, final int weeksOfExperience,
                       final RegistrationInfo registrationInfo,
                       final String audioType, String userID) {
    int age = getAge(registrationInfo.ageEntryGroup.box);
    String gender = registrationInfo.genderGroup.getValue();

    AsyncCallback<Long> async = getAddDLIUserCallback(weeksOfExperience, registrationInfo, audioType);
    addUser(age, gender, monthsOfExperience, registrationInfo.dialectGroup.getText(), "", userID, async);
  }

  private AsyncCallback<Long> getAddDLIUserCallback(final int weeksOfExperience, final RegistrationInfo registrationInfo, final String audioType) {
    return new AsyncCallback<Long>() {
      public void onFailure(Throwable caught) {
        Window.alert("addUser : Couldn't contact server.");
      }

      public void onSuccess(Long result) {
        System.out.println("addUser : server result is " + result);
        userManager.storeUser(result, audioType, "" + result, PropertyHandler.LOGIN_TYPE.STUDENT);

        DLIUser dliUser = new DLIUser(result, weeksOfExperience,
          new DLIUser.ILRLevel(registrationInfo.reading.getValue(), registrationInfo.rilr.getValue()),
          new DLIUser.ILRLevel(registrationInfo.listening.getValue(), registrationInfo.lilr.getValue()),
          new DLIUser.ILRLevel(registrationInfo.speaking.getValue(), registrationInfo.silr.getValue()),
          new DLIUser.ILRLevel(registrationInfo.writing.getValue(), registrationInfo.wilr.getValue())
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

  /**
   * @param age
   * @param gender
   * @param monthsOfExperience
   * @see UserManager#addAnonymousUser()
   */
  public void addUser(int age, String gender, int monthsOfExperience,
                      String audioType) {
    addUser(age, gender, monthsOfExperience, "", PropertyHandler.LOGIN_TYPE.ANONYMOUS, audioType);
  }

  /**
   * @param age
   * @param gender
   * @param monthsOfExperience
   * @param dialect
   * @param loginType
   * @see #addUser
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
        userManager.storeUser(result, audioType, "" + result, loginType);
      }
    };
    addUser(age, gender, monthsOfExperience, dialect, "", "", async);
  }

  private void addUser(int age, String gender, int monthsOfExperience, String dialect, String nativeLang, String userID,
                       AsyncCallback<Long> async) {
    service.addUser(age,
      gender,
      monthsOfExperience, dialect, nativeLang, userID, async);
  }

  private boolean highlightIntegerBox(FormField ageEntryGroup) {
    return highlightIntegerBox(ageEntryGroup, MIN_AGE, MAX_AGE, TEST_AGE);
  }

  private boolean highlightIntegerBox(FormField ageEntryGroup, int min, int max) {
    return highlightIntegerBox(ageEntryGroup, min, max, Integer.MAX_VALUE);
  }

  private boolean highlightIntegerBox(FormField ageEntryGroup, int min, int max, int exception) {
    String text = ageEntryGroup.box.getText();
    boolean validAge = false;
    if (text.length() == 0) {
      ageEntryGroup.group.setType(ControlGroupType.WARNING);
    } else {
      try {
        int age = Integer.parseInt(text);
        validAge = (age >= min && age <= max) || age == exception;
        ageEntryGroup.group.setType(validAge ? ControlGroupType.NONE : ControlGroupType.ERROR);
      } catch (NumberFormatException e) {
        ageEntryGroup.group.setType(ControlGroupType.ERROR);
      }
    }

    return validAge;
  }

  private class RegistrationInfo {
    private FormField ageEntryGroup;
    private ListBoxFormField genderGroup;
    private FormField weeks;
    private ListBoxFormField reading;
    private ListBoxFormField listening;
    private ListBoxFormField speaking;
    private ListBoxFormField writing;
    private CheckBox rilr;
    private CheckBox lilr;
    private CheckBox silr;
    private CheckBox wilr;
    private FormField dialectGroup;
/*
    public FormField getAgeEntryGroup() {
      return ageEntryGroup;
    }

    public ListBoxFormField getGenderGroup() {
      return genderGroup;
    }

    public FormField getWeeks() {
      return weeks;
    }

    public ListBoxFormField getReading() {
      return reading;
    }

    public ListBoxFormField getListening() {
      return listening;
    }

    public ListBoxFormField getSpeaking() {
      return speaking;
    }

    public ListBoxFormField getWriting() {
      return writing;
    }

    public CheckBox getRilr() {
      return rilr;
    }

    public CheckBox getLilr() {
      return lilr;
    }

    public CheckBox getSilr() {
      return silr;
    }

    public CheckBox getWilr() {
      return wilr;
    }

    public FormField getDialectGroup() {
      return dialectGroup;
    }*/

    public RegistrationInfo(Panel dialogBox) {
      Form form = new Form();
      DOM.setStyleAttribute(form.getElement(), "marginBottom", "0px");

      form.addStyleName("form-horizontal");
      Fieldset fieldset = new Fieldset();
      form.add(fieldset);
      dialogBox.add(form);

      ageEntryGroup = addControlFormField(fieldset, "Your age");
      genderGroup = getListBoxFormField(fieldset, "Gender", getGenderBox());
      weeks = addControlFormField(fieldset, "Weeks of Experience");

      final ControlGroup ilrLevel = new ControlGroup();
      ilrLevel.addStyleName("leftFiveMargin");
      ilrLevel.add(new ControlLabel("Select your ILR level (check boxes below if estimating)"));
      dialogBox.add(ilrLevel);

      FluidRow row = getILRLevels();

/*      Controls controls = new Controls();
      controls.add(row);*/
      dialogBox.add(row);

      FluidRow row2 = getEstimating();

      dialogBox.add(row2);

      dialectGroup = getDialect(fieldset);
    }

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

      List<String> levels = Arrays.asList("0+", "1", "1+", "2", "2+", "3", "3+", "4");
      reading = getListBoxFormField(c1, "Reading", getListBox2(levels));
      listening = getListBoxFormField(c2, "Listening", getListBox2(levels));
      speaking = getListBoxFormField(c3, "Speaking", getListBox2(levels));
      writing = getListBoxFormField(c4, "Writing", getListBox2(levels));
      return row;
    }

    private FluidRow getEstimating() {
      FluidRow row2 = new FluidRow();

      Column cc0 = new Column(2, new HTML("Estimating:"));
      row2.add(cc0);

      rilr = new CheckBox();
      rilr.addStyleName("leftThirtyMargin");
      lilr = new CheckBox();
      lilr.addStyleName("leftThirtyMargin");
      silr = new CheckBox();
      silr.addStyleName("leftThirtyMargin");
      wilr = new CheckBox();
      wilr.addStyleName("leftThirtyMargin");
      Column cc1 = new Column(2, rilr);
      row2.add(cc1);
      Column cc2 = new Column(2, lilr);
      row2.add(cc2);
      Column cc3 = new Column(2, silr);
      row2.add(cc3);
      Column cc4 = new Column(2, wilr);
      row2.add(cc4);
      return row2;
    }
  }
}
