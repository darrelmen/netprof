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

import mitll.langtest.client.services.ListService;
import mitll.langtest.shared.common.DominoSessionException;
import mitll.langtest.shared.common.RestrictedOperationException;
import mitll.langtest.shared.custom.UserList;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.CommonShell;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

@SuppressWarnings("serial")
public class ListServiceImpl extends MyRemoteServiceServlet implements ListService {
  private static final Logger logger = LogManager.getLogger(ListServiceImpl.class);
  private static final boolean DEBUG = false;

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
   * @return
   * @see mitll.langtest.client.custom.dialog.CreateListDialog#doCreate
   */
  @Override
  public UserList addUserList(String name, String description, String dliClass, boolean isPublic) throws DominoSessionException {
    int userIDFromSessionOrDB = getUserIDFromSessionOrDB();
    return getUserListManager().addUserList(userIDFromSessionOrDB, name, description, dliClass, isPublic, getProjectIDFromUser(userIDFromSessionOrDB));
  }

  @Override
  public void update(UserList userList) throws DominoSessionException {
    getUserIDFromSessionOrDB();
    getUserListManager().update(userList);
  }

  /**
   * @param id
   * @return
   * @seex ListManager#deleteList
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
    int userIDFromSessionOrDB = getUserIDFromSessionOrDB();
    return getUserListManager().deleteItemFromList(listid, exid, db.getTypeOrder(getProjectIDFromUser(userIDFromSessionOrDB)));
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
   * @return
   */
  public Collection<UserList<CommonShell>> getListsForUser(boolean onlyCreated, boolean visited) throws DominoSessionException {
    //  if (!onlyCreated && !visited) logger.error("getListsForUser huh? asking for neither your lists nor  your visited lists.");
    try {
      int userIDFromSessionOrDB = getUserIDFromSessionOrDB();
      return getUserListManager().getListsForUser(userIDFromSessionOrDB, getProjectIDFromUser(userIDFromSessionOrDB), onlyCreated, visited);
    } catch (Exception e) {
      logger.error("Got " + e, e);
      return Collections.emptyList();
    }
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
    CommonExercise customOrPredefExercise = db.getCustomOrPredefExercise(getProjectIDFromUser(), exID);
    getUserListManager().addItemToList(userListID, "" + exID, customOrPredefExercise.getID());
  }


  @Override
  public UserList<CommonShell> getReviewList() throws DominoSessionException, RestrictedOperationException {
    int userIDFromSessionOrDB = getUserIDFromSessionOrDB();
    if (hasQCPerm(userIDFromSessionOrDB)) {
      return getUserListManager().getCommentedList(db.getIDs(getProjectIDFromUser(userIDFromSessionOrDB)));
    } else {
      throw new RestrictedOperationException("getting review lists", true);
    }
  }

  /**
   * @return
   * @seex #newExercise
   */
  String getMediaDir() {
    return serverProps.getMediaDir();
  }

  /**
   * Put the new item in the database,
   * copy the audio under bestAudio ??
   * assign the item to a user list
   * <p>
   * So here we set the exercise id to the final id, not a provisional id, as assigned earlier.
   *
   * @param userListID
   * @param userExercise
   * @return CommonExercise with id from database
   * @see mitll.langtest.client.custom.dialog.NewUserExercise#afterValidForeignPhrase
   */
  @Override
  public CommonExercise newExercise(int userListID, CommonExercise userExercise) throws DominoSessionException {
    if (DEBUG) logger.debug("newExercise : made user exercise " + userExercise + " on list " + userListID);

    CommonExercise exercise = getExerciseIfKnown(userExercise);
    if (exercise != null) {
      addItemToUserList(userListID, exercise.getID());
      return exercise;
    } else {
      getUserListManager().newExercise(userListID, userExercise, serverProps.getMediaDir());
      if (DEBUG) logger.debug("\tnewExercise : made user exercise " + userExercise + " on list " + userListID);
      return userExercise;
    }
  }

  private CommonExercise getExerciseIfKnown(CommonExercise userExercise) {
    return getExerciseByVocab(userExercise.getProjectID(), userExercise.getForeignLanguage());
  }

  private CommonExercise getExerciseByVocab(int projectID, String foreignLanguage) {
    return db.getProject(projectID).getExerciseBySearch(foreignLanguage.trim());
  }

