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

package mitll.langtest.server.database.dialog;

import mitll.langtest.server.database.DAO;
import mitll.langtest.server.database.Database;
import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.shared.dialog.DialogStatus;
import mitll.npdata.dao.DBConnection;
import mitll.npdata.dao.SlickRelatedResult;
import mitll.npdata.dao.dialog.RelatedResultDAOWrapper;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

public class RelatedResultDAO extends DAO implements IRelatedResultDAO {
  // private static final Logger logger = LogManager.getLogger(RelatedResultDAO.class);
  private final RelatedResultDAOWrapper dao;

  /**
   * @param database
   * @param dbConnection
   * @see DatabaseImpl#makeDialogDAOs
   */
  public RelatedResultDAO(Database database, DBConnection dbConnection) {
    super(database);
    dao = new RelatedResultDAOWrapper(dbConnection);
  }

  /**
   * @param resultid
   * @param dialogsessionid
   * @return
   * @see mitll.langtest.server.audio.AudioFileHelper#rememberAnswer
   */
  @Override
  public int add(int resultid, int dialogsessionid) {
    return dao.insert(new SlickRelatedResult(
        -1,
        resultid,
        dialogsessionid, DialogStatus.DEFAULT.toString(), new Timestamp(System.currentTimeMillis())
    ));
  }

  @Override
  public Map<Integer, List<SlickRelatedResult>> getByProjectForDialogForUser(int projid, int dialogid, int userid) {
    return dao.byProjectForDialogForUser(projid, dialogid, userid);
  }

  @Override
  public SlickRelatedResult latestByProjectForDialogForUser(int projid, int dialogid, int userid) {
    List<SlickRelatedResult> slickRelatedResults = dao.latestByProjectForDialogForUser(projid, dialogid, userid);
    if (slickRelatedResults.isEmpty()) return null;
    else return slickRelatedResults.iterator().next();
  }

  // only marks status as deleted
  @Override
  public void removeForProject(int id) {
    dao.deleteForProject(id);
  }

  @Override
  public void createTable() {
    dao.createTable();
  }

  @Override
  public String getName() {
    return dao.dao().name();
  }
}
