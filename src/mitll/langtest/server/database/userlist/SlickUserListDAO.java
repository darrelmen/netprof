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
import mitll.langtest.server.database.exercise.ExerciseDAO;
import mitll.langtest.server.database.user.IUserDAO;
import mitll.langtest.server.database.userexercise.IUserExerciseDAO;
import mitll.langtest.shared.custom.UserList;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.CommonShell;
import mitll.npdata.dao.DBConnection;
import mitll.npdata.dao.SlickUserExercise;
import mitll.npdata.dao.SlickUserExerciseList;
import mitll.npdata.dao.userexercise.UserExerciseListDAOWrapper;
import org.apache.log4j.Logger;
import scala.collection.Seq;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class SlickUserListDAO
    extends DAO implements IUserListDAO, ISchema<UserList, SlickUserExerciseList> {
  private static final Logger logger = Logger.getLogger(SlickUserListDAO.class);

  private final UserExerciseListDAOWrapper dao;

  // Collection<String> typeOrder;
  IUserDAO userDAO;

  public SlickUserListDAO(Database database, DBConnection dbConnection, IUserDAO userDAO) {
    super(database);
    dao = new UserExerciseListDAOWrapper(dbConnection);
    this.userDAO = userDAO;
    //   this.typeOrder = database.getTypeOrder();
    //   this.exerciseDAO = exerciseDAO;
  }

  public void createTable() {
    dao.createTable();
  }

/*
  public void dropTable() {
    dao.drop();
  }
*/

  @Override
  public SlickUserExerciseList toSlick(UserList shared, String language) {
    return new SlickUserExerciseList(-1,
        shared.getCreator().getId(),
        shared.getName(),
        shared.getDescription(),
        shared.getClassMarker(),
        shared.isPrivate(),
        false,
        (int) shared.getUniqueID());
  }

  @Override
  public UserList fromSlick(SlickUserExerciseList slick) {
    return new UserList(
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

  List<UserList> getUserExercises(List<SlickUserExerciseList> all) {
    List<UserList> copy = new ArrayList<>();
    for (SlickUserExerciseList UserExercise : all) copy.add(fromSlick(UserExercise));
    return copy;
  }

  @Override
  public void addVisitor(long listid, long userid) {

  }

  @Override
  public void add(UserList userList) {

  }

  @Override
  public void updateModified(long uniqueID) {

  }

  @Override
  public int getCount() {
    return 0;
  }

  @Override
  public List<UserList<CommonShell>> getAllByUser(long userid) {
    return null;
  }

  @Override
  public List<UserList<CommonShell>> getAllPublic(long userid) {
    return null;
  }

  @Override
  public boolean hasByName(long userid, String name) {
    return false;
  }

  @Override
  public List<UserList<CommonShell>> getByName(long userid, String name) {
    return null;
  }

  @Override
  public boolean remove(long unique) {
    return false;
  }

  @Override
  public UserList<CommonShell> getWithExercises(long unique) {
    return null;
  }

  @Override
  public UserList<CommonShell> getWhere(long unique, boolean warnIfMissing) {
    return null;
  }

  @Override
  public Collection<UserList<CommonShell>> getListsForUser(long userid) {
    return null;
  }

  @Override
  public void setUserExerciseDAO(IUserExerciseDAO userExerciseDAO) {

  }

  @Override
  public void setPublicOnList(long userListID, boolean isPublic) {

  }
}
