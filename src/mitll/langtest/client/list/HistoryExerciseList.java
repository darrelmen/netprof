package mitll.langtest.client.list;

import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.SectionWidget;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.shared.CommonShell;

import java.util.*;
import java.util.logging.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 11/6/13
 * Time: 3:21 PM
 * To change this template use File | Settings | File Templates.
 */
public class HistoryExerciseList extends PagingExerciseList {
  private Logger logger = Logger.getLogger("HistoryExerciseList");

  public static final String ANY = "Clear";
  private static final boolean debugOnValueChange = false;
  private static final boolean DEBUG = false;

  protected final Map<String, SectionWidget> typeToBox = new HashMap<String, SectionWidget>();
  protected long userID;

  /**
   * @see mitll.langtest.client.bootstrap.FlexSectionExerciseList#FlexSectionExerciseList(Panel, Panel, LangTestDatabaseAsync, UserFeedback, ExerciseController, String, boolean)
   * @param currentExerciseVPanel
   * @param service
   * @param feedback
   * @param controller
   * @param showTypeAhead
   * @param instance
   * @param incorrectFirst
   */
  protected HistoryExerciseList(Panel currentExerciseVPanel, LangTestDatabaseAsync service, UserFeedback feedback,
                                ExerciseController controller,
                                boolean showTypeAhead, String instance, boolean incorrectFirst) {
    super(currentExerciseVPanel, service, feedback, null, controller, showTypeAhead, instance, incorrectFirst);
  }

  protected String getHistoryToken(String id) {
    return getHistoryToken("", id);
  }

  /**
   * @param search
   * @param id
   * @return
   * @see ExerciseList#pushNewItem(String, String)
   * @see #pushNewSectionHistoryToken()
   */
  protected String getHistoryToken(String search, String id) {
    if (typeToBox.isEmpty()) {
      return History.getToken();
    }
    //logger.info("getHistoryToken for " + id + " examining " +typeToBox.size() + " boxes.");
    StringBuilder unitAndChapterSelection = new StringBuilder();
    for (String type : typeToBox.keySet()) {
      String section = getCurrentSelection(type);
      //logger.info("\tHistoryExerciseList.getHistoryToken for " + type + " section = " +section);
      if (!section.equals(HistoryExerciseList.ANY)) {
        unitAndChapterSelection.append(type + "=" + section + ";");
      }
    }

/*    if (id != null && INCLUDE_ITEM_IN_BOOKMARK) {
      String historyToken = super.getHistoryToken(id);
      //logger.info("getHistoryToken for " + id + " would add " +historyToken);

      unitAndChapterSelection.append(historyToken);
    }*/
    //logger.info("\tgetHistoryToken for " + id + " is '" +unitAndChapterSelection.toString() + "'");
    String instanceSuffix = ";" + SelectionState.INSTANCE + "=" + getInstance();
    boolean hasItemID = id != null && id.length() > 0;

    String s = (hasItemID ? super.getHistoryToken(search, id) + ";" : "search=" + search + ";") + unitAndChapterSelection + instanceSuffix;
 //   logger.info("getHistoryToken '" + s + "'");
    return s;
  }

  /**
   * @param type
   * @return
   * @see PagingExerciseList#getHistoryToken(String, String)
   */
  private String getCurrentSelection(String type) {
    SectionWidget listBox = typeToBox.get(type);
    return listBox.getCurrentSelection();
  }

  protected void setHistoryItem(String historyToken) {
    //logger.info("------------ HistoryExerciseList.setHistoryItem '" + historyToken + "' -------------- ");
    History.newItem(historyToken);
  }

  /**
   * @param search
   * @param exerciseID
   * @see ListInterface#loadExercise(String)
   * @see #pushFirstSelection(String)
   */
  protected void pushNewItem(String search, String exerciseID) {
    logger.info("------------ HistoryExerciseList.pushNewItem : -- " + search + " : " + exerciseID);

    String historyToken = getHistoryToken(search, exerciseID);
    String trimmedToken = historyToken.length() > 2 ? historyToken.substring(0, historyToken.length() - 2) : historyToken;
    if (DEBUG)
      logger.info("------------ HistoryExerciseList.pushNewItem : push history '" + historyToken + "' -------------- " + search + " : " + exerciseID);

    String token = History.getToken();
    logger.info("\tpushNewItem : current token '" + token + "' vs new id '" + exerciseID +"'");

    token = getSelectionFromToken(token);
    if (DEBUG)
      logger.info("\tHistoryExerciseList.pushNewItem : current token '" + token + "' vs new id '" + exerciseID + "'");
    if (token != null && (historyToken.equals(token) || trimmedToken.equals(token))) {
      logger.info("\tHistoryExerciseList.pushNewItem : current token '" + token + "' same as new " + historyToken);
      checkAndAskServer(exerciseID);
    } else {
      logger.info("\tHistoryExerciseList.pushNewItem : current token '" + token + "' different menu state '" + historyToken + "' from new " + exerciseID);
      setHistoryItem(historyToken);
    }
  }

