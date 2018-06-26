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

import mitll.langtest.server.database.DAO;
import mitll.langtest.server.database.Database;
import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.server.database.userexercise.IUserExerciseDAO;
import mitll.npdata.dao.DBConnection;
import mitll.npdata.dao.SlickDialog;
import mitll.npdata.dao.dialog.DialogDAOWrapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Timestamp;
import java.util.*;

public class DialogDAO extends DAO implements IDialogDAO {
  private static final Logger logger = LogManager.getLogger(DialogDAO.class);

  private static final String DEFAULT_DIALOG = "DEFAULT_DIALOG";
  public static final long MIN = 60 * 1000L;
  public static final long HOUR = 60 * MIN;
  public static final long DAY = 24 * HOUR;
  public static final long YEAR = 365 * DAY;

  private final DialogDAOWrapper dao;
  //  private final ProjectPropertyDAO propertyDAO;
  // private final IUserExerciseDAO userExerciseDAO;
  private DatabaseImpl databaseImpl;

  /**
   * @param database
   * @param dbConnection
   * @param userExerciseDAO
   * @see DatabaseImpl#initializeDAOs
   */
  public DialogDAO(Database database,
                   DBConnection dbConnection,
                   IUserExerciseDAO userExerciseDAO,
                   DatabaseImpl databaseImpl) {
    super(database);
    //  propertyDAO = new ProjectPropertyDAO(database, dbConnection);
    dao = new DialogDAOWrapper(dbConnection);
//    this.userExerciseDAO = userExerciseDAO;
    this.databaseImpl = databaseImpl;
    ensureDefault(databaseImpl.getUserDAO().getDefaultUser());
  }

  public int ensureDefault(int defaultUser) {
    SlickDialog defaultProject = getDefaultDialog();
    if (defaultProject == null) {
      add(defaultUser,
          databaseImpl.getProjectDAO().getDefault(),
          -1,
          1,//databaseImpl.getImageDAO().getDefault(),
          System.currentTimeMillis(),
          System.currentTimeMillis(),
          DialogType.DEFAULT,
          "",
          "", "", "", "", 2, 0, "", "", "", ""
      );
      defaultProject = getDefaultDialog();
      return defaultProject == null ? -1 : defaultProject.id();
    } else {
      return defaultProject.id();
    }
  }

  public int getDefault() {
    SlickDialog defaultProject = getDefaultDialog();
    return defaultProject == null ? -1 : defaultProject.id();
  }

  private SlickDialog getDefaultDialog() {
    Collection<SlickDialog> aDefault = dao.getDefault();
    if (aDefault.isEmpty()) {
      return null;
    } else {
      return aDefault.iterator().next();
    }
  }

  @Override
  public boolean exists(int projid) {
    Collection<SlickDialog> SlickDialogs = dao.byID(projid);
    return !SlickDialogs.isEmpty();
  }

  public SlickDialog getByID(int projid) {
    Collection<SlickDialog> SlickDialogs = dao.byID(projid);
    return SlickDialogs.isEmpty() ? null : SlickDialogs.iterator().next();
  }

  /**
   * Don't update the project properties...
   *
   * @param changed
   * @return
   * @see mitll.langtest.server.domino.ProjectSync#updateProjectIfSomethingChanged
   */
  @Override
  public boolean easyUpdate(SlickDialog changed) {
    return dao.update(changed) > 0;
  }

/*  @Override
  public boolean easyUpdateNetprof(SlickDialog changed, long sinceWhen) {
    changed.updateNetprof(sinceWhen);
    return dao.update(changed) > 0;
  }*/

  /**
   * Why some things are slots on SlickDialog and why some things are project properties is kinda arbitrary...
   *
   * @paramx userid
   * @paramx projectInfo
   * @return
   * @see mitll.langtest.server.services.ProjectServiceImpl#update
   * @see mitll.langtest.client.project.ProjectEditForm#updateProject
   */
/*
  @Override
  public boolean update(int userid, SlickDialog projectInfo) {
    int projid = projectInfo.getID();
    Project currentProject = database.getProject(projid);

    int dominoID = projectInfo.getDominoID();
    SlickDialog changed = new SlickDialog(
        projid,
        userid,
        new Timestamp(projectInfo.getCreated()),   // created
        new Timestamp(System.currentTimeMillis()), // modified - now!
        new Timestamp(projectInfo.getLastImport()),// last import - maintain it
        new Timestamp(projectInfo.getLastNetprof()),// last netprof 1 update - maintain it
        projectInfo.getName(),
        projectInfo.getLanguage(),
        projectInfo.getCourse(),
        projectInfo.getProjectType().toString(),
        projectInfo.getStatus().toString(),
        projectInfo.getFirstType(),
        projectInfo.getSecondType(),
        getCountryCode(projectInfo),
        currentProject.getProject().ltsClass(),
        dominoID,
        projectInfo.getDisplayOrder()
    );

    boolean differentDomino = dominoID != currentProject.getProject().dominoid();
    if (differentDomino) {
      logger.info("changed domino project to " + dominoID);
    }
    boolean checkForDominoIDs = differentDomino || userExerciseDAO.areThereAnyUnmatched(projid);

    if (checkForDominoIDs) {
      setDominoIDOnExercises(projid, dominoID);
    }

    boolean didUpdate = easyUpdate(changed);
    if (!didUpdate) logger.error("update : couldn't update " + changed);
    boolean updateProps = updateProperties(projectInfo);
    boolean didChange = didUpdate || updateProps;

    if (didChange) {
      currentProject.clearPropCache();
//      logger.info("update for " + projid);
    } else {
      logger.warn("update : didn't update " + projectInfo + " for current " + currentProject);
    }

    return didChange;
  }
*/
  public void createTable() {
    dao.createTable();
  }

  @Override
  public String getName() {
    return dao.dao().name();
  }


  /**
   * Really does delete it - could take a long time if a big project.
   * <p>
   * Mainly called from drop.sh or tests
   * In general we want to retire projects when we don't want them visible.
   *
   * @param id
   * @see mitll.langtest.server.database.copy.CopyToPostgres#dropOneConfig
   */
  public boolean delete(int id) {
    // logger.info("delete project #" + id);
    return dao.delete(id) > 0;
  }

  public boolean deleteAllBut(int id) {
    // logger.info("delete project #" + id);
    return dao.deleteAllBut(id) > 0;
  }

  /**
   * TODO : consider adding lts class
   * TODO : consider adding domino project id
   *
   * @param userid
   * @param modified
   * @param dominoID
   * @return
   * @see #ensureDefault
   */
  @Override
  public int add(int userid,
                 int projid,
                 int dominoID,
                 int imageID,

                 long modified,
                 long lastimport,
                 DialogType kind,
                 String status,
                 String fltitle,
                 String entitle,
                 String flpresentation,
                 String enpresentation,
                 int numSpeakers,

                 float ilr,
                 String orientation,
                 String audio,
                 String passage,
                 String translation
  ) {
    return dao.insert(new SlickDialog(
        -1,
        userid,
        projid,
        dominoID,
        imageID,
        new Timestamp(modified),
        new Timestamp(lastimport),
        kind.toString(),
        status,
        fltitle,
        entitle,
        flpresentation,
        enpresentation,
        numSpeakers,
        ilr,
        orientation,
        audio,
        passage,
        translation
    ));
  }

  /**
   * @return
   * @see DatabaseImpl#getReport
   */
  @Override
  public Collection<SlickDialog> getAll() {
    return dao.getAll();
  }

  @Override
  public int getNum() {
    return dao.countAll();
  }
}
