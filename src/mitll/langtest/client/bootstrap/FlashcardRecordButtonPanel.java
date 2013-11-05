package mitll.langtest.client.bootstrap;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.user.client.ui.DecoratedPopupPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.PopupPanel;
import mitll.langtest.client.LangTest;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.ExerciseQuestionState;
import mitll.langtest.client.recorder.RecordButton;
import mitll.langtest.client.recorder.RecordButtonPanel;
import mitll.langtest.client.sound.SoundFeedback;
import mitll.langtest.shared.AudioAnswer;
import mitll.langtest.shared.Exercise;

import com.github.gwtbootstrap.client.ui.Heading;
import com.github.gwtbootstrap.client.ui.Image;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.safehtml.shared.UriUtils;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Panel;

/**
* Created with IntelliJ IDEA.
* User: GO22670
* Date: 10/25/13
* Time: 1:43 PM
* To change this template use File | Settings | File Templates.
*/
public class FlashcardRecordButtonPanel extends RecordButtonPanel {
  private static final int KEY_PRESS = 256;
  private static final int KEY_UP = 512;
  private static final int SPACE_CHAR = 32;
  private static final int DELAY_MILLIS = 1000;
  private static final int DELAY_MILLIS_LONG = 3000;
  private static final int LONG_DELAY_MILLIS = 3500;
  private static final int DELAY_CHARACTERS = 40;
  private static final int PERIOD_MILLIS = 300;
  private static final int HIDE_DELAY = 2500;

  private static final boolean NEXT_ON_BAD_AUDIO = false;
 // private static final boolean CONTINUE_TO_NEXT = false;
 private static final String PRONUNCIATION_SCORE = "Pronunciation score ";

  private static final String WAV = ".wav";
  private static final String MP3 = ".mp3";
  private static final String NO_SPACE_WARNING = "Press and hold space bar to begin recording, release to stop.";

  private Image waitingForResponseImage = new Image(UriUtils.fromSafeConstant(LangTest.LANGTEST_IMAGES + "animated_progress48.gif"));
  private Image recordImage1 = new Image(UriUtils.fromSafeConstant(LangTest.LANGTEST_IMAGES + "media-record-3.png"));
  private Image recordImage2 = new Image(UriUtils.fromSafeConstant(LangTest.LANGTEST_IMAGES + "media-record-4.png"));

  public Image correctImage = new Image(UriUtils.fromSafeConstant(LangTest.LANGTEST_IMAGES + "checkmark48.png"));
  public Image incorrectImage = new Image(UriUtils.fromSafeConstant(LangTest.LANGTEST_IMAGES + "redx48.png"));
  private Image enterImage;  // todo change to text with blue background
  private Timer t;
  private boolean keyIsDown;
  private boolean isDemoMode;
  boolean warnUserWhenNotSpace;

  private final Exercise exercise;
  private BootstrapExercisePanel widgets;
  private final boolean continueToNext;

  /**
   * @see BootstrapExercisePanel#getAnswerWidget(mitll.langtest.shared.Exercise, mitll.langtest.client.LangTestDatabaseAsync, mitll.langtest.client.exercise.ExerciseController, int)
   * @param widgets
   * @param service
   * @param controller
   * @param exercise
   * @param index
   * @param warnUserWhenNotSpace
   */
  public FlashcardRecordButtonPanel(BootstrapExercisePanel widgets, LangTestDatabaseAsync service,
                                    ExerciseController controller, Exercise exercise, int index,
                                    boolean warnUserWhenNotSpace) {
    super(service, controller, exercise, null, index);
    this.widgets = widgets;
    this.exercise = exercise;
    isDemoMode = controller.isDemoMode();
    this.warnUserWhenNotSpace = warnUserWhenNotSpace;
    continueToNext = !controller.getProps().getFlashcardNextAndPrev();
    recordButton.setTitle("Please press and hold the space bar to record");
  }

