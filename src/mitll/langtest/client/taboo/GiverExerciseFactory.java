package mitll.langtest.client.taboo;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.Column;
import com.github.gwtbootstrap.client.ui.ControlGroup;
import com.github.gwtbootstrap.client.ui.Controls;
import com.github.gwtbootstrap.client.ui.FluidContainer;
import com.github.gwtbootstrap.client.ui.Heading;
import com.github.gwtbootstrap.client.ui.RadioButton;
import com.github.gwtbootstrap.client.ui.Row;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.DecoratedPopupPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.PopupPanel;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.bootstrap.BootstrapExercisePanel;
import mitll.langtest.client.exercise.BusyPanel;
import mitll.langtest.client.sound.SoundFeedback;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.ExercisePanelFactory;
import mitll.langtest.client.exercise.NavigationHelper;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.shared.Exercise;
import mitll.langtest.shared.taboo.Game;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Created with IntelliJ IDEA.
 * <p/>
 * TODO : add receiver factory -- user does a free form input of the guess...
 * what if they get part of the word?  feedback when they get it correct.
 * <p/>
 * User: GO22670
 * Date: 7/9/12
 * Time: 6:18 PM
 * To change this template use File | Settings | File Templates.
 */
public class GiverExerciseFactory extends ExercisePanelFactory {
  private static final int CHECK_FOR_CORRECT_POLL_PERIOD = 1000;

  /**
   * @param service
   * @param userFeedback
   * @param controller
   * @see mitll.langtest.client.LangTest#setTabooFactory(long, boolean, boolean)
   */
  public GiverExerciseFactory(final LangTestDatabaseAsync service, final UserFeedback userFeedback,
                              final ExerciseController controller) {
    super(service, userFeedback, controller);
  }

  public Panel getExercisePanel(final Exercise e) {
    System.out.println("\nGiverExerciseFactory.getExercisePanel getting panel ...");
    controller.pingAliveUser();

    return new GiverPanel(e);
  }

  private class GiverPanel extends FluidContainer implements BusyPanel {
    private List<String> sentItems = new ArrayList<String>();
    private Map<RadioButton, String> choiceToExample;
    private Controls choice = new Controls();
    private final Button send = new Button("Send");
    private Heading pleaseWait = new Heading(4, "Please wait for receiver to answer...");
       List<String> synonymSentences;

