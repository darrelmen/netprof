package mitll.langtest.client.taboo;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.ControlGroup;
import com.github.gwtbootstrap.client.ui.Controls;
import com.github.gwtbootstrap.client.ui.Heading;
import com.github.gwtbootstrap.client.ui.Modal;
import com.github.gwtbootstrap.client.ui.RadioButton;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.github.gwtbootstrap.client.ui.event.HiddenEvent;
import com.github.gwtbootstrap.client.ui.event.HiddenHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import mitll.langtest.client.LangTest;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.ListInterface;
import mitll.langtest.client.user.UserManager;
import mitll.langtest.shared.taboo.GameInfo;
import mitll.langtest.shared.taboo.PartnerState;
import mitll.langtest.shared.taboo.TabooState;

import java.util.Collection;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 8/9/13
 * Time: 5:40 PM
 * To change this template use File | Settings | File Templates.
 */
public class Taboo {
  public static final String SIGN_OUT_TO_STOP_PLAYING = "Sign out to stop playing.";
  private final UserManager userManager;
  private final LangTestDatabaseAsync service;
  private LangTest langTest;
  boolean inSinglePlayer = false;

  private static final int INACTIVE_PERIOD_MILLIS = 1000 * 2; // ten minutes
  private static final int INACTIVE_PERIOD_MILLIS2 = 1000 * 2; // ten minutes

  private Timer userTimer, onlineTimer;
  private ExerciseController controller;
  /**
   * @see mitll.langtest.client.LangTest#onModuleLoad2()
   * @param userManager
   * @param service
   * @param langTest
   */
  public Taboo(UserManager userManager, LangTestDatabaseAsync service, LangTest langTest, ExerciseController controller) {
    this.userManager = userManager;
    this.service = service;
    this.langTest = langTest;
    this.controller = controller;
  }

  public void initialCheck(final long fuserid) {
    System.out.println("Taboo.initialCheck : me : " + fuserid + " ...");

    checkForPartner(fuserid);
  }

  /**
   * TODO : Need some way to quit too - I don't want to play anymore. Sign out?
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
        if (result.isAnyAvailable()) {
          System.out.println("checkForPartner.onSuccess : me : " + fuserid + " checking, anyUsersAvailable : " + result);
        }
        if (result.isJoinedPair()) {
          if (result.isGiver()) {
            afterRoleDeterminedConfirmation(fuserid,
              "You are the giver",
              "Now choose the next sentence your partner will see to guess the vocabulary word.",
              SIGN_OUT_TO_STOP_PLAYING,
              true);
          } else {
            afterRoleDeterminedConfirmation(fuserid,
              "You are the receiver",
              "Now choose the word that best fills in the blank in the sentence.",
              SIGN_OUT_TO_STOP_PLAYING,
              false);
          }

         // System.out.println("\n----> checkForPartner.onSuccess : me : " + fuserid + " isGiver " + result.isGiver());
        //  exerciseList.rememberAndLoadFirst(result.getExerciseShells());
        //  GameInfo gameInfo = result.getGameInfo();
          pollForPartnerOnline(fuserid, result.isGiver());
        } else if (result.isAnyAvailable()) {
          askUserToChooseRole(fuserid);
        } else {
          if (!inSinglePlayer) {
            System.out.println("me : " + fuserid + " doing single player : " + inSinglePlayer);
            inSinglePlayer = true;
            langTest.setTabooFactory(fuserid, false, true);
          }
          pollForPartner();
        }
      }
    });
  }

  private String lastSelection = "";
  /**
   * Keep checking that the partner is still active, and if the giver, check if selection state has changed.
   * @param fuserid
   * @param isGiver
   */
  private void pollForPartnerOnline(final long fuserid, final boolean isGiver) {
    onlineTimer = new Timer() {
      @Override
      public void run() {
/*        System.out.println("pollForPartnerOnline : checking if " +
          (isGiver ? " receiver partner " : " giver partner ") +
          " of me, " + fuserid + ", is online...");*/
        service.isPartnerOnline(fuserid, isGiver, new AsyncCallback<PartnerState>() {
          @Override
          public void onFailure(Throwable caught) {
            Window.alert("Couldn't contact server.");
          }

          @Override
          public void onSuccess(PartnerState partnerState) {
            if (partnerState.getOnline()) {
              pollForPartnerOnline(fuserid, isGiver);
              if (isGiver) {
                Map<String,Collection<String>> typeToSelection = partnerState.getTypeToSelection();

                if (!lastSelection.equals(typeToSelection.toString())) {
                  lastSelection = typeToSelection.toString();
                  System.out.println("pollForPartnerOnline : checked if" +
                    (isGiver ? " receiver partner " : " giver partner ") +
                    " of me, " + fuserid + ", is online and got state " + typeToSelection);
                }
                langTest.setSelectionState(typeToSelection);  // TODO user controller...
              }
              else {
/*                System.out.println("pollForPartnerOnline : checked if " +
                  (isGiver ? " receiver partner " : " giver partner ") +
                  " of me, " + fuserid + ", is online.");*/
              }
              controller.setGame(partnerState.getGameInfo());
            } else {
              onlineTimer.cancel();
              if (userManager.isActive()) {
                showPartnerSignedOut("Partner Signed Out", "Your partner signed out, will check for another...", fuserid);
              }
            }
          }
        });

      }
    };
    onlineTimer.schedule(INACTIVE_PERIOD_MILLIS2);
  }

  private void showPartnerSignedOut(String title, String message, final long fuserid) {
    new ModalInfoDialog(title,message,new HiddenHandler() {
      @Override
      public void onHidden(HiddenEvent hiddenEvent) {
        checkForPartner(fuserid);
      }
    });
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
   * TODO : use ModalInfoDialog
   * @see #checkForPartner(long)
   * @param userID
   * @param title
   * @param message
   * @param message2
   * @param isGiver
   */
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
        inSinglePlayer = false;
      }
    });
    modal.add(begin);

    modal.show();
  }

  private void cancelTimer() {
    if (userTimer != null) userTimer.cancel();
  }

  /**
   * After choosing role, check to see if
   * @param userID
   */
  private void askUserToChooseRole(final long userID) {
    final Modal askGiverReceiver = new Modal(true);
    askGiverReceiver.setCloseVisible(false);

    askGiverReceiver.setTitle("Choose role");
    Heading w = new Heading(4);
    w.setText("Would you like to give items or receive them?");
    askGiverReceiver.add(w);
    final ControlGroup recordingStyle = new ControlGroup();
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
          Window.alert("Please choose to give or receive.");  // TODO better highlighting
        } else {
          askGiverReceiver.hide();

          service.registerPair(userID, isGiver, new AsyncCallback<Void>() {
            @Override
            public void onFailure(Throwable caught) { Window.alert("Couldn't contact server."); }

            @Override
            public void onSuccess(Void result) {
              langTest.setTabooFactory(userID, isGiver, false);
              inSinglePlayer = false;

              System.out.println("role registered for " +userID + " checking for partner...");
              checkForPartner(userID);
            }
          });
        }
      }
    });
    askGiverReceiver.add(begin);
    askGiverReceiver.show();
  }
}
