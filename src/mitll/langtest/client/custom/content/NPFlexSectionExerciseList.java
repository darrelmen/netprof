package mitll.langtest.client.custom.content;

import com.github.gwtbootstrap.client.ui.event.HiddenEvent;
import com.github.gwtbootstrap.client.ui.event.HiddenHandler;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.bootstrap.FlexSectionExerciseList;
import mitll.langtest.client.dialog.ModalInfoDialog;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.client.user.UserManager;
import mitll.langtest.shared.exercise.CommonShell;

/**
 * Created by go22670 on 1/26/16.
 */
public class NPFlexSectionExerciseList<T extends CommonShell> extends FlexSectionExerciseList<T> {
  private FlexListLayout flexListLayout;

  /**
   * @param topRow
   * @param currentExercisePanel
   * @param instanceName
   * @param incorrectFirst
   * @see mitll.langtest.client.custom.Navigation#makePracticeHelper(LangTestDatabaseAsync, UserManager, ExerciseController, UserFeedback)
   * @see FlexListLayout#makeExerciseList(Panel, Panel, String, boolean)
   */
  public NPFlexSectionExerciseList(FlexListLayout flexListLayout, Panel topRow, Panel currentExercisePanel, String instanceName,
                                   boolean incorrectFirst) {
    super(topRow, currentExercisePanel, flexListLayout.service, flexListLayout.feedback,
        flexListLayout.controller, instanceName, incorrectFirst);
    this.flexListLayout = flexListLayout;
  }

  @Override
  protected void onLastItem() {
    new ModalInfoDialog("Complete", "List complete!", new HiddenHandler() {
      @Override
      public void onHidden(HiddenEvent hiddenEvent) {
        reloadExercises();
      }
    });
  }

  @Override
  protected void noSectionsGetExercises(long userID) {
    loadExercises(getHistoryToken("", ""), getPrefix(), false);
  }
}
