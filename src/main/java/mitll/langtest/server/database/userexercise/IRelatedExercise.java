package mitll.langtest.server.database.userexercise;

import mitll.langtest.server.database.IDAO;
import mitll.npdata.dao.SlickExercise;
import mitll.npdata.dao.SlickRelatedExercise;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface IRelatedExercise  extends IDAO {
  Collection<SlickExercise> getContextExercises(int id);

  void addBulkRelated(List<SlickRelatedExercise> relatedExercises);

  int getParentForContextID(int contextID);

  Collection<SlickRelatedExercise> getAllRelated(int projid);

  void deleteForProject(int projID);

  int deleteRelated(int related);

  Map<Integer, List<SlickRelatedExercise>> getDialogIDToRelated(int projid);
}
