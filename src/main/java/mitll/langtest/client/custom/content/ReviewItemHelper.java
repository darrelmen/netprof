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

package mitll.langtest.client.custom.content;

import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.SimplePanel;
import mitll.langtest.client.custom.INavigation;
import mitll.langtest.client.custom.dialog.ReviewEditableExercise;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.ExercisePanelFactory;
import mitll.langtest.client.exercise.SimplePagingContainer;
import mitll.langtest.client.list.ListOptions;
import mitll.langtest.client.list.NPExerciseList;
import mitll.langtest.client.list.PagingExerciseList;
import mitll.langtest.client.list.SelectionState;
import mitll.langtest.shared.custom.UserList;
import mitll.langtest.shared.exercise.ClientExercise;
import mitll.langtest.shared.exercise.CommonShell;
import mitll.langtest.shared.exercise.ExerciseListRequest;

import java.util.Collection;
import java.util.Map;
import java.util.logging.Logger;

import static mitll.langtest.shared.answer.ActivityType.QUALITY_CONTROL;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @seex ListManager#ListManager
 * @since 3/26/2014.
 */
public class ReviewItemHelper<T extends CommonShell, U extends ClientExercise> extends NPFHelper<T, U> {
  private final Logger logger = Logger.getLogger("ReviewItemHelper");

  /**
   *
   */
  private static final String ONLY_WITH_AUDIO_DEFECTS = "Only with audio defects";
  private FlexListLayout<T, U> flexListLayout;
  INavigation.VIEWS views;

  /**
   * @param controller
   * @seex ListManager#ListManager
   * @see mitll.langtest.client.banner.NewContentChooser#showReviewItems
   */
  public ReviewItemHelper(final ExerciseController controller) {
    super(controller, true, false);
  }

  /**
   * Left and right components
   *
   * @param userListID
   * @param instanceName
   * @return
   * @see #doNPF
   */
  @Override
  protected Panel doInternalLayout(final int userListID, INavigation.VIEWS instanceName) {
    logger.info(getClass() + " : doInternalLayout instanceName = " + instanceName + " for list " + userListID);
    // int id = userListID.getID();

    this.views = instanceName;
    this.flexListLayout = new ReviewFlexListLayout(userListID);
    Panel widgets = flexListLayout.doInternalLayout(userListID, instanceName, true);
    npfExerciseList = flexListLayout.npfExerciseList;
    return widgets;
  }

  @Override
  public void onResize() {
    if (flexListLayout != null) {
      flexListLayout.onResize();
    } else if (npfExerciseList != null) {
      npfExerciseList.onResize();
    }
  }

  private class ReviewFlexListLayout extends FlexListLayout<T, U> {
    private final int ulID;

    ReviewFlexListLayout(int ulID) {
      super(ReviewItemHelper.this.controller);
      this.ulID = ulID;
    }

    protected void styleTopRow(Panel twoRows, Panel topRow) {
      twoRows.add(topRow);
    }

    protected void styleBottomRow(Panel bottomRow) {
      bottomRow.setWidth("100%");
      bottomRow.addStyleName("inlineFlex");
    }

    @Override
    protected ExercisePanelFactory<T, U> getFactory(final PagingExerciseList<T, U> pagingExerciseList) {
      return new ExercisePanelFactory<T, U>(getController(), pagingExerciseList) {
        @Override
        public Panel getExercisePanel(U exercise) {
          ReviewEditableExercise<T, U> reviewEditableExercise =
              new ReviewEditableExercise<>(controller,
                  exercise,
                  ulID,
                  pagingExerciseList,
                  views
              );
          Panel widgets = reviewEditableExercise.addFields(npfExerciseList, new SimplePanel());
          reviewEditableExercise.setFields(exercise);

          return widgets;
        }
      };
    }

    @Override
    protected PagingExerciseList<T, U> makeExerciseList(Panel topRow, Panel currentExercisePanel,
                                                        INavigation.VIEWS instanceName, DivWidget listHeader, DivWidget footer) {
      FlexListLayout outer = this;
      return new NPExerciseList<T, U>(currentExercisePanel, outer.getController(),
          new ListOptions(instanceName)
              .setActivityType(QUALITY_CONTROL), -1) {
        com.github.gwtbootstrap.client.ui.CheckBox checkBox;

        @Override
        protected ExerciseListRequest getExerciseListRequest(Map<String, Collection<String>> typeToSection,
                                                             String prefix,
                                                             boolean onlyWithAudioAnno,
                                                             boolean onlyDefaultUser,
                                                             boolean onlyUninspected) {
          ExerciseListRequest exerciseListRequest = super.getExerciseListRequest(typeToSection, prefix, onlyWithAudioAnno, onlyDefaultUser,
              onlyUninspected)
              .setQC(true)
              .setAddContext(instanceName == INavigation.VIEWS.FIX_SENTENCES);
          logger.info("getExerciseListRequest making request " + exerciseListRequest);
          return exerciseListRequest;
        }

        protected ExerciseListRequest getExerciseListRequest(String prefix) {
          ExerciseListRequest exerciseListRequest = super
              .getExerciseListRequest(prefix)
              .setQC(true)
              .setAddContext(instanceName == INavigation.VIEWS.FIX_SENTENCES);
          logger.info("getExerciseListRequest req is " + exerciseListRequest);
          return exerciseListRequest;
        }

        /**
         * @see mitll.langtest.client.list.HistoryExerciseList#getHistoryToken
         * @param search
         * @param id
         * @return
         */
        @Override
        protected String getHistoryTokenFromUIState(String search, int id) {
          String s = super.getHistoryTokenFromUIState(search, id) +
              SelectionState.SECTION_SEPARATOR +
              SelectionState.ONLY_WITH_AUDIO_DEFECTS +
              "=" + checkBox.getValue();
          //    logger.info("ReviewItemHelper : history token now  " + s);
          return s;
        }

        @Override
        protected void addTableWithPager(SimplePagingContainer<?> pagingContainer) {
          // row 1
          Panel column = new FlowPanel();
          add(column);
          addTypeAhead(column);

          // row 2
          this.checkBox = new com.github.gwtbootstrap.client.ui.CheckBox(ONLY_WITH_AUDIO_DEFECTS);
          checkBox.addClickHandler(event -> pushNewSectionHistoryToken());
          checkBox.addStyleName("leftFiveMargin");
          add(checkBox);

          // row 3
          add(pagingContainer.getTableWithPager(new ListOptions()));
        }

        @Override
        protected void restoreUIState(SelectionState selectionState) {
          super.restoreUIState(selectionState);
          checkBox.setValue(selectionState.isOnlyWithAudioDefects());
        }
      };

    }
  }
}
