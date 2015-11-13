package mitll.langtest.server.database.exercise;

import mitll.langtest.server.database.AudioDAO;
import mitll.langtest.server.database.custom.AddRemoveDAO;
import mitll.langtest.server.database.custom.UserExerciseDAO;
import mitll.langtest.shared.CommonExercise;
import mitll.langtest.shared.CommonUserExercise;

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
  List<CommonExercise> getRawExercises();
  SectionHelper getSectionHelper();

  CommonExercise addOverlay(CommonUserExercise userExercise);
  void add(CommonUserExercise userExercise);

  boolean remove(String id);

  void setUserExerciseDAO(UserExerciseDAO userExerciseDAO);

  void setAddRemoveDAO(AddRemoveDAO addRemoveDAO);

  CommonExercise getExercise(String id);

  void setAudioDAO(AudioDAO audioDAO);

  void attachAudio(Collection<CommonUserExercise> all);
}
