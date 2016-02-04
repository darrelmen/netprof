package mitll.langtest.client.custom;

import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.custom.content.FlexListLayout;
import mitll.langtest.client.custom.content.NPFlexSectionExerciseList;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.ExercisePanelFactory;
import mitll.langtest.client.flashcard.StatsFlashcardFactory;
import mitll.langtest.client.list.PagingExerciseList;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.client.user.UserManager;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.CommonShell;

import java.util.Collection;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Created by go22670 on 2/4/16.
 */
class PracticeHelper extends SimpleChapterNPFHelper<CommonShell, CommonExercise> {
  private final Logger logger = Logger.getLogger("PracticeHelper");

  //  private Navigation navigation;
  private StatsFlashcardFactory<CommonShell, CommonExercise> statsFlashcardFactory;
  private Widget outerBottomRow;

  public PracticeHelper(//Navigation navigation,
                        LangTestDatabaseAsync service, UserFeedback feedback, UserManager userManager, ExerciseController controller) {
    super(service, feedback, userManager, controller, null);
    // this.navigation = navigation;
  }

  @Override
  protected ExercisePanelFactory<CommonShell, CommonExercise> getFactory(PagingExerciseList<CommonShell, CommonExercise> exerciseList) {
    statsFlashcardFactory = new StatsFlashcardFactory<>(service, feedback, controller, exerciseList, "practice", null);
    statsFlashcardFactory.setContentPanel(outerBottomRow);
    return statsFlashcardFactory;
  }

  @Override
  public void onResize() {
    super.onResize();
    if (statsFlashcardFactory != null) {
      statsFlashcardFactory.onResize();
    }
  }

  @Override
  protected FlexListLayout<CommonShell, CommonExercise> getMyListLayout(LangTestDatabaseAsync service, UserFeedback feedback,
                                                                        UserManager userManager, ExerciseController controller,
                                                                        SimpleChapterNPFHelper<CommonShell, CommonExercise> outer) {
    return new MyFlexListLayout<CommonShell, CommonExercise>(service, feedback, controller, outer) {
      final FlexListLayout outer = this;

      @Override
      protected PagingExerciseList<CommonShell, CommonExercise> makeExerciseList(Panel topRow,
                                                                                 Panel currentExercisePanel,
                                                                                 String instanceName,
                                                                                 boolean incorrectFirst) {
        return new NPFlexSectionExerciseList(outer, topRow, currentExercisePanel, instanceName, true) {
          @Override
          protected CommonShell findFirstExercise() {
            String currentExerciseID = statsFlashcardFactory.getCurrentExerciseID();
            if (currentExerciseID != null && !currentExerciseID.trim().isEmpty()) {
              logger.info("findFirstExercise ---> found previous state current ex = " + currentExerciseID);

              CommonShell shell = byID(currentExerciseID);

              if (shell == null) {
                logger.warning("huh? can't find " + currentExerciseID);
                return super.findFirstExercise();
              } else {
                statsFlashcardFactory.populateCorrectMap();
                return shell;
              }
            } else {
              return super.findFirstExercise();
            }
          }

          @Override
          protected void onLastItem() {
            statsFlashcardFactory.resetStorage();
          }

          @Override
          protected void loadExercisesUsingPrefix(Map<String, Collection<String>> typeToSection, String prefix,
                                                  boolean onlyWithAudioAnno) {
            super.loadExercisesUsingPrefix(typeToSection, prefix, onlyWithAudioAnno);
            statsFlashcardFactory.setSelection(typeToSection);
          }
        };
      }

      @Override
      protected void styleBottomRow(Panel bottomRow) {
        //    logger.info("-----\n\n Adding style to " + bottomRow.getElement().getId());
        bottomRow.addStyleName("centerPractice");
        outerBottomRow = bottomRow;
      }
    };
  }
}
