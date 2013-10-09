package mitll.langtest.client.custom;

import com.github.gwtbootstrap.client.ui.event.HiddenEvent;
import com.github.gwtbootstrap.client.ui.event.HiddenHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.SimplePanel;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.dialog.ModalInfoDialog;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.PagingExerciseList;
import mitll.langtest.client.scoring.GoodwaveExercisePanel;
import mitll.langtest.client.scoring.GoodwaveExercisePanelFactory;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.client.user.UserManager;
import mitll.langtest.shared.Exercise;
import mitll.langtest.shared.custom.UserExercise;
import mitll.langtest.shared.custom.UserList;

import java.util.ArrayList;

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

  public NPFHelper(LangTestDatabaseAsync service, UserFeedback feedback, UserManager userManager, ExerciseController controller) {
    this.service = service;
    this.feedback = feedback;
    this.controller = controller;
    this.userManager = userManager;
  }

  /**
   * @see Navigation#getListOperations(mitll.langtest.shared.custom.UserList, boolean)
   * @param ul
   * @param learn
   * @param instanceName
   */
  public void showNPF(UserList ul, Navigation.TabAndContent learn,String instanceName) {
    int widgetCount = learn.content.getWidgetCount();
    if (!madeNPFContent || widgetCount == 0) {
      System.out.println(getClass() + " : adding npf content widget count = " + widgetCount);
      addNPFToContent(ul, learn.content,instanceName);
      System.out.println(getClass() + " : after adding npf content widget count = " + learn.content.getWidgetCount());

      madeNPFContent = true;
    } else {
      System.out.println(getClass() + " : *NOT* adding npf content widget count = " + widgetCount);
      rememberAndLoadFirst(ul);
      System.out.println(getClass() + " : *NOT* after adding npf content widget count = " + learn.content.getWidgetCount());

    }
  }

  private void rememberAndLoadFirst(UserList ul) {
    npfExerciseList.rememberAndLoadFirst(new ArrayList<UserExercise>(ul.getExercises()));
  }

  private PagingExerciseList npfExerciseList;

  private void addNPFToContent(UserList ul, Panel listContent, String instanceName) {
    Panel npfContent = doNPF(ul,instanceName);

    listContent.add(npfContent);
    listContent.addStyleName("userListBackground");
  }

  private SimplePanel npfContentPanel;
  private Panel doNPF(UserList ul, String instanceName) {
    HorizontalPanel hp = new HorizontalPanel();
    SimplePanel left = new SimplePanel();
    hp.add(left);
    npfContentPanel = new SimplePanel();
    npfExerciseList = makeNPFExerciseList(getNpfContentPanel(),instanceName);

    left.add(npfExerciseList.getExerciseListOnLeftSide(controller.getProps()));
    rememberAndLoadFirst(ul);
    setupContent(hp);
    return hp;
  }

  protected SimplePanel setupContent(HorizontalPanel hp) {
    hp.add(getNpfContentPanel());
    getNpfContentPanel().addStyleName("greenBackground");
    getNpfContentPanel().addStyleName("userNPFContent");
    return npfContentPanel;
  }

  private PagingExerciseList makeNPFExerciseList(SimplePanel right, String instanceName) {
    PagingExerciseList exerciseList = new PagingExerciseList(right, service, feedback, false, false, controller, instanceName) {
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
    setFactory(exerciseList);
    return exerciseList;
  }

  protected void setFactory(final PagingExerciseList exerciseList) {
    exerciseList.setFactory(new GoodwaveExercisePanelFactory(service, feedback, controller, exerciseList, 0.7f) {
      @Override
      public Panel getExercisePanel(Exercise e) {
        return new GoodwaveExercisePanel(e, controller, exerciseList, 0.65f);
      }
    }, userManager, 1);
  }

  public SimplePanel getNpfContentPanel() {
    return npfContentPanel;
  }

  @Override
  public void onResize() {
    if (npfContentPanel != null) npfContentPanel.setWidth(Window.getClientWidth()*0.7f  + "px");
  }

  public void disableKeyHandler() {
    //To change body of created methods use File | Settings | File Templates.
  }
}
