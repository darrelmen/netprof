package mitll.langtest.client.scoring;

import com.github.gwtbootstrap.client.ui.Image;
import com.github.gwtbootstrap.client.ui.NavLink;
import com.github.gwtbootstrap.client.ui.RadioButton;
import com.github.gwtbootstrap.client.ui.SplitDropdownButton;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.safehtml.shared.UriUtils;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.CaptionPanel;
import com.google.gwt.user.client.ui.DecoratedPopupPanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.PopupPanel;
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
import mitll.langtest.shared.Exercise;
import mitll.langtest.shared.custom.UserExercise;
import mitll.langtest.shared.custom.UserList;

import java.util.Collection;
import java.util.List;

/**
 * Mainly delegates recording to the {@link mitll.langtest.client.recorder.SimpleRecordPanel}.
 * User: GO22670
 * Date: 5/11/12
 * Time: 11:51 AM
 * To change this template use File | Settings | File Templates.
 */
public class GoodwaveExercisePanel extends HorizontalPanel implements BusyPanel, RequiresResize, ProvidesResize {
  private static final String NATIVE_REFERENCE_SPEAKER = "Native Reference Speaker";
  private static final String USER_RECORDER = "User Recorder";
  //private static final String INSTRUCTIONS = "Instructions";
  private boolean isBusy = false;
  private static final String FAST = "Regular";
  private static final String SLOW = "Slow";

  private static final String WAV = ".wav";
  private static final String MP3 = ".mp3";
  private Image recordImage1;
  private Image recordImage2;

  /**
   * ??? Just for backward compatibility -- so we can run against old plan files
   */
  private String refAudio;

  private Exercise exercise = null;
  private final ExerciseController controller;
  private final LangTestDatabaseAsync service;
  private ScoreListener scorePanel;
  private AudioPanel contentAudio, answerAudio;
  private NavigationHelper navigationHelper;
  private SplitDropdownButton addToList;
  private int activeCount = 0;
  float screenPortion;

  /**
   * Has a left side -- the question content (Instructions and audio panel (play button, waveform)) <br></br>
   * and a right side -- the charts and gauges {@link ASRScorePanel}
   *
   * @param e             for this exercise
   * @param controller
   * @param listContainer
   * @param screenPortion
   * @see mitll.langtest.client.scoring.GoodwaveExercisePanelFactory#getExercisePanel(mitll.langtest.shared.Exercise)
   */
  public GoodwaveExercisePanel(final Exercise e, final ExerciseController controller, final ListInterface listContainer, float screenPortion) {
    this.exercise = e;
    this.controller = controller;
    this.service = controller.getService();
    this.screenPortion = screenPortion;

    addStyleName("inlineBlockStyle");
    getElement().setId("GoodwaveExercisePanel");
    final Panel center = new FlowPanel();
    center.addStyleName("blockStyle");
    center.getElement().setId("GoodwaveVerticalCenter");
    center.addStyleName("floatLeft");
    // attempt to left justify

    ASRScorePanel widgets = null;
    if (e.isRepeat()) {
      widgets = new ASRScorePanel();
      scorePanel = widgets;
    }

    HorizontalPanel horizontalPanel = new HorizontalPanel();
    horizontalPanel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_LEFT);
    Panel hp = horizontalPanel;

    if (controller.getProps().isClassroomMode()) {
      hp.getElement().setId("GoodwaveHorizontalPanel");
      Panel addToList = makeAddToList(e, controller);
      Widget questionContent = getQuestionContent(e, addToList);
      questionContent.addStyleName("floatLeft");
      hp.add(questionContent);
    } else {
      hp.add(getQuestionContent(e, null));
    }

    center.add(hp);
    add(center);

    if (e.isRepeat() && widgets != null) {
      add(widgets);
    }
    addQuestions(service, controller, 1, center, screenPortion); // todo : revisit screen portion...

