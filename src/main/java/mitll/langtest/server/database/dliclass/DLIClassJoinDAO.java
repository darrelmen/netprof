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

package mitll.langtest.server.database.dliclass;

import mitll.npdata.dao.DBConnection;
import mitll.npdata.dao.SlickDLIClassJoin;
import mitll.npdata.dao.dliclass.DLIClassJoinWrapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Timestamp;
import java.util.Set;

public class DLIClassJoinDAO implements IDLIClassJoinDAO {
  private static final Logger logger = LogManager.getLogger(DLIClassJoinDAO.class);
  private final DLIClassJoinWrapper dao;

  /**
   * @param dbConnection
   * @paramx database
   * @see mitll.langtest.server.database.DatabaseImpl#initializeDAOs
   */
  public DLIClassJoinDAO(DBConnection dbConnection) {
    dao = new DLIClassJoinWrapper(dbConnection);
  }

  public void createTable() {
    dao.createTable();
  }

  @Override
  public String getName() {
    return dao.dao().name();
  }

  @Override
  public boolean updateProject(int oldID, int newprojid) {
    return false;
  }

  @Override
  public Set<Integer> getStudents(int teacherid) {
    return dao.getStudents(teacherid);
  }

  @Override
  public void insert(int teacherid, int studentid) {
    dao.insert(new SlickDLIClassJoin(-1, new Timestamp(System.currentTimeMillis()), -1, studentid, teacherid));
  }
  @Override
  public void remove(int teacherid, int studentid) {
    dao.remove(teacherid,studentid);
  }
}
