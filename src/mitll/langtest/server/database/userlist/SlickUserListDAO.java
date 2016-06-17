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

import mitll.langtest.server.database.DAO;
import mitll.langtest.server.database.Database;
import mitll.langtest.server.database.ISchema;
import mitll.langtest.server.database.user.IUserDAO;
import mitll.langtest.server.database.userexercise.IUserExerciseDAO;
import mitll.langtest.shared.custom.UserList;
import mitll.langtest.shared.exercise.CommonShell;
import mitll.npdata.dao.DBConnection;
import mitll.npdata.dao.SlickUserExerciseList;
import mitll.npdata.dao.userexercise.UserExerciseListDAOWrapper;
import mitll.npdata.dao.userexercise.UserExerciseListVisitorDAOWrapper;
import org.apache.log4j.Logger;
import scala.Option;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class SlickUserListDAO extends DAO implements IUserListDAO, ISchema<UserList<CommonShell>, SlickUserExerciseList> {
  private static final Logger logger = Logger.getLogger(SlickUserListDAO.class);

  private final UserExerciseListDAOWrapper dao;
  private final UserExerciseListVisitorDAOWrapper visitorDAOWrapper;
  private final IUserExerciseDAO userExerciseDAO;

  private final IUserDAO userDAO;

  public SlickUserListDAO(Database database, DBConnection dbConnection, IUserDAO userDAO, IUserExerciseDAO userExerciseDAO) {
    super(database);
    dao = new UserExerciseListDAOWrapper(dbConnection);
    this.userDAO = userDAO;
    this.visitorDAOWrapper = new UserExerciseListVisitorDAOWrapper(dbConnection);
    this.userExerciseDAO = userExerciseDAO;
  }

  public void createTable() {
    dao.createTable();
  }

  @Override
  public SlickUserExerciseList toSlick(UserList<CommonShell> shared, String language) {
    return new SlickUserExerciseList(-1,
        shared.getCreator().getId(),
        shared.getName(),
        shared.getDescription(),
        shared.getClassMarker(),
        new Timestamp(shared.getModified()),
        shared.isPrivate(),
        false,
        (int) shared.getUniqueID());
  }

  @Override
  public UserList<CommonShell> fromSlick(SlickUserExerciseList slick) {
    return new UserList<CommonShell>(
        (long) slick.id(),
        userDAO.getUserWhere(slick.userid()),
        slick.name(),
        slick.description(),
        slick.classmarker(),
        slick.isprivate());
  }

  public void insert(SlickUserExerciseList UserExercise) {
    dao.insert(UserExercise);
  }

  public void addBulk(List<SlickUserExerciseList> bulk) {
    dao.addBulk(bulk);
  }

/*
  public int getNumRows() {
    return dao.getNumRows();
  }
*/

/*  @Override
  public List<UserExercise> getUserExercises() {
    List<SlickUserExercise> all = getAll();
    return getUserExercises(all);
  }*/

  private List<UserList<CommonShell>> fromSlick(Collection<SlickUserExerciseList> all) {
    List<UserList<CommonShell>> copy = new ArrayList<>();
    for (SlickUserExerciseList list : all) copy.add(fromSlick(list));
    return copy;
  }

  @Override
  public void addVisitor(long listid, long userid) {
    visitorDAOWrapper.insert((int) listid, (int) userid);
  }

  @Override
  public void add(UserList userList) {
    int assignedID = dao.insert(toSlick(userList, getLanguage()));
    userList.setUniqueID(assignedID);
  }

  @Override
  public void updateModified(long uniqueID) {
    dao.updateModified((int) uniqueID);
  }

  @Override
  public int getCount() {
    return dao.getNumRows();
  }

  /**
   * Side effect is to add user exercises to lists.
   *
   * @param userid
   * @return
   */
  @Override
  public List<UserList<CommonShell>> getAllByUser(long userid) {
    List<UserList<CommonShell>> userExerciseLists = fromSlick(dao.byUser((int) userid));

    for (UserList<CommonShell> ul : userExerciseLists) {
      populateList(ul);
    }
    return userExerciseLists;
  }

  /**
   * Could get slow.
   *
   * @param where
   */
  private void populateList(UserList<CommonShell> where) {
    where.setExercises(userExerciseDAO.getOnList(where.getUniqueID()));
  }

  /**
   * Expensive ...?
   *
   * @param userid
   * @return non empty public lists
   */
  @Override
  public List<UserList<CommonShell>> getAllPublic(long userid) {
    List<UserList<CommonShell>> userExerciseLists = fromSlick(dao.allPublic());
    populateLists(userExerciseLists, userid);

    List<UserList<CommonShell>> toReturn = new ArrayList<>();
    for (UserList<CommonShell> ul : userExerciseLists) {
      if (!ul.isEmpty()) {
        toReturn.add(ul);
      }
    }

    return toReturn;
  }

  /**
   * @return
   * @throws SQLException
   * @seex #getAll(long)
   * @see #getAllPublic
   * @see #getWhere(long, boolean)
   */
  private void populateLists(Collection<UserList<CommonShell>> lists, long userid) {
    for (UserList<CommonShell> ul : lists) {
      if (userid == -1 || ul.getCreator().getId() == userid || !ul.isFavorite()) {   // skip other's favorites
        populateList(ul);
      }
    }
  }

  @Override
  public boolean hasByName(long userid, String name) {
    return dao.hasByName((int) userid, name);
  }

  @Override
  public List<UserList<CommonShell>> getByName(long userid, String name) {
    return fromSlick(dao.getByName((int) userid, name));
  }

  @Override
  public boolean remove(long unique) {
    return dao.markDeleted((int) unique);
  }

  @Override
  public UserList<CommonShell> getWithExercises(long unique) {
    UserList<CommonShell> where = getWhere(unique, true);
    if (where != null) populateList(where);
    return where;
  }

  @Override
  public UserList<CommonShell> getWhere(long unique, boolean warnIfMissing) {
    Option<SlickUserExerciseList> slickUserExerciseListOption = dao.byID((int) unique);
    if (slickUserExerciseListOption.isDefined()) {
      return fromSlick(slickUserExerciseListOption.get());
    } else {
      if (warnIfMissing) logger.error("getWhere : huh? no user list with id " + unique);
      return null;
    }
  }

  @Override
  public Collection<UserList<CommonShell>> getListsForUser(int userid) {
    List<UserList<CommonShell>> userLists = fromSlick(dao.getVisitedBy(userid));
    populateLists(userLists, userid);
    return userLists;
  }

  @Override
  public void setUserExerciseDAO(IUserExerciseDAO userExerciseDAO) {

  }

  @Override
  public void setPublicOnList(long userListID, boolean isPublic) {
    int i = dao.setPublic((int) userListID, isPublic);
    if (i == 0) logger.error("setPublicOnList : huh? didn't update the userList for " + userListID );

  }

  public UserExerciseListVisitorDAOWrapper getVisitorDAOWrapper() {
    return visitorDAOWrapper;
  }
}
