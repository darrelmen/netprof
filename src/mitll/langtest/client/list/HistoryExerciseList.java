/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

package mitll.langtest.client.list;

import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.amas.SingleSelectExerciseList;
import mitll.langtest.client.bootstrap.FlexSectionExerciseList;
import mitll.langtest.client.custom.content.NPFlexSectionExerciseList;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.SectionWidget;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.shared.exercise.CommonShell;
import mitll.langtest.shared.exercise.ExerciseListRequest;
import mitll.langtest.shared.exercise.Shell;

import java.util.*;
import java.util.logging.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 11/6/13
 * Time: 3:21 PM
 * To change this template use File | Settings | File Templates.
 */
public class HistoryExerciseList<T extends CommonShell, U extends Shell, V extends SectionWidget>
    extends PagingExerciseList<T, U> implements ValueChangeHandler<String> {
  private final Logger logger = Logger.getLogger("HistoryExerciseList");

  public static final String ANY = "Clear";

  private HandlerRegistration handlerRegistration;
  protected long userID;
  protected final SectionWidgetContainer<V> sectionWidgetContainer;



  protected static final boolean DEBUG_ON_VALUE_CHANGE = true;
  private static final boolean DEBUG = true;

  /**
   * @param currentExerciseVPanel
   * @param service
   * @param feedback
   * @param controller
   * @param showTypeAhead
   * @param instance
   * @param incorrectFirst
   * @see mitll.langtest.client.bootstrap.FlexSectionExerciseList#FlexSectionExerciseList(Panel, Panel, LangTestDatabaseAsync, UserFeedback, ExerciseController, String, boolean)
   */
  protected HistoryExerciseList(Panel currentExerciseVPanel, LangTestDatabaseAsync service, UserFeedback feedback,
                                ExerciseController controller,
                                boolean showTypeAhead, String instance, boolean incorrectFirst) {
    super(currentExerciseVPanel, service, feedback, null, controller, showTypeAhead, instance, incorrectFirst);
    sectionWidgetContainer = getSectionWidgetContainer();
    addHistoryListener();
  }

  protected SectionWidgetContainer<V> getSectionWidgetContainer() {
    return new SectionWidgetContainer<V>();
  }

  protected V getSectionWidget(String type) {
    return sectionWidgetContainer.getWidget(type);
  }

  protected String getHistoryToken(String id) {
    return getHistoryTokenFromUIState("", id);
  }

  /**
   * @param search
   * @param id
   * @return
   * @see ExerciseList#pushNewItem(String, String)
   * @see #pushNewSectionHistoryToken()
   */
  protected String getHistoryTokenFromUIState(String search, String id) {
    String unitAndChapterSelection = sectionWidgetContainer.getHistoryToken();

    //logger.info("\tgetHistoryToken for " + id + " is '" +unitAndChapterSelection.toString() + "'");
    String instanceSuffix = getInstance().isEmpty() ? "" : ";" + SelectionState.INSTANCE + "=" + getInstance();
    boolean hasItemID = id != null && id.length() > 0;

    String s = (hasItemID ? super.getHistoryTokenFromUIState(search, id) + ";" : "search=" + search + ";") +
        unitAndChapterSelection + instanceSuffix;
    //logger.info("getHistoryTokenFromUIState '" + s + "'");
    return s;
  }

  private void addHistoryListener() {
    if (handlerRegistration == null) {
      handlerRegistration = History.addValueChangeHandler(this);
    }
  }

  private void removeHistoryListener() {
    if (handlerRegistration != null) {
      handlerRegistration.removeHandler();
      handlerRegistration = null;
    }
  }

  @Override
  protected void onUnload() {
    super.onUnload();
    removeHistoryListener();
  }

  /**
   * So we have a catch-22 -
   * <p>
   * If we fire the current history, we override the initial selection associated
   * with a user logging in for the first time.
   * <p>
   * If we don't, when we click on a link from an email, the item=NNN value will be ignored.
   * <p>
   * I gotta go with the latter.
   *
   * @param exerciseID
   * @param searchIfAny
   * @see #loadFirstExercise()
   */
  void pushFirstSelection(String exerciseID, String searchIfAny) {
    String token = History.getToken();
    String idFromToken = getIDFromToken(token);
/*    if (DEBUG) logger.info("ExerciseList.pushFirstSelection : current token '" + token + "' id from token '" + idFromToken +
        "' vs new exercise " + exerciseID + " instance " + getInstance());*/

    if (idFromToken.equals(exerciseID)) {
      if (DEBUG)
        logger.info("pushFirstSelection : (" + getInstance() + ") current token " + token + " same as new " + exerciseID);
      checkAndAskOrFirst(exerciseID);
    } else {
      if (DEBUG) logger.info("pushFirstSelection : (" + getInstance() + ") pushNewItem " + exerciseID  + " vs " + idFromToken);
      String toUse = getValidExerciseID(exerciseID);
      pushNewItem(searchIfAny, toUse);
    }
  }

  /**
   * @param token
   * @return
   * @see #onValueChange(com.google.gwt.event.logical.shared.ValueChangeEvent)
   */
  private String getIDFromToken(String token) {
    if (token.startsWith("#item=") || token.startsWith("item=")) {
      SelectionState selectionState = new SelectionState(token, !allowPlusInURL);
      if (!selectionState.getInstance().equals(getInstance())) {
        //if (DEBUG) logger.info("got history item for another instance '" + selectionState.getInstance() + "' vs me '" + instance +"'");
      } else {
        String item = selectionState.getItem();
        // if (DEBUG) logger.info("got history item for instance '" + selectionState.getInstance() + " : '" + item+"'");
        return item;
      }
    }
    return "";
  }

  /**
   * @see #loadExercisesUsingPrefix(Map, String, boolean, String)
   * @see #pushFirstSelection(String, String)
   * @see #pushNewItem(String, String)
   * @param exerciseID
   */

  private void checkAndAskOrFirst(String exerciseID) {
    String toUse = getValidExerciseID(exerciseID);
    if (hasExercise(toUse)) {
      checkAndAskServer(toUse);
    }
//    else if (!isEmpty()) {
//      pushNewItem(search, getFirst().getID());
//    }
  }

  private String getValidExerciseID(String exerciseID) {
    return hasExercise(exerciseID) ? exerciseID : isEmpty() ? "" : getFirst().getID();
  }

  /**
   * @param search
   * @param exerciseID
   * @see #loadExercise(String)
   * @see #pushFirstSelection(String, String)
   * @see PagingExerciseList#gotClickOnItem(CommonShell)
   */
  void pushNewItem(String search, String exerciseID) {
//    if (DEBUG) {
//      logger.info("HistoryExerciseList.pushNewItem : search '" + search + "' : item '" + exerciseID + "'");
//    }
    String historyToken = getHistoryTokenFromUIState(search, exerciseID);
    String trimmedToken = getTrimmedToken(historyToken);
    if (DEBUG) {
      logger.info("HistoryExerciseList.pushNewItem : push history '" + historyToken + "' search '" + search + "' : " + exerciseID);
    }

    String currentToken = History.getToken();
    // logger.info("HistoryExerciseList.pushNewItem : current currentToken '" + currentToken + "' vs new id '" + exerciseID + "'");
    //currentToken = getSelectionFromToken(currentToken);
//    if (DEBUG)
//      logger.info("HistoryExerciseList.pushNewItem : current currentToken '" + currentToken + "' vs new id '" + exerciseID + "'");
    if (currentToken != null && (historyToken.equals(currentToken) || trimmedToken.equals(currentToken))) {
      if (DEBUG)
        logger.info("HistoryExerciseList.pushNewItem : current currentToken '" + currentToken + "' same as new " + historyToken);
      checkAndAskOrFirst(exerciseID);
    } else {
      if (DEBUG)
        logger.info("HistoryExerciseList.pushNewItem : current currentToken '" + currentToken + "' different menu state '" + historyToken + "' from new " + exerciseID);
      setHistoryItem(historyToken);
    }
   // checkAndAskOrFirst(exerciseID);
  }

  private String getTrimmedToken(String historyToken) {
    return historyToken.length() > 2 ? historyToken.substring(0, historyToken.length() - 2) : historyToken;
  }

  /**
   * So if we have an existing history token, use it to set current selection.
   * If not, push the current state of the list boxes and act on it
   *
   * @see FlexSectionExerciseList#setSizesAndPushFirst()
   */
  protected void pushFirstListBoxSelection() {
    if (History.getToken().isEmpty()) {
      //logger.info("pushFirstListBoxSelection : history token is blank " + getInstance());
      pushNewSectionHistoryToken();
    } else {
      //logger.info("pushFirstListBoxSelection fire history for token from URL: " + initToken + " instance " + getInstance());
      History.fireCurrentHistoryState();
    }
  }

  /**
   * @see #pushFirstListBoxSelection
   * @see mitll.langtest.client.bootstrap.FlexSectionExerciseList#addClickHandlerToButton
   */
  protected void pushNewSectionHistoryToken() {
    String currentToken = History.getToken();
    SelectionState selectionState = getSelectionState(currentToken);
    String historyToken = getHistoryTokenFromUIState(getTypeAheadText(), selectionState.getItem());

    if (currentToken.equals(historyToken)) {
      if (isEmpty() || historyToken.isEmpty()) {
        if (DEBUG) logger.info("pushNewSectionHistoryToken : noSectionsGetExercises for token '" + historyToken +
            "' " + "current has " + getSize() + " instance " + getInstance());

        noSectionsGetExercises(userID);
      } else {
        logger.info("pushNewSectionHistoryToken : skipping same token '" + historyToken + "'" + " instance " + getInstance());
      }
    } else {
      if (DEBUG)
        logger.info("pushNewSectionHistoryToken : currentToken " + currentToken + " instance " + getInstance());
 //     setHistoryItem(historyToken);
      SelectionState newState = getSelectionState(sectionWidgetContainer.getHistoryToken());
      loadExercisesUsingPrefix(newState.getTypeToSection(), selectionState.getSearch(), false, selectionState.getItem());
    }
  }

  protected void setHistoryItem(String historyToken) {
    if (DEBUG) logger.info("HistoryExerciseList.setHistoryItem '" + historyToken + "' -------------- ");
    History.newItem(historyToken);
  }

  /**
   * @param type
   * @param sections
   * @see #restoreListBoxState(SelectionState)
   */
/*  protected void selectItem(String type, Collection<String> sections) {
    sectionWidgetContainer.selectItem(type, sections);
  }*/

  /**
   * @param e
   * @see mitll.langtest.client.list.PagingExerciseList#makePagingContainer()
   */
  @Override
  public void gotClickOnItem(T e) {
    loadByID(e.getID());
  }

  public void loadExercise(String itemID) {
    pushNewItem(getTypeAheadText(), itemID);
  }

  /**
   * Given a selectionState state, make sure the list boxes are consistent with it.
   *
   * @param selectionState
   * @see #onValueChange(com.google.gwt.event.logical.shared.ValueChangeEvent)
   */
  protected void restoreListBoxState(SelectionState selectionState) {
    logger.info("restoreListBoxState restore " + selectionState);
    sectionWidgetContainer.restoreListBoxState(selectionState, controller.getStartupInfo().getTypeOrder());

   /* Map<String, Collection<String>> selectionState2 = new HashMap<>();

    // make sure we all types have selections, even if it's the default Clear (ANY) selection
    for (String type : sectionWidgetContainer.getTypes()) {
      selectionState2.put(type, Collections.singletonList(HistoryExerciseList.ANY));
    }
    selectionState2.putAll(selectionState.getTypeToSection());

    boolean hasNonClearSelection = false;
    List<String> typesWithSelections = new ArrayList<>();
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

        if (!sectionWidgetContainer.hasType(type)) {
          if (!type.equals("item")) {
            logger.warning("restoreListBoxState for " + selectionState + " : huh? bad type '" + type +
                "', expecting something in " + sectionWidgetContainer.getTypes());
          }
        } else {
          typesWithSelections.add(type);
        }
      }
    }

    logger.info("restoreListBoxState :typesWithSelections " + typesWithSelections);

    // clear enabled state for all items below first selection...
    if (!typesWithSelections.isEmpty()) {
      List<String> afterFirst = new ArrayList<>();
      String first = typesWithSelections.get(0);
      boolean start = false;
      for (String type : typeOrder) {
        if (start) afterFirst.add(type);
        if (type.equals(first)) start = true;
      }

      logger.info("restoreListBoxState : afterFirst " + afterFirst);

      for (String type : afterFirst) {
        logger.info("restoreListBoxState : clearing enabled on " + type);
        clearEnabled(type);
      }
    }

    for (String type : typesWithSelections) {
      selectItem(type, selectionState2.get(type));
    }
    String unitAndChapterSelection = sectionWidgetContainer.getHistoryToken();

    logger.info("UI should now show " + selectionState.getTypeToSection() + " vs actual " + unitAndChapterSelection);*/
  }
/*
  protected void clearEnabled(String type) {
  }

  protected void enableAllButtonsFor(String type) {
  }*/

/*  protected Collection<String> getTypeOrder(Map<String, Collection<String>> selectionState2) {
    return selectionState2.keySet();
  }*/

  /**
   * Respond to push a history token.
   *
   * @param event
   */
  @Override
  public void onValueChange(ValueChangeEvent<String> event) {
    // if (DEBUG_ON_VALUE_CHANGE) logger.info("HistoryExerciseList.onValueChange : ------ start ---- " + getInstance());
    SelectionState selectionState = getSelectionState(event.getValue());
    //   logger.info("orig " + originalValue + " raw " + rawToken + " sel " + selectionState1.getInfo());
    String instance1 = selectionState.getInstance();

    if (!instance1.equals(getInstance()) && instance1.length() > 0) {
      if (DEBUG_ON_VALUE_CHANGE) {
        logger.info("onValueChange : skipping event " + event.getValue() + " for instance '" + instance1 +
            "' that is not mine '" + getInstance() + "'");
      }
      if (getCreatedPanel() == null) {
   //     noSectionsGetExercises(controller.getUser());
      }
      return;
    }
    if (DEBUG_ON_VALUE_CHANGE) {
      logger.info("HistoryExerciseList.onValueChange : originalValue '" + event.getValue() +
          "'" +
          " token is '" + event.getValue() + "' for " + instance1 + " vs my instance " + getInstance());
    }

    String item = selectionState.getItem();

    /*boolean restored =*/ restoreUIState(selectionState);
//    if (!item.isEmpty() && hasExercise(item) && !restored) {
////      logger.info("HistoryExerciseList.onValueChange : checkAndAskOrFirst for item '" + item + "'");
//      checkAndAskOrFirst(item);
//    } else {
    try {
      loadExercisesUsingPrefix(selectionState.getTypeToSection(), selectionState.getSearch(), false, item);
    } catch (Exception e) {
      logger.warning("HistoryExerciseList.onValueChange " + event.getValue() + " badly formed. Got " + e);
      // e.printStackTrace();
    }
//    }
  }

  /**
   * @param selectionState
   * @return true if we're just clicking on a different item in the list and don't need to reload the exercise list
   */
  private void restoreUIState(SelectionState selectionState) {
    String search = selectionState.getSearch();
    restoreListBoxState(selectionState);
//    if (DEBUG_ON_VALUE_CHANGE) {
//      logger.info("HistoryExerciseList.onValueChange : selectionState '" + selectionState + "' search from token '" + search +
//          "'");
//    }

    setTypeAheadText(search);
//    return true;
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
    loadExercisesUsingPrefix(typeToSection, prefix, onlyWithAudioAnno, "");
  }

  /**
   * @param typeToSection
   * @param prefix
   * @param onlyWithAudioAnno
   * @param exerciseID
   * @see #loadExercises
   * @see PagingExerciseList#loadExercises(String, String, boolean)
   * @see NPFlexSectionExerciseList#loadExercises
   */
  protected void loadExercisesUsingPrefix(Map<String, Collection<String>> typeToSection,
                                          String prefix,
                                          boolean onlyWithAudioAnno, String exerciseID) {
    ExerciseListRequest request = getRequest(prefix)
        .setTypeToSelection(typeToSection)
        .setOnlyWithAudioAnno(onlyWithAudioAnno);

    // logger.info("last success " + lastSuccessfulRequest);

    if (lastSuccessfulRequest == null || !request.sameAs(lastSuccessfulRequest)) {
      try {
        if (DEBUG) {
          logger.info("HistoryExerciseList.loadExercisesUsingPrefix looking for '" + prefix +
              "' (" + prefix.length() + " chars) in context of " + typeToSection + " list " + userListID +
              " instance " + getInstance() + " user " + controller.getUser() + " unrecorded " + getUnrecorded() +
              " only examples " + isOnlyExamples());
        }
        scheduleWaitTimer();
        String selectionID = userListID + "_" + typeToSection.toString();
        service.getExerciseIds(
            request,
            new SetExercisesCallback(selectionID, prefix, exerciseID, request));
      } catch (Exception e) {
        logger.warning("got " + e);
      }
    } else {
      if (DEBUG) {
        logger.info("skipping request for current list " + request);
      }
      if (exerciseID != null) {
        checkAndAskOrFirst(exerciseID);
      } else {
        logger.warning("Not doing anything as response to " + request);
      }
    }
  }

  @Override
  public void reload(Map<String, Collection<String>> typeToSection) {
    loadExercisesUsingPrefix(typeToSection, getTypeAheadText(), false, "");
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
   * @see #loadExercises
   * @see #pushNewSectionHistoryToken()
   */
  protected void noSectionsGetExercises(long userID) {
    super.getExercises(userID);
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
    return sectionWidgetContainer.getNumSelections();
  }
}
