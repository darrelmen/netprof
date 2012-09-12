package mitll.langtest.client.goodwave;

import com.goodwave.client.PlayAudioPanel;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.LangTest;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.ExercisePanel;
import mitll.langtest.client.exercise.ExerciseQuestionState;
import mitll.langtest.client.recorder.RecordButton;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.shared.AudioAnswer;
import mitll.langtest.shared.Exercise;

import java.util.List;

/**
 * Mainly delegates recording to the {@link mitll.langtest.client.recorder.SimpleRecordPanel}.
 * User: GO22670
 * Date: 5/11/12
 * Time: 11:51 AM
 * To change this template use File | Settings | File Templates.
 */
public class GoodwaveExercisePanel extends HorizontalPanel implements RequiresResize {
  private String refAudio;
  protected Exercise exercise = null;
  protected ExerciseController controller;
  protected LangTestDatabaseAsync service;
  private ScorePanel scorePanel;

  /**
   * @see mitll.langtest.client.exercise.ExercisePanelFactory#getExercisePanel(mitll.langtest.shared.Exercise)
   * @param e
   * @param service
   * @param userFeedback
   * @param controller
   */
  public GoodwaveExercisePanel(final Exercise e, final LangTestDatabaseAsync service, final UserFeedback userFeedback,
                               final ExerciseController controller) {
   // super(Style.Unit.PX);
    setWidth("100%");
    setHeight("100%");
    //super(e, service, userFeedback, controller);
    this.exercise = e;
    this.controller = controller;
    this.service = service;
    VerticalPanel center = new VerticalPanel();
    center.setWidth("100%");
    center.setHeight("100%");
    center.add(new HTML("<h3>Item #" + e.getID() + "</h3>"));

    // attempt to left justify
    HorizontalPanel hp = new HorizontalPanel();
    hp.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_LEFT);
    hp.add(getQuestionContent(e));
    //hp.setWidth((Window.getClientWidth()- LangTest.EXERCISE_LIST_WIDTH-100) + "px");
    center.add(hp);
    /*addNorth(new SimplePanel(),0);
    addSouth(new SimplePanel(),0);
    addWest(new SimplePanel(),0);
    addEast(new SimplePanel(),100);
    */add(center);
    scorePanel = new ScorePanel(false);

    add(scorePanel);
    int i = 1;

    addQuestions(e, service, controller, i, center);

    //if (scorePanel != null) scorePanel.initialize(); // TODO : activate!
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
    List<Exercise.QAPair> englishQuestions = e.getEnglishQuestions();
    int n = englishQuestions.size();
    //System.out.println("eng q " + englishQuestions);
    for (Exercise.QAPair pair : e.getQuestions()) {
      // add question header
      Exercise.QAPair engQAPair = englishQuestions.get(i - 1);

      getQuestionHeader(i, n, engQAPair, pair,toAddTo);
      i++;
      // add question prompt
      VerticalPanel vp = new VerticalPanel();
      addQuestionPrompt(vp, e);

      // add answer widget
      Widget answerWidget = getAnswerWidget(e, service, controller, i-1);
      vp.add(answerWidget);
   //   answers.add(answerWidget);

      toAddTo.add(vp);
    }
  }

  protected void getQuestionHeader(int i, int total, Exercise.QAPair engQAPair, Exercise.QAPair pair,  Panel toAddTo) {
    String questionHeader = "Question" +
        (total > 1 ? " #" + i : "")+
        " : " + pair.getQuestion();
    toAddTo.add(new HTML("<h4>" + questionHeader + "</h4>"));
  }

  private void addQuestionPrompt(Panel vp, Exercise e) {
    vp.add(new HTML(getQuestionPrompt(e.promptInEnglish)));
    SimplePanel spacer = new SimplePanel();
    spacer.setSize("500px", 20 + "px");
    vp.add(spacer);
  }

  public void onResize() {
    System.out.println("got resize " + getOffsetWidth());
  }

  /**
   * Replace the html 5 audio tag with our fancy waveform widget.
   *
   * @param e
   * @return
   */
  protected Widget getQuestionContent(Exercise e) {
    String content = e.getContent();
    String path = null;
    if (content.contains("audio")) {
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
    Widget questionContent = new HTML(content);
    vp.add(questionContent);
    if (path != null) {
      AudioPanel w = new AudioPanel(path, service, controller.getSoundManager());
      vp.add(w);
    }
    return vp;
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
  protected Widget getAnswerWidget(final Exercise exercise, LangTestDatabaseAsync service, final ExerciseController controller, final int index) {
    //final ExerciseQuestionState questionState = this;
    RecordAudioPanel widgets = new RecordAudioPanel(service, controller, exercise, null, index, refAudio);
    widgets.addScoreListener(scorePanel);
    return widgets;
  }

  protected String getQuestionPrompt(boolean promptInEnglish) {
    return getSpokenPrompt(promptInEnglish);
  }
  protected String getSpokenPrompt(boolean promptInEnglish) {
    return "&nbsp;&nbsp;&nbsp;Speak and record your answer in " +(promptInEnglish ? "English" : " the foreign language") +" :";
  }

  /**
   * on the server, notice which audio posts have arrived, and take the latest ones...
   * <br></br>
   * Move on to next exercise...
   *
   * @paramx service
   * @paramx userFeedback
   * @paramx controller
   * @paramxs completedExercise
   */
/*
  @Override
  protected void postAnswers(LangTestDatabaseAsync service, UserFeedback userFeedback, ExerciseController controller, Exercise completedExercise) {
    controller.loadNextExercise(completedExercise);
  }
*/

  private static class RecordAudioPanel extends AudioPanel {
    private final ExerciseController controller;
    private final Exercise exercise;
    private final ExerciseQuestionState questionState;
    private final int index;
    private final String refAudio;

    public RecordAudioPanel(LangTestDatabaseAsync service, ExerciseController controller, Exercise exercise, ExerciseQuestionState questionState, int index, String refAudio) {
      super(null, service, controller.getSoundManager());
      this.controller = controller;
      this.exercise = exercise;
      this.questionState = questionState;
      this.index = index;
      this.refAudio = refAudio;
    }

    @Override
    protected PlayAudioPanel makePlayAudioPanel() {
      final Button record = new Button("record");
      //record.addStyleName("squareButton");
      final Widget outer = this;

      RecordButton rb = new RecordButton(record) {
        @Override
        protected void stopRecording() {
          controller.stopRecording();

          service.writeAudioFile(controller.getBase64EncodedWavFile()
              , exercise.getPlan(), exercise.getID(), "" + index, "" + controller.getUser(), new AsyncCallback<AudioAnswer>() {
            public void onFailure(Throwable caught) {
            }

            public void onSuccess(AudioAnswer result) {
              String path1 = result.path;
              if (path1.endsWith(".wav")) path1 = path1.replace(".wav", ".mp3");
              setRefAudio(refAudio);
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
