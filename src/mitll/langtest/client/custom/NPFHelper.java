package mitll.langtest.client.custom;

import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.event.HiddenEvent;
import com.github.gwtbootstrap.client.ui.event.HiddenHandler;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.SimplePanel;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.dialog.ModalInfoDialog;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.ExercisePanelFactory;
import mitll.langtest.client.list.PagingExerciseList;
import mitll.langtest.client.scoring.GoodwaveExercisePanelFactory;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.client.user.UserManager;
import mitll.langtest.shared.CommonExercise;
import mitll.langtest.shared.CommonShell;
import mitll.langtest.shared.custom.UserList;

import java.util.ArrayList;

/**
 * Lets you show a user list with a paging container...
 * User: GO22670
 * Date: 10/8/13
 * Time: 3:27 PM
 * To change this template use File | Settings | File Templates.
 */
class NPFHelper implements RequiresResize {
  private boolean madeNPFContent = false;

  protected final LangTestDatabaseAsync service;
  protected final ExerciseController controller;
  protected final UserManager userManager;

  protected final UserFeedback feedback;
  protected PagingExerciseList npfExerciseList;
  private Panel npfContentPanel;
  boolean showQC;

  /**
   * @see mitll.langtest.client.custom.Navigation#Navigation
   * @param service
   * @param feedback
   * @param userManager
   * @param controller
   * @param showQC
   */
  public NPFHelper(LangTestDatabaseAsync service, UserFeedback feedback, UserManager userManager, ExerciseController controller, boolean showQC) {
    this.service = service;
    this.feedback = feedback;
    this.controller = controller;
    this.userManager = userManager;
    this.showQC = showQC;
  }

  /**
   * Add npf widget to content of a tab - here marked tabAndContent
   * @see Navigation#getListOperations
   * @param ul show this user list
   * @param tabAndContent in this tab
   * @param instanceName flex, review, etc.
   * @param loadExercises should we load exercises initially
   */
  public void showNPF(UserList ul, TabAndContent tabAndContent, String instanceName, boolean loadExercises) {
    //System.out.println(getClass() + " : adding npf content instanceName = " + instanceName + " for list " + ul);
    DivWidget content = tabAndContent.content;
    int widgetCount = content.getWidgetCount();
    if (!madeNPFContent || widgetCount == 0) {
      madeNPFContent = true;
      System.out.println("\t: adding npf content instanceName = " + instanceName + " for list " + ul);
      System.out.println("\t: first is = " + instanceName + "  " + ul.getExercises().iterator().next().getID());
      addNPFToContent(ul, content, instanceName, loadExercises);
    } else {
      System.out.println("\t: rememberAndLoadFirst instanceName = " + instanceName + " for list " + ul);
      System.out.println("\t: first is = " + instanceName + "  " + ul.getExercises().iterator().next().getID());
      rememberAndLoadFirst(ul);
    }
  }

  private void addNPFToContent(UserList ul, Panel listContent, String instanceName, boolean loadExercises) {
    Panel npfContent = doNPF(ul, instanceName, loadExercises);
    listContent.add(npfContent);
    listContent.addStyleName("userListBackground");
  }

  /**
   * Make the instance name uses the unique id for the list.
   *
   * @param ul
   * @param instanceName
   * @param loadExercises
   * @return
   */
  private Panel doNPF(UserList ul, String instanceName, boolean loadExercises) {
   // System.out.println(getClass() + " : doNPF instanceName = " + instanceName + " for list " + ul);
    Panel hp = doInternalLayout(ul, instanceName);

    if (loadExercises) {
      rememberAndLoadFirst(ul);
    }
    setupContent(hp);
    return hp;
  }

  /**
   * Left and right components
   * @param ul
   * @param instanceName
   * @return
   */
  protected Panel doInternalLayout(UserList ul, String instanceName) {
    //System.out.println(getClass() + " : doInternalLayout instanceName = " + instanceName + " for list " + ul);

    Panel hp = new HorizontalPanel();
    hp.getElement().setId("internalLayout_Row");

    Panel left = new SimplePanel();
    left.getElement().setId("internalLayout_LeftCol");
    left.addStyleName("floatLeft");

    hp.add(left);

    npfContentPanel = getRightSideContent(ul, instanceName);

    left.add(npfExerciseList.getExerciseListOnLeftSide(controller.getProps()));

    hp.add(npfContentPanel);

    return hp;
  }

  protected Panel getRightSideContent(UserList ul, String instanceName) {
    Panel npfContentPanel = new SimplePanel();
    npfContentPanel.addStyleName("floatRight");
    npfContentPanel.getElement().setId("internalLayout_RightContent");

    npfExerciseList = makeNPFExerciseList(npfContentPanel, instanceName + "_"+ul.getUniqueID());
    return npfContentPanel;
  }

  /**
   * @see #doNPF
   * @param right
   * @param instanceName
   * @return
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
   * @see #doNPF(mitll.langtest.shared.custom.UserList, String, boolean)
   * @see #showNPF(mitll.langtest.shared.custom.UserList, TabAndContent, String, boolean)
   * @param ul
   */
  private void rememberAndLoadFirst(final UserList ul) {
    System.out.println(getClass() + ".rememberAndLoadFirst : for " +ul);
    npfExerciseList.setUserListID(ul.getUniqueID());
    npfExerciseList.rememberAndLoadFirst(new ArrayList<CommonShell>(ul.getExercises()));
  }

  Panel setupContent(Panel hp) {
    return npfContentPanel;
  }

  PagingExerciseList makeExerciseList(final Panel right, final String instanceName) {
    //System.out.println(getClass() + ".makeExerciseList : instanceName " + instanceName);
    return new PagingExerciseList(right, service, feedback, null, controller, false, false,
      true, instanceName) {
      @Override
      protected void onLastItem() {
        new ModalInfoDialog("Complete", "List complete!", new HiddenHandler() {
          @Override
          public void onHidden(HiddenEvent hiddenEvent) {
            reloadExercises();
          }
        });
      }
    };
  }

  /**
   * @see #makeNPFExerciseList(com.google.gwt.user.client.ui.Panel, String)
   * @param exerciseList
   * @param instanceName
   * @param showQC
   */
  void setFactory(final PagingExerciseList exerciseList, final String instanceName, boolean showQC) {
    exerciseList.setFactory(getFactory(exerciseList, instanceName, showQC), userManager, 1);
  }

  protected ExercisePanelFactory getFactory(final PagingExerciseList exerciseList, final String instanceName, final boolean showQC) {
    return new GoodwaveExercisePanelFactory(service, feedback, controller, exerciseList, 1.0f) {
      @Override
      public Panel getExercisePanel(CommonExercise e) {
        //boolean showQC = controller.getAudioType().equalsIgnoreCase(Result.AUDIO_TYPE_REVIEW);
        if (showQC) {
          System.out.println("\nNPFHelper : making new QCNPFExercise for " +e + " instance " + instanceName);
          return new QCNPFExercise(e, controller, exerciseList, 1.0f, false, instanceName);
        }
        else {
          System.out.println("\nmaking new CommentNPFExercise for " +e + " instance " + instanceName);
          return new CommentNPFExercise(e, controller, exerciseList, 1.0f, false, instanceName);
        }
      }
    };
  }

  /**
   * @see #doNPF(mitll.langtest.shared.custom.UserList, String, boolean)
   * @return
   */
  Panel getNpfContentPanel() { return npfContentPanel; }

  @Override
  public void onResize() { if (npfContentPanel != null) {  npfExerciseList.onResize(); } }
}
