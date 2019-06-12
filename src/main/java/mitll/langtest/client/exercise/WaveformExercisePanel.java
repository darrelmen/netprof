/*
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2019. Other request for this document shall be referred
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
 * © 2015-2019 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 */

package mitll.langtest.client.exercise;

import com.github.gwtbootstrap.client.ui.Heading;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.*;
import mitll.langtest.client.custom.dialog.DominoLinkNotice;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.client.scoring.AudioPanel;
import mitll.langtest.client.scoring.UnitChapterItemHelper;
import mitll.langtest.client.services.ExerciseServiceAsync;
import mitll.langtest.client.user.BasicDialog;
import mitll.langtest.shared.ExerciseFormatter;
import mitll.langtest.shared.answer.AudioType;
import mitll.langtest.shared.dialog.DialogMetadata;
import mitll.langtest.shared.exercise.*;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class WaveformExercisePanel<L extends CommonShell, T extends ClientExercise & HasUnitChapter & Details>
    extends ExercisePanel<L, T> {
  private Logger logger = Logger.getLogger("WaveformExercisePanel");
  public static final String DOMINO_PROJECT = "Domino Project";
  public static final String PARENT_ITEM = "Parent";

  /**
   *
   */
  private static final String NO_AUDIO_TO_RECORD = "No in-context sentence for this exercise.";
  public static final String CONTEXT = "context=";

  /**
   * @see #addInstructions
   */
  private static final String RECORD_PROMPT = "Record the word or phrase at normal speed.";
  private static final String PROMPT_BOTH_SPEEDS = "Record the word or phrase, first at normal speed, then again at slow speed.";
  private boolean isBusy = false;
  private Collection<RecordAudioPanel> audioPanels;

  private static final String LANGUAGE_META_DATA = DialogMetadata.LANGUAGE.name();
  private static final String SPEAKER_META_DATA = DialogMetadata.SPEAKER.name();

  /**
   * @param e
   * @param controller
   * @param doNormalRecording
   * @see mitll.langtest.client.custom.SimpleChapterNPFHelper#getFactory(mitll.langtest.client.list.PagingExerciseList)
   */
  public WaveformExercisePanel(T e,
                               ExerciseController controller,
                               ListInterface<L, T> exerciseList,
                               boolean doNormalRecording) {
    super(e, controller, exerciseList, doNormalRecording);
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
   * add info about parent item too
   *
   * @see ExercisePanel#ExercisePanel
   */
  protected void addInstructions() {
    if (logger == null) logger = Logger.getLogger("WaveformExercisePanel");
//    logger.info("addInstructions - " + exercise.getID() + " " + exercise.isContext());
//    logger.info("addInstructions - parent #" + exercise.asCommon().getParentExerciseID());

    List<String> typeOrder = getTypeOrder();
    int parentExerciseID = exercise.asCommon().getParentExerciseID();

    addDominoProject(typeOrder);
    addParentItem(typeOrder, parentExerciseID);

    DivWidget container2 = new DivWidget();

    add(container2);

    // logger.info("typeOrder " + typeOrder);
    UIObject unitChapterItem = new UnitChapterItemHelper<T>(typeOrder).addUnitChapterItem(exercise, container2);
    if (unitChapterItem != null) {
      unitChapterItem.getElement().getStyle().setMarginTop(-8, Style.Unit.PX);
    }

    add(addDominoHint(container2));

    if (exercise.isContext()) {
      DivWidget container = new DivWidget();
      add(container);
      addHeading();
      if (parentExerciseID == -1) {
        logger.warning("addInstructions : exercise " + exercise.getID() + " has no parent?");
      } else {
        ExerciseServiceAsync<T> exerciseService = controller.getExerciseService();
        exerciseService.getExercise(parentExerciseID, new AsyncCallback<T>() {
          @Override
          public void onFailure(Throwable caught) {

          }

          @Override
          public void onSuccess(T result) {

            HTML maybeRTL = getMaybeRTL(ExerciseFormatter.getArabic(result.getFLToShow(), controller.getLanguageInfo()));
            maybeRTL.addStyleName("numItemFont");
            maybeRTL.getElement().getStyle().setFontStyle(Style.FontStyle.ITALIC);
            container.add(maybeRTL);
          }
        });
      }
    } else {
      addHeading();
    }
  }

  private void addParentItem(List<String> typeOrder, int parentExerciseID) {
    if (exercise.isContext()) {
      typeOrder.add(PARENT_ITEM);
      exercise.getUnitToValue().put(PARENT_ITEM, "" + parentExerciseID);
    }
  }

  private void addDominoProject(List<String> typeOrder) {
    new BasicDialog().addDominoProject(typeOrder, exercise, controller.getProjectID(), controller);
  }

  @NotNull
  private DivWidget addDominoHint(DivWidget container2) {
    DominoLinkNotice dominoLinkNotice = new DominoLinkNotice();
    Anchor anchor = dominoLinkNotice.getAnchor(controller);
    container2.add(anchor);
    HTML hint = dominoLinkNotice.getHint();
    anchor.addMouseOverHandler(event -> hint.getElement().getStyle().setColor("#999999"));
    anchor.addMouseOutHandler(event -> hint.getElement().getStyle().setColor("white"));
    DivWidget cont = new DivWidget();

    cont.add(hint);

    Style style = hint.getElement().getStyle();
    style.setColor("white");
    style.setMarginTop(-5, Style.Unit.PX);
    return cont;
  }

  private void addHeading() {
    boolean normalRecord = isNormalRecord();
    List<ExerciseAttribute> dialogAttributes = getDialogAttributes(exercise);
    //  logger.info("normal " + normalRecord + " attr " + dialogAttributes);
    if (!dialogAttributes.isEmpty()) {
      normalRecord = false;
    }
    add(new Heading(4, normalRecord ? PROMPT_BOTH_SPEEDS : RECORD_PROMPT));
  }

  /**
   * @return
   * @see #getAnswerWidget
   */
  private boolean isNormalRecord() {
    return doNormalRecording;
  }

  @NotNull
  private List<String> getTypeOrder() {
    List<String> typeOrder = new ArrayList<>(controller.getTypeOrder());

    if (exercise != null && exercise.getAttributes() != null) {
      exercise.getAttributes().forEach(exerciseAttribute -> {
        String property = exerciseAttribute.getProperty();
        exercise.getUnitToValue().put(property, exerciseAttribute.getValue());
        if (!typeOrder.contains(property)) {
          typeOrder.add(property);
        }
      });
    }
    return typeOrder;
  }


  @NotNull
  private List<ExerciseAttribute> getDialogAttributes(ClientExercise ex) {
    return ex.getAttributes()
        .stream()
        .filter(exerciseAttribute -> {
          String property = exerciseAttribute.getProperty();
          return (
              property.equalsIgnoreCase(SPEAKER_META_DATA) || property.equalsIgnoreCase(LANGUAGE_META_DATA));
        }).collect(Collectors.toList());
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

//    logger.info("getExerciseContent for " + e.getID() + " context " + e.isContext() + " " + isNormalRecord() +
//        "\n\tprompt " + recordPrompt);

    return ExerciseFormatter.getArabic(recordPrompt, controller.getLanguageInfo());
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

    if (!exercise.isContext() && !hasLanguageAttr(exercise)) {
      AudioType slow = normalRecord ? AudioType.SLOW : AudioType.CONTEXT_SLOW;

//      logger.info("getAnswerWidget audio type " + slow + " user " + controller.getUser() +
//          " \n\tcurrent" + controller.getUserManager().getCurrent());

      UIObject widgets = addRecordAudioPanelNoCaption(exercise, controller, index + 1, vp, slow, false);
      widgets.addStyleName("topFiveMargin");
    }

    return vp;
  }

  private boolean hasLanguageAttr(T ex) {
    return !ex.getAttributes()
        .stream()
        .filter(attr ->
            attr.getProperty().equalsIgnoreCase(DialogMetadata.LANGUAGE.name())
        )
        .collect(Collectors.toSet()).isEmpty();
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
    RecordAudioPanel fast = new RecordAudioPanel<>(exercise, controller, this, index, false,
        audioType, showCurrentRecording);
    audioPanels.add(fast);
    vp.add(fast);

    addAnswerWidget(index, fast);

    if (fast.isAudioPathSet()) {
//      logger.info("found audio path for " +exercise.getRefAudio());
      recordCompleted(fast);
    } else {
      logger.info("addRecordAudioPanelNoCaption no audio path for " + exercise.getID() + " " + exercise.getEnglish() + " " + exercise.getForeignLanguage());
    }

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
    logger.info("postAnswers " + completedExercise.getID());
    showRecordedState();
    exerciseList.loadNextExercise(completedExercise);
  }

  protected void showRecordedState() {
    exerciseList.redraw();
  }
}
