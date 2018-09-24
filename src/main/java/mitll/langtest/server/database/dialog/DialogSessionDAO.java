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

package mitll.langtest.server.database.dialog;

import mitll.langtest.client.custom.INavigation;
import mitll.langtest.server.database.DAO;
import mitll.langtest.server.database.Database;
import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.server.database.exercise.Project;
import mitll.langtest.shared.dialog.*;
import mitll.npdata.dao.*;
import mitll.npdata.dao.dialog.DialogSessionDAOWrapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;

public class DialogSessionDAO extends DAO implements IDialogSessionDAO {
  private static final Logger logger = LogManager.getLogger(DialogSessionDAO.class);


  private final DialogSessionDAOWrapper dao;

  // private final DatabaseImpl databaseImpl;

  /**
   * @param database
   * @param dbConnection
   * @see DatabaseImpl#initializeDAOs
   */
  public DialogSessionDAO(Database database, DBConnection dbConnection) {
    super(database);
    dao = new DialogSessionDAOWrapper(dbConnection);
  }

  // TODO : may need this later
/*
  public SlickDialog getByID(int projid) {
    Collection<SlickDialog> SlickDialogs = dao.byID(projid);
    return SlickDialogs.isEmpty() ? null : SlickDialogs.iterator().next();
  }
*/

  /**
   * join with attributes = meta data from domino
   *
   * join with exercises, in order
   *
   * TODO : join with aggregate score
   *
   * join with images
   *
   * @param userid
   * @return
   */
  @Override
  public List<IDialogSession> getDialogSessions(int userid, int dialogid) {
    return getiDialogSessions(getByUserAndDialog(userid, dialogid));
  }

  @Override
  public List<IDialogSession> getCurrentDialogSessions(int userid) {
    return getiDialogSessions(dao.byUser(userid));
  }

  @NotNull
  private List<IDialogSession> getiDialogSessions(Collection<SlickDialogSession> byProjID) {
    return byProjID.stream().map(ds -> {
      INavigation.VIEWS views = getViews(ds);
      return new DialogSession(ds.id(),
          ds.userid(),
          ds.projid(),
          ds.dialogid(),
          ds.modified().getTime(),
          ds.end().getTime(),
          views,
          DialogStatus.valueOf(ds.status()),
          ds.numrecordings(),
          ds.score(),
          ds.speakingrate()
      );
    }).collect(Collectors.toList());
  }

  @NotNull
  private INavigation.VIEWS getViews(SlickDialogSession ds) {
    INavigation.VIEWS views = null;
    try {
      views = INavigation.VIEWS.valueOf(ds.view().toUpperCase());
    } catch (IllegalArgumentException e) {
      logger.warn("can't parse " + ds.view().toUpperCase());
      views = INavigation.VIEWS.REHEARSE;
    }
    return views;
  }

  @Override
  public List<IDialogSession> getLatestDialogSessions(int projid, int userid) {
    return getiDialogSessions(getByProjAndUser(projid, userid));
  }

  // helpful in the summary view
  private Collection<SlickDialogSession> getByProjAndUser(int projid, int userid) {
    return dao.byProjAndUser(projid, userid);
  }

  private Collection<SlickDialogSession> getByUserAndDialog(int userid, int dialogid) {
    return dao.byUserAndDialog(userid, dialogid);
  }

  public void createTable() {
    dao.createTable();
  }

  @Override
  public String getName() {
    return dao.dao().name();
  }

  /**
   * @param modified
   * @param status
   * @return
   * @see mitll.langtest.server.database.project.DialogPopulate#populateDatabase(Project)
   */
  @Override
  public int add(int userid,
                 int projid,
                 int dialogid,


                 long modified,
                 long end,
                 INavigation.VIEWS views,
                 DialogStatus status,
                 int numRecordings,
                 float score,
                 float speakingRate
  ) {
    return dao.insert(new SlickDialogSession(
        -1,
        userid,
        projid,
        dialogid,
        new Timestamp(modified),
        new Timestamp(end),
        views.toString(),
        status.toString(),
        numRecordings,
        score,
        speakingRate
    ));
  }

  /**
   * @param ds
   * @see mitll.langtest.server.services.DialogServiceImpl#addSession
   */
  @Override
  public void add(DialogSession ds) {
    logger.info("Add session " + ds);

    dao.insert(new SlickDialogSession(
        -1,
        ds.getUserid(),
        ds.getProjid(),
        ds.getDialogid(),
        new Timestamp(ds.getModified()),
        new Timestamp(ds.getEnd()),
        ds.getView().toString(),
        ds.getStatus().toString(),
        ds.getNumRecordings(),
        ds.getScore(),
        ds.getSpeakingRate()
    ));
  }

  /**
   * FOR REAL
   *
   * @param projid
   */
  @Override
  public void removeForProject(int projid) {
    dao.removeForProject(projid);
  }
}
