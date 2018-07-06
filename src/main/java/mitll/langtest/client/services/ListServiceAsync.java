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

import com.google.gwt.user.client.rpc.AsyncCallback;
import mitll.langtest.shared.custom.*;
import mitll.langtest.shared.exercise.ClientExercise;
import mitll.langtest.shared.exercise.CommonShell;

import java.util.Collection;
import java.util.Map;

public interface ListServiceAsync {
  void getLightListsForUser(boolean onlyCreated, boolean visited, AsyncCallback<Collection<IUserListLight>> async);

  void getListsForUser(boolean onlyCreated, boolean visited, boolean includeQuiz, AsyncCallback<Collection<UserList<CommonShell>>> async);

  /**
   * @param onlyCreated
   * @param visited
   * @param async
   * @see mitll.langtest.client.list.FacetExerciseList#populateListChoices
   */
  void getSimpleListsForUser(boolean onlyCreated, boolean visited, UserList.LIST_TYPE list_type, AsyncCallback<Collection<IUserList>> async);

  void getListsWithIDsForUser(boolean onlyCreated, boolean visited, AsyncCallback<Collection<IUserListWithIDs>> async);


  void addItemToUserList(int userListID, int exID, AsyncCallback<Void> async);

  /**
   * @param name
   * @param description
   * @param dliClass
   * @param isPublic
   * @param listType
   * @param duration
   * @param minScore
   * @param showAudio
   * @param async
   * @see mitll.langtest.client.custom.exercise.NewListButton#addUserList
   */
  void addUserList(String name, String description, String dliClass,
                   boolean isPublic, UserList.LIST_TYPE listType, int size, int duration, int minScore, boolean showAudio,
                   Map<String,String> unitChapter,
                   AsyncCallback<UserList> async);

  void addVisitor(int userListID, int user, AsyncCallback<UserList> asyncCallback);

 // void newExercise(int userListID, ClientExercise userExercise, AsyncCallback<ClientExercise> async);


  void deleteList(int id, AsyncCallback<Boolean> async);

  void deleteItemFromList(int listid, int exid, AsyncCallback<Boolean> async);

  void getLists(AsyncCallback<Collection<UserList<CommonShell>>> async);

  void removeVisitor(int userListID, int user, AsyncCallback<Void> async);

  void update(UserList userList, AsyncCallback<Void> async);

  void getReviewList(AsyncCallback<UserList<CommonShell>> async);
}
