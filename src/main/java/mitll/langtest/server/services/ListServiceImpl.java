/*
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2019. Other request for this document shall be referred
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
 * © 2015-2019 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 */

package mitll.langtest.server.services;

import com.github.gwtbootstrap.client.ui.base.DivWidget;
import mitll.langtest.client.analysis.UserContainer;
import mitll.langtest.client.custom.ContentView;
import mitll.langtest.client.custom.userlist.ListContainer;
import mitll.langtest.client.services.ListService;
import mitll.langtest.server.database.custom.IUserListManager;
import mitll.langtest.server.database.exercise.BulkImport;
import mitll.langtest.server.database.exercise.Project;
import mitll.langtest.server.domino.AudioCopy;
import mitll.langtest.shared.common.DominoSessionException;
import mitll.langtest.shared.custom.*;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.CommonShell;
import mitll.langtest.shared.exercise.Exercise;
import mitll.langtest.shared.exercise.HasID;
import mitll.langtest.shared.project.Language;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

@SuppressWarnings("serial")
public class ListServiceImpl extends MyRemoteServiceServlet implements ListService {
  private static final Logger logger = LogManager.getLogger(ListServiceImpl.class);

  private static final boolean DEBUG = false;
  //private static final boolean DEBUG_IMPORT = false;

  /**
   * @return
   * @throws DominoSessionException
   * @see ContentView#showContent
   */
  @Override
  public Collection<UserList<CommonShell>> getLists() throws DominoSessionException {
    int userIDFromSession = getUserIDFromSessionOrDB();
    return getUserListManager().getUserListDAO().getAllPublicNotMine(userIDFromSession, getProjectIDFromUser(userIDFromSession));
  }

  /**
   * @param name
   * @param description
   * @param dliClass
   * @param isPublic
   * @param listType
   * @param quizSpec
   * @return
   * @see mitll.langtest.client.custom.dialog.CreateListDialog#doCreate
   */
  @Override
  public UserList addUserList(String name, String description, String dliClass, boolean isPublic,
                              UserList.LIST_TYPE listType,
                              int size,
                              QuizSpec quizSpec, Map<String, String> unitChapter) throws DominoSessionException {
    int userIDFromSessionOrDB = getUserIDFromSessionOrDB();
    IUserListManager userListManager = getUserListManager();
    int projectIDFromUser = getProjectIDFromUser(userIDFromSessionOrDB);

    return listType == UserList.LIST_TYPE.NORMAL ?
        userListManager.addUserList(userIDFromSessionOrDB, name, description, dliClass, isPublic, projectIDFromUser) :
        userListManager.addQuiz(userIDFromSessionOrDB, name, description, dliClass, isPublic, projectIDFromUser, size, quizSpec, unitChapter);
  }

  /**
   * @param userList
   * @throws DominoSessionException
   * @see mitll.langtest.client.custom.dialog.CreateListDialog#doEdit(UserList, ListContainer)
   */
  @Override
  public void update(UserList userList) throws DominoSessionException {
    getUserIDFromSessionOrDB();
    getUserListManager().update(userList);
  }

  /**
   * @param id
   * @return
   */
  @Override
  public boolean deleteList(int id) throws DominoSessionException {
    getUserIDFromSessionOrDB();
    return getUserListManager().deleteList(id);
  }

  /**
   * @param listid
   * @param exid
   * @return
   * @see mitll.langtest.client.custom.dialog.EditableExerciseList#deleteItem
   */
  @Override
  public boolean deleteItemFromList(int listid, int exid) throws DominoSessionException {
    getUserIDFromSessionOrDB();
    return getUserListManager().deleteItemFromList(listid, exid);
  }

  /**
   * @param userListID
   * @param user
   * @see mitll.langtest.client.list.FacetExerciseList#addVisitor
   */
  public UserList addVisitor(int userListID, int user) throws DominoSessionException {
    getUserIDFromSessionOrDB();
    return getUserListManager().addVisitor(userListID, user);
  }

  public void removeVisitor(int userListID, int user) throws DominoSessionException {
    getUserIDFromSessionOrDB();
    getUserListManager().removeVisitor(userListID, user);
  }