  @Override
  protected RecordButton makeRecordButton(ExerciseController controller, final RecordButtonPanel outer) {
    return new RecordButton(controller.getRecordTimeout(), true) {
      @Override
      protected void stopRecording() {
        outer.stopRecording();
      }

      @Override
      protected void startRecording() {
        outer.startRecording();
      }

      @Override
      protected void showRecording() {
        outer.showRecording();
      }

      @Override
      protected void showStopped() {
        outer.showStopped();
      }

      @Override
      public void doClick() {
        super.doClick();
      }

      @Override
      protected HandlerRegistration addKeyHandler() {
        return Event.addNativePreviewHandler(new
                                               Event.NativePreviewHandler() {

                                                 @Override
                                                 public void onPreviewNativeEvent(Event.NativePreviewEvent event) {
                                                   NativeEvent ne = event.getNativeEvent();
                                                   if ("[object KeyboardEvent]".equals(ne.getString())) {

                        /*                             System.out.println(new java.util.Date() + " : key handler : Got " + event + " type int " +
                                                       event.getTypeInt() + " assoc " + event.getAssociatedType() +
                                                       " native " + event.getNativeEvent() + " source " + event.getSource() + " " + ne.getCharCode());
*/
                                                     int typeInt = event.getTypeInt();
                                                     boolean keyPress = typeInt == KEY_PRESS;
                                                     boolean isSpace = ne.getCharCode() == SPACE_CHAR;

                                                     if (keyPress && !isSpace) {
                                                       if (warnUserWhenNotSpace) {
                                                         /*widgets.*/showPopup(NO_SPACE_WARNING);
                                                       }
                                                     }
                                                     if (keyPress && !keyIsDown && isSpace) {
                                                       keyIsDown = true;
                                                       doClick();
                                                     } else {
                                                       boolean keyUp = typeInt == KEY_UP;
                                                       if (keyUp && keyIsDown) {
                                                         keyIsDown = false;
                                                         doClick();
                                                       }
                                                     }
                                                   }
                                                 }
                                               });
      }
    };
  }

  @Override
  protected void layoutRecordButton() {}

  public void showPopup(String html) {
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

  @Override
  protected Anchor makeRecordButton() {
    recordButton = new ImageAnchor() {
      @Override
      protected void onLoad() {
        super.onLoad();
        recordButton.setFocus(true);
      }
    };
    enterImage = new Image(UriUtils.fromSafeConstant(LangTest.LANGTEST_IMAGES + "blueSpaceBar.png"));

    initRecordButton();

    return recordButton;
  }

  private void initRecordButton() {
    recordButton.setResource(enterImage);
    recordButton.setFocus(true);
    recordButton.addFocusHandler(new FocusHandler() {
      @Override
      public void onFocus(FocusEvent event) {
        System.out.println("record button got the focus! -------------- ");
      }
    });

    recordButton.addKeyDownHandler(new KeyDownHandler() {
      @Override
      public void onKeyDown(KeyDownEvent event) {
        System.out.println(" record button got key down : " + event + "-------------- ");
      }
    });
  }

  private boolean first = true;

  @Override
  public void showRecording() {
    recordButton.setResource(recordImage1);
    recordButton.setHeight("112px");

    flipImage();
  }

  private void flipImage() {
    t = new Timer() {
      @Override
      public void run() {
        recordButton.setResource(first ? recordImage2 : recordImage1);
        recordButton.setHeight("112px");
        first = !first;
      }
    };
    t.scheduleRepeating(PERIOD_MILLIS);
  }

  @Override
  public void showStopped() {
    t.cancel();
    recordButton.setResource(waitingForResponseImage);
    recordButton.setHeight("112px");
  }

  private int numMP3s = 0;

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

    recordButton.setResource(correct ? correctImage : incorrectImage);
    recordButton.setHeight("112px");

    String path = exercise.getRefAudio() != null ? exercise.getRefAudio() : exercise.getSlowAudioRef();
    final boolean hasRefAudio = path != null;
    //System.out.println("answer correct = " + correct + " pron score : " + score + " has ref " + hasRefAudio);

    String feedback = "";
    boolean badAudioRecording = result.validity != AudioAnswer.Validity.OK;
    if (badAudioRecording) {
      /*widgets.*/showPopup(result.validity.getPrompt());
      nextAfterDelay(correct, "");
    } else if (correct) {
      showCorrectFeedback(score);
    } else {   // incorrect!!
      if (hasRefAudio) {
        ensureMP3(result, score, path, hasRefAudio);
      } else {
        feedback = showIncorrectFeedback(result, score, hasRefAudio);
      }
    }
    if (!badAudioRecording && (correct || !hasRefAudio)) {
      System.out.println("receivedAudioAnswer: correct " + correct + " pron score : " + score +" has ref " + hasRefAudio);
      nextAfterDelay(correct, feedback);
    }
  }

