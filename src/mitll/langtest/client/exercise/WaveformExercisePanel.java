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
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.client.scoring.UnitChapterItemHelper;
import mitll.langtest.shared.ExerciseFormatter;
import mitll.langtest.shared.Result;
import mitll.langtest.shared.exercise.*;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Does fancy flashing record bulb while recording.
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 12/18/12
 * Time: 6:17 PM
 * To change this template use File | Settings | File Templates.
 */
public class WaveformExercisePanel<L extends CommonShell, T extends CommonExercise> extends ExercisePanel<L,T> {
//  private final Logger logger = Logger.getLogger("WaveformExercisePanel");
  public static final String CONTEXT = "context=";

  private static final String RECORD_PROMPT  = "Record the word or phrase, first at normal speed, then again at slow speed.";
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
  public WaveformExercisePanel(T e, LangTestDatabaseAsync service,
                               ExerciseController controller, ListInterface<L> exerciseList,
                               boolean doNormalRecording, String instance) {
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
   * @see ExercisePanel#ExercisePanel
   *
   */
  protected void addInstructions() {
    Panel flow = new UnitChapterItemHelper<T>(controller.getTypeOrder()).addUnitChapterItem(exercise, this);
    if (flow != null) {
      flow.getElement().getStyle().setMarginTop(-8, Style.Unit.PX);
    }
    add(new Heading(4, isExampleRecord() ? RECORD_PROMPT2 : RECORD_PROMPT));
  }

  private boolean isNormalRecord()  { return !isExampleRecord(); }
  private boolean isExampleRecord() {
    return message.equals(EXAMPLE_RECORD);
  }

  /**
   * TODO : support recording audio for multiple context sentences...?
   * @param e
   * @return
   */
  @Override
  protected String getExerciseContent(T e) {
    String context = isNormalRecord() ? e.getForeignLanguage() :
        hasContext(exercise) ? exercise.getDirectlyRelated().iterator().next().getForeignLanguage() : "No in-context audio for this exercise.";
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
  protected Widget getAnswerWidget(T exercise, LangTestDatabaseAsync service, ExerciseController controller, final int index) {
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

  private boolean hasContext(T exercise) {
    return !exercise.getDirectlyRelated().isEmpty();// exercise.getContext() != null && !exercise.getContext().isEmpty();
  }

  private void addExampleSentenceRecorder(T exercise, LangTestDatabaseAsync service, ExerciseController controller,
                                          int index, Panel vp) {
    RecordAudioPanel fast = new RecordAudioPanel<T>(exercise, controller, this, service, index, false,
        AudioAttribute.CONTEXT_AUDIO_TYPE, instance);
    audioPanels.add(fast);
    vp.add(fast);

    if (fast.isAudioPathSet()) recordCompleted(fast);
    addAnswerWidget(index, fast);
  }

  private VerticalPanel addRecordAudioPanelNoCaption(T exercise, LangTestDatabaseAsync service,
                                            ExerciseController controller, int index, Panel vp, String audioType) {
    RecordAudioPanel fast = new RecordAudioPanel<T>(exercise, controller, this, service, index, false, audioType, instance);
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
    for (RecordAudioPanel ap : audioPanels) {  ap.onResize();  }
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
  //  completedExercise.setState(STATE.RECORDED);
    // TODO : gah = do we really need to do this???
   // logger.info("Not setting state on " +completedExercise.getID());
    exerciseList.setState(completedExercise.getID(), STATE.RECORDED);
    exerciseList.redraw();
    exerciseList.loadNextExercise(completedExercise);
  }
}
