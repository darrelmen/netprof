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
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.ExercisePanelFactory;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.shared.Exercise;

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

  public Panel getExercisePanel(Exercise e) {
    FluidContainer container = new FluidContainer();

    Row w4 = new Row();
    w4.add(new Heading(3, "User is trying to guess: "));
    container.add(w4);

    Row w = new Row();
    w.add(new Heading(2,e.getRefSentence()));
    container.add(w);

    Row w2 = new Row();
    w2.add(new Column(12));
    container.add(w2);

    Row w3 = new Row();
    w3.add(new Heading(4, "Choose a hint sentence to send : "));
    container.add(w3);
    final ControlGroup recordingStyle = new ControlGroup();

    // recordingStyle.add(new ControlLabel("<b>Audio Recording Style</b>"));
    Controls controls = new Controls();
    final Map<RadioButton, String> choiceToExample = new HashMap<RadioButton, String>();
    List<String> synonymSentences = e.getSynonymSentences();

    // TODO : only present choices not already sent
    if (synonymSentences.size() > 7) synonymSentences = synonymSentences.subList(0,7);
    for (String example : synonymSentences) {
      final RadioButton give = new RadioButton("Giver",example);
      choiceToExample.put(give,example);
      controls.add(give);
    }
    recordingStyle.add(controls);
    container.add(controls);

    final Button begin = new Button("Send");
    begin.setType(ButtonType.PRIMARY);
    begin.setEnabled(true);

    begin.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        for (RadioButton choice : choiceToExample.keySet()) {
          if (choice.getValue()) {
            String exampleToSend = choiceToExample.get(choice);
            Window.alert("sending " + exampleToSend);
            // TODO : fill in service call to send sentence to receiver
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
}
