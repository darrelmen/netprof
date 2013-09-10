package mitll.langtest.client.taboo;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.Column;
import com.github.gwtbootstrap.client.ui.ControlGroup;
import com.github.gwtbootstrap.client.ui.Controls;
import com.github.gwtbootstrap.client.ui.FluidContainer;
import com.github.gwtbootstrap.client.ui.Heading;
import com.github.gwtbootstrap.client.ui.Image;
import com.github.gwtbootstrap.client.ui.RadioButton;
import com.github.gwtbootstrap.client.ui.Row;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.safehtml.shared.UriUtils;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.PopupPanel;
import mitll.langtest.client.LangTest;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.bootstrap.BootstrapExercisePanel;
import mitll.langtest.client.bootstrap.LeaderboardPlot;
import mitll.langtest.client.exercise.BusyPanel;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.ExercisePanelFactory;
import mitll.langtest.client.sound.SoundFeedback;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.shared.Exercise;
import mitll.langtest.shared.ExerciseShell;
import mitll.langtest.shared.flashcard.Leaderboard;
import mitll.langtest.shared.taboo.AnswerBundle;
import mitll.langtest.shared.taboo.Game;
import mitll.langtest.shared.taboo.GameInfo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * <p/>
 * TODOs : add receiver factory -- user does a free form input of the guess...
 * what if they get part of the word?  feedback when they get it correct.
 * <p/>
 * User: GO22670
 * Date: 7/9/12
 * Time: 6:18 PM
 * To change this template use File | Settings | File Templates.
 */
public class GiverExerciseFactory extends ExercisePanelFactory {
  private static final int CHECK_FOR_CORRECT_POLL_PERIOD = 1000;
  public static final int MAX_CLUES = 3;
  public static final int MIN_BOGUS_STIM = 5;
  private static final int MAX_CLUES_TO_SEND = ReceiverExerciseFactory.MAX_CLUES_TO_GIVE;
  private static final String PLEASE_WAIT_FOR_RECEIVER_TO_ANSWER = "Please wait for receiver to answer...";
  public static final String SENTENCE = "clue";
  private int stimulusCount = 0;
  private GameInfo gameInfo;
  private long lastTimestamp;
  private int score;

  private GiverPanel giverPanel;
  private Map<String, Collection<String>> typeToSection;
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

  public void setSelectionState(Map<String, Collection<String>> typeToSection) {  this.typeToSection = typeToSection; }

  /**
   * @see mitll.langtest.client.exercise.ExerciseList#makeExercisePanel
   * @param e
   * @return
   */
  public Panel getExercisePanel(final Exercise e) {
    System.out.println("GiverExerciseFactory.getExercisePanel getting panel ... for " +e.getID());
    controller.pingAliveUser();

    giverPanel = new GiverPanel(e);
    return giverPanel;
  }

  /**
   * @see TabooExerciseList#setGameOnGiver(mitll.langtest.shared.taboo.GameInfo)
   * @param game
   */
  public void setGameOnGiver(GameInfo game) {
    if (gameInfo == null) gameInfo = game;
    if (giverPanel != null) {
      int numExercises = game.getNumExercises();
      if (numExercises > -1 && game.getTimestamp() != lastTimestamp) {
        System.out.println("GiverExerciseFactory.setGameOnGiver : last timestamp " + lastTimestamp + "/" + new Date(lastTimestamp)+
          " <> new timestamp " + game.getTimestamp() + "/" + new Date(game.getTimestamp())+
          " num exercises " + numExercises);

        stimulusCount = 0;
        score = 0;
        giverPanel.showGame(gameInfo);
        this.gameInfo = game;
        lastTimestamp = game.getTimestamp();
      }
    }
    else {
      System.out.println("GiverExerciseFactory.setGameOnGiver panel is null");
    }
  }

