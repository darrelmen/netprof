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

package mitll.langtest.client.custom.dialog;

import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.SimplePanel;
import mitll.langtest.client.banner.IListenView;
import mitll.langtest.client.custom.INavigation;
import mitll.langtest.client.custom.userlist.ListView;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.ExercisePanelFactory;
import mitll.langtest.client.list.PagingExerciseList;
import mitll.langtest.client.scoring.TwoColumnExercisePanel;
import mitll.langtest.shared.custom.UserList;
import mitll.langtest.shared.exercise.ClientExercise;
import mitll.langtest.shared.exercise.CommonShell;
import mitll.langtest.shared.exercise.Exercise;
import mitll.langtest.shared.exercise.MutableExercise;
import mitll.langtest.shared.project.ProjectStartupInfo;
import mitll.langtest.shared.scoring.AlignmentOutput;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * Coordinates editing an item -
 * makes a panel that has the list and content on the right
 * <p>
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 12/9/13
 * Time: 4:58 PM
 * To change this template use File | Settings | File Templates.
 */
public class EditItem {
  //private final Logger logger = Logger.getLogger("EditItem");
  private final ExerciseController controller;
  private PagingExerciseList<CommonShell, ClientExercise> exerciseList;

  /**
   * @param controller
   * @see ListView#editList
   */
  public EditItem(ExerciseController controller) {
    this.controller = controller;
  }

  /**
   * @param originalList
   * @return
   * @see ListView#editList
   */
  public Panel editItem(UserList<CommonShell> originalList) {
    Panel hp = new DivWidget();
    hp.addStyleName("inlineFlex");
//    hp.getElement().setId("EditItem_for_" + originalList.getName());

    Panel pagerOnLeft = new SimplePanel();
    hp.add(pagerOnLeft);
    pagerOnLeft.addStyleName("rightFiveMargin");

    final Panel contentOnRight = new SimplePanel();
    contentOnRight.getElement().setId("EditItem_content");
    hp.add(contentOnRight);

    exerciseList = makeExerciseList(contentOnRight, INavigation.VIEWS.LISTS, originalList);
    pagerOnLeft.add(exerciseList.getExerciseListOnLeftSide());
    return hp;
  }

  int userListID = -1;

  /**
   * @param right
   * @param instanceName
   * @param originalList
   * @return
   * @paramz ul
   * @paramz includeAddItem
   * @see #editItem
   */
  private PagingExerciseList<CommonShell, ClientExercise> makeExerciseList(Panel right,
                                                                           INavigation.VIEWS instanceName,
                                                                           UserList<CommonShell> originalList) {
    //logger.info("EditItem.makeExerciseList - ul = " + ul + " " + includeAddItem);
    userListID = originalList.getID();
    EditableExerciseList exerciseList = new EditableExerciseList(controller, right, instanceName, originalList);
    this.exerciseList = exerciseList;
    setFactory(this.exerciseList);
    this.exerciseList.setUnaccountedForVertical(280);   // TODO do something better here
    // logger.info("setting vertical on " +exerciseList.getElement().getExID());
    Scheduler.get().scheduleDeferred(() -> this.exerciseList.onResize());
    //  Scheduler.get().scheduleDeferred(() -> exerciseList.getTypeAheadGrabFocus());
    return this.exerciseList;
  }

  public void onResize() {
    if (exerciseList != null) {
      exerciseList.onResize();
    }
  }

  /**
   * TODOx : don't do it like this!
   * <p>
   * TODOx : consider filling in context and context translation?
   * <p>
   *
   * @return
   * @see #makeExerciseList
   */
/*  public ClientExercise getNewItem() {
    int user = controller.getUserManager().getUser();
    Exercise exercise = new Exercise(
        NEW_EXERCISE_ID,
        user,
        "",
        getProjectid(),
        false);

    addContext(user, exercise);

    return exercise;
  }

  private void addContext(int userid, MutableExercise exercise) {
    Exercise context = new Exercise(-1,
        userid,
        "",
        getProjectid(),
        false);

    exercise.addContextExercise(context);
  }

  private int getProjectid() {
    ProjectStartupInfo projectStartupInfo = controller.getProjectStartupInfo();
    return projectStartupInfo == null ? -1 : projectStartupInfo.getProjectid();
  }*/
  private void setFactory(final PagingExerciseList<CommonShell, ClientExercise> exerciseList) {
    exerciseList.setFactory(new ExercisePanelFactory<CommonShell, ClientExercise>(controller, exerciseList) {
      private final Map<Integer, AlignmentOutput> alignments = new HashMap<>();

      @Override
      public Panel getExercisePanel(ClientExercise exercise) {
        if (exercise.isPredefined()) {
          TwoColumnExercisePanel<ClientExercise> widgets = new TwoColumnExercisePanel<>(exercise,
              controller,
              exerciseList,
              alignments, true, new IListenView() {
            @Override
            public int getVolume() {
              return 100;
            }

            @Override
            public int getDialogSessionID() {
              return -1;
            }
          },
              false);
          widgets.addWidgets(getFLChoice(), false, getPhoneChoices());
          return widgets;
        } else {
          EditableExerciseDialog<CommonShell, ClientExercise> reviewEditableExercise =
              new EditableExerciseDialog<CommonShell, ClientExercise>(
                  controller,
                  exercise,
                  userListID,
                  INavigation.VIEWS.LISTS
              ) {
                @NotNull
                @Override
                protected DivWidget getDominoEditInfo() {
                  return null;
                }

//            @Override
//            void afterValidForeignPhrase(Panel toAddTo, boolean onClick) {
//              postChangeIfDirty(onClick);
//            }
              };
          Panel widgets = reviewEditableExercise.addFields(exerciseList, new SimplePanel());
          reviewEditableExercise.setFields(exercise);
          return widgets;
        }
      }
    });
  }

  public void reload() {
    exerciseList.reload(new HashMap<>());
  }
}
