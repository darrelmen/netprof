package mitll.langtest.client.custom;

import com.google.gwt.user.client.ui.HasText;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.SimplePanel;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.ExercisePanelFactory;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.client.list.PagingExerciseList;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.client.user.UserManager;
import mitll.langtest.shared.CommonExercise;
import mitll.langtest.shared.custom.UserList;

/**
 * Created by GO22670 on 3/26/2014.
 */
public class ReviewItemHelper extends NPFHelper {
  private FlexListLayout flexListLayout;
  private HasText itemMarker;
  private ListInterface predefinedContent;
  private NPFHelper npfHelper;

  /**
   * @see mitll.langtest.client.custom.Navigation#Navigation(mitll.langtest.client.LangTestDatabaseAsync, mitll.langtest.client.user.UserManager, mitll.langtest.client.exercise.ExerciseController, mitll.langtest.client.list.ListInterface, mitll.langtest.client.user.UserFeedback)
   * @param service
   * @param feedback
   * @param userManager
   * @param controller
   * @param itemMarker
   * @param predefinedContent
   */
  public ReviewItemHelper(final LangTestDatabaseAsync service, final UserFeedback feedback,
                          final UserManager userManager, final ExerciseController controller,
                          final HasText itemMarker,
                          final ListInterface predefinedContent,
                          NPFHelper npfHelper) {
    super(service, feedback, userManager, controller);
    this.itemMarker = itemMarker;
    this.predefinedContent = predefinedContent;
    this.npfHelper = npfHelper;
    System.out.println(getClass() + " : ReviewItemHelper ");
  }

  /**
   * Left and right components
   *
   *
   * @param ul
   * @param instanceName
   * @return
   */
  protected Panel doInternalLayout(final UserList ul, String instanceName) {
    System.out.println(getClass() + " : doInternalLayout instanceName = " + instanceName + " for list " + ul);

    this.flexListLayout = new FlexListLayout(service,feedback,userManager,controller) {
      @Override
      protected ExercisePanelFactory getFactory(final PagingExerciseList pagingExerciseList, String instanceName) {
        return new ExercisePanelFactory(service,feedback,controller,predefinedContent) {
          @Override
          public Panel getExercisePanel(CommonExercise exercise) {
            ReviewEditableExercise reviewEditableExercise =
              new ReviewEditableExercise(service, controller, itemMarker, exercise.toCommonUserExercise(), ul,
                pagingExerciseList, predefinedContent, npfHelper);
            SimplePanel ignoredContainer = new SimplePanel();

            Panel widgets = reviewEditableExercise.addNew(ul, ul,
              npfExerciseList,
              ignoredContainer);
            reviewEditableExercise.setFields(exercise);

            return widgets;
          }
        };
      }
    };

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
