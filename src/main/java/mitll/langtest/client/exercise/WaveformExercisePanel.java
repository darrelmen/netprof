/*
 *
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2015. Other request for this document shall be referred
 * to DLIFLC.
 *
 * WARNING: This document may contain technical data whose export is restricted
 * by the Arms Export Control Act (AECA) or the Export Administration Act (EAA).
 * Transfer of this data by any means to a non-US person who is not eligible to
 * obtain export-controlled data is prohibited. By accepting this data, the consignee
 * agrees to honor the requirements of the AECA and EAA. DESTRUCTION NOTICE: For
 * unclassified, limited distribution documents, destroy by any method that will
 * prevent disclosure of the contents or reconstruction of the document.
 *
 * This material is based upon work supported under Air Force Contract No.
 * FA8721-05-C-0002 and/or FA8702-15-D-0001. Any opinions, findings, conclusions
 * or recommendations expressed in this material are those of the author(s) and
 * do not necessarily reflect the views of the U.S. Air Force.
 *
 * © 2015 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 *
 *
 */

package mitll.langtest.client.exercise;

import com.github.gwtbootstrap.client.ui.Heading;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.*;
import mitll.langtest.client.LangTest;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.client.scoring.AudioPanel;
import mitll.langtest.client.scoring.UnitChapterItemHelper;
import mitll.langtest.shared.ExerciseFormatter;
import mitll.langtest.shared.answer.AudioType;
import mitll.langtest.shared.exercise.ClientExercise;
import mitll.langtest.shared.exercise.CommonShell;
import mitll.langtest.shared.exercise.HasID;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

/**
 * Does fancy flashing record bulb while recording.
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 12/18/12
 * Time: 6:17 PM
 * To change this template use File | Settings | File Templates.
 */
public class WaveformExercisePanel<L extends CommonShell, T extends ClientExercise> extends ExercisePanel<L, T> {
  private Logger logger = Logger.getLogger("WaveformExercisePanel");

  private static final String NO_AUDIO_TO_RECORD = "No in-context sentence for this exercise.";
  public static final String CONTEXT = "context=";

  /**
   * @see #addInstructions
   */
  private static final String RECORD_PROMPT = "Record the word or phrase at normal speed.";//, first at normal speed, then again at slow speed.";
  private static final String RECORD_PROMPT2 = "Record the word or phrase, first at normal speed, then again at slow speed.";
  private boolean isBusy = false;
  private Collection<RecordAudioPanel> audioPanels;

  /**
   * @param e
   * @param controller
   * @param doNormalRecording
   * @param instance
   * @param enableNextOnlyWhenBothCompleted
   * @see mitll.langtest.client.custom.SimpleChapterNPFHelper#getFactory(mitll.langtest.client.list.PagingExerciseList)
   */
  public WaveformExercisePanel(T e,
                               ExerciseController controller, ListInterface<L, T> exerciseList,
                               boolean doNormalRecording,
                               String instance,
                               boolean enableNextOnlyWhenBothCompleted) {
    super(e, controller, exerciseList, instance, doNormalRecording, enableNextOnlyWhenBothCompleted);
  }

  @Override
  protected void onLoad() {
    super.onLoad();
    getParent().addStyleName("userNPFContentLightPadding");
  }

  /**
   * Make sure we disable the other companion panel.
   * <p>
   * Only change enabled state if it's not recording already.
   *
   * @param v
   */
  public void setBusy(boolean v) {
    this.isBusy = v;
    setButtonsEnabled(!isBusy);

    for (RecordAudioPanel ap : audioPanels) {
      if (!ap.isRecording()) {
        ap.setEnabled(!v);
      }
    }
  }

  public boolean isBusy() {
    return isBusy;
  }

  /**
   * @see ExercisePanel#ExercisePanel
   */
  protected void addInstructions() {

    if (logger == null) logger = Logger.getLogger("WaveformExercisePanel");
    List<String> typeOrder = getTypeOrder();


    UIObject flow = new UnitChapterItemHelper<T>(typeOrder).addUnitChapterItem(exercise, this);
    if (flow != null) {
      flow.getElement().getStyle().setMarginTop(-8, Style.Unit.PX);
    }
    boolean normalRecord = isNormalRecord();
    //  logger.info("addInstructions normal  "+normalRecord);
    add(new Heading(4, normalRecord ? RECORD_PROMPT2 : RECORD_PROMPT));//isExampleRecord() ? RECORD_PROMPT2 : RECORD_PROMPT));
  }

  @NotNull
  private List<String> getTypeOrder() {
    List<String> typeOrder = new ArrayList<>(controller.getTypeOrder());

    if (exercise != null && exercise.getAttributes() != null) {
//      exercise.getAttributes().forEach(exerciseAttribute -> logger.info("for " + exercise.getID() + " " + exerciseAttribute));
      exercise.getAttributes().forEach(exerciseAttribute -> {
        exercise.getUnitToValue().put(exerciseAttribute.getProperty(), exerciseAttribute.getValue());
        if (!typeOrder.contains(exerciseAttribute.getProperty())) {
          typeOrder.add(exerciseAttribute.getProperty());
        }
      });
    }
    return typeOrder;
  }

