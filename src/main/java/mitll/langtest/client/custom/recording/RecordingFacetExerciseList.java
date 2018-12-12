package mitll.langtest.client.custom.recording;

import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.base.ListItem;
import com.github.gwtbootstrap.client.ui.base.UnorderedList;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.custom.INavigation;
import mitll.langtest.client.dialog.ModalInfoDialog;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.list.SelectionState;
import mitll.langtest.shared.dialog.DialogMetadata;
import mitll.langtest.shared.exercise.*;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.logging.Logger;

import static mitll.langtest.client.custom.content.NPFHelper.COMPLETE;
import static mitll.langtest.client.custom.content.NPFHelper.LIST_COMPLETE;

/**
 * For recording items.
 *
 * @see RecorderNPFHelper#getMyListLayout
 */
class RecordingFacetExerciseList<T extends CommonShell & ScoredExercise> extends NoListFacetExerciseList<T> {
  private final Logger logger = Logger.getLogger("RecordingFacetExerciseList");

  private static final String NONE_RECORDED_YET = "None Recorded Yet";
  private static final String ALL_RECORDED = "All Recorded.";
  private static final String LANGUAGE_META_DATA = DialogMetadata.LANGUAGE.name();

  private final boolean isContext;

  /**
   * @param controller
   * @param topRow
   * @param currentExercisePanel
   * @param instanceName
   * @param listHeader
   * @param isContext
   */
  RecordingFacetExerciseList(ExerciseController controller,
                             Panel topRow,
                             Panel currentExercisePanel,
                             INavigation.VIEWS instanceName,
                             DivWidget listHeader,
                             boolean isContext) {
    super(
        controller,
        topRow,
        currentExercisePanel,
        instanceName,
        listHeader,
        instanceName);
    this.isContext = isContext;
  }

  @NotNull
  @Override
  protected String getNoneDoneMessage() {
    return NONE_RECORDED_YET;
  }

  @Override
  @NotNull
  protected String getAllDoneMessage() {
    return ALL_RECORDED;
  }

  @Override
  protected void onLastItem() {
    new ModalInfoDialog(COMPLETE, LIST_COMPLETE, hiddenEvent -> showEmptySelection());
  }

  @Override
  protected String getEmptySearchMessage() {
    return "<b>You've completed recording for this selection.</b>" +
        "<p>Please clear one of your selections and select a different unit or chapter.</p>";
  }

  /**
   * TODO : push down the part about CONTENT and maybe list.
   *
   * @param typeToSelection
   * @return
   * @see #getTypeToValues
   */
  @NotNull
  protected List<Pair> getPairs(Map<String, String> typeToSelection) {
    List<Pair> pairs = super.getPairs(typeToSelection);
    addDynamicFacetToPairs(typeToSelection, LANGUAGE_META_DATA, pairs);
    logger.info("pairs now " + pairs);
    return pairs;
  }

  @Override
  protected ExerciseListRequest getExerciseListRequest(Map<String, Collection<String>> typeToSection,
                                                       String prefix,
                                                       boolean onlyUninspected) {
    ExerciseListRequest exerciseListRequest = super.getExerciseListRequest(typeToSection, prefix, onlyUninspected);
    exerciseListRequest.setOnlyUnrecordedByMe(true);
    return exerciseListRequest;
  }

  /**
   * @param userListID
   * @param pairs
   * @return
   * @see #getTypeToValues
   */
  @NotNull
  @Override
  protected FilterRequest getFilterRequest(int userListID, List<Pair> pairs) {
    return new FilterRequest(incrReqID(), pairs, userListID)
        .setRecordRequest(true)
        .setExampleRequest(isContext);
  }

  /**
   * @param prefix
   * @return
   * @see #getExercises
   */
  @Override
  protected ExerciseListRequest getExerciseListRequest(String prefix) {
    ExerciseListRequest request = super.getExerciseListRequest(prefix);
    request.setOnlyExamples(isContext);
    return request;
  }

  /**
   * From the selection state in the URL.
   *
   * @param selectionState
   * @param typeOrder
   * @return
   * @see #getSectionWidgetContainer
   */
  @NotNull
  @Override
  protected Map<String, String> getNewTypeToSelection(SelectionState selectionState, Collection<String> typeOrder) {
    typeOrder = new ArrayList<>(typeOrder);
    typeOrder.add(LANGUAGE_META_DATA);

    return getTypeToSelection(selectionState, typeOrder);
  }

  @Override
  protected void addDynamicFacets(Map<String, Set<MatchInfo>> typeToValues, UnorderedList allTypesContainer) {
    Set<MatchInfo> matchInfos = typeToValues.get(LANGUAGE_META_DATA);
//    logger.info("addDynamicFacets match infos  " + matchInfos);
//    logger.info("addDynamicFacets typeToValues " + typeToValues);

    if (matchInfos != null && !matchInfos.isEmpty()) {
      addExerciseChoices(LANGUAGE_META_DATA, addContentFacet(allTypesContainer), matchInfos);
    }
  }

  /**
   *
   *
   * @param allTypesContainer
   */
  private ListItem addContentFacet(UnorderedList allTypesContainer) {
    ListItem widgets = addContentFacet();
    allTypesContainer.add(widgets);
    return widgets;
  }

  private ListItem addContentFacet() {
    return getTypeContainer(LANGUAGE_META_DATA, getTypeToSelection().containsKey(LANGUAGE_META_DATA));
  }
}
