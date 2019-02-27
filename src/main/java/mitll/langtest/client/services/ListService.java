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

package mitll.langtest.client.services;

import com.github.gwtbootstrap.client.ui.Button;
import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;
import mitll.langtest.client.analysis.UserContainer;
import mitll.langtest.client.custom.ContentView;
import mitll.langtest.shared.common.DominoSessionException;
import mitll.langtest.shared.custom.*;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.CommonShell;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@RemoteServiceRelativePath("list-manager")
public interface ListService extends RemoteService {
  /**
   * @return
   * @throws DominoSessionException
   * @see ContentView#showContent
   */
  Collection<UserList<CommonShell>> getLists() throws DominoSessionException;

  /**
   * @param name
   * @param description
   * @param dliClass
   * @param isPublic
   * @param listType
   * @param quizSpec
   * @return
   * @see mitll.langtest.client.custom.dialog.CreateListDialog#addUserList
   */
  UserList addUserList(String name, String description, String dliClass, boolean isPublic, UserList.LIST_TYPE listType,
                       int size, QuizSpec quizSpec, Map<String, String> unitChapter) throws DominoSessionException;

  void update(UserList userList) throws DominoSessionException;

  /**
   * @param id
   * @return
   * @see mitll.langtest.client.custom.userlist.ListView#gotDelete(Button, UserList)
   */
  boolean deleteList(int id) throws DominoSessionException;

  /**
   * @param listid
   * @param exid
   * @return
   * @see mitll.langtest.client.custom.dialog.EditableExerciseList#deleteItem
   */
  boolean deleteItemFromList(int listid, int exid) throws DominoSessionException;

  /**
   * @param userListID
   * @param user
   * @see mitll.langtest.client.list.FacetExerciseList#addVisitor
   */
  UserList addVisitor(int userListID, int user) throws DominoSessionException;

  /**
   * @param userListID
   * @param user
   * @see mitll.langtest.client.custom.userlist.ListView#gotDeleteVisitor
   */
  void removeVisitor(int userListID, int user) throws DominoSessionException;

  /**
   * @param onlyCreated
   * @param visited
   * @param includeQuiz
   * @return
   * @see UserContainer#getListBox
   */
  Collection<UserList<CommonShell>> getListsForUser(boolean onlyCreated, boolean visited, boolean includeQuiz) throws DominoSessionException;

  Collection<IUserList> getSimpleListsForUser(boolean onlyCreated, boolean visited, UserList.LIST_TYPE list_type) throws DominoSessionException;

  Collection<IUserListLight> getAllQuiz() throws DominoSessionException;

  Collection<IUserListWithIDs> getListsWithIDsForUser(boolean onlyCreated, boolean visited) throws DominoSessionException;

  int getNumOnList(int listid);
  /**
   * @param userListID
   * @param exID
   * @see mitll.langtest.client.scoring.UserListSupport#getAddListLink
   */
  void addItemToUserList(int userListID, int exID) throws DominoSessionException;

  /**
   * @param userListID
   * @param initialText
   * @return
   * @see mitll.langtest.client.custom.dialog.EditableExerciseList#checkIsValidPhrase
   */
  CommonExercise newExercise(int userListID, String initialText) throws DominoSessionException;

  QuizSpec getQuizInfo(int userListID);

  List<CommonShell> reallyCreateNewItems(int userListID, String userExerciseText) throws DominoSessionException;

  void clearAudio(int audioID) throws DominoSessionException;
}
