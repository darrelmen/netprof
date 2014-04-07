package mitll.langtest.client.custom;

import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.ExercisePanelFactory;
import mitll.langtest.client.list.PagingExerciseList;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.client.user.UserManager;
import mitll.langtest.shared.custom.UserList;

/**
 * Created by GO22670 on 3/26/2014.
 */
public class ChapterNPFHelper extends NPFHelper {
  private FlexListLayout flexListLayout;

  /**
   * @see mitll.langtest.client.custom.Navigation#Navigation(mitll.langtest.client.LangTestDatabaseAsync, mitll.langtest.client.user.UserManager, mitll.langtest.client.exercise.ExerciseController, mitll.langtest.client.list.ListInterface, mitll.langtest.client.user.UserFeedback)
   * @param service
   * @param feedback
   * @param userManager
   * @param controller
   */
  public ChapterNPFHelper(final LangTestDatabaseAsync service, final UserFeedback feedback,
                          final UserManager userManager, final ExerciseController controller) {
    super(service, feedback, userManager, controller);
    final NPFHelper outer = this;
    this.flexListLayout = new FlexListLayout(service,feedback,userManager,controller) {
      @Override
      protected ExercisePanelFactory getFactory(PagingExerciseList exerciseList, String instanceName) {
        return outer.getFactory(exerciseList, instanceName);
      }
    };

    System.out.println(getClass() + " : ChapterNPFHelper flexListLayout " + flexListLayout );
  }

  /**
   * Left and right components
   * @param ul
   * @param instanceName
   * @return
   */
  protected Panel doInternalLayout(UserList ul, String instanceName) {
    System.out.println(getClass() + " : doInternalLayout instanceName = " + instanceName + " for list " + ul);
    Panel widgets = flexListLayout.doInternalLayout(ul, instanceName);
    npfExerciseList = flexListLayout.npfExerciseList;
    return widgets;
  }

  @Override
  public void onResize() {
    if (flexListLayout != null) {
      flexListLayout.onResize();
    } else if (npfExerciseList != null) {
      npfExerciseList.onResize();
    } else {
      System.err.println("not sending resize event - flexListLayout is null?");
    }
  }
}
