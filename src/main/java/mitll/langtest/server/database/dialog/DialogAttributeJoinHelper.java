package mitll.langtest.server.database.dialog;

import mitll.langtest.server.domino.ProjectSync;
import mitll.npdata.dao.SlickDialogAttributeJoin;
import mitll.npdata.dao.dialog.DialogAttributeJoinDAOWrapper;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class DialogAttributeJoinHelper implements IDialogAttributeJoin {
  private final DialogAttributeJoinDAOWrapper attributeJoinDAOWrapper;

  DialogAttributeJoinHelper(DialogAttributeJoinDAOWrapper attributeJoinDAOWrapper) {
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
  public Map<Integer, Collection<SlickDialogAttributeJoin>> getAllJoinByProject(int projid) {
    return attributeJoinDAOWrapper.allByProject(projid);
  }


  @Override
  public void addBulkAttributeJoins(List<SlickDialogAttributeJoin> joins) {
    attributeJoinDAOWrapper.addBulk(joins);
  }

  /**
   * @param joins
   * @see ProjectSync#doUpdate
   */
/*  @Override
  public void removeBulkAttributeJoins(List<SlickDialogAttributeJoin> joins) {
    attributeJoinDAOWrapper.removeBulk(joins);
  }*/
}
