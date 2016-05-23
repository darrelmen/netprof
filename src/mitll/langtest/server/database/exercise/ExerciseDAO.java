/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

package mitll.langtest.server.database.exercise;

import mitll.langtest.server.database.AudioDAO;
import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.server.database.custom.AddRemoveDAO;
import mitll.langtest.server.database.custom.UserExerciseDAO;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.CommonShell;

import java.util.Collection;

/**
 * Container for exercises for the site.
 * Mainly we want to be able to add and remove exercies, and add overlays of user exercises
 * Also attaches audio (join) to exercises.
 *
 * User: GO22670
 * Date: 10/8/12
 * Time: 3:42 PM
 * To change this template use File | Settings | File Templates.
 */
public interface ExerciseDAO<T extends CommonShell> extends SimpleExerciseDAO<T> {
  /**
   * @param userExercise
   * @return
   * @see mitll.langtest.server.database.DatabaseImpl#editItem
   */
  CommonExercise addOverlay(CommonExercise userExercise);

  /**
   * @param userExercise
   * @see mitll.langtest.server.database.DatabaseImpl#duplicateExercise
   */
  void add(CommonExercise userExercise);

  /**
   * @param id
   * @return
   * @see mitll.langtest.server.database.DatabaseImpl#deleteItem(String)
   */
  boolean remove(String id);

  /**
   * @param userExerciseDAO
   * @see mitll.langtest.server.database.DatabaseImpl#makeDAO(String, String, String)
   */
  void setDependencies(String mediaDir, String installPath, UserExerciseDAO userExerciseDAO, AddRemoveDAO addRemoveDAO, AudioDAO audioDAO);

  /**
   * @param all
   * @see DatabaseImpl#getExerciseIDToRefAudio()
   */
  void attachAudio(Collection<CommonExercise> all);
}
