/*
 *
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2015. Other request for this document shall be referred
 * to DLIFLC.
 *
 * WARNING: This document may contain technical data whose export is restricted
 * by the Arms Export Control Act (AECA) or the Export Administration Act (EAA).
 * Transfer of this data by any means to a non-US person who is not eligible to
 * obtain export-controlled data is prohibited. By accepting this data, the consignee
 * agrees to honor the requirements of the AECA and EAA. DESTRUCTION NOTICE: For
 * unclassified, limited distribution documents, destroy by any method that will
 * prevent disclosure of the contents or reconstruction of the document.
 *
 * This material is based upon work supported under Air Force Contract No.
 * FA8721-05-C-0002 and/or FA8702-15-D-0001. Any opinions, findings, conclusions
 * or recommendations expressed in this material are those of the author(s) and
 * do not necessarily reflect the views of the U.S. Air Force.
 *
 * © 2015 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 *
 *
 */

package mitll.langtest.client.list;

import mitll.langtest.client.amas.SingleSelectExerciseList;
import mitll.langtest.client.exercise.SectionWidget;

import java.util.*;
import java.util.logging.Logger;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 4/27/16.
 */
public class SectionWidgetContainer<T extends SectionWidget> implements FacetContainer {
  private final Logger logger = Logger.getLogger("SectionWidgetContainer");

  private static final boolean DEBUG = false;
  static final String ANY = "Clear";

  private final Map<String, T> typeToBox = new HashMap<>();

  public T getWidget(String type) {
    return typeToBox.get(type);
  }

  /**
   * @param type
   * @param value
   * @seex mitll.langtest.client.bootstrap.FlexSectionExerciseList#populateButtonGroups(Collection)
   */
  public void setWidget(String type, T value) {
    typeToBox.put(type, value);
  }

  /**
   * Given a selectionState state, make sure the list boxes are consistent with it.
   *
   * @param selectionState
   * @see HistoryExerciseList#restoreListBoxState(SelectionState)
   */
  @Override
  public void restoreListBoxState(SelectionState selectionState, Collection<String> typeOrder) {
    Map<String, Collection<String>> selectionState2 = getCompleteState(selectionState);

//    logger.info("restoreListBoxState original state " + selectionState);
//    logger.info("type order     " + typeOrder);
//    logger.info("overlay state  " + selectionState2);

    boolean hasNonClearSelection = false;
    List<String> typesWithSelections = new ArrayList<>();
    if (typeOrder == null) {
      if (DEBUG) logger.warning("restoreListBoxState huh? type order is null for " + selectionState2);
      typeOrder = Collections.emptyList();
    }
    for (String type : typeOrder) {
      Collection<String> selections = selectionState2.get(type);
      if (selections == null) {
        if (DEBUG)  logger.warning("restoreListBoxState huh? no selection in selection state " + selectionState2.keySet() + " for " + type);
      }
      if (selections != null && selections.iterator().next().equals(getAnySelectionValue())) {
        if (hasNonClearSelection) {
          if (DEBUG) logger.info("restoreListBoxState : skipping type since below a selection = " + type);
        } else {
          if (DEBUG) logger.info("restoreListBoxState : clearing " + type);
          selectItem(type, selections);
        }
      } else {
        if (!hasNonClearSelection) {
          enableAllButtonsFor(type);  // first selection row should always be fully enabled -- there's nothing above it to constrain the selections
        }
        hasNonClearSelection = true;

        if (!hasType(type)) {
          if (hasTypes() && !type.equals("item")) {
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
        if (DEBUG) logger.info("restoreListBoxState : clearing enabled on '" + type + "'");
        clearEnabled(type);
      }
    }
    if (DEBUG) logger.info("restoreListBoxState :typesWithSelections " + typesWithSelections);

    for (String type : typesWithSelections) {
      selectItem(type, selectionState2.get(type));
    }
    String unitAndChapterSelection = getHistoryToken();

    if (DEBUG)
      logger.info("UI should now show " + selectionState.getTypeToSection() + " vs actual " + unitAndChapterSelection);
  }

  private Map<String, Collection<String>> getCompleteState(SelectionState selectionState) {
    Map<String, Collection<String>> selectionState2 = new HashMap<>();

    // make sure we all types have selections, even if it's the default Clear (ANY) selection
    for (String type : getTypes()) {
      selectionState2.put(type, Collections.singletonList(getAnySelectionValue()));
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

    //   String currentSelection = widget.getCurrentSelection();
    //   logger.info("after clear of " + type + " = " + currentSelection);
    //  logger.info("after clear of " + type + " = " + widget.getCurrentSelections());
  }

  private void enableAllButtonsFor(String type) {
    T widget = getWidget(type);
    if (widget != null) {
      widget.enableAll();
    }
  }

  /**
   * @return
   * @see ExerciseList#pushNewItem(String, int)
   * @see HistoryExerciseList#pushNewSectionHistoryToken()
   */
  @Override
  public String getHistoryToken() {
    if (typeToBox.isEmpty()) {
      return "";
    } else {
      //  logger.info("getHistoryTokenFromUIState examining " + typeToBox.size() + " boxes.");
      StringBuilder unitAndChapterSelection = new StringBuilder();
      for (String type : getTypes()) {
        String section = getCurrentSelection(type);
        //   logger.info("getHistoryTokenFromUIState type " + type + " section " + section);
        if (!section.equals(getAnySelectionValue())) {
          unitAndChapterSelection.append(type + "=" + section + SelectionState.SECTION_SEPARATOR);
        }
      }
      //  logger.info("getHistoryTokenFromUIState unitAndChapterSelection " + unitAndChapterSelection);

      return unitAndChapterSelection.toString();
    }
  }

  protected String getAnySelectionValue() {
    return ANY;
  }

  /**
   * @param type
   * @return
   * @see PagingExerciseList#getHistoryTokenFromUIState(String, int)
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

  private boolean hasTypes() {
    return !typeToBox.isEmpty();
  }

  /**
   * Make sure all sections have a selection - quiz, test type, ilr level
   *
   * @return
   * @see SingleSelectExerciseList#gotEmptyExerciseList()
   * @see SingleSelectExerciseList#gotSelection()
   * @see SingleSelectExerciseList#restoreListFromHistory
   */
  @Override
  public int getNumSelections() {
    int count = 0;
    // logger.info("type now " + typeToSelection);
    for (T widget : typeToBox.values()) {
      boolean hasSelection = !widget.getCurrentSelections().isEmpty();
      //logger.info("getNumSelections : widget " + widget.getProperty() + " value now " + currentSelection);
      if (hasSelection) {
        count++;
      }
    }
    return count;
  }

  /**
   * @param type
   * @param sections
   * @see #restoreListBoxState
   */
  protected void selectItem(String type, Collection<String> sections) {}
//  {
//    logger.warning("doing NO OP");
//  }

  public void clear() {
    typeToBox.clear();
  }

  public void clearSelections() {
    Set<String> types = typeToBox.keySet();
    for (String type : types) typeToBox.get(type).clearAll();
  }
}