  /**
   * @param onlyCreated
   * @param visited
   * @param includeQuiz
   * @return
   * @see mitll.langtest.client.custom.userlist.ListView#addYourLists(DivWidget)
   */
  public Collection<UserList<CommonShell>> getListsForUser(boolean onlyCreated, boolean visited, boolean includeQuiz) throws DominoSessionException {
    //  if (!onlyCreated && !visited) logger.error("getListsForUser huh? asking for neither your lists nor  your visited lists.");
    int userIDFromSessionOrDB = getUserIDFromSessionOrDB();
    long then = System.currentTimeMillis();
    Collection<UserList<CommonShell>> listsForUser = getUserListManager()
        .getListsForUser(userIDFromSessionOrDB, getProjectIDFromUser(userIDFromSessionOrDB), onlyCreated, visited, includeQuiz);

    long now = System.currentTimeMillis();

    if (now - then > 30) {
      logger.info("getListsForUser : took " + (now - then) + " to get " + listsForUser.size() + " lists for user " + userIDFromSessionOrDB);
    }
    return listsForUser;
  }

  public Collection<IUserListWithIDs> getListsWithIDsForUser(boolean onlyCreated, boolean visited) throws DominoSessionException {
    //  if (!onlyCreated && !visited) logger.error("getListsForUser huh? asking for neither your lists nor  your visited lists.");
    int userIDFromSessionOrDB = getUserIDFromSessionOrDB();
    long then = System.currentTimeMillis();
    Collection<IUserListWithIDs> listsForUser = getUserListManager()
        .getListsWithIdsForUser(userIDFromSessionOrDB, getProjectIDFromUser(userIDFromSessionOrDB), onlyCreated, visited);

    long now = System.currentTimeMillis();

    logger.info("getListsWithIDsForUser took " + (now - then) + " to get " + listsForUser.size() + " lists for user " + userIDFromSessionOrDB);
    return listsForUser;
  }

  @Override
  public int getNumOnList(int listid) {
    return getUserListManager().getNumOnList(listid);
  }

  /**
   * TODO : consider not doing it separately from other facets.
   *
   * @param onlyCreated only relevant for NORMAL lists
   * @param visited     only relevant for NORMAL lists
   * @param list_type
   * @return
   * @see mitll.langtest.client.list.ListFacetHelper#populateListChoices
   * @see mitll.langtest.client.banner.QuizChoiceHelper#getQuizIntro
   */
  @Override
  public Collection<IUserList> getSimpleListsForUser(boolean onlyCreated,
                                                     boolean visited,
                                                     UserList.LIST_TYPE list_type) throws DominoSessionException {
    int userIDFromSessionOrDB = getUserIDFromSessionOrDB();
    long then = System.currentTimeMillis();
    int projectIDFromUser = getProjectIDFromUser(userIDFromSessionOrDB);
    IUserListManager userListManager = getUserListManager();

    boolean isNormalList = list_type == UserList.LIST_TYPE.NORMAL;
    Collection<IUserList> listsForUser = isNormalList ?
        userListManager.getSimpleListsForUser(projectIDFromUser, userIDFromSessionOrDB, onlyCreated, visited) :
        userListManager.getAllPublicOrMine(projectIDFromUser, userIDFromSessionOrDB, list_type == UserList.LIST_TYPE.QUIZ, false);

    long now = System.currentTimeMillis();

    if (now - then > 10) {
      logger.info("getSimpleListsForUser took " + (now - then) + " to get " + listsForUser.size() +
          " lists for user " + userIDFromSessionOrDB + " type " + list_type);
    }

    return listsForUser;
  }

  /**
   * @return
   * @throws DominoSessionException
   * @see UserContainer#getQuizListBox
   */
  @Override
  public Collection<IUserListLight> getAllQuiz() throws DominoSessionException {
    int userIDFromSessionOrDB = getUserIDFromSessionOrDB();
    long then = System.currentTimeMillis();
    Collection<IUserListLight> listsForUser =
        getUserListManager().getUserListDAO().getAllOrMineLight(getProjectIDFromUser(userIDFromSessionOrDB), userIDFromSessionOrDB, true);

    long now = System.currentTimeMillis();

    if (now - then > 30) {
      logger.info("getAllQuiz took " + (now - then) + " to get " + listsForUser.size() + " lists for user " + userIDFromSessionOrDB);
    }

    return listsForUser;
  }

