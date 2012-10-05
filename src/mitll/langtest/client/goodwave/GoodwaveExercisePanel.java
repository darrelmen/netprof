package mitll.langtest.client.goodwave;

import com.goodwave.client.PlayAudioPanel;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CaptionPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.ProvidesResize;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.ExerciseQuestionState;
import mitll.langtest.client.recorder.RecordButton;
import mitll.langtest.client.user.UserFeedback;
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
  public static final String NATIVE_REFERENCE_SPEAKER = "Native Reference Speaker";

  private String refAudio;
  private String refSentence;

  protected Exercise exercise = null;
  protected ExerciseController controller;
  protected LangTestDatabaseAsync service;
  private ScoreListener scorePanel;
  private AudioPanel contentAudio, answerAudio;
  private static final boolean USE_ASR = true;

  /**
   * @see mitll.langtest.client.goodwave.GoodwaveExercisePanelFactory#getExercisePanel(mitll.langtest.shared.Exercise)
   * @param e
   * @param service
   * @param userFeedback
   * @param controller
   */
  public GoodwaveExercisePanel(final Exercise e, final LangTestDatabaseAsync service, final UserFeedback userFeedback,
                               final ExerciseController controller) {
    this.exercise = e;
    this.controller = controller;
    this.service = service;
    VerticalPanel center = new VerticalPanel();
    //center.add(new HTML("<h3>Item #" + e.getID() + "</h3>"));

    // attempt to left justify
    HorizontalPanel hp = new HorizontalPanel();
    hp.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_LEFT);
    hp.add(getQuestionContent(e));
    center.add(hp);
    setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
    add(center);
    setHorizontalAlignment(HasHorizontalAlignment.ALIGN_RIGHT);

    if (USE_ASR) {
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

    ResizableCaptionPanel cp = new ResizableCaptionPanel("User Recorder");
    cp.setContentWidget(answerWidget);
    toAddTo.add(cp);
  }

  public void onResize() {
    //System.out.println("GoodwaveExercisePanel : got resize " + getOffsetWidth());
    if (contentAudio != null) contentAudio.onResize();
    if (answerAudio != null) answerAudio.onResize();
  }

  /**
   * Replace the html 5 audio tag with our fancy waveform widget.
   *
   * @param e
   * @return
   */
  private Widget getQuestionContent(Exercise e) {
    String content = e.getContent();
    String path = null;
    if (e.getType() == Exercise.EXERCISE_TYPE.REPEAT) {
      this.refAudio = e.getRefAudio();
      this.refSentence =e.getRefSentence();
    }
    else if (content.contains("audio")) {
      int i = content.indexOf("source src=");
      String s = content.substring(i + "source src=".length() + 1).split("\\\"")[0];
      System.err.println("audio path '" + s + "'");
      path = s;

      int start = content.indexOf("<audio");
      int end = content.indexOf("audio>");
      content = content.substring(0, start) + content.substring(end + "audio>".length());
      this.refAudio = path;
      //  System.err.println("after " + content);
    }

    VerticalPanel vp = new VerticalPanel();

   // ResizableCaptionPanel cp = new ResizableCaptionPanel("Native Reference Speaker");
    Widget questionContent = new HTML(content);
    questionContent.addStyleName("leftTenMargin");
   // cp.setContentWidget(questionContent);
    vp.add(questionContent);
   // vp.add(cp);
    if (path != null) {
      AudioPanel w = new AudioPanel(path, service, controller.getSoundManager(), true);
      ResizableCaptionPanel cp = new ResizableCaptionPanel(NATIVE_REFERENCE_SPEAKER);
      cp.setContentWidget(w);
      vp.add(cp);

      contentAudio = w;
      contentAudio.setScreenPortion(controller.getScreenPortion());
    }
    return vp;
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
  private Widget getAnswerWidget(final Exercise exercise, LangTestDatabaseAsync service, final ExerciseController controller, final int index) {
    //final ExerciseQuestionState questionState = this;
    RecordAudioPanel widgets = new RecordAudioPanel(service, controller, exercise, null, index, refAudio, exercise.getRefSentence());
    widgets.addScoreListener(scorePanel);
    answerAudio = widgets;
    answerAudio.setScreenPortion(controller.getScreenPortion());

    return widgets;
  }

/*  private String getQuestionPrompt(boolean promptInEnglish) {
    return getSpokenPrompt(promptInEnglish);
  }*/
/*  private String getSpokenPrompt(boolean promptInEnglish) {
    return "&nbsp;&nbsp;&nbsp;Speak and record your answer in " +(promptInEnglish ? "English" : " the foreign language") +" :";
  }*/

  private static class RecordAudioPanel extends AudioPanel {
    private final ExerciseController controller;
    private final Exercise exercise;
    private final ExerciseQuestionState questionState;
    private final int index;
    private final String refAudio;
    private final String refSentence;

    /**
     * @see GoodwaveExercisePanel#getAnswerWidget(mitll.langtest.shared.Exercise, mitll.langtest.client.LangTestDatabaseAsync, mitll.langtest.client.exercise.ExerciseController, int)
     * @param service
     * @param controller
     * @param exercise
     * @param questionState
     * @param index
     * @param refAudio
     */
    public RecordAudioPanel(LangTestDatabaseAsync service, ExerciseController controller, Exercise exercise,
                            ExerciseQuestionState questionState, int index, String refAudio, String refSentence) {
      super(null, service, controller.getSoundManager(), USE_ASR);
      this.controller = controller;
      this.exercise = exercise;
      this.questionState = questionState;
      this.index = index;
      this.refAudio = refAudio;
      this.refSentence = refSentence;
    }

    @Override
    protected PlayAudioPanel makePlayAudioPanel() {
      final Button record = new Button("record");

      new RecordButton(record) {
        @Override
        protected void stopRecording() {
          controller.stopRecording();

          service.writeAudioFile(controller.getBase64EncodedWavFile()
              , exercise.getPlan(), exercise.getID(), "" + index, "" + controller.getUser(), new AsyncCallback<AudioAnswer>() {
            public void onFailure(Throwable caught) {}
            public void onSuccess(AudioAnswer result) {
              String path1 = result.path;
              if (path1.endsWith(".wav")) path1 = path1.replace(".wav", ".mp3");
              setRefAudio(refAudio, "This is a test.");
              getImagesForPath(path1);
           //   questionState.recordCompleted(outer);
            }
          });
        }

        @Override
        protected void startRecording() {
          controller.startRecording();
        }

        @Override
        protected void showRecording() {
          record.setText("stop");
        }

        @Override
        protected void showStopped() {
          record.setText("record");
        }
      };

      return new PlayAudioPanel(soundManager) {
        @Override
        protected void addButtons() {
          add(record);
          super.addButtons();
        }
      };

    }
  }
}
