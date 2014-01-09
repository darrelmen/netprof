package mitll.langtest.server.database;

import mitll.langtest.server.database.custom.UserExerciseDAO;
import mitll.langtest.shared.Exercise;
import mitll.langtest.shared.custom.UserExercise;

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
  List<Exercise> getRawExercises();
  SectionHelper getSectionHelper();

  void addOverlay(UserExercise userExercise);

  void setUserExerciseDAO(UserExerciseDAO userExerciseDAO);
  Exercise getExercise(String id);
}
