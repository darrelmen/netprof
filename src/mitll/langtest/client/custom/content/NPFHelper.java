/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

package mitll.langtest.client.custom.content;

import com.github.gwtbootstrap.client.ui.TabPanel;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.event.HiddenEvent;
import com.github.gwtbootstrap.client.ui.event.HiddenHandler;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.user.client.ui.*;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.custom.exercise.CommentNPFExercise;
import mitll.langtest.client.custom.tabs.TabAndContent;
import mitll.langtest.client.dialog.ModalInfoDialog;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.ExercisePanelFactory;
import mitll.langtest.client.list.NPExerciseList;
import mitll.langtest.client.list.PagingExerciseList;
import mitll.langtest.client.qc.QCNPFExercise;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.shared.custom.UserList;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.CommonShell;
import mitll.langtest.shared.exercise.HasID;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Lets you show a user list with a paging container...
 * List on the left, content on the right, made by a factory.
 * User: GO22670
 * Date: 10/8/13
 * Time: 3:27 PM
 * To change this template use File | Settings | File Templates.
 */
public class NPFHelper implements RequiresResize {
  private final Logger logger = Logger.getLogger("NPFHelper");

  private static final String LIST_COMPLETE = "List complete!";
  private static final String COMPLETE = "Complete";
  private boolean madeNPFContent = false;

  final LangTestDatabaseAsync service;
  final ExerciseController controller;

  final UserFeedback feedback;
  PagingExerciseList<CommonShell, CommonExercise> npfExerciseList = null;
  private final boolean showQC;
  DivWidget contentPanel;

  /**
   * @param service
   * @param feedback
   * @param controller
   * @param showQC
   * @see mitll.langtest.client.custom.Navigation#Navigation
   * @see mitll.langtest.client.custom.ListManager#ListManager
   */
  public NPFHelper(LangTestDatabaseAsync service, UserFeedback feedback, ExerciseController controller, boolean showQC) {
    this.service = service;
    this.feedback = feedback;
    this.controller = controller;
    this.showQC = showQC;
  }

  /**
   * @param ul
   * @param tabAndContent
   * @param instanceName
   * @param loadExercises
   * @see mitll.langtest.client.custom.ListManager#selectPreviouslyClickedSubTab(TabPanel, TabAndContent, TabAndContent, TabAndContent, UserList, String, boolean, boolean, boolean)
   */
  public void showNPF(UserList<CommonShell> ul, TabAndContent tabAndContent, String instanceName, boolean loadExercises) {
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
   * @see mitll.langtest.client.custom.ListManager#getListOperations
   */
  public void showNPF(UserList<CommonShell> ul, TabAndContent tabAndContent, String instanceName, boolean loadExercises,
                      HasID toSelect) {
    logger.info(getClass() + " : adding npf content instanceName = " + instanceName + " for list " + ul + " with " +ul.getExercises().size());

    DivWidget content = tabAndContent.getContent();
    int widgetCount = content.getWidgetCount();
    if (!madeNPFContent || widgetCount == 0) {
      madeNPFContent = true;
      logger.info("\t: adding npf content instanceName = " + instanceName + " for list " + ul);
      addNPFToContent(ul, content, instanceName, loadExercises, toSelect);
    } else {
      logger.info("\t: rememberAndLoadFirst instanceName = " + instanceName + " for list " + ul);
      rememberAndLoadFirst(ul, toSelect);
    }
  }

  private void addNPFToContent(UserList<CommonShell> ul, Panel listContent, String instanceName, boolean loadExercises,
                               HasID toSelect) {
    listContent.add(doNPF(ul, instanceName, loadExercises, toSelect));
    listContent.addStyleName("userListBackground");
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
    //logger.info(getClass() + " : doNPF instanceName = " + instanceName + " for list " + ul + " of size ");

    Panel hp = doInternalLayout(ul, instanceName);
    if (loadExercises) {
      rememberAndLoadFirst(ul, toSelect);
    }
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
    left.addStyleName("floatLeft");
    hp.add(left);

    // right side
    Panel npfContentPanel = getRightSideContent(ul, instanceName);
    hp.add(npfContentPanel);

    // this must come here!
    if (npfExerciseList == null) {
      logger.warning("huh? exercise list is null for " + instanceName + " and " + ul);
    } else {
      addExerciseListOnLeftSide(left, npfExerciseList.getExerciseListOnLeftSide(controller.getProps()));
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

    npfExerciseList = makeNPFExerciseList(npfContentPanel, instanceName + "_" + ul.getUniqueID());
    return npfContentPanel;
  }

  /**
   * @param right
   * @param instanceName
   * @return
   * @see #doNPF
   */
  PagingExerciseList<CommonShell, CommonExercise> makeNPFExerciseList(Panel right, String instanceName) {
    final PagingExerciseList<CommonShell, CommonExercise> exerciseList = makeExerciseList(right, instanceName);
    setFactory(exerciseList, instanceName, showQC);
    Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
      @Override
      public void execute() {
        exerciseList.onResize();
      }
    });
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
  private void rememberAndLoadFirst(final UserList<CommonShell> ul, HasID toSelect) {
    npfExerciseList.setUserListID(ul.getUniqueID());

    List<CommonShell> copy = new ArrayList<>();
    for (CommonShell ex : ul.getExercises()) {
      copy.add(ex);
    }
    String id = toSelect == null ? "": toSelect.getID();
    npfExerciseList.rememberAndLoadFirst(copy,"", "", id);
    npfExerciseList.setWidth("270px");
    npfExerciseList.getElement().getStyle().setProperty("minWidth", "270px");
  }

  /**
   * @param right
   * @param instanceName
   * @return
   * @see #makeNPFExerciseList
   */
  PagingExerciseList<CommonShell, CommonExercise> makeExerciseList(final Panel right, final String instanceName) {
    return new NPExerciseList(right, service, feedback, controller,
        true, instanceName, false) {
      @Override
      protected void onLastItem() {
        new ModalInfoDialog(COMPLETE, LIST_COMPLETE, new HiddenHandler() {
          @Override
          public void onHidden(HiddenEvent hiddenEvent) {
            reloadExercises();
          }
        });
      }
    };
  }

  /**
   * @param exerciseList
   * @param instanceName
   * @param showQC
   * @see #makeNPFExerciseList(com.google.gwt.user.client.ui.Panel, String)
   */
  private void setFactory(final PagingExerciseList<CommonShell, CommonExercise> exerciseList, final String instanceName, boolean showQC) {
    exerciseList.setFactory(getFactory(exerciseList, instanceName, showQC));
  }

  ExercisePanelFactory<CommonShell, CommonExercise> getFactory(
      final PagingExerciseList<CommonShell, CommonExercise> exerciseList,
      final String instanceName,
      final boolean showQC) {
    return new ExercisePanelFactory<CommonShell, CommonExercise>(service, feedback, controller, exerciseList) {
      @Override
      public Panel getExercisePanel(CommonExercise e) {
        if (showQC) {
          return new QCNPFExercise<>(e, controller, exerciseList, instanceName);
        } else {
          return new CommentNPFExercise<>(e, controller, exerciseList, false, instanceName);
        }
      }
    };
  }

  public void reload(UserList<CommonShell> userList) {
    rememberAndLoadFirst(userList, null);
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
    this.contentPanel = content;
  }
}
