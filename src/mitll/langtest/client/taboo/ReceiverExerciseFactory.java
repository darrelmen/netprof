package mitll.langtest.client.taboo;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.Column;
import com.github.gwtbootstrap.client.ui.FluidContainer;
import com.github.gwtbootstrap.client.ui.FluidRow;
import com.github.gwtbootstrap.client.ui.Heading;
import com.github.gwtbootstrap.client.ui.Image;
import com.github.gwtbootstrap.client.ui.Modal;
import com.github.gwtbootstrap.client.ui.Row;
import com.github.gwtbootstrap.client.ui.TextBox;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.github.gwtbootstrap.client.ui.event.HideEvent;
import com.github.gwtbootstrap.client.ui.event.HideHandler;
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
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Panel;
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
  private static final int CHECK_FOR_STIMULUS_INTERVAL = 1000;
  public static final int MAX_CLUES_TO_GIVE = 5;

  private SinglePlayerRobot singlePlayerRobot;
  private int exerciseCount = 0;
  private int stimulusCount;
  private int score;
  private Map<String, Collection<String>> selectionState;

  private boolean debugKeyHandler = false;

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

  /**
   * @see TabooExerciseList#rememberExercises(java.util.List)
   * @param exerciseShells
   */
  public void setExerciseShells(List<ExerciseShell> exerciseShells, Map<String, Collection<String>> selectionState) {
    this.selectionState = selectionState;
    System.out.println("ReceiverExerciseFactory.setExerciseShells on " + exerciseShells.size() + " and state "+ selectionState);

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
  }

  private class ReceiverPanel extends FluidContainer {
    private static final String PLEASE_WAIT = "Please wait for giver to send next sentence.";
    // Heading gameIndicator = new Heading(3);
    private Heading exerciseDisplay = new Heading(3);
    private Heading prompt = new Heading(4);
    private TextBox guessBox = new NoPasteTextBox();
    private Heading stimulus = new Heading(3);
    private String answer;
    private Button send,pass;
    private StimulusAnswerPair currentStimulus;

    private Image correctImage   = new Image(UriUtils.fromSafeConstant(LangTest.LANGTEST_IMAGES + "checkmark48.png"));
    private Image incorrectImage = new Image(UriUtils.fromSafeConstant(LangTest.LANGTEST_IMAGES + "redx48.png"));
    private Image arrowImage = new Image(UriUtils.fromSafeConstant(LangTest.LANGTEST_IMAGES + "rightArrow.png"));
    private Heading correct = new Heading(4);

    /**
     * @see ReceiverExerciseFactory#getExercisePanel(mitll.langtest.shared.Exercise)
     * @param service
     * @param controller
     */
    public ReceiverPanel(final LangTestDatabaseAsync service, final ExerciseController controller) {
      final ReceiverPanel outer = addWidgets(service,controller);
      System.out.println("-----> ReceiverPanel: making the panel... check for stimulus");
      stimulusCount = 0;
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
      guessBox.addStyleName("topMargin");
      addAccentButtons(controller, w);
      add(w);

      this.send = new Button("Send Answer");
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

      pass = new Button("Pass");
      pass.addStyleName("leftFiveMargin");
      pass.setType(ButtonType.INFO);
      pass.setEnabled(true);
      pass.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          guessBox.setText("");
          sendAnswer(service, controller, outer, soundFeedback, false);
        }
      });
      row.add(pass);

      add(container);
      add(warnNoFlash);
      add(addCorrectIncorrectFeedback());

      waitForNext();
      return outer;
    }

    private void addAccentButtons(ExerciseController controller, Row w) {
      if (!controller.getProps().doTabooEnglish()) {
         String [] accented = {"á", "é", "í", "ó", "ú", "ü", "ñ"};
        for (String accent : accented) {
          final Button w1 = new Button(accent);
          w1.setType(ButtonType.SUCCESS);
          w1.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
              guessBox.setText(guessBox.getText() + w1.getText().trim());
              guessBox.setFocus(true);
            }
          });
          w1.addStyleName("leftFiveMargin");
          w.add(w1);
        }
      }
    }

    private void sendAnswerOnClick(ExerciseController controller, LangTestDatabaseAsync service, ReceiverPanel outer, SoundFeedback soundFeedback) {
      controller.pingAliveUser();
      sendAnswer(service, controller, outer, soundFeedback, true);
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
      String outOf = gameInfo != null ? " out of " + gameInfo.getTotalClues() : "";
      correct.setText("Score " + score + outOf);
    }

    private void sendAnswer(final LangTestDatabaseAsync service, final ExerciseController controller,
                            final ReceiverPanel outer, SoundFeedback soundFeedback, boolean showTryAgain) {
      boolean isCorrect = checkCorrect();
      System.out.println("sending answer '" + guessBox.getText() + "' vs '" + answer+ "' which is " + isCorrect +
        " stim count " + stimulusCount + " on last " + currentStimulus.isLastStimulus());
      boolean onLast = currentStimulus.isLastStimulus() || stimulusCount == MAX_CLUES_TO_GIVE;   // never do more than 5 clues

      boolean movingOnToNext = isCorrect || onLast;

      if (isCorrect) {
        soundFeedback.playCorrect();

        int i = MAX_CLUES_TO_GIVE - stimulusCount + 1;
       // System.out.println("sendAnswer : adding " + i + " to " + score + " clues " + displayedStimulus.getNumClues() + " stim " + stimulusCount);
        score += i;
        System.out.println("sendAnswer : score " + score + " total clues " +gameInfo.getTotalClues());

        exerciseCount++;
        stimulusCount = 0;
        setCorrect();
        new PopupHelper().showPopup("Correct! +" + i + " points." + (singlePlayerRobot == null ? " Please wait for the next item." : ""), correctImage, 3500);
        maybeGoToNextItem(service, controller, outer,isCorrect,movingOnToNext);
      }
      else {
        soundFeedback.playIncorrect();
        if (onLast) {
          new PopupHelper().showPopup("Clues exhausted, moving to next item.", incorrectImage, arrowImage);
          exerciseCount++;
          stimulusCount = 0;
          setCorrect();
          maybeGoToNextItem(service, controller, outer,isCorrect,movingOnToNext);
        }
        else {
          registerAnswer(service, controller, outer, isCorrect, movingOnToNext);
          if (showTryAgain) new PopupHelper().showPopup("Try again...", incorrectImage);
        }
      }
      waitForNext();
    }

    /**
     * @see #sendAnswer(mitll.langtest.client.LangTestDatabaseAsync, mitll.langtest.client.exercise.ExerciseController, mitll.langtest.client.taboo.ReceiverExerciseFactory.ReceiverPanel, mitll.langtest.client.sound.SoundFeedback, boolean)
     * @param service
     * @param controller
     * @param outer
     */
    private void maybeGoToNextItem(LangTestDatabaseAsync service, ExerciseController controller, ReceiverPanel outer,boolean isCorrect, boolean movingOnToNext) {
      if (currentStimulus.isGameOver()) {
        // game over... dude...
        dealWithGameOver(service, controller, outer, isCorrect, movingOnToNext);
      } else {
        registerAnswer(service, controller, outer, isCorrect, movingOnToNext);

        loadNext(controller);
      }
    }

    private void loadNext(ExerciseController controller) {
     // System.out.println("ReceiverExerciseFactory.loadNext '" + exerciseID+ "'  ");

      controller.makeExercisePanel(null);
    }

    private boolean checkCorrect() {
      String guess = guessBox.getText().trim();
      guess = guess.replaceAll("\\p{Punct}$",""); // remove trailing punctuation
      String spacesRemoved = guess.replaceAll("\\s+", " ");
     /* System.out.println("guess now '" + spacesRemoved+
        "' vs '" +answer+
        "'");*/
      boolean isCorrect = spacesRemoved.equalsIgnoreCase(answer);
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

      service.registerAnswer(controller.getUser(), currentStimulus.getExerciseID(), stimulus.getText(), guessBox.getText(), correct,
        new RegisterAnswerResponseCallback(service, controller, outer, movingOnToNext));
    }

    private void waitForNext() {
      prompt.setText(singlePlayerRobot == null ? PLEASE_WAIT : "");
      exerciseDisplay.setText("");
      stimulus.setText("");

      guessBox.setVisible(false);
      guessBox.setText("");
      send.setVisible(false);
      pass.setVisible(false);
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
     * @see #sendAnswer(mitll.langtest.client.LangTestDatabaseAsync, mitll.langtest.client.exercise.ExerciseController, mitll.langtest.client.taboo.ReceiverExerciseFactory.ReceiverPanel, mitll.langtest.client.sound.SoundFeedback, boolean)
     * @param service
     * @param controller
     * @param outer
     */
    private void dealWithGameOver(final LangTestDatabaseAsync service, final ExerciseController controller,
                                  final ReceiverPanel outer, final boolean isCorrect,final boolean movingOnToNext) {
      System.out.println(new Date() + " ReceiverExerciseFactory.dealWithGameOver. ----------->\n\n");
      service.postGameScore(controller.getUser(), score, gameInfo.getTotalClues(), new AsyncCallback<Void>() {
        @Override
        public void onFailure(Throwable caught) {
          //To change body of implemented methods use File | Settings | File Templates.
        }

        @Override
        public void onSuccess(Void result) {
          registerAnswer(service, controller, outer, isCorrect, movingOnToNext);   // this way, giver will see response and then know to check for game score

          showLeaderboard(service, controller, outer);
        }
      });
    }

    private void showLeaderboard(final LangTestDatabaseAsync service, final ExerciseController controller, final ReceiverPanel outer) {
      final String gameNotice = "Game complete! Your score was " + score + " out of " + gameInfo.getTotalClues();
      if (gameInfo.anyGamesRemaining()) {
        service.getLeaderboard(selectionState, new AsyncCallback<Leaderboard>() {
          @Override
          public void onFailure(Throwable caught) {
          }

          @Override
          public void onSuccess(Leaderboard result) {
            Modal plot = new LeaderboardPlot().showLeaderboardPlot(result, controller.getUser(), 0, selectionState,
              gameNotice, 5000);
            plot.addHideHandler(new HideHandler() {
              @Override
              public void onHide(HideEvent hideEvent) {
                doNextGame(controller, service, outer);
              }
            });
          }
        });
      } else {
        showLeaderboard(service, controller, gameNotice +"<br/>Would you like to practice this chapter(s) again?",
          "To continue playing, choose another chapter.");
      }
    }

    /**
     * @see #dealWithGameOver(mitll.langtest.client.LangTestDatabaseAsync, mitll.langtest.client.exercise.ExerciseController, mitll.langtest.client.taboo.ReceiverExerciseFactory.ReceiverPanel, boolean, boolean)
     * @param controller
     * @param service
     * @param outer
     */
    private void doNextGame(final ExerciseController controller, final LangTestDatabaseAsync service, final ReceiverPanel outer) {
      if (singlePlayerRobot != null) {
        startGame(singlePlayerRobot.startGame());
        loadNext(controller);
      } else {
        service.startGame(controller.getUser(), false, new AsyncCallback<GameInfo>() {
          @Override
          public void onFailure(Throwable caught) {
            //To change body of implemented methods use File | Settings | File Templates.
          }

          @Override
          public void onSuccess(GameInfo result) {
            startGame(result);
            loadNext(controller);
            checkForStimulus(service, controller, outer);
          }
        });
      }
    }

    private void showLeaderboard(LangTestDatabaseAsync service, final ExerciseController controller, final String prompt1,
                                 final String clickNoMessage
    ) {
      ClickHandler onYes = new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          controller.startOver();
        }
      };
      ClickHandler onNo = new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          new PopupHelper().showPopup(clickNoMessage);
        }
      };
      showLeaderboard(service, controller, prompt1, onYes, onNo);
    }

    private void showLeaderboard(LangTestDatabaseAsync service, final ExerciseController controller, final String prompt1,
                                 final ClickHandler onYes, final ClickHandler onNo
    ) {
      service.getLeaderboard(selectionState, new AsyncCallback<Leaderboard>() {
        @Override
        public void onFailure(Throwable caught) {
        }

        @Override
        public void onSuccess(Leaderboard result) {
          new LeaderboardPlot().showLeaderboardPlot(result, controller.getUser(), 0, selectionState,
            prompt1,
            onYes,
            onNo,0);
        }
      });
    }

    private Timer checkStimTimer = null;

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
      if (result.isNoStimYet()) {
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

    private void cancelStimTimer() {
      if (checkStimTimer != null) {
        System.out.println("\tCancelling timer " + checkStimTimer);
        checkStimTimer.cancel();
      }
    }

    /**
     * @see #checkForStimulus(mitll.langtest.client.LangTestDatabaseAsync, mitll.langtest.client.exercise.ExerciseController, mitll.langtest.client.taboo.ReceiverExerciseFactory.ReceiverPanel)
     * @see #gotStimulusResponse(mitll.langtest.shared.taboo.StimulusAnswerPair, mitll.langtest.client.taboo.ReceiverExerciseFactory.ReceiverPanel, mitll.langtest.client.LangTestDatabaseAsync, mitll.langtest.client.exercise.ExerciseController)
     * @param result
     * @param outer
     */
    private void showStimulus(StimulusAnswerPair result, ReceiverPanel outer) {
     // correctImage.setVisible(false);
   //   incorrectImage.setVisible(false);
      //onLastStim = result.isLastStimulus();
      currentStimulus = result;
/*      if (onLastStim)
        System.out.println("\n\n\n\non last " + onLastStim);*/
      if (gameInfo != null) {
        showGame();
      }

      prompt.setText("Fill in the blank.");
      guessBox.setVisible(true);
      guessBox.setFocus(true);
      send.setVisible(true);
      pass.setVisible(true);
    //  if (result.getStimulus() != null) {
        showStimFull(result);
    /*  }
      else {
        System.err.println("huh? how can stimulus be null????");
      }*/
      stimulusCount++;
      outer.answer = result.getAnswer();
      //exerciseID = result.getExerciseID();
      System.out.println("--------> showStimulus game over = " + currentStimulus.isGameOver() +
        " stim count " + stimulusCount + " ex id " +result.getExerciseID());
    }

/*    private void showStim(StimulusAnswerPair result) {
      stimulus.setText("Clue #" + (stimulusCount+1) + "<br/><font color=#0036a2>" + result.getStimulus() +"</font>");
      stimulus.setVisible(true);
    }*/
    private void showStimFull(StimulusAnswerPair result) {
      stimulus.setText("Clue " + (stimulusCount+1) + " of " + result.getNumClues() + "<br/>" +
        "<font color=#0036a2>" + result.getStimulus() +"</font>");
      stimulus.setVisible(true);
    }

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
     // System.out.println(new Date() +"\tReceiverExerciseFactory.userHitEnterKey " + keyHandler);
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
      removeKeyHandler();
      cancelStimTimer();
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
          //System.out.println("RegisterAnswerResponseCallback.onSuccess -- ");

          checkForStimulus(service, controller, outer);
        }
      }
    }
  }
}
