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

package mitll.langtest.server;

import mitll.hlt.domino.shared.Constants;
import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.server.database.DatabaseServices;
import mitll.langtest.server.database.security.IUserSecurityManager;
import mitll.langtest.server.services.MyRemoteServiceServlet;
import mitll.langtest.shared.common.DominoSessionException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.jetbrains.annotations.NotNull;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletResponse;
import javax.servlet.UnavailableException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLDecoder;
import java.nio.file.Files;

public class SessionCheckDefaultServlet extends HttpServlet {
  private static final Logger log = LogManager.getLogger(SessionCheckDefaultServlet.class);

  private static final String DATABASE_REFERENCE = "databaseReference";

  private DatabaseServices db;
  private IUserSecurityManager securityManager;

  protected int getUserIDFromSessionOrDB(HttpServletRequest httpRequest) throws DominoSessionException {
    if (securityManager == null) log.error("huh? no security manager?");
    return securityManager.getUserIDFromSession(httpRequest);
  }

  /**
   * OK, let's also check to see if the person requesting the audio has the right to hear it.
   * <p>
   * Students cannot hear audio made by other students.
   *
   * @param request
   * @param response
   * @throws ServletException
   * @throws IOException
   */
  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    String requestURI = request.getRequestURI();
    try {
      log.info("doGet : req for : " + requestURI);

      if (db == null) {
        findSharedDatabase(getServletContext());
      }

      int userIDFromSessionOrDB = getUserIDFromSessionOrDB(request);

      log.info("doGet : found session user " + userIDFromSessionOrDB + " req for : " + requestURI);
      //   log.warn("doGet : found session user " + userIDFromSessionOrDB + " req for path : " + request.getPathInfo());

      String filename = URLDecoder.decode(request.getPathInfo().substring(1), "UTF-8");
      File file = new File(fixParent(requestURI), filename);

      log.info("file now " + file.getAbsolutePath() + " exists " + file.exists());

      if (!file.exists()) {
        filename = URLDecoder.decode(requestURI, "UTF-8");

        String parent = fixParent(requestURI);

        file = new File(parent, filename);
        log.info("file 2 now " + file.getAbsolutePath() + " exists " + file.exists());
      }

      if (file.exists()) {
        if (requestURI.contains("/answers")) {
          // 1 who recorded the audio?
          String fileToFind = requestURI.substring(1);

          int userForFile = getUserForFile(requestURI, fileToFind);

          if (userForFile == -1) {
            log.warn("not sure who recorded this file " + requestURI);
          } else {
            if (userForFile == userIDFromSessionOrDB) {
              // 2 are you the same person? if so you get to hear it
              log.info("OK, it's your file.");
            } else {
              boolean student = db.getUserDAO().isStudent(userIDFromSessionOrDB);
              if (student) {
                // 4 if you are a student sorry, you don't get to hear it
                reply(response, "not your file.");
                return;
              } else {
                // 3 if you are not the same, are you are teacher, then you can hear it
                log.info("OK, you're a teacher");
              }
            }
          }
          log.info("got answers " + requestURI);
        }
      }

      if (file.exists()) {
        response.setHeader("Content-Type", getServletContext().getMimeType(filename));
        response.setHeader("Content-Length", String.valueOf(file.length()));
        response.setHeader("Content-Disposition", "inline; filename=\"" + file.getName() + "\"");

        Files.copy(file.toPath(), response.getOutputStream());
      } else {
        String message = "Nope.";
        reply(response, message);
      }
    } catch (DominoSessionException dse) {
      log.warn("doGet : nope - no session " + dse.getMessage() + " req for : " + requestURI);
      handleAccessFailure(request, response);
    } catch (Exception e) {
      if (e.getClass().getCanonicalName().equals("org.apache.catalina.connector.ClientAbortException")) {
        log.warn("doGet : User reload during request {}.", request.getRequestURL());
      } else {
        log.error("doGet : Unexpected exception during request {}.", request.getRequestURL(), e);
      }
    }
  }

  private int getUserForFile(String requestURI, String fileToFind) {
    log.info("checking owner of " +fileToFind);

    fileToFind = requestURI.startsWith("answers")? requestURI.substring("answers".length()):requestURI;


    fileToFind  = fileToFind.startsWith("netprof")?fileToFind.substring("netprof".length()):fileToFind;
    log.info("checking now " +fileToFind);

    int answers = fileToFind.indexOf("answers");
    if (answers != -1) {
      fileToFind = fileToFind.substring(answers);
      log.info("test now " + fileToFind);
    }
    fileToFind = fileToFind.length() > 4 ? fileToFind.substring(0, fileToFind.length() - 4) + ".wav" : fileToFind;
    log.info("testing " + fileToFind);
    return db.getProjectManagement().getUserForFile(fileToFind);
  }

  @NotNull
  private String fixParent(String requestURI) {
    String parent = "/opt/netprof";

    if (requestURI.contains("bestAudio")) {
      parent += "/bestAudio";
    }
    else if (requestURI.contains("answers")) {
      parent += "/answers";
    }
    return parent;
  }

  private void reply(HttpServletResponse response, String message) {
    try {
      response.setContentType("text/plain");
      response.getWriter().write(message);
    } catch (IOException e) {
      log.error("Error Writing to ouput stream! Request: {}", "", e);
    }
  }

  /**
   * Find shared db and make the user security manager.
   */
  private void findSharedDatabase(ServletContext servletContext) {
    if (db == null) {
      db = getDatabase(servletContext);
      if (db == null) {
        log.error("findSharedDatabase no database?");
      } else {
        securityManager = db.getUserSecurityManager();
        log.warn("got security manager " + securityManager);
      }
    }
  }

  /**
   * Find the shared db reference.
   *
   * @return
   */
  private DatabaseImpl getDatabase(ServletContext servletContext) {
    DatabaseImpl db = null;

    Object databaseReference = servletContext.getAttribute(DATABASE_REFERENCE);
    if (databaseReference != null) {
      db = (DatabaseImpl) databaseReference;
      log.debug("getDatabase : found existing database reference " + db + " under " + servletContext);
    } else {
      log.warn("getDatabase : no existing db reference yet - config error?");
    }
    return db;
  }

  private void handleAccessFailure(final HttpServletRequest request,
                                   final ServletResponse response) throws IOException, ServletException {
    String requestURI = request.getRequestURI();
    log.error("System access failed security filter! Request: {}", requestURI);
    try {
      response.setContentType("text/plain");
      response.getWriter().write("{ \"" + Constants.SESSION_EXPIRED_CODE + "\": true}");
    } catch (IOException e) {
      log.error("Error Writing to ouput stream! Request: {}", requestURI, e);
    }
  }

  @Override
  public void init() throws UnavailableException {
    //  super.init();
    log.info("init for servlet");
  }
}
