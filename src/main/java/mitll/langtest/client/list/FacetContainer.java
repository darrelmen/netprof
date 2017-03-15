package mitll.langtest.client.list;

import java.util.Collection;

/**
 * Created by go22670 on 3/14/17.
 */
public interface FacetContainer {
  void restoreListBoxState(SelectionState selectionState, Collection<String> typeOrder);

  String getHistoryToken();

  int getNumSelections();
}
