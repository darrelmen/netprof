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

package mitll.langtest.client.banner;

import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.custom.SimpleChapterNPFHelper;
import mitll.langtest.client.custom.content.FlexListLayout;
import mitll.langtest.client.custom.content.NPFlexSectionExerciseList;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.ExercisePanelFactory;
import mitll.langtest.client.flashcard.PolyglotDialog;
import mitll.langtest.client.flashcard.StatsFlashcardFactory;
import mitll.langtest.client.list.ListOptions;
import mitll.langtest.client.list.PagingExerciseList;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.CommonShell;

import java.util.Collection;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 2/4/16.
 */
public class PracticeHelper extends SimpleChapterNPFHelper<CommonShell, CommonExercise> {
  //private final Logger logger = Logger.getLogger("PracticeHelper");

  private StatsFlashcardFactory<CommonShell, CommonExercise> statsFlashcardFactory;
  private Widget outerBottomRow;
  private PolyglotDialog.MODE_CHOICE mode;
  private PolyglotDialog.PROMPT_CHOICE promptChoice;
  private NewContentChooser navigation;

  /**
   * @param controller
   * @see NewContentChooser#NewContentChooser(ExerciseController, IBanner)
   */
  PracticeHelper(ExerciseController controller) {
    super(controller);
  }

  /**
   * @param exerciseList
   * @return
   * @see SimpleChapterNPFHelper.MyFlexListLayout#getFactory
   */
  @Override
  protected ExercisePanelFactory<CommonShell, CommonExercise> getFactory(PagingExerciseList<CommonShell, CommonExercise> exerciseList) {
    statsFlashcardFactory = new StatsFlashcardFactory<>(controller, exerciseList, "practice", null);
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
  protected FlexListLayout<CommonShell, CommonExercise> getMyListLayout(SimpleChapterNPFHelper<CommonShell, CommonExercise> outer) {
    return new MyFlexListLayout<CommonShell, CommonExercise>(controller, outer) {
      final FlexListLayout outer = this;

      @Override
      protected PagingExerciseList<CommonShell, CommonExercise> makeExerciseList(Panel topRow,
                                                                                 Panel currentExercisePanel,
                                                                                 String instanceName, DivWidget listHeader, DivWidget footer) {
        return new NPFlexSectionExerciseList(outer.getController(), topRow, currentExercisePanel,
            new ListOptions(instanceName)
                .setShowPager(false).
                setShowTypeAhead(false),
            listHeader,
            true // TODO : horrible hack
        ) {

          protected void goToFirst(String searchIfAny, int exerciseID) {
            super.goToFirst(searchIfAny, exerciseID);
            statsFlashcardFactory.setMode(mode, promptChoice);
            statsFlashcardFactory.setNavigation(navigation);
          }

          protected void gotVisibleRangeChanged(Collection<Integer> idsForRange, int currrentReq) {
          }

          @Override
          protected void onLastItem() {
            statsFlashcardFactory.resetStorage();
          }

          /**
           * The issue is there should only be only keyboard focus - either the space bar and prev/next or
           * the search box. - so we should hide the search box.
           *
           * @param typeToSection
           * @param prefix
           * @param exerciseID
           * @param onlyWithAudioAnno
           * @param onlyUnrecorded
           * @param onlyDefaultUser
           * @param onlyUninspected
           */
          @Override
          protected void loadExercisesUsingPrefix(Map<String, Collection<String>> typeToSection,
                                                  String prefix,
                                                  int exerciseID, boolean onlyWithAudioAnno,
                                                  boolean onlyUnrecorded, boolean onlyDefaultUser, boolean onlyUninspected) {
            //  logger.info("getMyListLayout : got loadExercisesUsingPrefix " +prefix + " WERE NOT USING PREFIX");
            super.loadExercisesUsingPrefix(typeToSection, "", exerciseID, onlyWithAudioAnno, onlyUnrecorded, onlyDefaultUser, onlyUninspected);
            statsFlashcardFactory.setSelection(typeToSection);
          }
        };
      }

      @Override
      protected void styleBottomRow(Panel bottomRow) {
        bottomRow.addStyleName("centerPractice");
        outerBottomRow = bottomRow;
      }
    };
  }

  public void setMode(PolyglotDialog.MODE_CHOICE mode, PolyglotDialog.PROMPT_CHOICE promptChoice) {
    this.mode = mode;
    this.promptChoice = promptChoice;
    if (statsFlashcardFactory != null) {
      statsFlashcardFactory.setMode(mode, promptChoice);
    }
  }

  public void setNavigation(NewContentChooser navigation) {
    this.navigation = navigation;
  }
}
