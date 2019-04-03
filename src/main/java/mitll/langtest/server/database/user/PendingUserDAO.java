/*
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2019. Other request for this document shall be referred
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
 * © 2015-2019 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 */

package mitll.langtest.server.database.user;

import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.shared.user.ActiveUser;
import mitll.npdata.dao.DBConnection;
import mitll.npdata.dao.SlickPendingUser;
import mitll.npdata.dao.project.PendingUserDAOWrapper;

import java.sql.Timestamp;
import java.util.List;

/**
 * @see DatabaseImpl#initializeDAOs
 */
public class PendingUserDAO  implements IPendingUserDAO {
//  private static final Logger logger = LogManager.getLogger(PendingUserDAO.class);

  private final PendingUserDAOWrapper dao;

  /**
   * @param dbConnection
   * @see DatabaseImpl#initializeDAOs
   */
  public PendingUserDAO(DBConnection dbConnection) {
    dao = new PendingUserDAOWrapper(dbConnection);
  }

  @Override
  public boolean updateProject(int oldID, int newprojid) {
    return false;
  }

  public void createTable() {
    dao.createTable();
  }

  @Override
  public String getName() {
    return dao.dao().name();
  }

  @Override
  public void insert(int userid, int projid) {
    Timestamp modified = new Timestamp(System.currentTimeMillis());
    dao.insert(new SlickPendingUser(-1, userid, projid, modified, ActiveUser.PENDING.REQUESTED.toString(), modified, -1));
  }

  @Override
  public void update(int id, ActiveUser.PENDING state, int byUser) {
    dao.update(id, state.toString(), byUser);
  }

  @Override
  public List<SlickPendingUser> pendingOnProject(int projid) {
    return dao.pendingOnProject(projid);
  }
}
