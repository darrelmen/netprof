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

package mitll.langtest.server.services;

import mitll.langtest.client.analysis.UserContainer;
import mitll.langtest.client.banner.NewContentChooser;
import mitll.langtest.client.custom.ContentView;
//import mitll.langtest.client.custom.userlist.ImportBulk;
import mitll.langtest.client.custom.dialog.ReviewEditableExercise;
import mitll.langtest.client.custom.userlist.ListContainer;
import mitll.langtest.client.services.ListService;
import mitll.langtest.server.database.custom.IUserListManager;
import mitll.langtest.server.database.exercise.Project;
import mitll.langtest.server.domino.AudioCopy;
import mitll.langtest.shared.common.DominoSessionException;
import mitll.langtest.shared.common.RestrictedOperationException;
import mitll.langtest.shared.custom.*;
import mitll.langtest.shared.exercise.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

@SuppressWarnings("serial")
public class ListServiceImpl extends MyRemoteServiceServlet implements ListService {
  private static final Logger logger = LogManager.getLogger(ListServiceImpl.class);

  private static final boolean DEBUG = false;
  private static final boolean DEBUG_IMPORT = false;

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
   * @param duration
   * @param minScore
   * @param showAudio
   * @return
   * @see mitll.langtest.client.custom.dialog.CreateListDialog#doCreate
   */
  @Override
  public UserList addUserList(String name, String description, String dliClass, boolean isPublic,
                              UserList.LIST_TYPE listType,
                              int size,
                              int duration, int minScore, boolean showAudio,
                              Map<String, String> unitChapter) throws DominoSessionException {
    int userIDFromSessionOrDB = getUserIDFromSessionOrDB();
    IUserListManager userListManager = getUserListManager();
    int projectIDFromUser = getProjectIDFromUser(userIDFromSessionOrDB);

    return listType == UserList.LIST_TYPE.NORMAL ?
        userListManager.addUserList(userIDFromSessionOrDB, name, description, dliClass, isPublic, projectIDFromUser) :
        userListManager.addQuiz(userIDFromSessionOrDB, name, description, dliClass, isPublic, projectIDFromUser, size, duration, minScore, showAudio, unitChapter);
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
   * @see ContentView#showContent
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

  /**
   * TODO : consider not doing it separately from other facets.
   *
   * @param onlyCreated
   * @param visited
   * @return
   * @see mitll.langtest.client.list.FacetExerciseList#populateListChoices
   */
  @Override
  public Collection<IUserList> getSimpleListsForUser(boolean onlyCreated, boolean visited, UserList.LIST_TYPE list_type) throws DominoSessionException {
    int userIDFromSessionOrDB = getUserIDFromSessionOrDB();
    long then = System.currentTimeMillis();
    int projectIDFromUser = getProjectIDFromUser(userIDFromSessionOrDB);
    IUserListManager userListManager = getUserListManager();

    Collection<IUserList> listsForUser = list_type == UserList.LIST_TYPE.NORMAL ? userListManager
        .getSimpleListsForUser(userIDFromSessionOrDB, projectIDFromUser, onlyCreated, visited) :
        userListManager.getAllQuizUserList(projectIDFromUser, userIDFromSessionOrDB);

    long now = System.currentTimeMillis();

    if (now - then > 10) {
      logger.info("getSimpleListsForUser took " + (now - then) + " to get " + listsForUser.size() +
          " lists for user " + userIDFromSessionOrDB + " type " + list_type);
    }

    return listsForUser;
  }

  /**
   * @param onlyCreated
   * @param visited
   * @return
   * @throws DominoSessionException
   * @see UserContainer#getListBox
   */
  @Override
  public Collection<IUserListLight> getLightListsForUser(boolean onlyCreated, boolean visited) throws DominoSessionException {
    int userIDFromSessionOrDB = getUserIDFromSessionOrDB();
    long then = System.currentTimeMillis();
    Collection<IUserListLight> listsForUser = getUserListManager()
        .getNamesForUser(userIDFromSessionOrDB, getProjectIDFromUser(userIDFromSessionOrDB), onlyCreated, visited);

    long now = System.currentTimeMillis();

    if (now - then > 30) {
      logger.info("took " + (now - then) + " to get " + listsForUser.size() + " lists for user " + userIDFromSessionOrDB);
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
   * @return
   * @throws DominoSessionException
   * @throws RestrictedOperationException
   * @see NewContentChooser#getReviewList
   * @param isContext
   */
/*  @Override
  public UserList<CommonShell> getReviewList(boolean isContext) throws DominoSessionException, RestrictedOperationException {
    int userIDFromSessionOrDB = getUserIDFromSessionOrDB();
    if (hasQCPerm(userIDFromSessionOrDB)) {
      return getUserListManager().getCommentedList(getProjectIDFromUser(userIDFromSessionOrDB), isContext);
    } else {
      throw getRestricted("getting review lists");
    }
  }*/

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
    if (isValidForeignPhrase(initialText)) {
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

  private boolean isValidForeignPhrase(String foreign) throws DominoSessionException {
    return getAudioFileHelper().checkLTSOnForeignPhrase(foreign, "");
  }

  private boolean isValidForeignPhrase(Project project, String foreign) {
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
    UserList<?> list = db.getUserListManager().getUserListDAO().getList(userListID);
    if (list == null) logger.warn("no quiz with list id " + userListID);
    QuizSpec quizSpec = list != null ? new QuizSpec(list.getRoundTimeMinutes(), list.getMinScore(), list.shouldShowAudio()) : new QuizSpec(10, 35, false);
    if (DEBUG) logger.info("Returning " + quizSpec + " for " + userListID);
    return quizSpec;
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


    Set<String> currentKnownFL = getCurrentOnList(userListByID);

    if (DEBUG) {
      logger.info("reallyCreateNewItems got " + lines.length + " lines");
      logger.info("reallyCreateNewItems currentKnownFL " + currentKnownFL.size());
    }
    Set<Integer> ids = userListByID.getExercises().stream().map(HasID::getID).collect(Collectors.toSet());

    Set<CommonExercise> knownAlready = new HashSet<>();
    List<CommonExercise> newItems = convertTextToExercises(lines, knownAlready, ids, currentKnownFL, project, userIDFromSession);

    List<CommonExercise> reallyNewItems = new ArrayList<>();
    List<CommonExercise> actualItems = addItemsToList(userListID, project, knownAlready, newItems, reallyNewItems);

    if (!reallyNewItems.isEmpty()) {
      new AudioCopy(getDatabase(), db.getProjectManagement(), db)
          .copyAudio(projid, reallyNewItems, Collections.emptyMap());
    }


    {
//      Set<Integer> current = new HashSet<>();
//      userListByID.getExercises().forEach(ex -> current.add(ex.getID()));

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
//    return actualItems.size();

    List<CommonShell> exercises = userListByID.getExercises();

    logger.info("reallyCreateNewItems : Returning " + actualItems.size() +
        "\n\tvs input lines" + lines.length +
        "\n\tcurrent on list " + exercises.size());
    return exercises;
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
        } else if (isValidForeignPhrase(project, foreignLanguage)) {
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

  /**
   * @param lines
   * @param knownAlready
   * @param currentKnownFL
   * @param project
   * @param userIDFromSession
   * @return
   * @see #reallyCreateNewItems(int, String)
   */
  private List<CommonExercise> convertTextToExercises(String[] lines,
                                                      Set<CommonExercise> knownAlready,
                                                      Set<Integer> onListAlready,
                                                      Set<String> currentKnownFL,
                                                      Project project,
                                                      int userIDFromSession) {
    boolean onFirst = true;
    boolean firstColIsEnglish = false;
    List<CommonExercise> newItems = new ArrayList<>();

    logger.info("convertTextToExercises currently know about " + currentKnownFL.size() + " items on list.");

    for (String line : lines) {
      String[] parts = line.split("\\t");
//      logger.info("\tgot " + parts.length + " parts");
      if (parts.length > 1) {
        String fl = parts[0].trim();
        String english = parts[1].trim();

        if (onFirst && english.equalsIgnoreCase(project.getLanguage())) {
          if (DEBUG_IMPORT) logger.info("convertTextToExercises skipping header line");
          firstColIsEnglish = true;
        } else {
          if (fl.trim().isEmpty()) {
            logger.warn("convertTextToExercises skipping line " + line);
          } else {
            if (!currentKnownFL.contains(fl)) {
              CommonExercise known = makeOrFindExercise(newItems, firstColIsEnglish, userIDFromSession, fl, english, project,
                  onListAlready);

              if (known != null && known.getID() > 0) {
                logger.info("convertTextToExercises made or found " + known.getID() +
                    "  '" + fl + "' = '" + english + "'");
                knownAlready.add(known);
              }
            } else {
              logger.info("convertTextToExercises skipping " + fl + " that's already on the list.");
            }
          }
        }
      }
      onFirst = false;
    }

    return newItems;
  }

  @NotNull
  private Set<String> getCurrentOnList(UserList<CommonShell> userListByID) {
    Set<String> currentKnownFL = new HashSet<>();
    for (CommonShell shell : userListByID.getExercises()) {
      currentKnownFL.add(shell.getForeignLanguage());
    }
    return currentKnownFL;
  }

  /**
   * @param newItems
   * @param firstColIsEnglish
   * @param userIDFromSession
   * @param fl
   * @param english
   * @param project
   * @param onListAlready
   * @return
   * @see #convertTextToExercises
   */
  private CommonExercise makeOrFindExercise(List<CommonExercise> newItems,
                                            boolean firstColIsEnglish,
                                            int userIDFromSession,
                                            String fl,
                                            String english,
                                            Project project,
                                            Set<Integer> onListAlready) {
    english = english.replaceAll("&#39;", "").trim();

    int projectID = project.getID();
    if (firstColIsEnglish || (isValidForeignPhrase(project, english) && !isValidForeignPhrase(project, fl))) {
      String temp = english;
      english = fl;
      fl = temp;
      //logger.info("flip english '" +english+ "' to fl '" +fl+ "'");
    }

    if (DEBUG) logger.info("makeOrFindExercise : onListAlready " + onListAlready.size() + " " + onListAlready);

    CommonExercise exercise = getExerciseByVocab(projectID, fl);
    boolean found = false;
    if (exercise != null) {
      if (DEBUG) logger.info("makeOrFindExercise : exercise " + exercise.getID() + " " + exercise.getForeignLanguage());
      if (exercise.getEnglish().equalsIgnoreCase(english)) {
        if (DEBUG)
          logger.info("makeOrFindExercise : exercise english match " + exercise.getID() + " " + exercise.getEnglish());
        boolean contains = onListAlready.contains(exercise.getID());
        if (!contains) {
          newItems.add(exercise);
        } else {
          found = true;
          exercise = null;
        }
      } else {
        if (DEBUG) logger.info("english mismatch " + english + " vs " + exercise.getEnglish());
        exercise = null;
      }
    } else {
      if (DEBUG) logger.info("makeOrFindExercise : exercise " + exercise);
      // gotta be mine
      List<CommonExercise> exactMatch = db.getExerciseDAO(projectID).getExactMatch(fl, userIDFromSession);

      if (!exactMatch.isEmpty()) {
        if (exactMatch.size() > 1)
          logger.info("makeOrFindExercise : found " + exactMatch.size() + " exact matches for " + fl);

        CommonExercise next = exactMatch.iterator().next();
        if (DEBUG) logger.info("makeOrFindExercise : next " + next.getID() + " " + next.getForeignLanguage());
        boolean contains = onListAlready.contains(next.getID());
        if (!contains) {
          newItems.add(next);
          exercise = next;
        } else {
          found = true;
          exercise = null;
        }
      }
    }

    if (exercise == null && !found) { // OK gotta make a new one
      Exercise newItem =
          new Exercise(-1,
              userIDFromSession,
              english,
              projectID,
              false);
      newItem.setForeignLanguage(fl);
      newItems.add(newItem);
      exercise = newItem;
      if (DEBUG) logger.info("reallyCreateNewItems new " + newItem);
    }

    return exercise;
  }

  private CommonExercise getExerciseByVocab(int projectID, String foreignLanguage) {
    return db.getProject(projectID).getExerciseByExactMatch(foreignLanguage.trim());
  }

  /**
   * @param exercise
   * @return
   * @see mitll.langtest.client.custom.dialog.ReviewEditableExercise#duplicateExercise
   */
 /* @Override
  public CommonExercise duplicateExercise(CommonExercise exercise) {
    return db.duplicateExercise(exercise);
  }*/
}