package mitll.langtest.server.database;

import mitll.langtest.shared.Exercise;

import java.util.List;
import java.util.Map;
import java.util.Set;

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

  Exercise getExercise(String id);

  List<String> getErrors();
}
