package mitll.langtest.client.list;

import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.SectionWidget;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.shared.ExerciseListWrapper;
import mitll.langtest.shared.ExerciseShell;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 11/6/13
 * Time: 3:21 PM
 * To change this template use File | Settings | File Templates.
 */
public class HistoryExerciseList extends PagingExerciseList {
  public static final String ANY = "Clear";
  protected final Map<String,SectionWidget> typeToBox = new HashMap<String, SectionWidget>();
  /**
   * So the concern is that if we allow people to send bookmarks with items, we can allow them to skip
   * forward in a list we're trying to present in a certain order.
   * I.e. when a new user logs in, we want them to do all of their items in the order we've chosen
   * for them (least answered/recorded first), and not let them skip forward in the list.
   */
  private final boolean includeItemInBookmark = false;
  protected long userID;

  public HistoryExerciseList(Panel currentExerciseVPanel, LangTestDatabaseAsync service, UserFeedback feedback,
                             boolean showTurkToken, boolean showInOrder, ExerciseController controller,
                             boolean showTypeAhead, String instance) {
    super(currentExerciseVPanel, service, feedback, showTurkToken, showInOrder, controller, showTypeAhead, instance);
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

  protected String getFirstItem(String type) {
    SectionWidget listBox = typeToBox.get(type);
    return listBox.getFirstItem();  // first is Any
  }

  /**
   * @see mitll.langtest.client.list.HistoryExerciseList#loadExercises(String, String)
   * @see #onValueChange(com.google.gwt.event.logical.shared.ValueChangeEvent)
   * @param token
   * @return object representing type=value pairs from history token
   */
  protected SelectionState getSelectionState(String token) {
    return new SelectionState(token, !allowPlusInURL);
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
      System.out.println("\tHistoryExerciseList.getHistoryToken for " + type + " section = " +section);
      if (section.equals(HistoryExerciseList.ANY)) {
        //System.out.println("getHistoryToken : Skipping box " + type + " (ANY) ");
      } else {
        builder.append(type + "=" + section + ";");
      }
    }
/*    if (id != null && includeItemInBookmark) {
      String historyToken = super.getHistoryToken(id);
      //System.out.println("getHistoryToken for " + id + " would add " +historyToken);

      builder.append(historyToken);
    }*/
    System.out.println("\tgetHistoryToken for " + id + " is '" +builder.toString() + "'");
    if (id != null && id.length() > 0 && builder.toString().isEmpty()) return super.getHistoryToken(id);
    return builder.toString();
  }

  protected void setHistoryItem(String historyToken) {
    System.out.println("------------ HistoryExerciseList.setHistoryItem '" + historyToken + "' -------------- ");
    History.newItem(historyToken);
  }

  /**
   * @see #loadExercise(mitll.langtest.shared.ExerciseShell)
   * @see #pushFirstSelection(String)
   * @param exerciseID
   */
  protected void pushNewItem(String exerciseID) {
    String historyToken = getHistoryToken(exerciseID);
    String trimmedToken = historyToken.length() > 2? historyToken.substring(0,historyToken.length()-2) : historyToken;
    System.out.println(new Date() + "------------ HistoryExerciseList.pushNewItem : push history '" + historyToken + "' -------------- ");

    String token = History.getToken();
    token = getSelectionFromToken(token);
    getSelectionState(token);
    //System.out.println("pushNewItem : current token '" + token + "' vs new id '" + exerciseID +"'");
    if (token != null && (historyToken.equals(token) || trimmedToken.equals(token))) {
      System.out.println("\tpushNewItem : current token '" + token + "' same as new " + historyToken);
      loadByIDFromToken(exerciseID);
    } else {
      System.out.println("\tpushNewItem : current token '" + token + "' different menu state '" +historyToken+ "' from new " + exerciseID);
      setHistoryItem(historyToken);
    }
  }

  /**
   * @see #loadExercises(java.util.Map, String)
   * @see #pushNewSectionHistoryToken()
   * @param userID
   */
  protected void noSectionsGetExercises(long userID) {
    System.out.println("HistoryExerciseList.noSectionsGetExercises for " + userID);
    super.getExercises(userID, true);
  }