  /**
   * Create in bulk, e.g. as import from quizlet export format.
   *
   * @return
   * @paramx userListID
   * @paramx userExerciseText
   */
/*
  @Override
  public Collection<CommonExercise> reallyCreateNewItems(int userListID, String userExerciseText) {
    String[] lines = userExerciseText.split("\n");

    if (DEBUG) logger.info("got " + lines.length + " lines");

    UserList<CommonShell> userListByID =
        getUserListManager().getUserListByID(userListID, Collections.emptyList(), Collections.emptySet());
    Set<String> currentKnownFL = getCurrentOnList(userListByID);

    Set<CommonExercise> knownAlready = new HashSet<>();
    List<CommonExercise> newItems = convertTextToExercises(lines, knownAlready, currentKnownFL);

    List<CommonExercise> actualItems = addItemsToList(userListID, userListByID, knownAlready, newItems);
    logger.info("reallyCreateNewItems : Returning " + actualItems.size() + "/" + lines.length);
    return actualItems;
  }
*/
 /* private List<CommonExercise> addItemsToList(int userListID,
                                              UserList<CommonShell> userListByID,
                                              Set<CommonExercise> knownAlready,
                                              List<CommonExercise> newItems) {
    List<CommonExercise> actualItems = new ArrayList<>();

    IUserListManager userListManager = getUserListManager();
    for (CommonExercise candidate : newItems) {
      String foreignLanguage = candidate.getForeignLanguage();
      if (knownAlready.contains(candidate)) {
        userListManager.addItemToList(userListID, "", candidate.getID());
        actualItems.add(candidate);
      } else if (isValidForeignPhrase(foreignLanguage, candidate.getTransliteration())) {
        userListManager.newExerciseOnList(userListByID, candidate, serverProps.getMediaDir());
        actualItems.add(candidate);
      } else {
        logger.info("item #" + candidate.getID() + " '" + candidate.getForeignLanguage() + "' is invalid");
      }
    }

    return actualItems;
  }*/

 /* private List<CommonExercise> convertTextToExercises(String[] lines,
                                                      Set<CommonExercise> knownAlready,
                                                      Set<String> currentKnownFL) {
    int projectID = getProjectIDFromUser();
    boolean onFirst = true;
    boolean firstColIsEnglish = false;
    List<CommonExercise> newItems = new ArrayList<>();

    logger.info("convertTextToExercises currently know about " + currentKnownFL.size());

    int userIDFromSession = getUserIDFromSessionOrDB();

    for (String line : lines) {
      String[] parts = line.split("\\t");
//      logger.info("\tgot " + parts.length + " parts");
      if (parts.length > 1) {
        String fl = parts[0];
        String english = parts[1];


        if (onFirst && english.equalsIgnoreCase(getProject().getLanguage())) {
          logger.info("convertTextToExercises skipping header line");
          firstColIsEnglish = true;
        } else {
          if (fl.trim().isEmpty()) {
            logger.warn("convertTextToExercises skipping line " + line);
          } else {
            if (!currentKnownFL.contains(fl)) {
              CommonExercise known = makeOrFindExercise(newItems, firstColIsEnglish, projectID, userIDFromSession, fl, english);
              logger.info("convertTextToExercises made or found " + fl + "=" + english);

              if (known != null) knownAlready.add(known);
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
    for (CommonShell shell : userListByID.getExercises()) currentKnownFL.add(shell.getForeignLanguage());
    return currentKnownFL;
  }
*/
/*  private CommonExercise makeOrFindExercise(List<CommonExercise> newItems, boolean firstColIsEnglish, int projectID,
                                            int userIDFromSession, String fl, String english) {
    if (firstColIsEnglish || (isValidForeignPhrase(english, "") && !isValidForeignPhrase(fl, ""))) {
      String temp = english;
      english = fl;
      fl = temp;
      //logger.info("flip english '" +english+ "' to fl '" +fl+ "'");
    }

    CommonExercise exercise = getExerciseByVocab(projectID, fl);

    if (exercise != null) {
      if (exercise.getEnglish().equalsIgnoreCase(english)) {
        newItems.add(exercise);
        return exercise;
      }
    }

    Exercise newItem =
        new Exercise(-1,
            userIDFromSession,
            english,
            projectID,
            false);
    newItem.setForeignLanguage(fl);
    newItems.add(newItem);
    logger.info("reallyCreateNewItems new " + newItem);
    return null;
  }*/

  /**
   * @param exercise
   * @return
   * @see mitll.langtest.client.custom.dialog.ReviewEditableExercise#duplicateExercise
   */
 /* @Override
  public CommonExercise duplicateExercise(CommonExercise exercise) {
    return db.duplicateExercise(exercise);
  }*/

  /**
   * Can't check if it's valid if we don't have a model.
   *
   * @param foreign
   * @param transliteration
   * @return
   * @see mitll.langtest.client.custom.dialog.NewUserExercise#isValidForeignPhrase
   */
/*  private boolean isValidForeignPhrase(String foreign, String transliteration) throws DominoSessionException {
    return getProject().getAudioFileHelper().checkLTSOnForeignPhrase(foreign, transliteration);
  }*/

  /**
   * @param userExercise
   * @see mitll.langtest.client.custom.dialog.NewUserExercise#editItem
   */
  @Override
  public void editItem(CommonExercise userExercise, boolean keepAudio) throws DominoSessionException {
    getUserIDFromSessionOrDB();
    db.editItem(userExercise, keepAudio);
  }

  /**
   * @paramx id
   * @paramx context
   * @seez mitll.langtest.client.custom.userlist.ListOperations#attachMedia
   */
/*
  public void updateContext(long id, String context) {
    getUserListDAO().updateContext(id, context);
  }

  public void updateRichText(long id, String richText) {
    getUserListDAO().updateRichText(id, richText);
  }
*/

/*
  private IUserListDAO getUserListDAO() {
    return getUserListManager().getUserListDAO();
  }
*/

/*
  public void updateName(long id, String name) {
    getUserListDAO().updateName(id, name);
  }
*/
}