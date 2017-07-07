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

package mitll.langtest.server.database.custom;

import mitll.langtest.server.database.annotation.IAnnotationDAO;
import mitll.langtest.server.database.userlist.IUserExerciseListVisitorDAO;
import mitll.langtest.server.database.userlist.IUserListDAO;
import mitll.langtest.server.database.userlist.IUserListExerciseJoinDAO;
import mitll.langtest.shared.custom.UserList;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.CommonShell;
import mitll.langtest.shared.exercise.STATE;
import mitll.npdata.dao.DBConnection;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface IUserListManager /*extends IStateManager*/ {
  @Deprecated
  int REVIEW_MAGIC_ID = -100;
  @Deprecated
  int COMMENT_MAGIC_ID = -200;
  @Deprecated
  int ATTN_LL_MAGIC_ID = -300;

  UserList addUserList(int userid, String name, String description, String dliClass, boolean isPublic, int projid);

  int getNumLists(int userid, int projid);

  Collection<UserList<CommonShell>> getMyLists(int userid, int projid);

  Collection<UserList<CommonShell>> getListsForUser(int userid, int projid, boolean listsICreated, boolean visitedLists);

  /**
   * @param userid
   * @param projid
   * @return
   * @see mitll.langtest.server.database.DatabaseImpl#rememberProject(int, int)
   */
  UserList createFavorites(int userid, int projid);

  UserList<CommonShell> getCommentedList(Collection<String> typeOrder, Set<Integer> ids);

  UserList<CommonShell> getAttentionList(Collection<String> typeOrder, Set<Integer> ids);

  UserList<CommonShell> getDefectList(Collection<String> typeOrder, Set<Integer> ids);

  UserList<CommonExercise> getCommentedListEx(Collection<String> typeOrder, Set<Integer> ids);

  UserList<CommonExercise> getAttentionListEx(Collection<String> typeOrder, Set<Integer> ids);

  UserList<CommonExercise> getDefectListEx(Collection<String> typeOrder, Set<Integer> ids);

  @Deprecated
  UserList<CommonShell> getUserListByID(int id, Collection<String> typeOrder, Set<Integer> ids);

  UserList<CommonShell> getSimpleUserListByID(int id);

  UserList<CommonExercise> getUserListByIDExercises(long id, int projid, Collection<String> typeOrder, Set<Integer> ids);

  List<UserList<CommonShell>> getUserListsForText(String search, int userid, int projid);

  UserList getUserList(int userListID);

  void newExercise(int userListID, CommonExercise userExercise, String mediaDir);

  void newExerciseOnList(UserList userList, CommonExercise userExercise, String mediaDir);

  void addItemToList(int userListID, String exerciseID, int exid);

  void editItem(CommonExercise userExercise, String mediaDir, Collection<String> typeOrder);

  UserList addVisitor(int userListID, int user);

  void removeVisitor(int userListID, int user);

  boolean addDefect(int exerciseID, String field, String comment);

  void addAnnotation(int exerciseID, String field, String status, String comment, int userid);


  void addAnnotations(CommonExercise exercise);

  void markState(int exid, STATE state, int creatorID);

  void markCorrectness(int id, boolean correct, int userid);

  boolean deleteList(int id);

  boolean deleteItemFromList(int listid, int exid, Collection<String> typeOrder);

  void setPublicOnList(long userListID, boolean isPublic);

  Collection<Integer> getAudioAnnos();

  IAnnotationDAO getAnnotationDAO();

  IUserListDAO getUserListDAO();

  IUserListExerciseJoinDAO getUserListExerciseJoinDAO();

  void createTables(DBConnection dbConnection, List<String> created);

  IUserExerciseListVisitorDAO getVisitorDAO();
}
