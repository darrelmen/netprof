package mitll.langtest.client.list;

import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.SectionWidget;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.shared.CommonShell;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 11/6/13
 * Time: 3:21 PM
 * To change this template use File | Settings | File Templates.
 */
public class HistoryExerciseList extends PagingExerciseList {
  public static final String ANY = "Clear";
  private static final boolean debugOnValueChange = false;
  private static final boolean DEBUG = false;

  protected final Map<String,SectionWidget> typeToBox = new HashMap<String, SectionWidget>();
  /**
   * So the concern is that if we allow people to send bookmarks with items, we can allow them to skip
   * forward in a list we're trying to present in a certain order.
   * I.e. when a new user logs in, we want them to do all of their items in the order we've chosen
   * for them (least answered/recorded first), and not let them skip forward in the list.
   */
 // private static final boolean INCLUDE_ITEM_IN_BOOKMARK = false;
  protected long userID;
  protected HistoryExerciseList(Panel currentExerciseVPanel, LangTestDatabaseAsync service, UserFeedback feedback,
                                ExerciseController controller,
                                boolean showTypeAhead, String instance) {
    super(currentExerciseVPanel, service, feedback, null, controller, showTypeAhead, instance);
  }

  /**
   * @see #pushNewItem(String)
   * @see #pushNewSectionHistoryToken()
   * @param id
   * @return
   */
  protected String getHistoryToken(String id) {
    if (typeToBox.isEmpty()) {
      return History.getToken();
    }
    //System.out.println("getHistoryToken for " + id + " examining " +typeToBox.size() + " boxes.");
    StringBuilder builder = new StringBuilder();
    for (String type : typeToBox.keySet()) {
      String section = getCurrentSelection(type);
      //System.out.println("\tHistoryExerciseList.getHistoryToken for " + type + " section = " +section);
      if (!section.equals(HistoryExerciseList.ANY)) {
        builder.append(type + "=" + section + ";");
      }
    }

/*    if (id != null && INCLUDE_ITEM_IN_BOOKMARK) {
      String historyToken = super.getHistoryToken(id);
      //System.out.println("getHistoryToken for " + id + " would add " +historyToken);

      builder.append(historyToken);
    }*/
    //System.out.println("\tgetHistoryToken for " + id + " is '" +builder.toString() + "'");
    String instanceSuffix = ";" + SelectionState.INSTANCE + "=" + getInstance();
    boolean hasItemID = id != null && id.length() > 0;
    return (hasItemID ? super.getHistoryToken(id) + ";" : "") + builder + instanceSuffix;
  }

  /**
   * @see #getHistoryToken(String)
   * @param type
   * @return
   */
  private String getCurrentSelection(String type) {
    SectionWidget listBox = typeToBox.get(type);
    return listBox.getCurrentSelection();
  }

  protected void setHistoryItem(String historyToken) {
    //System.out.println("------------ HistoryExerciseList.setHistoryItem '" + historyToken + "' -------------- ");
    History.newItem(historyToken);
  }

  /**
   * @see ListInterface#loadExercise(String)
   * @see #pushFirstSelection(String)
   * @param exerciseID
   */
  protected void pushNewItem(String exerciseID) {
    String historyToken = getHistoryToken(exerciseID);
    String trimmedToken = historyToken.length() > 2? historyToken.substring(0,historyToken.length()-2) : historyToken;
    if (DEBUG) System.out.println(new Date() + "------------ HistoryExerciseList.pushNewItem : push history '" + historyToken + "' -------------- ");

    String token = History.getToken();
    //System.out.println("\tpushNewItem : current token '" + token + "' vs new id '" + exerciseID +"'");

    token = getSelectionFromToken(token);
    if (DEBUG) System.out.println("\tHistoryExerciseList.pushNewItem : current token '" + token + "' vs new id '" + exerciseID +"'");
    if (token != null && (historyToken.equals(token) || trimmedToken.equals(token))) {
      System.out.println("\tHistoryExerciseList.pushNewItem : current token '" + token + "' same as new " + historyToken);
      checkAndAskServer(exerciseID);
    } else {
      System.out.println("\tHistoryExerciseList.pushNewItem : current token '" + token + "' different menu state '" +historyToken+ "' from new " + exerciseID);
      setHistoryItem(historyToken);
    }
  }

  /**
   * So if we have an existing history token, use it to set current selection.
   * If not, push the current state of the list boxes and act on it
   * @see ListInterface#getExercises(long)
   */
  protected void pushFirstListBoxSelection() {
    String initToken = History.getToken();
    if (initToken.length() == 0) {
      System.out.println("pushFirstListBoxSelection : history token is blank " + getInstance());

      pushNewSectionHistoryToken();
    } else {
      System.out.println("pushFirstListBoxSelection fire history for token from URL: " +initToken + " instance " + getInstance());
      History.fireCurrentHistoryState();
    }
  }

