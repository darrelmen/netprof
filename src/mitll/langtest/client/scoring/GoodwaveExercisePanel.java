package mitll.langtest.client.scoring;

import com.github.gwtbootstrap.client.ui.Image;
import com.github.gwtbootstrap.client.ui.Label;
import com.github.gwtbootstrap.client.ui.RadioButton;
import com.github.gwtbootstrap.client.ui.Tooltip;
import com.github.gwtbootstrap.client.ui.constants.Placement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.safehtml.shared.UriUtils;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.CaptionPanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.InlineHTML;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.ProvidesResize;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.LangTest;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.exercise.BusyPanel;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.NavigationHelper;
import mitll.langtest.client.exercise.PostAnswerProvider;
import mitll.langtest.client.gauge.ASRScorePanel;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.client.sound.PlayAudioPanel;
import mitll.langtest.client.sound.PlayListener;
import mitll.langtest.client.sound.SoundManagerAPI;
import mitll.langtest.shared.AudioAnswer;
import mitll.langtest.shared.AudioAttribute;
import mitll.langtest.shared.Exercise;

import java.util.Collection;
import java.util.Date;

/**
 * Mainly delegates recording to the {@link mitll.langtest.client.recorder.SimpleRecordPanel}.
 * User: GO22670
 * Date: 5/11/12
 * Time: 11:51 AM
 * To change this template use File | Settings | File Templates.
 */
public class GoodwaveExercisePanel extends HorizontalPanel implements BusyPanel, RequiresResize, ProvidesResize {
  protected static final String NATIVE_REFERENCE_SPEAKER = "Native Reference Speaker";
  private static final String USER_RECORDER = "User Recorder";
  private static final boolean SHOW_SPECTROGRAM = false;
  private boolean isBusy = false;

  private static final String WAV = ".wav";
  private static final String MP3 = ".mp3";
  private Image recordImage1;
  private Image recordImage2;
  private String refAudio;

  protected final Exercise exercise;
  protected final ExerciseController controller;
  protected final LangTestDatabaseAsync service;
  protected ScoreListener scorePanel;
  private AudioPanel contentAudio, answerAudio;
  private NavigationHelper navigationHelper;
  private final float screenPortion;
  String instance;

  /**
   * Has a left side -- the question content (Instructions and audio panel (play button, waveform)) <br></br>
   * and a right side -- the charts and gauges {@link ASRScorePanel}
   *
   * @param e             for this exercise
   * @param controller
   * @param listContainer
   * @param screenPortion
   * @param instance
   * @see mitll.langtest.client.scoring.GoodwaveExercisePanelFactory#getExercisePanel(mitll.langtest.shared.Exercise)
   */
  public GoodwaveExercisePanel(final Exercise e, final ExerciseController controller, final ListInterface listContainer,
                               float screenPortion, boolean addKeyHandler, String instance) {
    this.exercise = e;
    this.controller = controller;
    this.service = controller.getService();
    this.screenPortion = screenPortion;
    this.instance = instance;
    setWidth("100%");
    addStyleName("inlineBlockStyle");
    getElement().setId("GoodwaveExercisePanel");
    final Panel center = new FlowPanel();
    center.addStyleName("blockStyle");
    center.getElement().setId("GoodwaveVerticalCenter");
    center.addStyleName("floatLeft");
    // attempt to left justify

    ASRScorePanel widgets = makeScorePanel(e, instance);

/*    HorizontalPanel hp = new HorizontalPanel();
    hp.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_LEFT);
    hp.getElement().setId("questionContentRowContainer");*/

    addQuestionContentRow(e, controller, center);

   // center.add(hp);

    // content is on the left side
    add(center);

    // score panel with gauge is on the right
    if (e.isRepeat() && widgets != null) {
      add(widgets);
    }
    addUserRecorder(service, controller, center, screenPortion); // todo : revisit screen portion...

    this.navigationHelper = new NavigationHelper(exercise, controller, new PostAnswerProvider() {
      @Override
      public void postAnswers(ExerciseController controller, Exercise completedExercise) {
        nextWasPressed(listContainer, completedExercise);
      }
    }, listContainer, true, addKeyHandler);
    navigationHelper.addStyleName("topBarMargin");
   // center.add(navigationHelper.makeSpacer());
    center.add(navigationHelper);
  }

