/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

package mitll.langtest.client.custom.content;

import com.github.gwtbootstrap.client.ui.FluidRow;
import com.github.gwtbootstrap.client.ui.TabPanel;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.event.HiddenEvent;
import com.github.gwtbootstrap.client.ui.event.HiddenHandler;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.user.client.ui.*;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.bootstrap.FlexSectionExerciseList;
import mitll.langtest.client.custom.exercise.CommentNPFExercise;
import mitll.langtest.client.custom.tabs.TabAndContent;
import mitll.langtest.client.dialog.ModalInfoDialog;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.ExercisePanelFactory;
import mitll.langtest.client.list.HistoryExerciseList;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.client.list.PagingExerciseList;
import mitll.langtest.client.qc.QCNPFExercise;
import mitll.langtest.client.scoring.GoodwaveExercisePanelFactory;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.client.user.UserManager;
import mitll.langtest.shared.CommonExercise;
import mitll.langtest.shared.CommonShell;
import mitll.langtest.shared.custom.UserList;

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
  Logger logger = Logger.getLogger("NPFHelper");

  protected static final String LIST_COMPLETE = "List complete!";
  protected static final String COMPLETE = "Complete";
  private boolean madeNPFContent = false;

  protected final LangTestDatabaseAsync service;
  protected final ExerciseController controller;
  protected final UserManager userManager;

  protected final UserFeedback feedback;
  public PagingExerciseList npfExerciseList;
  private final boolean showQC;
  DivWidget contentPanel;
  protected String instanceName;

  /**
   * @param service
   * @param feedback
   * @param userManager
   * @param controller
   * @param showQC
   * @see mitll.langtest.client.custom.Navigation#Navigation
   * @see mitll.langtest.client.custom.ListManager#ListManager(LangTestDatabaseAsync, UserManager, ExerciseController, UserFeedback, TabPanel)
   */
  public NPFHelper(LangTestDatabaseAsync service, UserFeedback feedback, UserManager userManager,
                   ExerciseController controller, boolean showQC) {
    this.service = service;
    this.feedback = feedback;
    this.controller = controller;
    this.userManager = userManager;
    this.showQC = showQC;
  }

  public void showNPF(UserList ul, TabAndContent tabAndContent, String instanceName, boolean loadExercises) {
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
   * @see mitll.langtest.client.custom.ListManager#getListOperations(UserList, String)
   */
  public void showNPF(UserList ul, TabAndContent tabAndContent, String instanceName, boolean loadExercises,
                      CommonExercise toSelect) {
    logger.info(getClass() + " : adding npf content instanceName = " + instanceName + " for list " + ul);
    this.instanceName = instanceName;
    DivWidget content = tabAndContent.getContent();
    int widgetCount = content.getWidgetCount();
    if (!madeNPFContent || widgetCount == 0) {
      madeNPFContent = true;
      logger.info("\t: adding npf content instanceName = " + instanceName + " for list " + ul);
      // System.out.println("\t: first is = " + instanceName + "  " + ul.getExercises().iterator().next().getID());
      addNPFToContent(ul, content, instanceName, loadExercises, toSelect);
    } else {
      logger.info("\t: rememberAndLoadFirst instanceName = " + instanceName + " for list " + ul);
      //   System.out.println("\t: first is = " + instanceName + "  " + ul.getExercises().iterator().next().getID());
      rememberAndLoadFirst(ul, toSelect);
    }
  }

  public ListInterface getExerciseList() {
    return npfExerciseList;
  }

  private void addNPFToContent(UserList ul, Panel listContent, String instanceName, boolean loadExercises,
                               CommonExercise toSelect) {
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
   * @see #addNPFToContent(UserList, Panel, String, boolean, CommonExercise)
   */
  private Panel doNPF(UserList ul, String instanceName, boolean loadExercises, CommonExercise toSelect) {
    // System.out.println(getClass() + " : doNPF instanceName = " + instanceName + " for list " + ul);
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
   * @see #doNPF(UserList, String, boolean, String)
   */
  protected Panel doInternalLayout(UserList ul, String instanceName) {
    //System.out.println(getClass() + " : doInternalLayout instanceName = " + instanceName + " for list " + ul);
    logger.info(getClass() + " : doInternalLayout instanceName = " + instanceName + " for list " + ul);

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

  protected Panel getRightSideContent(UserList ul, String instanceName) {
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
  PagingExerciseList makeNPFExerciseList(Panel right, String instanceName) {
    final PagingExerciseList exerciseList = makeExerciseList(right, instanceName);
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
   * @param ul
   * @param toSelect
   * @see #doNPF
   * @see #showNPF
   */
  private void rememberAndLoadFirst(final UserList ul, CommonExercise toSelect) {
    npfExerciseList.setUserListID(ul.getUniqueID());
    List<CommonShell> copy = new ArrayList<CommonShell>(ul.getExercises());
    logger.info("rememberAndLoadFirst " + copy.size() + " exercises from  " +ul.getName());
    //  npfExerciseList.rememberAndLoadFirst(new ArrayList<CommonShell>(ul.getExercises()));
    npfExerciseList.rememberAndLoadFirst(copy, toSelect, "");
    npfExerciseList.setWidth("270px");
    npfExerciseList.getElement().getStyle().setProperty("minWidth", "270px");
  }

  /**
   * @param right
   * @param instanceName
   * @return
   * @see #makeNPFExerciseList
   */
  PagingExerciseList makeExerciseList(final Panel right, final String instanceName) {
    return new HistoryExerciseList(right, service, feedback, controller,
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
  void setFactory(final PagingExerciseList exerciseList, final String instanceName, boolean showQC) {
    exerciseList.setFactory(getFactory(exerciseList, instanceName, showQC));
  }

  protected ExercisePanelFactory getFactory(final PagingExerciseList exerciseList, final String instanceName, final boolean showQC) {
    return new GoodwaveExercisePanelFactory(service, feedback, controller, exerciseList, 1.0f) {
      @Override
      public Panel getExercisePanel(CommonExercise e) {
        if (showQC) {
          return new QCNPFExercise(e, controller, exerciseList, instanceName);
        } else {
          return new CommentNPFExercise(e, controller, exerciseList, false, instanceName);
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
    this.contentPanel = content;
  }

  public String getInstanceName() {
    return instanceName;
  }

  /**
   * Created by GO22670 on 3/28/2014.
   */
  public abstract static class FlexListLayout implements RequiresResize {
    Logger logger = Logger.getLogger("FlexListLayout");

    public PagingExerciseList npfExerciseList;
    private final ExerciseController controller;
    private final LangTestDatabaseAsync service;
    private final UserFeedback feedback;
    private final UserManager userManager;
    final boolean incorrectFirst;

    /**
     * @param service
     * @param feedback
     * @param userManager
     * @param controller
     * @see ChapterNPFHelper#ChapterNPFHelper(mitll.langtest.client.LangTestDatabaseAsync, mitll.langtest.client.user.UserFeedback, mitll.langtest.client.user.UserManager, mitll.langtest.client.exercise.ExerciseController, boolean)
     * @see ReviewItemHelper#doInternalLayout(mitll.langtest.shared.custom.UserList, String)
     */
    public FlexListLayout(LangTestDatabaseAsync service, UserFeedback feedback,
                          UserManager userManager, ExerciseController controller) {
      this.controller = controller;
      this.service = service;
      this.feedback = feedback;
      this.userManager = userManager;
      this.incorrectFirst = false;
    }

    /**
     * @param ul
     * @param instanceName
     * @return
     * @see ChapterNPFHelper#doInternalLayout(mitll.langtest.shared.custom.UserList, String)
     * @see ReviewItemHelper#doInternalLayout(mitll.langtest.shared.custom.UserList, String)
     */
    public Panel doInternalLayout(UserList ul, String instanceName) {
      Panel twoRows = new FlowPanel();
      twoRows.getElement().setId("NPFHelper_twoRows");

      Panel exerciseListContainer = new SimplePanel();
      exerciseListContainer.addStyleName("floatLeft");
      exerciseListContainer.getElement().setId("NPFHelper_exerciseListContainer");

      // second row ---------------
      FluidRow topRow = new FluidRow();
      topRow.getElement().setId("NPFHelper_topRow");

      twoRows.add(topRow);

      Panel bottomRow = new HorizontalPanel();
      bottomRow.add(exerciseListContainer);
      bottomRow.getElement().setId("NPFHelper_bottomRow");
      styleBottomRow(bottomRow);

      twoRows.add(bottomRow);

      FlowPanel currentExerciseVPanel = new FlowPanel();
      currentExerciseVPanel.getElement().setId("NPFHelper_defect_currentExercisePanel");

      currentExerciseVPanel.addStyleName("floatLeftList");
      bottomRow.add(currentExerciseVPanel);

      long uniqueID = ul == null ? -1 : ul.getUniqueID();
      FlexSectionExerciseList widgets = makeNPFExerciseList(topRow, currentExerciseVPanel, instanceName, uniqueID, incorrectFirst);
      npfExerciseList = widgets;

      if (npfExerciseList == null) {
        logger.warning("huh? exercise list is null for " + instanceName + " and " + ul);
      } else {
        Widget exerciseListOnLeftSide = npfExerciseList.getExerciseListOnLeftSide(controller.getProps());
        exerciseListContainer.add(exerciseListOnLeftSide);
      }

      widgets.addWidgets();
      return twoRows;
    }

    protected void styleBottomRow(Panel bottomRow) {
      bottomRow.setWidth("100%");
      bottomRow.addStyleName("trueInlineStyle");
    }

    /**
     * @param topRow
     * @param currentExercisePanel
     * @param instanceName
     * @param userListID
     * @param incorrectFirst
     * @return
     * @see #doInternalLayout(mitll.langtest.shared.custom.UserList, String)
     */
    private FlexSectionExerciseList makeNPFExerciseList(final Panel topRow, Panel currentExercisePanel, String instanceName,
                                                        long userListID, boolean incorrectFirst) {
      final FlexSectionExerciseList exerciseList = makeExerciseList(topRow, currentExercisePanel, instanceName, incorrectFirst);
      exerciseList.setUserListID(userListID);

      exerciseList.setFactory(getFactory(exerciseList, instanceName));
      Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
        @Override
        public void execute() {
          exerciseList.onResize();
        }
      });
      return exerciseList;
    }

    protected abstract ExercisePanelFactory getFactory(final PagingExerciseList exerciseList, final String instanceName);

    protected FlexSectionExerciseList makeExerciseList(final Panel topRow, Panel currentExercisePanel,
                                                       final String instanceName, boolean incorrectFirst) {
      return new MyFlexSectionExerciseList(topRow, currentExercisePanel, instanceName, incorrectFirst);
    }

    @Override
    public void onResize() {
      if (npfExerciseList != null) {
        npfExerciseList.onResize();
      }
    }

    protected class MyFlexSectionExerciseList extends FlexSectionExerciseList {
      /**
       * @param topRow
       * @param currentExercisePanel
       * @param instanceName
       * @param incorrectFirst
       * @seex mitll.langtest.client.custom.Navigation.RecorderNPFHelper#getMyListLayout(mitll.langtest.client.LangTestDatabaseAsync, mitll.langtest.client.user.UserFeedback, mitll.langtest.client.user.UserManager, mitll.langtest.client.exercise.ExerciseController, mitll.langtest.client.custom.SimpleChapterNPFHelper)
       * @see mitll.langtest.client.custom.Navigation#makePracticeHelper(mitll.langtest.client.LangTestDatabaseAsync, mitll.langtest.client.user.UserManager, mitll.langtest.client.exercise.ExerciseController, mitll.langtest.client.user.UserFeedback)
       * @see mitll.langtest.client.custom.content.NPFHelper.FlexListLayout#makeExerciseList(com.google.gwt.user.client.ui.Panel, com.google.gwt.user.client.ui.Panel, String, boolean)
       */
      public MyFlexSectionExerciseList(Panel topRow, Panel currentExercisePanel, String instanceName, boolean incorrectFirst) {
        super(topRow, currentExercisePanel, FlexListLayout.this.service, FlexListLayout.this.feedback, FlexListLayout.this.controller, instanceName, incorrectFirst);
      }

      @Override
      protected void onLastItem() {
        new ModalInfoDialog("Complete", "List complete!", new HiddenHandler() {
          @Override
          public void onHidden(HiddenEvent hiddenEvent) {
            reloadExercises();
          }
        });
      }

      @Override
      protected void noSectionsGetExercises(long userID) {
        loadExercises(getHistoryToken("", ""), getPrefix(), false);
      }
    }
  }
}
