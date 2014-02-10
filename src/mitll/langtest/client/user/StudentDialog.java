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
import com.github.gwtbootstrap.client.ui.ListBox;
import com.github.gwtbootstrap.client.ui.Modal;
import com.github.gwtbootstrap.client.ui.RadioButton;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.constants.ControlGroupType;
import com.github.gwtbootstrap.client.ui.constants.Placement;
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
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.*;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.PropertyHandler;
import mitll.langtest.shared.DLIUser;
import mitll.langtest.shared.Result;

import java.util.ArrayList;
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
  private static final int ILR_CHOICE_WIDTH = 80;
  private static final int MIN_WEEKS = 0;
  private static final int MAX_WEEKS = 104;
  private static final int MIN_LENGTH_USER_ID = 4;
  private static final String DATA_COLLECTION = "Data Collection";
  private static final String PRACTICE = "Practice";
  private static final String DEMO = "Demo";
  private static final String PURPOSE = "Purpose";
  private final UserManager userManager;
  private final UserNotification langTest;
  private List<String> purposes;

  public StudentDialog(LangTestDatabaseAsync service, PropertyHandler props, UserManager userManager, UserNotification userNotification) {
    super(service, props, userManager, userNotification);
    this.userManager = userManager;
    this.langTest = userNotification;
    purposes = new ArrayList<String>();
    purposes.add(props.getPurposeDefault());

    for (String purpose : Arrays.asList(DATA_COLLECTION, PRACTICE, DEMO)) {
      if (!purpose.equalsIgnoreCase(props.getPurposeDefault())) purposes.add(purpose);
    }
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
    ListBox listBox2 = getListBox2(purposes, ILR_CHOICE_WIDTH);
    //listBox2.addStyleName("leftFiveMargin");



   // DivWidget row = new DivWidget();
    //Panel hp = new HorizontalPanel();
    Panel hp = new DivWidget();
    fieldset.add(hp);
   // divLeft.addStyleName("floatLeft");

   // divRight.addStyleName("floatRight");

    //dialogBox.add(hp);

    final ListBoxFormField purpose = getListBoxFormField(hp, PURPOSE, listBox2, true);
    purpose.box.setFocus(true);
    purpose.group.addStyleName("floatLeft");

    recordingOrder = getRecordingOrder(hp, true);
    recordingOrder.group.addStyleName("floatRight");

    final FormField user = addControlFormField(fieldset, "User ID", MIN_LENGTH_USER_ID);

    user.setVisible(purpose.getValue().equals(DATA_COLLECTION));
    user.box.addStyleName("leftFiveMargin");
    user.group.addStyleName("floatLeft");

    //final FormField password = addControlFormField(fieldset, "Password", true);

    purpose.box.setWidth("150px");

    Panel register = new FlowPanel();
    RegistrationInfo registrationInfo = new RegistrationInfo(register);
    final AccordionGroup accordion = getAccordion(dialogBox, register);
    accordion.setVisible(!canSkipRegister(purpose.getValue()));

    purpose.box.addChangeHandler(new ChangeHandler() {
      @Override
      public void onChange(ChangeEvent event) {
        boolean b = canSkipRegister(purpose.getValue());
        user.setVisible(purpose.getValue().equals(DATA_COLLECTION));
        accordion.setVisible(!canSkipRegister(purpose.getValue()));

        if (b) {
          accordion.hide();
        }
        else {
          final String userID = user.box.getText();
          if (!userID.isEmpty()
            //&& checkValidPassword(password)
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
    closeButton.addClickHandler(makeCloseHandler(dialogBox, registrationInfo, user, /*password,*/ purpose, accordion));

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
   * @paramx password
   * @param purpose
   * @param accordion
   * @see #displayLoginBox()
   */
  private ClickHandler makeCloseHandler(final Modal dialogBox,
                                        final RegistrationInfo registrationInfo,
                                        final FormField user,
                                        final ListBoxFormField purpose,
                                        final AccordionGroup accordion) {
    return new ClickHandler() {
      /**
       * Do validation.
       * Fired when the user clicks on the sendButton.
       */
      public void onClick(ClickEvent event) {
        String purposeSetting = purpose.getValue();
        final String audioType = getAudioTypeFromPurpose(purposeSetting);
        if (canSkipRegister(purposeSetting)) {
          dialogBox.hide();
          langTest.rememberAudioType(audioType);
          addAnonymousUser(audioType);
        } else {
          checkUserAndMaybeRegister(audioType, user, dialogBox, accordion, registrationInfo);
        }
      }
    };
  }

  private void checkUserAndMaybeRegister(final String audioType, FormField user, final Modal dialogBox,
                                         final AccordionGroup accordion, final RegistrationInfo registrationInfo) {
    final String userID = user.box.getText();
    if (checkValidUser(user)
      //&& checkValidPassword(password)
      ) {
      service.userExists(userID, new AsyncCallback<Integer>() {
        @Override
        public void onFailure(Throwable caught) {}

        @Override
        public void onSuccess(Integer result) {
          boolean exists = result != -1;
          if (exists) {
            dialogBox.hide();
            langTest.rememberAudioType(audioType);
            userManager.storeUser(result, audioType, userID, PropertyHandler.LOGIN_TYPE.STUDENT);
          } else {
            accordion.show();
            checkThenRegister(audioType, registrationInfo, dialogBox, userID);
          }
          setRecordingOrder();
        }
      });
    }
  }

  private void checkThenRegister(String audioType, RegistrationInfo registrationInfo, Modal dialogBox, String userID) {
    if (highlightIntegerBox(registrationInfo.ageEntryGroup)) {
      if (highlightIntegerBox(registrationInfo.weeks, MIN_WEEKS, MAX_WEEKS)) {
        if (registrationInfo.genderGroup.getValue().equals(UNSET)) {
          registrationInfo.genderGroup.markSimpleError("Please select a gender.", Placement.LEFT);
        } else {
          if (registrationInfo.dialectGroup.getText().isEmpty()) {
            markError(registrationInfo.dialectGroup, "Please enter a language dialect.", Placement.LEFT);
          } else if (registrationInfo.checkValidity() && registrationInfo.checkValidity2()) {
            dialogBox.hide();
            sendNameToServer(registrationInfo, audioType, userID);
          }
        }
      } else {
        markError(registrationInfo.weeks, "Please enter weeks between " + MIN_WEEKS + " and " + MAX_WEEKS + ".");
      }
    } else {
      markError(registrationInfo.ageEntryGroup.group, registrationInfo.ageEntryGroup.box, "",
       "Enter age between " + MIN_AGE + " and " + MAX_AGE + ".");
    }
  }

  private String getAudioTypeFromPurpose(String purposeValue) {
    return purposeValue.equalsIgnoreCase(PRACTICE) ?
      Result.AUDIO_TYPE_PRACTICE : (purposeValue.equalsIgnoreCase(DEMO) ?
      Result.AUDIO_TYPE_DEMO :
      Result.AUDIO_TYPE_REGULAR);
  }

  private boolean canSkipRegister(String purposeValue) {
    return purposeValue.equalsIgnoreCase(PRACTICE) || purposeValue.equalsIgnoreCase(DEMO);
  }

  private boolean checkValidUser(FormField user) {
    final String userID = user.box.getText();
    int length = userID.length();
    if (length == 0) {
      markError(user, "Please enter a user id.");
      return false;
    } else if (length < MIN_LENGTH_USER_ID || length > USER_ID_MAX_LENGTH) {
      markError(user, "Please enter a user id of reasonable length.");
      return false;
    }
    user.clearError();
    return true;
  }

/*  private boolean checkValidPassword(FormField password) {
    final String userID = password.box.getText();
    if (userID.length() > USER_ID_MAX_LENGTH) {
      markError(password, "Please enter a password of reasonable length.");
      return false;
    } else if (userID.length() == 0) {
      markError(password, "Please enter the password that you've been told.");
      return false;
    }
    return true;
  }*/

  private FormField getDialect(Panel dialogBox) {
    final FormField dialectGroup = addControlFormField(dialogBox, "Dialect");
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

  private void addAnonymousUser(String audioType) {  addUser(89, "male", 0, audioType);  }

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
        userManager.storeUser(result, audioType, "" + result, loginType);
      }
    };
    addUser(age, gender, monthsOfExperience, dialect, "", "", async);
  }

  private void addUser(int age, String gender, int monthsOfExperience, String dialect, String nativeLang, String userID,
                       AsyncCallback<Long> async) {
    service.addUser(age,
      gender,
      monthsOfExperience, nativeLang, dialect, userID, async);
  }

  private boolean highlightIntegerBox(FormField ageEntryGroup) {
    return highlightIntegerBox(ageEntryGroup, MIN_AGE, MAX_AGE, TEST_AGE);
  }

  private class RegistrationInfo {
    private FormField ageEntryGroup;
    private ListBoxFormField genderGroup;
    private FormField weeks;
    private ListBoxFormField reading;
    private ListBoxFormField listening;
    private ListBoxFormField speaking;
    private ListBoxFormField writing;
    private FormField dialectGroup;

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
      ilrLevel.add(new ControlLabel("Select your ILR level (check boxes below if estimating)"));
      dialogBox.add(ilrLevel);
      ilrLevel.addStyleName("floatLeft");

      FluidRow row = getILRLevels();
      row.addStyleName("floatLeft");

      dialogBox.add(row);

      FluidRow row2 = getEstimating2();
      dialogBox.add(row2);

    }

    public boolean checkValidity() {
      for (ListBoxFormField f : Arrays.asList(reading,listening,speaking,writing)) {
         if (f.box.getValue().equals(UNSET)) {
           f.markSimpleError("Choose a level", Placement.TOP);
           return false;
         }
      }
      return true;
    }

    public boolean checkValidity2() {
      for (YesNo f : ilrs) {
        if (!f.markSimpleError(Placement.TOP)) {
          return false;
        }
      }
      return true;
    }

    public boolean getValue(String ilr) {
      for (YesNo yn : ilrs) {
        if (yn.getName().equals(ilr)) return yn.getValue();
      }
      return false;
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

      List<String> levels = Arrays.asList(UNSET,"0+", "1", "1+", "2", "2+", "3", "3+", "4");
      reading   = getListBoxFormField(c1, "Reading", getListBox2(levels));
      listening = getListBoxFormField(c2, "Listening", getListBox2(levels));
      speaking  = getListBoxFormField(c3, "Speaking", getListBox2(levels));
      writing   = getListBoxFormField(c4, "Writing", getListBox2(levels));
      return row;
    }

    private List<YesNo> ilrs = new ArrayList<YesNo>();
    private FluidRow getEstimating2() {
      FluidRow row2 = new FluidRow();

      Column cc0 = new Column(2, new HTML("Estimating:"));
      row2.add(cc0);

      for (String ilr : Arrays.asList("rilr","lilr","silr","wilr")) {
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
    public ControlGroup group;

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

    public boolean markSimpleError() {
      return markSimpleError(Placement.RIGHT);
    }

    public boolean markSimpleError(Placement placement) {
      if (!yes.getValue() && !no.getValue()) {
        markError(group, yes, "Please choose", "Click yes or no.", placement);
        return false;
      } else return true;
    }
    public boolean getValue() { return yes.getValue(); }

    public String getName() {
      return name;
    }
  }
}
