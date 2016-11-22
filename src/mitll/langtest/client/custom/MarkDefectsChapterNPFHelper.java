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
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.LangTest;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.custom.content.FlexListLayout;
import mitll.langtest.client.custom.content.NPFlexSectionExerciseList;
import mitll.langtest.client.exercise.*;
import mitll.langtest.client.list.PagingExerciseList;
import mitll.langtest.client.list.SelectionState;
import mitll.langtest.client.qc.QCNPFExercise;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.client.user.UserManager;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.CommonShell;

import java.util.logging.Logger;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 3/30/16.
 */
class MarkDefectsChapterNPFHelper extends SimpleChapterNPFHelper<CommonShell, CommonExercise> {
  private final Logger logger = Logger.getLogger("MarkDefectsChapterNPFHelper");

  private static final String SHOW_ONLY_AUDIO_BY_UNKNOWN_GENDER = "Show Only Audio by Unknown Gender";
  private static final String MARK_DEFECTS1 = "markDefects";

  /**
   * @param service
   * @param feedback
   * @param userManager
   * @param controller
   * @param learnHelper
   * @see Navigation#Navigation(LangTestDatabaseAsync, UserManager, ExerciseController, UserFeedback)
   */
  MarkDefectsChapterNPFHelper(LangTestDatabaseAsync service,
                              UserFeedback feedback,
                              UserManager userManager,
                              ExerciseController controller,
                              SimpleChapterNPFHelper learnHelper) {
    super(service, feedback, userManager, controller, learnHelper);
  }

  @Override
  protected FlexListLayout<CommonShell, CommonExercise> getMyListLayout(LangTestDatabaseAsync service,
                                                                        UserFeedback feedback,
                                                                        UserManager userManager,
                                                                        ExerciseController controller,
                                                                        SimpleChapterNPFHelper<CommonShell, CommonExercise> outer) {
    return new MyFlexListLayout<CommonShell, CommonExercise>(service, feedback, controller, outer) {

      @Override
      protected PagingExerciseList<CommonShell, CommonExercise> makeExerciseList(Panel topRow,
                                                                                 Panel currentExercisePanel,
                                                                                 String instanceName,
                                                                                 boolean incorrectFirst) {
//        logger.info("instance is " + instanceName);
        return new NPFlexSectionExerciseList(this, topRow, currentExercisePanel, instanceName, incorrectFirst, true) {
          private CheckBox filterOnly, uninspectedOnly;

          @Override
          protected void addTableWithPager(ClickablePagingContainer<CommonShell> pagingContainer) {
            // row 1
            Panel column = new FlowPanel();
            add(column);
            addTypeAhead(column);

            // row 2
            ;
            add(filterOnly = getFilterCheckbox());
            add(uninspectedOnly = getUninspectedCheckbox());

            // row 3
            add(pagingContainer.getTableWithPager());

            addEventHandler(instanceName, pagingContainer);
          }

          /**
           * @see  #addTableWithPager
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
            return addFilter("Show only uninspected items.");
          }

          /**
           * @see mitll.langtest.client.list.HistoryExerciseList#getHistoryToken
           * @param search
           * @param id
           * @return
           */
          protected String getHistoryTokenFromUIState(String search, String id) {
            String s = super.getHistoryTokenFromUIState(search, id) + ";" +
                SelectionState.ONLY_DEFAULT + "=" + filterOnly.getValue() + ";" +
                SelectionState.ONLY_UNINSPECTED + "=" + uninspectedOnly.getValue();

  //          logger.info("token " + s);
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

  private void addEventHandler(final String instanceName, SimplePagingContainer container) {
    LangTest.EVENT_BUS.addHandler(DefectEvent.TYPE, authenticationEvent -> {
//      if (!authenticationEvent.getSource().equals(instanceName)) {
//        // logger.info("this " + getClass() + " instance " + instanceName + " updating progress " + authenticationEvent.getSource());
//        getProgressInfo(instanceName);
//      }

      logger.info("got defect event");
      // container.onResize();
    });
  }

  protected ExercisePanelFactory<CommonShell, CommonExercise> getFactory(final PagingExerciseList<CommonShell, CommonExercise> exerciseList) {
    return new ExercisePanelFactory<CommonShell, CommonExercise>(service, feedback, controller, exerciseList) {
      @Override
      public Panel getExercisePanel(CommonExercise e) {
        return new QCNPFExercise<>(e, controller, exerciseList, MARK_DEFECTS1);
      }
    };
  }
}
