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
import mitll.langtest.client.custom.INavigation;
import mitll.langtest.client.dialog.ExceptionHandlerDialog;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.flashcard.StatsFlashcardFactory;
import mitll.langtest.shared.exercise.CommonShell;
import mitll.langtest.shared.exercise.ExerciseListRequest;
import mitll.langtest.shared.exercise.ExerciseListWrapper;
import mitll.langtest.shared.exercise.Shell;
import mitll.langtest.shared.project.ProjectStartupInfo;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * TODO : all the history stuff is way too confusing - make it simpler!
 *
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

  private static final int DEFAULT_PROJECT_ID = 1;

  static final String SECTION_SEPARATOR = SelectionState.SECTION_SEPARATOR;
  private HandlerRegistration handlerRegistration;
  private final FacetContainer sectionWidgetContainer;

  private static final boolean DEBUG_ON_VALUE_CHANGE = true;
  private static final boolean DEBUG = true;
  private static final boolean DEBUG_PUSH = false;

  /**
   * @param currentExerciseVPanel
   * @param controller
   * @see FacetExerciseList#FacetExerciseList(Panel, Panel, ExerciseController, ListOptions, DivWidget, boolean, INavigation.VIEWS)
   */
  protected HistoryExerciseList(Panel currentExerciseVPanel,
                                ExerciseController controller,
                                ListOptions options) {
    super(currentExerciseVPanel, null, controller, options);
    sectionWidgetContainer = getSectionWidgetContainer();
    addHistoryListener();
  }

  protected abstract FacetContainer getSectionWidgetContainer();

  protected String getInitialHistoryToken() {
    //  logger.info("\tgetInitialHistoryToken ");
    return getHistoryTokenFromUIState(getTypeAheadText(), -1);
  }

  /**
   * @param search
   * @param id
   * @return
   * @see ExerciseList#pushNewItem
   * @see #pushNewSectionHistoryToken
   */
  protected String getHistoryTokenFromUIState(String search, int id) {
    //   String unitAndChapterSelection = sectionWidgetContainer.getHistoryToken();
    //logger.info("\tgetHistoryToken for " + id + " is '" +unitAndChapterSelection.toString() + "'");
    if (logger == null) {
      logger = Logger.getLogger("HistoryExerciseList");
    }

    if (DEBUG) logger.info(getInstance() + " : getHistoryTokenFromUIState for " + id + " and search '" + search + "'");

    boolean hasItemID = id != -1;
    String instanceSuffix =
        getInstance() == INavigation.VIEWS.NONE ? "" : SECTION_SEPARATOR + SelectionState.INSTANCE + "=" + getInstance();

    String s = (hasItemID ?
        super.getHistoryTokenFromUIState(search, id) :
        getSearchTerm(search)) +

        SECTION_SEPARATOR +
        sectionWidgetContainer.getHistoryToken() +

        SECTION_SEPARATOR +
        instanceSuffix;

    if (DEBUG_PUSH || true) logger.info("getHistoryTokenFromUIState '" + s + "'");

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

  protected ProjectStartupInfo getStartupInfo() {
    return controller.getProjectStartupInfo();
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
    logger.info("pushFirstSelection : (" + getInstance() + ") current token " + token);
    int exidFromToken = getIDFromToken(token);
/*    if (DEBUG) logger.info("ExerciseList.pushFirstSelection : current token '" + token + "' id from token '" + idFromToken +
        "' vs new exercise " + exerciseID + " instance " + getInstance());*/

    if (exidFromToken == exerciseID) {
      if (DEBUG)
        logger.info("pushFirstSelection : (" + getInstance() + ") " +
            "\n\tcurrent token " + token + " same as new " + exerciseID);
      checkAndAskOrFirst(exerciseID);
    } else {
      if (DEBUG)
        logger.info("pushFirstSelection : (" + getInstance() + ") " +
            "\n\tpushNewItem " + exerciseID + " vs " + exidFromToken);
      pushNewItem(searchIfAny, getValidExerciseID(exerciseID));
    }
  }

  /**
   * @param token
   * @return
   * @see #onValueChange(com.google.gwt.event.logical.shared.ValueChangeEvent)
   */
  private int getIDFromToken(String token) {
    // if (token.startsWith("#item=") || token.startsWith("item=")) {
    SelectionState selectionState = new SelectionState(token, !allowPlusInURL);
    if (selectionState.getView() != getInstance()) {
      if (DEBUG) logger.warning("getIDFromToken got history item for another instance '" + selectionState.getView()
          + "' vs me '" + getInstance() + "'");
      //   return -1;

      return selectionState.getItem();
    } else {
      int item = selectionState.getItem();
      // if (DEBUG) logger.info("got history item for instance '" + selectionState.getInstance() + " : '" + item+"'");
      return item;
    }
    // }
    // return -1;
  }

  /**
   * @param exerciseID
   * @see #loadExercisesUsingPrefix
   * @see ExerciseList#pushFirstSelection(int, String)
   * @see ExerciseList#pushNewItem(String, int)
   */
  private void checkAndAskOrFirst(int exerciseID) {
    int toUse = getValidExerciseID(exerciseID);
    if (hasExercise(toUse)) {
      // logger.info("\tcheckAndAskOrFirst " + exerciseID);
      checkAndAskServer(toUse);
    } else {
      logger.warning("checkAndAskOrFirst no exercise " + exerciseID);
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
   * @see #loadExercise
   * @see #pushFirstSelection
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
        logger.info(getInstance() + " HistoryExerciseList.pushNewItem : current" +
            "\n\t currentToken '" + currentToken + "' different menu state '" + historyToken + "' from new " + exerciseID);
      }
      setHistoryItem(historyToken);
    }
  }

  private String getTrimmedToken(String historyToken) {
    return historyToken.length() > 2 ? historyToken.substring(0, historyToken.length() - 2) : historyToken;
  }

  /**
   * @see FacetExerciseList#gotSelection
   */
  protected void pushNewSectionHistoryToken() {
    String currentToken = getHistoryToken();
    SelectionState selectionState = getSelectionState(currentToken);
    String historyToken = getHistoryTokenFromUIState(getTypeAheadText(), selectionState.getItem());

    if (DEBUG_PUSH) {
      logger.info("pushNewSectionHistoryToken " +
          "\n\tcurrent   " + currentToken +
          "\n\tselection " + selectionState +
          "\n\titem      " + selectionState.getItem() +
          "\n\tnew token " + historyToken
      );
    }

    if (currentToken.equals(historyToken)) {
      if (isEmpty() || historyToken.isEmpty()) {
        if (DEBUG) logger.info("pushNewSectionHistoryToken : calling noSectionsGetExercises for" +
            "\n\ttoken '" + historyToken +
            "' " + "\n\tcurrent has " + getSize() + " instance " + getInstance());

        noSectionsGetExercises(selectionState.getItem());
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

  /**
   * @param historyToken
   * @see #pushNewItem
   */
  void setHistoryItem(String historyToken) {
    String token = History.getToken();
  logger.info("before " + token);
    if (DEBUG_PUSH) {
      logger.info("HistoryExerciseList.setHistoryItem '" + historyToken + "' -------------- ");
    }
    History.newItem(historyToken);
  }

  /**
   * @param selectionState
   * @param newState
   */
  private void loadFromSelectionState(SelectionState selectionState, SelectionState newState) {
    if (DEBUG) logger.info("loadFromSelectionState" +
        "\n\told state " + selectionState.getInfo() +
        "\n\tnew state " + newState.getInfo());


    loadExercisesUsingPrefix(
        newState.getTypeToSection(),
        selectionState.getSearch(),
        selectionState.getItem(),

        newState.isOnlyWithAudioDefects(),
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
    ProjectStartupInfo projectStartupInfo = controller.getProjectStartupInfo();
    if (projectStartupInfo != null) {
      if (sectionWidgetContainer != null) {
        sectionWidgetContainer.restoreListBoxState(selectionState, getTypeOrderSimple());
      }
    }
  }

  List<String> getTypeOrderSimple() {
    return getStartupInfo().getTypeOrder();
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
    if (DEBUG_ON_VALUE_CHANGE) logger.info("HistoryExerciseList.onValueChange : ------ start ---- " + getInstance());
    if (controller.getProjectStartupInfo() == null) {
      logger.warning("onValueChange skipping change event since no project");
      return;
    }

    String value = event.getValue();
    SelectionState selectionState = getSelectionState(value);

    maybeSwitchProject(selectionState, controller.getProjectStartupInfo().getProjectid());

    if (DEBUG_ON_VALUE_CHANGE) {
      logger.info("onValueChange got '" + value + "' sel '" + selectionState + "' '" + selectionState.getInfo() +"'");
    }
    INavigation.VIEWS instance1 = selectionState.getView();

    if (instance1 != getInstance()) {
      if (DEBUG_ON_VALUE_CHANGE) {
        logger.info("onValueChange : MAYBE skipping event " + value + " for instance '" + instance1 +
            "' that is not mine '" + getInstance() + "'");
      }

    /*  if (getCreatedPanel() == null) {
        popRequest();
        noSectionsGetExercises(selectionState.getItem());
      }
      return;*/
    }
    if (DEBUG_ON_VALUE_CHANGE) {
      logger.info("HistoryExerciseList.onValueChange : " +
          "\n\toriginalValue '" + value +          "'" +
         // " token is '" + value + "' " +
          "for " + instance1 + " vs my instance " + getInstance());
    }

    restoreUIState(selectionState);

    try {
      loadFromSelectionState(selectionState, selectionState);
    } catch (Exception e) {
      logger.warning("HistoryExerciseList.selectionStateChanged " + value + " badly formed. Got " + e);
      String exceptionAsString = ExceptionHandlerDialog.getExceptionAsString(e);
      logger.info("HistoryExerciseList.selectionStateChanged logException stack " + exceptionAsString);
    }
  }

  void maybeSwitchProject(SelectionState selectionState, int currentProject) {
    int project = selectionState.getProject();
    //int currentProject = projectStartupInfo.getProjectid();
    //   logger.info("maybeSwitchProject project " + project + " vs " + currentProject);
    if (project != currentProject) {
      if (project > DEFAULT_PROJECT_ID) {
        logger.info("onValueChange project from state " + project + " != " + currentProject);
        projectChangedTo(project);
      }
    }
  }

  void projectChangedTo(int project) {
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

    // String search = selectionState.getSearch();
    // logger.info("restoreUIState search box should be "+search);
    setTypeAheadText(selectionState.getSearch());
  }

  void simpleLoadExercises(String selectionState, String prefix, int exerciseID) {
    loadExercises(selectionState, prefix, false, false, false, exerciseID);
  }

  /**
   * When we get a history token push, select the exercise type, section, and optionally item.
   *
   * @see #simpleLoadExercises
   */
  private void loadExercises(String selectionState,
                             String prefix,

                             boolean onlyWithAudioAnno,
                             boolean onlyDefaultUser,
                             boolean onlyUninspected,
                             int exerciseID) {
    Map<String, Collection<String>> typeToSection = getSelectionState(selectionState).getTypeToSection();
    if (DEBUG) logger.info("HistoryExerciseList.loadExercises : looking for " +
        "'" + prefix + "' (" + prefix.length() + " chars) in list id " + userListID + " instance " + getInstance());
    loadExercisesUsingPrefix(typeToSection, prefix, exerciseID, onlyWithAudioAnno, onlyDefaultUser, onlyUninspected);
  }

  /**
   * TODO : gah - why so complicated??? replace with request
   *
   * @param typeToSection
   * @param prefix
   * @param exerciseID
   * @param onlyWithAudioAnno
   * @param onlyDefaultUser
   * @param onlyUninspected
   * @see HistoryExerciseList#loadExercises
   * @see ExerciseList.SetExercisesCallback#onSuccess
   */
  protected void loadExercisesUsingPrefix(Map<String, Collection<String>> typeToSection,
                                          String prefix,
                                          int exerciseID,

                                          boolean onlyWithAudioAnno,
                                          boolean onlyDefaultUser,
                                          boolean onlyUninspected) {
    ExerciseListRequest request =
        getExerciseListRequest(typeToSection, prefix, onlyWithAudioAnno, onlyDefaultUser, onlyUninspected);

    if (DEBUG) {
      logger.info("loadExercisesUsingPrefix got" +
          "\n\ttype->section " + typeToSection +
          "\n\tprefix        " + prefix +
          "\n\trequest       " + request +
          "\n\tlast          " + lastSuccessfulRequest);
    }

    if (lastSuccessfulRequest == null || !request.sameAs(lastSuccessfulRequest)) {
      try {
        if (DEBUG) {
          logger.info("loadExercisesUsingPrefix looking for '" + prefix +
              "' (" + prefix.length() + " chars) in context of " + typeToSection + " list " + userListID +
              " instance " + getInstance()
          );
        }
        getExerciseIDs(typeToSection, prefix, exerciseID, request);
      } catch (Exception e) {
        logger.warning("loadExercisesUsingPrefix got " + e);
      }
    } else {
      if (DEBUG) {
        logger.info(getClass() + " skipping request for current list" + "\n" + request + " vs\n" + lastSuccessfulRequest);
      }
      if (exerciseID != -1) {
        checkAndAskOrFirst(exerciseID);
      } else {
        if (DEBUG)
          logger.info("loadExercisesUsingPrefix Not doing anything as response to request " + request + "\n\tfor exercise " + exerciseID);
      }
    }
  }

  public SelectionState getSelectionState() {
    return getSelectionState(getHistoryTokenFromUIState(getTypeAheadText(), -1));
  }

  protected ExerciseListRequest getExerciseListRequest(Map<String, Collection<String>> typeToSection,
                                                       String prefix,
                                                       boolean onlyWithAudioAnno,
                                                       boolean onlyDefaultUser,
                                                       boolean onlyUninspected) {
    return getExerciseListRequest(prefix)
        .setTypeToSelection(typeToSection)
        .setOnlyWithAudioAnno(onlyWithAudioAnno)
        .setOnlyDefaultAudio(onlyDefaultUser)
        .setOnlyUninspected(onlyUninspected);
  }

  /**
   * @param typeToSection
   * @param prefix
   * @param exerciseID
   * @param request
   * @see #loadExercisesUsingPrefix(Map, String, int, boolean, boolean, boolean)
   */
  protected void getExerciseIDs(Map<String, Collection<String>> typeToSection,
                                String prefix,
                                int exerciseID,
                                ExerciseListRequest request) {
    waitCursorHelper.scheduleWaitTimer();
    if (DEBUG) {
      logger.info("getExerciseIDs for '" + prefix + "' and " + exerciseID +
          "\n\tfor " + request);

//      String exceptionAsString = ExceptionHandlerDialog.getExceptionAsString(new Exception());
//      logger.info("logException stack " + exceptionAsString);
    }
    if (controller.getUser() > 0) {
      // final long then = System.currentTimeMillis();
      service.getExerciseIds(
          request,
          new SetExercisesCallback(userListID + "_" + typeToSection.toString(), prefix, exerciseID, request));
    }
  }

  /**
   * @param typeToSection
   * @see StatsFlashcardFactory#reload
   */
  @Override
  public void reload(Map<String, Collection<String>> typeToSection) {
    loadExercisesUsingPrefix(typeToSection, getTypeAheadText(), -1, false, false, false);
  }

  /**
   * @param token
   * @return object representing type=value pairs from history token
   * @see #loadExercises
   * @see #onValueChange
   */
  SelectionState getSelectionState(String token) {
    return new SelectionState(token, !allowPlusInURL);
  }

  /**
   * @param exerciseID
   * @see #onValueChange
   * @see #pushNewSectionHistoryToken
   */
  protected void noSectionsGetExercises(int exerciseID) {
    // logger.info("noSectionsGetExercises " +userID);
    super.getExercises();
  }

  /**
   * Make sure all sections have a selection - quiz, test type, ilr level
   *
   * @return
   */
  protected int getNumSelections() {
    return sectionWidgetContainer.getNumSelections();
  }
}