  /**
   * Make sure all the mp3s we may play exist on the server.
   *
   * @param result
   * @param score
   * @param path
   * @param hasRefAudio
   */
  private void ensureMP3(final AudioAnswer result, final double score, String path, final boolean hasRefAudio) {
    boolean hasSynonymAudio = !exercise.getSynonymAudioRefs().isEmpty();

    if (hasSynonymAudio) {
      numMP3s = exercise.getSynonymAudioRefs().size();
      for (String spath : exercise.getSynonymAudioRefs()) {
        //   spath = (spath.endsWith(WAV)) ? spath.replace(WAV, MP3) : spath;
        service.ensureMP3(spath, new AsyncCallback<Void>() {
          @Override
          public void onFailure(Throwable caught) {
            Window.alert("Couldn't contact server (ensureMP3).");
          }

          @Override
          public void onSuccess(Void result2) {
            numMP3s--;
            if (numMP3s == 0) {
              showIncorrectFeedback(result, score, hasRefAudio);
            }
          }
        });
      }
    } else {
      service.ensureMP3(path, new AsyncCallback<Void>() {
        @Override
        public void onFailure(Throwable caught) {
          Window.alert("Couldn't contact server (ensureMP3).");
        }

        @Override
        public void onSuccess(Void result2) {
          showIncorrectFeedback(result, score, hasRefAudio);
        }
      });
    }
  }

  private void showCorrectFeedback(double score) {
    widgets.showPronScoreFeedback(score, PRONUNCIATION_SCORE);
    widgets.getSoundFeedback().playCorrect();
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
    widgets.showPronScoreFeedback(score, PRONUNCIATION_SCORE);
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
        playAllAudio(correctPrompt,toPlay);
      } else {
        String path = exercise.getRefAudio();
        if (path == null) {
          path = exercise.getSlowAudioRef(); // fall back to slow audio
        }
        if (path == null) {
          widgets.getSoundFeedback().playIncorrect(); // this should never happen
        } else {
          path = (path.endsWith(WAV)) ? path.replace(WAV, MP3) : path;
          final String fcorrectPrompt = correctPrompt;

          widgets.getSoundFeedback().createSound(path, new SoundFeedback.EndListener() {
            @Override
            public void songEnded() {
              goToNextItem(fcorrectPrompt);
            }
          });
        }
      }
    } else {
      widgets.getSoundFeedback().playIncorrect();

      System.out.println("doing nextAfterDelay");
      // Schedule the timer to run once in 1 seconds.
      Timer t = new Timer() {
        @Override
        public void run() {
          initRecordButton();
          widgets.clearFeedback();
        }
      };
      int incorrectDelay = DELAY_MILLIS_LONG;
      t.schedule(incorrectDelay);
    }

    if (isDemoMode) {
      correctPrompt = "Heard: " + result.decodeOutput + "<p>" + correctPrompt;
    }
    Heading recoOutput = widgets.getRecoOutput();
    if (recoOutput != null) {
      recoOutput.setText(correctPrompt);
      DOM.setStyleAttribute(recoOutput.getElement(), "color", "#000000");
    }
    return correctPrompt;
  }

  private void playAllAudio(final String infoToShow, final List<String> toPlay) {
    String path = toPlay.get(0);
    path = (path.endsWith(WAV)) ? path.replace(WAV, MP3) : path;

    System.out.println("playAllAudio : " + toPlay.size() + " playing " + path);
    widgets.getSoundFeedback().createSound(path, new SoundFeedback.EndListener() {
      @Override
      public void songEnded() {
        toPlay.remove(0);

        System.out.println("playAllAudio : songEnded " + toPlay.size() + " items left.");

        if (!toPlay.isEmpty()) {
          playAllAudio(infoToShow,toPlay);
        } else {
          goToNextItem(infoToShow);
        }
      }
    });
  }

  private void goToNextItem(String infoToShow) {
    if (continueToNext) {
      Timer t = new Timer() {
        @Override
        public void run() {
          controller.loadNextExercise(exercise);
        }
      };
      int delay = getFeedbackLengthProportionalDelay(infoToShow);
      System.out.println("goToNextItem : using delay " + delay);
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
      System.out.println("doing nextAfterDelay");
      // Schedule the timer to run once in 1 seconds.
      Timer t = new Timer() {
        @Override
        public void run() {
          controller.loadNextExercise(exercise);
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
      initRecordButton();
      widgets.clearFeedback();
    }
  }

  /**
   * @see #postAudioFile(com.google.gwt.user.client.ui.Panel, int)
   */
  @Override
  protected void receivedAudioFailure() {
    recordButton.setResource(enterImage);
  }
}
