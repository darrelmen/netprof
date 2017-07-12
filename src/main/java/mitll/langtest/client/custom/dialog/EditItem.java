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

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.user.client.ui.*;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.ExercisePanelFactory;
import mitll.langtest.client.list.PagingExerciseList;
import mitll.langtest.client.scoring.TwoColumnExercisePanel;
import mitll.langtest.shared.custom.UserList;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.CommonShell;
import mitll.langtest.shared.exercise.Exercise;
import mitll.langtest.shared.exercise.MutableExercise;
import mitll.langtest.shared.project.ProjectStartupInfo;
import mitll.langtest.shared.scoring.AlignmentOutput;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

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
  private final Logger logger = Logger.getLogger("EditItem");

  /**
   * @see #getNewItem
   * @see #makeExerciseList
   */
  private static final int NEW_EXERCISE_ID = -100;
  private static final String EDIT_ITEM = "editItem";

  private final ExerciseController controller;

  private PagingExerciseList<CommonShell, CommonExercise> exerciseList;
  private final String instanceName;

  /**
   * @param controller
   * @see ListManager#ListManager
   */
  public EditItem(ExerciseController controller) {
    this.controller = controller;
    this.instanceName = "EditItem";
  }

  public void reload() {
    exerciseList.getExercises();
  }

  /**
   * @param originalList
   * @return
   * @paramx itemMarker
   * @paramx includeAddItem
   * @see ListManager#showEditItem
   */
  public Panel editItem(UserList<CommonShell> originalList) {
    Panel hp = new HorizontalPanel();
    hp.getElement().setId("EditItem_for_" + originalList.getName());
    Panel pagerOnLeft = new SimplePanel();
    hp.add(pagerOnLeft);
    pagerOnLeft.addStyleName("rightFiveMargin");
    final Panel contentOnRight = new SimplePanel();
    contentOnRight.getElement().setId("EditItem_content");

    hp.add(contentOnRight);

    exerciseList = makeExerciseList(contentOnRight, EDIT_ITEM, originalList);
    pagerOnLeft.add(exerciseList.getExerciseListOnLeftSide());

    rememberAndLoadFirst(originalList, exerciseList);
    return hp;
  }

  public void onResize() {
    if (exerciseList != null) {
      //  logger.info("EditItem onResize");
      exerciseList.onResize();
    }
  }

  /**
   * @param right
   * @param instanceName
   * @param originalList
   * @return
   * @paramz ul
   * @paramz includeAddItem
   * @see #editItem
   */
  private PagingExerciseList<CommonShell, CommonExercise> makeExerciseList(Panel right,
                                                                           String instanceName,
                                                                           UserList<CommonShell> originalList) {
    //logger.info("EditItem.makeExerciseList - ul = " + ul + " " + includeAddItem);
    exerciseList = new EditableExerciseList(controller, this, right, instanceName, originalList);
    setFactory(exerciseList, originalList);
    exerciseList.setUnaccountedForVertical(280);   // TODO do something better here
    // logger.info("setting vertical on " +exerciseList.getElement().getExID());
    Scheduler.get().scheduleDeferred(() -> exerciseList.onResize());
    return exerciseList;
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
  public CommonExercise getNewItem() {
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

  public void addContext(int userid, MutableExercise exercise) {
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
  }

  private void setFactory(final PagingExerciseList<CommonShell, CommonExercise> exerciseList,
                          final UserList<CommonShell> originalList) {
    final PagingExerciseList<CommonShell, CommonExercise> outer = exerciseList;

    exerciseList.setFactory(new ExercisePanelFactory<CommonShell, CommonExercise>(
        controller, exerciseList) {
      private final Map<Integer, AlignmentOutput> alignments = new HashMap<>();

      @Override
      public Panel getExercisePanel(CommonExercise exercise) {
        Panel panel = new ResizableSimple();
        panel.getElement().setId("EditItemPanel");

        logger.info("Creator " + exercise.getCreator() + " vs " + controller.getUser());
        boolean iCreatedThisItem = didICreateThisItem(exercise) ||
            (controller.getUserManager().isTeacher() && !exercise.isPredefined());  // asked that teachers be able to record audio for other's items
        if (iCreatedThisItem) {  // it's mine!
          EditableExerciseDialog editableExercise =
              new EditableExerciseDialog(controller,
                  exercise,
                  originalList.getID(),
                  outer,
                  getInstance()
              ) {
                @Override
                protected void addItemsAtTop(Panel container) {

                }
              };
          panel.add(editableExercise.addFields(outer, panel));
          editableExercise.setFields(exercise);
        } else {
          return new TwoColumnExercisePanel<>(exercise,
              controller,
              exerciseList,
              getChoices(), getPhoneChoices(), alignments);
        }

        return panel;
      }
    });
  }

  private class ResizableSimple extends SimplePanel implements RequiresResize {

    @Override
    public void onResize() {
      Widget widget = getWidget();
      if (widget instanceof RequiresResize) {
        ((RequiresResize) widget).onResize();
      } else {
        logger.info("skipping " + widget.getElement().getId());
      }
    }
  }

  /**
   * @param ul
   * @param npfExerciseList
   * @see #editItem
   */
  private void rememberAndLoadFirst(final UserList<CommonShell> ul,
                                    PagingExerciseList<CommonShell, CommonExercise> npfExerciseList) {
    npfExerciseList.setUserListID(ul.getID());
    npfExerciseList.rememberAndLoadFirst(ul.getExercises());
  }

  private String getInstance() {
    return instanceName;
  }

  /**
   * @param exercise
   * @return
   * @paramx originalList
   * @seex EditItem#getAddOrEditPanel(CommonExercise, UserList, boolean)
   */
  private boolean didICreateThisItem(CommonExercise exercise) {
    return exercise.getCreator() == controller.getUser();
  }
}
