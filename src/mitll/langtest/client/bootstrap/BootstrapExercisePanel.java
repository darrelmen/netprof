package mitll.langtest.client.bootstrap;

import com.github.gwtbootstrap.client.ui.Column;
import com.github.gwtbootstrap.client.ui.Dropdown;
import com.github.gwtbootstrap.client.ui.FluidContainer;
import com.github.gwtbootstrap.client.ui.FluidRow;
import com.github.gwtbootstrap.client.ui.Heading;
import com.github.gwtbootstrap.client.ui.Image;
import com.github.gwtbootstrap.client.ui.Nav;
import com.github.gwtbootstrap.client.ui.NavLink;
import com.github.gwtbootstrap.client.ui.Paragraph;
import com.github.gwtbootstrap.client.ui.ProgressBar;
import com.github.gwtbootstrap.client.ui.base.ProgressBarBase;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.i18n.client.HasDirection;
import com.google.gwt.i18n.shared.WordCountDirectionEstimator;
import com.google.gwt.media.client.Audio;
import com.google.gwt.safehtml.shared.UriUtils;
import com.google.gwt.storage.client.Storage;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.DecoratedPopupPanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.LangTest;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.ExerciseQuestionState;
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
 * Date: 3/6/13
 * Time: 3:07 PM
 * To change this template use File | Settings | File Templates.
 */
public class BootstrapExercisePanel extends FluidContainer {
  private static final int LONG_DELAY_MILLIS = 3500;
  private static final int DELAY_MILLIS = 1000;//1250;
  private static final int DELAY_MILLIS_LONG = 3000;
  private static final String PRONUNCIATION_SCORE = "Pronunciation score ";
  private static final int DELAY_CHARACTERS = 40;
  private static final String LISTEN_TO_THIS_AUDIO = "Listen to this audio";
  private static final String FEEDBACK_TIMES_SHOWN = "FeedbackTimesShown";
  private static final int PERIOD_MILLIS = 300;
  private static final int MAX_INTRO_FEEBACK_COUNT = -1;
  private static final int KEY_PRESS = 256;
  private static final int KEY_UP = 512;
  private static final int SPACE_CHAR = 32;
  private static final int HIDE_DELAY = 2500;
  private static final String WAV = ".wav";
  private static final String MP3 = ".mp3";
  private static final boolean NEXT_ON_BAD_AUDIO = false;

  private Column scoreFeedbackColumn;
  private List<MyRecordButtonPanel> answerWidgets = new ArrayList<MyRecordButtonPanel>();
  private Storage stockStore = null;

  private Image waitingForResponseImage = new Image(UriUtils.fromSafeConstant(LangTest.LANGTEST_IMAGES + "animated_progress48.gif"));
  private Image recordImage1 = new Image(UriUtils.fromSafeConstant(LangTest.LANGTEST_IMAGES + "media-record-3.png"));
  private Image recordImage2 = new Image(UriUtils.fromSafeConstant(LangTest.LANGTEST_IMAGES + "media-record-4.png"));
  public Image correctImage = new Image(UriUtils.fromSafeConstant(LangTest.LANGTEST_IMAGES + "checkmark48.png"));
  public Image incorrectImage = new Image(UriUtils.fromSafeConstant(LangTest.LANGTEST_IMAGES + "redx48.png"));
  private Image enterImage = new Image(UriUtils.fromSafeConstant(LangTest.LANGTEST_IMAGES + "blueSpaceBar.png"));
  public static final String WARN_NO_FLASH = "<font color='red'>Flash is not activated. Do you have a flashblocker? Please add this site to its whitelist.</font>";

  private Heading recoOutput;
  private boolean keyIsDown;
  private boolean isDemoMode;
  private int feedback = 0;
  private Timer t;
  private ProgressBar scoreFeedback = new ProgressBar();
  private SoundFeedback soundFeedback;

