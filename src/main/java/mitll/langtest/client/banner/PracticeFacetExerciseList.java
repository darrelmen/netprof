package mitll.langtest.client.banner;

import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.custom.INavigation;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.flashcard.ControlState;
import mitll.langtest.client.flashcard.StatsFlashcardFactory;
import mitll.langtest.client.list.LearnFacetExerciseList;
import mitll.langtest.client.list.ListOptions;
import mitll.langtest.shared.exercise.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.logging.Logger;

public class PracticeFacetExerciseList<T extends CommonShell & ScoredExercise, U extends ClientExercise>
    extends LearnFacetExerciseList<T> {
  private final Logger logger = Logger.getLogger("PracticeFacetExerciseList");
  private final PracticeHelper<T, U> practiceHelper;
  private ControlState controlState;

  protected PracticeFacetExerciseList(ExerciseController controller,
                                      PracticeHelper<T, U> practiceHelper,
                                      Panel topRow,
                                      Panel currentExercisePanel,
                                      ListOptions listOptions,
                                      DivWidget listHeader,
                                      INavigation.VIEWS views) {
    super(
        topRow,
        currentExercisePanel,
        controller,
        listOptions.setShowPager(false).setShowTypeAhead(false),
        listHeader,
        views);
    this.practiceHelper = practiceHelper;
  }

  public void setControlState(ControlState state) {
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
      if (practiceHelper.getPolyglotFlashcardFactory() != null) {
        practiceHelper.getPolyglotFlashcardFactory().setMode(practiceHelper.getMode());
      }
      if (getStatsFlashcardFactory() != null) {
        getStatsFlashcardFactory().setNavigation(practiceHelper.getNavigation());
      }
    }
  }

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

    StatsFlashcardFactory<T, U> statsFlashcardFactory = getStatsFlashcardFactory();
    if (statsFlashcardFactory != null) {
      statsFlashcardFactory.setSelection(typeToSection);
    }
  }

  private StatsFlashcardFactory<T, U> getStatsFlashcardFactory() {
    return practiceHelper == null ? null : practiceHelper.getStatsFlashcardFactory();
  }

  @Override
  protected void gotRangeChanged() {

  }

  @Override
  protected void getVisibleExercises(final Collection<Integer> visibleIDs, final int currentReq) {
    checkAndGetExercises(visibleIDs, currentReq);
  }

  @Override
  protected Collection<Integer> getVisibleForDrill(int itemID, Collection<Integer> visibleIDs) {
    if (itemID > 0) {
      visibleIDs = new ArrayList<>();
      visibleIDs.add(itemID);
    }
    return visibleIDs;
  }

  @Override
  protected void reallyGetExercises(Collection<Integer> visibleIDs, final int currentReq) {
    if (visibleIDs.isEmpty()) {
      CommonShell currentExercise = getCurrentExercise();
      if (currentExercise != null) {
        int id = currentExercise.getID();
        if (!visibleIDs.contains(id)) {
          visibleIDs.add(id);
          logger.warning("reallyGetExercises added current ex to visible " + id);
/*
          int c = 0;
          for (Integer id2 : visibleIDs) {
            logger.info("#" + c++ + " : " + id2);
          }
*/
        }
      }

      logger.info("\n\n\treallyGetExercises now " + visibleIDs.size() + " visible ids : " + visibleIDs + " currentReq " + currentReq);

    }
    super.reallyGetExercises(visibleIDs, currentReq);
  }

  @Override
  protected void showExercises(final Collection<ClientExercise> result, final int reqID) {
    hidePrevNextWidgets();
    showDrill(result);
    goGetNextPage();
    setProgressVisible(false);
  }

  /**
   * @param result
   * @see #showExercises
   */
  protected void showDrill(Collection<ClientExercise> result) {
    ClientExercise next = result.iterator().next();
    markCurrentExercise(next.getID());
    addExerciseWidget(next);
  }
}
