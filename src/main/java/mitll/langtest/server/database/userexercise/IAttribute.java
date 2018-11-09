package mitll.langtest.server.database.userexercise;

import mitll.langtest.server.database.IDAO;
import mitll.langtest.shared.exercise.ExerciseAttribute;

import java.util.Collection;
import java.util.Map;

public interface IAttribute extends IDAO {
  int addAttribute(int projid, long now, int userid, ExerciseAttribute attribute, boolean checkExists);

  Collection<String> getAttributeTypes(int projid);

  /**
   *
   * @param projid
   * @return
   */
  Map<Integer, ExerciseAttribute> getIDToPair(int projid);
}
