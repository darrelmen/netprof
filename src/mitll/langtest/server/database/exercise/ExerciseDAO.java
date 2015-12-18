/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

package mitll.langtest.server.database.exercise;

import mitll.langtest.server.database.AudioDAO;
import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.server.database.UserManagement;
import mitll.langtest.server.database.custom.AddRemoveDAO;
import mitll.langtest.server.database.custom.UserExerciseDAO;
import mitll.langtest.shared.custom.UserExercise;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.CommonUserExercise;

import java.util.Collection;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 10/8/12
 * Time: 3:42 PM
 * To change this template use File | Settings | File Templates.
 */
public interface ExerciseDAO {
  /**
   * @see DatabaseImpl#getExercise(String)
   * @see DatabaseImpl#makeDAO(String, String, String)
   * @see UserManagement#getUsers()
   * @return
   */
  List<CommonExercise> getRawExercises();

  /**
   * @see DatabaseImpl#getSectionHelper()
   * @see UserExerciseDAO#add(UserExercise, boolean)
   * @see UserExerciseDAO#getUserExercises(String)
   * @see ExcelImport#removeExercises()
   * @return
   */
  SectionHelper getSectionHelper();

  /**
   * @see mitll.langtest.server.database.DatabaseImpl#editItem(UserExercise)
   * @param userExercise
   * @return
   */
  CommonExercise addOverlay(CommonUserExercise userExercise);

  /**
   * @see mitll.langtest.server.database.DatabaseImpl#duplicateExercise(UserExercise)
   * @param userExercise
   */
  void add(CommonUserExercise userExercise);

  /**
   * @see mitll.langtest.server.database.DatabaseImpl#deleteItem(String)
   * @param id
   * @return
   */
  boolean remove(String id);

  /**
   * @see mitll.langtest.server.database.DatabaseImpl#makeDAO(String, String, String)
    * @param userExerciseDAO
   */
  void setUserExerciseDAO(UserExerciseDAO userExerciseDAO);

  /**
   * @see mitll.langtest.server.database.DatabaseImpl#makeDAO(String, String, String)
   * @param addRemoveDAO
   */
  void setAddRemoveDAO(AddRemoveDAO addRemoveDAO);

  /**
   * @see mitll.langtest.server.database.DatabaseImpl#getExercise(String)
   * @param id
   * @return
   */
  CommonExercise getExercise(String id);

  /**
   * @see mitll.langtest.server.database.DatabaseImpl#makeDAO(String, String, String)
   * @param audioDAO
   * @param mediaDir
   * @param installPath
   */
  void setAudioDAO(AudioDAO audioDAO, String mediaDir, String installPath);

  /**
   * @see DatabaseImpl#getExerciseIDToRefAudio()
   * @param all
   */
  void attachAudio(Collection<CommonUserExercise> all);
}
