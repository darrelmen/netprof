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

package mitll.langtest.server.services;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import mitll.langtest.server.LangTestDatabaseImpl;
import mitll.langtest.server.PathHelper;
import mitll.langtest.server.ServerProperties;
import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.server.database.exercise.Project;
import mitll.langtest.server.database.security.DominoSessionException;
import mitll.langtest.server.database.security.UserSecurityManager;
import mitll.langtest.shared.user.User;
import org.apache.log4j.Logger;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import java.io.File;

@SuppressWarnings("serial")
public class MyRemoteServiceServlet extends RemoteServiceServlet {
  private static final Logger logger = Logger.getLogger(MyRemoteServiceServlet.class);

  protected DatabaseImpl db;
  protected ServerProperties serverProps;
  UserSecurityManager securityManager;

  private DatabaseImpl getDatabase() {
    DatabaseImpl db = null;

    Object databaseReference = getServletContext().getAttribute(LangTestDatabaseImpl.DATABASE_REFERENCE);
    if (databaseReference != null) {
      db = (DatabaseImpl) databaseReference;
      // logger.debug("found existing database reference " + db + " under " +getServletContext());
    } else {
      logger.info("getDatabase : no existing db reference yet...");
    }
    return db;
  }

  void findSharedDatabase() {
    if (db == null) {
      db = getDatabase();
      if (db == null) {
        logger.error("no database?");
      }
      else {
        securityManager = new UserSecurityManager(db.getUserDAO());
      }
    }
  }

  /**
   * The config web.xml file.
   * As a final step, creates the DatabaseImpl!<br></br>
   * <p>
   * NOTE : makes the database available to other servlets via the databaseReference servlet context attribute.
   * Note that this will only ever be called once.
   *
   * @param servletContext
   * @see #init()
   */
  void readProperties(ServletContext servletContext) {
    String relativeConfigDir = "config" + File.separator + servletContext.getInitParameter("config");
    PathHelper pathHelper = new PathHelper(getServletContext());
    String configDir = pathHelper.getInstallPath() + File.separator + relativeConfigDir;
    serverProps = new ServerProperties(servletContext, configDir);
  }


  protected int getProjectID() {
    try {
      User loggedInUser = getSessionUser();
      if (loggedInUser == null) return -1;
      int i = db.getUserProjectDAO().mostRecentByUser(loggedInUser.getId());
      return i;
    } catch (DominoSessionException e) {
      logger.error("Got " + e, e);
      return -1;
    }
  }

  protected Project getProject() {
    try {
      User loggedInUser = getSessionUser();
      if (loggedInUser == null) {
        return null;
      }
      else {
        return db.getProjectForUser(loggedInUser.getId());
      }
    } catch (DominoSessionException e) {
      logger.error("got " + e, e);
      return null;
    }
  }

  /**
   * Get the current user from the session
   * @return
   * @throws DominoSessionException
   */
  User getSessionUser() throws DominoSessionException { return securityManager.getLoggedInUser(getThreadLocalRequest()); }
}