  /**
   * So if we have an existing history token, use it to set current selection.
   * If not, push the current state of the list boxes and act on it
   *
   * @see ListInterface#getExercises(long)
   */
  protected void pushFirstListBoxSelection() {
    String initToken = History.getToken();
    if (initToken.length() == 0) {
      logger.info("pushFirstListBoxSelection : history token is blank " + getInstance());

      pushNewSectionHistoryToken();
    } else {
      logger.info("pushFirstListBoxSelection fire history for token from URL: " + initToken + " instance " + getInstance());
      History.fireCurrentHistoryState();
    }
  }

  /**
   * @see #pushFirstListBoxSelection
   * @see mitll.langtest.client.bootstrap.FlexSectionExerciseList#addClickHandlerToButton
   */
  protected void pushNewSectionHistoryToken() {
    String historyToken = getHistoryToken("", null);
    String currentToken = History.getToken();

    if (currentToken.equals(historyToken)) {
      if (isEmpty() || historyToken.isEmpty()) {
        logger.info("pushNewSectionHistoryToken : noSectionsGetExercises for token '" + historyToken +
            "' " + "current has " + getSize() + " instance " + getInstance());

        noSectionsGetExercises(userID);
      } else {
        logger.info("pushNewSectionHistoryToken : skipping same token '" + historyToken + "'" + " instance " + getInstance());
      }
    } else {
      //logger.info("pushNewSectionHistoryToken : currentToken " + currentToken + " instance " + getInstance());
      setHistoryItem(historyToken);
    }
  }

  /**
   * @param type
   * @param sections
   * @see #restoreListBoxState(SelectionState)
   */
  protected void selectItem(String type, Collection<String> sections) {
    SectionWidget listBox = typeToBox.get(type);
    listBox.selectItem(sections, false);
  }

  /**
   * @param e
   * @see mitll.langtest.client.list.PagingExerciseList#makePagingContainer()
   */
  @Override
  protected void gotClickOnItem(CommonShell e) {
    loadByID(e.getID());
  }

  public void loadExercise(String itemID) { pushNewItem(getTypeAheadText(), itemID);  }

  /**
   * Given a selectionState state, make sure the list boxes are consistent with it.
   *
   * @param selectionState
   * @see #onValueChange(com.google.gwt.event.logical.shared.ValueChangeEvent)
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
      selectionState2.put(type, section);
    }

    boolean hasNonClearSelection = false;
    List<String> typesWithSelections = new ArrayList<String>();
    Collection<String> typeOrder = getTypeOrder(selectionState2);
    if (typeOrder == null) {
      logger.warning("huh? type order is null for " + selectionState2);
      typeOrder = Collections.emptyList();
    }
    for (String type : typeOrder) {
      Collection<String> selections = selectionState2.get(type);
      if (selections.iterator().next().equals(HistoryExerciseList.ANY)) {
        if (hasNonClearSelection) {
          //logger.info("restoreListBoxState : skipping type since below a selection = " + type);
        } else {
          //logger.info("restoreListBoxState : clearing " + type);

          selectItem(type, selections);
        }
      } else {
        if (!hasNonClearSelection) {
          enableAllButtonsFor(type);  // first selection row should always be fully enabled -- there's nothing above it to constrain the selections
        }
        hasNonClearSelection = true;

        if (!typeToBox.containsKey(type)) {
          if (!type.equals("item")) {
            logger.warning("restoreListBoxState for " + selectionState + " : huh? bad type '" + type +
                "', expecting something in " + typeToBox.keySet());
          }
        } else {
          typesWithSelections.add(type);
        }
      }
    }

    //logger.info("restoreListBoxState :typesWithSelections " + typesWithSelections);

    // clear enabled state for all items below first selection...
    if (!typesWithSelections.isEmpty()) {
      List<String> afterFirst = new ArrayList<String>();
      String first = typesWithSelections.get(0);
      boolean start = false;
      for (String type : typeOrder) {
        if (start) afterFirst.add(type);
        if (type.equals(first)) start = true;
      }

      //logger.info("restoreListBoxState : afterFirst " + afterFirst);

      for (String type : afterFirst) {
        //logger.info("restoreListBoxState : clearing enabled on " + type);

        clearEnabled(type);
      }
    }

    for (String type : typesWithSelections) {
      selectItem(type, selectionState2.get(type));
    }
  }

  protected void clearEnabled(String type) {
  }

  protected void enableAllButtonsFor(String type) {
  }

  protected Collection<String> getTypeOrder(Map<String, Collection<String>> selectionState2) {
    return selectionState2.keySet();
  }

  /**
   * Respond to push a history token.
   *
   * @param event
   */
  @Override
  public void onValueChange(ValueChangeEvent<String> event) {
    if (debugOnValueChange) logger.info(" HistoryExerciseList.onValueChange : ------ start ---- " + getInstance());

    String originalValue = event.getValue();
    String rawToken = getTokenFromEvent(event);
    SelectionState selectionState1 = getSelectionState(rawToken);

 //   logger.info("orig " + originalValue + " raw " + rawToken + " sel " + selectionState1.getInfo());

    String instance1 = selectionState1.getInstance();

    if (!instance1.equals(getInstance()) && instance1.length() > 0) {
      if (debugOnValueChange)  logger.info("onValueChange : skipping event " + rawToken + " for instance '" + instance1 +
          "' that is not mine "+ getInstance());
      if (getCreatedPanel() == null) {
        noSectionsGetExercises(controller.getUser());
      }
      return;
    }
    if (debugOnValueChange) {
      logger.info(new Date() + " HistoryExerciseList.onValueChange : originalValue '" +originalValue+
        "'" +
        " token is '" + rawToken + "' for " + instance1 + " vs my instance " + getInstance());
    }

    String item = selectionState1.getItem();

    if (item != null && item.length() > 0 && hasExercise(item)) {
      //  if (INCLUDE_ITEM_IN_BOOKMARK) {
      checkAndAskServer(item);
  /*    }
      else {
        logger.info("onValueChange : skipping item " + item);
      }*/
    } else {
      String token = event.getValue();
      try {
        SelectionState selectionState = getSelectionState(token);
        restoreListBoxState(selectionState);
        if (debugOnValueChange) {
          logger.info("HistoryExerciseList.onValueChange : selectionState '" + selectionState + "'");
        }

        setTypeAheadText(selectionState.getSearch());
        loadExercisesUsingPrefix(selectionState.getTypeToSection(), selectionState.getSearch(), false);
      } catch (Exception e) {
        logger.warning("HistoryExerciseList.onValueChange " + token + " badly formed. Got " + e);
        e.printStackTrace();
      }
    }
  }

