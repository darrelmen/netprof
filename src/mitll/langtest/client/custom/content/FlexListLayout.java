package mitll.langtest.client.custom.content;

import com.github.gwtbootstrap.client.ui.FluidRow;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.user.client.ui.*;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.ExercisePanelFactory;
import mitll.langtest.client.list.ExerciseList;
import mitll.langtest.client.list.PagingExerciseList;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.client.user.UserManager;
import mitll.langtest.shared.custom.UserList;
import mitll.langtest.shared.exercise.CommonShell;
import mitll.langtest.shared.exercise.Shell;

import java.util.logging.Logger;

/**
 * Created by GO22670 on 3/28/2014.
 */
public abstract class FlexListLayout<T extends CommonShell> implements RequiresResize {
  private Logger logger = Logger.getLogger("FlexListLayout");

  public PagingExerciseList<T> npfExerciseList;

  final ExerciseController controller;
  final LangTestDatabaseAsync service;
  final UserFeedback feedback;
//  private final UserManager userManager;
  final boolean incorrectFirst;

  /**
   * @param service
   * @param feedback
   * @param userManager
   * @param controller
   * @see ChapterNPFHelper#ChapterNPFHelper(LangTestDatabaseAsync, UserFeedback, UserManager, ExerciseController, boolean)
   * @see ReviewItemHelper#doInternalLayout(mitll.langtest.shared.custom.UserList, String)
   */
  public FlexListLayout(LangTestDatabaseAsync service, UserFeedback feedback,
                        UserManager userManager, ExerciseController controller) {
    this.controller = controller;
    this.service = service;
    this.feedback = feedback;
  //  this.userManager = userManager;
    this.incorrectFirst = false;
  }

  /**
   * TODO : don't pass in user list
   * @param ul
   * @param instanceName
   * @return
   * @see ChapterNPFHelper#doInternalLayout(mitll.langtest.shared.custom.UserList, String)
   * @see ReviewItemHelper#doInternalLayout(mitll.langtest.shared.custom.UserList, String)
   */
  public Panel doInternalLayout(UserList<?> ul, String instanceName) {
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

    Panel currentExerciseVPanel = getCurrentExercisePanel();
    bottomRow.add(currentExerciseVPanel);

    long uniqueID = ul == null ? -1 : ul.getUniqueID();

    // TODO : only has to be paging b/c it needs to setUserListID
    PagingExerciseList<T> widgets = makeNPFExerciseList(topRow, currentExerciseVPanel, instanceName, uniqueID, incorrectFirst);
    npfExerciseList = widgets;

    addThirdColumn(bottomRow);

    if (npfExerciseList == null) {
      logger.warning("huh? exercise list is null for " + instanceName + " and " + uniqueID);
    } else {
      Widget exerciseListOnLeftSide = npfExerciseList.getExerciseListOnLeftSide(controller.getProps());
      exerciseListContainer.add(exerciseListOnLeftSide);
    }

    widgets.addWidgets();
    return twoRows;
  }

  protected Panel getCurrentExercisePanel() {
    FlowPanel currentExerciseVPanel = new FlowPanel();
    currentExerciseVPanel.getElement().setId("NPFHelper_defect_currentExercisePanel");
    currentExerciseVPanel.addStyleName("floatLeftList");
    return currentExerciseVPanel;
  }

  protected void addThirdColumn(Panel bottomRow) {}

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
   * @see #doInternalLayout(UserList, String)
   */
  private PagingExerciseList<T> makeNPFExerciseList(final Panel topRow, Panel currentExercisePanel, String instanceName,
                                           long userListID, boolean incorrectFirst) {
    final PagingExerciseList<T> exerciseList = makeExerciseList(topRow, currentExercisePanel, instanceName, incorrectFirst);
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

  protected abstract ExercisePanelFactory<T> getFactory(final PagingExerciseList<T> exerciseList, final String instanceName);

  protected abstract PagingExerciseList<T> makeExerciseList(final Panel topRow, Panel currentExercisePanel,
                                                   final String instanceName, boolean incorrectFirst);
//  {
//    return new NPFlexSectionExerciseList<T>(this, topRow, currentExercisePanel, instanceName, incorrectFirst);
//  }

  @Override
  public void onResize() {
    if (npfExerciseList != null) {
      npfExerciseList.onResize();
    }
  }
}
