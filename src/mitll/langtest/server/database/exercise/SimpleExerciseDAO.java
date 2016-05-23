package mitll.langtest.server.database.exercise;

import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.server.database.user.UserManagement;
import mitll.langtest.shared.exercise.CommonShell;

import java.util.List;

/**
 * Created by go22670 on 2/22/16.
 */
public interface SimpleExerciseDAO<T extends CommonShell> {
  /**
   * @return
   * @see DatabaseImpl#getExercise(String)
   * @see DatabaseImpl#makeDAO(String, String, String)
   * @see UserManagement#getUsers()
   */
  List<T> getRawExercises();

  /**
   * @param id
   * @return
   * @see DatabaseImpl#getExercise(String)
   */
  T getExercise(String id);

  int getNumExercises();

  SectionHelper<T> getSectionHelper();

  void reload();
}
