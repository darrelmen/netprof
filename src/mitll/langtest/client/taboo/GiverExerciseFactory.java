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
 *
 * TODO : add receiver factory -- user does a free form input of the guess...
 *   what if they get part of the word?  feedback when they get it correct.
 *
 * User: GO22670
 * Date: 7/9/12
 * Time: 6:18 PM
 * To change this template use File | Settings | File Templates.
 */
public class GiverExerciseFactory extends ExercisePanelFactory {
  /**
   * @see mitll.langtest.client.LangTest#setFactory
   * @param service
   * @param userFeedback
   * @param controller
   */
  public GiverExerciseFactory(final LangTestDatabaseAsync service, final UserFeedback userFeedback,
                              final ExerciseController controller) {
    super(service,userFeedback,controller);
  }

  List<String> sentItems = new ArrayList<String>();
  Map<RadioButton, String> choiceToExample;
  Controls choice = new Controls();

  public Panel getExercisePanel(final Exercise e) {
    FluidContainer container = new FluidContainer();

    Row w4 = new Row();
    w4.add(new Heading(3, "User is trying to guess: "));
    container.add(w4);

    Row w = new Row();
    final String refSentence = e.getRefSentence().trim();
    w.add(new Heading(2, refSentence));
    container.add(w);

    Row w2 = new Row();
    w2.add(new Column(12));
    container.add(w2);

    Row w3 = new Row();
    w3.add(new Heading(4, "Choose a hint sentence to send : "));
    container.add(w3);

    // recordingStyle.add(new ControlLabel("<b>Audio Recording Style</b>"));
    choiceToExample = populateChoices(e, refSentence);
    final ControlGroup recordingStyle = new ControlGroup(); // necessary?
    recordingStyle.add(choice);
    container.add(choice);

    final Button begin = new Button("Send");
    begin.setType(ButtonType.PRIMARY);
    begin.setEnabled(true);

    begin.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        for (RadioButton choice : choiceToExample.keySet()) {
          if (choice.getValue()) {
            final String exampleToSend = choiceToExample.get(choice);
            final String toSendWithBlankedOutItem = getObfuscated(exampleToSend, refSentence);
            final int user = controller.getUser();
            service.sendStimulus(user, toSendWithBlankedOutItem, refSentence, new AsyncCallback<Void>() {
              @Override
              public void onFailure(Throwable caught) {
                Window.alert("couldn't contact server.");
              }

              @Override
              public void onSuccess(Void result) {
                System.out.println("Giver " +user +" Sent '" + exampleToSend + "'");
                checkForCorrect(user,toSendWithBlankedOutItem,e,refSentence);
              }
            });
            return;
          }
        }


          // if giver, then need to see wordlist, select next item to give to receiver
          // after giving, poll for answer submission by receiver - correct, move on to next item
          //   incorrect, choose next stimulus

          // if receiver, wait for giver to give you an item, then choose your response
       }

    });

    container.add(begin);

    return container;
  }

  private Map<RadioButton, String> populateChoices(Exercise e, String refSentence) {
    choice.clear();
    final Map<RadioButton, String> choiceToExample = new HashMap<RadioButton, String>();
    List<String> notSentYet = getNotSentYetHints(e, refSentence);
    // TODO : only present choices not already sent
    if (notSentYet.size() > 7) notSentYet = notSentYet.subList(0,7);
    for (String example : notSentYet) {
      final RadioButton give = new RadioButton("Giver",example);
      choiceToExample.put(give,example);
      choice.add(give);
    }
    return choiceToExample;
  }

  private List<String> getNotSentYetHints(Exercise e, String refSentence) {
    List<String> synonymSentences = e.getSynonymSentences();

    List<String> notSentYet = new ArrayList<String>();
    for (String candidate:synonymSentences) {
      if (sentItems.contains(getObfuscated(candidate,refSentence))) {
        System.out.println("---> already sent " + candidate);
      }
      else {
        notSentYet.add(candidate);
      }
    }
    return notSentYet;
  }

  private String getObfuscated(String exampleToSend, String refSentence) {
    StringBuilder builder = new StringBuilder();
    for (int i = 0; i < refSentence.length(); i++) builder.append('-');
    if (!exampleToSend.contains(refSentence)) {
      System.err.println("huh? '" +exampleToSend + "' doesn't contain '" +refSentence +"'");
    }
    return exampleToSend.replaceAll(refSentence, builder.toString());
  }

  private void checkForCorrect(final long userid, final String stimulus, final Exercise current, final String refSentence) {
    service.checkCorrect(userid,stimulus, new AsyncCallback<Integer>() {
      @Override
      public void onFailure(Throwable caught) {
        Window.alert("couldn't contact server.");
      }

      @Override
      public void onSuccess(Integer result) {
        if (result == 0) { // incorrect
          showPopup("They didn't guess correctly, please send another sentence.");
          sentItems.add(stimulus);
          choiceToExample = populateChoices(current, refSentence);
        }
        else if (result == 1) {
          showPopup("They guessed correctly!  Moving on to next item.");
          controller.loadNextExercise(current);
        }
        else { // they haven't answered yet
          Timer t = new Timer() {
            @Override
            public void run() {
              checkForCorrect(userid,stimulus,current,refSentence);
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
