package mitll.langtest.client.scoring;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CaptionPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.ProvidesResize;
import com.google.gwt.user.client.ui.RadioButton;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.gauge.ASRScorePanel;
import mitll.langtest.client.gauge.ScorePanel;
import mitll.langtest.client.recorder.RecordButton;
import mitll.langtest.client.sound.PlayAudioPanel;
import mitll.langtest.shared.AudioAnswer;
import mitll.langtest.shared.Exercise;

/**
 * Mainly delegates recording to the {@link mitll.langtest.client.recorder.SimpleRecordPanel}.
 * User: GO22670
 * Date: 5/11/12
 * Time: 11:51 AM
 * To change this template use File | Settings | File Templates.
 */
public class GoodwaveExercisePanel extends HorizontalPanel implements RequiresResize, ProvidesResize {
  private static final String NATIVE_REFERENCE_SPEAKER = "Native Reference Speaker";
  private static final String USER_RECORDER = "User Recorder";
  private static final String INSTRUCTIONS = "Instructions";
  private static final String RECORD = "record";
  private static final String STOP = "stop";
  private static final String FAST = "Fast";
  private static final String SLOW = "Slow";

  private static final String WAV = ".wav";
  private static final String MP3 = ".mp3";
  private static final int AUTO_STOP_DELAY  = 15000; // millis

  /**
   * Just for backward compatibility -- so we can run against old plan files
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

   // System.out.println("got exercise " + e + " with ref sentence '" +e.getRefSentence() +"'");
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

    addQuestions(e, service, controller, 1, center);
  }

  /**
   * For every question,
   * <ul>
   *  <li>show the text of the question,  </li>
   *  <li>the prompt to the test taker (e.g "Speak your response in English")  </li>
   *  <li>an answer widget (either a simple text box, an flash audio record and playback widget, or a list of the answers, when grading </li>
   * </ul>     <br></br>
   * Remember the answer widgets so we can notice which have been answered, and then know when to enable the next button.
   * @param e
   * @param service
   * @param controller used in subclasses for audio control
   * @param i
   */
  private void addQuestions(Exercise e, LangTestDatabaseAsync service, ExerciseController controller, int i, Panel toAddTo) {
    Widget answerWidget = getAnswerWidget(e, service, controller, i);

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

    VerticalPanel vp = new VerticalPanel();

    CaptionPanel cpContent = new CaptionPanel(INSTRUCTIONS);
    Widget questionContent = new HTML(content);
    questionContent.addStyleName("leftTenMargin");
    cpContent.setContentWidget(questionContent);
    vp.add(cpContent);

    if (path != null) {
      vp.add(getScoringAudioPanel(e, path));
    }
    return vp;
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
    ASRScoringAudioPanel w =
        new ASRScoringAudioPanel(path, e.getRefSentence(), service,
            controller.getSoundManager(),
            controller.showOnlyOneExercise(), controller.getSegmentRepeats()) {
          @Override
          protected Widget getBeforePlayWidget() {
            if (e.getType() == Exercise.EXERCISE_TYPE.REPEAT_FAST_SLOW) {
              return getFastAndSlowRadioButtons();
            }
            else {
              return super.getBeforePlayWidget();
            }
          }

          private VerticalPanel getFastAndSlowRadioButtons() {
            VerticalPanel vp = new VerticalPanel();
            RadioButton fast = new RadioButton("group", FAST);
            vp.add(fast);
            fast.setWidth("40px");

            fast.addClickHandler(new ClickHandler() {
              @Override
              public void onClick(ClickEvent event) {
                setRefAudio(e.getRefAudio());
                getImagesForPath(wavToMP3(e.getRefAudio()));
              }
            });
            RadioButton slow = new RadioButton("group", SLOW);
            slow.addClickHandler(new ClickHandler() {
              @Override
              public void onClick(ClickEvent event) {
                setRefAudio(e.getSlowAudioRef());
                getImagesForPath(wavToMP3(e.getSlowAudioRef()));
              }
            });
            slow.setWidth("40px");
            vp.add(slow);
            vp.setWidth("50px");
            fast.setValue(true);
            HorizontalPanel hp = new HorizontalPanel();
            hp.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_LEFT);
            hp.add(vp);
            hp.setWidth("60px");
            return vp;
          }
        };
    w.setRefAudio(path, e.getRefSentence());
    ResizableCaptionPanel cp = new ResizableCaptionPanel(NATIVE_REFERENCE_SPEAKER);
    cp.setContentWidget(w);

