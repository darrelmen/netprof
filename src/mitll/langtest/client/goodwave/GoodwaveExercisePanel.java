package mitll.langtest.client.goodwave;

import com.goodwave.client.PlayAudioPanel;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.ExercisePanel;
import mitll.langtest.client.exercise.ExerciseQuestionState;
import mitll.langtest.client.recorder.RecordButton;
import mitll.langtest.client.recorder.RecordButtonPanel;
import mitll.langtest.client.recorder.SimpleRecordPanel;
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
public class GoodwaveExercisePanel extends ExercisePanel implements RequiresResize {
  /**
   * @see mitll.langtest.client.exercise.ExercisePanelFactory#getExercisePanel(mitll.langtest.shared.Exercise)
   * @param e
   * @param service
   * @param userFeedback
   * @param controller
   */
  public GoodwaveExercisePanel(final Exercise e, final LangTestDatabaseAsync service, final UserFeedback userFeedback,
                               final ExerciseController controller) {
    super(e, service, userFeedback, controller);

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
  @Override
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
  @Override
  protected Widget getAnswerWidget(final Exercise exercise, LangTestDatabaseAsync service, final ExerciseController controller, final int index) {
    final ExerciseQuestionState questionState = this;
    return new RecordAudioPanel(service, controller, exercise, questionState, index);
  }

  @Override
  protected String getQuestionPrompt(boolean promptInEnglish) {
    return getSpokenPrompt(promptInEnglish);
  }

  /**
   * on the server, notice which audio posts have arrived, and take the latest ones...
   * <br></br>
   * Move on to next exercise...
   *
   * @param service
   * @param userFeedback
   * @param controller
   * @param completedExercise
   */
  @Override
  protected void postAnswers(LangTestDatabaseAsync service, UserFeedback userFeedback, ExerciseController controller, Exercise completedExercise) {
    controller.loadNextExercise(completedExercise);
  }

  private static class RecordAudioPanel extends AudioPanel {
    private final ExerciseController controller;
    private final Exercise exercise;
    private final ExerciseQuestionState questionState;
    private final int index;

    public RecordAudioPanel(LangTestDatabaseAsync service, ExerciseController controller, Exercise exercise, ExerciseQuestionState questionState, int index) {
      super(null, service, controller.getSoundManager());
      this.controller = controller;
      this.exercise = exercise;
      this.questionState = questionState;
      this.index = index;
    }

    @Override
    protected PlayAudioPanel makePlayAudioPanel() {
      final Button record = new Button("\u25ba record");
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
              getImagesForPath(path1);
              questionState.recordCompleted(outer);
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
          record.setText("\u25ba record");
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
