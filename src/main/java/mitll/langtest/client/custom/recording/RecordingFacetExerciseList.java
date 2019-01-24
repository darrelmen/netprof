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
import mitll.langtest.shared.project.ProjectType;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.logging.Logger;

/**
 * For recording items.
 *
 * @see RecorderNPFHelper#getMyListLayout
 */
class RecordingFacetExerciseList<T extends CommonShell & ScoredExercise> extends NoListFacetExerciseList<T> {
  private final Logger logger = Logger.getLogger("RecordingFacetExerciseList");

  private static final String NONE_RECORDED_YET = "None Recorded Yet";
  private static final String ALL_RECORDED = "All Recorded.";
  private static final String ANY = "Any";
  /**
   * @see #getPairs(Map)
   */
  private static final List<String> DYNAMIC_FACETS = Arrays.asList(DialogMetadata.LANGUAGE.name(), DialogMetadata.SPEAKER.name());

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
    if (controller.getProjectStartupInfo().getProjectType() == ProjectType.DIALOG) {
      DYNAMIC_FACETS.forEach(facet -> {
        boolean added = addDynamicFacetToPairs(typeToSelection, facet, pairs);
        if (!added) {
          pairs.add(new Pair(facet, ANY));
        }
      });
    }
  //  logger.info("getPairs pairs now " + pairs + " for " + typeToSelection);
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
        .setProjectType(controller.getProjectStartupInfo().getProjectType())
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
    //   logger.info("getExerciseListRequest " + isContext);
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
  protected Map<String, String> getNewTypeToSelection(SelectionState selectionState, final Collection<String> typeOrder) {
    List<String> copy = new ArrayList<>(typeOrder);
    copy.addAll(DYNAMIC_FACETS);
    return getTypeToSelection(selectionState, copy);
  }

  @Override
  protected void addDynamicFacets(Map<String, Set<MatchInfo>> typeToValues, UnorderedList allTypesContainer) {
    DYNAMIC_FACETS.forEach(facet -> addExerciseChoices(typeToValues, allTypesContainer, facet));
  }

  private void addExerciseChoices(Map<String, Set<MatchInfo>> typeToValues, UnorderedList allTypesContainer, String languageMetaData) {
    Set<MatchInfo> matchInfos = typeToValues.get(languageMetaData);
//    logger.info("addDynamicFacets match infos  " + matchInfos);
//    logger.info("addDynamicFacets typeToValues " + typeToValues);
    if (matchInfos != null && !matchInfos.isEmpty()) {
      addExerciseChoices(languageMetaData, addContentFacet(allTypesContainer, languageMetaData), matchInfos);
    }
  }

  /**
   * @param allTypesContainer
   */
  private ListItem addContentFacet(UnorderedList allTypesContainer, String facet) {
    ListItem widgets = addContentFacet(facet);
    allTypesContainer.add(widgets);
    return widgets;
  }

  private ListItem addContentFacet(String facet) {
    return getTypeContainer(facet);
  }

  @NotNull
  private ListItem getTypeContainer(String languageMetaData) {
    return getTypeContainer(languageMetaData, getTypeToSelection().containsKey(languageMetaData));
  }
}
