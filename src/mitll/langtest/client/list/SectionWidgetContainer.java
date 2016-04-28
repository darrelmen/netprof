package mitll.langtest.client.list;

import com.google.gwt.user.client.History;
import mitll.langtest.client.amas.SingleSelectExerciseList;
import mitll.langtest.client.exercise.SectionWidget;

import java.util.*;
import java.util.logging.Logger;

/**
 * Created by go22670 on 4/27/16.
 */
public class SectionWidgetContainer<T extends SectionWidget> {
  private final Logger logger = Logger.getLogger("SectionWidgetContainer");

  private final boolean DEBUG =true;
  private final Map<String, T> typeToBox = new HashMap<>();

  public T getWidget(String type) {
    return typeToBox.get(type);
  }

  public void setWidget(String type, T value) {
    typeToBox.put(type, value);
  }

  /**
   * Given a selectionState state, make sure the list boxes are consistent with it.
   *
   * @param selectionState
   * @see #onValueChange
   */
  void restoreListBoxState(SelectionState selectionState, Collection<String> typeOrder) {
    Map<String, Collection<String>> selectionState2 = getCompleteState(selectionState);

    logger.info("original state " + selectionState);
    logger.info("type order     " + typeOrder);
    logger.info("overlay state  " + selectionState2);

    boolean hasNonClearSelection = false;
    List<String> typesWithSelections = new ArrayList<>();
    if (typeOrder == null) {
      logger.warning("huh? type order is null for " + selectionState2);
      typeOrder = Collections.emptyList();
    }
    for (String type : typeOrder) {
      Collection<String> selections = selectionState2.get(type);
      if (selections.iterator().next().equals(HistoryExerciseList.ANY)) {
        if (hasNonClearSelection) {
          if (DEBUG) logger.info("restoreListBoxState : skipping type since below a selection = " + type);
        } else {
          if (DEBUG)  logger.info("restoreListBoxState : clearing " + type);
          selectItem(type, selections);
        }
      } else {
        if (!hasNonClearSelection) {
          enableAllButtonsFor(type);  // first selection row should always be fully enabled -- there's nothing above it to constrain the selections
        }
        hasNonClearSelection = true;

        if (!hasType(type)) {
          if (!type.equals("item")) {
            logger.warning("restoreListBoxState for " + selectionState + " : huh? bad type '" + type +
                "', expecting something in " + getTypes());
          }
        } else {
          typesWithSelections.add(type);
        }
      }
    }

    if (DEBUG) logger.info("restoreListBoxState :typesWithSelections " + typesWithSelections);

    // clear enabled state for all items below first selection...
    if (!typesWithSelections.isEmpty()) {
      List<String> afterFirst = new ArrayList<>();
      String first = typesWithSelections.get(0);
      boolean start = false;
      for (String type : typeOrder) {
        if (start) afterFirst.add(type);
        if (type.equals(first)) start = true;
      }

      if (DEBUG) logger.info("restoreListBoxState : afterFirst " + afterFirst);

      for (String type : afterFirst) {
        if (DEBUG) logger.info("restoreListBoxState : clearing enabled on '" + type +"'");
        clearEnabled(type);
      }
    }
    if (DEBUG) logger.info("restoreListBoxState :typesWithSelections " + typesWithSelections);

    for (String type : typesWithSelections) {
      selectItem(type, selectionState2.get(type));
    }
    String unitAndChapterSelection = getHistoryToken();

    logger.info("UI should now show " + selectionState.getTypeToSection() + " vs actual " + unitAndChapterSelection);
  }

  private Map<String, Collection<String>> getCompleteState(SelectionState selectionState) {
    Map<String, Collection<String>> selectionState2 = new HashMap<>();

    // make sure we all types have selections, even if it's the default Clear (ANY) selection
    for (String type : getTypes()) {
      selectionState2.put(type, Collections.singletonList(HistoryExerciseList.ANY));
    }
    selectionState2.putAll(selectionState.getTypeToSection());
    return selectionState2;
  }

  /**
   * @param type
   * @see HistoryExerciseList#restoreListBoxState(mitll.langtest.client.list.SelectionState)
   */
  private void clearEnabled(String type) {
    T widget = getWidget(type);
    widget.clearEnabled();
    widget.clearSelectionState();

    String currentSelection = widget.getCurrentSelection();
    logger.info("after clear of " + type + " = " +currentSelection);
    logger.info("after clear of " + type + " = " +widget.getCurrentSelections());
  }

  private void enableAllButtonsFor(String type) {  getWidget(type).enableAll();  }

  /**
   * @return
   * @see ExerciseList#pushNewItem(String, String)
   * @see HistoryExerciseList#pushNewSectionHistoryToken()
   */
  String getHistoryToken() {
    if (typeToBox.isEmpty()) {
      return History.getToken();
    }
    logger.info("getHistoryTokenFromUIState examining " +typeToBox.size() + " boxes.");
    StringBuilder unitAndChapterSelection = new StringBuilder();
    for (String type : getTypes()) {
      String section = getCurrentSelection(type);
      logger.info("getHistoryTokenFromUIState type " +type+ " section " +section);

      if (!section.equals(HistoryExerciseList.ANY)) {
        unitAndChapterSelection.append(type + "=" + section + ";");
      }
    }
    logger.info("getHistoryTokenFromUIState unitAndChapterSelection " +unitAndChapterSelection);

    return unitAndChapterSelection.toString();
  }

  /**
   * @param type
   * @return
   * @see PagingExerciseList#getHistoryTokenFromUIState(String, String)
   */
  private String getCurrentSelection(String type) {
    return typeToBox.get(type).getCurrentSelection();
  }

  private Collection<String> getTypes() {
    return typeToBox.keySet();
  }

  public Collection<T> getValues() {
    return typeToBox.values();
  }

  private boolean hasType(String type) {
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
    //typeToBox.get(type).selectItem(sections, false);
    logger.warning("doing NO OP");
  }

  public void clear() {
    typeToBox.clear();
  }

  public void clearSelections() {
    //logger.info("clearSelections");
    for (String type : typeToBox.keySet()) typeToBox.get(type).clearAll();
  }
}
