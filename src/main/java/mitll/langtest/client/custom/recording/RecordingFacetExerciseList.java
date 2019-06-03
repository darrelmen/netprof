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
import mitll.langtest.shared.project.ProjectMode;
import mitll.langtest.shared.project.ProjectType;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.logging.Logger;

/**
 * For recording items.
 *
 * @see RecorderNPFHelper#getMyListLayout
 */
public class RecordingFacetExerciseList<T extends CommonShell & ScoredExercise> extends NoListFacetExerciseList<T> {
  private final Logger logger = Logger.getLogger("RecordingFacetExerciseList");

  private static final String NONE_RECORDED_YET = "None Recorded Yet";
  private static final String ALL_RECORDED = "All Recorded.";
  private static final String ANY = "Any";
  /**
   * @see #getPairs(Map)
   */
  private static final List<String> DYNAMIC_FACETS =
      Arrays.asList(DialogMetadata.LANGUAGE.name(), DialogMetadata.SPEAKER.name());

  /**
   * @see #getFilterRequest(int, List)
   */
  protected final boolean isContext;
  private boolean isDialog;

  private static final boolean DEBUG = false;

  /**
   * @param controller
   * @param topRow
   * @param currentExercisePanel
   * @param instanceName
   * @param listHeader
   * @param isContext
   */
  public RecordingFacetExerciseList(ExerciseController controller,
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
    isDialog = isDialog() && controller.getMode() == ProjectMode.DIALOG;
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
    if (isDialog) {
      DYNAMIC_FACETS.forEach(facet -> {
        boolean added = addDynamicFacetToPairs(typeToSelection, facet, pairs);
        if (!added) {
          pairs.add(new Pair(facet, ANY));
        }
      });
    }
    //logger.info("getPairs pairs now " + pairs + " for " + typeToSelection);
    return pairs;
  }

  private boolean isDialog() {
    ProjectType projectType = controller.getProjectStartupInfo().getProjectType();
   // logger.info("isDialog project type " + controller.getProjectStartupInfo());
    return projectType == ProjectType.DIALOG;
  }

  @Override
  protected ExerciseListRequest getExerciseListRequest(Map<String, Collection<String>> typeToSection,
                                                       String prefix,
                                                       boolean onlyUninspected) {
  //  logger.info("getExerciseListRequest type->sel " +typeToSection);
    ExerciseListRequest exerciseListRequest = super.getExerciseListRequest(typeToSection, prefix, onlyUninspected);
    exerciseListRequest.setOnlyUnrecordedByMe(true).setMode(controller.getMode());
  //  logger.info("getExerciseListRequest exerciseListRequest " +exerciseListRequest);
   // logger.info("getExerciseListRequest type->sel " +exerciseListRequest.getTypeToSelection());
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
        .setMode(controller.getMode())
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
    ProjectMode mode = controller.getMode();
    request.setOnlyExamples(isContext).setMode(mode);
   // logger.info("getExerciseListRequest isContext = " + isContext);
    if (DEBUG) logger.info("getExerciseListRequest req " + request);
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
    if (isDialog) {
      List<String> copy = new ArrayList<>(typeOrder);
      copy.addAll(DYNAMIC_FACETS);
      return getTypeToSelection(selectionState, copy);
    } else {
      return super.getNewTypeToSelection(selectionState, typeOrder);
    }
  }

  @Override
  protected void addDynamicFacets(Map<String, Set<MatchInfo>> typeToValues, UnorderedList allTypesContainer) {
    if (isDialog) {
      DYNAMIC_FACETS.forEach(facet -> addExerciseChoices(typeToValues, allTypesContainer, facet));
    }
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
    ListItem widgets = getTypeContainer(facet);
    allTypesContainer.add(widgets);
    return widgets;
  }

  @NotNull
  private ListItem getTypeContainer(String languageMetaData) {
    return getTypeContainer(languageMetaData, getTypeToSelection().containsKey(languageMetaData));
  }
}
