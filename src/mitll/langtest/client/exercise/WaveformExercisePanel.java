package mitll.langtest.client.exercise;

import com.github.gwtbootstrap.client.ui.Heading;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.CaptionPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.ProvidesResize;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.shared.CommonExercise;
import mitll.langtest.shared.CommonShell;
import mitll.langtest.shared.ExerciseFormatter;
import mitll.langtest.shared.Result;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Does fancy flashing record bulb while recording.
 * User: GO22670
 * Date: 12/18/12
 * Time: 6:17 PM
 * To change this template use File | Settings | File Templates.
 */
public class WaveformExercisePanel extends ExercisePanel {
  public static final String REPEAT_TWICE = "<i>Record the word or phrase twice, first at normal speed, then again at slow speed.</i>";
  private boolean isBusy = false;
  private Collection<RecordAudioPanel> audioPanels;

  /**
   * @param e
   * @param service
   * @param userFeedback
   * @param controller
   * @seex mitll.langtest.client.exercise.ExercisePanelFactory#getExercisePanel(mitll.langtest.shared.Exercise)
   */
  public WaveformExercisePanel(final CommonExercise e, final LangTestDatabaseAsync service, final UserFeedback userFeedback,
                               final ExerciseController controller, ListInterface exerciseList) {
    super(e, service, userFeedback, controller, exerciseList);
    getElement().setId("WaveformExercisePanel");
  }

  @Override
  protected void onLoad() {
    super.onLoad();
    getParent().addStyleName("userNPFContentLightPadding");

  }

  public void setBusy(boolean v) {
    this.isBusy = v;
    setButtonsEnabled(!isBusy);
  }

  public boolean isBusy() {
    return isBusy;
  }
  protected void addInstructions() {  add(new Heading(4, "Record the word or phrase twice, first at normal speed, then again at slow speed."));  }

  protected String getExerciseContent(CommonExercise e) {
    return ExerciseFormatter.getArabic(e.getForeignLanguage());
  }
  /**
   * Has a answerPanel mark to indicate when the saved audio has been successfully posted to the server.
   *
   * @param exercise
   * @param service
   * @param controller
   * @param index
   * @return
   * @seex ExercisePanel#ExercisePanel(mitll.langtest.shared.Exercise, mitll.langtest.client.LangTestDatabaseAsync, mitll.langtest.client.user.UserFeedback, ExerciseController, ListInterface)
   */
  protected Widget getAnswerWidget(CommonExercise exercise, LangTestDatabaseAsync service, ExerciseController controller, final int index) {
    audioPanels = new ArrayList<RecordAudioPanel>();
    VerticalPanel vp = new VerticalPanel();

    RecordAudioPanel fast = new RecordAudioPanel(exercise, controller, this, service, index, true, Result.AUDIO_TYPE_REGULAR);
    ResizableCaptionPanel cp = new ResizableCaptionPanel("Regular speed recording");
    cp.setContentWidget(fast);
    vp.add(cp);

    audioPanels.add(fast);
    addAnswerWidget(index, fast);

    RecordAudioPanel slow = new RecordAudioPanel(exercise, controller, this, service, index + 1, true, Result.AUDIO_TYPE_SLOW);

    cp = new ResizableCaptionPanel("Slow speed recording");
    cp.setContentWidget(slow);
    audioPanels.add(slow);
    addAnswerWidget(index + 1, slow);

    vp.add(cp);
    return vp;
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


  protected Widget getContentScroller(HTML maybeRTLContent) {
    return maybeRTLContent;
  }

  @Override
  protected String getInstructions() {
    String prefix = "<br/>" + "";//THREE_SPACES;
    String prompt = REPEAT_TWICE;
    if (controller.getAudioType().equals(Result.AUDIO_TYPE_REGULAR)) {
      prompt = REPEAT_ONCE;
    }
    return prefix + prompt;
  }

  @Override
  public void onResize() {
    for (RecordAudioPanel ap : audioPanels) {
      ap.onResize();
    }
  }

  /**
   * @param promptInEnglish
   * @return
   * @seex ExercisePanel#addQuestionPrompt(com.google.gwt.user.client.ui.Panel, mitll.langtest.shared.Exercise)
   */
  @Override
  protected String getQuestionPrompt(boolean promptInEnglish) {
    return "";
  }

  /**
   * on the server, notice which audio posts have arrived, and take the latest ones...
   * <br></br>
   * Move on to next exercise...
   *
   * @param controller
   * @param completedExercise
   */
  @Override
  public void postAnswers(ExerciseController controller, CommonExercise completedExercise) {
    completedExercise.setState(CommonShell.STATE.RECORDED);
    exerciseList.setState(completedExercise.getID(), CommonShell.STATE.RECORDED);
    exerciseList.redraw();
    exerciseList.loadNextExercise(completedExercise);
  }
}
