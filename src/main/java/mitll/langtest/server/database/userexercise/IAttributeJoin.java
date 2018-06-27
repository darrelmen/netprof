package mitll.langtest.server.database.userexercise;

import mitll.langtest.server.database.IDAO;
import mitll.npdata.dao.SlickExerciseAttributeJoin;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface IAttributeJoin  extends IDAO {
  Map<Integer, Collection<SlickExerciseAttributeJoin>> getAllJoinByProject(int projid);

  void addBulkAttributeJoins(List<SlickExerciseAttributeJoin> joins);

  void removeBulkAttributeJoins(List<SlickExerciseAttributeJoin> joins);
}
