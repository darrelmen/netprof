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

import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.amas.SingleSelectExerciseList;
import mitll.langtest.client.bootstrap.FlexSectionExerciseList;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.SectionWidget;
import mitll.langtest.client.flashcard.StatsFlashcardFactory;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.shared.ExerciseListWrapper;
import mitll.langtest.shared.exercise.CommonShell;
import mitll.langtest.shared.exercise.ExerciseListRequest;
import mitll.langtest.shared.exercise.Shell;

import java.util.Collection;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Created with IntelliJ IDEA.
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 11/6/13
 * Time: 3:21 PM
 * To change this template use File | Settings | File Templates.
 */
public class HistoryExerciseList<T extends CommonShell, U extends Shell, V extends SectionWidget>
    extends PagingExerciseList<T, U> implements ValueChangeHandler<String> {
  private Logger logger = Logger.getLogger("HistoryExerciseList");

  public static final String ANY = "Clear";

  private HandlerRegistration handlerRegistration;
  protected long userID;
  protected final SectionWidgetContainer<V> sectionWidgetContainer;

  protected static final boolean DEBUG_ON_VALUE_CHANGE = false;
  private static final boolean DEBUG = true;

  /**
   * @param currentExerciseVPanel
   * @param service
   * @param feedback
   * @param controller
   * @param showTypeAhead
   * @param instance
   * @param incorrectFirst
   * @param showFirstNotCompleted
   * @see FlexSectionExerciseList#FlexSectionExerciseList(Panel, Panel, LangTestDatabaseAsync, UserFeedback, ExerciseController, String, boolean, boolean)
   */
  protected HistoryExerciseList(Panel currentExerciseVPanel, LangTestDatabaseAsync service, UserFeedback feedback,
                                ExerciseController controller,
                                boolean showTypeAhead,
                                String instance,
                                boolean incorrectFirst,
                                boolean showFirstNotCompleted) {
    super(currentExerciseVPanel, service, feedback, null, controller, showTypeAhead, instance, incorrectFirst, showFirstNotCompleted);
//    logger.info("showFirstNotCompleted " + showFirstNotCompleted + " for " + instance + " : " + this.getElement().getId());
    sectionWidgetContainer = getSectionWidgetContainer();
    addHistoryListener();
  }

  protected SectionWidgetContainer<V> getSectionWidgetContainer() {
    return new SectionWidgetContainer<V>();
  }

  protected V getSectionWidget(String type) {
    return sectionWidgetContainer.getWidget(type);
  }

  /**
   * @param id
   * @return
   */
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
    if (logger == null) {
      logger = Logger.getLogger("HistoryExerciseList");
    }
//    logger.info("\tgetHistoryToken for " + id );
    String instanceSuffix = getInstance().isEmpty() ? "" : ";" + SelectionState.INSTANCE + "=" + getInstance();
    boolean hasItemID = id != null && id.length() > 0;

    String s = (hasItemID ?
        super.getHistoryTokenFromUIState(search, id) + ";" :
        "search=" + search + ";") +
        sectionWidgetContainer.getHistoryToken() +
        instanceSuffix;
    // logger.info("getHistoryTokenFromUIState '" + s + "'");
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
   * @see ExerciseList#loadFirstExercise(String)
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
      if (DEBUG)
        logger.info("pushFirstSelection : (" + getInstance() + ") pushNewItem " + exerciseID + " vs " + idFromToken);
      pushNewItem(searchIfAny, getValidExerciseID(exerciseID));
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
   * @param exerciseID
   * @see #loadExercisesUsingPrefix(Map, String, boolean, String, boolean, boolean)
   * @see #pushFirstSelection(String, String)
   * @see #pushNewItem(String, String)
   */
  private void checkAndAskOrFirst(String exerciseID) {
    String toUse = getValidExerciseID(exerciseID);
    if (hasExercise(toUse)) {
      //  logger.info("\tcheckAndAskOrFirst "+ exerciseID);
      checkAndAskServer(toUse);
    }
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
      SelectionState newState = getSelectionState(historyToken);
      loadExercisesUsingPrefix(
          newState.getTypeToSection(),
          selectionState.getSearch(),
          newState.isOnlyWithAudioDefects(),
          selectionState.getItem(),
          newState.isOnlyUnrecorded(),
          newState.isOnlyDefault());
    }
  }

  protected void setHistoryItem(String historyToken) {
    if (DEBUG) logger.info("HistoryExerciseList.setHistoryItem '" + historyToken + "' -------------- ");
    History.newItem(historyToken);
  }

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

  protected void ignoreStaleRequest(ExerciseListWrapper<T> result) {
    popRequest();
  }


  /**
   * Given a selectionState state, make sure the list boxes are consistent with it.
   *
   * @param selectionState
   * @see #onValueChange(com.google.gwt.event.logical.shared.ValueChangeEvent)
   */
  protected void restoreListBoxState(SelectionState selectionState) {
//    logger.info("restoreListBoxState restore " + selectionState);
    sectionWidgetContainer.restoreListBoxState(selectionState, controller.getStartupInfo().getTypeOrder());
  }

  /**
   * Respond to push a history token.
   *
   * @param event
   */
  @Override
  public void onValueChange(ValueChangeEvent<String> event) {
    // if (DEBUG_ON_VALUE_CHANGE) logger.info("HistoryExerciseList.onValueChange : ------ start ---- " + getInstance());
    SelectionState selectionState = getSelectionState(event.getValue());
//    logger.info("onValueChange " + event.getValue() + " sel " + selectionState);
    String instance1 = selectionState.getInstance();

    if (!instance1.equals(getInstance()) && instance1.length() > 0) {
      if (DEBUG_ON_VALUE_CHANGE) {
        logger.info("onValueChange : skipping event " + event.getValue() + " for instance '" + instance1 +
            "' that is not mine '" + getInstance() + "'");
      }
      if (getCreatedPanel() == null) {
        popRequest();
        noSectionsGetExercises(controller.getUser());
      }
      return;
    }
    if (DEBUG_ON_VALUE_CHANGE) {
      logger.info("HistoryExerciseList.onValueChange : originalValue '" + event.getValue() +
          "'" +
          " token is '" + event.getValue() + "' for " + instance1 + " vs my instance " + getInstance());
    }

    restoreUIState(selectionState);

    try {
      loadExercisesUsingPrefix(
          selectionState.getTypeToSection(),
          selectionState.getSearch(),
          selectionState.isOnlyWithAudioDefects(),
          selectionState.getItem(),
          selectionState.isOnlyUnrecorded(),
          selectionState.isOnlyDefault());
    } catch (Exception e) {
      logger.warning("HistoryExerciseList.onValueChange " + event.getValue() + " badly formed. Got " + e);
    }
  }

  /**
   * @param selectionState
   * @return true if we're just clicking on a different item in the list and don't need to reload the exercise list
   */
  protected void restoreUIState(SelectionState selectionState) {
    String search = selectionState.getSearch();
    restoreListBoxState(selectionState);
    if (DEBUG_ON_VALUE_CHANGE) {
      logger.info("HistoryExerciseList.onValueChange : selectionState '" + selectionState + "' search from token '" + search +
          "'");
    }

    // if (timeOfLastRequest >= getTimeOfLastKeyPress()) {
    //   logger.info("restoreUIState time of last " + new Date(timeOfLastRequest) + " > " + new Date(getTimeOfLastKeyPress()));
    setTypeAheadText(search);
    //  }
    //  else {
    //    logger.info("restoreUIState ----> key press is newer ");
    //  }
  }

  /**
   * When we get a history token push, select the exercise type, section, and optionally item.
   *
   * @see #gotTypeAheadEvent(String, boolean)
   */
  protected void loadExercises(String selectionState, String prefix,
                               boolean onlyWithAudioAnno,
                               boolean onlyUnrecorded,
                               boolean onlyDefaultUser) {
    Map<String, Collection<String>> typeToSection = getSelectionState(selectionState).getTypeToSection();
/*    logger.info("HistoryExerciseList.loadExercises : looking for " +
      "'" + prefix + "' (" + prefix.length() + " chars) in list id "+userListID + " instance " + getInstance());*/
    loadExercisesUsingPrefix(typeToSection, prefix, onlyWithAudioAnno, "", onlyUnrecorded, onlyDefaultUser);
  }

  /**
   * @param typeToSection
   * @param prefix
   * @param onlyWithAudioAnno
   * @param exerciseID
   * @param onlyUnrecorded
   * @param onlyDefaultUser
   * @see PagingExerciseList#loadExercises
   */
  protected void loadExercisesUsingPrefix(Map<String, Collection<String>> typeToSection,
                                          String prefix,
                                          boolean onlyWithAudioAnno,
                                          String exerciseID,
                                          boolean onlyUnrecorded,
                                          boolean onlyDefaultUser) {
    ExerciseListRequest request = getRequest(prefix)
        .setTypeToSelection(typeToSection)
        .setOnlyWithAudioAnno(onlyWithAudioAnno)
        .setOnlyUnrecordedByMe(onlyUnrecorded)
        .setOnlyDefaultAudio(onlyDefaultUser);

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
        logger.info(getClass() + " skipping request for current list" + "\n" + request + " vs\n" + lastSuccessfulRequest);
      }
      if (exerciseID != null) {
        checkAndAskOrFirst(exerciseID);
      } else {
        logger.warning("Not doing anything as response to " + request);
      }
    }
  }

  /**
   * @param typeToSection
   * @see StatsFlashcardFactory.StatsPracticePanel#doIncorrectFirst
   */
  @Override
  public void reload(Map<String, Collection<String>> typeToSection) {
    loadExercisesUsingPrefix(typeToSection, getTypeAheadText(), false, "", false, false);
  }

  /**
   * @param token
   * @return object representing type=value pairs from history token
   * @see PagingExerciseList#loadExercises
   * @see #onValueChange(com.google.gwt.event.logical.shared.ValueChangeEvent)
   */
  protected SelectionState getSelectionState(String token) {
    return new SelectionState(token, !allowPlusInURL);
  }

  /**
   * @param userID
   * @see PagingExerciseList#loadExercises
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
