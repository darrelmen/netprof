package mitll.langtest.client.bootstrap;

import com.github.gwtbootstrap.client.ui.Column;
import com.github.gwtbootstrap.client.ui.FluidContainer;
import com.github.gwtbootstrap.client.ui.FluidRow;
import com.github.gwtbootstrap.client.ui.Heading;
import com.github.gwtbootstrap.client.ui.Image;
import com.github.gwtbootstrap.client.ui.Paragraph;
import com.github.gwtbootstrap.client.ui.ProgressBar;
import com.github.gwtbootstrap.client.ui.base.ProgressBarBase;
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
import com.google.gwt.safehtml.shared.UriUtils;
import com.google.gwt.storage.client.Storage;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.DecoratedPopupPanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.LangTest;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.ExerciseQuestionState;
import mitll.langtest.client.list.ListInterface;
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
  private static final int DELAY_MILLIS = 1200;//1250;
  private static final int DELAY_MILLIS_LONG = 3000;
  public static final String PRONUNCIATION_SCORE = "Pronunciation score ";
  private Column scoreFeedbackColumn;
  private static final String FEEDBACK_TIMES_SHOWN = "FeedbackTimesShown";
  public static final int DELAY_MILLIS1 = 300;
  private static final int PERIOD_MILLIS = DELAY_MILLIS1;
  private static final int MAX_INTRO_FEEBACK_COUNT = 5;
  private static final int KEY_PRESS = 256;
  private static final int KEY_UP = 512;
  private static final int SPACE_CHAR = 32;
  private static final int HIDE_DELAY = 2500;
  private static final String WAV = ".wav";
  private static final String MP3 = ".mp3";

  private List<MyRecordButtonPanel> answerWidgets = new ArrayList<MyRecordButtonPanel>();
  private Storage stockStore = null;

  private Image waitingForResponseImage = new Image(UriUtils.fromSafeConstant(LangTest.LANGTEST_IMAGES + "animated_progress48.gif"));
  private Image recordImage1 = new Image(UriUtils.fromSafeConstant(LangTest.LANGTEST_IMAGES + "media-record-3.png"));
  private Image recordImage2 = new Image(UriUtils.fromSafeConstant(LangTest.LANGTEST_IMAGES + "media-record-4.png"));
  public Image correctImage = new Image(UriUtils.fromSafeConstant(LangTest.LANGTEST_IMAGES + "checkmark48.png"));
  public Image incorrectImage = new Image(UriUtils.fromSafeConstant(LangTest.LANGTEST_IMAGES + "redx48.png"));
  private Image enterImage = new Image(UriUtils.fromSafeConstant(LangTest.LANGTEST_IMAGES + "blueSpaceBar.png"));//"space48_2.png"));
  private static final String HELP_IMAGE = LangTest.LANGTEST_IMAGES + "/helpIconBlue.png";
  public static final String WARN_NO_FLASH = "<font color='red'>Flash is not activated. Do you have a flashblocker? Please add this site to its whitelist.</font>";

  private Heading recoOutput;
  private boolean keyIsDown;
  private boolean isDemoMode;
  private int feedback = 0;
  private Timer t;
  private ProgressBar scoreFeedback = new ProgressBar();
  private SoundFeedback soundFeedback;
  private ListInterface exerciseList;

  /**
   *
   * @param e
   * @param service
   * @param controller
   * @param exerciseList
   * @param addKeyBinding
   * @see mitll.langtest.client.flashcard.FlashcardExercisePanelFactory#getExercisePanel(mitll.langtest.shared.Exercise)
   */
  public BootstrapExercisePanel(final Exercise e, final LangTestDatabaseAsync service,
                                final ExerciseController controller, ListInterface exerciseList, boolean addKeyBinding) {
    setStyleName("exerciseBackground");
    addStyleName("cardBorder");
    this.exerciseList = exerciseList;
    stockStore = Storage.getLocalStorageIfSupported();
    if (stockStore != null) {
      feedback = getCookieValue(FEEDBACK_TIMES_SHOWN);
    }
    isDemoMode = controller.isDemoMode();
    HTML warnNoFlash = new HTML(WARN_NO_FLASH);
    soundFeedback = new SoundFeedback(controller.getSoundManager(), warnNoFlash);

    add(getHelpRow(controller));
    System.out.println("got here : BootstrapExercisePanel " + this);
  //  new Exception().printStackTrace();;
    // try {
    //  add(new HTML("Got here"));
    add(getCardPrompt(e, controller));
 /*   } catch (Exception e1) {
      Window.alert("got " +e1);
    }*/

    addRecordingAndFeedbackWidgets(e, service, controller, addKeyBinding);
    warnNoFlash.setVisible(false);
    add(warnNoFlash);
  }

  @Override
  public void onBrowserEvent(Event event) {
    super.onBrowserEvent(event);

    System.out.println("onBrowserEvent : got event " + event + " : " + DOM.eventGetType(event));
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
    helpRow.add(getHelpImage(controller));
    return helpRow;
  }

  private Image getHelpImage(final ExerciseController controller) {
    Image image = new Image(HELP_IMAGE);

    image.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        controller.showFlashHelp();
      }
    });
    return image;
  }

  /**
   * Make a row to show the question content (the prompt or stimulus)
   * and the space bar and feedback widgets beneath it.
   *
   * @param e
   * @return
   */
  protected FlowPanel getCardPrompt(Exercise e, ExerciseController controller) {
    FluidRow questionRow = new FluidRow();
    Widget questionContent = getQuestionContent(e, controller);
    Column contentContainer = new Column(12, questionContent);
    questionRow.add(contentContainer);
    return questionRow;
  }

  private Widget getQuestionContent(Exercise e, ExerciseController controller) {
    int headingSize = 1;
    String stimulus = e.getEnglishSentence();

    if (stimulus == null) {
      System.err.println("huh? exer " +e + " has null english???");
    }
    //    String markup = "<p>\\W&nbsp; </p>";
    //    Window.alert("doing getQuestionContent");
    //String stimulus;
    String content = getContentFromExercise(e);
    if (content == null) {
      //   content = stimulus;
      Widget hero = new Heading(headingSize, stimulus);
      hero.addStyleName("cardText");
      //   if (true) throw new Exception("Testin!");
      return hero;
    } else {
      stimulus = content;
      //   System.err.println("before " + content);
      //   content = content.replaceAll("<p>","<br>");
      //   content = content.replaceAll("</p>","</br>");
      // if (content.contains(markup)) {
      //  System.out.println("getQuestionContent : removing " + markup);
      //   content = content.replaceAll(markup, "<br></br>");
      // }
    }

    //System.out.println("stim " + stimulus);
    //stimulus += "nullStim_Initially";
    //  headingSize = 4;
    //  String content = e.getContent();
    if (content != null) {
   /*   if (content.contains(markup)) {
        System.out.println("getQuestionContent : removing " + markup);
        content = content.replaceAll(markup, "<br></br>");
      }*/
      if (content.contains("<p>")) {
        Exercise.QAPair qaPair = e.getForeignLanguageQuestions().get(0);
        String questionText = "<br>" +
          qaPair.getQuestion();
        stimulus = "<h1>" +
          //  "Debug baby!"+
          content + questionText +
          "</h1>";
      }
      HTML html = getHTML(stimulus, true, controller);
      html.addStyleName("cardText");
      html.addStyleName("marginRight");

      return html;
    }

    // if (stimulus == null) stimulus = "Blank for exercise #" +e.getID();

    // System.out.println("Heading size " + headingSize);

    Widget hero = new Heading(headingSize, stimulus);
    hero.addStyleName("cardText");
    //   if (true) throw new Exception("Testin!");
    return hero;
  }

  protected String getContentFromExercise(Exercise e) {
    return e.getContent();
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
  /*  if (getLanguage().equalsIgnoreCase("Pashto")) {
      html.addStyleName("pashtofont");
    }
    else {
      html.addStyleName("xlargeFont");
    }*/
    html.addStyleName("xlargeFont");

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
   * @param addKeyBinding
   */
  private void addRecordingAndFeedbackWidgets(Exercise e, LangTestDatabaseAsync service, ExerciseController controller,
                                              boolean addKeyBinding) {
    // add answer widget to do the recording
    MyRecordButtonPanel answerWidget = getAnswerWidget(e, service, controller, 1, addKeyBinding);
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

  private MyRecordButtonPanel getAnswerWidget(final Exercise exercise, LangTestDatabaseAsync service,
                                              ExerciseController controller, final int index, boolean addKeyBinding) {
    return new MyRecordButtonPanel(service, controller, exercise, index, true, addKeyBinding);
  }

  /**
   * Not sure if this is actually necessary -- this is part of who gets the focus when the flashcard is inside
   * an internal frame in a dialog.
   *
   * @seex BootstrapFlashcardExerciseList#grabFocus(BootstrapExercisePanel)
   */
  private void grabFocus() {
    for (MyRecordButtonPanel widget : answerWidgets) {
      //System.out.println("Grab focus!!!");
      widget.getRecordButton().setFocus(true);
    }
  }

  private class MyRecordButtonPanel extends RecordButtonPanel {
    private final Exercise exercise;

    public MyRecordButtonPanel(LangTestDatabaseAsync service, ExerciseController controller, Exercise exercise,
                               int index, boolean doFlashcardAudio, boolean addKeyBinding) {
      super(service, controller, exercise, null, index, doFlashcardAudio, addKeyBinding);
      this.exercise = exercise;
    }

    /**
     * @see
     * @param controller
     * @param outer
     * @param addKeyHandler
     * @return
     */
    @Override
    protected RecordButton makeRecordButton(ExerciseController controller, final RecordButtonPanel outer, boolean addKeyHandler) {
      return new RecordButton(controller.getRecordTimeout(), addKeyHandler) {
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

        /**
         * @see RecordButton#RecordButton(int, boolean)
         * @return
         */
        @Override
        public HandlerRegistration addKeyHandler() {
          keyHandler = Event.addNativePreviewHandler(new
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
          System.out.println(new java.util.Date() + " : key handler " + keyHandler);
          return keyHandler;
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
      System.out.println("receivedAudioAnswer : answer correct = " + correct + " pron score : " + score + " has ref audio " + hasRefAudio);

      if (result.validity != AudioAnswer.Validity.OK) {
        showPopup(result.validity.getPrompt());
        nextAfterDelay(correct);
      } else if (correct) {
        showCorrectFeedback(score);
      } else {   // incorrect!!
   /*     if (hasRefAudio) {
          ensureMP3(result, score, path, hasRefAudio);
        } else {*/
          showIncorrectFeedback(result, score, hasRefAudio);
     //   }
      }
      if (correct || !hasRefAudio) {
        nextAfterDelay(correct);
      }
    }

    /**
     * Make sure all the mp3s we may play exist on the server.
     *
     * @paramx result
     * @paramx score
     * @paramx path
     * @paramx hasRefAudio
     */
/*    private void ensureMP3(final AudioAnswer result, final double score, String path, final boolean hasRefAudio) {
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
    }*/

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
     */
    private void showIncorrectFeedback(AudioAnswer result, double score, boolean hasRefAudio) {
      showPronScoreFeedback(score);

      boolean hasSynonymAudio = !exercise.getSynonymAudioRefs().isEmpty();
      if (hasRefAudio) {
        if (hasSynonymAudio) {
          toPlay.addAll(exercise.getSynonymAudioRefs());
          System.out.println("showIncorrectFeedback : playing " + toPlay);
          playAllAudio();
        } else {
          String path = exercise.getRefAudio();
          path = (path.endsWith(WAV)) ? path.replace(WAV, MP3) : path;

          System.out.println("showIncorrectFeedback : playing " + path);

          soundFeedback.createSound(path, new SoundFeedback.EndListener() {
            @Override
            public void songEnded() {
              goToNextItem();
            }
          });
        }
      } else {
        soundFeedback.playIncorrect();
      }
      String correctPrompt = getCorrectDisplay();

      if (isDemoMode) {
        correctPrompt = "Heard: " + result.decodeOutput + "<p>" + correctPrompt;
      }
      recoOutput.setText(correctPrompt);
      DOM.setStyleAttribute(recoOutput.getElement(), "color", "#000000");
    }

    private void playAllAudio() {
      String path = toPlay.get(0);
      path = (path.endsWith(WAV)) ? path.replace(WAV, MP3) : path;

      System.out.println("playAllAudio : " + toPlay.size() + " playing " + path);
      soundFeedback.createSound(path, new SoundFeedback.EndListener() {
        @Override
        public void songEnded() {
          toPlay.remove(0);

          System.out.println("playAllAudio : songEnded " + toPlay.size() + " items left.");

          if (!toPlay.isEmpty()) {
            playAllAudio();
          } else {
            goToNextItem();
          }
        }
      });
    }

    private void goToNextItem() {
      Timer t = new Timer() {
        @Override
        public void run() {
          exerciseList.loadNextExercise(exercise);
        }
      };
      t.schedule(isDemoMode ? LONG_DELAY_MILLIS : DELAY_MILLIS);
    }

    private String getCorrectDisplay() {
      String refSentence = exercise.getRefSentence();
      String translit = exercise.getTranslitSentence().length() > 0 ? " (" + exercise.getTranslitSentence() + ")" : "";

      if (refSentence == null || refSentence.length() == 0) {
        List<Exercise.QAPair> questions = exercise.getForeignLanguageQuestions();
        refSentence = getAltAnswers(questions);
        List<Exercise.QAPair> eq = exercise.getEnglishQuestions();
        translit = " (" + getAltAnswers(eq) + " )";
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

    private void nextAfterDelay(boolean correct) {
      // Schedule the timer to run once in 1 seconds.
      System.out.println("doing nextAfterDelay " + correct);
      Timer t = new Timer() {
        @Override
        public void run() {
          exerciseList.loadNextExercise(exercise);
        }
      };
      t.schedule(isDemoMode ? LONG_DELAY_MILLIS : correct ? DELAY_MILLIS : DELAY_MILLIS_LONG);
    }

    @Override
    protected void receivedAudioFailure() {
      recordButton.setResource(enterImage);
    }
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
  public void onUnload() {
    for (MyRecordButtonPanel answers : answerWidgets) {
      answers.onUnload();
    }
  }

  public void addKeyHandler() {
    for (MyRecordButtonPanel answers : answerWidgets) {
      System.out.println("addKeyHandler : avp adding key handler : " + answers);

      answers.addKeyHandler();
    }
  }

  public void removeKeyHandler() {
    for (MyRecordButtonPanel answers : answerWidgets) {
      answers.removeKeyHandler();
    }
  }
}
