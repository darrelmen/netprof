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

import mitll.npdata.dao.DBConnection;
import mitll.npdata.dao.SlickUserProject;
import mitll.npdata.dao.word.UserProjectDAOWrapper;
import org.apache.log4j.Logger;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;

public class UserProjectDAO implements IUserProjectDAO {
  private static final Logger logger = Logger.getLogger(UserProjectDAO.class);

  private UserProjectDAOWrapper dao;

  public UserProjectDAO(DBConnection dbConnection) {  dao = new UserProjectDAOWrapper(dbConnection);  }

  public void createTable() {
    dao.createTable();
  }

  @Override
  public String getName() {
    return dao.dao().name();
  }

  /**
   * @see mitll.langtest.server.services.UserServiceImpl#userExists(String, String, int)
   * @param userid
   * @param projid
   * @return
   */
  @Override
  public void add(int userid, int projid) {
    //logger.info("UserProjectDAO adding " + userid + " -> " + projid);
    dao.insert(new SlickUserProject(-1, userid, projid, new Timestamp(System.currentTimeMillis())));
  }

  @Override
  public Collection<SlickUserProject> getAll() {
    return dao.getAll();
  }

  @Override
  public int mostRecentByUser(int user) {
    List<SlickUserProject> slickUserProjects = dao.mostRecentByUser(user);
    SlickUserProject next = slickUserProjects.iterator().next();
    logger.info("most recent project for " +user + " is " + next);
    return slickUserProjects.isEmpty() ? -1 : next.projid();
  }

//  @Override
//  public SlickUserProject mostRecentProjectByUser(int user) {
//    List<SlickUserProject> slickUserProjects = dao.mostRecentByUser(user);
//    return slickUserProjects.iterator().next();
//  }
}
