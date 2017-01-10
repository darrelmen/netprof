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

import com.github.gwtbootstrap.client.ui.Button;
import mitll.langtest.client.services.ListService;
import mitll.langtest.server.database.custom.IUserListManager;
import mitll.langtest.shared.custom.UserList;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.CommonShell;
import mitll.langtest.shared.exercise.Exercise;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

@SuppressWarnings("serial")
public class ListServiceImpl extends MyRemoteServiceServlet implements ListService {
  private static final Logger logger = LogManager.getLogger(ListServiceImpl.class);
  private static final boolean DEBUG = true;

  /**
   * @param userid
   * @param name
   * @param description
   * @param dliClass
   * @param isPublic
   * @return
   * @see mitll.langtest.client.custom.dialog.CreateListDialog#doCreate
   */
  @Override
  public long addUserList(int userid, String name, String description, String dliClass, boolean isPublic) {
    return getUserListManager().addUserList(userid, name, description, dliClass, isPublic, getProjectID());
  }

  /**
   * @param id
   * @return
   * @see mitll.langtest.client.custom.ListManager#deleteList(Button, UserList, boolean)
   */
  @Override
  public boolean deleteList(long id) {
    return getUserListManager().deleteList(id);
  }

  /**
   * @param listid
   * @param exid
   * @return
   * @see mitll.langtest.client.custom.dialog.NewUserExercise#deleteItem
   */
  @Override
  public boolean deleteItemFromList(long listid, int exid) {
    return getUserListManager().deleteItemFromList(listid, exid, db.getTypeOrder(getProjectID()));
  }

  /**
   * @param userListID
   * @param isPublic
   * @see mitll.langtest.client.custom.ListManager#setPublic
   */
  @Override
  public void setPublicOnList(long userListID, boolean isPublic) {
    getUserListManager().setPublicOnList(userListID, isPublic);
  }

  /**
   * @param userListID
   * @param user
   * @see mitll.langtest.client.custom.ListManager#addVisitor(mitll.langtest.shared.custom.UserList)
   */
  public void addVisitor(long userListID, int user) {
    getUserListManager().addVisitor(userListID, user);
  }

  /**
   * @param userid
   * @param onlyCreated
   * @param visited
   * @return
   * @see mitll.langtest.client.custom.Navigation#showInitialState()
   * @see mitll.langtest.client.custom.ListManager#viewLessons
   * @see mitll.langtest.client.custom.exercise.NPFExercise#populateListChoices
   */
  public Collection<UserList<CommonShell>> getListsForUser(int userid, boolean onlyCreated, boolean visited) {
    //  if (!onlyCreated && !visited) logger.error("getListsForUser huh? asking for neither your lists nor  your visited lists.");
    return getUserListManager().getListsForUser(userid, onlyCreated, visited, getProjectID());
  }

  /**
   * @param search
   * @param userid
   * @return
   * @see mitll.langtest.client.custom.ListManager#viewLessons
   */
  @Override
  public Collection<UserList<CommonShell>> getUserListsForText(String search, int userid) {
    return getUserListManager().getUserListsForText(search, userid, getProjectID());
  }

  /**
   * TODO : maybe remove second arg
   *
   * @param userListID
   * @param exID
   * @return
   * @see mitll.langtest.client.custom.exercise.NPFExercise#populateListChoices
   */
  public void addItemToUserList(long userListID, int exID) {
    CommonExercise customOrPredefExercise = db.getCustomOrPredefExercise(getProjectID(), exID);
    getUserListManager().addItemToList(userListID, "" + exID, customOrPredefExercise.getID());
  }

  /**
   * @return
   * @see mitll.langtest.client.custom.ListManager#viewReview
   */
  @Override
  public List<UserList<CommonShell>> getReviewLists() {
    IUserListManager userListManager = getUserListManager();
    int projectID = getProjectID();
    Collection<String> typeOrder = db.getTypeOrder(projectID);
    Set<Integer> ids = db.getIDs(projectID);
    UserList<CommonShell> defectList = userListManager.getDefectList(typeOrder, ids);

    List<UserList<CommonShell>> lists = new ArrayList<>();
    lists.add(defectList);

    lists.add(userListManager.getCommentedList(typeOrder, ids));
    if (!getProject().isNoModel()) {
      lists.add(userListManager.getAttentionList(typeOrder, ids));
    }
    return lists;
  }

