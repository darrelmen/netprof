package mitll.langtest.client.exercise;

import com.github.gwtbootstrap.client.ui.Heading;
import com.google.gwt.user.client.ui.CaptionPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.ProvidesResize;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.client.scoring.GoodwaveExercisePanel;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.shared.CommonExercise;
import mitll.langtest.shared.ExerciseFormatter;
import mitll.langtest.shared.Result;
import mitll.langtest.shared.STATE;

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
  private static final String REPEAT_TWICE = "<i>Record the word or phrase, first at normal speed, then again at slow speed.</i>";
  private static final String RECORD_PROMPT = "Record the word or phrase, first at normal speed, then again at slow speed.";
  public static final String REGULAR_SPEED_RECORDING = "Regular speed recording";
  public static final String SLOW_SPEED_RECORDING = "Slow speed recording";
  private boolean isBusy = false;
  private Collection<RecordAudioPanel> audioPanels;

  /**
   * @param e
   * @param service
   * @param userFeedback
   * @param controller
   * @see mitll.langtest.client.custom.SimpleChapterNPFHelper#getFactory(mitll.langtest.client.list.PagingExerciseList)
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
  protected void addInstructions() {
    if (!exercise.getUnitToValue().isEmpty()) {
      Panel unitLessonForExercise = getUnitLessonForExercise();
      unitLessonForExercise.add(getItemHeader(exercise));
      add(unitLessonForExercise);
    }

    add(new Heading(4, RECORD_PROMPT));
  }

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

    // add normal speed recording widget
    RecordAudioPanel fast = new RecordAudioPanel(exercise, controller, this, service, index, true, Result.AUDIO_TYPE_REGULAR);
    ResizableCaptionPanel cp = new ResizableCaptionPanel(REGULAR_SPEED_RECORDING);
    cp.setContentWidget(fast);
    audioPanels.add(fast);
    vp.add(cp);

    if (fast.isAudioPathSet()) recordCompleted(fast);
    addAnswerWidget(index, fast);

    // add slow speed recording widget

    RecordAudioPanel slow = new RecordAudioPanel(exercise, controller, this, service, index + 1, true, Result.AUDIO_TYPE_SLOW);

    cp = new ResizableCaptionPanel(SLOW_SPEED_RECORDING);
    cp.setContentWidget(slow);
    audioPanels.add(slow);
    vp.add(cp);

    if (slow.isAudioPathSet()) recordCompleted(slow);
    addAnswerWidget(index + 1, slow);

    return vp;
  }

  private Panel getUnitLessonForExercise() {
    HorizontalPanel flow = new HorizontalPanel();
    flow.getElement().setId("getUnitLessonForExercise_unitLesson");
    flow.addStyleName("leftFiveMargin");
    //System.out.println("getUnitLessonForExercise " + exercise + " unit value " +exercise.getUnitToValue());

    for (String type : controller.getStartupInfo().getTypeOrder()) {
      Heading child = new Heading(GoodwaveExercisePanel.HEADING_FOR_UNIT_LESSON, type, exercise.getUnitToValue().get(type));
      child.addStyleName("rightFiveMargin");
      flow.add(child);
    }
    return flow;
  }

  Widget getItemHeader(CommonExercise e) {
    Heading w = new Heading(GoodwaveExercisePanel.HEADING_FOR_UNIT_LESSON, "Item", e.getID());
    w.getElement().setId("ItemHeading");
    return w;
  }

  public static class ResizableCaptionPanel extends CaptionPanel implements ProvidesResize, RequiresResize {
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
   * @see mitll.langtest.client.exercise.NavigationHelper#clickNext(ExerciseController, mitll.langtest.shared.CommonExercise)
   */
  @Override
  public void postAnswers(ExerciseController controller, CommonExercise completedExercise) {
    completedExercise.setState(STATE.RECORDED);
    exerciseList.setState(completedExercise.getID(), STATE.RECORDED);
    exerciseList.redraw();
    exerciseList.loadNextExercise(completedExercise);
  }
}
