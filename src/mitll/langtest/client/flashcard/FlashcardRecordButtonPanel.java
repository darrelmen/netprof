package mitll.langtest.client.flashcard;

import com.github.gwtbootstrap.client.ui.Heading;
import com.github.gwtbootstrap.client.ui.base.IconAnchor;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.DecoratedPopupPanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.ExerciseQuestionState;
import mitll.langtest.client.recorder.FlashcardRecordButton;
import mitll.langtest.client.recorder.RecordButton;
import mitll.langtest.client.recorder.RecordButtonPanel;
import mitll.langtest.client.sound.SoundFeedback;
import mitll.langtest.shared.AudioAnswer;
import mitll.langtest.shared.Exercise;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 10/25/13
 * Time: 1:43 PM
 * To change this template use File | Settings | File Templates.
 */
public class FlashcardRecordButtonPanel extends RecordButtonPanel implements RecordButton.RecordingListener {
  private static final int DELAY_MILLIS = 1000;
  private static final int DELAY_MILLIS_LONG = 3000;
  private static final int LONG_DELAY_MILLIS = 3500;
  private static final int DELAY_CHARACTERS = 40;
  private static final int HIDE_DELAY = 2500;

  private static final boolean NEXT_ON_BAD_AUDIO = false;
  private static final String PRONUNCIATION_SCORE = "Pronunciation score ";

  private static final String WAV = ".wav";
  private static final String MP3 = ".mp3";

  private final boolean isDemoMode;

  private final Exercise exercise;
  private final BootstrapExercisePanel exercisePanel;
  private final boolean continueToNext;

  /**
   *
   * @param exercisePanel
   * @param service
   * @param controller
   * @param exercise
   * @param index
   * @see BootstrapExercisePanel#getAnswerWidget(mitll.langtest.shared.Exercise, mitll.langtest.client.LangTestDatabaseAsync, mitll.langtest.client.exercise.ExerciseController, int, boolean)
   */
  public FlashcardRecordButtonPanel(BootstrapExercisePanel exercisePanel, LangTestDatabaseAsync service,
                                    ExerciseController controller, Exercise exercise, int index) {
    super(service, controller, exercise, null, index, true, controller.shouldAddRecordKeyBinding());

    this.exercisePanel = exercisePanel;
    this.exercise = exercise;
    isDemoMode = controller.isDemoMode();
    continueToNext = !controller.getProps().getFlashcardNextAndPrev();
    recordButton.setTitle("Press and hold the space bar or mouse button to record");
  }

  private IconAnchor waiting;
  private IconAnchor correctIcon;
  private IconAnchor incorrect;

  @Override
  protected void addImages() {
    waiting = new IconAnchor();
    correctIcon = new IconAnchor();
    incorrect = new IconAnchor();

    waiting.setBaseIcon(MyCustomIconType.waiting);
    waiting.setVisible(false);

    correctIcon.setBaseIcon(MyCustomIconType.correct);
    correctIcon.setVisible(false);

    incorrect.setBaseIcon(MyCustomIconType.incorrect);
    incorrect.setVisible(false);
  }

  @Override
  public Widget getRecordButton() {
    Widget recordButton1 = super.getRecordButton();
    Panel hp = new FlowPanel();
    hp.add(recordButton1);
    hp.add(waiting);
    hp.add(correctIcon);
    hp.add(incorrect);
    return hp;
  }

  protected RecordButton makeRecordButton(ExerciseController controller) {
    return new FlashcardRecordButton(controller.getRecordTimeout(), this, true, true);  // TODO : fix later in classroom?
  }