  /**
   * So if we have an existing history token, use it to set current selection.
   * If not, push the current state of the list boxes and act on it
   * @see mitll.langtest.client.list.ListInterface#getExercises(long, boolean)
   */
  protected void pushFirstListBoxSelection() {
    String initToken = History.getToken();
    if (initToken.length() == 0) {
      System.out.println("pushFirstListBoxSelection : history token is blank");

      pushNewSectionHistoryToken();
    } else {
      System.out.println("pushFirstListBoxSelection fire history for token from URL: " +initToken);
      setModeLinks(initToken);
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

    setModeLinks(historyToken);
    if (currentToken.equals(historyToken)) {
      if (currentExercises == null || currentExercises.isEmpty() || historyToken.isEmpty()) {
        System.out.println("pushNewSectionHistoryToken : noSectionsGetExercises for token '" + historyToken + "' current " +currentExercises);

        noSectionsGetExercises(userID);
      } else {
        System.out.println("pushNewSectionHistoryToken : skipping same token '" + historyToken + "'");
      }
    } else {
      System.out.println("pushNewSectionHistoryToken : currentToken " + currentToken);

      setHistoryItem(historyToken);
    }
  }

  protected void setModeLinks(String historyToken) {}

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
  protected void gotClickOnItem(ExerciseShell e) {
    System.out.println("----------- got click on " + e.getID() + " -------------- ");
    if (!includeItemInBookmark) {
      loadByID(e.getID());
    }
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
    for (String type : typeOrder) {
      Collection<String> selections = selectionState2.get(type);
      if (selections.iterator().next().equals(HistoryExerciseList.ANY)) {
        if (hasNonClearSelection) {
          System.out.println("restoreListBoxState : skipping type since below a selection = " + type);
        }
        else {
          System.out.println("restoreListBoxState : clearing " + type);

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
          //selectItem(type, selections);
        }
      }
    }

    System.out.println("restoreListBoxState :typesWithSelections " + typesWithSelections);

    // clear enabled state for all items below first selection...
    if (!typesWithSelections.isEmpty()) {
      List<String> afterFirst = new ArrayList<String>();
      String first = typesWithSelections.get(0);
      boolean start = false;
      for (String type : typeOrder) {
         if (start) afterFirst.add(type);
         if (type.equals(first)) start = true;
      }

     // List<String> afterFirst = typesWithSelections.subList(1, typesWithSelections.size());
      System.out.println("restoreListBoxState : afterFirst " + afterFirst);

      for (String type : afterFirst) {
        System.out.println("restoreListBoxState : clearing enabled on " + type);

        clearEnabled(type);
      }
    }

    for (String type : typesWithSelections) {
      System.out.println("restoreListBoxState : selecting items for " + type);

      Collection<String> selections = selectionState2.get(type);
      selectItem(type, selections);
    }
/*    List<String> toRemove = new ArrayList<String>();
    for (Map.Entry<String, Collection<String>> typeSelectionPair : selectionState2.entrySet()) {
      Collection<String> selections = typeSelectionPair.getValue();
      if (selections.size() == 1 && selections.iterator().next().equals(ANY)) {
        toRemove.add(typeSelectionPair.getKey());
        selectItem(typeSelectionPair.getKey(), selections);
      }
    }

    System.out.println("restoreListBoxState : selection state " + selectionState2);
    Collection<String> types = new ArrayList<String>(getTypeOrder(selectionState2));
    types.removeAll(toRemove);
    // whatever is left here are actual selections
    for (String type : types) {
      if (selectionState2.containsKey(type)) {
        Collection<String> section = selectionState2.get(type);
        if (!typeToBox.containsKey(type)) {
          if (!type.equals("item")) {
            System.err.println("restoreListBoxState for " + selectionState + " : huh? bad type '" + type +
              "', expecting something in " + typeToBox.keySet());
          }
        } else {
          selectItem(type, section);
        }
      }
    }*/
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
    String rawToken = getTokenFromEvent(event);
    System.out.println(new Date() +" HistoryExerciseList.onValueChange : ------ start: token is '" + rawToken +"' ----");
    SelectionState selectionState1 = getSelectionState(rawToken);

    String instance1 = selectionState1.getInstance();

    if (!instance1.equals(instance) && instance1.length() > 0) {
      System.out.println("onValueChange : skipping event " + rawToken + " for instance '" + instance1 +
          "' that is not mine "+instance);
      if (getCreatedPanel() == null) {
/*
        getExercises(controller.getUser(),false);
*/
        noSectionsGetExercises(controller.getUser());
      }
      return;
    }

    String item = selectionState1.getItem();

    if (item != null && item.length() > 0 && hasExercise(item)) {
    //  if (includeItemInBookmark) {
        loadByIDFromToken(item);
  /*    }
      else {
        System.out.println("onValueChange : skipping item " + item);
      }*/
    } else {
      String token = event.getValue();
      try {
        SelectionState selectionState = getSelectionState(token);
        restoreListBoxState(selectionState);
        loadExercises(selectionState.getTypeToSection(), selectionState.getItem());
      } catch (Exception e) {
        System.out.println("onValueChange " + token + " badly formed. Got " + e);
        e.printStackTrace();
      }
    }
    //System.out.println("onValueChange : ------ end : token is '" + rawToken + "' ------------ ");
  }

  /**
   * When we get a history token push, select the exercise type, section, and optionally item.
   * @param typeToSection
   * @param item null is OK
   * @see #onValueChange(com.google.gwt.event.logical.shared.ValueChangeEvent)
   */
  protected void loadExercises(final Map<String, Collection<String>> typeToSection, final String item) {
    System.out.println("HistoryExerciseList.loadExercises : " + typeToSection + " and item '" + item + "'");
    if (controller.showCompleted()) {
      service.getCompletedExercises(controller.getUser(), controller.isReviewMode(), new AsyncCallback<Set<String>>() {
        @Override
        public void onFailure(Throwable caught) {}

        @Override
        public void onSuccess(Set<String> result) {
          controller.getExerciseList().setCompleted(result);
          loadExercisesUsingPrefix(typeToSection, getPrefix());
        }
      });
    }
    else {
      loadExercisesUsingPrefix(typeToSection, getPrefix());
    }
  }

  protected void loadExercises(String selectionState, String prefix) {
    Map<String, Collection<String>> typeToSection = getSelectionState(selectionState).getTypeToSection();
    loadExercisesUsingPrefix(typeToSection, prefix);
  }

  private void loadExercisesUsingPrefix(Map<String, Collection<String>> typeToSection, String prefix) {
    if (prefix.isEmpty()) {
      reallyLoadExercises(typeToSection, null);
    } else {
      lastReqID++;
      System.out.println("HistoryExerciseList.loadExercisesUsingPrefix looking for '" + prefix + "' (" + prefix.length() + " chars) in context of " + typeToSection);

      if (typeToSection.isEmpty()) {
        service.getExerciseIds(lastReqID, userID, prefix, -1, new SetExercisesCallback());
      } else {
        service.getExercisesForSelectionState(lastReqID, typeToSection, userID, prefix, new MySetExercisesCallback(null));
      }
    }
  }

  private void reallyLoadExercises(Map<String, Collection<String>> typeToSection, String item) {
    System.out.println("reallyLoadExercises looking for '" + item + "' in context of " + typeToSection);

    if (typeToSection.isEmpty() && item == null) {
      noSectionsGetExercises(userID);
    } else {
      lastReqID++;
      service.getExercisesForSelectionState(lastReqID, typeToSection, userID, new MySetExercisesCallback(item));
    }
  }

  /**
   * Ask the server for the items for the type->item map.  Remember the results and select the first one.
   */
  protected class MySetExercisesCallback extends SetExercisesCallback {
    private final String item;

    /**
     * @see HistoryExerciseList#loadExercises(java.util.Map, String)
     * @param item
     */
    public MySetExercisesCallback(String item) {  this.item = item;  }

    @Override
    public void onSuccess(ExerciseListWrapper result) {
      System.out.println("MySetExercisesCallback : onSuccess " + result.getExercises().size() + " items and item " +item);

      if (isStaleResponse(result)) {
        System.out.println("\t----> ignoring result " + result.getReqID() + " b/c before latest " + lastReqID);
      } else {
        if (result.getExercises().isEmpty()) {
          System.out.println("\t----> result is empty...");

          if (item != null && item.startsWith("Custom")) {
            System.out.println("\t----> skip warning about empty list for now "); // TODO revisit this
          }
          else {
            gotEmptyExerciseList();
          }
          rememberExercises(result.getExercises());
        } else {
          if (item != null) {
            rememberExercises(result.getExercises());
            controller.showProgress();
            if (!loadByID(item)) {
              System.out.println("\tMySetExercisesCallback.onSuccess : loading first exercise since couldn't load item=" + item);
              loadFirstExercise();
            }
            else {
              //System.out.println("\tMySetExercisesCallback.onSuccess :");
            }
          } else {
            System.out.println("\tMySetExercisesCallback.onSuccess : item is null");

            super.onSuccess(result);     // remember and load the first one
          }
        }
      }
    }
  }
}
