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
import mitll.langtest.shared.CommonUserExercise;
import mitll.langtest.shared.custom.UserList;

/**
 * Created by GO22670 on 3/26/2014.
 */
public class ReviewItemHelper extends NPFHelper {
  FlexListLayout flexListLayout;
  HasText itemMarker;
  ListInterface predefinedContent;
  NPFHelper npfHelper;

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
                          // final UserList originalList,
                          final ListInterface predefinedContent,
                          NPFHelper npfHelper) {
    super(service, feedback, userManager, controller);
    // final NPFHelper outer = this;
    this.itemMarker = itemMarker;
    this.predefinedContent = predefinedContent;
    this.npfHelper = npfHelper;
    System.out.println(getClass() + " : ReviewItemHelper "+ npfHelper);
  }

  /**
   * Left and right components
   *
   *
   *  TODO : after fixed -- item isn't removed -- do we need to update the exercise list too?
   * @param ul
   * @param instanceName
   * @return
   */
  protected Panel doInternalLayout(final UserList ul, String instanceName) {
    this.flexListLayout = new FlexListLayout(service,feedback,userManager,controller) {
      @Override
      protected ExercisePanelFactory getFactory(final PagingExerciseList pagingExerciseList, String instanceName) {
        return new ExercisePanelFactory(service,feedback,controller,predefinedContent) {
          @Override
          public Panel getExercisePanel(CommonExercise exercise) {

         //   makeListOfOnlyYourItems

            System.out.println("list " + pagingExerciseList);
            System.out.println("npfExerciseList " + npfExerciseList);

            ReviewEditableExercise reviewEditableExercise =
              new ReviewEditableExercise(service, controller, itemMarker, exercise.toCommonUserExercise(), ul,
                pagingExerciseList, predefinedContent, npfHelper);
            SimplePanel toAddTo = new SimplePanel();

            Panel widgets = reviewEditableExercise.addNew(ul, ul,
              //exerciseList,
              npfExerciseList,
              toAddTo);
            reviewEditableExercise.setFields(exercise);

            return widgets;



 /*           Panel panel = new SimplePanel();
            panel.getElement().setId("EditItemPanel");
            // TODO : do something better here than toCommonUserExercise
            reviewEditableExercise.populatePanel(e.toCommonUserExercise(), panel, ul, originalList, itemMarker, outer);
            return panel;*/
          }

    /*      private UserList makeListOfOnlyYourItems(UserList toCopy) {
            UserList copy2 = new UserList(toCopy);
            for (CommonUserExercise ue : toCopy.getExercises()) {
              copy2.addExercise(ue);
            }
            return copy2;
          }*/
        };
      }
    };

    Panel widgets = flexListLayout.doInternalLayout(ul, instanceName);
    npfExerciseList = flexListLayout.npfExerciseList;
    return widgets;
  }
}
