package mitll.langtest.client;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.ColumnSortEvent;
import com.google.gwt.user.cellview.client.SimplePager;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.Cookies;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import mitll.langtest.shared.User;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 5/15/12
 * Time: 11:32 AM
 * To change this template use File | Settings | File Templates.
 */
public class UserManager {
  private static final int EXPIRATION_MINUTES = 30;
  private static final int MIN_AGE = 6;
  private static final int MAX_AGE = 90;
  private LangTestDatabaseAsync service;
  private UserNotification langTest;
  //Logger logger = Logger.getLogger("UserManager");

  public UserManager(UserNotification lt, LangTestDatabaseAsync service) {
    this.langTest = lt;
    this.service = service;
  }

  // user tracking
  public void storeUser(long sessionID) {
    final long DURATION = 1000 * 60 * 60 * EXPIRATION_MINUTES; //duration remembering login. 2 weeks in this example.
    Date expires = new Date(System.currentTimeMillis() + DURATION);
    Cookies.setCookie("sid", "" + sessionID, expires);
    if (langTest != null) {
      langTest.gotUser(sessionID);
    }
  }

  /**
   * @see mitll.langtest.client.LangTest#login()
   */
  public void login() {
    int user = getUser();
    if ( user != -1) {
     // alert("login Cookie now " + Cookies.getCookie("sid"));
//      alert("user:login sessionID not null = " + sessionID + " and there are " + Cookies.getCookieNames().size() + " cookies!");
      langTest.gotUser(user);
    }
    else {
   //   alert("login Cookie now " + Cookies.getCookie("sid"));
      displayLoginBox();
    }
  }

  public int getUser() {
    String sid = Cookies.getCookie("sid");
    if (sid == null || sid.equals("-1")) {
     // System.err.println("sid not set!");
      return -1;
    }
    return Integer.parseInt(sid);
  }

  public void clearUser() {
    Cookies.setCookie("sid","-1");
    //alert("clearUser Cookie now " + Cookies.getCookie("sid"));
    //Cookies.removeCookie("sid");    // this doesn't always seem to work???
  }

  private void displayLoginBox() {
    // Create the popup dialog box
    final DialogBox dialogBox = new DialogBox();
    dialogBox.setText("Login Questions");
    dialogBox.setAnimationEnabled(true);

    // Enable glass background.
    dialogBox.setGlassEnabled(true);

    final Button closeButton = new Button("Login");
    closeButton.setEnabled(false);

    // We can set the id of a widget by accessing its Element
    closeButton.getElement().setId("closeButton");
    final TextBox ageEntryBox = new TextBox();
    ageEntryBox.addKeyUpHandler(new KeyUpHandler() {
      public void onKeyUp(KeyUpEvent event) {
        String text = ageEntryBox.getText();
        if (text.length() == 0) {
          closeButton.setEnabled(false);
          return;
        }
        try {
          int age = Integer.parseInt(text);
          closeButton.setEnabled (age > MIN_AGE && age < MAX_AGE);
        } catch (NumberFormatException e) {
          closeButton.setEnabled(false);
        }
      }
    });

    // Add a drop box with the list types
    final ListBox genderBox = new ListBox(false);
    for (String s : Arrays.asList("Male", "Female")) {
      genderBox.addItem(s);
    }
    genderBox.ensureDebugId("cwListBox-dropBox");
    VerticalPanel genderPanel = new VerticalPanel();
    genderPanel.setSpacing(4);
    //  dropBoxPanel.add(new HTML(constants.cwListBoxSelectCategory()));
    genderPanel.add(genderBox);

    // add experience drop box
    final ListBox experienceBox = new ListBox(false);
    List<String> choices = Arrays.asList(
      "0-3 months (Semester 1)",
      "4-6 months (Semester 1)",
      "7-9 months (Semester 2)",
      "10-12 months (Semester 2)",
      "13-16 months (Semester 3)",
      "Native speaker");
    final int lastChoice = choices.size()-1;
    for (String c : choices) {
      experienceBox.addItem(c);
    }
/*    experienceBox.addItem("More than 22 months");
    experienceBox.addItem("Native Speaker");*/
    experienceBox.ensureDebugId("cwListBox-dropBox");
    VerticalPanel experiencePanel = new VerticalPanel();
    experiencePanel.setSpacing(4);
    //  dropBoxPanel.add(new HTML(constants.cwListBoxSelectCategory()));
    experiencePanel.add(experienceBox);

    VerticalPanel dialogVPanel = new VerticalPanel();
    dialogVPanel.addStyleName("dialogVPanel");
    dialogVPanel.add(new HTML("<b>Please enter your age</b>"));
    dialogVPanel.add(ageEntryBox);
    dialogVPanel.add(new HTML("<br><b>Please select gender</b>"));
    dialogVPanel.add(genderPanel);
    dialogVPanel.add(new HTML("<br><b>Please select months of experience</b>"));
    dialogVPanel.add(experiencePanel);
    dialogVPanel.setHorizontalAlignment(VerticalPanel.ALIGN_RIGHT);
    dialogVPanel.add(closeButton);
    dialogBox.setWidget(dialogVPanel);

    // Create a handler for the sendButton and nameField
    class MyHandler implements ClickHandler, KeyUpHandler {
      /**
       * Fired when the user clicks on the sendButton.
       */
      public void onClick(ClickEvent event) {
        dialogBox.hide();
        sendNameToServer();
      }

      /**
       * Fired when the user types in the nameField.
       */
      public void onKeyUp(KeyUpEvent event) {
        if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER) {
          sendNameToServer();
        }
      }

      /**
       * Send the name from the nameField to the server and wait for a response.
       */
      private void sendNameToServer() {
        int monthsOfExperience = experienceBox.getSelectedIndex()*3;
        if (experienceBox.getSelectedIndex() == lastChoice) {
          monthsOfExperience = 20*12;
        }
       /* if (monthsOfExperience == 21) {
          monthsOfExperience++;
        }
        else if (monthsOfExperience == 24) {
          monthsOfExperience = 20*12;
        }*/
        service.addUser(Integer.parseInt(ageEntryBox.getText()),
          genderBox.getValue(genderBox.getSelectedIndex()),
          monthsOfExperience, new AsyncCallback<Long>() {
          public void onFailure(Throwable caught) {
            // Show the RPC error message to the user
            dialogBox.setText("Remote Procedure Call - Failure");
            dialogBox.center();
            closeButton.setFocus(true);
          }

          public void onSuccess(Long result) {
            System.out.println("server result is " + result);
            storeUser(result);
          }
        });
      }
    }