  private class GiverPanel extends FluidContainer implements BusyPanel {
    private List<Exercise.QAPair> sentItems = new ArrayList<Exercise.QAPair>();
    private Map<RadioButton, Exercise.QAPair> choiceToExample;
    private Controls choice = new Controls();
    private final Button send = new Button("Send");
    private Heading pleaseWait = new Heading(4, PLEASE_WAIT_FOR_RECEIVER_TO_ANSWER);
    private List<Exercise.QAPair> clueAnswerPairs;
    private Set<Exercise.QAPair> otherExerciseClues = new HashSet<Exercise.QAPair>();
    private Set<String> otherClues = new HashSet<String>();
    private Heading exerciseDisplay = new Heading(3);
    private Heading stimulus = new Heading(3);
    private String exerciseID;
    private Image correctImage   = new Image(UriUtils.fromSafeConstant(LangTest.LANGTEST_IMAGES + "checkmark48.png"));
    private Image incorrectImage = new Image(UriUtils.fromSafeConstant(LangTest.LANGTEST_IMAGES + "redx48.png"));
    private Image arrowImage = new Image(UriUtils.fromSafeConstant(LangTest.LANGTEST_IMAGES + "rightArrow.png"));

    public GiverPanel(final Exercise exercise) {
      if (exercise == null) {
        System.err.println("huh? exercise is null?");
        return;
      }
      exerciseID = exercise.getID();

      Row w5 = new Row();
      w5.add(exerciseDisplay);
      add(w5);

      clueAnswerPairs = Game.randomSample2(exercise.getQuestions(), ReceiverExerciseFactory.MAX_CLUES_TO_GIVE, rnd);

      if (clueAnswerPairs.isEmpty()) System.err.println("huh? clueAnswerPairs is empty???");

      if (gameInfo != null) {
        showGame(gameInfo);
      }
      else {
        System.out.println("GiverPanel : game info is null");
      }
      Row w33 = new Row();
      w33.add(stimulus);
      add(w33);

      HTML warnNoFlash = new HTML(BootstrapExercisePanel.WARN_NO_FLASH);
      warnNoFlash.setVisible(false);
      final SoundFeedback soundFeedback = new SoundFeedback(controller.getSoundManager(), warnNoFlash);

      Row w4 = new Row();
      w4.add(new Heading(3, "User is trying to guess: "));
      add(w4);

      Row w = new Row();

      final String vocabItem = getVocabItem(exercise);
      w.add(new Heading(2, vocabItem));
      add(w);

      Row w2 = new Row();
      w2.add(new Column(12));
      add(w2);

      Row w3 = new Row();
      w3.add(new Heading(4, "Choose a hint " +
        SENTENCE +
        " to send : "));
      add(w3);

      otherClues.clear();
      otherExerciseClues.clear();

      getUnrelatedClues(7);
      // recordingStyle.add(new ControlLabel("<b>Audio Recording Style</b>"));
      final ControlGroup recordingStyle = new ControlGroup(); // necessary?
      recordingStyle.add(choice);
      add(choice);

      add(warnNoFlash);
      send.setType(ButtonType.PRIMARY);
      send.setEnabled(true);
     // send.setTitle("Hit Enter to send.");

      send.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) { clickOnSend(exercise, soundFeedback); }
      });

      add(send);
      add(pleaseWait);
      pleaseWait.setText("");
    }

    /**
     * TODO : do this in not-dumb way
     * @param n
     */
    private void getUnrelatedClues(final int n) {
      controller.askForRandomExercise(new AsyncCallback<Exercise>() {
        @Override
        public void onFailure(Throwable caught) {
          //To change body of implemented methods use File | Settings | File Templates.
        }

        @Override
        public void onSuccess(Exercise result) {
          List<Exercise.QAPair> questions = result.getQuestions();
        //  System.out.println("getUnrelatedClues " + n + " for " + result.getID() + " got " + questions.size());
          for (Exercise.QAPair pair : questions) {
            if (!otherClues.contains(pair.getQuestion())) {
              otherClues.add(pair.getQuestion());
              otherExerciseClues.add(pair);
             // System.out.println("getUnrelatedClues map " + pair + " to " +result.getID());
              break;
            }
          }

          if (otherExerciseClues.size() > MIN_BOGUS_STIM) {
            //System.out.println("getUnrelatedClues otherExerciseClues " + otherExerciseClues.size());

            choiceToExample = populateChoices(result.getID(),clueAnswerPairs, otherExerciseClues);
          } else {
            if (n > 0) {
              getUnrelatedClues(n - 1);
            }
            else {
            //  System.out.println("getUnrelatedClues " + n + " for " + result.getID() + " got " + questions.size());
              choiceToExample = populateChoices(result.getID(),clueAnswerPairs, otherExerciseClues);
            }
          }
        }
      });
    }
    /**
     * @see GiverExerciseFactory#setGameOnGiver(mitll.langtest.shared.taboo.GameInfo)
     * @param gameInfo
     */
    private void showGame(GameInfo gameInfo) {
      System.out.println("----> showGame " + gameInfo + " with " + exerciseID);

      String gameInfoString = "Game #" + (gameInfo.getGameCount());// + " of " + gameInfo.getNumGames();
      int indexOfItem = gameInfo.getIndexOfItem(exerciseID);
      if (indexOfItem == -1) {
        System.err.println("\n\n\n----> showGame can't find ex # " + exerciseID);
      } else {
        exerciseDisplay.setText(gameInfoString + ", item #" + (indexOfItem+1));// + " of " + gameInfo.getNumExercises());
        showStimIndex();
      }
    }

    /**
     * @see #showGame(mitll.langtest.shared.taboo.GameInfo)
     * @see #showCorrectFeedback(mitll.langtest.client.sound.SoundFeedback, mitll.langtest.shared.Exercise)
     * @see #showIncorrectFeedback(mitll.langtest.shared.taboo.AnswerBundle, mitll.langtest.client.sound.SoundFeedback, mitll.langtest.shared.Exercise)
     */
    private void showStimIndex() {
      stimulus.setText("Clue #" + (stimulusCount+1));// + " of " + clueAnswerPairs.size());
    }