  /**
   * @param e
   * @param service
   * @param controller
   * @see mitll.langtest.client.flashcard.FlashcardExercisePanelFactory#getExercisePanel(mitll.langtest.shared.Exercise)
   */
  public BootstrapExercisePanel(final Exercise e, final LangTestDatabaseAsync service,
                                final ExerciseController controller) {
    setStyleName("exerciseBackground");
    addStyleName("cardBorder");
    stockStore = Storage.getLocalStorageIfSupported();
    if (stockStore != null) {
      feedback = getCookieValue(FEEDBACK_TIMES_SHOWN);
    }
    isDemoMode = controller.isDemoMode();
    HTML warnNoFlash = new HTML(WARN_NO_FLASH);
    soundFeedback = new SoundFeedback(controller.getSoundManager(), warnNoFlash);

    add(getHelpRow(controller));
    add(getCardPrompt(e, controller));
    addRecordingAndFeedbackWidgets(e, service, controller);
    warnNoFlash.setVisible(false);
    add(warnNoFlash);
  }

  /**
   * Store a cookie for the initial feedback for new users.
   *
   * @param key
   * @return
   */
  private int getCookieValue(String key) {
    int times = 0;
    String timesHelpShown = stockStore.getItem(key);
    if (timesHelpShown == null) {
      stockStore.setItem(key, "1");
    } else {
      try {
        times = Integer.parseInt(timesHelpShown);
      } catch (NumberFormatException e1) {
        times = 10;
      }
    }
    return times;
  }

  /**
   * @param key
   * @see MyRecordButtonPanel#showCorrectFeedback(double)
   */
  private void incrCookie(String key) {
    int cookieValue = getCookieValue(key);
    stockStore.setItem(key, "" + (cookieValue + 1));
  }

  /**
   * Make row for help question mark, right justify
   *
   * @param controller
   * @return
   */
  private Widget getHelpRow(final ExerciseController controller) {
    FlowPanel helpRow = new FlowPanel();
    helpRow.addStyleName("floatRight");
    helpRow.addStyleName("helpPadding");

    // add help image on right side of row
    helpRow.add(getHelp(controller));
    return helpRow;
  }

