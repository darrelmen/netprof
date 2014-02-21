package mitll.langtest.client.custom;

import com.google.gwt.user.client.ui.HasText;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.shared.ExerciseShell;
import mitll.langtest.shared.StartupInfo;
import mitll.langtest.shared.custom.UserExercise;

/**
 * Created by GO22670 on 2/20/14.
 */
public class ChapterNewExercise<T extends ExerciseShell> extends NewUserExercise<T> {
  public ChapterNewExercise(final LangTestDatabaseAsync service,
                            ExerciseController controller, HasText itemMarker, EditItem editItem, UserExercise newExercise) {
    super(service, controller, itemMarker, editItem, newExercise);
  }

  /**
   * TODO add drop down widget to allow selection of unit/lesson
   * TODO add fields in user exercise to record info
   * TODO add SQL columns to read/write info
   * TODO add server side addition to hierarchy for search by unit/lesson
   * TODO add to global list of items?
   *
   * TODO : add delete capability?  = another table???
   *
   *
   * @param container
   */
  @Override
  protected void addItemsAtTop(Panel container) {
    //super.addItemsAtTop(container);
    StartupInfo startupInfo = controller.getStartupInfo();
    System.out.println("startup info " + startupInfo);
  }
}