    // Add a handler to send the name to the server
    MyHandler handler = new MyHandler();
    closeButton.addClickHandler(handler);

    int left = (Window.getClientWidth() - 0) / 3;
    int top  = (Window.getClientHeight() - 0) / 3;
    dialogBox.setPopupPosition(left, top);

    dialogBox.show();
  }

  private Widget lastTable = null;
  private Button closeButton;
  public void showUsers() {
    // Create the popup dialog box
    final DialogBox dialogBox = new DialogBox();
    dialogBox.setText("Registered Users");

    // Enable glass background.
    dialogBox.setGlassEnabled(true);

    closeButton = new Button("Close");
    closeButton.setEnabled(true);
    closeButton.getElement().setId("closeButton");

    final VerticalPanel dialogVPanel = new VerticalPanel();
    dialogVPanel.setWidth("1200px");
    dialogBox.setWidth("1200px");

    int left = (Window.getClientWidth() - 0) / 10;
    int top  = (Window.getClientHeight() - 0) / 10;
    dialogBox.setPopupPosition(left, top);

    service.getUsers(new AsyncCallback<List<User>>() {
      public void onFailure(Throwable caught) {}
      public void onSuccess(List<User> result) {
        if (lastTable != null) {
          dialogVPanel.remove(lastTable);
          dialogVPanel.remove(closeButton);
        }

        Widget table = getTable(result);
        dialogVPanel.add(table);
        dialogVPanel.add(closeButton);

        lastTable = table;
        dialogBox.show();
      }
    });

    dialogBox.setWidget(dialogVPanel);

    // Add a handler to send the name to the server
    closeButton.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        dialogBox.hide();
      }
    });
  }

  private Widget getTable(List<User> result) {
    CellTable<User> table = new CellTable<User>();
    table.setWidth("1100px");
    TextColumn<User> id = new TextColumn<User>() {
      @Override
      public String getValue(User contact) { return ""+contact.id; }};
    id.setSortable(true);
    table.addColumn(id,"ID");

    TextColumn<User> age = new TextColumn<User>() {
      @Override
      public String getValue(User contact) { return ""+contact.age; }};
    age.setSortable(true);
    table.addColumn(age, "Age");

    TextColumn<User> gender = new TextColumn<User>() {
      @Override
      public String getValue(User contact) { return contact.gender == 0 ? "male" : "female"; }};
    gender.setSortable(true);
    table.addColumn(gender,"Gender");

    TextColumn<User> experience = new TextColumn<User>() {
      @Override
      public String getValue(User contact) { return ""+contact.experience + " months"; }};
    experience.setSortable(true);
    table.addColumn(experience,"Experience");

    TextColumn<User> ipaddr = new TextColumn<User>() {
      @Override
      public String getValue(User contact) { return ""+contact.ipaddr; }};
    ipaddr.setSortable(true);
    table.addColumn(ipaddr,"IP Addr");

    TextColumn<User> password = new TextColumn<User>() {
      @Override
      public String getValue(User contact) { return ""+contact.password; }};
    password.setSortable(true);
    table.addColumn(password,"Password");

    TextColumn<User> date = new TextColumn<User>() {
      @Override
      public String getValue(User contact) { return ""+new Date(contact.timestamp); }};
    date.setSortable(true);
    table.addColumn(date,"Time");

    // Create a data provider.
    ListDataProvider<User> dataProvider = new ListDataProvider<User>();

    // Connect the table to the data provider.
    dataProvider.addDataDisplay(table);

    // Add the data to the data provider, which automatically pushes it to the
    // widget.
    List<User> list = dataProvider.getList();
    for (User contact : result) {
      list.add(contact);
    }
    table.setRowCount(list.size());

    // Add a ColumnSortEvent.ListHandler to connect sorting to the
    // java.util.List.
    ColumnSortEvent.ListHandler<User> columnSortHandler = new ColumnSortEvent.ListHandler<User>(
      list);
    columnSortHandler.setComparator(id,
      new Comparator<User>() {
        public int compare(User o1, User o2) {
          if (o1 == o2) {
            return 0;
          }

          // Compare the name columns.
          if (o1 != null) {
            return (o2 != null) ? (int)(o1.id - o2.id) : 0;
          }
          return -1;
        }
      });
    table.addColumnSortHandler(columnSortHandler);

    // We know that the data is sorted alphabetically by default.
    table.getColumnSortList().push(id);

    // Create a SimplePager.
    SimplePager pager = new SimplePager();

    // Set the cellList as the display.
    pager.setDisplay(table);

    // Add the pager and list to the page.
    VerticalPanel vPanel = new VerticalPanel();
    vPanel.add(pager);
    vPanel.add(table);
    return vPanel;
  }
}
