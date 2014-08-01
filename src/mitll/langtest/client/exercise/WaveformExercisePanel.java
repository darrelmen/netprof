package mitll.langtest.client.exercise;

import com.github.gwtbootstrap.client.ui.Heading;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.i18n.shared.WordCountDirectionEstimator;
import com.google.gwt.user.client.ui.*;
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
  private static final String REGULAR_SPEED_RECORDING = "Regular speed recording";
  private static final String SLOW_SPEED_RECORDING = "Slow speed recording";
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

  @Override
  protected String getExerciseContent(CommonExercise e) { return ExerciseFormatter.getArabic(e.getForeignLanguage()); }

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
    addRecordAudioPanel(exercise, service, controller, index, vp,Result.AUDIO_TYPE_REGULAR,REGULAR_SPEED_RECORDING);
    // add slow speed recording widget
    addRecordAudioPanel(exercise, service, controller, index+1, vp,Result.AUDIO_TYPE_SLOW,SLOW_SPEED_RECORDING);
    if (exercise.getContext() != null && !exercise.getContext().isEmpty()) {
      DivWidget div = new DivWidget();
      div.addStyleName("Instruction");
      InlineHTML englishPhrase = new InlineHTML(exercise.getContext(), WordCountDirectionEstimator.get().estimateDirection(exercise.getContext()));
   //   englishPhrase.addStyleName(true ? "Instruction-data-with-wrap" : "Instruction-data");
      englishPhrase.addStyleName("Instruction-data-with-wrap");
        div.add(englishPhrase);
      englishPhrase.addStyleName("xlargeFont");
      div.addStyleName("topMargin");

      VerticalPanel widgets = addRecordAudioPanel(exercise, service, controller, index + 1, vp, "context=" + Result.AUDIO_TYPE_REGULAR, "Example Sentence");
      widgets.insert(div,0);
    }

    return vp;
  }

  private VerticalPanel addRecordAudioPanel(CommonExercise exercise, LangTestDatabaseAsync service,
                                            ExerciseController controller, int index, Panel vp, String audioType, String caption) {

    System.out.println("addRecordAudioPanel " + exercise + " audioType " +audioType);

    RecordAudioPanel fast = new RecordAudioPanel(exercise, controller, this, service, index, true, audioType);
    ResizableCaptionPanel cp = new ResizableCaptionPanel(caption);
    cp.setContentWidget(fast);
    audioPanels.add(fast);
    vp.add(cp);

    if (fast.isAudioPathSet()) recordCompleted(fast);
    addAnswerWidget(index, fast);
    return fast;
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