  /**
   * TODO : maybe remove second arg
   *
   * @param userListID
   * @param exID
   * @return
   * @see mitll.langtest.client.scoring.UserListSupport#getAddListLink
   */
  public void addItemToUserList(int userListID, int exID) throws DominoSessionException {
    if (db.getCustomOrPredefExercise(getProjectIDFromUser(), exID) == null) {
      logger.error("can't find ex #" + exID);
    } else {
      getUserListManager().addItemToList(userListID, exID);
    }
  }


  /**
   * TODO: Tamas wants this back in.
   * <p>
   * Put the new item in the database,
   * copy the audio under bestAudio ??
   * assign the item to a user list
   * <p>
   * So here we set the exercise id to the final id, not a provisional id, as assigned earlier.
   *
   * @param userListID
   * @param initialText
   * @return CommonExercise with id from database
   * @see mitll.langtest.client.custom.dialog.EditableExerciseList#checkIsValidPhrase
   */
  @Override
  public CommonExercise newExercise(int userListID, String initialText) throws DominoSessionException {
    if (isValidForeignPhrase(initialText).isEmpty()) {
      int userIDFromSessionOrDB = getUserIDFromSessionOrDB();
      int projectIDFromUser = getProjectIDFromUser();
//    if (DEBUG) logger.debug("newExercise : made user exercise " + userExercise + " on list " + userListID);

//    CommonExercise exercise = getExerciseIfKnown(userExercise);
//    if (exercise != null) {
//      addItemToUserList(userListID, exercise.getID());
//      return exercise;
//    } else {

      CommonExercise exercise = makeNewExercise(userIDFromSessionOrDB, projectIDFromUser, initialText);
      getUserListManager().newExercise(userListID, exercise);
      if (DEBUG) {
        logger.info("\tnewExercise : made user exercise " + exercise + " on list " + userListID);
        logger.info("\tnewExercise : made user exercise context " + exercise.getDirectlyRelated() + " on list " + userListID);
      }
      return exercise;
    } else {
      return null;
    }
  }

  private Collection<String> isValidForeignPhrase(String foreign) throws DominoSessionException {
    return getAudioFileHelper().checkLTSOnForeignPhrase(foreign, "");
  }

  private boolean isValid(Project project, String foreign) {
    return isValidForeignPhrase(project, foreign).isEmpty();
  }

  private Collection<String> isValidForeignPhrase(Project project, String foreign) {
    return project.getAudioFileHelper().checkLTSOnForeignPhrase(foreign, "");
  }

  @NotNull
  private CommonExercise makeNewExercise(int user, int projectID, String safeText) {
    CommonExercise newItem = getNewItem(user, projectID);
    newItem.getMutable().setForeignLanguage(safeText);
    return newItem;
  }

  /**
   * TODOx : don't do it like this!
   * <p>
   * <p>
   *
   * @return
   * @seex #makeExerciseList
   */
  private CommonExercise getNewItem(int user, int projectID) {
    return new Exercise(-1, user, "", projectID, false);
  }

  /**
   * @param userListID
   * @return
   * @see mitll.langtest.client.flashcard.PolyglotFlashcardFactory#PolyglotFlashcardFactory
   * @see mitll.langtest.client.flashcard.PolyglotPracticePanel#PolyglotPracticePanel
   */
  public QuizSpec getQuizInfo(int userListID) {
    return db.getUserListManager().getQuizInfo(userListID);
  }

