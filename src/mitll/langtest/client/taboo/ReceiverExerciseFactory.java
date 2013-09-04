package mitll.langtest.client.taboo;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.Column;
import com.github.gwtbootstrap.client.ui.FluidContainer;
import com.github.gwtbootstrap.client.ui.FluidRow;
import com.github.gwtbootstrap.client.ui.Heading;
import com.github.gwtbootstrap.client.ui.Image;
import com.github.gwtbootstrap.client.ui.Row;
import com.github.gwtbootstrap.client.ui.TextBox;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.github.gwtbootstrap.client.ui.event.HiddenEvent;
import com.github.gwtbootstrap.client.ui.event.HiddenHandler;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.event.shared.HandlerRegistration;
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
import mitll.langtest.client.bootstrap.LeaderboardPlot;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.ExercisePanelFactory;
import mitll.langtest.client.exercise.NoPasteTextBox;
import mitll.langtest.client.sound.SoundFeedback;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.shared.Exercise;
import mitll.langtest.shared.ExerciseShell;
import mitll.langtest.shared.flashcard.Leaderboard;
import mitll.langtest.shared.taboo.GameInfo;
import mitll.langtest.shared.taboo.StimulusAnswerPair;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * <p/>
 * <p/>
 * User: GO22670
 * Date: 7/9/12
 * Time: 6:18 PM
 * To change this template use File | Settings | File Templates.
 */
public class ReceiverExerciseFactory extends ExercisePanelFactory {
  private static final int POPUP_DURATION = 2000;
  private static final int CHECK_FOR_STIMULUS_INTERVAL = 1000;
  public static final int MAX_CLUES_TO_GIVE = 5;

  private SinglePlayerRobot singlePlayerRobot;
  private int exerciseCount = 0;
  private int stimulusCount;
  private int correctCount, incorrectCount;
  private int score;
  private int totalClues;
  boolean debugKeyHandler = false;

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

  /**
   * @see TabooExerciseList#useExercise(mitll.langtest.shared.Exercise, mitll.langtest.shared.ExerciseShell)
   * @param totallyIgnored
   * @return
   */
  public Panel getExercisePanel(Exercise totallyIgnored) {
    System.out.println("ReceiverExerciseFactory.getExercisePanel getting receiver panel ...");
    controller.pingAliveUser();
    return new ReceiverPanel(service,controller);
  }

  Map<String, Collection<String>> selectionState;
  /**
   * @see TabooExerciseList#rememberExercises(java.util.List)
   * @param exerciseShells
   */
  public void setExerciseShells(List<ExerciseShell> exerciseShells, Map<String, Collection<String>> selectionState) {
    this.selectionState = selectionState;
    System.out.println("ReceiverExerciseFactory.setExerciseShells on " + exerciseShells.size());

    if (singlePlayerRobot != null) {
      System.out.println("ReceiverExerciseFactory.setExerciseShells single player robot.");

      singlePlayerRobot.setExerciseShells(exerciseShells);
      startGame(singlePlayerRobot.startGame());
    }
    else {
      startGame();
    }
  }

  private void startGame() {
    System.out.println("ReceiverExerciseFactory.getExercisePanel startGame");

    service.startGame(controller.getUser(), true, new AsyncCallback<GameInfo>() {
      @Override
      public void onFailure(Throwable caught) {
        Window.alert("ReceiverExerciseFactory.getExercisePanel startGame : can't contact server.");
      }

      @Override
      public void onSuccess(GameInfo result) {
        startGame(result);
      }
    });
  }

  private GameInfo gameInfo;
  private void startGame(GameInfo gameInfo) {
    this.gameInfo = gameInfo;
    exerciseCount = 0;
    stimulusCount = 0;
    score = 0;
    totalClues = 0;
  }

  private class ReceiverPanel extends FluidContainer {
    private static final String PLEASE_WAIT = "Please wait for giver to send next sentence.";
    // Heading gameIndicator = new Heading(3);
    private Heading exerciseDisplay = new Heading(3);
    private Heading prompt = new Heading(4);
    private TextBox guessBox = new NoPasteTextBox();
    private Heading stimulus = new Heading(3);
    private String answer;
    private String exerciseID;
    private Button send;

    private boolean onLastStim = false;
    private StimulusAnswerPair displayedStimulus;

    // Image correctImage   = new Image(UriUtils.fromSafeConstant(LangTest.LANGTEST_IMAGES + "checkmark48.png"));
    //  Image incorrectImage = new Image(UriUtils.fromSafeConstant(LangTest.LANGTEST_IMAGES + "redx48.png"));
    private Heading correct = new Heading(4);
    private boolean isGameOver;

