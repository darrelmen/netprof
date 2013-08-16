package mitll.langtest.client.taboo;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.ControlGroup;
import com.github.gwtbootstrap.client.ui.Controls;
import com.github.gwtbootstrap.client.ui.Heading;
import com.github.gwtbootstrap.client.ui.Modal;
import com.github.gwtbootstrap.client.ui.RadioButton;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import mitll.langtest.client.LangTest;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.user.UserManager;
import mitll.langtest.shared.TabooState;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 8/9/13
 * Time: 5:40 PM
 * To change this template use File | Settings | File Templates.
 */
public class Taboo {
  private final UserManager userManager;
  private final LangTestDatabaseAsync service;
  private LangTest langTest;
  boolean startedSinglePlayer;

  //private static final int FIRST_POLL_PERIOD_MILLIS = 1000 * 3; // ten minutes
  private static final int INACTIVE_PERIOD_MILLIS = 1000 * 2; // ten minutes
  private static final int INACTIVE_PERIOD_MILLIS2 = 1000 * 5; // ten minutes

  private Timer userTimer, onlineTimer;

  /**
   * @see mitll.langtest.client.LangTest#onModuleLoad2()
   * @param userManager
   * @param service
   * @param langTest
   */
  public Taboo(UserManager userManager, LangTestDatabaseAsync service, LangTest langTest) {
    this.userManager = userManager;
    this.service = service;
    this.langTest = langTest;
  }

  public void initialCheck(final long fuserid) {
    checkForPartner(fuserid);
  }

  private void showUserState(String title, String message, final long fuserid) {
    final Modal modal = new Modal(true);
    modal.setTitle(title);
    Heading w = new Heading(4);
    w.setText(message);
    modal.add(w);

    final Button begin = new Button("OK");
    begin.setType(ButtonType.PRIMARY);
    begin.setEnabled(true);

    begin.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        modal.hide();
        checkForPartner(fuserid);
      }
    });
    modal.add(begin);

    modal.show();
  }

  /**
   * TODO : Need some way to quit too - I don't want to play anymore.
   * @param fuserid
   */
  private void checkForPartner(final long fuserid) {
    cancelTimer();
    if (fuserid == -1) {
      System.out.println("checkForPartner : me : " + fuserid + " has signed out.");
      return;
    }
    service.anyUsersAvailable(fuserid, new AsyncCallback<TabooState>() {
      @Override
      public void onFailure(Throwable caught) {
        Window.alert("Couldn't contact server.");
      }

      @Override
      public void onSuccess(TabooState result) {
        //System.out.println("checkForPartner.onSuccess : me : " + fuserid + " checking, anyUsersAvailable : " + result);
        if (result.isJoinedPair()) {
          if (result.isGiver()) {
            afterRoleDeterminedConfirmation(fuserid,
              "You are the giver",
              "Now choose the next sentence your partner will see to guess the vocabulary word.",
              "Sign out to stop playing.",
              true);
          } else {
            afterRoleDeterminedConfirmation(fuserid,
              "You are the receiver",
              "Now choose the word that best fills in the blank in the sentence.",
              "Sign out to stop playing.",
              false);
          }
          pollForPartnerOnline(fuserid, result.isGiver());
        } else if (result.isAnyAvailable()) {
          chooseRoleModal(fuserid);
        } else {
          if (!startedSinglePlayer) {
            System.out.println("me : " + fuserid + " doing single player");

            langTest.setTabooFactory(fuserid, false, true);
            startedSinglePlayer = true;
          }
          pollForPartner();
        }
      }
    });
  }

  private void pollForPartnerOnline(final long fuserid, final boolean isGiver) {
    onlineTimer = new Timer() {
      @Override
      public void run() {
/*        System.out.println("pollForPartnerOnline : checking if " +
          (isGiver ? " receiver partner " : " giver partner ") +
          " of me, " + fuserid + ", is online...");*/
        service.isPartnerOnline(fuserid, isGiver, new AsyncCallback<Boolean>() {
          @Override
          public void onFailure(Throwable caught) {
            Window.alert("Couldn't contact server.");
          }

          @Override
          public void onSuccess(Boolean result) {
            if (result) {
              pollForPartnerOnline(fuserid, isGiver);
            } else {
              onlineTimer.cancel();
              showUserState("Partner Signed Out", "Your partner signed out, will check for another...", fuserid);
            }
          }
        });

      }
    };
    onlineTimer.schedule(INACTIVE_PERIOD_MILLIS2);
  }

  private void pollForPartner() {
    userTimer = new Timer() {
      @Override
      public void run() {
        checkForPartner(userManager.getUser());
      }
    };
    userTimer.schedule(INACTIVE_PERIOD_MILLIS);
  }

  private void afterRoleDeterminedConfirmation(final long userID, String title, String message, String message2, final boolean isGiver) {
    final Modal modal = new Modal(true);
    modal.setTitle(title);
    modal.add(new Heading(4, message));
    if (message2 != null) {
      modal.add(new Heading(4, message2));
    }

    final Button begin = new Button("Begin Game");
    begin.setType(ButtonType.PRIMARY);
    begin.setEnabled(true);

    begin.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        modal.hide();
        langTest.setTabooFactory(userID, isGiver, false);
        startedSinglePlayer = false;
      }
    });
    modal.add(begin);

    modal.show();
  }

  private void cancelTimer() {
    if (userTimer != null) userTimer.cancel();
  }

  private void chooseRoleModal(final long userID) {
    final Modal askGiverReceiver = new Modal(true);
    askGiverReceiver.setCloseVisible(false);

    askGiverReceiver.setTitle("Choose role");
    Heading w = new Heading(4);
    w.setText("Would you like to give items or receive them?");
    askGiverReceiver.add(w);
    final ControlGroup recordingStyle = new ControlGroup();

    // recordingStyle.add(new ControlLabel("<b>Audio Recording Style</b>"));
    Controls controls = new Controls();

    final RadioButton give    = new RadioButton("Giver","Give");
    final RadioButton receive = new RadioButton("Giver","Receive");
    controls.add(give);
    controls.add(receive);
    recordingStyle.add(controls);
    askGiverReceiver.add(recordingStyle);

    final Button begin = new Button("Begin Game");
    begin.setType(ButtonType.PRIMARY);
    begin.setEnabled(true);

    begin.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        final Boolean isGiver = give.getValue();
        if (!isGiver && !receive.getValue()) {
          Window.alert("please choose to give or receive.");  // TODO better highlighting
        } else {
          askGiverReceiver.hide();

          service.registerPair(userID, isGiver, new AsyncCallback<Void>() {
            @Override
            public void onFailure(Throwable caught) { Window.alert("Couldn't contact server."); }

            @Override
            public void onSuccess(Void result) {
              langTest.setTabooFactory(userID, isGiver, false);
            }
          });
        }
      }
    });
    askGiverReceiver.add(begin);
    askGiverReceiver.show();
  }
}
