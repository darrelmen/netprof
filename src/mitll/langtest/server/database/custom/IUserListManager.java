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
import mitll.langtest.server.database.reviewed.IReviewedDAO;
import mitll.langtest.server.database.reviewed.StateCreator;
import mitll.langtest.server.database.userlist.IUserExerciseListVisitorDAO;
import mitll.langtest.server.database.userlist.IUserListDAO;
import mitll.langtest.server.database.userlist.IUserListExerciseJoinDAO;
import mitll.langtest.shared.custom.UserList;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.CommonShell;
import mitll.langtest.shared.exercise.STATE;
import mitll.langtest.shared.exercise.Shell;
import mitll.npdata.dao.DBConnection;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface IUserListManager {
  public static final int REVIEW_MAGIC_ID = -100;
  public static final int COMMENT_MAGIC_ID = -200;
  public static final int ATTN_LL_MAGIC_ID = -300;

  void setStateOnExercises();

  Map<Integer, StateCreator> getExerciseToState(boolean skipUnset);

  void markState(Collection<? extends CommonShell> shells);

  long addUserList(int userid, String name, String description, String dliClass, boolean isPublic);

  Collection<UserList<CommonShell>> getMyLists(int userid);

  Collection<UserList<CommonShell>> getListsForUser(int userid, boolean listsICreated, boolean visitedLists);

  UserList createFavorites(int userid);

  UserList<CommonShell> getCommentedList(Collection<String> typeOrder);

  UserList<CommonShell> getAttentionList(Collection<String> typeOrder);

  UserList<CommonShell> getDefectList(Collection<String> typeOrder);

  List<UserList<CommonShell>> getUserListsForText(String search, int userid);

  void reallyCreateNewItem(long userListID, CommonExercise userExercise, String mediaDir);

  void addItemToList(long userListID, String exerciseID, int exid);

  void editItem(CommonExercise userExercise,
                boolean createIfDoesntExist,
                String mediaDir);

  CommonExercise duplicate(CommonExercise userExercise);

  UserList<CommonShell> getUserListByID(long id, Collection<String> typeOrder);

  void addVisitor(long userListID, long user);

  boolean addDefect(String exerciseID, String field, String comment);

  void addAnnotation(int exerciseID, String field, String status, String comment, int userid);

  void addAnnotations(CommonExercise exercise);

  void markState(String id, STATE state, int creatorID);

  void setState(Shell shell, STATE state, long creatorID);

  void setSecondState(Shell shell, STATE state, long creatorID);

  STATE getCurrentState(int exerciseID);

  void removeReviewed(int exerciseid);

  void markCorrectness(String id, boolean correct, int userid);

  boolean deleteList(long id);

  boolean deleteItemFromList(long listid, int exid, Collection<String> typeOrder);

  void setPublicOnList(long userListID, boolean isPublic);

  Collection<Integer> getAudioAnnos();

  IAnnotationDAO getAnnotationDAO();

  IUserListDAO getUserListDAO();

  IUserListExerciseJoinDAO getUserListExerciseJoinDAO();

  IReviewedDAO getReviewedDAO();

  IReviewedDAO getSecondStateDAO();

  void createTables(DBConnection dbConnection, List<String> created);

  IUserExerciseListVisitorDAO getVisitorDAO();
}
