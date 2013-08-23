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
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.LangTest;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.bootstrap.BootstrapExercisePanel;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.ExercisePanelFactory;
import mitll.langtest.client.exercise.NoPasteTextBox;
import mitll.langtest.client.sound.SoundFeedback;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.shared.Exercise;
import mitll.langtest.shared.ExerciseShell;
import mitll.langtest.shared.taboo.StimulusAnswerPair;

import java.util.Collection;
import java.util.Date;

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
  public static final int POPUP_DURATION = 2000;
  public static final int CHECK_FOR_STIMULUS_INTERVAL = 2000;
  private SinglePlayerRobot singlePlayerRobot;

  /**
   *
   * @param service
   * @param userFeedback
   * @param controller
   * @param singlePlayerRobot
   * @see mitll.langtest.client.LangTest#setTabooFactory
   */
  public ReceiverExerciseFactory(final LangTestDatabaseAsync service, final UserFeedback userFeedback,
                                 final ExerciseController controller, SinglePlayerRobot singlePlayerRobot) {
    super(service, userFeedback, controller);
    this.singlePlayerRobot = singlePlayerRobot;
  }

  private int correctCount, incorrectCount;

  /**
   * @see TabooExerciseList#useExercise(mitll.langtest.shared.Exercise, mitll.langtest.shared.ExerciseShell)
   * @param totallyIgnored
   * @return
   */
  public Panel getExercisePanel(Exercise totallyIgnored) {
    System.out.println("\nReceiverExerciseFactory.getExercisePanel getting receiver panel ...");
    controller.pingAliveUser();

    return new ReceiverPanel(service,controller);
  }

 // private Set<String> exerciseList = new HashSet<String>();

  /**
   * @see TabooExerciseList#rememberExercises(java.util.List)
   * @param exerciseShells
   */
  public void setExerciseShells(Collection<ExerciseShell> exerciseShells) {
    if (singlePlayerRobot != null) {
      singlePlayerRobot.setExerciseShells(exerciseShells);
    }
   // for (ExerciseShell shell : exerciseShells) exerciseList.add(shell.getID());
  }

  private class ReceiverPanel extends FluidContainer {
    private static final String PLEASE_WAIT = "Please wait for giver to send next sentence.";
    Heading exerciseDisplay = new Heading(3);
    Heading prompt = new Heading(3/*, "Checking for game partners..."*/);
    TextBox guessBox = new NoPasteTextBox();
    Heading stimulus;
    String answer;
    String exerciseID;
    final Button send = new Button("Send Answer");
    Image correctImage   = new Image(UriUtils.fromSafeConstant(LangTest.LANGTEST_IMAGES + "checkmark48.png"));
    Image incorrectImage = new Image(UriUtils.fromSafeConstant(LangTest.LANGTEST_IMAGES + "redx48.png"));
    private Heading correct = new Heading(4);

    public ReceiverPanel(final LangTestDatabaseAsync service, final ExerciseController controller) {
      final ReceiverPanel outer = addWidgets(service,controller);
      System.out.println("-----> ReceiverPanel: making the panel...");
      checkForStimulus(service, controller, outer);
    }

    private ReceiverPanel addWidgets(final LangTestDatabaseAsync service, final ExerciseController controller) {
      final ReceiverPanel outer = this;
      Row w4 = new Row();
      w4.add(exerciseDisplay);
      add(w4);

      Row w42 = new Row();
      w42.add(prompt);
      add(w42);

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

      waitForNext(true);

      send.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          sendAnswerOnClick(controller, service, outer, soundFeedback);
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
      add(addCorrectIncorrectFeedback());
      return outer;
    }

    private void sendAnswerOnClick(ExerciseController controller, LangTestDatabaseAsync service, ReceiverPanel outer, SoundFeedback soundFeedback) {
      controller.pingAliveUser();
      sendAnswer(service, controller, outer, soundFeedback);
    }

    public Widget addCorrectIncorrectFeedback() {
      Panel correctAndImageRow = new FlowPanel();
      correctAndImageRow.addStyleName("headerBackground");
      correctAndImageRow.addStyleName("blockStyle");
      correctAndImageRow.addStyleName("topMargin");
      Panel pair = new HorizontalPanel();
      correctAndImageRow.add(pair);

      Image checkmark = new Image(LangTest.LANGTEST_IMAGES + "checkboxCorrectRatio.png");
      checkmark.addStyleName("checkboxPadding");
      pair.add(checkmark);
      correct.addStyleName("correctRatio");
      pair.add(correct);
      setCorrect();
      return correctAndImageRow;    //To change body of overridden methods use File | Settings | File Templates.
    }

    public void setCorrect() {
      correct.setText(correctCount + "/" + (correctCount + incorrectCount));
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
       // System.out.println("increment correct");

        correctCount++;
        setCorrect();
//        keepTrackOfRemainingExercises();
      }
      else {
        incorrectImage.setVisible(true);
        soundFeedback.playIncorrect();
        if (onLastStim) {
          showPopup("All clues sent, moving to next item.", 3000);
          incorrectCount++;
          //System.out.println("---> incrementing incorrect");
          setCorrect();
//          keepTrackOfRemainingExercises();
        }
        else {
          showPopup("Try again...");
         // System.out.println("\t not incrementing incorrect");
        }
      }
      waitForNext(false);
    }