    this.navigationHelper = new NavigationHelper(exercise, controller, new PostAnswerProvider() {
      @Override
      public void postAnswers(ExerciseController controller, Exercise completedExercise) {
        System.out.println("postAnswers : load next exercise " + completedExercise.getID());
        listContainer.loadNextExercise(completedExercise);
      }
    }, listContainer, true);
    center.add(navigationHelper.makeSpacer());
    center.add(navigationHelper);
  }

  private Panel makeAddToList(Exercise e, ExerciseController controller) {
    addToList = new SplitDropdownButton("Add Item to List");
    addToList.setIcon(IconType.PLUS_SIGN);
    populateListChoices(e, controller, addToList);
    addToList.setType(ButtonType.PRIMARY);
    return addToList;

  }

  private void populateListChoices(final Exercise e, ExerciseController controller, final SplitDropdownButton w1) {
    System.out.println("populateListChoices populate list choices for " + controller.getUser());
    service.getListsForUser(controller.getUser(), true, new AsyncCallback<Collection<UserList>>() {
      @Override
      public void onFailure(Throwable caught) {
      }

      @Override
      public void onSuccess(Collection<UserList> result) {
        w1.clear();
        activeCount = 0;
        boolean anyAdded = false;
        for (final UserList ul : result) {
          if (!ul.contains(new UserExercise(e))) {
            activeCount++;
            anyAdded = true;
            final NavLink widget = new NavLink(ul.getName());
            w1.add(widget);
            widget.addClickHandler(new ClickHandler() {
              @Override
              public void onClick(ClickEvent event) {
                service.addItemToUserList(ul.getUniqueID(), new UserExercise(e), new AsyncCallback<List<UserExercise>>() {
                  @Override
                  public void onFailure(Throwable caught) {
                  }

                  @Override
                  public void onSuccess(List<UserExercise> result) {
                    showPopup("Item Added!");
                    widget.setVisible(false);
                    activeCount--;
                    if (activeCount == 0) {
                      NavLink widget = new NavLink("Exercise already added to your list(s)");
                      w1.add(widget);
                    }
                  }
                });
              }
            });
          }
        }
        if (!anyAdded) {
          NavLink widget = new NavLink("Exercise already added to your list(s)");
          w1.add(widget);
        }
      }
    });
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

  public void setBusy(boolean v) {
    this.isBusy = v;
  }

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
   * @param i
   * @param screenPortion
   */
  private void addQuestions(LangTestDatabaseAsync service, ExerciseController controller, int i, Panel toAddTo, float screenPortion) {
    Widget answerWidget = getAnswerWidget(service, controller, i, screenPortion);

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
  private Widget getQuestionContent(Exercise e, Panel addToList) {
    String content = e.getContent();
    String path = null;

    System.err.println("getQuestionContent content '" + content + "'");

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
    vp.getElement().setId("verticalContainer");
    vp.addStyleName("blockStyle");
    Widget questionContent = new HTML(content);
    questionContent.getElement().setId("QuestionContent");
    FlowPanel fp = new FlowPanel();
    questionContent.addStyleName("floatLeft");
    fp.addStyleName("trueInlineStyle");
    fp.add(questionContent);
    if (addToList != null) {
      fp.add(addToList);
      addToList.addStyleName("floatRight");
    }

    vp.add(fp);  // was cpContent


    Widget scoringAudioPanel = getScoringAudioPanel(e, path);
    SimplePanel div = new SimplePanel(scoringAudioPanel);
    div.addStyleName("trueInlineStyle");

    div.addStyleName("floatLeft");

    vp.add(div);
    return vp;
  }

  /**
   * If the exercise type is {@link Exercise.EXERCISE_TYPE#REPEAT_FAST_SLOW} then we put the fast/slow radio
   * buttons before the play button.
   *
   * @param e
   * @param path
   * @return
   */
  private Widget getScoringAudioPanel(final Exercise e, String path) {
    if (path != null) {
      path = wavToMP3(path);
    }
    ASRScoringAudioPanel audioPanel;

    System.out.println("getScoringAudioPanel : score panel " + scorePanel);
    if (e.getType() == Exercise.EXERCISE_TYPE.REPEAT_FAST_SLOW) {
      audioPanel = new FastAndSlowASRScoringAudioPanel(path, controller, scorePanel);
    } else {
      audioPanel = new ASRScoringAudioPanel(path, e.getRefSentence(), service, controller, false, scorePanel);
    }
    audioPanel.getElement().setId("ASRScoringAudioPanel");
    audioPanel.setRefAudio(path, e.getRefSentence());
    ResizableCaptionPanel cp = new ResizableCaptionPanel(NATIVE_REFERENCE_SPEAKER);
    cp.setContentWidget(audioPanel);

    contentAudio = audioPanel;
    contentAudio.setScreenPortion(screenPortion);
    return cp;
  }

  private String wavToMP3(String path) {
    return (path.endsWith(WAV)) ? path.replace(WAV, MP3) : path;
  }

  public void wasRevealed() {
    populateListChoices(exercise, controller, addToList);
  }

  private static class ResizableCaptionPanel extends CaptionPanel implements ProvidesResize, RequiresResize {
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
        false, // no keyboard
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
     * @see AudioPanel#getPlayButtons(com.google.gwt.user.client.ui.Widget)
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
      }

      @Override
      protected void addButtons() {
        add(postAudioRecordButton);
        postAudioRecordButton.addStyleName("rightFiveMargin");
        super.addButtons();
      }

      /**
       * No keyboard listener for play button -- since there can be two play buttons -- which one gets the space bar?
       */
      @Override
      protected void addKeyboardListener() {
      }
    }

    private class MyPostAudioRecordButton extends PostAudioRecordButton {
      public MyPostAudioRecordButton(ExerciseController controller) {
        super(exercise, controller, ASRRecordAudioPanel.this.service, ASRRecordAudioPanel.this.index);
      }

      @Override
      public void useResult(AudioAnswer result) {
        setRefAudio(refAudio, exercise.getRefSentence());
        setResultID(result.getResultID());
        getImagesForPath(wavToMP3(result.path));
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
      protected void useInvalidResult(AudioAnswer result) {
      }
    }
  }

  public boolean isBusy() {
    return isBusy;
  }

  /**
   * Scoring panel that, depending on the type of the exercise, shows radio buttons for fast and slow
   * versions of the audio file.
   */
  private class FastAndSlowASRScoringAudioPanel extends ASRScoringAudioPanel {
    private static final String RADIO_BUTTON_WIDTH = "40px";
    private static final String GROUP = "group";

    /**
     * @param path
     * @see GoodwaveExercisePanel#getScoringAudioPanel(mitll.langtest.shared.Exercise, String)
     */
    public FastAndSlowASRScoringAudioPanel(String path, ExerciseController controller1, ScoreListener scoreListener) {
      super(path,
        exercise.getRefSentence(),
        GoodwaveExercisePanel.this.service,
        controller1,
        false // no keyboard space bar binding
        ,
        scoreListener);
    }

    /**
     * @return
     * @see AudioPanel#addWidgets(String)
     */
    @Override
    protected Widget getBeforePlayWidget() {
      VerticalPanel vp = new VerticalPanel();

      boolean anyRef = false;
      boolean madeFast = false;
      if (exercise.getRefAudio() != null) {
        RadioButton fast = new RadioButton(GROUP, FAST);
        vp.add(fast);
        fast.setWidth(RADIO_BUTTON_WIDTH);

        fast.addClickHandler(new ClickHandler() {
          @Override
          public void onClick(ClickEvent event) {
            doPause();    // if the audio is playing, stop it
            setRefAudio(exercise.getRefAudio());
            getImagesForPath(wavToMP3(exercise.getRefAudio()));
          }
        });
        fast.setValue(true);
        anyRef = true;
        madeFast = true;
      }

      if (exercise.getSlowAudioRef() != null) {
        RadioButton slow = new RadioButton(GROUP, SLOW);
        slow.addClickHandler(new ClickHandler() {
          @Override
          public void onClick(ClickEvent event) {
            doPause();
            setRefAudio(exercise.getSlowAudioRef());
            getImagesForPath(wavToMP3(exercise.getSlowAudioRef()));
          }
        });
        slow.setWidth(RADIO_BUTTON_WIDTH);
        vp.add(slow);
        if (!madeFast) {
          slow.setValue(true);
        }
        anyRef = true;
      }
      vp.setWidth("80px");

      if (!anyRef) {
        vp.add(new Label("No reference audio."));
      }

      HorizontalPanel hp = new HorizontalPanel();
      hp.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_LEFT);
      hp.add(vp);
      hp.setWidth("60px");
      return vp;
    }
  }
}
