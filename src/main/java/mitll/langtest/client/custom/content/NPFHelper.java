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

import com.github.gwtbootstrap.client.ui.TabPanel;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.user.client.ui.*;
import mitll.langtest.client.custom.tabs.TabAndContent;
import mitll.langtest.client.custom.userlist.ListManager;
import mitll.langtest.client.dialog.ModalInfoDialog;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.ExercisePanelFactory;
import mitll.langtest.client.list.ListOptions;
import mitll.langtest.client.list.NPExerciseList;
import mitll.langtest.client.list.PagingExerciseList;
import mitll.langtest.client.qc.QCNPFExercise;
import mitll.langtest.client.scoring.TwoColumnExercisePanel;
import mitll.langtest.shared.custom.UserList;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.CommonShell;
import mitll.langtest.shared.exercise.HasID;
import mitll.langtest.shared.scoring.AlignmentOutput;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
public class NPFHelper implements RequiresResize {
  private final Logger logger = Logger.getLogger("NPFHelper");

  private static final String LIST_COMPLETE = "List complete!";
  private static final String COMPLETE = "Complete";
  private boolean madeNPFContent = false;

  final ExerciseController controller;

  PagingExerciseList<CommonShell, CommonExercise> npfExerciseList = null;
  private final boolean showQC;
  private final boolean showFirstNotCompleted;

  /**
   * TODO : remove showQC
   *
   * @param controller
   * @param showQC
   * @seex AVPHelper#AVPHelper(ExerciseController)
   * @see ReviewItemHelper#ReviewItemHelper(ExerciseController)
   * @see ListManager#ListManager
   */
  NPFHelper(ExerciseController controller, boolean showQC, boolean showFirstNotCompleted) {
    this.controller = controller;
    this.showQC = showQC;
    this.showFirstNotCompleted = showFirstNotCompleted;
  }

  /**
   * @param ul
   * @param tabAndContent
   * @param instanceName
   * @param loadExercises
   * @see ListManager#selectPreviouslyClickedSubTab(TabPanel, TabAndContent, TabAndContent, TabAndContent, UserList, String, boolean, boolean, boolean)
   */
  public void showNPF(UserList<CommonShell> ul, TabAndContent tabAndContent, String instanceName, boolean loadExercises) {
    logger.info(getClass() + " : adding npf content instanceName = " +
        instanceName + " for list " + ul + " with " + ul.getExercises().size() + " load " + loadExercises);

    showNPF(ul, tabAndContent, instanceName, loadExercises, null);
  }

  /**
   * Add npf widget to content of a tab - here marked tabAndContent
   *
   * @param ul            show this user list
   * @param tabAndContent in this tab
   * @param instanceName  flex, review, etc.
   * @param loadExercises should we load exercises initially
   * @param toSelect
   * @see ListManager#getListOperations
   */
  public void showNPF(UserList<CommonShell> ul,
                      TabAndContent tabAndContent,
                      String instanceName,
                      boolean loadExercises,
                      HasID toSelect) {
    logger.info(getClass() + " : adding npf content instanceName = " +
        instanceName + " for list " + ul + " with " + ul.getExercises().size());
    DivWidget content = tabAndContent.getContent();
    int widgetCount = content.getWidgetCount();
    if (!madeNPFContent || widgetCount == 0) {
      madeNPFContent = true;
      logger.info("\t: adding npf content instanceName = " + instanceName + " for list " + ul + " load " + loadExercises);
      addNPFToContent(ul, content, instanceName, loadExercises, toSelect);
    } else {
      logger.info("\t: rememberAndLoadFirst instanceName = " + instanceName + " for list " + ul);
      rememberAndLoadFirstFromUserList(ul, toSelect);
    }
  }

  private void addNPFToContent(UserList<CommonShell> ul,
                               Panel listContent,
                               String instanceName,
                               boolean loadExercises,
                               HasID toSelect) {
    listContent.add(doNPF(ul, instanceName, loadExercises, toSelect));
  }

  /**
   * Make the instance name uses the unique id for the list.
   *
   * @param ul
   * @param instanceName
   * @param loadExercises
   * @param toSelect
   * @return
   * @see #addNPFToContent
   */
  private Panel doNPF(UserList<CommonShell> ul, String instanceName, boolean loadExercises, HasID toSelect) {
    logger.info(getClass() + " : doNPF instanceName = " + instanceName + " for list " + ul + " of size " + loadExercises);

    Panel hp = doInternalLayout(ul, instanceName);
    if (loadExercises) {
      rememberAndLoadFirstFromUserList(ul, toSelect);
    } else logger.warning("not loading exercises?");
    return hp;
  }