/*    private void showGameFull(GameInfo gameInfo) {
      String gameInfoString = "Game " + (gameInfo.getGameCount() + 1) + " of " + gameInfo.getNumGames();
      int numExercises = gameInfo.getNumExercises();
      exerciseDisplay.setText(gameInfoString + ", item " + (exerciseCount) + " of " + numExercises);
      stimulus.setText("Clue " + (stimulusCount) + " of " + clueAnswerPairs.size());
    }*/

    private void clickOnSend(Exercise exercise, SoundFeedback soundFeedback) {
      final Exercise.QAPair stimulus = getSelectedItem();
      if (stimulus != null) {
        sentItems.add(stimulus);
        send.setEnabled(false);
        pleaseWait.setText(PLEASE_WAIT_FOR_RECEIVER_TO_ANSWER);
        controller.pingAliveUser();
        boolean lastChoiceRemaining = sentAllClues();
        if (lastChoiceRemaining) {
          System.out.println("\n---> clickOnSend : sent " + sentItems.size() + " but num clues " + clueAnswerPairs.size());
        }
       // String id = questionToExercise.get(stimulus).getID();
        boolean giverChosePoorly = otherExerciseClues.contains(stimulus);// !exercise.getID().equals(id);
        System.out.println("giver chose poorly = " + giverChosePoorly);// + " " + exercise.getID() + " vs " + id);
        String correctResponse = stimulus.getAnswer();
        if (giverChosePoorly) {   // if the clue is for a different item, they can still guess correctly...
          correctResponse = getVocabItem(exercise);
        }
        sendStimulus(stimulus.getQuestion(), exercise,
          correctResponse, soundFeedback, lastChoiceRemaining, clueAnswerPairs.size(), giverChosePoorly);
      } else {
        new PopupHelper().showPopup("Please select a " +
          SENTENCE +
          " to send.");
      }
    }

    private boolean sentAllClues() {
      return (clueAnswerPairs.size() == sentItems.size());
    }

    private Random rnd = new Random();
    private String lastSentExercise = "";

    /**
     * @see GiverPanel#clickOnSend
     * @param stimulus
     * @param exercise
     * @param soundFeedback
     * @param lastChoiceRemaining
     * @param numClues
     * @param giverChosePoorly
     */
    private void sendStimulus(final String stimulus, final Exercise exercise,
                              String answer, final SoundFeedback soundFeedback,
                              boolean lastChoiceRemaining, int numClues, boolean giverChosePoorly) {
      final int user = controller.getUser();
      String exerciseID = exercise.getID();
      boolean differentExercise = lastSentExercise.length() > 0 && !lastSentExercise.equals(exerciseID);
      lastSentExercise = exerciseID;

      boolean onLast = gameInfo.onLast(exercise);

      System.out.println(new Date() +" : sendStimulus lastSentExercise : " + lastSentExercise + " Sent '" + stimulus + "' on last " +onLast);

      stimulusCount++;
      service.sendStimulus(user, exerciseID, stimulus, answer, lastChoiceRemaining,
        differentExercise, numClues, onLast, giverChosePoorly, new AsyncCallback<Integer>() {
        @Override
        public void onFailure(Throwable caught) { Window.alert("sendStimulus : couldn't contact server."); }

        @Override
        public void onSuccess(Integer result) {
          if (result == 0) { // partner is still active
            System.out.println("sendStimulus.onSuccess : Giver " + user + " Sent '" + stimulus + "'");
            checkForCorrect(user, stimulus, exercise, soundFeedback);
          }
        }
      });
    }

    /**
     * @see #clickOnSend
     * @return
     */
    private Exercise.QAPair getSelectedItem() {
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
     * @see #getUnrelatedClues(int)
     * @param clueAnswerPairs
     * @return
     */
    private Map<RadioButton, Exercise.QAPair> populateChoices(String exerciseID,
                                                              List<Exercise.QAPair> clueAnswerPairs,
                                                              Set<Exercise.QAPair> otherExerciseClues) {
     // System.out.println("populate choices for " + exerciseID);
      if (clueAnswerPairs.isEmpty()) {
        System.err.println("huh? no clues?");
      }

      Set<Exercise.QAPair> allClues = new HashSet<Exercise.QAPair>();

      List<Exercise.QAPair> notSentYet = getNotSentYetHints(clueAnswerPairs);
      Iterator<Exercise.QAPair> iterator = notSentYet.iterator();
      Set<String> cluePhrases = new HashSet<String>();   // guarantee uniqueness
      while (allClues.size() < MAX_CLUES && iterator.hasNext()) {
        Exercise.QAPair next = iterator.next();
        String clue = next.getQuestion();
        if (!cluePhrases.contains(clue)) {
          cluePhrases.add(clue);
          allClues.add(next);
        }
      }

      iterator = otherExerciseClues.iterator();
      while (allClues.size() < ReceiverExerciseFactory.MAX_CLUES_TO_GIVE && iterator.hasNext()) {
        Exercise.QAPair next = iterator.next();
        if (!cluePhrases.contains(next.getQuestion()) && !sentItems.contains(next)) {
          cluePhrases.add(next.getQuestion());
          allClues.add(next);
        }
      }

      List<Exercise.QAPair> clues = new ArrayList<Exercise.QAPair>(allClues);

      new Shuffler().shuffle2(clues, rnd);  // TODO parameterize?

      choice.clear();
      Map<RadioButton, Exercise.QAPair> choiceToExample = new HashMap<RadioButton, Exercise.QAPair>();
      for (Exercise.QAPair example : clues) {
        final RadioButton give = new RadioButton("Giver", example.getQuestion().replaceAll("_\\s_", "__"));
        choiceToExample.put(give, example);
        choice.add(give);
      }
      return choiceToExample;
    }

    private List<Exercise.QAPair> getNotSentYetHints(List<Exercise.QAPair> clueAnswerPairs) {
      List<Exercise.QAPair> notSentYet = new ArrayList<Exercise.QAPair>();
     // System.out.println("getNotSentYetHints for " + synonymSentences.size());
      for (Exercise.QAPair candidate : clueAnswerPairs) {
        if (sentItems.contains(candidate)) {
          //System.out.println("---> already sent " + candidate);
        } else {
          notSentYet.add(candidate);
        }
      }
     // System.out.println("getNotSentYetHints now " + notSentYet.size());
      return notSentYet;
    }

    /**
     * Keep asking server if the receiver has made a correct answer or not.
     *
     * @see #sendStimulus(String, mitll.langtest.shared.Exercise, String, mitll.langtest.client.sound.SoundFeedback, boolean, int, boolean)
     * @param userid
     * @param stimulus
     * @param current
     * @paramx refSentence
     */
    private void checkForCorrect(final long userid, final String stimulus, final Exercise current,
                                 final SoundFeedback feedback) {
      //System.out.println(new Date() +  " giver : checkForCorrect for " + stimulus );
      service.checkCorrect(userid, new AsyncCallback<AnswerBundle>() {
        @Override
        public void onFailure(Throwable caught) {
          Window.alert("checkCorrect : couldn't contact server.");
        }

        @Override
        public void onSuccess(AnswerBundle result) {

          if (result.didReceiverReply()) {
            System.out.println("\t" + new Date() + "giver : onSuccess got " + result);
            gotAnswerFromReceiver(result, feedback, current);
          } else { // they haven't answered yet
            Timer t = new Timer() {
              @Override
              public void run() {
                checkForCorrect(userid, stimulus, current, feedback);
              }
            };
            t.schedule(CHECK_FOR_CORRECT_POLL_PERIOD);
          }
        }
      });
    }

    private void gotAnswerFromReceiver(AnswerBundle result, SoundFeedback feedback, final Exercise current) {
      if (result.isCorrect()) {
        showCorrectFeedback(feedback, current);
      } else {
        showIncorrectFeedback(result, feedback, current);
      }
    }

    private void showCorrectFeedback(SoundFeedback feedback, Exercise current) {
      lastSentExercise = ""; // clear last sent hint -- they got it correct
      int i = ReceiverExerciseFactory.MAX_CLUES_TO_GIVE - stimulusCount + 1;
      // System.out.println("sendAnswer : adding " + i + " to " + score + " clues " + displayedStimulus.getNumClues() + " stim " + stimulusCount);
      score += i;
      stimulusCount = 0;
      showStimIndex();
      new PopupHelper().showPopup("They guessed correctly!  Moving on to next item.", correctImage);
      feedback.playCorrect();

      loadNext(current);
      send.setEnabled(true);
      //pleaseWait.setVisible(false);
      pleaseWait.setText("");
    }

    private void showIncorrectFeedback(AnswerBundle result, SoundFeedback feedback, final Exercise current) {
      feedback.playIncorrect();
      otherClues.clear();
      otherExerciseClues.clear();
      getUnrelatedClues(7);

      if (stimulusCount == MAX_CLUES_TO_SEND || sentAllClues()) {
        new PopupHelper().showPopup("They didn't guess correctly in " + stimulusCount + " tries" +
          ", moving to next item...", incorrectImage, arrowImage, new CloseHandler<PopupPanel>() {
          @Override
          public void onClose(CloseEvent<PopupPanel> event) {
            stimulusCount = 0;
            showStimIndex();
            loadNext(current);
          }
        });
      } else {
        showStimIndex();

        boolean wasRealAnswer = result.getAnswer().length() > 0;
        String message = wasRealAnswer ? "They guessed : <b>" +
          result.getAnswer() +
          "</b>, please send another clue." : "They passed, please send another clue.";
        new PopupHelper().showPopup(message, incorrectImage);

        send.setEnabled(true);
        pleaseWait.setText(wasRealAnswer ? "Last guess was : <b>" + result.getAnswer() + "</b>" : "Partner passed.");
      }
    }

    @Override
    public boolean isBusy() {
      return pleaseWait.getText().length() > 0;
    }
  }

  private String getVocabItem(Exercise exercise) {
    return controller.getProps().doTabooEnglish() ? exercise.getEnglishSentence().trim() : exercise.getRefSentence().trim();
  }

  private void loadNext(Exercise current) {
    System.out.println("----> Giver : loadNext " + current.getID());

    if (gameInfo.onLast(current)) {
      System.out.println("Giver : game over! ");
      showLeaderboard();
    }
    else {
      System.out.println("Giver : loadNext " + current.getID() + " not last...");

      ExerciseShell next = gameInfo.getNext(current);
      if (next == null) System.err.println("huh? nothing after " + current);
      else controller.loadExercise(next);
    }
  }

  private void showLeaderboard() {
    service.getLeaderboard(typeToSection, new AsyncCallback<Leaderboard>() {
      @Override
      public void onFailure(Throwable caught) {
      }

      @Override
      public void onSuccess(Leaderboard result) {
        if (result == null) System.err.println("huh? no leaderboard for " + typeToSection.toString());
        else {
          String message = "Game complete! Score was " + score + " out of " + gameInfo.getTotalClues();
          /*Modal plot =*/ new LeaderboardPlot().showLeaderboardPlot(result, controller.getUser(), 0, typeToSection,
            message, 5000);
        }
      }
    });
  }
}
