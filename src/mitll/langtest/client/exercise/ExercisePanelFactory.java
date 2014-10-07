package mitll.langtest.client.exercise;

import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.shared.CommonExercise;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 7/9/12
 * Time: 6:18 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class ExercisePanelFactory {
  protected final LangTestDatabaseAsync service;
  protected final UserFeedback userFeedback;
  protected final ExerciseController controller;
  protected ListInterface exerciseList;

  /**
   * @see mitll.langtest.client.custom.dialog.EditItem#setFactory
   * @param service
   * @param userFeedback
   * @param controller
   * @param exerciseList
   */
  public ExercisePanelFactory(final LangTestDatabaseAsync service, final UserFeedback userFeedback,
                              final ExerciseController controller, ListInterface exerciseList) {
    this.service = service;
    this.userFeedback = userFeedback;
    this.controller = controller;
    this.exerciseList = exerciseList;
  }

  public void setExerciseList(ListInterface exerciseList) {
     this.exerciseList = exerciseList;
  }

  /**
   * @see mitll.langtest.client.list.ExerciseList#makeExercisePanel
   * @param e
   * @return
   */
  public abstract Panel getExercisePanel(CommonExercise e);
}