  /**
   * When we get a history token push, select the exercise type, section, and optionally item.
   *
   * @see #onValueChange(com.google.gwt.event.logical.shared.ValueChangeEvent)
   */
  protected void loadExercises(String selectionState, String prefix, boolean onlyWithAudioAnno) {
    Map<String, Collection<String>> typeToSection = getSelectionState(selectionState).getTypeToSection();
/*    logger.info("HistoryExerciseList.loadExercises : looking for " +
      "'" + prefix + "' (" + prefix.length() + " chars) in list id "+userListID + " instance " + getInstance());*/
    loadExercisesUsingPrefix(typeToSection, prefix, onlyWithAudioAnno);
  }

  /**
   * @param typeToSection
   * @param prefix
   * @param onlyWithAudioAnno
   * @see #loadExercises(java.util.Map, String)
   * @see PagingExerciseList#loadExercises(String, String, boolean)
   * @see mitll.langtest.client.custom.content.NPFHelper.FlexListLayout.MyFlexSectionExerciseList#loadExercises(java.util.Map, String)
   */
  protected void loadExercisesUsingPrefix(Map<String, Collection<String>> typeToSection, String prefix,
                                          boolean onlyWithAudioAnno) {
    lastReqID++;
    if (DEBUG) {
      logger.info("HistoryExerciseList.loadExercisesUsingPrefix looking for '" + prefix +
          "' (" + prefix.length() + " chars) in context of " + typeToSection + " list " + userListID +
          " instance " + getInstance() + " user " + controller.getUser() + " unrecorded " + getUnrecorded() +
          " only examples " + isOnlyExamples());
    }
    String selectionID = userListID + "_" + typeToSection.toString();
    scheduleWaitTimer();
    service.getExerciseIds(lastReqID, typeToSection, prefix, userListID, controller.getUser(), getRole(),
        getUnrecorded(), isOnlyExamples(), incorrectFirstOrder, onlyWithAudioAnno, new SetExercisesCallback(selectionID));
  }

  @Override
  public void reload(Map<String, Collection<String>> typeToSection) {
    loadExercisesUsingPrefix(typeToSection, getTypeAheadText(), false);
  }

  /**
   * @param token
   * @return object representing type=value pairs from history token
   * @see PagingExerciseList#loadExercises(String, String, boolean)
   * @see #onValueChange(com.google.gwt.event.logical.shared.ValueChangeEvent)
   */
  protected SelectionState getSelectionState(String token) {
    return new SelectionState(token, !allowPlusInURL);
  }

  /**
   * @param userID
   * @see #loadExercises(java.util.Map, String)
   * @see #pushNewSectionHistoryToken()
   */
  protected void noSectionsGetExercises(long userID) {
    super.getExercises(userID);
  }
}
