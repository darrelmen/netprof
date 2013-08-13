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

  private static final int FIRST_POLL_PERIOD_MILLIS = 1000 * 3; // ten minutes
  private static final int INACTIVE_PERIOD_MILLIS = 1000 * 15; // ten minutes

  private Timer userTimer;

  public Taboo(UserManager userManager, LangTestDatabaseAsync service, LangTest langTest) {
    this.userManager = userManager;
    this.service = service;
    this.langTest = langTest;
  }

  public void initialCheck(final long fuserid) {
    userTimer = new Timer() {
      @Override
      public void run() {
        checkForPartner(fuserid);
      }
    };
    userTimer.schedule(FIRST_POLL_PERIOD_MILLIS);
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

  /**
   * TODO : Need some way to quit too - I don't want to play anymore.
   * @param fuserid
   */
  private void checkForPartner(final long fuserid) {
    cancelTimer();
    service.anyUsersAvailable(fuserid, new AsyncCallback<TabooState>() {
      @Override
      public void onFailure(Throwable caught) { Window.alert("Couldn't contact server."); }

      @Override
      public void onSuccess(TabooState result) {
        System.out.println("me : " + fuserid + " checking, anyUsersAvailable : " + result);
        if (result.isJoinedPair()) {
          if (result.isGiver()) {
            doModal(fuserid,"You are the giver", "Now choose the next sentence your partner will see to guess the vocabulary word.", true);
          }
          else {
            doModal(fuserid,"You are the receiver","Now choose the word that best fills in the blank in the sentence.",false);
          }

          // TODO : somehow we begin receiving items to do and to guess at.
          // do the right factory, make a panel that can receive stimulus and show choices
          // panel posts results back, giver notices results

        }
        else if (result.isAnyAvailable()) {
          doTabooModal(fuserid);
        }
        else {
          //Window.alert("do single player Mode");
          // TODO fill in single player mode
          pollForPartner();
        }
      }
    });
  }


  private void doModal(final long userID, String title, String message, final boolean isGiver) {
    final Modal modal = new Modal(true);
    modal.setTitle(title);
    Heading w = new Heading(4);
    w.setText(message);
    modal.add(w);

    final Button begin = new Button("Begin Game");
    begin.setType(ButtonType.PRIMARY);
    begin.setEnabled(true);

    begin.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        modal.hide();
        langTest.setTabooFactory(userID, isGiver);
        // TODO : set the factory for the receiver...
      }
    });
    modal.add(begin);

    modal.show();
  }

  private void cancelTimer() {
    if (userTimer != null) userTimer.cancel();
  }

  private void doTabooModal(final long userID) {
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

          // TODO : tell server we're playing... it will tell the partner to either be the giver or the receiver
          service.registerPair(userID, isGiver, new AsyncCallback<Void>() {
            @Override
            public void onFailure(Throwable caught) { Window.alert("Couldn't contact server."); }

            @Override
            public void onSuccess(Void result) {  // TODO : make the right exercise panel factory for the role (giver chooses and gives...)
              langTest.setTabooFactory(userID, isGiver);
            }
          });

          // if giver, then need to see wordlist, select next item to give to receiver
          // after giving, poll for answer submission by receiver - correct, move on to next item
          //   incorrect, choose next stimulus

          // if receiver, wait for giver to give you an item, then choose your response
        }
      }
    });
    askGiverReceiver.add(begin);

    askGiverReceiver.show();
  }
}
