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

package mitll.langtest.client.custom;

import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.custom.content.FlexListLayout;
import mitll.langtest.client.custom.content.NPFlexSectionExerciseList;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.ExercisePanelFactory;
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
class PracticeHelper extends SimpleChapterNPFHelper<CommonShell, CommonExercise> {
  private final Logger logger = Logger.getLogger("PracticeHelper");

  private StatsFlashcardFactory<CommonShell, CommonExercise> statsFlashcardFactory;
  private Widget outerBottomRow;

  PracticeHelper(ExerciseController controller) { super(controller, null);  }

  @Override
  protected ExercisePanelFactory<CommonShell, CommonExercise> getFactory(PagingExerciseList<CommonShell, CommonExercise> exerciseList) {
    statsFlashcardFactory = new StatsFlashcardFactory<>( controller, exerciseList, "practice", null);
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
    return new MyFlexListLayout<CommonShell, CommonExercise>( controller, outer) {
      final FlexListLayout outer = this;

      @Override
      protected PagingExerciseList<CommonShell, CommonExercise> makeExerciseList(Panel topRow,
                                                                                 Panel currentExercisePanel,
                                                                                 String instanceName, DivWidget listHeader) {
        return new NPFlexSectionExerciseList(outer.getController(), topRow, currentExercisePanel, new ListOptions(instanceName), listHeader, 1) {
          @Override
          protected CommonShell findFirstExercise() {
            int currentExerciseID = statsFlashcardFactory.getCurrentExerciseID();
            if (currentExerciseID != -1) {//null && !currentExerciseID.trim().isEmpty()) {
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
          protected void loadExercisesUsingPrefix(Map<String, Collection<String>> typeToSection,
                                                  String prefix,
                                                  int exerciseID, boolean onlyWithAudioAnno,
                                                  boolean onlyUnrecorded, boolean onlyDefaultUser, boolean onlyUninspected) {
//            logger.info("got loadExercisesUsingPrefix " +prefix);
            //  controller.logException(new Exception("where did this come from?"));
            //prefix = ""; // practice helper doesn't use a search box
            super.loadExercisesUsingPrefix(typeToSection, "", exerciseID, onlyWithAudioAnno, onlyUnrecorded, onlyDefaultUser, onlyUninspected);
            statsFlashcardFactory.setSelection(typeToSection);
          }
        };
      }

      @Override
      protected void styleBottomRow(Panel bottomRow) {
        //    logger.info("-----\n\n Adding style to " + bottomRow.getElement().getExID());
        bottomRow.addStyleName("centerPractice");
        outerBottomRow = bottomRow;
      }
    };
  }
}
