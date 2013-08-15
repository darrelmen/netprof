package mitll.langtest.client.taboo;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.Column;
import com.github.gwtbootstrap.client.ui.FluidContainer;
import com.github.gwtbootstrap.client.ui.Heading;
import com.github.gwtbootstrap.client.ui.Image;
import com.github.gwtbootstrap.client.ui.Row;
import com.github.gwtbootstrap.client.ui.TextBox;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.safehtml.shared.UriUtils;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.DecoratedPopupPanel;
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
  SinglePlayerRobot singlePlayerRobot;

  /**
   *
   * @param service
   * @param userFeedback
   * @param controller
   * @param singlePlayerRobot
   * @see mitll.langtest.client.LangTest#setFactory
   */
  public ReceiverExerciseFactory(final LangTestDatabaseAsync service, final UserFeedback userFeedback,
                                 final ExerciseController controller, SinglePlayerRobot singlePlayerRobot) {
    super(service, userFeedback, controller);
    if (singlePlayerRobot != null) {
      singlePlayerRobot.doSinglePlayer();
    }
    this.singlePlayerRobot = singlePlayerRobot;
  }

  /**
   * @see TabooExerciseList#useExercise(mitll.langtest.shared.Exercise, mitll.langtest.shared.ExerciseShell)
   * @param e
   * @return
   */
  public Panel getExercisePanel(Exercise e) {
    System.out.println("getExercisePanel getting receiver panel ...");
    return new ReceiverPanel(service,controller);
  }

  private class ReceiverPanel extends FluidContainer {
    private static final String PLEASE_WAIT = "Please wait for giver to send next sentence.";
    Heading prompt = new Heading(3, "Checking for game partners...");
    TextBox guessBox = new NoPasteTextBox();
    Heading stimulus;
    String answer;
    final Button send = new Button("Send Answer");
    Image correctImage   = new Image(UriUtils.fromSafeConstant(LangTest.LANGTEST_IMAGES + "checkmark48.png"));
    Image incorrectImage = new Image(UriUtils.fromSafeConstant(LangTest.LANGTEST_IMAGES + "redx48.png"));

    public ReceiverPanel(final LangTestDatabaseAsync service, final ExerciseController controller) {
      final ReceiverPanel outer = addWidgets(service,controller);
      checkForStimulus(service, controller, outer);
    }

    private ReceiverPanel addWidgets(final LangTestDatabaseAsync service, final ExerciseController controller) {
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

      send.setType(ButtonType.PRIMARY);
      send.setEnabled(true);
      send.setTitle("Press the enter key.");

      HTML warnNoFlash = new HTML(BootstrapExercisePanel.WARN_NO_FLASH);
      warnNoFlash.setVisible(false);
      final SoundFeedback soundFeedback = new SoundFeedback(controller.getSoundManager(), warnNoFlash);

      waitForNext();

      send.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          sendAnswer(service, controller, outer, soundFeedback);
        }

      });

      FluidContainer container = new FluidContainer();
      container.add(send);
 //     container.add(correctImage);
  //    correctImage.setVisible(false);
    //  container.add(incorrectImage);
   ///   incorrectImage.setVisible(false);

      add(container);

      add(warnNoFlash);
      return outer;
    }

    private void sendAnswer(final LangTestDatabaseAsync service, final ExerciseController controller, final ReceiverPanel outer, SoundFeedback soundFeedback) {
      boolean isCorrect = guessBox.getText().equalsIgnoreCase(answer);
      System.out.println("sending answer '" + guessBox.getText() + "' vs '" + answer+ "' which is " + isCorrect);
      registerAnswer(service, controller, outer, isCorrect);

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

    private void registerAnswer(final LangTestDatabaseAsync service, final ExerciseController controller, final ReceiverPanel outer, boolean correct) {
      if (singlePlayerRobot != null) {
        singlePlayerRobot.registerAnswer(correct);
      }

      service.registerAnswer(controller.getUser(), stimulus.getText(), answer, correct, new RegisterAnswerResponseCallback(service, controller, outer));
    }

    private void waitForNext() {
      prompt.setText(PLEASE_WAIT);
      stimulus.setVisible(false);
      stimulus.setText("");

      guessBox.setVisible(false);
      guessBox.setText("");
   //   correctImage.setVisible(false);
  //    incorrectImage.setVisible(false);
      send.setVisible(false);
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

    /**
     * @see ReceiverPanel#ReceiverPanel(mitll.langtest.client.LangTestDatabaseAsync, mitll.langtest.client.exercise.ExerciseController)
     * @param service
     * @param controller
     * @param outer
     */
    private void checkForStimulus(final LangTestDatabaseAsync service, final ExerciseController controller, final ReceiverPanel outer) {
      if (singlePlayerRobot != null) {
        System.out.println("we have a single player robot...");
        singlePlayerRobot.checkForStimulus(controller.getUser(), new StimulusAnswerPairAsyncCallback(outer, service, controller));
      } else {
        service.checkForStimulus(controller.getUser(), new StimulusAnswerPairAsyncCallback(outer, service, controller));
      }
    }

    private void gotStimlusResponse(StimulusAnswerPair result, final ReceiverPanel outer, final LangTestDatabaseAsync service, final ExerciseController controller) {
      if (result != null) {
        System.out.println("checkForStimulus.onSuccess : got  " + result);

        showStimlus(result, outer);
        //  System.out.println("checkForStimulus : answer '" + answer + "'");
      } else {
        Timer t = new Timer() {
          @Override
          public void run() {
            checkForStimulus(service, controller, outer);
          }
        };

        // Schedule the timer to run once in 1 seconds.
        t.schedule(1000);
      }
    }

    private void showStimlus(StimulusAnswerPair result, ReceiverPanel outer) {
      prompt.setText("Please fill in the blank.");
      guessBox.setVisible(true);
      send.setVisible(true);
      stimulus.setVisible(true);
      stimulus.setText(result.stimulus);
      outer.answer = result.answer;
    }

    private HandlerRegistration keyHandler;

    private void addKeyHandler() {
      keyHandler = Event.addNativePreviewHandler(new
                                                   Event.NativePreviewHandler() {

                                                     @Override
                                                     public void onPreviewNativeEvent(Event.NativePreviewEvent event) {
                                                       NativeEvent ne = event.getNativeEvent();
                                                       int keyCode = ne.getKeyCode();

                                                       boolean isEnter = keyCode == KeyCodes.KEY_ENTER;

                                                       //   System.out.println("key code is " +keyCode);
                                                       if (isEnter && event.getTypeInt() == 512 &&
                                                         "[object KeyboardEvent]".equals(ne.getString())) {
                                                         ne.preventDefault();
                                                         send.fireEvent(new ButtonClickEvent ());
                                                       }
                                                     }
                                                   });
      // System.out.println("addKeyHandler made click handler " + keyHandler);
    }

    private class ButtonClickEvent extends ClickEvent{
        /*To call click() function for Programmatic equivalent of the user clicking the button.*/
    }

    @Override
    protected void onLoad() {
      super.onLoad();
      addKeyHandler();
    }

    @Override
    protected void onUnload() {
      super.onUnload();
      removeKeyHandler();
    }

    public void removeKeyHandler() {
      System.out.println("removeKeyHandler : " + keyHandler);

      if (keyHandler != null) keyHandler.removeHandler();
    }

    private class StimulusAnswerPairAsyncCallback implements AsyncCallback<StimulusAnswerPair> {
      private final ReceiverPanel outer;
      private final LangTestDatabaseAsync service;
      private final ExerciseController controller;

      public StimulusAnswerPairAsyncCallback(ReceiverPanel outer, LangTestDatabaseAsync service, ExerciseController controller) {
        this.outer = outer;
        this.service = service;
        this.controller = controller;
      }

      @Override
      public void onFailure(Throwable caught) {
        Window.alert("couldn't contact server.");
      }

      @Override
      public void onSuccess(StimulusAnswerPair result) {
        gotStimlusResponse(result, outer, service, controller);
      }
    }

    private class RegisterAnswerResponseCallback implements AsyncCallback<Void> {
      private final LangTestDatabaseAsync service;
      private final ExerciseController controller;
      private final ReceiverPanel outer;

      public RegisterAnswerResponseCallback(LangTestDatabaseAsync service, ExerciseController controller, ReceiverPanel outer) {
        this.service = service;
        this.controller = controller;
        this.outer = outer;
      }

      @Override
      public void onFailure(Throwable caught) {
        Window.alert("couldn't contact server");
      }

      @Override
      public void onSuccess(Void result) {
        checkForStimulus(service, controller, outer);
      }
    }

  }
}
