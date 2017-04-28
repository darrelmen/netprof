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

import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.amas.SingleSelectExerciseList;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.flashcard.StatsFlashcardFactory;
import mitll.langtest.shared.exercise.CommonShell;
import mitll.langtest.shared.exercise.ExerciseListRequest;
import mitll.langtest.shared.exercise.ExerciseListWrapper;
import mitll.langtest.shared.exercise.Shell;
import scala.tools.cmd.gen.AnyVals;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static mitll.langtest.client.list.FacetExerciseList.LISTS;

/**
 * Created with IntelliJ IDEA.
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 11/6/13
 * Time: 3:21 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class HistoryExerciseList<T extends CommonShell, U extends Shell>
    extends PagingExerciseList<T, U>
    implements ValueChangeHandler<String> {
  private Logger logger = Logger.getLogger("HistoryExerciseList");

  static final String SECTION_SEPARATOR = SelectionState.SECTION_SEPARATOR;
  private HandlerRegistration handlerRegistration;
  protected long userID;
  private final FacetContainer sectionWidgetContainer;

  protected static final boolean DEBUG_ON_VALUE_CHANGE = true;
  private static final boolean DEBUG = true;
  private static final boolean DEBUG_PUSH = false;

  /**
   * @param currentExerciseVPanel
   * @param controller
   * @see FacetExerciseList#FacetExerciseList(Panel, Panel, ExerciseController, ListOptions, DivWidget, DivWidget, int)
   */
  protected HistoryExerciseList(Panel currentExerciseVPanel,
                                ExerciseController controller,
                                ListOptions options) {
    super(currentExerciseVPanel, null, controller, options);
    sectionWidgetContainer = getSectionWidgetContainer();
    addHistoryListener();
  }

  protected abstract FacetContainer getSectionWidgetContainer();

  protected String getHistoryToken(int id) {
    logger.info("\tgetHistoryToken " + id);
    return getHistoryTokenFromUIState(getTypeAheadText(), id);
  }

  protected String getInitialHistoryToken() {
    logger.info("\tgetInitialHistoryToken ");
    return getHistoryTokenFromUIState(getTypeAheadText(), -1);
  }

  /**
   * @param search
   * @param id
   * @return
   * @see ExerciseList#pushNewItem
   * @see #pushNewSectionHistoryToken()
   */
  protected String getHistoryTokenFromUIState(String search, int id) {
    //   String unitAndChapterSelection = sectionWidgetContainer.getHistoryToken();
    //logger.info("\tgetHistoryToken for " + id + " is '" +unitAndChapterSelection.toString() + "'");
    if (logger == null) {
      logger = Logger.getLogger("HistoryExerciseList");
    }

    logger.info(getInstance() + " : getHistoryTokenFromUIState for " + id + " and search '" + search + "'");

    boolean hasItemID = id != -1;
    String instanceSuffix = getInstance().isEmpty() ? "" : SECTION_SEPARATOR + SelectionState.INSTANCE + "=" + getInstance();

    String s = (hasItemID ?
        super.getHistoryTokenFromUIState(search, id) :
        "search=" + search) + SECTION_SEPARATOR +
        sectionWidgetContainer.getHistoryToken() + SECTION_SEPARATOR +
        instanceSuffix;

    if (DEBUG_PUSH) logger.info("getHistoryTokenFromUIState '" + s + "'");

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
  void pushFirstSelection(int exerciseID, String searchIfAny) {
    String token = getHistoryToken();
    int exidFromToken = getIDFromToken(token);
/*    if (DEBUG) logger.info("ExerciseList.pushFirstSelection : current token '" + token + "' id from token '" + idFromToken +
        "' vs new exercise " + exerciseID + " instance " + getInstance());*/

    if (exidFromToken == exerciseID) {
      if (DEBUG)
        logger.info("pushFirstSelection : (" + getInstance() + ") current token " + token + " same as new " + exerciseID);
      checkAndAskOrFirst(exerciseID);
    } else {
      if (DEBUG)
        logger.info("pushFirstSelection : (" + getInstance() + ") pushNewItem " + exerciseID + " vs " + exidFromToken);
      pushNewItem(searchIfAny, getValidExerciseID(exerciseID));
    }
  }

  /**
   * @param token
   * @return
   * @see #onValueChange(com.google.gwt.event.logical.shared.ValueChangeEvent)
   */
  private int getIDFromToken(String token) {
    if (token.startsWith("#item=") || token.startsWith("item=")) {
      SelectionState selectionState = new SelectionState(token, !allowPlusInURL);
      if (!selectionState.getInstance().equals(getInstance())) {
        //if (DEBUG) logger.info("got history item for another instance '" + selectionState.getInstance() + "' vs me '" + instance +"'");
      } else {
        int item = selectionState.getItem();
        // if (DEBUG) logger.info("got history item for instance '" + selectionState.getInstance() + " : '" + item+"'");
        return item;
      }
    }
    return -1;
  }

  /**
   * @param exerciseID
   * @see #loadExercisesUsingPrefix
   * @see ExerciseList#pushFirstSelection(int, String)
   * @see ExerciseList#pushNewItem(String, int)
   */
  protected void checkAndAskOrFirst(int exerciseID) {
    int toUse = getValidExerciseID(exerciseID);
    if (hasExercise(toUse)) {
      //   logger.info("\tcheckAndAskOrFirst "+ exerciseID);
      checkAndAskServer(toUse);
    }
  }

  /**
   * Fall back to first if invalid id and list not empty.
   *
   * @param exerciseID
   * @return
   */
  private int getValidExerciseID(int exerciseID) {
    return hasExercise(exerciseID) ? exerciseID : isEmpty() ? -1 : getFirstID();
  }

  /**
   * @param search
   * @param exerciseID
   * @see Reloadable#loadExercise(int)
   * @see ExerciseList#pushFirstSelection(int, String)
   * @see PagingExerciseList#gotClickOnItem(CommonShell)
   */
  void pushNewItem(String search, int exerciseID) {
    if (DEBUG_PUSH) {
      logger.info(getInstance() + " HistoryExerciseList.pushNewItem : search '" + search + "' : item '" + exerciseID + "'");
    }
    String historyToken = getHistoryTokenFromUIState(search, exerciseID);
    String trimmedToken = getTrimmedToken(historyToken);
    if (DEBUG_PUSH) {
      logger.info(getInstance() + " HistoryExerciseList.pushNewItem : push history '" + historyToken + "' search '" + search + "' : " + exerciseID);
    }

    String currentToken = getHistoryToken();
//    if (DEBUG)
//      logger.info("HistoryExerciseList.pushNewItem : current currentToken '" + currentToken + "' vs new id '" + exerciseID + "'");
    if (currentToken != null && (historyToken.equals(currentToken) || trimmedToken.equals(currentToken))) {
      if (DEBUG_PUSH)
        logger.info(getInstance() + " HistoryExerciseList.pushNewItem : current currentToken '" + currentToken + "' same as new " + historyToken);
      checkAndAskOrFirst(exerciseID);
    } else {
      if (DEBUG_PUSH) {
        logger.info(getInstance() + " HistoryExerciseList.pushNewItem : current currentToken '" + currentToken + "' different menu state '" + historyToken + "' from new " + exerciseID);
      }
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
   * @see SimpleSelectExerciseList#addChoiceRow
   */
  void pushFirstListBoxSelection() {
    if (getHistoryToken().isEmpty()) {
      logger.info("pushFirstListBoxSelection : history token is blank " + getInstance());
      pushNewSectionHistoryToken();
    } else {
      logger.info("pushFirstListBoxSelection fire history for token from URL: " + getHistoryToken() + " instance " + getInstance());
      History.fireCurrentHistoryState();
    }
  }

  /**
   * @seex mitll.langtest.client.bootstrap.FlexSectionExerciseList#addClickHandlerToButton
   * @see #pushFirstListBoxSelection
   */
  protected void pushNewSectionHistoryToken() {
    String currentToken = getHistoryToken();
    SelectionState selectionState = getSelectionState(currentToken);
    if (DEBUG_PUSH) {
      logger.info("pushNewSectionHistoryToken " + currentToken + " sel " + selectionState + " item " + selectionState.getItem());
    }

    String historyToken = getHistoryTokenFromUIState(getTypeAheadText(), selectionState.getItem());

    if (currentToken.equals(historyToken)) {
      if (isEmpty() || historyToken.isEmpty()) {
        if (DEBUG) logger.info("pushNewSectionHistoryToken : calling noSectionsGetExercises for" +
            "\n\ttoken '" + historyToken +
            "' " + "\n\tcurrent has " + getSize() + " instance " + getInstance());

        noSectionsGetExercises(userID);
      } else {
        logger.info("pushNewSectionHistoryToken : skipping same token '" + historyToken + "'" + " instance " + getInstance());
      }
    } else {
      if (DEBUG)
        logger.info("pushNewSectionHistoryToken : currentToken " + currentToken + " instance " + getInstance());
      loadFromSelectionState(selectionState, getSelectionState(historyToken));
    }
  }

  protected String getHistoryToken() {
    return History.getToken();
  }

  protected void setHistoryItem(String historyToken) {
    if (DEBUG_PUSH) logger.info("HistoryExerciseList.setHistoryItem '" + historyToken + "' -------------- ");
    History.newItem(historyToken);
  }

  /**
   * For when we want to reload the list, but not by pushing state onto the URL.
   *
   * @see mitll.langtest.client.custom.MarkDefectsChapterNPFHelper#addEventHandler
   */
  public void reloadFromState() {
    SelectionState selectionState = getSelectionState(getHistoryToken());
    String typeAheadText = getTypeAheadText();

    ExerciseListRequest request =
        getExerciseListRequest(selectionState.getTypeToSection(),
            typeAheadText,
            selectionState.isOnlyWithAudioDefects(),
            selectionState.isOnlyUnrecorded(),
            selectionState.isOnlyDefault(),
            selectionState.isOnlyUninspected());
    getExerciseIDs(selectionState.getTypeToSection(),
        typeAheadText,
        -1,
        request);
  }

  protected void loadFromSelectionState(SelectionState selectionState, SelectionState newState) {
    logger.info("loadFromSelectionState old state " + selectionState.getInfo() + " new state " + newState.getInfo());
    loadExercisesUsingPrefix(
        newState.getTypeToSection(),
        selectionState.getSearch(),
        selectionState.getItem(),

        newState.isOnlyWithAudioDefects(),
        newState.isOnlyUnrecorded(),
        newState.isOnlyDefault(),
        newState.isOnlyUninspected());
  }


  /**
   * @param e
   * @see mitll.langtest.client.list.PagingExerciseList#makePagingContainer()
   */
  @Override
  public void gotClickOnItem(T e) {
    loadByID(e.getID());
  }

  public void loadExercise(int itemID) {
    pushNewItem(getTypeAheadText(), itemID);
  }

  protected void ignoreStaleRequest(ExerciseListWrapper<T> result) {
    popRequest();
  }

  /**
   * Given a selectionState state, make sure the list boxes are consistent with it.
   *
   * @param selectionState
   * @see #onValueChange
   * @see #restoreUIState
   */
  protected void restoreListBoxState(SelectionState selectionState) {
    if (DEBUG) logger.info("restoreListBoxState restore '" + selectionState + "'");
    List<String> typeOrder = controller.getProjectStartupInfo().getTypeOrder();
    List<String> added = new ArrayList<>(typeOrder);
    added.add(LISTS);
    sectionWidgetContainer.restoreListBoxState(selectionState, typeOrder);
  }

  /**
   * Respond to push of a history token.
   *
   * @param event
   * @see #pushNewItem(String, int)
   * @see #setHistoryItem(String)
   */
  @Override
  public void onValueChange(ValueChangeEvent<String> event) {
    // if (DEBUG_ON_VALUE_CHANGE) logger.info("HistoryExerciseList.onValueChange : ------ start ---- " + getInstance());
    String value = event.getValue();
    selectionStateChanged(value);
  }

  protected void selectionStateChanged(String value) {
    SelectionState selectionState = getSelectionState(value);
    logger.info("selectionStateChanged got " + value + " sel " + selectionState + " " + selectionState.getInfo());
    String instance1 = selectionState.getInstance();

    if (!instance1.equals(getInstance()) && instance1.length() > 0) {
      if (DEBUG_ON_VALUE_CHANGE) {
        logger.info("selectionStateChanged : skipping event " + value + " for instance '" + instance1 +
            "' that is not mine '" + getInstance() + "'");
      }
      if (getCreatedPanel() == null) {
        popRequest();
        noSectionsGetExercises(controller.getUserState().getUser());
      }
      return;
    }
    if (DEBUG_ON_VALUE_CHANGE) {
      logger.info("HistoryExerciseList.selectionStateChanged : originalValue '" + value +
          "'" +
          " token is '" + value + "' for " + instance1 + " vs my instance " + getInstance());
    }

    restoreUIState(selectionState);

    try {
      loadFromSelectionState(selectionState, selectionState);
    } catch (Exception e) {
      logger.warning("HistoryExerciseList.selectionStateChanged " + value + " badly formed. Got " + e);
    }
  }

  /**
   * @param selectionState
   * @return true if we're just clicking on a different item in the list and don't need to reload the exercise list
   */
  protected void restoreUIState(SelectionState selectionState) {
    restoreListBoxState(selectionState);
    if (DEBUG_ON_VALUE_CHANGE) {
      logger.info("HistoryExerciseList.restoreUIState : selectionState '" + selectionState +
          "' search from token '" + selectionState.getSearch() +
          "'");
    }

    // if (timeOfLastRequest >= getTimeOfLastKeyPress()) {
    //   logger.info("restoreUIState time of last " + new Date(timeOfLastRequest) + " > " + new Date(getTimeOfLastKeyPress()));
    setTypeAheadText(selectionState.getSearch());
    //  }
    //  else {
    //    logger.info("restoreUIState ----> key press is newer ");
    //  }
  }

  protected void simpleLoadExercises(String selectionState, String prefix) {
    loadExercises(selectionState, prefix, false, false, false, false);
  }

  /**
   * When we get a history token push, select the exercise type, section, and optionally item.
   *
   * @see #gotTypeAheadEvent(String, boolean)
   */
  protected void loadExercises(String selectionState,
                               String prefix,

                               boolean onlyWithAudioAnno,
                               boolean onlyUnrecorded,
                               boolean onlyDefaultUser,
                               boolean onlyUninspected) {
    Map<String, Collection<String>> typeToSection = getSelectionState(selectionState).getTypeToSection();
    logger.info("HistoryExerciseList.loadExercises : looking for " +
        "'" + prefix + "' (" + prefix.length() + " chars) in list id " + userListID + " instance " + getInstance());
    loadExercisesUsingPrefix(typeToSection, prefix, -1, onlyWithAudioAnno, onlyUnrecorded, onlyDefaultUser, onlyUninspected);
  }

  /**
   * TODO : gah - why so complicated??? replace with request
   *
   * @param typeToSection
   * @param prefix
   * @param exerciseID
   * @param onlyWithAudioAnno
   * @param onlyUnrecorded
   * @param onlyDefaultUser
   * @param onlyUninspected
   * @see PagingExerciseList#loadExercises
   * @see ExerciseList.SetExercisesCallback#onSuccess
   */
  protected void loadExercisesUsingPrefix(Map<String, Collection<String>> typeToSection,
                                          String prefix,
                                          int exerciseID,

                                          boolean onlyWithAudioAnno,
                                          boolean onlyUnrecorded,
                                          boolean onlyDefaultUser,
                                          boolean onlyUninspected) {
    ExerciseListRequest request =
        getExerciseListRequest(typeToSection, prefix, onlyWithAudioAnno, onlyUnrecorded, onlyDefaultUser, onlyUninspected);

    logger.info("loadExercisesUsingPrefix got " + typeToSection + " prefix " + prefix + " and made " + request);

    if (lastSuccessfulRequest == null || !request.sameAs(lastSuccessfulRequest)) {
      try {
        if (DEBUG) {
          logger.info("HistoryExerciseList.loadExercisesUsingPrefix looking for '" + prefix +
              "' (" + prefix.length() + " chars) in context of " + typeToSection + " list " + userListID +
              " instance " + getInstance()
          );
        }
        getExerciseIDs(typeToSection, prefix, exerciseID, request);
      } catch (Exception e) {
        logger.warning("got " + e);
      }
    } else {
      if (DEBUG) {
        logger.info(getClass() + " skipping request for current list" + "\n" + request + " vs\n" + lastSuccessfulRequest);
      }
      if (exerciseID != -1) {
        checkAndAskOrFirst(exerciseID);
      } else {
        logger.warning("loadExercisesUsingPrefix Not doing anything as response to request " + request + "\n\tfor exercise " + exerciseID);
      }
    }
  }

  public SelectionState getSelectionState() {
    return getSelectionState(getHistoryTokenFromUIState(getTypeAheadText(), -1));
  }

  protected ExerciseListRequest getExerciseListRequest(Map<String, Collection<String>> typeToSection, String prefix,
                                                     boolean onlyWithAudioAnno, boolean onlyUnrecorded,
                                                     boolean onlyDefaultUser, boolean onlyUninspected) {
    return getRequest(prefix)
        .setTypeToSelection(typeToSection)
        .setOnlyWithAudioAnno(onlyWithAudioAnno)
        .setOnlyUnrecordedByMe(onlyUnrecorded)
        .setOnlyDefaultAudio(onlyDefaultUser)
        .setOnlyUninspected(onlyUninspected);
  }

  /**
   * @param typeToSection
   * @param prefix
   * @param exerciseID
   * @param request
   * @see #loadExercisesUsingPrefix(Map, String, int, boolean, boolean, boolean, boolean)
   */
  private void getExerciseIDs(Map<String, Collection<String>> typeToSection,
                              String prefix,
                              int exerciseID,
                              ExerciseListRequest request) {
    waitCursorHelper.scheduleWaitTimer();
    if (DEBUG) {
      logger.info("getExerciseIDs for '" + prefix + "' and " + exerciseID);
      logger.info("getExerciseIDs for '" + request);
    }
    service.getExerciseIds(
        request,
        new SetExercisesCallback(userListID + "_" + typeToSection.toString(), prefix, exerciseID, request));
  }

  /**
   * @param typeToSection
   * @see StatsFlashcardFactory.StatsPracticePanel#doIncorrectFirst
   */
  @Override
  public void reload(Map<String, Collection<String>> typeToSection) {
    loadExercisesUsingPrefix(typeToSection, getTypeAheadText(), -1, false, false, false, false);
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
   * @see #pushNewSectionHistoryToken
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
