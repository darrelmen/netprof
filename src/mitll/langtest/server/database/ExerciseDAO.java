package mitll.langtest.server.database;

import mitll.langtest.server.database.custom.AddRemoveDAO;
import mitll.langtest.server.database.custom.UserExerciseDAO;
import mitll.langtest.shared.CommonExercise;
import mitll.langtest.shared.CommonUserExercise;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 10/8/12
 * Time: 3:42 PM
 * To change this template use File | Settings | File Templates.
 */
public interface ExerciseDAO {
  List<CommonExercise> getRawExercises();
  SectionHelper getSectionHelper();

  void addOverlay(CommonUserExercise userExercise);
  void add(CommonUserExercise userExercise);

  boolean remove(String id);

  void setUserExerciseDAO(UserExerciseDAO userExerciseDAO);

  void setAddRemoveDAO(AddRemoveDAO addRemoveDAO);

  CommonExercise getExercise(String id);

 // List<String> getErrors();
}