  public void wasRevealed() {}

  protected ASRScorePanel makeScorePanel(Exercise e, String instance) {
    ASRScorePanel widgets = null;
    if (e.isRepeat()) {
      widgets = new ASRScorePanel("GoodwaveExercisePanel_"+instance);
      scorePanel = widgets;
    }
    return widgets;
  }

  protected void nextWasPressed(ListInterface listContainer, Exercise completedExercise) {
    listContainer.loadNextExercise(completedExercise);
  }

  protected void addQuestionContentRow(Exercise e, ExerciseController controller, Panel hp) {
     hp.add(getQuestionContent(e, (Panel) null));
  }

  public void setBusy(boolean v) {  this.isBusy = v;  }

  /**
   * For every question,
   * <ul>
   * <li>show the text of the question,  </li>
   * <li>the prompt to the test taker (e.g "Speak your response in English")  </li>
   * <li>an answer widget (either a simple text box, an flash audio record and playback widget, or a list of the answers, when grading </li>
   * </ul>     <br></br>
   * Remember the answer widgets so we can notice which have been answered, and then know when to enable the next button.
   *
   * @param service
   * @param controller    used in subclasses for audio control
   * @paramx i
   * @param screenPortion
   */
  protected void addUserRecorder(LangTestDatabaseAsync service, ExerciseController controller, Panel toAddTo, float screenPortion) {
    Widget answerWidget = getAnswerWidget(service, controller, 1, screenPortion);

    ResizableCaptionPanel cp = new ResizableCaptionPanel(USER_RECORDER);
    cp.setContentWidget(answerWidget);
    toAddTo.add(cp);
  }

  public void onResize() {
    //System.out.println("GoodwaveExercisePanel : got resize " + getOffsetWidth());
    if (contentAudio != null) contentAudio.onResize();
    if (answerAudio != null) answerAudio.onResize();
  }

  /**
   * Show the instructions and the audio panel.<br></br>
   * <p/>
   * Replace the html 5 audio tag with our fancy waveform widget.
   *
   * @param e for this exercise
   * @return the panel that has the instructions and the audio panel
   * @see #GoodwaveExercisePanel
   */
  protected Widget getQuestionContent(Exercise e, Panel addToList) {
    String content = e.getContent();
    String path = null;
    if (e.isRepeat()) {
      this.refAudio = e.getRefAudio() != null ? e.getRefAudio() : e.getSlowAudioRef();
      path = refAudio;
    } else if (content.contains("audio")) {  // if we don't have proper REPEAT exercises
      int i = content.indexOf("source src=");
      String s = content.substring(i + "source src=".length() + 1).split("\\\"")[0];
      System.err.println("audio path '" + s + "'");
      path = s;

      int start = content.indexOf("<audio");
      int end = content.indexOf("audio>");
      content = content.substring(0, start) + content.substring(end + "audio>".length());
      this.refAudio = path;
    }

    final VerticalPanel vp = new VerticalPanel();
    vp.getElement().setId("getQuestionContent_verticalContainer");
    vp.addStyleName("blockStyle");

    Widget questionContent = getQuestionContent(e, content);


    //Panel rowForContent = new FlowPanel();

    if (addToList != null) {
      Panel rowForContent = new HorizontalPanel();
      rowForContent.setWidth("100%");
      rowForContent.getElement().setId("getQuestionContent_rowForContent");
      //rowForContent.addStyleName("trueInlineStyle");
      rowForContent.add(questionContent);
      rowForContent.add(addToList);
      addToList.addStyleName("floatRight");
      vp.add(rowForContent);
    }
    else {
      vp.add(questionContent);
    }

    Widget scoringAudioPanel = getScoringAudioPanel(e, path);
    SimplePanel div = new SimplePanel(scoringAudioPanel);


    div.addStyleName("trueInlineStyle");

    div.addStyleName("floatLeft");

    vp.add(div);
    return vp;
  }

  protected Widget getQuestionContent(Exercise e,String content) {
    Widget questionContent = new HTML(content);
    questionContent.getElement().setId("QuestionContent");
    questionContent.addStyleName("floatLeft");
    return questionContent;
  }

