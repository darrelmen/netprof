package mitll.langtest.client.user;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.CheckBox;
import com.github.gwtbootstrap.client.ui.Column;
import com.github.gwtbootstrap.client.ui.ControlGroup;
import com.github.gwtbootstrap.client.ui.ControlLabel;
import com.github.gwtbootstrap.client.ui.FluidRow;
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
import com.google.gwt.user.client.ui.HTML;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.PropertyHandler;
import mitll.langtest.shared.DLIUser;

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

  public StudentDialog(LangTestDatabaseAsync service, PropertyHandler props, UserManager userManager) {
    super(service, props);
    this.userManager = userManager;
  }

  /**
   * @see UserManager#login()
   */
  public void displayLoginBox() {
    final Modal dialogBox = getDialog();
    dialogBox.setMaxHeigth("650px");
    dialogBox.setHeight("600px");
    dialogBox.setTitle("Login Questions");

    final FormField ageEntryGroup = addControlFormField(dialogBox, "Your age");
    final ListBoxFormField genderGroup = getListBoxFormField(dialogBox, "Gender", getGenderBox());
    final FormField weeks = addControlFormField(dialogBox, "Weeks of Experience");

    final ControlGroup ilrLevel = new ControlGroup();
    ilrLevel.addStyleName("leftFiveMargin");
    ilrLevel.add(new ControlLabel("Select your ILR level (check boxes below if estimating)"));
    dialogBox.add(ilrLevel);

    FluidRow row = new FluidRow();
    Column column = new Column(2,new HTML());
    row.add(column);

    List<String> levels = java.util.Arrays.asList("0+", "1", "1+", "2", "2+", "3", "3+", "4");
    Column c1 = new Column(2);     row.add(c1);
    Column c2 = new Column(2);     row.add(c2);
    Column c3 = new Column(2);     row.add(c3);
    Column c4 = new Column(2);     row.add(c4);

    ListBox listBox = getListBox2(levels);
    final ListBoxFormField reading = getListBoxFormField(c1, "Reading", listBox);
    final ListBoxFormField listening = getListBoxFormField(c2, "Listening", getListBox2(levels));
    final ListBoxFormField speaking = getListBoxFormField(c3, "Speaking", getListBox2(levels));
    final ListBoxFormField writing = getListBoxFormField(c4, "Writing", getListBox2(levels));

    dialogBox.add(row);

    FluidRow row2 = new FluidRow();

    Column cc0 = new Column(2,new HTML("Estimating:"));
    row2.add(cc0);

    final  CheckBox rilr = new CheckBox(); rilr.addStyleName("leftThirtyMargin");
    final  CheckBox lilr = new CheckBox(); lilr.addStyleName("leftThirtyMargin");
    final  CheckBox silr = new CheckBox(); silr.addStyleName("leftThirtyMargin");
    final CheckBox wilr = new CheckBox();  wilr.addStyleName("leftThirtyMargin");
    Column cc1 = new Column(2, rilr);     row2.add(cc1);
    Column cc2 = new Column(2, lilr);     row2.add(cc2);
    Column cc3 = new Column(2, silr);     row2.add(cc3);
    Column cc4 = new Column(2, wilr);     row2.add(cc4);

    dialogBox.add(row2);

    final FormField dialectGroup = addControlFormField(dialogBox, "Dialect");
    dialectGroup.box.addStyleName("topMargin");

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
        if (highlightIntegerBox(ageEntryGroup)) {
          if (highlightIntegerBox(weeks, MIN_WEEKS, MAX_WEEKS)) {
            if (dialectGroup.getText().isEmpty()) {
              markError(dialectGroup, "Please enter a language dialect.");
            } else {
              dialogBox.hide();
              sendNameToServer();
            }
          } else {
            markError(weeks, "Please enter weeks between " +MIN_WEEKS + " and " + MAX_WEEKS + ".");
          }
        } else {
          markError(ageEntryGroup, "Please enter age between " + MIN_AGE + " and " + MAX_AGE + ".");
        }
      }

      /**
       * Send the name from the nameField to the server and wait for a response.
       */
      private void sendNameToServer() {
       int weeksValue = Integer.parseInt(weeks.getText());
        addUser(0, ageEntryGroup.box, genderGroup.box, dialectGroup.box, weeksValue,
          reading.getValue(), listening.getValue(), speaking.getValue(), writing.getValue(),
          rilr.getValue(), lilr.getValue(), silr.getValue(), wilr.getValue());
      }
    }

    // Add a handler to send the name to the server
    closeButton.addClickHandler(new MyHandler());

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

  protected ListBox getListBox2(List<String> values) {
    final ListBox genderBox = getListBox(values);
    genderBox.setWidth("60px");
    // genderBox.ensureDebugId("cwListBox-dropBox");
    return genderBox;
  }


  /**
   * @param monthsOfExperience
   * @param ageEntryBox
   * @param genderBox
   * @param dialectBox
   * @see #displayLoginBox()
   */
  private void addUser(int monthsOfExperience, TextBox ageEntryBox, ListBox genderBox, TextBox dialectBox,
                      final int weeksOfExperience,
                      final  String reading, final String listening, final  String speaking,  final String writing,
                      final  boolean rEst, final  boolean lEst, final  boolean sEst, final  boolean wEst) {
    int age = getAge(ageEntryBox);
    String gender = genderBox.getValue(genderBox.getSelectedIndex());

    AsyncCallback<Long> async = new AsyncCallback<Long>() {
      public void onFailure(Throwable caught) {
        Window.alert("addUser : Couldn't contact server.");
      }

      public void onSuccess(Long result) {
        System.out.println("addUser : server result is " + result);
        userManager.storeUser(result, "", "" + result, PropertyHandler.LOGIN_TYPE.STUDENT);

        DLIUser dliUser = new DLIUser(result, weeksOfExperience,
          new DLIUser.ILRLevel(reading,rEst),
          new DLIUser.ILRLevel(listening,lEst),
          new DLIUser.ILRLevel(speaking,sEst),
          new DLIUser.ILRLevel(writing,wEst)
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
    addUser(age, gender, monthsOfExperience, dialectBox.getText(), async);
  }

  /**
   * @param age
   * @param gender
   * @param monthsOfExperience
   * @see UserManager#addAnonymousUser()
   */
  public void addUser(int age, String gender, int monthsOfExperience) {
    addUser(age, gender, monthsOfExperience, "", PropertyHandler.LOGIN_TYPE.ANONYMOUS);
  }

  /**
   * @param age
   * @param gender
   * @param monthsOfExperience
   * @param dialect
   * @param loginType
   * @see #addUser(int, String, int)
   */
  private void addUser(int age, String gender, int monthsOfExperience, String dialect,
                       final PropertyHandler.LOGIN_TYPE loginType) {
    AsyncCallback<Long> async = new AsyncCallback<Long>() {
      public void onFailure(Throwable caught) {
        Window.alert("addUser : Couldn't contact server.");
      }

      public void onSuccess(Long result) {
        System.out.println("addUser : server result is " + result);
        userManager.storeUser(result, "", "" + result, loginType);
      }
    };
    addUser(age, gender, monthsOfExperience, dialect, async);
  }

  private void addUser(int age, String gender, int monthsOfExperience, String dialect, AsyncCallback<Long> async) {
    service.addUser(age,
      gender,
      monthsOfExperience, dialect, async);
  }

  private Button makeCloseButton() {
    final Button closeButton = new Button("Login");
    closeButton.setType(ButtonType.PRIMARY);

    // We can set the id of a widget by accessing its Element
    closeButton.getElement().setId("closeButton");
    return closeButton;
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
        validAge = (age > min && age < max) || age == exception;
        ageEntryGroup.group.setType(validAge ? ControlGroupType.NONE : ControlGroupType.ERROR);
      } catch (NumberFormatException e) {
        ageEntryGroup.group.setType(ControlGroupType.ERROR);
      }
    }

    return validAge;
  }
}