  /**
   * @see #receivedAudioAnswer(mitll.langtest.shared.AudioAnswer, mitll.langtest.client.exercise.ExerciseQuestionState, com.google.gwt.user.client.ui.Panel)
   * @param html
   */
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
    t.schedule(HIDE_DELAY);
  }

  private void initRecordButton() {
    recordButton.initRecordButton();
    correctIcon.setVisible(false);
    incorrect.setVisible(false);
    waiting.setVisible(false);
  }

  /**
   * Deal with three cases: <br></br>
   * * the audio was invalid in some way : too short, too quiet, too loud<br></br>
   * * the audio was the correct response<br></br>
   * * the audio was incorrect<br></br><p></p>
   * <p/>
   * And then move on to the next item.
   *
   * @param result        response from server
   * @param questionState ignored here
   * @param outer         ignored here
   * @see mitll.langtest.client.recorder.RecordButtonPanel#postAudioFile
   */
  @Override
  protected void receivedAudioAnswer(final AudioAnswer result, ExerciseQuestionState questionState, Panel outer) {
    boolean correct = result.isCorrect();
    final double score = result.getScore();
    recordButton.setVisible(false);
    waiting.setVisible(false);
    if (correct) {
      correctIcon.setVisible(true);
    } else {
      incorrect.setVisible(true);
    }

    String path = exercise.getRefAudio() != null ? exercise.getRefAudio() : exercise.getSlowAudioRef();
    final boolean hasRefAudio = path != null;

    String feedback = "";
    boolean badAudioRecording = result.validity != AudioAnswer.Validity.OK;
    if (badAudioRecording) {
      showPopup(result.validity.getPrompt());
      nextAfterDelay(correct, "");
    } else if (correct) {
      showCorrectFeedback(score);
    } else {   // incorrect!!
      if (hasRefAudio) {
        ensureMP3(result, score, hasRefAudio);
      } else {
        feedback = showIncorrectFeedback(result, score, hasRefAudio);
      }
    }
    if (!badAudioRecording && (correct || !hasRefAudio)) {
      System.out.println("receivedAudioAnswer: correct " + correct + " pron score : " + score + " has ref " + hasRefAudio);
      nextAfterDelay(correct, feedback);
    }
  }

  /**
   * Make sure all the mp3s we may play exist on the server.
   *
   * @param result
   * @param score
   * @param hasRefAudio
   */
  private void ensureMP3(final AudioAnswer result, final double score, final boolean hasRefAudio) {
    showIncorrectFeedback(result, score, hasRefAudio);
  }

  private void showCorrectFeedback(double score) {
    exercisePanel.showPronScoreFeedback(score, PRONUNCIATION_SCORE);
    exercisePanel.getSoundFeedback().playCorrect();
  }

  /**
   * If there's reference audio, play it and wait for it to finish.
   *
   * @param result
   * @param score
   * @param hasRefAudio
   * @see #receivedAudioAnswer(mitll.langtest.shared.AudioAnswer, mitll.langtest.client.exercise.ExerciseQuestionState, com.google.gwt.user.client.ui.Panel)
   */
  private String showIncorrectFeedback(AudioAnswer result, double score, boolean hasRefAudio) {
    if (result.isSaidAnswer()) { // if they said the right answer, but poorly, show pron score
      exercisePanel.showPronScoreFeedback(score, PRONUNCIATION_SCORE);
    }
    boolean hasSynonymAudio = !exercise.getSynonymAudioRefs().isEmpty();
    System.out.println("showIncorrectFeedback : playing " + exercise.getSynonymAudioRefs()
      + " result " + result + " score " + score + " has ref " + hasRefAudio +
      " hasSynonymAudio " + hasSynonymAudio);

    String correctPrompt = getCorrectDisplay();
    if (hasRefAudio && continueToNext) {
      System.out.println("has ref " + continueToNext);
      if (hasSynonymAudio) {
        List<String> toPlay = new ArrayList<String>(exercise.getSynonymAudioRefs());
        //  System.out.println("showIncorrectFeedback : playing " + toPlay);
        playAllAudio(correctPrompt, toPlay);
      } else {
        String path = exercise.getRefAudio();
        System.out.println("showIncorrectFeedback : regular " + path);
        if (path == null) {
          System.out.println("showIncorrectFeedback : slow " + path);
          path = exercise.getSlowAudioRef(); // fall back to slow audio
        }

        if (path == null) {
          exercisePanel.getSoundFeedback().playIncorrect(); // this should never happen
        } else {
          path = (path.endsWith(WAV)) ? path.replace(WAV, MP3) : path;
          path = ensureForwardSlashes(path);
          System.out.println("showIncorrectFeedback : playing " + path);

          final String fcorrectPrompt = correctPrompt;

          exercisePanel.getSoundFeedback().createSound(path, new SoundFeedback.EndListener() {
            @Override
            public void songEnded() {
              goToNextItem(fcorrectPrompt);
            }
          });
        }
      }
    } else {
      exercisePanel.getSoundFeedback().playIncorrect();

      System.out.println("doing nextAfterDelay");
      // Schedule the timer to run once in 1 seconds.
      Timer t = new Timer() {
        @Override
        public void run() {
          initRecordButton();
          exercisePanel.clearFeedback();
        }
      };
      int incorrectDelay = DELAY_MILLIS_LONG;
      t.schedule(incorrectDelay);
    }

    if (isDemoMode) {
      correctPrompt = "Heard: " + result.decodeOutput + "<p>" + correctPrompt;
    }
    Heading recoOutput = exercisePanel.getRecoOutput();
    if (recoOutput != null) {
      recoOutput.setText(correctPrompt);
      DOM.setStyleAttribute(recoOutput.getElement(), "color", "#000000");
    }
    return correctPrompt;
  }

  private String ensureForwardSlashes(String wavPath) {
    return wavPath.replaceAll("\\\\", "/");
  }

  /**
   * @param infoToShow
   * @param toPlay
   * @see #showIncorrectFeedback(mitll.langtest.shared.AudioAnswer, double, boolean)
   */
  private void playAllAudio(final String infoToShow, final List<String> toPlay) {
    String path = toPlay.get(0);
    path = (path.endsWith(WAV)) ? path.replace(WAV, MP3) : path;

    System.out.println("playAllAudio : " + toPlay.size() + " playing " + path);
    exercisePanel.getSoundFeedback().createSound(path, new SoundFeedback.EndListener() {
      @Override
      public void songEnded() {
        toPlay.remove(0);

        System.out.println("playAllAudio : songEnded " + toPlay.size() + " items left.");

        if (!toPlay.isEmpty()) {
          playAllAudio(infoToShow, toPlay);
        } else {
          goToNextItem(infoToShow);
        }
      }
    });
  }

  /**
   * @param infoToShow
   * @see #playAllAudio(String, java.util.List)
   * @see #showIncorrectFeedback(mitll.langtest.shared.AudioAnswer, double, boolean)
   */
  private void goToNextItem(String infoToShow) {
    if (continueToNext) {
      Timer t = new Timer() {
        @Override
        public void run() {
          controller.getExerciseList().loadNextExercise(exercise);
        }
      };
      int delay = getFeedbackLengthProportionalDelay(infoToShow);
      System.out.println("goToNextItem : using delay " + delay + " info " + infoToShow);
      t.schedule(isDemoMode ? LONG_DELAY_MILLIS : delay);
    } else {
      initRecordButton();
    }
  }

  private int getFeedbackLengthProportionalDelay(String feedback) {
    int mult1 = feedback.length() / DELAY_CHARACTERS;
    int mult = Math.max(1, mult1);
    return mult * DELAY_MILLIS;
  }

  private String getCorrectDisplay() {
    String refSentence = exercise.getRefSentence();
    String translit = exercise.getTranslitSentence().length() > 0 ? "<br/>(" + exercise.getTranslitSentence() + ")" : "";

    if (refSentence == null || refSentence.length() == 0) {
      List<Exercise.QAPair> questions = exercise.getForeignLanguageQuestions();
      refSentence = getAltAnswers(questions);
      List<Exercise.QAPair> eq = exercise.getEnglishQuestions();
      translit = "<br/>(" + getAltAnswers(eq) + " )";
    }
    boolean hasSynonyms = !exercise.getSynonymSentences().isEmpty();
    if (hasSynonyms) {
      refSentence = "";
      for (int i = 0; i < exercise.getSynonymSentences().size(); i++) {
        String synonym = exercise.getSynonymSentences().get(i);
        String translit2 = exercise.getSynonymTransliterations().get(i);
        refSentence += synonym + "(" + translit2 + ") or ";
      }
      refSentence = refSentence.substring(0, refSentence.length() - " or ".length());
    }
    return "Answer: " + refSentence + (hasSynonyms ? "" : translit);
  }

  private String getAltAnswers(List<Exercise.QAPair> questions) {
    Exercise.QAPair qaPair = questions.get(0);
    StringBuilder b = new StringBuilder();
    for (String alt : qaPair.getAlternateAnswers()) {
      if (alt.trim().length() > 0) {
        b.append(alt).append(", ");
      }
    }
    if (b.length() > 0) {
      return b.toString().substring(0, b.length() - 2);
    } else return "";
  }

  private void nextAfterDelay(boolean correct, String feedback) {
    if (NEXT_ON_BAD_AUDIO) {
      System.out.println("doing nextAfterDelay : correct " + correct + " feedback " + feedback);
      // Schedule the timer to run once in 1 seconds.
      Timer t = new Timer() {
        @Override
        public void run() {
          controller.getExerciseList().loadNextExercise(exercise);
        }
      };
      int incorrectDelay = DELAY_MILLIS_LONG;
      if (!feedback.isEmpty()) {
        int delay = getFeedbackLengthProportionalDelay(feedback);
        incorrectDelay += delay;

        System.out.println("nextAfterDelay Delay is " + incorrectDelay + " len " + feedback.length());
      }
      t.schedule(isDemoMode ? LONG_DELAY_MILLIS : correct ? DELAY_MILLIS : incorrectDelay);
    } else {
      if (!correct) {
        initRecordButton();
        exercisePanel.clearFeedback();
      } else {
        // go to next item
        Timer t = new Timer() {
          @Override
          public void run() {
            controller.getExerciseList().loadNextExercise(exercise);
          }
        };
        t.schedule(DELAY_MILLIS);
      }
    }
  }

  /**
   * @see #postAudioFile(com.google.gwt.user.client.ui.Panel, int)
   */
  @Override
  protected void receivedAudioFailure() {
    recordButton.setBaseIcon(MyCustomIconType.enter);
  }

  @Override
  public void flip(boolean first) {
  }

  @Override
  public void stopRecording() {
    super.stopRecording();
    recordButton.setVisible(false);
    waiting.setVisible(true);
  }
}
