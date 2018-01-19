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

package mitll.langtest.server.database.user;

import mitll.langtest.server.PathHelper;
import mitll.langtest.server.database.project.ProjectServices;
import mitll.npdata.dao.DBConnection;
import mitll.npdata.dao.SlickUserProject;
import mitll.npdata.dao.word.UserProjectDAOWrapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class UserProjectDAO implements IUserProjectDAO {
  //private static final Logger logger = LogManager.getLogger(UserProjectDAO.class);
  private UserProjectDAOWrapper dao;

  /**
   * @see mitll.langtest.server.database.DatabaseImpl#initializeDAOs
   * @param dbConnection
   */
  public UserProjectDAO(DBConnection dbConnection) {
    dao = new UserProjectDAOWrapper(dbConnection);
  }

  public void createTable() {
    dao.createTable();
  }

  @Override
  public String getName() {
    return dao.dao().name();
  }

  /**
   * @param userid
   * @param projid
   * @return
   * @see ProjectServices#rememberUsersCurrentProject(int, int)
   */
  @Override
  public void add(int userid, int projid) {
    dao.insert(new SlickUserProject(-1, userid, projid, new Timestamp(System.currentTimeMillis())));
  }

  public void addBulk(Collection<SlickUserProject> bulk) {
    dao.addBulk(bulk);
  }

  @Override
  public void forgetUsersBulk(Collection<Integer> bulk) {
    dao.forgetUsersBulk(bulk);
  }

  @Override
  public void forget(int userid) {
    dao.forget(userid);
  }

  /**
   * TODO - we seem to hit this too often
   *
   * @param user
   * @return -1 if has no project
   * @see mitll.langtest.server.database.DatabaseImpl#projectForUser
   */
  @Override
  public int mostRecentByUser(int user) {
    List<Integer> slickUserProjects = dao.mostRecentByUser(user);
    return slickUserProjects.isEmpty() ? -1 : slickUserProjects.iterator().next();
  }

  @Override
  public Map<Integer, Integer> getUserToProject() {
    return dao.getUserToProject();
  }

  @Override
  public Collection<Integer> getUsersForProject(int projid) {
    return dao.usersOnProject(projid);
  }
}