  /**
   * @see #pushFirstListBoxSelection
   * @see mitll.langtest.client.bootstrap.FlexSectionExerciseList#addClickHandlerToButton
   */
  protected void pushNewSectionHistoryToken() {
    String historyToken = getHistoryToken(null);
    String currentToken = History.getToken();

    if (currentToken.equals(historyToken)) {
      if (isEmpty() || historyToken.isEmpty()) {
        System.out.println("pushNewSectionHistoryToken : noSectionsGetExercises for token '" + historyToken +
          "' " + "current has " + getSize() + " instance " + getInstance());

        noSectionsGetExercises(userID);
      } else {
        System.out.println("pushNewSectionHistoryToken : skipping same token '" + historyToken + "'" + " instance " + getInstance());
      }
    } else {
      System.out.println("pushNewSectionHistoryToken : currentToken " + currentToken + " instance " + getInstance());

      setHistoryItem(historyToken);
    }
  }

  /**
   * @see #restoreListBoxState(SelectionState)
   * @param type
   * @param sections
   */
  protected void selectItem(String type, Collection<String> sections) {
    SectionWidget listBox = typeToBox.get(type);
    listBox.selectItem(sections, false);
  }

  /**
   * @see mitll.langtest.client.list.PagingExerciseList#makePagingContainer()
   * @param e
   */
  @Override
  protected void gotClickOnItem(CommonShell e) {
    //System.out.println("----------- got click on " + e.getID() + " -------------- ");
    //if (!INCLUDE_ITEM_IN_BOOKMARK) {
      loadByID(e.getID());
   // }
  }

  /**
   * Given a selectionState state, make sure the list boxes are consistent with it.
   * @see #onValueChange(com.google.gwt.event.logical.shared.ValueChangeEvent)
   * @param selectionState
   */
  protected void restoreListBoxState(SelectionState selectionState) {
    Map<String, Collection<String>> selectionState2 = new HashMap<String, Collection<String>>();

    // make sure we all types have selections, even if it's the default Clear (ANY) selection
    for (String type : typeToBox.keySet()) {
      selectionState2.put(type, Collections.singletonList(HistoryExerciseList.ANY));
    }

    for (Map.Entry<String, Collection<String>> pair : selectionState.getTypeToSection().entrySet()) {
      String type = pair.getKey();
      Collection<String> section = pair.getValue();
      selectionState2.put(type,section);
    }

    boolean hasNonClearSelection = false;
    List<String> typesWithSelections = new ArrayList<String>();
    Collection<String> typeOrder = getTypeOrder(selectionState2);
    if (typeOrder == null) {
      System.err.println("huh? type order is null for " + selectionState2);
      typeOrder = Collections.emptyList();
    }
    for (String type : typeOrder) {
      Collection<String> selections = selectionState2.get(type);
      if (selections.iterator().next().equals(HistoryExerciseList.ANY)) {
        if (hasNonClearSelection) {
          //System.out.println("restoreListBoxState : skipping type since below a selection = " + type);
        }
        else {
          //System.out.println("restoreListBoxState : clearing " + type);

          selectItem(type, selections);
        }
      }
      else {
        if (!hasNonClearSelection) {
          enableAllButtonsFor(type);  // first selection row should always be fully enabled -- there's nothing above it to constrain the selections
        }
        hasNonClearSelection = true;

        if (!typeToBox.containsKey(type)) {
          if (!type.equals("item")) {
            System.err.println("restoreListBoxState for " + selectionState + " : huh? bad type '" + type +
              "', expecting something in " + typeToBox.keySet());
          }
        } else {
          typesWithSelections.add(type);
        }
      }
    }

    //System.out.println("restoreListBoxState :typesWithSelections " + typesWithSelections);

    // clear enabled state for all items below first selection...
    if (!typesWithSelections.isEmpty()) {
      List<String> afterFirst = new ArrayList<String>();
      String first = typesWithSelections.get(0);
      boolean start = false;
      for (String type : typeOrder) {
         if (start) afterFirst.add(type);
         if (type.equals(first)) start = true;
      }

      //System.out.println("restoreListBoxState : afterFirst " + afterFirst);

      for (String type : afterFirst) {
        //System.out.println("restoreListBoxState : clearing enabled on " + type);

        clearEnabled(type);
      }
    }

    for (String type : typesWithSelections) {
      selectItem(type, selectionState2.get(type));
    }
  }