  /**
   * TODO: This is a bit of a mess.
   * Create in bulk, e.g. as import from quizlet export format.
   *
   * @return
   * @paramx userListID
   * @paramx userExerciseText
   * @see mitll.langtest.client.custom.userlist.ImportBulk#doBulk
   */
  @Override
  public List<CommonShell> reallyCreateNewItems(int userListID, String userExerciseText) throws DominoSessionException {
    int userIDFromSession = getUserIDFromSessionOrDB();

    UserList<CommonShell> userListByID = getUserListManager().getUserListByID(userListID);

    int projid = userListByID.getProjid();
    Project project = getProject(projid);
    String[] lines = userExerciseText.split("\n");

    // production onlhy
    Project englishProject = db.getProjectManagement().getProductionByLanguage(Language.ENGLISH);
    Set<String> currentKnownFL = getCurrentOnList(userListByID);

    if (DEBUG) {
      logger.info("reallyCreateNewItems got " + lines.length + " lines");
      logger.info("reallyCreateNewItems currentKnownFL " + currentKnownFL.size());
    }
    Set<Integer> ids = userListByID.getExercises().stream().map(HasID::getID).collect(Collectors.toSet());

    Set<CommonExercise> knownAlready = new HashSet<>();
    List<CommonExercise> newItems = new BulkImport(db, db).convertTextToExercises(lines, knownAlready, ids, currentKnownFL,
        project, englishProject, userIDFromSession);

    List<CommonExercise> reallyNewItems = new ArrayList<>();
    List<CommonExercise> actualItems = addItemsToList(userListID, project, knownAlready, newItems, reallyNewItems);

    if (!reallyNewItems.isEmpty()) {
      new AudioCopy(getDatabase(), db.getProjectManagement(), db)
          .copyAudio(projid, reallyNewItems, Collections.emptyMap());
    }

    {
      actualItems.forEach(commonExercise -> {
        if (!ids.contains(commonExercise.getID())) {
          userListByID.addExercise(commonExercise);
          if (DEBUG)
            logger.info("reallyCreateNewItems Adding " + commonExercise.getID() + " " + commonExercise.getEnglish() + " " + commonExercise.getForeignLanguage());
        } else {
          if (DEBUG)
            logger.info("reallyCreateNewItems not adding " + commonExercise.getID() + " " + commonExercise.getEnglish() + " " + commonExercise.getForeignLanguage());
        }
      });
    }

    List<CommonShell> exercises = userListByID.getExercises();

    logger.info("reallyCreateNewItems : Returning " + actualItems.size() +
        "\n\tvs input lines" + lines.length +
        "\n\tcurrent on list " + exercises.size());
    return exercises;
  }


  @NotNull
  private Set<String> getCurrentOnList(UserList<CommonShell> userListByID) {
    Set<String> currentKnownFL = new HashSet<>();
    for (CommonShell shell : userListByID.getExercises()) {
      currentKnownFL.add(shell.getForeignLanguage());
    }
    return currentKnownFL;
  }

  @Override
  public void clearAudio(int audioID) throws DominoSessionException {
    getUserIDFromSessionOrDB();
    db.getAudioDAO().markDefect(audioID);
  }

  /**
   * Assemble the set of known and new exercises to add to the user list.
   *
   * @param userListID
   * @param project
   * @param knownAlready
   * @param candidates   if it's a known exercise don't create it
   * @return
   */
  private List<CommonExercise> addItemsToList(int userListID,
                                              Project project,
                                              Set<CommonExercise> knownAlready,
                                              List<CommonExercise> candidates,
                                              List<CommonExercise> reallyNewItems) {
    List<CommonExercise> actualItems = new ArrayList<>();

    IUserListManager userListManager = getUserListManager();
    if (DEBUG) logger.info("addItemsToList candidates #" + candidates.size());
    for (CommonExercise candidate : candidates) {
      if (candidate == null) logger.warn("\n\n\nhuh? candidate is null???");
      else {
        String foreignLanguage = candidate.getForeignLanguage();
        int id = candidate.getID();

        if (knownAlready.contains(candidate)) {
          if (DEBUG) logger.info("addItemsToList item #" + id + " '" + foreignLanguage + "' is known already...");
          userListManager.addItemToList(userListID, id);
          actualItems.add(candidate);
        } else if (isValidForeignPhrase(project, foreignLanguage).isEmpty()) {
          if (DEBUG) logger.info("addItemsToList new item #" + id + " '" + foreignLanguage + "' is  valid");
          userListManager.newExercise(userListID, candidate);
          actualItems.add(candidate);
          reallyNewItems.add(candidate);
        } else {
          if (DEBUG) logger.info("addItemsToList item #" + id + " '" + foreignLanguage + "' is invalid");
        }
      }
    }

    return actualItems;
  }
}