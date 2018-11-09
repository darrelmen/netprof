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

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.SimplePanel;
import mitll.langtest.client.banner.IListenView;
import mitll.langtest.client.banner.NewContentChooser;
import mitll.langtest.client.custom.INavigation;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.ExercisePanelFactory;
import mitll.langtest.client.list.ListOptions;
import mitll.langtest.client.list.NPExerciseList;
import mitll.langtest.client.list.PagingExerciseList;
import mitll.langtest.client.qc.QCNPFExercise;
import mitll.langtest.shared.custom.UserList;
import mitll.langtest.shared.exercise.ClientExercise;
import mitll.langtest.shared.exercise.CommonShell;
import mitll.langtest.shared.exercise.HasID;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Lets you show a user list with a paging container...
 * List on the left, content on the right, made by a factory.
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 10/8/13
 * Time: 3:27 PM
 * To change this template use File | Settings | File Templates.
 */
public class NPFHelper<T extends CommonShell, U extends ClientExercise> implements RequiresResize, IListenView {
  private final Logger logger = Logger.getLogger("NPFHelper");

  public static final String LIST_COMPLETE = "List complete!";
  public static final String COMPLETE = "Complete";

  final ExerciseController controller;

  PagingExerciseList<T, U> npfExerciseList = null;
  //private final boolean showQC;
  private final boolean showFirstNotCompleted;

  /**
   * TODO : remove showQC
   *
   * @param controller
   * @see ReviewItemHelper#ReviewItemHelper(ExerciseController)
   */
  NPFHelper(ExerciseController controller, boolean showFirstNotCompleted) {
    this.controller = controller;
    // this.showQC = showQC;
    this.showFirstNotCompleted = showFirstNotCompleted;
  }

  /**
   * Make the instance name uses the unique id for the list.
   *
   * @param ul
   * @param instanceName
   * @param loadExercises
   * @param toSelect
   * @return
   * @see NewContentChooser#getReviewList
   */
  public Panel doNPF(UserList<T> ul, INavigation.VIEWS instanceName, boolean loadExercises, HasID toSelect) {
    logger.info(getClass() + " : doNPF instanceName = " + instanceName + " for list " + ul + " of size " + loadExercises);

    Panel hp = doInternalLayout(ul.getID(), instanceName);
    if (loadExercises) {
      rememberAndLoadFirstFromUserList(ul, toSelect);
    } else {
      logger.warning("not loading exercises?");
    }
    return hp;
  }

  /**
   * Left and right components
   *
   * @param userListID
   * @param instanceName
   * @return
   * @see #doNPF
   */
  Panel doInternalLayout(int userListID, INavigation.VIEWS instanceName) {
//    logger.info(getClass() + " : doInternalLayout instanceName = " + instanceName + " for list " + ul);
    // row 1
    Panel hp = new HorizontalPanel();
    hp.getElement().setId("internalLayout_Row");

    // left side
    Panel left = new SimplePanel();
    left.getElement().setId("internalLayout_LeftCol");
    left.addStyleName("floatLeftAndClear");
    hp.add(left);

    // right side
    hp.add(getRightSideContent(instanceName));

    // this must come here!
    if (npfExerciseList == null) {
      logger.warning("huh? exercise list is null for " + instanceName + " and " + userListID);
    } else {
      left.add(npfExerciseList.getExerciseListOnLeftSide());
    }

    return hp;
  }

  private Panel getRightSideContent(INavigation.VIEWS instanceName) {
    Panel npfContentPanel = new SimplePanel();
    npfContentPanel.addStyleName("floatRight");
    npfContentPanel.getElement().setId("internalLayout_RightContent");

    npfExerciseList = makeNPFExerciseList(npfContentPanel, instanceName, showFirstNotCompleted);
    return npfContentPanel;
  }

  /**
   * @param right
   * @param instanceName
   * @param showFirstNotCompleted
   * @return
   * @see ##getRightSideContent
   */
  private PagingExerciseList<T, U> makeNPFExerciseList(Panel right, INavigation.VIEWS instanceName, boolean showFirstNotCompleted) {
    //  logger.info("got " + getClass() + " instance " + instanceName+ " show first " + showFirstNotCompleted);
    final PagingExerciseList<T, U> exerciseList =
        makeExerciseList(right, new ListOptions().setInstance(instanceName).setShowFirstNotCompleted(showFirstNotCompleted));
    setFactory(exerciseList, instanceName);
    Scheduler.get().scheduleDeferred(exerciseList::onResize);
    return exerciseList;
  }

  /**
   * TODO : why do we copy the list?
   *
   * @param ul
   * @param toSelect for now just the id?
   * @see #doNPF
   */
  private void rememberAndLoadFirstFromUserList(final UserList<T> ul, HasID toSelect) {
    npfExerciseList.setUserListID(ul.getID());

    List<T> copy = new ArrayList<>(ul.getExercises());

    int id = toSelect == null ? -1 : toSelect.getID();
    // logger.info("rememberAndLoadFirstFromUserList " + copy.size() + " exercises for " + id);
    npfExerciseList.rememberAndLoadFirst(copy, "", "", id);
    npfExerciseList.setWidth("270px");
    npfExerciseList.getElement().getStyle().setProperty("minWidth", "270px");
  }

  /**
   * @param right
   * @param listOptions
   * @return
   * @see #makeNPFExerciseList
   */
  private PagingExerciseList<T, U> makeExerciseList(final Panel right, ListOptions listOptions) {
    return new NPExerciseList(right, controller, listOptions, -1);
  }

  /**
   * @param exerciseList
   * @param instanceName
   * @see #makeNPFExerciseList(Panel, String, boolean)
   */
  private void setFactory(final PagingExerciseList<T, U> exerciseList, final INavigation.VIEWS instanceName) {
    exerciseList.setFactory(getFactory(exerciseList, instanceName));
  }

  private ExercisePanelFactory<T, U> getFactory(final PagingExerciseList<T, U> exerciseList, final INavigation.VIEWS instanceName) {
    return new ExercisePanelFactory<T, U>(controller, exerciseList) {
      @Override
      public Panel getExercisePanel(U e) {
        return new QCNPFExercise<U>(e, controller, exerciseList, instanceName);
      }
    };
  }

  @Override
  public int getVolume() {
    return 100;
  }

  @Override
  public int getDialogSessionID() {
    return -1;
  }

  @Override
  public void onResize() {
    if (npfExerciseList != null) {
      npfExerciseList.onResize();
    }
    //else {
      //logger.info("no exercise list " +instanceName + "  for " + getClass());
    //}
  }
}
