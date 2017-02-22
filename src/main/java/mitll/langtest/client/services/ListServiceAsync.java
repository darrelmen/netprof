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
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.shared.custom.UserList;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.CommonShell;

import java.util.Collection;
import java.util.List;

public interface ListServiceAsync {
  /**
   * TODO : add ability to search through list text...
   * @param search
   * @param async
   */
  void getUserListsForText(String search, AsyncCallback<Collection<UserList<CommonShell>>> async);

  void getListsForUser(boolean onlyCreated, boolean visited, AsyncCallback<Collection<UserList<CommonShell>>> async);

  void addItemToUserList(long userListID, int exID, AsyncCallback<Void> async);

  void addUserList(String name, String description, String dliClass,
                   boolean isPublic, AsyncCallback<Long> async);

  void addVisitor(long userListID, int user, AsyncCallback<Void> asyncCallback);

  void getReviewLists(AsyncCallback<List<UserList<CommonShell>>> async);

  void setPublicOnList(long userListID, boolean isPublic, AsyncCallback<Void> async);

  void newExercise(long userListID, CommonExercise userExercise, AsyncCallback<CommonExercise> async);

  void reallyCreateNewItems(long userListID, String userExerciseText, AsyncCallback<Collection<CommonExercise>> async);

  /**
   * @see mitll.langtest.client.custom.dialog.EditableExerciseDialog#postEditItem
   * @param userExercise
   * @param keepAudio
   * @param async
   */
  void editItem(CommonExercise userExercise, boolean keepAudio, AsyncCallback<Void> async);

  void deleteList(long id, AsyncCallback<Boolean> async);

  void deleteItemFromList(long listid, int exid, AsyncCallback<Boolean> async);

  void duplicateExercise(CommonExercise id, AsyncCallback<CommonExercise> async);
}
