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
import mitll.langtest.server.LogAndNotify;
import mitll.langtest.server.PathHelper;
import mitll.langtest.server.ServerProperties;
import mitll.langtest.server.audio.AudioFileHelper;
import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.server.database.exercise.Project;
import mitll.langtest.server.database.exercise.SectionHelper;
import mitll.langtest.server.database.security.DominoSessionException;
import mitll.langtest.server.database.security.IUserSecurityManager;
import mitll.langtest.server.database.security.UserSecurityManager;
import mitll.langtest.server.mail.MailSupport;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.user.User;
import mitll.npdata.dao.SlickProject;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.util.Properties;

@SuppressWarnings("serial")
public class MyRemoteServiceServlet extends RemoteServiceServlet implements LogAndNotify {
  private static final Logger logger = LogManager.getLogger(MyRemoteServiceServlet.class);

  protected DatabaseImpl db;
  protected ServerProperties serverProps;
  protected IUserSecurityManager securityManager;
  protected PathHelper pathHelper;
  Properties uiProperties;

  /**
   * JUST FOR AMAS and interop with old h2 database...
   */
  @Deprecated protected AudioFileHelper audioFileHelper;

  @Override
  public void init() {
    findSharedDatabase();
    readProperties(getServletContext());

    if (serverProps.isAMAS()) {
      audioFileHelper = new AudioFileHelper(pathHelper, serverProps, db, this, null);
    }
  }

  private DatabaseImpl getDatabase() {
    DatabaseImpl db = null;

    Object databaseReference = getServletContext().getAttribute(LangTestDatabaseImpl.DATABASE_REFERENCE);
    if (databaseReference != null) {
      db = (DatabaseImpl) databaseReference;
      this.pathHelper = new PathHelper(getServletContext(), db.getServerProps());
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
        securityManager = new UserSecurityManager(db.getUserDAO(), db.getUserSessionDAO());
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
    String configDir = pathHelper.getInstallPath() + File.separator + relativeConfigDir;
    serverProps = new ServerProperties(servletContext, configDir);
  }

  protected int getProjectID() {
    try {
      User loggedInUser = getSessionUser();
      if (loggedInUser == null) return -1;
      int i = db.getUserProjectDAO().mostRecentByUser(loggedInUser.getID());
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
        return db.getProjectForUser(loggedInUser.getID());
      }
    } catch (DominoSessionException e) {
      logger.error("got " + e, e);
      return null;
    }
  }

  public User getUserFromSession() {
    try {
      User loggedInUser = securityManager.getLoggedInUser(getThreadLocalRequest());
      if (loggedInUser != null) {
        db.setStartupInfo(loggedInUser);
      }
      return loggedInUser;
    } catch (DominoSessionException e) {
      logger.error("Got " + e,e);
      return null;
    }
  }

  protected int getUserIDFromSession() {
    return securityManager.getUserIDFromRequest(getThreadLocalRequest());
  }

  /**
   * Get the current user from the session
   * @return
   * @throws DominoSessionException
   */
  User getSessionUser() throws DominoSessionException { return securityManager.getLoggedInUser(getThreadLocalRequest()); }

  protected String getLanguage() {
    Project project = getProject();
    if (project == null) {
      logger.error("no current project ");
      return "";
    } else {
      SlickProject project1 = project.getProject();
      return project1.language();
    }
  }

  @Override
  public void logAndNotifyServerException(Exception e) {
    logAndNotifyServerException(e, "");
  }

  @Override
  public void logAndNotifyServerException(Exception e, String additionalMessage) {
    String message1 = e == null ? "null_ex" : e.getMessage() == null ? "null_msg" : e.getMessage();
    if (!message1.contains("Broken Pipe")) {
      String prefix = additionalMessage.isEmpty() ? "" : additionalMessage + "\n";
      String prefixedMessage = prefix + "for " + pathHelper.getInstallPath() +
          (e != null ? " got " + "Server Exception : " + ExceptionUtils.getStackTrace(e) : "");
      String subject = "Server Exception on " + pathHelper.getInstallPath();
      sendEmail(subject, getInfo(prefixedMessage));

      logger.debug(getInfo(prefixedMessage));
    }
  }

  protected void sendEmail(String subject, String prefixedMessage) {
    getMailSupport().email(serverProps.getEmailAddress(), subject, prefixedMessage);
  }

  private MailSupport getMailSupport() {
    return new MailSupport(serverProps.isDebugEMail(), serverProps.isTestEmail());
  }

  protected String getInfo(String message) {
    HttpServletRequest request = getThreadLocalRequest();
    if (request != null) {
      String remoteAddr = request.getHeader("X-FORWARDED-FOR");
      if (remoteAddr == null || remoteAddr.isEmpty()) {
        remoteAddr = request.getRemoteAddr();
      }
      String userAgent = request.getHeader("User-Agent");

      String strongName = getPermutationStrongName();
      String serverName = getThreadLocalRequest().getServerName();
      String msgStr = message +
          "\nremoteAddr : " + remoteAddr +
          "\nuser agent : " + userAgent +
          "\ngwt        : " + strongName +
          "\nserver     : " + serverName;

      return msgStr;
    } else {
      return "";
    }
  }

  protected SectionHelper<CommonExercise> getSectionHelper() {
    return db.getSectionHelper(getProjectID());
  }

  protected AudioFileHelper getAudioFileHelper() {
    if (serverProps.isAMAS()) {
      return audioFileHelper;
    } else {
      Project project = getProject();
      if (project == null) {
        logger.warn("getAudioFileHelper no current project???");
        return null;
      }
      return project.getAudioFileHelper();
    }
  }
}
