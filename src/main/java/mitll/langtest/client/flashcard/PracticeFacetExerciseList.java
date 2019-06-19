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

package mitll.langtest.client.flashcard;

import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.banner.PracticeHelper;
import mitll.langtest.client.custom.INavigation;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.list.ListFacetExerciseList;
import mitll.langtest.client.list.ListOptions;
import mitll.langtest.client.list.SelectionState;
import mitll.langtest.shared.exercise.ClientExercise;
import mitll.langtest.shared.exercise.CommonShell;
import mitll.langtest.shared.exercise.ScoredExercise;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Only show one item at a time, for flashcards.
 * Don't show the search box, since it competes for focus with the space bar and arrow key navigation for the cards.
 *
 * @param <T>
 * @param <U>
 */
public class PracticeFacetExerciseList<T extends CommonShell & ScoredExercise, U extends ClientExercise>
    extends ListFacetExerciseList<T> {
  private final Logger logger = Logger.getLogger("PracticeFacetExerciseList");
  private final PracticeHelper<T> practiceHelper;
  private ControlState controlState;

  private static final boolean DEBUG = false;

  public PracticeFacetExerciseList(Panel topRow,
                                   Panel currentExercisePanel,
                                   ExerciseController controller,
                                   ListOptions listOptions,
                                   DivWidget listHeader,
                                   INavigation.VIEWS views,
                                   PracticeHelper<T> practiceHelper) {
    super(topRow,
        currentExercisePanel,
        controller,
        listOptions.setShowPager(false).setShowTypeAhead(false),
        listHeader,
        views);
    this.practiceHelper = practiceHelper;
  }

  /**
   * @param state
   * @see StatsFlashcardFactory#StatsFlashcardFactory
   */
  void setControlState(ControlState state) {
    this.controlState = state;
  }

  /**
   * Turn off auto advance if not visible any more.
   * Thanks Reina!
   */
  @Override
  protected void onDetach() {
    super.onDetach();
// logger.info("\n\ngot detach ---> \n\n\n");
    if (controlState.isAutoPlay()) {
      // logger.info("onDetach : audio on, so turn auto advance OFF");
      controlState.setAutoPlay(false);
    }
  }

  @Override
  protected int getPageIndex() {
    return 0;
  }

  protected void goToFirst(String searchIfAny, int exerciseID) {
    //logger.info("\ngoToFirst ---> \n\n\n");
    super.goToFirst(searchIfAny, exerciseID);

    if (practiceHelper != null) {
      if (getStatsFlashcardFactory() != null) {
        getStatsFlashcardFactory().setNavigation(practiceHelper.getNavigation());
      }
    }
  }

  /**
   * @see #loadNextExercise
   */
  @Override
  protected void onLastItem() {
    if (getStatsFlashcardFactory() != null) {
      getStatsFlashcardFactory().resetStorage();
    }
  }

  /**
   * NOTE:
   *
   * The issue is there should only be only keyboard focus - either the space bar and prev/next or
   * the search box. - so we should hide the search box.
   *
   * @param typeToSection
   * @param prefix
   * @param exerciseID
   * @param onlyWithAudioAnno
   * @param onlyDefaultUser
   * @param onlyUninspected
   */
  @Override
  protected void loadExercisesUsingPrefix(Map<String, Collection<String>> typeToSection,
                                          String prefix,
                                          int exerciseID, boolean onlyWithAudioAnno,
                                          boolean onlyDefaultUser, boolean onlyUninspected) {
    super.loadExercisesUsingPrefix(typeToSection, "", exerciseID, onlyWithAudioAnno, onlyDefaultUser, onlyUninspected);

    StatsFlashcardFactory<T, ClientExercise> statsFlashcardFactory = getStatsFlashcardFactory();
    if (statsFlashcardFactory != null) {
      statsFlashcardFactory.setSelection(typeToSection);
    }
  }

  private StatsFlashcardFactory<T, ClientExercise> getStatsFlashcardFactory() {
    return practiceHelper == null ? null : practiceHelper.getStatsFlashcardFactory();
  }

  @Override
  protected void getVisibleExercises(final Collection<Integer> visibleIDs, final int currentReq) {
    checkAndGetExercises(visibleIDs, currentReq);
  }

  @Override
  protected Collection<Integer> getVisibleForSingleItemList(int itemID, Collection<Integer> visibleIDs) {
    if (DEBUG) {
      logger.info("getVisibleForSingleItemList " + visibleIDs.size() +
          " visible ids : " + visibleIDs + " itemID " + itemID);
    }

    if (itemID > 0) {
      visibleIDs = new ArrayList<>();
      visibleIDs.add(itemID);
    }
    return visibleIDs;
  }

  @Override
  protected void reallyGetExercises(Collection<Integer> visibleIDs, final int currentReq) {
    if (DEBUG) {
      logger.info("reallyGetExercises " + visibleIDs.size() + " visible ids : " + visibleIDs + " currentReq " + currentReq);
    }

    if (visibleIDs.isEmpty()) {
      CommonShell currentExercise = getCurrentExercise();
      if (currentExercise != null) {
        int id = currentExercise.getID();
        if (!visibleIDs.contains(id)) {
          visibleIDs.add(id);
          logger.warning("reallyGetExercises added current ex to visible " + id);
        }
      }
//      logger.info("\n\n\treallyGetExercises now " + visibleIDs.size() + " visible ids : " + visibleIDs + " currentReq " + currentReq);
    }
    super.reallyGetExercises(visibleIDs, currentReq);
  }

  @Override
  protected void showExercises(final Collection<ClientExercise> result, final int reqID) {
    if (DEBUG) {
      logger.info("showExercises " + result.size() + " currentReq " + reqID);
    }

    showOnlySortBox();
    showOnlyOne(result);
  }

  protected void showOnlyOne(Collection<ClientExercise> result) {
    showOnlyOneExercise(result);
    goGetNextPage();
    setProgressVisible(false);
  }

  /**
   * @param result
   * @see #showExercises
   */
  private void showOnlyOneExercise(Collection<ClientExercise> result) {
    if (DEBUG) {
      logger.info("showOnlyOneExercise " + result.size());
    }

    ClientExercise next = result.iterator().next();
    int id = next.getID();
    if (DEBUG) {
      logger.info("showOnlyOneExercise id " + id);
    }
    markCurrentExercise(id);
    addExerciseWidget(next);
  }

  @Override
  protected void loadFromSelectionState(SelectionState selectionState, SelectionState newState) {
    loadExercisesUsingPrefix(
        newState.getTypeToSection(),
        selectionState.getSearch(),
        selectionState.getItem(),

        newState.isOnlyWithAudioDefects(),
        newState.isOnlyDefault(),
        newState.isOnlyUninspected());
  }
}
