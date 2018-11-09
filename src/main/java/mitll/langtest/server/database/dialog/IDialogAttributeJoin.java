package mitll.langtest.server.database.dialog;

import mitll.langtest.server.database.IDAO;
import mitll.npdata.dao.SlickDialogAttributeJoin;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface IDialogAttributeJoin extends IDAO {
  Map<Integer, Collection<SlickDialogAttributeJoin>> getAllJoinByProject(int projid);

  void addBulkAttributeJoins(List<SlickDialogAttributeJoin> joins);
  //void removeBulkAttributeJoins(List<SlickDialogAttributeJoin> joins);
}
