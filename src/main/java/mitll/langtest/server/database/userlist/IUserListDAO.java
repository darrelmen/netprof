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

package mitll.langtest.server.database.userlist;

import mitll.langtest.server.database.IDAO;
import mitll.langtest.server.database.custom.IUserListManager;
import mitll.langtest.server.database.userexercise.IUserExerciseDAO;
import mitll.langtest.shared.custom.UserList;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.CommonShell;
import mitll.npdata.dao.SlickUserExerciseList;

import java.util.Collection;
import java.util.List;

public interface IUserListDAO extends IDAO {
  void updateModified(long uniqueID);

  /**
   * @param uniqueID
   * @param context
   * @see mitll.langtest.server.services.ListServiceImpl#updateContext(long, String)
   */
/*
  void updateContext(long uniqueID, String context);

  void updateRichText(long uniqueID, String richText);

  void updateName(long id, String name);
*/

  /**
   * @param listid
   * @param userid
   * @see IUserListManager#addVisitor(int, int)
   */
  void addVisitor(int listid, int userid);

  void removeVisitor(int listid, int userid);

  void add(UserList<CommonShell> userList, int projid);

  /**
   * JUST FOR DEBUGGING
   *
   * @return
   * @see mitll.langtest.server.database.custom.UserListManager#createUserList
   */
  int getCount();

  List<UserList<CommonShell>> getAllByUser(int userid, int projectID);

  List<UserList<CommonShell>> getAllPublic(long userid, int projectID);

  boolean hasByName(long userid, String name, int projid);

  List<UserList<CommonShell>> getByName(long userid, String name, int projid);

  boolean remove(int unique);

  void bringBack(long unique);

  UserList<CommonShell> getWithExercises(int unique);

  UserList<CommonExercise> getWithExercisesEx(int unique);

  UserList<CommonExercise> getList(int unique);

  UserList<CommonShell> getWhere(int unique, boolean warnIfMissing);

  Collection<UserList<CommonShell>> getAllPublicNotMine(int userid, int projid);

  /**
   * @see mitll.langtest.server.database.custom.UserListManager#getListsForUser(int, int, boolean, boolean)
   * @param userid
   * @param projid
   * @return
   */
  Collection<UserList<CommonShell>> getVisitedLists(int userid, int projid);

  Collection<SlickUserExerciseList> getByUser(int userid, int projid);

  /**
   * @see mitll.langtest.server.database.custom.UserListManager#getRawLists
   * @see SlickUserListDAO#getVisitedLists
   * @param userid
   * @param projid
   * @return
   */
  Collection<SlickUserExerciseList> getVisitedBy(int userid, int projid);

  void setUserExerciseDAO(IUserExerciseDAO userExerciseDAO);

  int getNumMineAndPublic(int userid, int projid);

  void setPublicOnList(long userListID, boolean isPublic);

  void update(UserList<?> userList);
}