/*
    private void keepTrackOfRemainingExercises() {
      if (!exerciseList.contains(exerciseID)) System.err.println("huh? can't find " + exerciseID + " in list of current exercises...");
      else exerciseList.remove(exerciseID);
    }
*/

    private void registerAnswer(final LangTestDatabaseAsync service, final ExerciseController controller, final ReceiverPanel outer, boolean correct) {
      if (singlePlayerRobot != null) {
        singlePlayerRobot.registerAnswer(correct);
      }

      service.registerAnswer(controller.getUser(), exerciseID, stimulus.getText(), guessBox.getText(), correct,
        new RegisterAnswerResponseCallback(service, controller, outer));
    }

    private void waitForNext(boolean initialCall) {
      //if (!initialCall) {
        prompt.setText(PLEASE_WAIT);
      //}
      exerciseDisplay.setText("");
      stimulus.setVisible(false);
      stimulus.setText("");

      guessBox.setVisible(false);
      guessBox.setText("");
   //   correctImage.setVisible(false);
  //    incorrectImage.setVisible(false);
      send.setVisible(false);
    }


    private void showPopup(String html) {
      showPopup(html, POPUP_DURATION);
    }

    private void showPopup(String html, int dur) {

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
      t.schedule(dur);
    }

    /**
     * @see ReceiverPanel#ReceiverPanel(mitll.langtest.client.LangTestDatabaseAsync, mitll.langtest.client.exercise.ExerciseController)
     * @param service
     * @param controller
     * @param outer
     */
    private void checkForStimulus(final LangTestDatabaseAsync service, final ExerciseController controller, final ReceiverPanel outer) {
      //System.out.println(new Date() + " : checkForStimulus : user " + controller.getUser() + "----------------");
      //cancelStimTimer();

      if (singlePlayerRobot != null) {
       // System.out.println("we have a single player robot...");
        singlePlayerRobot.checkForStimulus(controller.getUser(), new StimulusAnswerPairAsyncCallback(outer, service, controller));
      } else {
        service.checkForStimulus(controller.getUser(), new StimulusAnswerPairAsyncCallback(outer, service, controller));
      }
    }

    private Timer checkStimTimer = null;

    /**
     * See if giver has sent the next stimulus yet.
     * If not, poll until it arrives.
     *
     * @see ReceiverPanel.StimulusAnswerPairAsyncCallback#onSuccess(mitll.langtest.shared.taboo.StimulusAnswerPair)
     * @param result
     * @param outer
     * @param service
     * @param controller
     */
    private void gotStimulusResponse(StimulusAnswerPair result, final ReceiverPanel outer,
                                    final LangTestDatabaseAsync service, final ExerciseController controller) {
     // System.out.println(new Date() + " gotStimulusResponse : got  " + result);

      if (result != null && !result.noStimYet) {
        //  cancelStimTimer();
        System.out.println(new Date() + " gotStimulusResponse : showStimlus  " + result);

        showStimulus(result, outer);
        //  System.out.println("checkForStimulus : answer '" + answer + "'");
      } else if (result != null) {
        //System.out.println("---> " +new Date() + "gotStimulusResponse : got  " + result);

        if (checkStimTimer == null) {
          //System.out.println("\tMaking a new timer for " + "? at " +new Date() );

          checkStimTimer = new Timer() {
            @Override
            public void run() {
             // System.out.println("Fired!  Timer : " + checkStimTimer + " " + new Date() + " : checkForStimulus : " + controller.getUser());
              checkStimTimer = null;
              checkForStimulus(service, controller, outer);
            }
          };
          //System.out.println("Queued: Timer : " + checkStimTimer + " " + new Date() + " : checkForStimulus user #" + controller.getUser());

          // Schedule the timer to run once in 1 seconds.
          checkStimTimer.schedule(CHECK_FOR_STIMULUS_INTERVAL);
        }
        else {
          System.out.println("\tNot making a new timer for " + "? at " +new Date() );

        }
      } else { // no more stimulus left...
        removeKeyHandler();
        prompt.setText("Please choose another chapter(s).");
        new ModalInfoDialog("Word list complete", "Please choose another chapter.");
      }
    }

    private void cancelStimTimer() {
      if (checkStimTimer != null) {
        System.out.println("\tCancelling timer " + checkStimTimer);

        checkStimTimer.cancel();
      }
    }

    private boolean onLastStim = false;
    private void showStimulus(StimulusAnswerPair result, ReceiverPanel outer) {
      onLastStim = result.isLastStimulus;
      exerciseDisplay.setText("Phrase # " + result.getExerciseID());

      prompt.setText("Please fill in the blank.");
      guessBox.setVisible(true);
      send.setVisible(true);
      stimulus.setVisible(true);
      stimulus.setText(result.getStimulus());
      outer.answer = result.getAnswer();
      exerciseID = result.getExerciseID();
      guessBox.setFocus(true);
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
                                                         userHitEnterKey();
                                                       }
                                                     }
                                                   });
      // System.out.println("addKeyHandler made click handler " + keyHandler);
    }

    private void userHitEnterKey() {
      send.fireEvent(new ButtonClickEvent());
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
      cancelStimTimer();
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
        gotStimulusResponse(result, outer, service, controller);
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
/*
  private void showUserState(String title, String message) {
    new ModalInfoDialog(title, message);
  }*/
}