    /**
     * @see ReceiverExerciseFactory#getExercisePanel(mitll.langtest.shared.Exercise)
     * @param service
     * @param controller
     */
    public ReceiverPanel(final LangTestDatabaseAsync service, final ExerciseController controller) {
      final ReceiverPanel outer = addWidgets(service,controller);
      System.out.println("-----> ReceiverPanel: making the panel... check for stimulus");
      checkForStimulus(service, controller, outer);
    }

    private ReceiverPanel addWidgets(final LangTestDatabaseAsync service, final ExerciseController controller) {
      final ReceiverPanel outer = this;
      Row w5 = new Row();
      w5.add(exerciseDisplay);
      add(w5);

      Row w2 = new Row();
      w2.add(new Column(12));
      add(w2);

      Row w3 = new Row();
      w3.add(stimulus);
      add(w3);

      Row w42 = new Row();
      w42.add(prompt);
      add(w42);

      Row w = new Row();
      w.add(guessBox);
      add(w);

      Button send = new Button("Send Answer");
      send.setType(ButtonType.PRIMARY);
      send.setEnabled(true);
      send.setTitle("Press the enter key.");

      HTML warnNoFlash = new HTML(BootstrapExercisePanel.WARN_NO_FLASH);
      warnNoFlash.setVisible(false);
      final SoundFeedback soundFeedback = new SoundFeedback(controller.getSoundManager(), warnNoFlash);

      send.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          sendAnswerOnClick(controller, service, outer, soundFeedback);
        }

      });

      FluidContainer container = new FluidContainer();
      FluidRow row = new FluidRow();
      container.add(row);
      row.add(send);
     // row.add(correctImage);
    //  correctImage.setVisible(true);
    //  row.add(incorrectImage);
    //  incorrectImage.setVisible(false);

      add(container);
      add(warnNoFlash);
      add(addCorrectIncorrectFeedback());

      this.send = send;
      waitForNext();
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
      return correctAndImageRow;
    }

    public void setCorrect() {
      correct.setText(correctCount + "/" + (correctCount + incorrectCount));
    }

    private void sendAnswer(final LangTestDatabaseAsync service, final ExerciseController controller,
                            final ReceiverPanel outer, SoundFeedback soundFeedback) {
      boolean isCorrect = checkCorrect();
      System.out.println("sending answer '" + guessBox.getText() + "' vs '" + answer+ "' which is " + isCorrect + " stim count " + stimulusCount + " on last " + onLastStim);
      boolean onLast = onLastStim || stimulusCount == MAX_CLUES_TO_GIVE;   // never do more than 5 clues

      registerAnswer(service, controller, outer, isCorrect, isCorrect || onLast);

      if (isCorrect) {
       // correctImage.setVisible(true);
        controller.addAdHocExercise(answer);
        soundFeedback.playCorrect();

        int i = displayedStimulus.getNumClues() - stimulusCount + 1;
        System.out.println("sendAnswer : adding " + i + " to " + score + " clues " + displayedStimulus.getNumClues() + " stim " + stimulusCount);
        score += i;
        totalClues += displayedStimulus.getNumClues();
        System.out.println("sendAnswer : score " + score + " total clues " +totalClues);

        correctCount++;
        exerciseCount++;
        stimulusCount = 0;
        setCorrect();
        showPopup("Correct!" + (singlePlayerRobot == null ? " Please wait for the next item." : ""));
        maybeGoToNextItem(service, controller, outer);
      }
      else {
       // incorrectImage.setVisible(true);
        soundFeedback.playIncorrect();
        if (onLast) {
         // showPopup("Clues exhausted, moving to next item.", 3000);
          incorrectCount++;
          exerciseCount++;
          stimulusCount = 0;
          totalClues += displayedStimulus.getNumClues();
          setCorrect();
          maybeGoToNextItem(service, controller, outer);
        }
        else {
          showPopup("Try again...");
        }
      }
      waitForNext();
    }

    private void maybeGoToNextItem(LangTestDatabaseAsync service, ExerciseController controller, ReceiverPanel outer) {
      if (isGameOver) {
        // game over... dude...
        dealWithGameOver(service, controller, outer);
      } else {
        loadNext(controller);
      }
    }

    private void loadNext(ExerciseController controller) {
     // System.out.println("ReceiverExerciseFactory.loadNext '" + exerciseID+ "'  ");

      controller.loadExercise(gameInfo.getFirst());
     // controller.loadNextExercise(exerciseID);
    }

    private boolean checkCorrect() {
      String guess = guessBox.getText();
      boolean isCorrect = guess.equalsIgnoreCase(answer);
      if (!isCorrect) {
        for (String prefix : Arrays.asList("to ", "the ")) {  // TODO : hack for ENGLISH
          if (answer.startsWith(prefix)) { // verbs
            String truncated = answer.substring(prefix.length());
            isCorrect = guess.equalsIgnoreCase(truncated);

            if (isCorrect) break;
          } else if (guess.startsWith(prefix)) { // verbs
            String truncated = guess.substring(prefix.length());
            isCorrect = answer.equalsIgnoreCase(truncated);

            if (isCorrect) break;
          }
        }
      }
      return isCorrect;
    }

    private void registerAnswer(final LangTestDatabaseAsync service, final ExerciseController controller,
                                final ReceiverPanel outer, boolean correct, boolean movingOnToNext) {
      if (singlePlayerRobot != null) {
        singlePlayerRobot.registerAnswer(correct);
      }

      service.registerAnswer(controller.getUser(), exerciseID, stimulus.getText(), guessBox.getText(), correct,
        new RegisterAnswerResponseCallback(service, controller, outer, movingOnToNext));
    }

    private void waitForNext() {
      prompt.setText(singlePlayerRobot == null ? PLEASE_WAIT : "");
      exerciseDisplay.setText("");
      stimulus.setText("");

      guessBox.setVisible(false);
      guessBox.setText("");
      send.setVisible(false);
    }

    private void showPopup(String html) {
      showPopup(html, POPUP_DURATION, null);
    }

    private void showPopup(String html, int dur, CloseHandler<PopupPanel> closeHandler) {
      final PopupPanel pleaseWait = new DecoratedPopupPanel();
      pleaseWait.setAutoHideEnabled(true);
      pleaseWait.add(new HTML(html));
      pleaseWait.center();
      if (closeHandler != null) {
        pleaseWait.addCloseHandler(closeHandler);
      }

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
     * @see #gotStimulusResponse(mitll.langtest.shared.taboo.StimulusAnswerPair, mitll.langtest.client.taboo.ReceiverExerciseFactory.ReceiverPanel, mitll.langtest.client.LangTestDatabaseAsync, mitll.langtest.client.exercise.ExerciseController)
     *
     * @param service
     * @param controller
     * @param outer
     */
    private void checkForStimulus(final LangTestDatabaseAsync service, final ExerciseController controller, final ReceiverPanel outer) {
      //System.out.println(new Date() + " : checkForStimulus : user " + controller.getUser() + " ----------------");
     // new Exception().printStackTrace();
      if (singlePlayerRobot != null) {
      //  System.out.println("ReceiverExerciseFactory.checkForStimulus : we have a single player robot...");
        singlePlayerRobot.checkForStimulus(new AsyncCallback<StimulusAnswerPair>() {
          @Override
          public void onFailure(Throwable caught) {
            Window.alert("checkForStimulus : Couldn't contact server. Check network connection.");
          }

          @Override
          public void onSuccess(StimulusAnswerPair result) {
            System.out.println(new Date() + " gotStimulusResponse : showStimulus  " + result);
            showStimulus(result, outer);
          }
        });
      } else {
        service.checkForStimulus(controller.getUser(), new StimulusAnswerPairAsyncCallback(outer, service, controller));
      }
    }

    /**
     * @see #sendAnswer(mitll.langtest.client.LangTestDatabaseAsync, mitll.langtest.client.exercise.ExerciseController, mitll.langtest.client.taboo.ReceiverExerciseFactory.ReceiverPanel, mitll.langtest.client.sound.SoundFeedback)
     * @param service
     * @param controller
     * @param outer
     */
    private void dealWithGameOver(final LangTestDatabaseAsync service, final ExerciseController controller, final ReceiverPanel outer) {
      new ModalInfoDialog("Game complete!", "Game complete! Your score was " + score + " out of " + totalClues,
        new HiddenHandler() {
          @Override
          public void onHidden(HiddenEvent hiddenEvent) {
            System.out.println(new Date() + " ReceiverExerciseFactory.dealWithGameOver..");
            service.postGameScore(controller.getUser(), score, totalClues, new AsyncCallback<Void>() {
              @Override
              public void onFailure(Throwable caught) {
                //To change body of implemented methods use File | Settings | File Templates.
              }

              @Override
              public void onSuccess(Void result) {
                if (!gameInfo.anyGamesRemaining()) {//controller.isLastExercise(exerciseID)) {
                  showLeaderboard(service, controller);
                } else {
                  loadNext(controller);

                  if (singlePlayerRobot != null) {
                    startGame(singlePlayerRobot.startGame());
                  } else {
                    service.startGame(controller.getUser(), false, new AsyncCallback<GameInfo>() {
                      @Override
                      public void onFailure(Throwable caught) {
                        //To change body of implemented methods use File | Settings | File Templates.
                      }

                      @Override
                      public void onSuccess(GameInfo result) {
                        startGame(result);
                        checkForStimulus(service, controller, outer);
                      }
                    });
                  }
                }
              }
            });
          }
        });
    }

    private void showLeaderboard(LangTestDatabaseAsync service, final ExerciseController controller) {
      service.getLeaderboard(controller.getUser(), new AsyncCallback<Leaderboard>() {
        @Override
        public void onFailure(Throwable caught) {}

        @Override
        public void onSuccess(Leaderboard result) {
          new LeaderboardPlot().showLeaderboardPlot(result, controller.getUser(), 0, selectionState,
            "Would you like to practice this chapter(s) again?",
            new ClickHandler() {
              @Override
              public void onClick(ClickEvent event) {
                controller.startOver();
              }
            },
            new ClickHandler() {
              @Override
              public void onClick(ClickEvent event) {
                showPopup("To continue playing, choose another chapter.");
              }
            });
        }
      });
    }

    private Timer checkStimTimer = null;
    private Timer verifyStimTimer = null;

    /**
     * See if giver has sent the next stimulus yet.
     * If not, poll until it arrives.
     *
     * @param result
     * @param outer
     * @param service
     * @param controller
     * @see ReceiverPanel.StimulusAnswerPairAsyncCallback#onSuccess(mitll.langtest.shared.taboo.StimulusAnswerPair)
     */
    private void gotStimulusResponse(StimulusAnswerPair result, final ReceiverPanel outer,
                                     final LangTestDatabaseAsync service, final ExerciseController controller) {
      //System.out.println(new Date() + " gotStimulusResponse : got  " + result);

      if (result.noStimYet) {
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

          checkStimTimer.schedule(CHECK_FOR_STIMULUS_INTERVAL);
        } else {
          System.out.println("\tNot making a new timer for " + "? at " + new Date());
        }
      } else {
        System.out.println(new Date() + " ReceiverExerciseFactory.gotStimulusResponse : showStimlus  " + result);
        showStimulus(result, outer);
      }
    }

    private void pollForStimChange(final ReceiverPanel outer) {
      if (verifyStimTimer == null) {
        verifyStimTimer = new Timer() {
          @Override
          public void run() {
           // System.out.println("pollForStimChange : Fired!  Timer : " + checkStimTimer + " " + new Date() + " : checkForStimulus : " + controller.getUser());
            verifyStimTimer = null;
            service.checkForStimulus(controller.getUser(), new AsyncCallback<StimulusAnswerPair>() {
              @Override
              public void onFailure(Throwable caught) {
              }

              @Override
              public void onSuccess(StimulusAnswerPair result) {
                if (!result.equals(displayedStimulus)) {
                  if (result.noStimYet) waitForNext();
                  else showStimulus(result, outer);
                }
                else pollForStimChange(outer);
              }
            });
          }
        };
        //System.out.println("pollForStimChange : Queued: Timer : " + checkStimTimer + " " + new Date() + " : checkForStimulus user #" + controller.getUser());

        // Schedule the timer to run once in 1 seconds.
        verifyStimTimer.schedule(CHECK_FOR_STIMULUS_INTERVAL);
      }
      else {
        System.out.println("pollForStimChange : DID NOT Queue: Timer : " + checkStimTimer + " " + new Date() + " : checkForStimulus user #" + controller.getUser());
      }
    }

    private void cancelStimTimer() {
      if (checkStimTimer != null) {
        System.out.println("\tCancelling timer " + checkStimTimer);

        checkStimTimer.cancel();
      }
    }

    private void showStimulus(StimulusAnswerPair result, ReceiverPanel outer) {
     // correctImage.setVisible(false);
   //   incorrectImage.setVisible(false);
      displayedStimulus = result;
      onLastStim = result.isLastStimulus;
      if (onLastStim)
      System.out.println("\n\n\n\non last " + onLastStim);
      if (gameInfo != null) {
        showGame();
      }

      prompt.setText("Fill in the blank.");
      guessBox.setVisible(true);
      send.setVisible(true);
      stimulus.setVisible(true);
      if (result.getStimulus() != null) {
        showStim(result);
      }
      outer.answer = result.getAnswer();
      exerciseID = result.getExerciseID();
      guessBox.setFocus(true);
      if (singlePlayerRobot == null) {
        pollForStimChange(outer);
      }
      isGameOver = result.isGameOver();
    }

    private void showStim(StimulusAnswerPair result) {
      stimulus.setText("Clue #" + (++stimulusCount) + "<br/><font color=#0036a2>" + result.getStimulus() +"</font>");
    }
