package mitll.langtest.server.database.userexercise;

import mitll.npdata.dao.SlickExercise;
import mitll.npdata.dao.SlickRelatedExercise;
import mitll.npdata.dao.userexercise.RelatedCoreExerciseDAOWrapper;
import mitll.npdata.dao.userexercise.RelatedExerciseDAOWrapper;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class RelatedCoreExerciseHelper implements IRelatedExercise {
  private RelatedCoreExerciseDAOWrapper daoWrapper;

  /**
   *
   * @param daoWrapper
   */
  RelatedCoreExerciseHelper(RelatedCoreExerciseDAOWrapper daoWrapper ) {
    this.daoWrapper = daoWrapper;
  }

  public void createTable() {
    daoWrapper.createTable();
  }

  public String getName() {
    return daoWrapper.getName();
  }

  @Override
  public boolean updateProject(int oldID, int newprojid) {
    return daoWrapper.updateProject(oldID, newprojid) > 0;
  }

  @Override
  public Collection<SlickExercise> getContextExercises(int exid) {
    return daoWrapper.contextExercises(exid);
  }


  @Override
  public void addBulkRelated(List<SlickRelatedExercise> relatedExercises) {
    daoWrapper.addBulk(relatedExercises);
  }

  @Override
  public int getParentForContextID(int contextID) {
    return daoWrapper.parentForContextID(contextID);
  }

  @Override
  public Collection<SlickRelatedExercise> getAllRelated(int projid) {
    return daoWrapper.allByProject(projid);
  }

  @Override
  public void deleteForProject(int projID) {    daoWrapper.deleteForProject(projID);  }

  @Override
  public int deleteRelated(int related) {
    return daoWrapper.deleteRelated(related);
  }

  @Override
  public Map<Integer, List<SlickRelatedExercise>> getDialogIDToRelated(int projid) {
    return daoWrapper.byProjectForDialog(projid);
  }
}
