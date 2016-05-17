/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

package mitll.langtest.client.exercise;

import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.shared.exercise.Shell;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 7/9/12
 * Time: 6:18 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class ExercisePanelFactory <T extends Shell, U extends Shell> {
  protected final LangTestDatabaseAsync service;
  protected final ExerciseController controller;
  protected ListInterface<T> exerciseList;

  /**
   * @see mitll.langtest.client.custom.dialog.EditItem#setFactory
   * @param service
   * @param userFeedback
   * @param controller
   * @param exerciseList
   */
  public ExercisePanelFactory(final LangTestDatabaseAsync service, final UserFeedback userFeedback,
                              final ExerciseController controller,
                              ListInterface<T> exerciseList) {
    this.service = service;
    this.controller = controller;
    this.exerciseList = exerciseList;
  }

  public void setExerciseList(ListInterface<T> exerciseList) {
     this.exerciseList = exerciseList;
  }

  /**
   * @see mitll.langtest.client.list.ExerciseList#makeExercisePanel
   * @param e
   * @return
   */
  public abstract Panel getExercisePanel(U e);
}