  protected void clearEnabled(String type) {}
  protected void enableAllButtonsFor(String type) {}
  protected Collection<String> getTypeOrder(Map<String, Collection<String>> selectionState2) {
    return selectionState2.keySet();
  }

  /**
     * Respond to push a history token.
     * @param event
     */
  @Override
  public void onValueChange(ValueChangeEvent<String> event) {
    //if (debugOnValueChange && false) System.out.println(new Date() +" HistoryExerciseList.onValueChange : ------ start ---- " + getInstance());

    String originalValue = event.getValue();
    String rawToken = getTokenFromEvent(event);
    SelectionState selectionState1 = getSelectionState(rawToken);

    String instance1 = selectionState1.getInstance();

    if (!instance1.equals(getInstance()) && instance1.length() > 0) {
      if (debugOnValueChange)  System.out.println("onValueChange : skipping event " + rawToken + " for instance '" + instance1 +
          "' that is not mine "+ getInstance());
      if (getCreatedPanel() == null) {
        noSectionsGetExercises(controller.getUser());
      }
      return;
    }
    if (debugOnValueChange) {
      System.out.println(new Date() + " HistoryExerciseList.onValueChange : originalValue '" +originalValue+
        "'" +
        " token is '" + rawToken + "' for " + instance1 + " vs my instance " + getInstance());
    }

    String item = selectionState1.getItem();

    if (item != null && item.length() > 0 && hasExercise(item)) {
    //  if (INCLUDE_ITEM_IN_BOOKMARK) {
        checkAndAskServer(item);
  /*    }
      else {
        System.out.println("onValueChange : skipping item " + item);
      }*/
    } else {
      String token = event.getValue();
      try {
        SelectionState selectionState = getSelectionState(token);
        restoreListBoxState(selectionState);
        if (debugOnValueChange) {
          System.out.println(new Date() + " HistoryExerciseList.onValueChange : selectionState '" + selectionState + "'");
        }

        loadExercises(selectionState.getTypeToSection(), selectionState.getItem());
      } catch (Exception e) {
        System.err.println("HistoryExerciseList.onValueChange " + token + " badly formed. Got " + e);
        e.printStackTrace();
      }
    }
  }

  /**
   * When we get a history token push, select the exercise type, section, and optionally item.
   * @param typeToSection
   * @param item null is OK
   * @see #onValueChange(com.google.gwt.event.logical.shared.ValueChangeEvent)
   */
  protected void loadExercises(final Map<String, Collection<String>> typeToSection, final String item) {
    //System.out.println("HistoryExerciseList.loadExercises : instance " + getInstance() + " " + typeToSection + " and item '" + item + "'");
    loadExercisesUsingPrefix(typeToSection, getPrefix());
  }

  protected void loadExercises(String selectionState, String prefix) {
    Map<String, Collection<String>> typeToSection = getSelectionState(selectionState).getTypeToSection();
/*    System.out.println("HistoryExerciseList.loadExercises : looking for " +
      "'" + prefix + "' (" + prefix.length() + " chars) in list id "+userListID + " instance " + getInstance());*/
    loadExercisesUsingPrefix(typeToSection, prefix);
  }

  protected void loadExercisesUsingPrefix(Map<String, Collection<String>> typeToSection, String prefix) {
    lastReqID++;
    if (DEBUG || true) {
      System.out.println("HistoryExerciseList.loadExercisesUsingPrefix looking for '" + prefix +
        "' (" + prefix.length() + " chars) in context of " + typeToSection + " list " + userListID +
        " instance " + getInstance() + " user " + controller.getUser() + " unrecorded " + getUnrecorded() + " only examples " +isOnlyExamples());
    }
    String selectionID = userListID + "_"+typeToSection.toString();
    scheduleWaitTimer();
    service.getExerciseIds(lastReqID, typeToSection, prefix, userListID, controller.getUser(), getRole(), getUnrecorded(), isOnlyExamples(), new SetExercisesCallback(selectionID));
  }

  /**
   * @see PagingExerciseList#loadExercises(String, String)
   * @see #onValueChange(com.google.gwt.event.logical.shared.ValueChangeEvent)
   * @param token
   * @return object representing type=value pairs from history token
   */
  protected SelectionState getSelectionState(String token) {
    return new SelectionState(token, !allowPlusInURL);
  }

  /**
   * @see #loadExercises(java.util.Map, String)
   * @see #pushNewSectionHistoryToken()
   * @param userID
   */
  protected void noSectionsGetExercises(long userID) {  super.getExercises(userID);  }
}
