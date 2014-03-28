package mitll.langtest.client.custom;

import com.google.gwt.user.client.ui.HasText;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.client.user.UserManager;
import mitll.langtest.shared.CommonUserExercise;
import mitll.langtest.shared.custom.UserList;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 12/9/13
 * Time: 4:58 PM
 * To change this template use File | Settings | File Templates.
 */
public class ReviewEditItem extends EditItem {


  /**
   *
   * @param service
   * @param userManager
   * @param controller
   * @param npfHelper
   * @see Navigation#Navigation
   */
  public ReviewEditItem(final LangTestDatabaseAsync service, final UserManager userManager, ExerciseController controller,
                        ListInterface listInterface, UserFeedback feedback, NPFHelper npfHelper) {
    //super(service, userManager, controller, listInterface, feedback, npfHelper);
    super(service, userManager, controller, listInterface, feedback, null);
  }

  /**
   * @param exercise
   * @param itemMarker
   * @param originalList
   * @param doNewExercise
   * @return
   * @see #populatePanel
   */
  @Override
  protected NewUserExercise getAddOrEditPanel(CommonUserExercise exercise, HasText itemMarker, UserList originalList, boolean doNewExercise) {
    //return new ReviewEditableExercise(service,controller,this, itemMarker, exercise, originalList, exerciseList);
    return null;
  }
}
