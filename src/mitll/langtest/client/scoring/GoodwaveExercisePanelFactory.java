/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

package mitll.langtest.client.scoring;

import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.ExercisePanelFactory;
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
public class GoodwaveExercisePanelFactory extends ExercisePanelFactory {
  private final float screenPortion;

  /**
   * @see mitll.langtest.client.LangTest#onModuleLoad2()
   * @param service
   * @param userFeedback
   * @param controller
   * @param listContainer
   * @param screenPortion
   */
  public GoodwaveExercisePanelFactory(final LangTestDatabaseAsync service, final UserFeedback userFeedback,
                                      final ExerciseController controller, ListInterface listContainer, float screenPortion) {
    super(service, userFeedback, controller, listContainer);
    this.screenPortion = screenPortion;
  }

  @Override
  public Panel getExercisePanel(CommonExercise e) {
    return new GoodwaveExercisePanel(e, controller, exerciseList, screenPortion,
      false // don't do keybinding stuff in classroom mode... at least for now
      ,
      "normal");
  }
}
