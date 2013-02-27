package mitll.langtest.server.database;

import mitll.langtest.shared.Exercise;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 10/8/12
 * Time: 3:42 PM
 * To change this template use File | Settings | File Templates.
 */
public interface ExerciseDAO {
  List<Exercise> getRawExercises();

  Map<String,Collection<String>> getTypeToSections();
  Collection<String> getSubsectionsForTypeAndSection(String type, String section);
  Collection<Exercise> getExercisesForSection(String type, String section);
}