  /**
   * TODO : support recording audio for multiple context sentences...?
   *
   * @param e
   * @return
   * @see #getQuestionContent
   */
  @Override
  protected String getExerciseContent(T e) {
    if (logger == null) {
      logger = Logger.getLogger("WaveformExercisePanel");
    }
    String recordPrompt = getRecordPrompt(e);
  /*  logger.info("getExerciseContent for " + e.getID() + " context " + e.isContext() + " " + isNormalRecord() +
        "\n\tprompt " +recordPrompt);*/
    return ExerciseFormatter.getArabic(recordPrompt, controller.getLanguage());
  }

  private String getRecordPrompt(T e) {
    return isNormalRecord() ? e.getFLToShow() : hasContext(exercise) ? getFLToShow() : NO_AUDIO_TO_RECORD;
  }

  private String getFLToShow() {
    return exercise.isContext() ? exercise.getFLToShow() : exercise.getDirectlyRelated().iterator().next().getFLToShow();
  }

  protected String getEnglishToShow() {
    return isNormalRecord() ?
        exercise.getEnglish() :
        exercise.getDirectlyRelated().isEmpty() ?
            "" :
            exercise.getDirectlyRelated().iterator().next().getEnglish();
  }

  /**
   * Has a answerPanel mark to indicate when the saved audio has been successfully posted to the server.
   *
   * @param exercise
   * @param controller
   * @param index
   * @return
   * @seex ExercisePanel#ExercisePanel(mitll.langtest.shared.Exercise, mitll.langtest.client.services.LangTestDatabaseAsync, mitll.langtest.client.user.UserFeedback, ExerciseController, ListInterface)
   */
  protected Widget getAnswerWidget(T exercise, ExerciseController controller, final int index) {
    audioPanels = new ArrayList<>();
    Panel vp = new DivWidget();

    // add normal speed recording widget
    boolean normalRecord = isNormalRecord();

    {
      AudioType regular = normalRecord ? AudioType.REGULAR : AudioType.CONTEXT_REGULAR;

//      logger.info("getAnswerWidget audio type " + regular + " user " + controller.getUser() +
//          " \n\tcurrent" + controller.getUserManager().getCurrent());
      addRecordAudioPanelNoCaption(exercise, controller, index, vp, regular, false);
    }
    // add slow speed recording widget

    if (!exercise.isContext()) {
      AudioType slow = normalRecord ? AudioType.SLOW : AudioType.CONTEXT_SLOW;

//      logger.info("getAnswerWidget audio type " + slow + " user " + controller.getUser() +
//          " \n\tcurrent" + controller.getUserManager().getCurrent());

      UIObject widgets = addRecordAudioPanelNoCaption(exercise, controller, index + 1, vp, slow, false);
      widgets.addStyleName("topFiveMargin");
    }

    return vp;
  }

  private boolean hasContext(T exercise) {
    return exercise.isContext() || !exercise.getDirectlyRelated().isEmpty();
  }

  /**
   * @param exercise
   * @param controller
   * @param index
   * @param vp
   * @param audioType
   * @param showCurrentRecording
   * @return
   * @see ExercisePanel#getAnswerWidget(CommonShell, ExerciseController, int)
   */
  private DivWidget addRecordAudioPanelNoCaption(T exercise,
                                                 ExerciseController controller,
                                                 int index,
                                                 Panel vp,
                                                 AudioType audioType,
                                                 boolean showCurrentRecording) {
    RecordAudioPanel fast = new RecordAudioPanel<>(exercise, controller, this, index, false, audioType, showCurrentRecording);
    audioPanels.add(fast);
    vp.add(fast);

    if (fast.isAudioPathSet()) recordCompleted(fast);
    addAnswerWidget(index, fast);
    return fast;
  }

  protected Widget getContentScroller(HTML maybeRTLContent) {
    return maybeRTLContent;
  }

  @Override
  public void onResize() {
/*    logger.info(getElement().getId() + " gotResize " + (audioPanels
        != null ? audioPanels.size() : ""));*/
    audioPanels.forEach(AudioPanel::onResize);
  }

  /**
   * on the server, notice which audio posts have arrived, and take the latest ones...
   * <br></br>
   * Move on to next exercise...
   *
   * @param controller
   * @param completedExercise
   * @see mitll.langtest.client.exercise.NavigationHelper#clickNext
   */
  @Override
  public void postAnswers(ExerciseController controller, HasID completedExercise) {
    showRecordedState(completedExercise);
    exerciseList.loadNextExercise(completedExercise);
  }

  protected void showRecordedState(HasID completedExercise) {
    LangTest.EVENT_BUS.fireEvent(new AudioChangedEvent(instance));
    exerciseList.redraw();
  }
}
