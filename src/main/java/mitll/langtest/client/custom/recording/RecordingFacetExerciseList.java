package mitll.langtest.client.custom.recording;

import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.base.ListItem;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.list.FacetExerciseList;
import mitll.langtest.client.list.ListOptions;
import mitll.langtest.client.list.SelectionState;
import mitll.langtest.shared.answer.ActivityType;
import mitll.langtest.shared.exercise.ExerciseListRequest;
import mitll.langtest.shared.exercise.FilterRequest;
import mitll.langtest.shared.exercise.MatchInfo;
import mitll.langtest.shared.exercise.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

class RecordingFacetExerciseList extends FacetExerciseList {
  public static final String RECORDED = "Recorded";
  private final Logger logger = Logger.getLogger("RecordingFacetExerciseList");
  //private final RecorderNPFHelper helper;

  RecordingFacetExerciseList(ExerciseController controller,
                             Panel topRow,
                             Panel currentExercisePanel,
                             String instanceName,
                             DivWidget listHeader) {
    super(
        topRow,
        currentExercisePanel,
        controller,
        new ListOptions(instanceName)
            .setInstance(instanceName)
            .setShowFirstNotCompleted(true)
            .setActivityType(ActivityType.RECORDER)
        //    .setShowPager(false).
        //  setShowTypeAhead(false)
        , listHeader, true);
    //  this.helper = practiceHelper;
  }

  @NotNull
  @Override
  protected FilterRequest getFilterRequest(int userListID, List<Pair> pairs) {
    return new FilterRequest(incrReqID(), pairs, userListID).setRecordRequest(true);
  }

  @NotNull
  @Override
  protected String getDynamicFacet() {
    return RECORDED;
  }

  protected boolean isDynamicFacetInteger() {
    return false;
  }

  /**
   * @param prefix
   * @return
   */
  @Override
  protected ExerciseListRequest getRequest(String prefix) {
    ExerciseListRequest request = super.getRequest(prefix);
    if (getTypeToSelection().containsKey(RECORDED)) {
      String s = getTypeToSelection().get(RECORDED);
      logger.info("getRequest got selection " + s);
      if (s.startsWith("Unrecord")) {
        logger.info("marked unrecorded");
      }
      request.setOnlyUnrecordedByMe(s.startsWith("Unrecord"));
      request.setOnlyRecordedByMatchingGender(s.startsWith("Record"));

    } else {
      logger.info("getRequest no recorded selection in " + getTypeToSelection().keySet());
    }
 //   Collection<String> remove = request.getTypeToSelection().remove(RECORDED);
 //   logger.info("removed " + remove);
    logger.info("req     " + request);
    return request;
  }

  @Override
  protected SelectionState getSelectionState(String token) {
    SelectionState selectionState = new SelectionState(token, !allowPlusInURL);
    //selectionState.getTypeToSection().remove(RECORDED);
    return selectionState;
  }

  @Override
  protected String getChoiceHandlerValue(String type, String key, int newUserListID) {
    return key;
  }

/*  @NotNull
  protected Map<String, String> getNewTypeToSelection(SelectionState selectionState, Collection<String> typeOrder) {
    typeOrder = new ArrayList<>(typeOrder);
    typeOrder.add(RECORDED);

    return getTypeToSelection(selectionState, typeOrder);
  }*/

  //  @Override
//  protected void addTableWithPager(SimplePagingContainer<?> pagingContainer) {
//    addEventHandler(instanceName);
//  }
  @Override
  protected ListItem addListFacet(Map<String, Set<MatchInfo>> typeToValues) {
    ListItem liForDimensionForType = getTypeContainer(RECORDED);
    Widget favorites = liForDimensionForType.getWidget(0);
    liForDimensionForType.clear();
    liForDimensionForType.add(favorites);
    logger.info("addListFacet --- for " + typeToValues.get(RECORDED) + "  ");
    liForDimensionForType.add(addChoices(typeToValues, RECORDED));
    return liForDimensionForType;


  }
/*  protected void goToFirst(String searchIfAny, int exerciseID) {
    super.goToFirst(searchIfAny, exerciseID);
    if (practiceHelper.getPolyglotFlashcardFactory() != null) {
      practiceHelper.getPolyglotFlashcardFactory().setMode(practiceHelper.getMode());
    }
    getStatsFlashcardFactory().setNavigation(practiceHelper.getNavigation());
  }*/

/*  @Override
  protected void onLastItem() {
    getStatsFlashcardFactory().resetStorage();
  }*/

  /**
   * The issue is there should only be only keyboard focus - either the space bar and prev/next or
   * the search box. - so we should hide the search box.
   *
   * @paramx typeToSection
   * @paramx prefix
   * @paramx exerciseID
   * @paramx onlyWithAudioAnno
   * @paramx onlyUnrecorded
   * @paramx onlyDefaultUser
   * @paramx onlyUninspected
   */
/*  @Override
  protected void loadExercisesUsingPrefix(Map<String, Collection<String>> typeToSection,
                                          String prefix,
                                          int exerciseID, boolean onlyWithAudioAnno,
                                          boolean onlyUnrecorded, boolean onlyDefaultUser, boolean onlyUninspected) {
    super.loadExercisesUsingPrefix(typeToSection, "", exerciseID, onlyWithAudioAnno,
        onlyUnrecorded, onlyDefaultUser, onlyUninspected);
    getStatsFlashcardFactory().setSelection(typeToSection);
  }*/

  void restoreUI(SelectionState selectionState) {
    restoreUIState(selectionState);
  }
}
