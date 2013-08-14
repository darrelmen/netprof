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
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.DecoratedPopupPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.PopupPanel;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.bootstrap.BootstrapExercisePanel;
import mitll.langtest.client.bootstrap.SoundFeedback;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.ExercisePanelFactory;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.shared.Exercise;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
  /**
   * @param service
   * @param userFeedback
   * @param controller
   * @see mitll.langtest.client.LangTest#setFactory
   */
  public GiverExerciseFactory(final LangTestDatabaseAsync service, final UserFeedback userFeedback,
                              final ExerciseController controller) {
    super(service, userFeedback, controller);
  }

  public Panel getExercisePanel(final Exercise e) {
    return new GiverPanel(e);
  }

  private class GiverPanel extends FluidContainer {
    private List<String> sentItems = new ArrayList<String>();
    private Map<RadioButton, String> choiceToExample;
    private Controls choice = new Controls();
    private final Button send = new Button("Send");
    private Heading pleaseWait = new Heading(4, "Please wait for receiver to answer...");

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
      final String refSentence = exercise.getRefSentence().trim();
      w.add(new Heading(2, refSentence));
      add(w);

      Row w2 = new Row();
      w2.add(new Column(12));
      add(w2);

      Row w3 = new Row();
      w3.add(new Heading(4, "Choose a hint sentence to send : "));
      add(w3);

      // recordingStyle.add(new ControlLabel("<b>Audio Recording Style</b>"));
      choiceToExample = populateChoices(exercise, refSentence);
      final ControlGroup recordingStyle = new ControlGroup(); // necessary?
      recordingStyle.add(choice);
      add(choice);

      add(warnNoFlash);
      send.setType(ButtonType.PRIMARY);
      send.setEnabled(true);

      send.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          final String stimulus = getSelectedItem();
          if (stimulus != null) {
            send.setEnabled(false);
            pleaseWait.setVisible(true);

            sendStimulus(stimulus, refSentence, exercise, soundFeedback);
          } else {
            showPopup("Please select a sentence to send.");
          }
        }

      });

      add(send);
      add(pleaseWait);
      pleaseWait.setVisible(false);
    }

    private void sendStimulus(final String stimulus, final String refSentence, final Exercise exercise, final SoundFeedback soundFeedback) {
      final String toSendWithBlankedOutItem = getObfuscated(stimulus, refSentence);

      System.out.println("stimulus    " + stimulus);
      System.out.println("refSentence " + refSentence);
      System.out.println("index " + stimulus.indexOf(refSentence));
      System.out.println("toSendWithBlankedOutItem " + toSendWithBlankedOutItem);

      final int user = controller.getUser();
      service.sendStimulus(user, toSendWithBlankedOutItem, refSentence, new AsyncCallback<Void>() {
        @Override
        public void onFailure(Throwable caught) {
          Window.alert("couldn't contact server.");
        }

        @Override
        public void onSuccess(Void result) {
          System.out.println("sendStimulus.onSuccess : Giver " + user + " Sent '" + toSendWithBlankedOutItem + "' and not '" + stimulus + "'");
          checkForCorrect(user, toSendWithBlankedOutItem, exercise, refSentence, soundFeedback);
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
     * @param e
     * @param refSentence
     * @return
     */
    private Map<RadioButton, String> populateChoices(Exercise e, String refSentence) {
      choice.clear();
      final Map<RadioButton, String> choiceToExample = new HashMap<RadioButton, String>();
      List<String> notSentYet = getNotSentYetHints(e, refSentence);

      if (notSentYet.size() > 7) notSentYet = notSentYet.subList(0, 7);
      for (String example : notSentYet) {
        final RadioButton give = new RadioButton("Giver", example);
        choiceToExample.put(give, example);
        choice.add(give);
      }
      return choiceToExample;
    }

    private List<String> getNotSentYetHints(Exercise e, String refSentence) {
      List<String> synonymSentences = e.getSynonymSentences();

      List<String> notSentYet = new ArrayList<String>();
      System.out.println("getNotSentYetHints for " + synonymSentences.size());
      for (String candidate : synonymSentences) {
        if (sentItems.contains(getObfuscated(candidate, refSentence))) {
          System.out.println("---> already sent " + candidate);
        } else {
          notSentYet.add(candidate);
        }
      }
      System.out.println("getNotSentYetHints now " + notSentYet.size());

      return notSentYet;
    }

    private String getObfuscated(String exampleToSend, String refSentence) {
      StringBuilder builder = new StringBuilder();
      for (int i = 0; i < refSentence.length(); i++) builder.append('_');
      if (!exampleToSend.contains(refSentence)) {
        System.err.println("huh? '" + exampleToSend + "' doesn't contain '" + refSentence + "'");
      }
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
    private void checkForCorrect(final long userid, final String stimulus, final Exercise current, final String refSentence,
                                 final SoundFeedback feedback) {
      service.checkCorrect(userid, stimulus, new AsyncCallback<Integer>() {
        @Override
        public void onFailure(Throwable caught) {
          Window.alert("couldn't contact server.");
        }

        @Override
        public void onSuccess(Integer result) {
          if (result == 0) { // incorrect
            showPopup("They didn't guess correctly, please send another sentence.");
            feedback.playIncorrect();

            sentItems.add(stimulus);
            choiceToExample = populateChoices(current, refSentence);
            send.setEnabled(true);
            pleaseWait.setVisible(false);
          } else if (result == 1) {
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
            t.schedule(2000);
          }
        }
      });
    }

    private void showPopup(String html) {
      final PopupPanel pleaseWait = new DecoratedPopupPanel();
      pleaseWait.setAutoHideEnabled(true);
      pleaseWait.add(new HTML(html));
      pleaseWait.center();

      Timer t = new Timer() {
        @Override
        public void run() {
          pleaseWait.hide();
        }
      };
      t.schedule(3000);
    }
  }
}
