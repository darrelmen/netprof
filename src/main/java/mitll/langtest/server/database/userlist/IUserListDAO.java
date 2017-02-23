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
import mitll.langtest.server.database.userexercise.IUserExerciseDAO;
import mitll.langtest.shared.custom.UserList;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.CommonShell;

import java.util.Collection;
import java.util.List;

public interface IUserListDAO extends IDAO {
  /**
   * @see mitll.langtest.server.database.custom.UserListManager#addVisitor(long, long)
   * @param listid
   * @param userid
   */
  void addVisitor(long listid, long userid);

  void add(UserList<CommonShell> userList, int projid);

  void updateModified(long uniqueID);

  void updateContext(long uniqueID, String context);

  int getCount();

  List<UserList<CommonShell>> getAllByUser(long userid, int projectID);

  List<UserList<CommonShell>> getAllPublic(long userid, int projectID);

  boolean hasByName(long userid, String name, int projid);

  List<UserList<CommonShell>> getByName(long userid, String name, int projid);

  boolean remove(long unique);

  UserList<CommonShell> getWithExercises(long unique);
  UserList<CommonExercise> getWithExercisesEx(long unique);

  UserList<CommonShell> getWhere(long unique, boolean warnIfMissing);

  Collection<UserList<CommonShell>> getListsForUser(int userid, int projid);

  void setUserExerciseDAO(IUserExerciseDAO userExerciseDAO);

  void setPublicOnList(long userListID, boolean isPublic);
}