  /**
   * Left and right components
   *
   * @param ul
   * @param instanceName
   * @return
   * @see #doNPF
   */
  Panel doInternalLayout(UserList<CommonShell> ul, String instanceName) {
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
    Panel npfContentPanel = getRightSideContent(ul, instanceName);
    hp.add(npfContentPanel);

    // this must come here!
    if (npfExerciseList == null) {
      logger.warning("huh? exercise list is null for " + instanceName + " and " + ul);
    } else {
      addExerciseListOnLeftSide(left, npfExerciseList.getExerciseListOnLeftSide());
    }

    return hp;
  }

  void addExerciseListOnLeftSide(Panel left, Widget exerciseListOnLeftSide) {
    left.add(exerciseListOnLeftSide);
  }

  Panel getRightSideContent(UserList<CommonShell> ul, String instanceName) {
    Panel npfContentPanel = new SimplePanel();
    npfContentPanel.addStyleName("floatRight");
    npfContentPanel.getElement().setId("internalLayout_RightContent");

    npfExerciseList = makeNPFExerciseList(npfContentPanel, instanceName + "_" + ul.getID(), showFirstNotCompleted);
    return npfContentPanel;
  }

  /**
   * @param right
   * @param instanceName
   * @param showFirstNotCompleted
   * @return
   * @see ##getRightSideContent
   */
  PagingExerciseList<CommonShell, CommonExercise> makeNPFExerciseList(Panel right, String instanceName, boolean showFirstNotCompleted) {
    //  logger.info("got " + getClass() + " instance " + instanceName+ " show first " + showFirstNotCompleted);
    final PagingExerciseList<CommonShell, CommonExercise> exerciseList =
        makeExerciseList(right, new ListOptions().setInstance(instanceName).setShowFirstNotCompleted(showFirstNotCompleted));
    setFactory(exerciseList, instanceName, showQC);
    Scheduler.get().scheduleDeferred(exerciseList::onResize);
    return exerciseList;
  }

  /**
   * TODO : why do we copy the list?
   *
   * @param ul
   * @param toSelect for now just the id?
   * @see #doNPF
   * @see #showNPF
   */
  private void rememberAndLoadFirstFromUserList(final UserList<CommonShell> ul, HasID toSelect) {
    npfExerciseList.setUserListID(ul.getID());

    List<CommonShell> copy = new ArrayList<>();
    copy.addAll(ul.getExercises());

    int id = toSelect == null ? -1 : toSelect.getID();
    logger.info("rememberAndLoad " + copy.size() + " exercises for " + id);
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
  PagingExerciseList<CommonShell, CommonExercise> makeExerciseList(final Panel right, ListOptions listOptions) {
    return new NPExerciseList(right, controller, listOptions, -1) {
      @Override
      protected void onLastItem() {
        new ModalInfoDialog(COMPLETE, LIST_COMPLETE, hiddenEvent -> reloadExercises());
      }
    };
  }

  /**
   * @param exerciseList
   * @param instanceName
   * @param showQC
   * @see #makeNPFExerciseList(Panel, String, boolean)
   */
  private void setFactory(final PagingExerciseList<CommonShell, CommonExercise> exerciseList, final String instanceName, boolean showQC) {
    exerciseList.setFactory(getFactory(exerciseList, instanceName, showQC));
  }

  ExercisePanelFactory<CommonShell, CommonExercise> getFactory(
      final PagingExerciseList<CommonShell, CommonExercise> exerciseList,
      final String instanceName,
      final boolean showQC) {
    return new ExercisePanelFactory<CommonShell, CommonExercise>(controller, exerciseList) {
      private final Map<Integer, AlignmentOutput> alignments = new HashMap<>();

      @Override
      public Panel getExercisePanel(CommonExercise e) {
        if (showQC) {
          return new QCNPFExercise<>(e, controller, exerciseList, instanceName);
        } else {
          return new TwoColumnExercisePanel<>(e,
              controller,
              exerciseList,
              getChoices(), getPhoneChoices(), alignments);
        }
      }
    };
  }

  @Override
  public void onResize() {
    if (npfExerciseList != null) {
      npfExerciseList.onResize();
    } else {
      //logger.info("no exercise list " +instanceName + "  for " + getClass());
    }
  }

  public void setContentPanel(DivWidget content) {
    DivWidget contentPanel = content;
  }
}
