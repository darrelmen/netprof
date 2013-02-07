package mitll.langtest.client.scoring;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.CaptionPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.ProvidesResize;
import com.google.gwt.user.client.ui.RadioButton;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.exercise.BusyPanel;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.gauge.ASRScorePanel;
import mitll.langtest.client.gauge.ScorePanel;
import mitll.langtest.client.sound.PlayAudioPanel;
import mitll.langtest.client.sound.PlayListener;
import mitll.langtest.shared.AudioAnswer;
import mitll.langtest.shared.Exercise;

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
  private static final String INSTRUCTIONS = "Instructions";
  private boolean isBusy = false;
  private static final String FAST = "Fast";
  private static final String SLOW = "Slow";

  private static final String WAV = ".wav";
  private static final String MP3 = ".mp3";

  /**
   * ??? Just for backward compatibility -- so we can run against old plan files
   */
  private String refAudio;

  protected Exercise exercise = null;
  protected ExerciseController controller;
  protected LangTestDatabaseAsync service;
  private ScoreListener scorePanel;
  private AudioPanel contentAudio, answerAudio;

  /**
   * Has a left side -- the question content (Instructions and audio panel (play button, waveform)) <br></br>
   * and a right side -- the charts and gauges {@link ASRScorePanel}
   * @see mitll.langtest.client.scoring.GoodwaveExercisePanelFactory#getExercisePanel(mitll.langtest.shared.Exercise)
   * @param e for this exercise
   * @param service so we can post recorded audio
   * @param controller
   */
  public GoodwaveExercisePanel(final Exercise e, final LangTestDatabaseAsync service,
                               final ExerciseController controller) {
    this.exercise = e;
    this.controller = controller;
    this.service = service;

    VerticalPanel center = new VerticalPanel();

    // attempt to left justify
    HorizontalPanel hp = new HorizontalPanel();
    hp.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_LEFT);
    hp.add(getQuestionContent(e));
    center.add(hp);
    setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
    add(center);
    setHorizontalAlignment(HasHorizontalAlignment.ALIGN_RIGHT);

    if (e.isRepeat()) {
      ASRScorePanel widgets = new ASRScorePanel();
      add(widgets);
      scorePanel = widgets;
    }
    else {
      ScorePanel w = new ScorePanel(false);
      add(w);
      scorePanel = w;
    }

    addQuestions(service, controller, 1, center);
  }

  /**
   * For every question,
   * <ul>
   *  <li>show the text of the question,  </li>
   *  <li>the prompt to the test taker (e.g "Speak your response in English")  </li>
   *  <li>an answer widget (either a simple text box, an flash audio record and playback widget, or a list of the answers, when grading </li>
   * </ul>     <br></br>
   * Remember the answer widgets so we can notice which have been answered, and then know when to enable the next button.
   * @param service
   * @param controller used in subclasses for audio control
   * @param i
   */
  private void addQuestions(LangTestDatabaseAsync service, ExerciseController controller, int i, Panel toAddTo) {
    Widget answerWidget = getAnswerWidget(service, controller, i);

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
   *
   * Replace the html 5 audio tag with our fancy waveform widget.
   *
   * @see #GoodwaveExercisePanel
   * @param e for this exercise
   * @return the panel that has the instructions and the audio panel
   */
  private Widget getQuestionContent(Exercise e) {
    String content = e.getContent();
    String path = null;
    if (e.isRepeat()) {
      this.refAudio = e.getRefAudio();
      path = refAudio;
    }
    else if (content.contains("audio")) {  // if we don't have proper REPEAT exercises
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

    CaptionPanel cpContent = new CaptionPanel(INSTRUCTIONS);
    Widget questionContent = new HTML(content);
    questionContent.addStyleName("leftTenMargin");
    cpContent.setContentWidget(questionContent);
    vp.add(cpContent);

    if (path != null) {
      ensureMP3(e, path, vp);
    }
    return vp;
  }

  /**
   * soundmanager plays mp3 files -- make sure there is a copy of the wav file as an mp3 on the server.
   * @see #getQuestionContent(mitll.langtest.shared.Exercise)
   * @param e
   * @param path
   * @param vp
   */
  private void ensureMP3(Exercise e, String path, final VerticalPanel vp) {
    final String fpath = path;
    final Exercise fe = e;
    //System.out.println("ensuring mp3 exists for " +path);
    service.ensureMP3(path,new AsyncCallback<Void>() {
      @Override
      public void onFailure(Throwable caught) {
        System.err.println("huh? couldn't write an MP3?");
      }

      @Override
      public void onSuccess(Void result) {
        if (fe.getType() == Exercise.EXERCISE_TYPE.REPEAT_FAST_SLOW) {
          //System.out.println("ensuring mp3 exists for slow audio path " +fe.getSlowAudioRef());

          service.ensureMP3(fe.getSlowAudioRef(), new AsyncCallback<Void>() {
            @Override
            public void onFailure(Throwable caught) {
              System.err.println("huh? couldn't write an MP3 for " + fe.getSlowAudioRef() + "?");
            }

            @Override
            public void onSuccess(Void result) {
              vp.add(getScoringAudioPanel(fe, fpath));
            }
          });
        } else {
          vp.add(getScoringAudioPanel(fe, fpath));
        }
      }
    });
  }

  /**
   * If the exercise type is {@link Exercise.EXERCISE_TYPE#REPEAT_FAST_SLOW} then we put the fast/slow radio
   * buttons before the play button.
   * @param e
   * @param path
   * @return
   */
  private Widget getScoringAudioPanel(final Exercise e, String path) {
    path = wavToMP3(path);
    ASRScoringAudioPanel audioPanel;

    if (e.getType() == Exercise.EXERCISE_TYPE.REPEAT_FAST_SLOW) {
      audioPanel = new FastAndSlowASRScoringAudioPanel(path);
    }
    else {
      audioPanel = new ASRScoringAudioPanel(path,e.getRefSentence(),service,controller, false);
    }
    audioPanel.setRefAudio(path, e.getRefSentence());
    ResizableCaptionPanel cp = new ResizableCaptionPanel(NATIVE_REFERENCE_SPEAKER);
    cp.setContentWidget(audioPanel);

    contentAudio = audioPanel;
    contentAudio.setScreenPortion(controller.getScreenPortion());
    return cp;
  }

  private String wavToMP3(String path) {
    return (path.endsWith(WAV)) ? path.replace(WAV, MP3) : path;
  }

  private static class ResizableCaptionPanel extends CaptionPanel implements ProvidesResize, RequiresResize {
   public ResizableCaptionPanel(String name) { super(name); }
    public void onResize() {
      Widget contentWidget = getContentWidget();
      if (contentWidget instanceof RequiresResize) {
        ((RequiresResize)contentWidget).onResize();
      }
    }
  }

  /**
   * Has a answerPanel mark to indicate when the saved audio has been successfully posted to the server.
   *
   * @paramx exercise
   * @param service
   * @param controller
   * @param index
   * @return
   */
  private Widget getAnswerWidget(LangTestDatabaseAsync service,
                                 final ExerciseController controller, final int index) {
    ScoringAudioPanel widgets = new ASRRecordAudioPanel(service, index);
    widgets.addScoreListener(scorePanel);
    answerAudio = widgets;
    answerAudio.setScreenPortion(controller.getScreenPortion());

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
     * @see GoodwaveExercisePanel#getAnswerWidget
     * @param service
     * @paramx controller
     * @param index
     */
    public ASRRecordAudioPanel(LangTestDatabaseAsync service, int index) {
      super(service, controller.getSoundManager(), controller.showOnlyOneExercise(), controller.getSegmentRepeats(),
          false // no keyboard
      );
      this.index = index;
    }

    /**
     * So here we're trying to make the record and play buttons know about each other
     * to the extent that when we're recording, we can't play audio, and when we're playing
     * audio, we can't record. We also mark the widget as busy so we can't move on to a different exercise.
     *
     * @see AudioPanel#getPlayButtons(com.google.gwt.user.client.ui.Widget)
     * @param toAdd
     * @return
     */
    @Override
    protected PlayAudioPanel makePlayAudioPanel(Widget toAdd) {
      postAudioRecordButton = new PostAudioRecordButton(exercise, controller, service, index) {
        @Override
        public void useResult(AudioAnswer result) {
          setRefAudio(refAudio, exercise.getRefSentence());
          getImagesForPath(wavToMP3(result.path));
        }

        @Override
        protected void startRecording() {
          playAudioPanel.setPlayEnabled(false);
          isBusy = true;
          super.startRecording();
        }

        @Override
        protected void stopRecording() {
          playAudioPanel.setPlayEnabled(true);
          isBusy = false;
          super.stopRecording();
        }
      };

      playAudioPanel = new PlayAudioPanel(soundManager, new PlayListener() {
        public void playStarted() {
          isBusy = true;
          postAudioRecordButton.getRecord().setEnabled(false);
        }

        public void playStopped() {
          isBusy = false;
          postAudioRecordButton.getRecord().setEnabled(true);
        }
      }) {
        @Override
        protected void addButtons() {
          add(postAudioRecordButton.getRecord());
          super.addButtons();
        }

        /**
         * No keyboard listener for play button -- since there can be two play buttons -- which one gets the space bar?
         */
        @Override
        protected void addKeyboardListener() {}
      };
      return playAudioPanel;
    }

    @Override
    protected void onUnload() {
      super.onUnload();
      postAudioRecordButton.onUnload();
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
     * @see GoodwaveExercisePanel#getScoringAudioPanel(mitll.langtest.shared.Exercise, String)
     * @param path
     */
    public FastAndSlowASRScoringAudioPanel(String path) {
      super(path,
          exercise.getRefSentence(),
          GoodwaveExercisePanel.this.service,
          controller,
          false // no keyboard space bar binding
      );
    }

    @Override
    protected Widget getBeforePlayWidget() {
      VerticalPanel vp = new VerticalPanel();
      RadioButton fast = new RadioButton(GROUP, FAST);
      vp.add(fast);
      fast.setWidth(RADIO_BUTTON_WIDTH);

      fast.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          setRefAudio(exercise.getRefAudio());
          getImagesForPath(wavToMP3(exercise.getRefAudio()));
        }
      });
      RadioButton slow = new RadioButton(GROUP, SLOW);
      slow.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          setRefAudio(exercise.getSlowAudioRef());
          getImagesForPath(wavToMP3(exercise.getSlowAudioRef()));
        }
      });
      slow.setWidth(RADIO_BUTTON_WIDTH);
      vp.add(slow);
      vp.setWidth("50px");
      fast.setValue(true);
      HorizontalPanel hp = new HorizontalPanel();
      hp.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_LEFT);
      hp.add(vp);
      hp.setWidth("60px");
      return vp;
    }
  }
}
