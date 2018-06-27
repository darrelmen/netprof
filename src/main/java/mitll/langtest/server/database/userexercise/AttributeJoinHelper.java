package mitll.langtest.server.database.userexercise;

import mitll.langtest.server.domino.ProjectSync;
import mitll.npdata.dao.SlickExerciseAttributeJoin;
import mitll.npdata.dao.userexercise.ExerciseAttributeJoinDAOWrapper;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class AttributeJoinHelper implements IAttributeJoin {
  private final ExerciseAttributeJoinDAOWrapper attributeJoinDAOWrapper;

  AttributeJoinHelper(ExerciseAttributeJoinDAOWrapper attributeJoinDAOWrapper) {
    this.attributeJoinDAOWrapper = attributeJoinDAOWrapper;
  }

  public void createTable() {
    attributeJoinDAOWrapper.createTable();
  }

  public String getName() {
    return attributeJoinDAOWrapper.getName();
  }

  /**
   * Exercise attribute join table is independent of project - makes no reference to project - nothing to update
   * @param oldID
   * @param newprojid
   * @return
   */
  @Override
  public boolean updateProject(int oldID, int newprojid) {
    return true;
  }

  @Override
  public Map<Integer, Collection<SlickExerciseAttributeJoin>> getAllJoinByProject(int projid) {
    return attributeJoinDAOWrapper.allByProject(projid);
  }


  @Override
  public void addBulkAttributeJoins(List<SlickExerciseAttributeJoin> joins) {
    attributeJoinDAOWrapper.addBulk(joins);
  }

  /**
   * @param joins
   * @see ProjectSync#doUpdate
   */
  @Override
  public void removeBulkAttributeJoins(List<SlickExerciseAttributeJoin> joins) {
    attributeJoinDAOWrapper.removeBulk(joins);
  }
}
