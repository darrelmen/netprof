package mitll.langtest.client.custom;

import com.github.gwtbootstrap.client.ui.event.HiddenEvent;
import com.github.gwtbootstrap.client.ui.event.HiddenHandler;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.SimplePanel;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.dialog.ModalInfoDialog;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.list.PagingExerciseList;
import mitll.langtest.client.scoring.GoodwaveExercisePanelFactory;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.client.user.UserManager;
import mitll.langtest.shared.Exercise;
import mitll.langtest.shared.Result;
import mitll.langtest.shared.custom.UserExercise;
import mitll.langtest.shared.custom.UserList;

import java.util.ArrayList;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 10/8/13
 * Time: 3:27 PM
 * To change this template use File | Settings | File Templates.
 */
public class NPFHelper implements RequiresResize {
  private boolean madeNPFContent = false;

  private LangTestDatabaseAsync service;
  private final ExerciseController controller;
  private UserManager userManager;

  private UserFeedback feedback;
  private PagingExerciseList<UserExercise> npfExerciseList;
  private SimplePanel npfContentPanel;

  /**
   * @see mitll.langtest.client.custom.Navigation#Navigation(mitll.langtest.client.LangTestDatabaseAsync, mitll.langtest.client.user.UserManager, mitll.langtest.client.exercise.ExerciseController, mitll.langtest.client.list.ListInterface, mitll.langtest.client.user.UserFeedback)
   * @param service
   * @param feedback
   * @param userManager
   * @param controller
   */
  public NPFHelper(LangTestDatabaseAsync service, UserFeedback feedback, UserManager userManager, ExerciseController controller) {
    this.service = service;
    this.feedback = feedback;
    this.controller = controller;
    this.userManager = userManager;
  }

  /**
   * @see Navigation#getListOperations
   * @param ul
   * @param learn
   * @param instanceName
   */
  public void showNPF(UserList ul, Navigation.TabAndContent learn, String instanceName) {
    System.out.println(getClass() + " : adding npf content instanceName = " + instanceName + " for list " + ul/* + " " + getElement().getID()*/);

    int widgetCount = learn.content.getWidgetCount();
    if (!madeNPFContent || widgetCount == 0) {
      addNPFToContent(ul, learn.content, instanceName);
      madeNPFContent = true;
    } else {
      rememberAndLoadFirst(ul);
    }
  }

  private void addNPFToContent(UserList ul, Panel listContent, String instanceName) {
    Panel npfContent = doNPF(ul,instanceName);
    listContent.add(npfContent);
    listContent.addStyleName("userListBackground");
  }

  private Panel doNPF(UserList ul, String instanceName) {
    Panel hp = new HorizontalPanel();
    Panel left = new SimplePanel();
    hp.add(left);
    left.addStyleName("floatLeft");
    npfContentPanel = new SimplePanel();
    hp.add(npfContentPanel);
    npfContentPanel.addStyleName("floatRight");
    npfExerciseList = makeNPFExerciseList(npfContentPanel, instanceName + "_"+ul.getName());

    left.add(npfExerciseList.getExerciseListOnLeftSide(controller.getProps()));
    rememberAndLoadFirst(ul);
    setupContent(hp);
    return hp;
  }

  private void rememberAndLoadFirst(final UserList ul) {
    if (controller.isReviewMode()) {
      System.out.println("review mode " + controller.isReviewMode());
      service.getCompletedExercises(controller.getUser(), controller.isReviewMode(), new AsyncCallback<Set<String>>() {
        @Override
        public void onFailure(Throwable caught) {
        }

        @Override
        public void onSuccess(Set<String> result) {
          npfExerciseList.setCompleted(result);
          npfExerciseList.setUserListID(ul.getUniqueID());
          npfExerciseList.rememberAndLoadFirst(new ArrayList<UserExercise>(ul.getExercises()));
        }
      });
    } else {
      npfExerciseList.setUserListID(ul.getUniqueID());
      npfExerciseList.rememberAndLoadFirst(new ArrayList<UserExercise>(ul.getExercises()));
    }
  }

  protected Panel setupContent(Panel hp) { return npfContentPanel; }

  /**
   * @see #doNPF
   * @param right
   * @param instanceName
   * @return
   */
  private PagingExerciseList<UserExercise> makeNPFExerciseList(Panel right, String instanceName) {
    final PagingExerciseList<UserExercise> exerciseList = new PagingExerciseList<UserExercise>(right, service, feedback, false, false, controller,
      true, instanceName) {
      @Override
      protected void onLastItem() {
        new ModalInfoDialog("Complete","List complete!", new HiddenHandler() {
          @Override
          public void onHidden(HiddenEvent hiddenEvent) {
            reloadExercises();
          }
        });
      }
    };
    setFactory(exerciseList, instanceName);
    exerciseList.setUnaccountedForVertical(320);
    Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
      @Override
      public void execute() {
        exerciseList.onResize();
      }
    });
    return exerciseList;
  }

  protected void setFactory(final PagingExerciseList<UserExercise> exerciseList, final String instanceName) {
    exerciseList.setFactory(new GoodwaveExercisePanelFactory(service, feedback, controller, exerciseList, 1.0f) {
      @Override
      public Panel getExercisePanel(Exercise e) {
        if (controller.getAudioType().equalsIgnoreCase(Result.AUDIO_TYPE_REVIEW)) {
          System.out.println("\nNPFHelper : making new QCNPFExercise for " +e + " instance " + instanceName);
          return new QCNPFExercise(e, controller, exerciseList, 1.0f, false, instanceName);
        }
        else {
          System.out.println("\nmaking new CommentNPFExercise for " +e + " instance " + instanceName);
          return new CommentNPFExercise(e, controller, exerciseList, 1.0f, false, instanceName);
        }
      }
    }, userManager, 1);
  }

  /**
   * @see #doNPF(mitll.langtest.shared.custom.UserList, String)
   * @return
   */
  protected SimplePanel getNpfContentPanel() { return npfContentPanel; }

  @Override
  public void onResize() { if (npfContentPanel != null) {  npfExerciseList.onResize(); } }

  /**
   * @see mitll.langtest.client.custom.EditItem.EditableExercise#getCreateButton(mitll.langtest.shared.custom.UserList, mitll.langtest.client.list.ListInterface, com.google.gwt.user.client.ui.Panel, com.github.gwtbootstrap.client.ui.ControlGroup)
   */
  public void reload() {
    if (npfExerciseList == null) {
      System.err.println("how can npfExerciseList be null?");
    }
    else {
      npfExerciseList.redraw();
    }
  }
}