  /**
   * If the exercise type is {@link Exercise.EXERCISE_TYPE#REPEAT_FAST_SLOW} then we put the fast/slow radio
   * buttons before the play button.
   *
   * @see #getQuestionContent(mitll.langtest.shared.Exercise, Panel)
   * @param e
   * @param path
   * @return
   */
  protected Widget getScoringAudioPanel(final Exercise e, String path) {
    if (path != null) {
      path = wavToMP3(path);
    }
    ASRScoringAudioPanel audioPanel = getAudioPanel(e, path);
    ResizableCaptionPanel cp = new ResizableCaptionPanel(NATIVE_REFERENCE_SPEAKER);
    cp.setContentWidget(getAudioPanelContent(audioPanel,e,path));

    contentAudio = audioPanel;
    contentAudio.setScreenPortion(screenPortion);
    return cp;
  }

  protected Widget getAudioPanelContent(ASRScoringAudioPanel audioPanel, Exercise exercise, String audioRef) {
    return audioPanel;
  }

  protected ASRScoringAudioPanel getAudioPanel(Exercise e, String path) {
    ASRScoringAudioPanel audioPanel;

    //System.out.println("getScoringAudioPanel : score panel " + scorePanel);
    if (e.getType() == Exercise.EXERCISE_TYPE.REPEAT_FAST_SLOW) {
      audioPanel = new FastAndSlowASRScoringAudioPanel(exercise, path, service, controller, scorePanel);
    } else {
      audioPanel = new ASRScoringAudioPanel(path, e.getRefSentence(), service, controller, SHOW_SPECTROGRAM, scorePanel, 23);
    }
    audioPanel.getElement().setId("ASRScoringAudioPanel");
    audioPanel.setRefAudio(path, e.getRefSentence());
    return audioPanel;
  }

  protected String wavToMP3(String path) {
    return (path.endsWith(WAV)) ? path.replace(WAV, MP3) : path;
  }

  protected void addIncorrectComment(final String commentToPost, final String field) {
    System.out.println(new Date() + " : post to server " + exercise.getID() +
      " field " + field + " commentLabel '" + commentToPost + "' is incorrect");
    //  final long then = System.currentTimeMillis();
    String status = "incorrect";
    addAnnotation(field, status, commentToPost);
  }

  protected void addCorrectComment(final String field) {
    System.out.println(new Date() + " : post to server " + exercise.getID() +
      " field " + field + " is correct");
    //  final long then = System.currentTimeMillis();
    String status = "correct";
    addAnnotation(field, status, "");
  }

  private void addAnnotation(final String field, final String status, final String commentToPost) {
    service.addAnnotation(exercise.getID(), field, status, commentToPost, controller.getUser(), new AsyncCallback<Void>() {
      @Override
      public void onFailure(Throwable caught) {
      }

      @Override
      public void onSuccess(Void result) {
        //long now = System.currentTimeMillis();
        System.out.println("\t" + new Date() + " : onSuccess : posted to server " + exercise.getID() +
          " field " + field + " commentLabel " + commentToPost + " is " + status);//, took " + (now - then) + " millis");
      }
    });
  }

  protected Panel getContentWidget(String label, String value, boolean withWrap) {
    Panel nameValueRow = new FlowPanel();
    nameValueRow.getElement().setId("nameValueRow_" + label);
    nameValueRow.addStyleName("Instruction");

    InlineHTML foreignPhrase = new InlineHTML(label);
    foreignPhrase.addStyleName("Instruction-title");
    nameValueRow.add(foreignPhrase);

    InlineHTML englishPhrase = new InlineHTML(value);
    englishPhrase.addStyleName(withWrap ? "Instruction-data-with-wrap" : "Instruction-data");
    nameValueRow.add(englishPhrase);
    englishPhrase.addStyleName("leftFiveMargin");
    return nameValueRow;
  }

  /**
   * @see mitll.langtest.client.custom.QCNPFExercise#populateCommentRow(com.google.gwt.user.client.ui.FocusWidget, boolean, com.google.gwt.user.client.u.Panel)
   * @return
   */
  protected Label getCommentLabel() {
    final Label commentLabel = new Label("comment?");
    DOM.setStyleAttribute(commentLabel.getElement(), "backgroundColor", "#ff0000");
    commentLabel.setVisible(true);
    commentLabel.addStyleName("ImageOverlay");
    return commentLabel;
  }

