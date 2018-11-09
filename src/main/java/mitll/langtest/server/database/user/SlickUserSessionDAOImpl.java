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

import mitll.langtest.server.database.DAO;
import mitll.langtest.server.database.Database;
import mitll.langtest.server.database.security.IUserSecurityManager;
import mitll.langtest.server.database.security.NPUserSecurityManager;
import mitll.npdata.dao.DBConnection;
import mitll.npdata.dao.SlickUserSession;
import mitll.npdata.dao.user.UserSessionDAOWrapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import scala.Tuple2;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class SlickUserSessionDAOImpl extends DAO implements IUserSessionDAO {
 // private static final Logger logger = LogManager.getLogger(SlickUserSessionDAOImpl.class);
  private final UserSessionDAOWrapper dao;
  private final IUserProjectDAO userProjectDAO;

  /**
   * @param database
   * @param userProjectDAO
   * @param dbConnection
   * @see mitll.langtest.server.database.DatabaseImpl#initializeDAOs
   */
  public SlickUserSessionDAOImpl(Database database, IUserProjectDAO userProjectDAO, DBConnection dbConnection) {
    super(database);
    dao = new UserSessionDAOWrapper(dbConnection);
    this.userProjectDAO = userProjectDAO;
  }

  public void createTable() {
    dao.createTable();
  }

  @Override
  public String getName() {
    return dao.dao().name();
  }

  /**
   * @param user
   * @return
   * @see IUserSecurityManager#setSessionUser
   */
  @Override
  public void add(SlickUserSession user) {
    dao.add(user);
  }

  /**
   * @param sesssion
   * @return -1 if not in the database
   */
  @Override
  public int getUserForSession(String sesssion) {
    Collection<Integer> userForSession = dao.getUserForSession(sesssion);
    return userForSession.isEmpty() ? -1 : userForSession.iterator().next();
  }

  @Override
  public boolean updateVisitedForSession(String session) {
    return dao.updateVisitedForSession(session);
  }

/*
  @Override
  public int getUserForSV(String sesssion, String v) {
    Collection<Integer> userForSession = dao.getUserForSV(sesssion, v);
    return userForSession.isEmpty() ? -1 : userForSession.iterator().next();
  }*/

  /**
   * @paramx session
   * @see NPUserSecurityManager#logoutUser
   */
  @Override
  public void removeAllSessionsForUser(int userId) {
    dao.removeAllSessionsForUser(userId);
  }

  @Override
  public Map<Integer, ActiveInfo> getActiveSince(long when) {
    return getUserToActiveInfo(dao.since(new Timestamp(when)));
  }

  /**
   * @return
   * @see NPUserSecurityManager#getActiveTeachers
   */
  @Override
  public Map<Integer, ActiveInfo> getActive() {
    return getUserToActiveInfo(dao.all());
  }

  @Override
  public Map<Integer, ActiveInfo> getActiveFrom(Set<Integer> userIDs) {
    return getUserToActiveInfoSimple(userIDs);
  }

  @NotNull
  private Map<Integer, ActiveInfo> getUserToActiveInfo(Map<Integer, Tuple2<Long, Long>> since) {
    Map<Integer, Integer> usersToProject = userProjectDAO.getUsersToProject(since.keySet());

    Map<Integer, ActiveInfo> userToActiveInfo = new HashMap<>();

    since.forEach((k, pair) -> {
      Long loggedInTime = pair._1();
      Long visited = pair._2();

      ActiveInfo value = new ActiveInfo(k, loggedInTime, visited, usersToProject.getOrDefault(k, -1));
      // logger.info("getActiveSince " + k + "->" + value);
      userToActiveInfo.put(k, value);
    });
    return userToActiveInfo;
  }


  @NotNull
  private Map<Integer, ActiveInfo> getUserToActiveInfoSimple(Set<Integer> userIDs) {
    Map<Integer, ActiveInfo> userToActiveInfo = new HashMap<>();
    Map<Integer, Tuple2<Integer, Long>> usersToProjectAndTime = userProjectDAO.getUsersToProjectAndTime(userIDs);

  //  logger.info("getUserToActiveInfoSimple got back " + usersToProjectAndTime.size());

    usersToProjectAndTime.forEach((k, pair) -> {
      Integer projectID = pair._1();
      Long visited = pair._2();

      ActiveInfo value = new ActiveInfo(k, visited, visited, projectID);
      // logger.info("getActiveSince " + k + "->" + value);
      userToActiveInfo.put(k, value);
    });
    return userToActiveInfo;
  }
}