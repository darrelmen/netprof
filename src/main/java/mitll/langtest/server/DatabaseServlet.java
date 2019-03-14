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

package mitll.langtest.server;

import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.server.database.audio.EnsureAudioHelper;
import mitll.langtest.server.database.audio.IEnsureAudioHelper;
import mitll.langtest.server.database.exercise.Project;
import mitll.langtest.server.database.security.IUserSecurityManager;
import mitll.langtest.server.services.MyRemoteServiceServlet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;

public class DatabaseServlet extends HttpServlet {
  private static final Logger logger = LogManager.getLogger(DatabaseServlet.class);
  DatabaseImpl db = null;
  PathHelper pathHelper;
  IUserSecurityManager securityManager;
  IEnsureAudioHelper ensureAudioHelper;

  /**
   * @see DownloadServlet#init
   */
  void setPaths() {
    pathHelper = getPathHelper();
    ensureAudioHelper = new EnsureAudioHelper(db, pathHelper);
  }

  private PathHelper getPathHelper() {
    DatabaseImpl database = getDatabase();
    if (database == null) {
      logger.warn("huh? no database?");
      return null;
    }
    ServerProperties serverProps = database.getServerProps();
    if (serverProps == null) throw new IllegalArgumentException("getPathHelper huh? props is null?");
    return new PathHelper(getServletContext(), serverProps);
  }

  /**
   * @param request
   * @return
   * @see #doGet
   */
  int getProjectIDFromSession(HttpServletRequest request) {
    getDatabase();
    int userIDFromRequest = securityManager.getUserIDFromRequest(request);
    if (userIDFromRequest == -1) {
      return -1;
    } else {
      return getMostRecentProjectByUser(userIDFromRequest);
    }
  }

  String getProjectName(int id) {
    Project project = getDatabase().getProjectManagement().getProject(id, false);
    return project == null ? "UNK" : project.getName();
  }

  /**
   * @param id
   * @return
   * @see #getProjectIDFromSession
   * @see DownloadServlet#getProjectIDFromUser
   */
  int getMostRecentProjectByUser(int id) {
    return getDatabase().getUserProjectDAO().getCurrentProjectForUser(id);
  }

  /**
   * @return
   * @see #getProjectIDFromSession
   */
  DatabaseImpl getDatabase() {
    if (db == null) {
      Object databaseReference = getServletContext().getAttribute(MyRemoteServiceServlet.DATABASE_REFERENCE);
      if (databaseReference != null) {
        db = (DatabaseImpl) databaseReference;
        securityManager = db.getUserSecurityManager();
        if (securityManager == null) logger.error("getDatabase huh? no security manager?");
        // logger.debug("getDatabase found existing database reference " + db + " under " +getServletContext());
      } else {
        logger.error("getDatabase huh? no existing db reference?");
      }
    }
    return db;
  }
}
