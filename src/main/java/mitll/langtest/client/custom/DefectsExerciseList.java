package mitll.langtest.client.custom;

import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.custom.recording.NoListFacetExerciseList;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.list.HistoryExerciseList;
import mitll.langtest.client.list.PagingExerciseList;
import mitll.langtest.shared.exercise.*;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

class DefectsExerciseList<T extends CommonShell & ScoredExercise> extends NoListFacetExerciseList<T> {
  private final Logger logger = Logger.getLogger("DefectsExerciseList");

  //  private MarkDefectsChapterNPFHelper markDefectsChapterNPFHelper;
  //Logger logger = Logger.getLogger("NPExerciseList_Defects");
  // private CheckBox filterOnly, uninspectedOnly;
  private boolean isContext;

   DefectsExerciseList(ExerciseController controller,
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

  /**
   * @param typeToSection
   * @param prefix
   * @param onlyWithAudioAnno
   * @param onlyDefaultUser
   * @param onlyUninspected
   * @return
   * @see HistoryExerciseList#loadExercisesUsingPrefix
   */
  @Override
  protected ExerciseListRequest getExerciseListRequest(Map<String, Collection<String>> typeToSection,
                                                       String prefix,
                                                       boolean onlyWithAudioAnno,
                                                       boolean onlyDefaultUser,
                                                       boolean onlyUninspected) {

    ExerciseListRequest exerciseListRequest = super
        .getExerciseListRequest(typeToSection, prefix, onlyWithAudioAnno, onlyDefaultUser, onlyUninspected)
        .setQC(true)
        .setAddContext(isContext);

    logger.info("getExerciseListRequest req " + exerciseListRequest);
    return exerciseListRequest;
  }

  protected ExerciseListRequest getExerciseListRequest(String prefix) {
    ExerciseListRequest exerciseListRequest =
        super.getExerciseListRequest(prefix).setQC(true).setAddContext(isContext);
    logger.info("getExerciseListRequest prefix req " + exerciseListRequest);
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
        .setOnlyUninspected(true)
        .setExampleRequest(isContext);
  }

/*  @Override
  protected void addTableWithPager(SimplePagingContainer<?> pagingContainer) {
    // row 1
    Panel column = new FlowPanel();
    add(column);
    addTypeAhead(column);

    // row 2
    add(uninspectedOnly = getUninspectedCheckbox());
    add(filterOnly = getFilterCheckbox());

    // row 3
    add(pagingContainer.getTableWithPager(new ListOptions()));
  }*/

  /**
   * @see  PagingExerciseList#addTableWithPager
   */
/*  private CheckBox getFilterCheckbox() {
    return addFilter(MarkDefectsChapterNPFHelper.SHOW_ONLY_AUDIO_BY_UNKNOWN_GENDER);
  }

  private CheckBox addFilter(String title) {
    CheckBox filterOnly = new CheckBox(title);
    filterOnly.addClickHandler(event -> pushNewSectionHistoryToken());
    filterOnly.addStyleName("leftFiveMargin");
    return filterOnly;
  }

  private CheckBox getUninspectedCheckbox() {
    return addFilter(MarkDefectsChapterNPFHelper.SHOW_ONLY_UNINSPECTED_ITEMS);
  }*/

  /**
   * @see HistoryExerciseList#getHistoryToken
   * @param search
   * @param id
   * @return
   */
/*  protected String getHistoryTokenFromUIState(String search, int id) {
    return
        super.getHistoryTokenFromUIState(search, id) + SelectionState.SECTION_SEPARATOR +
            SelectionState.ONLY_DEFAULT + "=" + filterOnly.getValue() + SelectionState.SECTION_SEPARATOR +
            SelectionState.ONLY_UNINSPECTED + "=" + uninspectedOnly.getValue();
  }

  @Override
  protected void restoreUIState(SelectionState selectionState) {
    super.restoreUIState(selectionState);
    filterOnly.setValue(selectionState.isOnlyDefault());
    uninspectedOnly.setValue(selectionState.isOnlyUninspected());
  }*/
}