    contentAudio = w;
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
   * @param exercise
   * @param service
   * @param controller
   * @param index
   * @return
   * @see mitll.langtest.client.exercise.ExercisePanel#ExercisePanel(mitll.langtest.shared.Exercise, mitll.langtest.client.LangTestDatabaseAsync, mitll.langtest.client.user.UserFeedback, mitll.langtest.client.exercise.ExerciseController)
   */
  private Widget getAnswerWidget(final Exercise exercise, LangTestDatabaseAsync service,
                                 final ExerciseController controller, final int index) {
    ScoringAudioPanel widgets = exercise.isRepeat() ?
        new ASRRecordAudioPanel(service, index) :
        new DTWRecordAudioPanel(service, index);
    widgets.addScoreListener(scorePanel);
    answerAudio = widgets;
    answerAudio.setScreenPortion(controller.getScreenPortion());

    return widgets;
  }

  /**
   * @deprecated Tamas says we won't use this in the future
   */
  private class DTWRecordAudioPanel extends DTWScoringPanel {
    private final int index;

    /**
     * @see GoodwaveExercisePanel#getAnswerWidget(mitll.langtest.shared.Exercise, mitll.langtest.client.LangTestDatabaseAsync, mitll.langtest.client.exercise.ExerciseController, int)
     * @param service
     * @param index
     */
    public DTWRecordAudioPanel(LangTestDatabaseAsync service, int index) {
      super(service, controller.getSoundManager(), controller.showOnlyOneExercise());
      this.index = index;
    }

    @Override
    protected PlayAudioPanel makePlayAudioPanel(Widget toadd) {
      final PostAudioRecordButton postAudioRecordButton = new PostAudioRecordButton(this, index);

      return new PlayAudioPanel(soundManager) {
        @Override
        protected void addButtons() {
          add(postAudioRecordButton.getRecord());
          super.addButtons();
        }
      };
    }
  }

  private class ASRRecordAudioPanel extends ASRScoringAudioPanel {
    private final int index;

    /**
     * @see GoodwaveExercisePanel#getAnswerWidget(mitll.langtest.shared.Exercise, mitll.langtest.client.LangTestDatabaseAsync, mitll.langtest.client.exercise.ExerciseController, int)
     * @param service
     * @paramx controller
     * @param index
     */
    public ASRRecordAudioPanel(LangTestDatabaseAsync service, int index) {
      super(service, controller.getSoundManager(), controller.showOnlyOneExercise(), controller.getSegmentRepeats());
      this.index = index;
    }

    @Override
    protected PlayAudioPanel makePlayAudioPanel(Widget toAdd) {
      final PostAudioRecordButton postAudioRecordButton = new PostAudioRecordButton(this, index);

      return new PlayAudioPanel(soundManager) {
        @Override
        protected void addButtons() {
          add(postAudioRecordButton.getRecord());
          super.addButtons();
        }
      };
    }
  }

  private class PostAudioRecordButton extends RecordButton {
    private ScoringAudioPanel widgets;
    private int index;

    public PostAudioRecordButton(final ScoringAudioPanel widgets, int index) {
      super(new Button(RECORD), AUTO_STOP_DELAY);
      this.widgets = widgets;
      this.index = index;
    }

    @Override
    protected void stopRecording() {
      controller.stopRecording();

      service.writeAudioFile(controller.getBase64EncodedWavFile()
          , exercise.getPlan(), exercise.getID(),
          "" + index, "" + controller.getUser(),
          new AsyncCallback<AudioAnswer>() {
            public void onFailure(Throwable caught) {
              showPopup(AudioAnswer.Validity.INVALID.getPrompt());
            }

            /**
             * Feedback for when audio isn't valid for some reason.
             * @param toShow
             */
            private void showPopup(String toShow) {
              final PopupPanel popupImage = new PopupPanel(true);
              popupImage.add(new HTML(toShow));
              popupImage.showRelativeTo(getRecord());
              Timer t = new Timer() {
                @Override
                public void run() { popupImage.hide(); }
              };
              t.schedule(3000);
            }

            public void onSuccess(AudioAnswer result) {
              System.out.println("PostAudioRecordButton : Got audio answer " + result);
              if (result.validity == AudioAnswer.Validity.OK) {
                widgets.setRefAudio(refAudio, exercise.getRefSentence());
                widgets.getImagesForPath(wavToMP3(result.path));
              }
              else {
                showPopup(result.validity.getPrompt());
              }
            }
          });
    }

    @Override
    protected void startRecording() {
      controller.startRecording();
    }

    /**
     * So we don't want the button changing width when we change the text.
     */
    @Override
    protected void showRecording() {
      int w = getRecord().getOffsetWidth();
      ((Button)getRecord()).setText(STOP);
      if (getRecord().getOffsetWidth() < w) getRecord().setWidth(w +"px");
    }

    @Override
    protected void showStopped() {
      ((Button)getRecord()).setText(RECORD);
    }
  }
}