/*    private void showStimFull(StimulusAnswerPair result) {
      stimulus.setText("Clue " + (++stimulusCount) + " of " + displayedStimulus.getNumClues() + "<br/><font color=#0036a2>" + result.getStimulus() +"</font>");
    }*/

    private void showGame() {
      String gameInfoString = "Game #" + (gameInfo.getGameCount());// + " of " + gameInfo.getNumGames();
      //  gameIndicator.setText(gameInfo);
      exerciseDisplay.setText(gameInfoString + ", item #" + (exerciseCount + 1));// + " of " + gameInfo.getInitialNumExercises());
    }

/*
    private void showGameFull() {
      String gameInfoString = "Game " + (gameInfo.getGameCount() + 1) + " of " + gameInfo.getNumGames();
      //  gameIndicator.setText(gameInfo);
      exerciseDisplay.setText(gameInfoString + ", item " + (exerciseCount + 1) + " of " + gameInfo.getInitialNumExercises());
    }
*/

    private HandlerRegistration keyHandler;

    private void addKeyHandler() {
      keyHandler = Event.addNativePreviewHandler(new
                                                   Event.NativePreviewHandler() {

                                                     @Override
                                                     public void onPreviewNativeEvent(Event.NativePreviewEvent event) {
                                                       NativeEvent ne = event.getNativeEvent();
                                                       int keyCode = ne.getKeyCode();
                                                       boolean isEnter = keyCode == KeyCodes.KEY_ENTER;
                                                       if (isEnter && event.getTypeInt() == 512 &&
                                                         "[object KeyboardEvent]".equals(ne.getString())) {
                                                   //      System.out.println("ReceiverExerciseFactory.addKeyHandler got click target " +  ne.getEventTarget());

                                                         ne.preventDefault();
                                                         ne.stopPropagation();
                                                         userHitEnterKey();
                                                       }
                                                     }
                                                   });
      if (debugKeyHandler) System.out.println(new Date() +" ReceiverExerciseFactory.addKeyHandler made click handler " + keyHandler);
    }

    private void userHitEnterKey() {
      System.out.println(new Date() +"\tReceiverExerciseFactory.userHitEnterKey " + keyHandler);
      if (send != null && guessBox.getText().length() > 0) {
        send.fireEvent(new ButtonClickEvent());
      }
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
     // System.out.println(new Date() +" ReceiverExerciseFactory : onUnload");

      removeKeyHandler();
      cancelStimTimer();

      if (verifyStimTimer != null) {
        System.out.println("\tCancelling timer " + verifyStimTimer);

        verifyStimTimer.cancel();
      }
    }

    public void removeKeyHandler() {
      if (keyHandler == null) {
        if (debugKeyHandler) System.err.println(new Date() +" -- ReceiverExerciseFactory : removeKeyHandler : " + keyHandler);
      } else {
        if (debugKeyHandler) System.out.println(new Date() +" ReceiverExerciseFactory : removeKeyHandler : " + keyHandler);
      }
      if (keyHandler != null){
        keyHandler.removeHandler();
        keyHandler = null;
      }
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
        Window.alert("StimulusAnswerPairAsyncCallback : couldn't contact server.");
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
      private final boolean movingOn;

      public RegisterAnswerResponseCallback(LangTestDatabaseAsync service, ExerciseController controller, ReceiverPanel outer, boolean movingOn) {
        this.service = service;
        this.controller = controller;
        this.outer = outer;
        this.movingOn = movingOn;
      }

      @Override
      public void onFailure(Throwable caught) {
        Window.alert("RegisterAnswerResponseCallback : couldn't contact server");
      }

      @Override
      public void onSuccess(Void result) {
        if (!movingOn) {
          System.out.println("RegisterAnswerResponseCallback.onSuccess -- ");

          checkForStimulus(service, controller, outer);
        }
      }
    }
  }
}
