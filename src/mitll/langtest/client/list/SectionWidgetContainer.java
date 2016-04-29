package mitll.langtest.client.list;

import com.google.gwt.user.client.History;
import mitll.langtest.client.amas.SingleSelectExerciseList;
import mitll.langtest.client.exercise.SectionWidget;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by go22670 on 4/27/16.
 */
public class SectionWidgetContainer<T extends SectionWidget> {
  private final Map<String, T> typeToBox = new HashMap<>();

  public T getWidget(String type) {
    return typeToBox.get(type);
  }

  public void setWidget(String type, T value) {
    typeToBox.put(type, value);
  }

  /**
   * @return
   * @see ExerciseList#pushNewItem(String, String)
   * @see HistoryExerciseList#pushNewSectionHistoryToken()
   */
  protected String getHistoryToken() {
    if (typeToBox.isEmpty()) {
      return "";// History.getToken();
    }
    //logger.info("getHistoryToken for " + id + " examining " +typeToBox.size() + " boxes.");
    StringBuilder unitAndChapterSelection = new StringBuilder();
    for (String type : getTypes()) {
      String section = getCurrentSelection(type);
      if (!section.equals(HistoryExerciseList.ANY)) {
        unitAndChapterSelection.append(type + "=" + section + ";");
      }
    }
    return unitAndChapterSelection.toString();
  }

  /**
   * @param type
   * @return
   * @see PagingExerciseList#getHistoryToken(String, String)
   */
  private String getCurrentSelection(String type) {
    return typeToBox.get(type).getCurrentSelection();
  }

  public Collection<String> getTypes() {
    return typeToBox.keySet();
  }

  public Collection<T> getValues() { return typeToBox.values(); }

  boolean hasType(String type) {
    return typeToBox.containsKey(type);
  }

  /**
   * Make sure all sections have a selection - quiz, test type, ilr level
   *
   * @return
   * @see SingleSelectExerciseList#gotEmptyExerciseList()
   * @see SingleSelectExerciseList#gotSelection()
   * @see SingleSelectExerciseList#restoreListFromHistory
   */
  protected int getNumSelections() {
    int count = 0;
    // logger.info("type now " + typeToSelection);
    for (T widget : typeToBox.values()) {
      boolean hasSelection = !widget.getCurrentSelections().isEmpty();
      //logger.info("getNumSelections : widget " + widget.getType() + " value now " + currentSelection);
      if (hasSelection) {
        count++;
      }
    }
    return count;
  }

  /**
   * @param type
   * @param sections
   * @see #restoreListBoxState(SelectionState)
   */
  protected void selectItem(String type, Collection<String> sections) {
    typeToBox.get(type).selectItem(sections, false);
  }

  public void clear() {
    typeToBox.clear();
  }

  public void clearSelections() {
    //logger.info("clearSelections");
    for (String type : typeToBox.keySet()) typeToBox.get(type).clearAll();
  }
}
