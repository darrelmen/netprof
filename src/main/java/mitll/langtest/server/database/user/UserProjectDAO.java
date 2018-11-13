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

import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.server.database.project.ProjectServices;
import mitll.npdata.dao.DBConnection;
import mitll.npdata.dao.SlickUserProject;
import mitll.npdata.dao.project.UserProjectDAOWrapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import scala.Tuple2;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @see mitll.langtest.server.database.DatabaseImpl#initializeDAOs
 */
public class UserProjectDAO implements IUserProjectDAO {
  private static final Logger logger = LogManager.getLogger(UserProjectDAO.class);
  private final UserProjectDAOWrapper dao;

//  private final ConcurrentHashMap<Integer, Integer> userToProjectCache = new ConcurrentHashMap<>();

  /**
   * @param dbConnection
   * @see mitll.langtest.server.database.DatabaseImpl#initializeDAOs
   */
  public UserProjectDAO(DBConnection dbConnection) {
    dao = new UserProjectDAOWrapper(dbConnection);
  }

  public void createTable() {
    dao.createTable();
  }

  @Override
  public boolean updateProject(int oldID, int newprojid) {
    return dao.updateProject(oldID, newprojid) > 0;
  }

  @Override
  public String getName() {
    return dao.dao().name();
  }

  /**
   * @param bulk
   * @see mitll.langtest.server.database.copy.UserCopy#addUserProjectBinding
   */
  public void addBulk(Collection<SlickUserProject> bulk) {
    dao.addBulk(bulk);
  }

  /**
   * @param bulk
   * @see mitll.langtest.server.database.copy.UserCopy#addUserProjectBinding
   */
  @Override
  public void forgetUsersBulk(Collection<Integer> bulk) {
    dao.forgetUsersBulk(bulk);
  }

  /**
   * A no-op if the current project for the user is as expected, but will switch project if not.
   *
   * Only way this should return false if if the user is logged out and we assume we check the session before we get here.
   *
   * @param userid
   * @param projid
   * @see mitll.langtest.server.services.OpenUserServiceImpl#setCurrentUserToProject
   */
  @Override
  public int setCurrentProjectForUser(int userid, int projid) {
    int mostRecentByUser = getCurrentProjectForUser(userid);

    if (mostRecentByUser == -1) { // they logged out!
      logger.info("setCurrentProjectForUser no most recent project " + mostRecentByUser + " did they log out?");
    }

    if (mostRecentByUser != projid) {
      logger.info("setCurrentProjectForUser switched tabs, was " + mostRecentByUser + " but now will be " + projid);
      upsert(userid, projid);
    } else {
      // logger.info("OK, just confirming current project for " + getCurrentProjectForUser + " is " + projid);
    }
    return mostRecentByUser;
  }

  /**
   * @param userid
   * @param projid
   * @return
   * @see #setCurrentProjectForUser
   * @see ProjectServices#rememberUsersCurrentProject
   */
  @Override
  public void upsert(int userid, int projid) {
    dao.upsert(new SlickUserProject(-1, userid, projid, new Timestamp(System.currentTimeMillis())));

    // remember
    //userToProjectCache.put(userid, projid);
  }

  /**
   * TODO - we seem to hit this too often
   *
   * @param user
   * @return -1 if has no project
   * @see mitll.langtest.server.database.DatabaseImpl#projectForUser
   */
  @Override
  public int getCurrentProjectForUser(int user) {
    Integer project = null;//userToProjectCache.get(user);

    if (project == null) {
      long then = System.currentTimeMillis();
      List<Integer> slickUserProjects = dao.mostRecentByUser(user);
      long now = System.currentTimeMillis();
//    logger.info("getCurrentProjectForUser : took " + (now - then) + " to get current prpject for user  " + user + " = " + slickUserProjects);
      int i = slickUserProjects.isEmpty() ? -1 : slickUserProjects.iterator().next();

      if (i == -1) {
        logger.info("getCurrentProjectForUser no current project for user #" + user);
      }

//      userToProjectCache.put(user, i);

      return i;
    } else {
      return project;
    }
  }

  /**
   * @param userid
   * @see mitll.langtest.server.services.OpenUserServiceImpl#forgetProject
   */
  @Override
  public void forget(int userid) {
  //  userToProjectCache.remove(userid);
    dao.forget(userid);
  }

  /**
   * @return
   * @see DatabaseImpl#getReport
   */
  @Override
  public Map<Integer, Integer> getUserToProject() {
    return dao.getUserToProject();
  }

  /**
   * @param projid
   * @return
   * @see mitll.langtest.server.database.project.ProjectManagement#rememberUsers(int)
   */
  @Override
  public Collection<Integer> getUsersForProject(int projid) {
    return dao.usersOnProject(projid);
  }

  @Override
  public Map<Integer, Integer> getUsersToProject(Collection<Integer> userids) {
    return dao.getUserToProjects(userids);
  }

  @Override
  public Map<Integer, Tuple2<Integer, Long>> getUsersToProjectAndTime(Collection<Integer> userids) {
    return dao.getUserToProjectAndTime(userids);
  }
}
