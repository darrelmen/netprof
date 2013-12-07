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
import mitll.langtest.client.list.PagingExerciseList;
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
  private PagingExerciseList npfExerciseList;
  private SimplePanel npfContentPanel;

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

  private void addNPFToContent(UserList ul, Panel listContent, String instanceName) {
    Panel npfContent = doNPF(ul,instanceName);
    listContent.add(npfContent);
    listContent.addStyleName("userListBackground");
  }

  private Panel doNPF(UserList ul, String instanceName) {
    Panel hp = new HorizontalPanel();
    SimplePanel left = new SimplePanel();
    hp.add(left);
    left.addStyleName("floatLeft");
    npfContentPanel = new SimplePanel();
    hp.add(npfContentPanel);
    npfContentPanel.addStyleName("floatRight");
    npfExerciseList = makeNPFExerciseList(npfContentPanel,instanceName/*,ul*/);

    left.add(npfExerciseList.getExerciseListOnLeftSide(controller.getProps()));
    rememberAndLoadFirst(ul);
    setupContent(hp);
    return hp;
  }

  private void rememberAndLoadFirst(UserList ul) {
    npfExerciseList.setUserList(ul);
    npfExerciseList.rememberAndLoadFirst(new ArrayList<UserExercise>(ul.getExercises()),null);
  }

  protected Panel setupContent(Panel hp) {
    return npfContentPanel;
  }

  private PagingExerciseList makeNPFExerciseList(SimplePanel right, String instanceName/*,UserList ul*/) {
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
        return new GoodwaveExercisePanel(e, controller, exerciseList, 0.65f,false, "classroom");
      }
    }, userManager, 1);
  }

  /**
   * @see #doNPF(mitll.langtest.shared.custom.UserList, String)
   * @return
   */
  protected SimplePanel getNpfContentPanel() {
    return npfContentPanel;
  }

  @Override
  public void onResize() {
    if (npfContentPanel != null) {
      float v = (Window.getClientWidth() * 0.8f) - 100;
      System.out.println("content width is " + v);
    //  npfContentPanel.setWidth(v + "px");
    }
  }

  //public void removeKeyHandler() {}
  //public void addKeyHandler() {}
}