  /**
   * @see mitll.langtest.client.custom.NPFExercise#makeAddToList(mitll.langtest.shared.Exercise, mitll.langtest.client.exercise.ExerciseController)
   * @param w
   * @param tip
   * @param placement
   * @return
   */
  protected Tooltip createAddTooltip(Widget w, String tip, Placement placement) {
    Tooltip tooltip = new Tooltip();
    tooltip.setWidget(w);
    tooltip.setText(tip);
    tooltip.setAnimation(true);
// As of 4/22 - bootstrap 2.2.1.0 -
// Tooltips have an bug which causes the cursor to
// toggle between finger and normal when show delay
// is configured.

    tooltip.setShowDelay(500);
    tooltip.setHideDelay(500);

    tooltip.setPlacement(placement);
    tooltip.reconfigure();
    return tooltip;
  }

  protected static class ResizableCaptionPanel extends CaptionPanel implements ProvidesResize, RequiresResize {
    public ResizableCaptionPanel(String name) {
      super(name);
    }

    public void onResize() {
      Widget contentWidget = getContentWidget();
      if (contentWidget instanceof RequiresResize) {
        ((RequiresResize) contentWidget).onResize();
      }
    }
  }

  /**
   * Has a answerPanel mark to indicate when the saved audio has been successfully posted to the server.
   *
   * @param service
   * @param controller
   * @param index
   * @param screenPortion
   * @return
   */
  private Widget getAnswerWidget(LangTestDatabaseAsync service, final ExerciseController controller, final int index, float screenPortion) {
    ScoringAudioPanel widgets = new ASRRecordAudioPanel(service, index, controller);
    widgets.addScoreListener(scorePanel);
    answerAudio = widgets;
    answerAudio.setScreenPortion(screenPortion);

    return widgets;
  }

  /**
   * An ASR scoring panel with a record button.
   */
  private class ASRRecordAudioPanel extends ASRScoringAudioPanel {
    private final int index;
    private PostAudioRecordButton postAudioRecordButton;
    private PlayAudioPanel playAudioPanel;

    /**
     * @param service
     * @param index
     * @param controller
     * @see GoodwaveExercisePanel#getAnswerWidget
     */
    public ASRRecordAudioPanel(LangTestDatabaseAsync service, int index, ExerciseController controller) {
      super(service, controller.getSegmentRepeats(),
        // no keyboard
        controller, scorePanel);
      this.index = index;

    }

    /**
     * So here we're trying to make the record and play buttons know about each other
     * to the extent that when we're recording, we can't play audio, and when we're playing
     * audio, we can't record. We also mark the widget as busy so we can't move on to a different exercise.
     *
     * @param toAdd
     * @return
     * @see AudioPanel#getPlayButtons(Widget)
     */
    @Override
    protected PlayAudioPanel makePlayAudioPanel(Widget toAdd) {
      recordImage1 = new Image(UriUtils.fromSafeConstant(LangTest.LANGTEST_IMAGES + "media-record-3_32x32.png"));
      recordImage2 = new Image(UriUtils.fromSafeConstant(LangTest.LANGTEST_IMAGES + "media-record-4_32x32.png"));
      postAudioRecordButton = new MyPostAudioRecordButton(controller);
      DOM.setElementProperty(postAudioRecordButton.getElement(), "margin", "8px");
      playAudioPanel = new MyPlayAudioPanel(recordImage1, recordImage2, soundManager, postAudioRecordButton, GoodwaveExercisePanel.this);
      return playAudioPanel;
    }

    @Override
    protected void onUnload() {
      super.onUnload();
      navigationHelper.removeKeyHandler();
    }

    private class MyPlayAudioPanel extends PlayAudioPanel {
      public MyPlayAudioPanel(Image recordImage1, Image recordImage2, SoundManagerAPI soundManager,
                              final PostAudioRecordButton postAudioRecordButton1, final GoodwaveExercisePanel goodwaveExercisePanel) {
        super(soundManager, new PlayListener() {
          public void playStarted() {
            goodwaveExercisePanel.setBusy(true);
            postAudioRecordButton1.setEnabled(false);
          }

          public void playStopped() {
            goodwaveExercisePanel.setBusy(false);
            postAudioRecordButton1.setEnabled(true);
          }
        });
        add(recordImage1);
        recordImage1.setVisible(false);
        add(recordImage2);
        recordImage2.setVisible(false);
        getElement().setId("GoodwaveExercisePanel_MyPlayAudioPanel");
      }

