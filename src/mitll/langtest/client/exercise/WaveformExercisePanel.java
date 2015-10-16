package mitll.langtest.client.exercise;

import com.github.gwtbootstrap.client.ui.Heading;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.*;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.client.scoring.GoodwaveExercisePanel;
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
  private static final String RECORD_PROMPT = "Record the word or phrase, first at normal speed, then again at slow speed.";
  private static final String RECORD_PROMPT2 = "Record the in-context sentence.";
  private static final String EXAMPLE_RECORD = "EXAMPLE_RECORD";
  private boolean isBusy = false;
  private Collection<RecordAudioPanel> audioPanels;

  /**
   * @param e
   * @param service
   * @param controller
   * @param doNormalRecording
   * @param instance
   * @see mitll.langtest.client.custom.SimpleChapterNPFHelper#getFactory(mitll.langtest.client.list.PagingExerciseList)
   */
  public WaveformExercisePanel(final CommonExercise e, final LangTestDatabaseAsync service,
                               final ExerciseController controller, ListInterface exerciseList, boolean doNormalRecording, String instance) {
    super(e, service, controller, exerciseList, doNormalRecording ? "" : EXAMPLE_RECORD, instance);
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

  /**
   * @see ExercisePanel#ExercisePanel(CommonExercise, LangTestDatabaseAsync, ExerciseController, ListInterface, String, String)
   *
   */
  protected void addInstructions() {
    if (!exercise.getUnitToValue().isEmpty()) {
      Panel unitLessonForExercise = getUnitLessonForExercise();
      unitLessonForExercise.add(getItemHeader(exercise));
      add(unitLessonForExercise);
    }

    add(new Heading(4, isExampleRecord() ? RECORD_PROMPT2 : RECORD_PROMPT));
  }

  private boolean isExampleRecord() {
    return message.equals(EXAMPLE_RECORD);
  }
  private boolean isNormalRecord() { return !isExampleRecord(); }

  @Override
  protected String getExerciseContent(CommonExercise e) {
    //System.out.println("normal recording for " +e.getID());
    String context = isNormalRecord() ? e.getForeignLanguage() : hasContext(exercise) ? exercise.getContext() : "No in-context audio for this exercise.";
    return ExerciseFormatter.getArabic(context);
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
    Panel vp = new VerticalPanel();

    // add normal speed recording widget
    if (isNormalRecord()) {
      addRecordAudioPanelNoCaption(exercise, service, controller, index, vp, Result.AUDIO_TYPE_REGULAR);
      // add slow speed recording widget
      VerticalPanel widgets = addRecordAudioPanelNoCaption(exercise, service, controller, index + 1, vp, Result.AUDIO_TYPE_SLOW);
      widgets.addStyleName("topFiveMargin");
    } else {
      addExampleSentenceRecorder(exercise, service, controller, index, vp);
    }

    return vp;
  }

  private boolean hasContext(CommonExercise exercise) {
    return exercise.getContext() != null && !exercise.getContext().isEmpty();
  }

  private void addExampleSentenceRecorder(CommonExercise exercise, LangTestDatabaseAsync service, ExerciseController controller, int index, Panel vp) {
    RecordAudioPanel fast = new RecordAudioPanel(exercise, controller, this, service, index, false, "context=" + Result.AUDIO_TYPE_REGULAR, instance);
    audioPanels.add(fast);
    vp.add(fast);

    if (fast.isAudioPathSet()) recordCompleted(fast);
    addAnswerWidget(index, fast);
  }

  private VerticalPanel addRecordAudioPanelNoCaption(CommonExercise exercise, LangTestDatabaseAsync service,
                                            ExerciseController controller, int index, Panel vp, String audioType) {
//    System.out.println("addRecordAudioPanel " + exercise + " audioType " +audioType);
    RecordAudioPanel fast = new RecordAudioPanel(exercise, controller, this, service, index, false, audioType, instance);
    audioPanels.add(fast);
    vp.add(fast);

    if (fast.isAudioPathSet()) recordCompleted(fast);
    addAnswerWidget(index, fast);
    return fast;
  }

  private Panel getUnitLessonForExercise() {
    Panel flow = new HorizontalPanel();
    flow.getElement().setId("getUnitLessonForExercise_unitLesson");
    flow.addStyleName("leftFiveMargin");
    flow.getElement().getStyle().setMarginTop(-8, Style.Unit.PX);
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

  protected Widget getContentScroller(HTML maybeRTLContent) {
    return maybeRTLContent;
  }

  @Override
  public void onResize() {
    for (RecordAudioPanel ap : audioPanels) {  ap.onResize();  }
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