  private Panel getHelp(final ExerciseController controller) {
    Nav div = new Nav();
    Dropdown menu = new Dropdown(controller.getGreeting());
    menu.setIcon(IconType.QUESTION_SIGN);
    NavLink help = new NavLink("Help");
    help.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        controller.showFlashHelp();
      }
    });
    menu.add(help);
    NavLink widget = new NavLink("Log Out");
    widget.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        controller.resetState();
      }
    });
    menu.add(widget);
    div.add(menu);
    return div;
  }

  /**
   * Make a row to show the question content (the prompt or stimulus)
   * and the space bar and feedback widgets beneath it.
   *
   * @param e
   * @return
   */
  private Widget getCardPrompt(Exercise e, ExerciseController controller) {
    FluidRow questionRow = new FluidRow();
    Widget questionContent = getQuestionContent(e, controller);
    Column contentContainer = new Column(12, questionContent);
    questionRow.add(contentContainer);
    return questionRow;
  }

  private Widget getQuestionContent(Exercise e, ExerciseController controller) {
    int headingSize = 1;
    String stimulus = e.getEnglishSentence();
    String content = e.getContent();

    if (content == null) {
      content = stimulus;
    } else {
      stimulus = content;
    }

    if (content != null/* && !controller.isFlashCard()*/) {
      System.out.println("\tfor " + e.getID() + " not flashcard");

      Exercise.QAPair qaPair = e.getForeignLanguageQuestions().get(0);
      content = content.replaceAll("<p> &nbsp; </p>", "");
      String splitTerm = LISTEN_TO_THIS_AUDIO;
      String[] split = content.split(splitTerm);
      String prefix = split[0];
      HTML contentPrefix = getHTML(prefix, true, controller);
      contentPrefix.addStyleName("marginRight");

      Panel container = new FlowPanel();
      Heading child = new Heading(5, "Exercise " + e.getID());
      child.addStyleName("leftTenMargin");
      container.add(child);
      container.add(contentPrefix);

      // Todo : this is vulnerable to a variety of issues.
      if (e.getRefAudio() != null && e.getRefAudio().length() > 0) {
        Panel container2 = new FlowPanel();
        HTML prompt = getHTML("<h3 style='margin-right: 30px'>" + splitTerm + "</h3>", true, controller);
        container2.add(prompt);
        SimplePanel simplePanel = new SimplePanel();
        simplePanel.add(getAudioWidget(e));
        container2.add(simplePanel);

        String suffix = split[1];
        HTML contentSuffix = getHTML("<br/>" + // TODO: br is a hack
          "<h3 style='margin-right: 30px'>" + suffix + "</h3>", true, controller);
        contentSuffix.addStyleName("marginRight");
        container2.add(contentSuffix);
        container2.addStyleName("rightAlign");
        container.add(container2);
      }

      container.add(getHTML("<h2 style='margin-right: 30px'>" + qaPair.getQuestion() + "</h2>", true, controller));

      return container;
    } else {
      Widget hero = new Heading(headingSize, stimulus);
      hero.addStyleName("marginRight");
      return hero;
    }
  }

  private Widget getAudioWidget(Exercise e) {
    String refAudio = e.getRefAudio();
    String type = refAudio.substring(refAudio.length() - 3);

    final Audio audio = getAudioNoFocus(refAudio, type);
    audio.addStyleName("floatRight");
    audio.addStyleName("rightMargin");

    return audio;
  }

  private Audio getAudioNoFocus(String refAudio, String type) {
    final Audio audio = Audio.createIfSupported();
    audio.setControls(true);
    audio.addSource(refAudio, "audio/" + type);
    audio.addSource(refAudio.replace(".mp3", ".ogg"), "audio/ogg");
    audio.addFocusHandler(new FocusHandler() {
      @Override
      public void onFocus(FocusEvent event) {
        audio.setFocus(false);
      }
    });
    return audio;
  }

  private HTML getHTML(String content, boolean requireAlignment, ExerciseController controller) {
    boolean rightAlignContent = controller.isRightAlignContent();
    HasDirection.Direction direction =
      requireAlignment && rightAlignContent ? HasDirection.Direction.RTL : WordCountDirectionEstimator.get().estimateDirection(content);

    HTML html = new HTML(content, direction);
    html.setWidth("100%");
    if (requireAlignment && rightAlignContent) {
      html.addStyleName("rightAlign");
    }

    html.addStyleName("wrapword");
    return html;
  }

  /**
   * Three rows below the stimulus word/expression:<p></p>
   * record space bar image <br></br>
   * reco feedback - whether the recorded audio was correct/incorrect, etc.  <br></br>
   * score feedback - pron score
   *
   * @param e
   * @param service
   * @param controller used in subclasses for audio control
   */
  private void addRecordingAndFeedbackWidgets(Exercise e, LangTestDatabaseAsync service, ExerciseController controller) {
    // add answer widget to do the recording
    MyRecordButtonPanel answerWidget = getAnswerWidget(e, service, controller, 1);
    this.answerWidgets.add(answerWidget);

    FluidRow recordButtonRow = getRecordButtonRow(answerWidget.getRecordButton());
    add(recordButtonRow);

    FluidRow recoOutputRow = getRecoOutputRow();
    add(recoOutputRow);

    FluidRow feedbackRow = getScoreFeedbackRow();
    add(feedbackRow);
  }

  /**
   * Center align the record button image.
   *
   * @param recordButton
   * @return
   */
  private FluidRow getRecordButtonRow(Widget recordButton) {
    FluidRow recordButtonRow = new FluidRow();
    Paragraph recordButtonContainer = new Paragraph();
    recordButtonContainer.addStyleName("alignCenter");
    recordButtonContainer.add(recordButton);
    recordButton.addStyleName("alignCenter");
    recordButtonRow.add(new Column(12, recordButtonContainer));
    return recordButtonRow;
  }

  /**
   * Center align the text feedback (correct/incorrect)
   *
   * @return
   */
  private FluidRow getRecoOutputRow() {
    FluidRow recoOutputRow = new FluidRow();
    Paragraph paragraph2 = new Paragraph();
    paragraph2.addStyleName("alignCenter");

    recoOutputRow.add(new Column(12, paragraph2));
    recoOutput = new Heading(3, "Answer");
    recoOutput.addStyleName("cardHiddenText");   // same color as background so text takes up space but is invisible
    DOM.setStyleAttribute(recoOutput.getElement(), "color", "#ebebec");

    paragraph2.add(recoOutput);
    return recoOutputRow;
  }

  /**
   * Holds the pron score feedback.
   * Initially made with a placeholder.
   *
   * @return
   */
  private FluidRow getScoreFeedbackRow() {
    FluidRow feedbackRow = new FluidRow();
    SimplePanel widgets = new SimplePanel();
    widgets.setHeight("40px");
    scoreFeedbackColumn = new Column(6, 3, widgets);
    feedbackRow.add(scoreFeedbackColumn);
    return feedbackRow;
  }

  protected MyRecordButtonPanel getAnswerWidget(final Exercise exercise, LangTestDatabaseAsync service,
                                                ExerciseController controller, final int index) {
    return new MyRecordButtonPanel(service, controller, exercise, index);
  }

  /**
   * Not sure if this is actually necessary -- this is part of who gets the focus when the flashcard is inside
   * an internal frame in a dialog.
   *
   * @see BootstrapFlashcardExerciseList#grabFocus(BootstrapExercisePanel)
   */
  public void grabFocus() {
    for (MyRecordButtonPanel widget : answerWidgets) {
      widget.getRecordButton().setFocus(true);
    }
  }

  private class MyRecordButtonPanel extends RecordButtonPanel {
    private final Exercise exercise;

    public MyRecordButtonPanel(LangTestDatabaseAsync service, ExerciseController controller, Exercise exercise, int index) {
      super(service, controller, exercise, null, index);
      this.exercise = exercise;
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
                                                         showPopup("Press and hold space bar to begin recording, release to stop.");
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
    protected void layoutRecordButton() {
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

      return recordButton;
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

      onUnload();
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
     * @see mitll.langtest.client.recorder.RecordButtonPanel#stopRecording()
     */
    @Override
    protected void receivedAudioAnswer(final AudioAnswer result, ExerciseQuestionState questionState, Panel outer) {
      boolean correct = result.isCorrect();
      final double score = result.getScore();

      recordButton.setResource(correct ? correctImage : incorrectImage);
      recordButton.setHeight("112px");

      String path = exercise.getRefAudio() != null ? exercise.getRefAudio() : exercise.getSlowAudioRef();
      final boolean hasRefAudio = path != null;
      System.out.println("answer correct = " + correct + " pron score : " + score + " has ref " + hasRefAudio);

      String feedback = "";
      if (result.validity != AudioAnswer.Validity.OK) {
        showPopup(result.validity.getPrompt());
        if (NEXT_ON_BAD_AUDIO) nextAfterDelay(correct, "");
      } else if (correct) {
        showCorrectFeedback(score);
      } else {   // incorrect!!
        if (hasRefAudio) {
          ensureMP3(result, score, path, hasRefAudio);
        } else {
          feedback = showIncorrectFeedback(result, score, hasRefAudio);
        }
      }
      if (correct || !hasRefAudio) {
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
      showPronScoreFeedback(score);

      soundFeedback.playCorrect();
      if (feedback < MAX_INTRO_FEEBACK_COUNT) {
        String correctPrompt = "Correct! It's: " + exercise.getRefSentence();
        recoOutput.setText(correctPrompt);
        DOM.setStyleAttribute(recoOutput.getElement(), "color", "#000000");
        incrCookie(FEEDBACK_TIMES_SHOWN);
      }
    }

    private List<String> toPlay = new ArrayList<String>();

    /**
     * If there's reference audio, play it and wait for it to finish.
     *
     * @param result
     * @param score
     * @param hasRefAudio
     * @see #receivedAudioAnswer(mitll.langtest.shared.AudioAnswer, mitll.langtest.client.exercise.ExerciseQuestionState, com.google.gwt.user.client.ui.Panel)
     */
    private String showIncorrectFeedback(AudioAnswer result, double score, boolean hasRefAudio) {
      showPronScoreFeedback(score);
      boolean hasSynonymAudio = !exercise.getSynonymAudioRefs().isEmpty();
      System.out.println("showIncorrectFeedback : playing " + toPlay + " result " + result + " score " + score + " has ref " + hasRefAudio +
        " hasSynonymAudio " + hasSynonymAudio);

      String correctPrompt = getCorrectDisplay();
      if (hasRefAudio) {
        if (hasSynonymAudio) {
          toPlay.addAll(exercise.getSynonymAudioRefs());
          System.out.println("showIncorrectFeedback : playing " + toPlay);
          playAllAudio(correctPrompt);
        } else {
          String path = exercise.getRefAudio();
          if (path == null) path = exercise.getSlowAudioRef(); // fall back to slow audio
          if (path == null) {
            soundFeedback.playIncorrect(); // this should never happen
          } else {
            path = (path.endsWith(WAV)) ? path.replace(WAV, MP3) : path;
            final String fcorrectPrompt = correctPrompt;

            soundFeedback.createSound(path, new SoundFeedback.EndListener() {
              @Override
              public void songEnded() {
                goToNextItem(fcorrectPrompt);
              }
            });
          }
        }
      } else {
        soundFeedback.playIncorrect();
      }

      if (isDemoMode) {
        correctPrompt = "Heard: " + result.decodeOutput + "<p>" + correctPrompt;
      }
      recoOutput.setText(correctPrompt);
      DOM.setStyleAttribute(recoOutput.getElement(), "color", "#000000");
      return correctPrompt;
    }

    private void playAllAudio(final String infoToShow) {
      String path = toPlay.get(0);
      path = (path.endsWith(WAV)) ? path.replace(WAV, MP3) : path;

      System.out.println("playAllAudio : " + toPlay.size() + " playing " + path);
      soundFeedback.createSound(path, new SoundFeedback.EndListener() {
        @Override
        public void songEnded() {
          toPlay.remove(0);

          System.out.println("playAllAudio : songEnded " + toPlay.size() + " items left.");

          if (!toPlay.isEmpty()) {
            playAllAudio(infoToShow);
          } else {
            goToNextItem(infoToShow);
          }
        }
      });
    }

    private void goToNextItem(String infoToShow) {
      Timer t = new Timer() {
        @Override
        public void run() {
          controller.loadNextExercise(exercise);
        }
      };
      int delay = getFeedbackLengthProportionalDelay(infoToShow);
      System.out.println("goToNextItem : using delay " + delay);
      t.schedule(isDemoMode ? LONG_DELAY_MILLIS : delay);
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

    private void nextAfterDelay(boolean correct, String feedback) {
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
    }

    @Override
    protected void receivedAudioFailure() {
      recordButton.setResource(enterImage);
    }
  }

  private int getFeedbackLengthProportionalDelay(String feedback) {
    int mult1 = feedback.length() / DELAY_CHARACTERS;
    int mult = Math.max(1, mult1);
    return mult * BootstrapExercisePanel.DELAY_MILLIS;
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

  /**
   * Show progress bar with score percentage, colored by score.
   * Note it has to be wide enough to hold the text "pronunciation score xxx %"
   *
   * @param score
   */
  private void showPronScoreFeedback(double score) {
    if (score < 0) score = 0;
    double percent = 100 * score;

    scoreFeedbackColumn.clear();
    scoreFeedbackColumn.add(scoreFeedback);

    int percent1 = (int) percent;
    scoreFeedback.setPercent(percent1 < 40 ? 40 : percent1);   // just so the words will show up

    scoreFeedback.setText(PRONUNCIATION_SCORE + (int) percent + "%");
    scoreFeedback.setVisible(true);
    scoreFeedback.setColor(
      score > 0.8 ? ProgressBarBase.Color.SUCCESS :
        score > 0.6 ? ProgressBarBase.Color.DEFAULT :
          score > 0.4 ? ProgressBarBase.Color.WARNING : ProgressBarBase.Color.DANGER);
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
    t.schedule(HIDE_DELAY);
  }

  @Override
  protected void onUnload() {
    for (MyRecordButtonPanel answers : answerWidgets) {
      answers.onUnload();
    }
  }
}
