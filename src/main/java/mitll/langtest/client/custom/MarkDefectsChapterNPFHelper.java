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

import com.github.gwtbootstrap.client.ui.CheckBox;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.LangTest;
import mitll.langtest.client.custom.content.FlexListLayout;
import mitll.langtest.client.exercise.DefectEvent;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.ExercisePanelFactory;
import mitll.langtest.client.exercise.SimplePagingContainer;
import mitll.langtest.client.list.*;
import mitll.langtest.client.qc.QCNPFExercise;
import mitll.langtest.shared.answer.ActivityType;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.CommonShell;
import mitll.langtest.shared.exercise.ExerciseListRequest;

import java.util.Collection;
import java.util.Map;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 3/30/16.
 */
public class MarkDefectsChapterNPFHelper extends SimpleChapterNPFHelper<CommonShell, CommonExercise> {
//  private final Logger logger = Logger.getLogger("MarkDefectsChapterNPFHelper");
  private static final String SHOW_ONLY_UNINSPECTED_ITEMS = "Show Only Uninspected Items.";
  private static final String SHOW_ONLY_AUDIO_BY_UNKNOWN_GENDER = "Show Only Audio by Unknown Gender";

  /**
   * @param controller
   * @param learnHelper
   * @see
   */
  public MarkDefectsChapterNPFHelper(ExerciseController controller, SimpleChapterNPFHelper learnHelper) {
    super(controller, learnHelper);
  }

  /**
   * Adds two checkboxes to filter for uninspected items and items with audio that's old enough it's not marked
   * by gender.
   *
   * @param outer
   * @return
   */
  @Override
  protected FlexListLayout<CommonShell, CommonExercise> getMyListLayout(SimpleChapterNPFHelper<CommonShell, CommonExercise> outer) {
    return new MyFlexListLayout<CommonShell, CommonExercise>(controller, outer) {
      protected void styleTopRow(Panel twoRows, Panel topRow) {
        twoRows.add(topRow);
      }

      protected void styleBottomRow(Panel bottomRow) {
        bottomRow.setWidth("100%");
        bottomRow.addStyleName("inlineFlex");
      }

      @Override
      protected PagingExerciseList<CommonShell, CommonExercise> makeExerciseList(Panel topRow,
                                                                                 Panel currentExercisePanel,
                                                                                 String instanceName,
                                                                                 DivWidget listHeader, DivWidget footer) {
//        logger.info("instance is " + instanceName);
        return new NPExerciseList(currentExercisePanel,
            controller,
            new ListOptions().setInstance(instanceName)) {
          private CheckBox filterOnly, uninspectedOnly;

          @Override
          protected ExerciseListRequest getExerciseListRequest(Map<String, Collection<String>> typeToSection, String prefix, boolean onlyWithAudioAnno, boolean onlyUnrecorded, boolean onlyDefaultUser, boolean onlyUninspected) {
            return super.getExerciseListRequest(typeToSection, prefix, onlyWithAudioAnno, onlyUnrecorded, onlyDefaultUser, onlyUninspected).setQC(true);
          }

          @Override
          protected void addTableWithPager(SimplePagingContainer<?> pagingContainer) {
            // row 1
            Panel column = new FlowPanel();
            add(column);
            addTypeAhead(column);

            // row 2
            add(filterOnly = getFilterCheckbox());
            add(uninspectedOnly = getUninspectedCheckbox());

            // row 3
            add(pagingContainer.getTableWithPager(new ListOptions()));

            addEventHandler(instanceName, this);
          }

          /**
           * @see  PagingExerciseList#addTableWithPager
           */
          private CheckBox getFilterCheckbox() {
            return addFilter(SHOW_ONLY_AUDIO_BY_UNKNOWN_GENDER);
          }

          private CheckBox addFilter(String title) {
            CheckBox filterOnly = new CheckBox(title);
            filterOnly.addClickHandler(event -> pushNewSectionHistoryToken());
            filterOnly.addStyleName("leftFiveMargin");
            return filterOnly;
          }

          private CheckBox getUninspectedCheckbox() {
            return addFilter(SHOW_ONLY_UNINSPECTED_ITEMS);
          }

          /**
           * @see mitll.langtest.client.list.HistoryExerciseList#getHistoryToken
           * @param search
           * @param id
           * @return
           */
          protected String getHistoryTokenFromUIState(String search, int id) {
            String s = super.getHistoryTokenFromUIState(search, id) + SelectionState.SECTION_SEPARATOR +
                SelectionState.ONLY_DEFAULT + "=" + filterOnly.getValue() + SelectionState.SECTION_SEPARATOR +
                SelectionState.ONLY_UNINSPECTED + "=" + uninspectedOnly.getValue();
            return s;
          }

          @Override
          protected void restoreUIState(SelectionState selectionState) {
            super.restoreUIState(selectionState);
            filterOnly.setValue(selectionState.isOnlyDefault());
            uninspectedOnly.setValue(selectionState.isOnlyUninspected());
          }
        };
      }
    };
  }

  /**
   * So if you fix a defect in the fix defects tab, want it to be reflected here in mark defects.
   * For instance if you mark a defect here, and fix it there, coming back here, if you filter
   * for uninspected, the item should not be there.
   *
   * @param instanceName
   * @param container
   * @see mitll.langtest.client.custom.dialog.ReviewEditableExercise#afterValidForeignPhrase
   */
  private void addEventHandler(final String instanceName, HistoryExerciseList container) {
    LangTest.EVENT_BUS.addHandler(DefectEvent.TYPE, authenticationEvent -> {
      if (authenticationEvent.getSource().equals(instanceName)) {
        //logger.info("skip self event from " + instanceName);
      } else {
        //    logger.info("---> got defect event " + instanceName);
        container.reloadFromState();
      }
    });
  }

  protected ExercisePanelFactory<CommonShell, CommonExercise> getFactory(final PagingExerciseList<CommonShell, CommonExercise> exerciseList) {
    return new ExercisePanelFactory<CommonShell, CommonExercise>(controller, exerciseList) {
      @Override
      public Panel getExercisePanel(CommonExercise e) {
        return new QCNPFExercise<>(e, controller, exerciseList, ActivityType.MARK_DEFECTS.toString());
      }
    };
  }
}