  /**
   * Put the new item in the database,
   * copy the audio under bestAudio
   * assign the item to a user list
   * <p>
   * So here we set the exercise id to the final id, not a provisional id, as assigned earlier.
   *
   * @param userListID
   * @param userExercise
   * @param language
   * @see mitll.langtest.client.custom.dialog.NewUserExercise#afterValidForeignPhrase
   */
 /* @Override
  public CommonExercise reallyCreateNewItem(long userListID, CommonExercise userExercise, String language) {
    if (DEBUG) logger.debug("reallyCreateNewItem : made user exercise " + userExercise + " on list " + userListID);
    getUserListManager().reallyCreateNewItem(userListID, userExercise, getMediaDir());
    int id = userExercise.getID();

    for (AudioAttribute audioAttribute : userExercise.getAudioAttributes()) {
      if (DEBUG) logger.debug("\treallyCreateNewItem : update " + audioAttribute + " to " + id);
      db.getAudioDAO().updateExerciseID(
          audioAttribute.getUniqueID(),
          id,
          audioAttribute.getAudioRef());
    }
    if (DEBUG) logger.debug("\treallyCreateNewItem : made user exercise " + userExercise + " on list " + userListID);

    return userExercise;
  }*/

  /**
   * @return
   * @see #reallyCreateNewItem
   */
  String getMediaDir() {
    return serverProps.getMediaDir();
  }

  @Override
  public Collection<CommonExercise> reallyCreateNewItems(int creator, long userListID, String userExerciseText) {
    String[] lines = userExerciseText.split("\n");
    if (DEBUG) logger.info("got " + lines.length + " lines");
    List<CommonExercise> newItems = new ArrayList<>();
    UserList<CommonShell> userListByID =
        db.getUserListManager().getUserListByID(userListID, Collections.emptyList(), Collections.emptySet());
    int n = userListByID.getExercises().size();
    Set<String> currentKnownFL = new HashSet<>();
    for (CommonShell shell : userListByID.getExercises()) currentKnownFL.add(shell.getForeignLanguage());
    boolean onFirst = true;
    boolean firstColIsEnglish = false;
    for (String line : lines) {
      String[] parts = line.split("\\t");
//      logger.info("\tgot " + parts.length + " parts");
      if (parts.length > 1) {
        String fl = parts[0];
        String english = parts[1];
        if (onFirst && english.equalsIgnoreCase(getProject().getLanguage())) {
          logger.info("reallyCreateNewItems skipping header line");
          firstColIsEnglish = true;
        } else {
          if (firstColIsEnglish || (isValidForeignPhrase(english, "") && !isValidForeignPhrase(fl, ""))) {
            String temp = english;
            english = fl;
            fl = temp;
            //logger.info("flip english '" +english+ "' to fl '" +fl+ "'");
          }
          Exercise newItem =
              new Exercise(-1,
                  (int) creator,
                  english,
                  getProjectID(),
                  false);
          newItem.setForeignLanguage(fl);

          newItems.add(newItem);
          logger.info("reallyCreateNewItems new " + newItem);
        }
      }
      onFirst = false;
    }

    List<CommonExercise> actualItems = new ArrayList<>();
    for (CommonExercise candidate : newItems) {
      String foreignLanguage = candidate.getForeignLanguage();
      if (!currentKnownFL.contains(foreignLanguage)) {
        if (isValidForeignPhrase(foreignLanguage, candidate.getTransliteration())) {
          getUserListManager().reallyCreateNewItem(userListID, candidate, serverProps.getMediaDir());
          actualItems.add(candidate);
        } else {
          logger.info("item #" + candidate.getID() + " '" + candidate.getForeignLanguage() + "' is invalid");
        }
      }
    }
    logger.info("Returning " + actualItems.size() + "/" + lines.length);
    return actualItems;
  }

  /**
   * @param exercise
   * @return
   * @see mitll.langtest.client.custom.dialog.ReviewEditableExercise#duplicateExercise
   */
  @Override
  public CommonExercise duplicateExercise(CommonExercise exercise) {
    return db.duplicateExercise(exercise);
  }

  /**
   * Can't check if it's valid if we don't have a model.
   *
   * @param foreign
   * @param transliteration
   * @return
   * @see mitll.langtest.client.custom.dialog.NewUserExercise#isValidForeignPhrase(mitll.langtest.shared.custom.UserList, mitll.langtest.client.list.ListInterface, com.google.gwt.user.client.ui.Panel, boolean)
   */
  private boolean isValidForeignPhrase(String foreign, String transliteration) {
    return getProject().getAudioFileHelper().checkLTSOnForeignPhrase(foreign, transliteration);
  }

  /**
   * @param userExercise
   * @see mitll.langtest.client.custom.dialog.EditableExerciseDialog#postEditItem
   */
  @Override
  public void editItem(CommonExercise userExercise, boolean keepAudio) {
    db.editItem(userExercise, keepAudio);
    if (DEBUG) logger.debug("editItem : now user exercise " + userExercise);
  }

  IUserListManager getUserListManager() {
    return db.getUserListManager();
  }
}