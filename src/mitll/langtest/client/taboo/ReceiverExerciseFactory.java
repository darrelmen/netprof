package mitll.langtest.client.taboo;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.Column;
import com.github.gwtbootstrap.client.ui.FluidContainer;
import com.github.gwtbootstrap.client.ui.Heading;
import com.github.gwtbootstrap.client.ui.Image;
import com.github.gwtbootstrap.client.ui.Row;
import com.github.gwtbootstrap.client.ui.TextBox;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.safehtml.shared.UriUtils;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.DecoratedPopupPanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.PopupPanel;
import mitll.langtest.client.LangTest;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.bootstrap.BootstrapExercisePanel;
import mitll.langtest.client.bootstrap.SoundFeedback;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.ExercisePanelFactory;
import mitll.langtest.client.exercise.NoPasteTextBox;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.shared.Exercise;
import mitll.langtest.shared.StimulusAnswerPair;

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
public class ReceiverExerciseFactory extends ExercisePanelFactory {
  /**
   * @param service
   * @param userFeedback
   * @param controller
   * @see mitll.langtest.client.LangTest#setFactory
   */
  public ReceiverExerciseFactory(final LangTestDatabaseAsync service, final UserFeedback userFeedback,
                                 final ExerciseController controller) {
    super(service, userFeedback, controller);
  }

  /**
   * @see TabooExerciseList#useExercise(mitll.langtest.shared.Exercise, mitll.langtest.shared.ExerciseShell)
   * @param e
   * @return
   */
  public Panel getExercisePanel(Exercise e) {
    System.out.println("getExercisePanel getting receiver panel ...");
    return new ReceiverPanel(service,userFeedback,controller);
  }

  private static class ReceiverPanel extends FluidContainer {
    private static final String PLEASE_WAIT = "Please wait for giver to send next sentence.";
    Heading prompt = new Heading(3, PLEASE_WAIT);
    TextBox guessBox = new NoPasteTextBox();
    Heading stimulus;
    String answer;
    final Button begin = new Button("Send Answer");
    //Image correct = BootstrapExercisePanel.incorrectImage;
    Image correctImage = new Image(UriUtils.fromSafeConstant(LangTest.LANGTEST_IMAGES + "checkmark48.png"));
    Image incorrectImage = new Image(UriUtils.fromSafeConstant(LangTest.LANGTEST_IMAGES + "redx48.png"));

    public ReceiverPanel(final LangTestDatabaseAsync service, final UserFeedback userFeedback,
                         final ExerciseController controller) {
      final ReceiverPanel outer = addWidgets(service,userFeedback,controller);
      checkForStimlus(service, controller, outer);

    }

    private ReceiverPanel addWidgets(final LangTestDatabaseAsync service, final UserFeedback userFeedback,
                                     final ExerciseController controller) {
      final ReceiverPanel outer = this;
      Row w4 = new Row();
      w4.add(prompt);
      add(w4);

      Row w2 = new Row();
      w2.add(new Column(12));
      add(w2);

      Row w3 = new Row();
      stimulus = new Heading(4);
      w3.add(stimulus);
      add(w3);


      Row w = new Row();
      w.add(guessBox);
      add(w);

      begin.setType(ButtonType.PRIMARY);
      begin.setEnabled(true);


      HTML warnNoFlash = new HTML(BootstrapExercisePanel.WARN_NO_FLASH);
      warnNoFlash.setVisible(false);
      final SoundFeedback soundFeedback = new SoundFeedback(controller.getSoundManager(), warnNoFlash);

      waitForNext();

      begin.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          boolean isCorrect = guessBox.getText().equalsIgnoreCase(answer);
          System.out.println("sending answer '" + guessBox.getText() + "' vs '" + answer+ "' which is " + isCorrect);
          service.registerAnswer(controller.getUser(),stimulus.getText(),answer,isCorrect,new AsyncCallback<Void>() {
            @Override
            public void onFailure(Throwable caught) {
              Window.alert("couldn't contact server");
            }

            @Override
            public void onSuccess(Void result) {
              checkForStimlus(service,controller,outer);
            }
          });

          if (isCorrect) {
            showPopup("Correct! Please wait for the next item.");
            correctImage.setVisible(true);
            controller.addAdHocExercise(answer);
            soundFeedback.playCorrect();
          }
          else {
            incorrectImage.setVisible(true);
            showPopup("Try again...");
            soundFeedback.playIncorrect();
          }
          waitForNext();
        }

      });

      FluidContainer container = new FluidContainer();
      container.add(begin);
 //     container.add(correctImage);
  //    correctImage.setVisible(false);
    //  container.add(incorrectImage);
   ///   incorrectImage.setVisible(false);

      add(container);

      add(warnNoFlash);
      return outer;
    }

    private void waitForNext() {
      prompt.setText(PLEASE_WAIT);
      stimulus.setVisible(false);
      stimulus.setText("");

      guessBox.setVisible(false);
      guessBox.setText("");
   //   correctImage.setVisible(false);
  //    incorrectImage.setVisible(false);
      begin.setVisible(false);
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
      t.schedule(2000);
    }


    private void checkForStimlus(final LangTestDatabaseAsync service, final ExerciseController controller, final ReceiverPanel outer) {
      service.checkForStimulus(controller.getUser(), new AsyncCallback<StimulusAnswerPair>() {
        @Override
        public void onFailure(Throwable caught) {
          Window.alert("couldn't contact server.");
        }

        @Override
        public void onSuccess(StimulusAnswerPair result) {
          if (result != null) {
            prompt.setText("Please fill in the blank.");
            guessBox.setVisible(true);
            begin.setVisible(true);
            stimulus.setVisible(true);
            stimulus.setText(result.stimulus);
            outer.answer = result.answer;
            System.out.println("answer " + answer);
          }
          else {
            Timer t = new Timer() {
              @Override
              public void run() {
                checkForStimlus(service,controller,outer);
              }
            };

            // Schedule the timer to run once in 1 seconds.
            t.schedule(1000);
          }
        }
      });
    }
    //return container;
  }
}