    public GiverPanel(final Exercise exercise) {
      if (exercise == null) {
        System.err.println("huh? exercise is null?");
        return;
      }

      HTML warnNoFlash = new HTML(BootstrapExercisePanel.WARN_NO_FLASH);
      warnNoFlash.setVisible(false);
      final SoundFeedback soundFeedback = new SoundFeedback(controller.getSoundManager(), warnNoFlash);

      Row w4 = new Row();
      w4.add(new Heading(3, "User is trying to guess: "));
      add(w4);

      Row w = new Row();

      final String refSentence = controller.getProps().doTabooEnglish() ? exercise.getEnglishSentence().trim() : exercise.getRefSentence().trim();
      w.add(new Heading(2, refSentence));
      add(w);

      Row w2 = new Row();
      w2.add(new Column(12));
      add(w2);

      Row w3 = new Row();
      w3.add(new Heading(4, "Choose a hint sentence to send : "));
      add(w3);
      synonymSentences = Game.randomSample2(exercise.getSynonymSentences(), ReceiverExerciseFactory.MAX_CLUES_TO_GIVE, rnd);

      // recordingStyle.add(new ControlLabel("<b>Audio Recording Style</b>"));
      choiceToExample = populateChoices(synonymSentences, refSentence);
      final ControlGroup recordingStyle = new ControlGroup(); // necessary?
      recordingStyle.add(choice);
      add(choice);

      add(warnNoFlash);
      send.setType(ButtonType.PRIMARY);
      send.setEnabled(true);
     // send.setTitle("Hit Enter to send.");

      send.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          clickOnSend(exercise, refSentence, soundFeedback);
        }

      });

      add(send);
      add(pleaseWait);
      pleaseWait.setVisible(false);

      NavigationHelper w1 = new NavigationHelper(exercise, controller,
        false // means next button is always enabled
      );
      w1.addStyleName("topMargin");
      add(w1);
    }

    private void clickOnSend(Exercise exercise, String refSentence, SoundFeedback soundFeedback) {
      final String stimulus = getSelectedItem();
      if (stimulus != null) {
        send.setEnabled(false);
        pleaseWait.setVisible(true);
        controller.pingAliveUser();
        boolean lastChoiceRemaining = (synonymSentences.size() - sentItems.size()) == 1;
        sendStimulus(stimulus, refSentence, exercise, soundFeedback, lastChoiceRemaining, synonymSentences.size());
      } else {
        showPopup("Please select a sentence to send.");
      }
    }

    Random rnd = new Random();
    private String lastSentExercise = "";

    /**
     * @see GiverPanel#GiverPanel(mitll.langtest.shared.Exercise)
     * @param stimulus
     * @param refSentence
     * @param exercise
     * @param soundFeedback
     * @param lastChoiceRemaining
     * @param numClues
     */
    private void sendStimulus(final String stimulus, final String refSentence, final Exercise exercise, final SoundFeedback soundFeedback,
                              boolean lastChoiceRemaining, int numClues) {
      final String toSendWithBlankedOutItem = getObfuscated(stimulus, refSentence);
      final int user = controller.getUser();
      boolean differentExercise = lastSentExercise.length() > 0 && !lastSentExercise.equals(exercise.getID());
      lastSentExercise = exercise.getID();
      service.sendStimulus(user, exercise.getID(), toSendWithBlankedOutItem, refSentence, lastChoiceRemaining,
        differentExercise, numClues, new AsyncCallback<Integer>() {
        @Override
        public void onFailure(Throwable caught) { Window.alert("couldn't contact server."); }

        @Override
        public void onSuccess(Integer result) {
          if (result == 0) {
            System.out.println("sendStimulus.onSuccess : Giver " + user + " Sent '" + toSendWithBlankedOutItem + "' and not '" + stimulus + "'");
            checkForCorrect(user, toSendWithBlankedOutItem, exercise, refSentence, soundFeedback);
          } else {
            //showUserState("Partner Signed Out","Your partner signed out, will check for another if any available.");
          }
        }
      });
    }


    private String getSelectedItem() {
      for (RadioButton choice : choiceToExample.keySet()) {
        if (choice.getValue()) {
          return choiceToExample.get(choice);
        }
      }
      return null;
    }

    /**
     * TODO : ask server what items have been sent instead of keeping in client, since if we reload the page,
     * we loose the history.  OR we could shove it into the cache...?
     *
     * TODO : show mix of items...
     *
     * @param synonymSentences
     * @param refSentence
     * @return
     */
    private Map<RadioButton, String> populateChoices(List<String> synonymSentences, String refSentence) {
      choice.clear();
      final Map<RadioButton, String> choiceToExample = new HashMap<RadioButton, String>();
      List<String> notSentYet = getNotSentYetHints(synonymSentences, refSentence);

      if (notSentYet.size() > 7) notSentYet = notSentYet.subList(0, 7);
      for (String example : notSentYet) {
        final RadioButton give = new RadioButton("Giver", example);
        choiceToExample.put(give, example);
        choice.add(give);
      }
      return choiceToExample;
    }

    private List<String> getNotSentYetHints(List<String> synonymSentences, String refSentence) {
     // List<String> synonymSentences = e.getSynonymSentences();
 //     List<String> synonymSentences = Game.randomSample2(e.getSynonymSentences(), ReceiverExerciseFactory.MAX_CLUES_TO_GIVE, rnd);

      List<String> notSentYet = new ArrayList<String>();
     // System.out.println("getNotSentYetHints for " + synonymSentences.size());
      for (String candidate : synonymSentences) {
        if (sentItems.contains(getObfuscated(candidate, refSentence))) {
          System.out.println("---> already sent " + candidate);
        } else {
          notSentYet.add(candidate);
        }
      }
     // System.out.println("getNotSentYetHints now " + notSentYet.size());

      return notSentYet;
    }

    private String getObfuscated(String exampleToSend, String refSentence) {
      StringBuilder builder = new StringBuilder();
      for (int i = 0; i < refSentence.length(); i++) builder.append('_');
    /*  if (!exampleToSend.contains(refSentence)) {
        System.err.println("huh? '" + exampleToSend + "' doesn't contain '" + refSentence + "'");
      }*/
      return exampleToSend.replaceAll(refSentence, builder.toString());
    }

    /**
     * Keep asking server if the receiver has made a correct answer or not.
     *
     * @param userid
     * @param stimulus
     * @param current
     * @param refSentence
     */
    private void checkForCorrect(final long userid, final String stimulus, final Exercise current,
                                 final String refSentence,
                                 final SoundFeedback feedback) {
      service.checkCorrect(userid, stimulus, new AsyncCallback<Integer>() {
        @Override
        public void onFailure(Throwable caught) {
          Window.alert("couldn't contact server.");
        }

        @Override
        public void onSuccess(Integer result) {
          System.out.println("checkForCorrect got " + result);
          if (result == 0) { // incorrect
            feedback.playIncorrect();
            sentItems.add(stimulus);
            choiceToExample = populateChoices(synonymSentences, refSentence);
            if (choiceToExample.isEmpty()) {
              showPopup("They didn't guess correctly, moving to next item...", new CloseHandler<PopupPanel>() {
                @Override
                public void onClose(CloseEvent<PopupPanel> event) {
                  controller.loadNextExercise(current);
                }
              });

            } else {
              showPopup("They didn't guess correctly, please send another sentence.");

              send.setEnabled(true);
              pleaseWait.setVisible(false);
            }
          } else if (result == 1) {
            lastSentExercise = ""; // clear last sent hint -- they got it correct
            showPopup("They guessed correctly!  Moving on to next item.");
            feedback.playCorrect();

            controller.loadNextExercise(current);
            send.setEnabled(true);
            pleaseWait.setVisible(false);
          } else { // they haven't answered yet
            Timer t = new Timer() {
              @Override
              public void run() {
                checkForCorrect(userid, stimulus, current, refSentence, feedback);
              }
            };
            t.schedule(CHECK_FOR_CORRECT_POLL_PERIOD);
          }
        }
      });
    }

    private void showPopup(String html) {
      showPopup(html, null);
    }

    private void showPopup(String html, CloseHandler<PopupPanel> closeHandler) {
      final PopupPanel pleaseWait = new DecoratedPopupPanel();
      pleaseWait.setAutoHideEnabled(true);
      pleaseWait.add(new HTML(html));
      pleaseWait.center();
      if (closeHandler != null)
        pleaseWait.addCloseHandler(closeHandler);

      Timer t = new Timer() {
        @Override
        public void run() {
          pleaseWait.hide();
        }
      };
      t.schedule(3000);
    }

    @Override
    public boolean isBusy() {
      return pleaseWait.isVisible();
    }
  }
}