      @Override
      protected void addButtons() {
        add(postAudioRecordButton);
        postAudioRecordButton.addStyleName("rightFiveMargin");
        super.addButtons();
      }
    }

    private class MyPostAudioRecordButton extends PostAudioRecordButton {
      public MyPostAudioRecordButton(ExerciseController controller) {
        super(exercise, controller, ASRRecordAudioPanel.this.service, ASRRecordAudioPanel.this.index, true);
      }

      @Override
      public void useResult(AudioAnswer result) {
        setRefAudio(refAudio, exercise.getRefSentence());
        setResultID(result.getResultID());
        getImagesForPath(result.path);
      }

      @Override
      public void startRecording() {
        playAudioPanel.setPlayEnabled(false);
        isBusy = true;
        super.startRecording();
        recordImage1.setVisible(true);
      }

      @Override
      public void stopRecording() {
        playAudioPanel.setPlayEnabled(true);
        isBusy = false;
        super.stopRecording();
        recordImage1.setVisible(false);
        recordImage2.setVisible(false);

      }

      @Override
      public void flip(boolean first) {
        recordImage1.setVisible(first);
        recordImage2.setVisible(!first);
      }

      /**
       * @param result
       * @see mitll.langtest.client.scoring.PostAudioRecordButton#stopRecording()
       */
      @Override
      protected void useInvalidResult(AudioAnswer result) {}
    }
  }

  public boolean isBusy() { return isBusy;  }

  protected void setAudioRef(String audioRef) {}

  class FastAndSlowASRScoringAudioPanel extends ASRScoringAudioPanel {
    private static final String GROUP = "group";
    /**
     * @param exercise
     * @param path
     * @param service
     * @see mitll.langtest.client.scoring.GoodwaveExercisePanel#getScoringAudioPanel(mitll.langtest.shared.Exercise, String)
     */
    public FastAndSlowASRScoringAudioPanel(Exercise exercise,
                                           String path, LangTestDatabaseAsync service, ExerciseController controller1,
                                           ScoreListener scoreListener) {
      super(path,
        exercise.getRefSentence(),
        service,
        controller1,
        SHOW_SPECTROGRAM, scoreListener, 23);
    }

    /**
     * Add radio button choices to control which audio cut is chosen/gets played.
     *
     * @return
     * @see mitll.langtest.client.scoring.AudioPanel#addWidgets(String)
     */
    @Override
    protected Widget getBeforePlayWidget() {
      VerticalPanel vp = new VerticalPanel();

      Collection<AudioAttribute> audioAttributes = exercise.getAudioAttributes();
      RadioButton first = null;

      //System.out.println("getBeforePlayWidget : for " + audioPath + "Attributes were " + audioAttributes);
      RadioButton regular = null;
      for (final AudioAttribute audioAttribute : audioAttributes) {
        RadioButton fast = new RadioButton(GROUP + "_" + audioPath + "_"+instance, audioAttribute.getDisplay());
        if (audioAttribute.isRegularSpeed()) {
          regular = fast;
        }
        if (first == null) {
          first = fast;
        }
        vp.add(fast);

        fast.addClickHandler(new ClickHandler() {
          @Override
          public void onClick(ClickEvent event) {
            showAudio(audioAttribute);
          }
        });
      }

      if (regular != null) {
        regular.setValue(true);
        //System.out.println("selecting regular speed ");

      } else if (first != null) {
        first.setValue(true);
        //System.out.println("selecting first ");

      } else {
        System.err.println("no radio choice got selected??? ");
      }
      if (audioAttributes.isEmpty()) {
        vp.add(new Label("No reference audio."));
      }

      HorizontalPanel hp = new HorizontalPanel();
      hp.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_LEFT);
      hp.add(vp);
      return vp;
    }

    private void showAudio(AudioAttribute audioAttribute) {
      doPause();    // if the audio is playing, stop it
      String audioRef = audioAttribute.getAudioRef();
      setRefAudio(audioRef);
      getImagesForPath(audioRef);

      setAudioRef(audioRef);
    }
  }
}
