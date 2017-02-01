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

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasText;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.SimplePanel;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.custom.ReloadableContainer;
import mitll.langtest.client.custom.dialog.ReviewEditableExercise;
import mitll.langtest.client.exercise.ClickablePagingContainer;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.ExercisePanelFactory;
import mitll.langtest.client.list.PagingExerciseList;
import mitll.langtest.client.list.SelectionState;
import mitll.langtest.client.services.ExerciseServiceAsync;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.shared.answer.ActivityType;
import mitll.langtest.shared.custom.UserList;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.CommonShell;
import mitll.langtest.shared.exercise.Exercise;

import java.util.logging.Logger;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 3/26/2014.
 */
public class ReviewItemHelper extends NPFHelper {
  private final Logger logger = Logger.getLogger("ReviewItemHelper");

  /**
   *
   */
  private static final String ONLY_WITH_AUDIO_DEFECTS = "Only with audio defects";

  private FlexListLayout<CommonShell, CommonExercise> flexListLayout;
  private final HasText itemMarker;
  private final ReloadableContainer predefinedContent;

  /**
   * @param service
   * @param feedback
   * @param controller
   * @param predefinedContent
   * @param exerciseServiceAsync
   * @see mitll.langtest.client.custom.Navigation#Navigation
   * @see mitll.langtest.client.custom.ListManager#ListManager
   */
  public ReviewItemHelper(final LangTestDatabaseAsync service,
                          final UserFeedback feedback,
                          final ExerciseController controller,
                          final ReloadableContainer predefinedContent,
                          ExerciseServiceAsync exerciseServiceAsync) {
    super(service, feedback, controller, true,false, exerciseServiceAsync);
    this.itemMarker = null;
    this.predefinedContent = predefinedContent;
    if (predefinedContent == null) logger.warning("huh? predefinedContent is null");
  }

  /**
   * Left and right components
   *
   * @param ul
   * @param instanceName
   * @return
   * @see #doNPF
   */
  @Override
  protected Panel doInternalLayout(final UserList<CommonShell> ul, String instanceName) {
//    logger.info(getClass() + " : doInternalLayout instanceName = " + instanceName + " for list " + ul);
    this.flexListLayout = new ReviewFlexListLayout(ul);
    Panel widgets = flexListLayout.doInternalLayout(ul, instanceName);
    npfExerciseList = flexListLayout.npfExerciseList;
    return widgets;
  }

  @Override
  public void onResize() {
    if (flexListLayout != null) {
      flexListLayout.onResize();
    } else if (npfExerciseList != null) {
      npfExerciseList.onResize();
    } else {
      //System.out.println("ReviewItemHelper.onResize : not sending resize event - flexListLayout is null?");
    }
  }

  // private class ReviewFlexListLayout<CommonExercise extends CommonShell & AnnotationExercise & AudioRefExercise> extends FlexListLayout<CommonExercise> {
  private class ReviewFlexListLayout extends FlexListLayout<CommonShell, CommonExercise> {
    private final UserList<CommonShell> ul;

    ReviewFlexListLayout(UserList<CommonShell> ul) {
      super(ReviewItemHelper.this.service, ReviewItemHelper.this.feedback, ReviewItemHelper.this.controller, ReviewItemHelper.this.exerciseServiceAsync);
      this.ul = ul;
    }

    @Override
    protected ExercisePanelFactory<CommonShell, CommonExercise> getFactory(final PagingExerciseList<CommonShell, CommonExercise> pagingExerciseList) {
      return new ExercisePanelFactory<CommonShell, CommonExercise>(service, feedback, controller, pagingExerciseList) {
        @Override
        public Panel getExercisePanel(CommonExercise exercise) {
          CommonExercise userExercise = new Exercise(exercise);

          ReviewEditableExercise reviewEditableExercise =
              new ReviewEditableExercise(service, controller, itemMarker,
                  userExercise, ul,
                  pagingExerciseList,
                  predefinedContent,
                  "ReviewEditableExercise"
              );

          SimplePanel ignoredContainer = new SimplePanel();

          Panel widgets = reviewEditableExercise.addNew(
              ul,
              ul,
              npfExerciseList,
              ignoredContainer);
          reviewEditableExercise.setFields(exercise);

          return widgets;
        }
      };
    }

    @Override
    protected PagingExerciseList<CommonShell, CommonExercise> makeExerciseList(Panel topRow, Panel currentExercisePanel,
                                                                               String instanceName, boolean incorrectFirst) {
      FlexListLayout outer = this;
      return new NPFlexSectionExerciseList(outer, topRow, currentExercisePanel, instanceName, incorrectFirst, ActivityType.REVIEW) {
        com.github.gwtbootstrap.client.ui.CheckBox checkBox;

        /**
         * @see mitll.langtest.client.list.HistoryExerciseList#getHistoryToken
         * @param search
         * @param id
         * @return
         */
        @Override
        protected String getHistoryTokenFromUIState(String search, int id) {
          String s = super.getHistoryTokenFromUIState(search, id) +
              ";" +
              SelectionState.ONLY_WITH_AUDIO_DEFECTS +
              "=" + checkBox.getValue();
      //    logger.info("ReviewItemHelper : history token now  " + s);
          return s;
        }

        @Override
        protected void addTableWithPager(ClickablePagingContainer pagingContainer) {
          // row 1
          Panel column = new FlowPanel();
          add(column);
          addTypeAhead(column);

          // row 2
          this.checkBox = new com.github.gwtbootstrap.client.ui.CheckBox(ONLY_WITH_AUDIO_DEFECTS);
          checkBox.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
              pushNewSectionHistoryToken();
            }
          });
          checkBox.addStyleName("leftFiveMargin");
          add(checkBox);

          // row 3
          add(pagingContainer.getTableWithPager());
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
