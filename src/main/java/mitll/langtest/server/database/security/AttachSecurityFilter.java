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
package mitll.langtest.server.database.security;

import mitll.hlt.domino.shared.Constants;
import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.server.database.DatabaseServices;
import mitll.langtest.shared.common.DominoSessionException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import static org.apache.logging.log4j.web.WebLoggerContextUtils.getServletContext;

/**
 *
 * AttachSecurityFilter: A security filter that ensures a user is allowed
 * to access a particular file.
 *
 * @author Raymond Budd <a href="mailto:raymond.budd@ll.mit.edu">raymond.budd@ll.mit.edu</a>
 * @since Jul 29, 2014 9:46:37 AM
 */

public class AttachSecurityFilter implements Filter {
  private static final Logger log = LogManager.getLogger(AttachSecurityFilter.class);

  private static final String DATABASE_REFERENCE = "databaseReference";

  private DatabaseServices db;

  private IUserSecurityManager securityManager;
  private ServletContext servletContext;

  private static final boolean DEBUG = true;

  public void init(FilterConfig filterConfig) throws ServletException {
    this.servletContext = filterConfig.getServletContext();
    if (DEBUG) log.info("found servlet context " +servletContext);
  }

  @Override
  public void doFilter(final ServletRequest request,
                       final ServletResponse response,
                       final FilterChain chain)
      throws IOException, ServletException {
    if (db == null) {
      findSharedDatabase(servletContext);
    }
    if (!(request instanceof HttpServletRequest)) {
      if (DEBUG) log.info("doFilter : skipping " + request.toString() + " since not HttpServletReq");
      chain.doFilter(request, response);
    }
    else {
      if (DEBUG)     log.info("doFilter : req for " + ((HttpServletRequest) request).getRequestURI());
      if (DEBUG) log.info("doFilter : chain is " + chain);


      final HttpServletRequest httpRequest = (HttpServletRequest) request;
      try {
        boolean isValid = isValidRequest(((HttpServletRequest) request));

        if (!isValid) throw new DominoSessionException("not allowed");
        // MUST do this -
        chain.doFilter(request, response);

        if (DEBUG) log.info("doFilter after chain doFilter " + httpRequest.getRequestURI());
      } catch (DominoSessionException dse) {
        log.warn("doFilter : nope - no session " + dse.getMessage() + " req for : " + ((HttpServletRequest) request).getRequestURI());
        handleAccessFailure(httpRequest, response);
      } catch (Exception e) {
        if (e.getClass().getCanonicalName().equals("org.apache.catalina.connector.ClientAbortException")) {
          log.info("doFilter : User reload during request {}.", httpRequest.getRequestURL());
        } else {
          log.error("doFilter : Unexpected exception during request {}.", httpRequest.getRequestURL(), e);
        }
      }
    }
  }

  @NotNull
  private boolean isValidRequest(HttpServletRequest request) throws DominoSessionException, UnsupportedEncodingException {
    String requestURI = request.getRequestURI();
    boolean valid = true;
    int userIDFromSessionOrDB = getUserIDFromSessionOrDB(request);

    if (DEBUG) log.info("isValidRequest : found session user " + userIDFromSessionOrDB + " req for : " + requestURI);
    //   log.warn("doGet : found session user " + userIDFromSessionOrDB + " req for path : " + request.getPathInfo());

    File file = getFileFromRequest(request, requestURI);

    if (file.exists()) {
      if (requestURI.contains("/answers")) {
//        if (DEBUG) log.info("isValidRequest got answers " + requestURI);
        // 1 who recorded the audio?
        int userForFile = getUserForFile(requestURI, file);

        if (userForFile == -1) {
          log.warn("isValidRequest not sure who recorded this file " + requestURI);
        } else {
          if (userForFile == userIDFromSessionOrDB) {
            // 2 are you the same person? if so you get to hear it
            if (DEBUG) log.info("isValidRequest OK, it's your file.");
          } else {
            boolean student = db.getUserDAO().isStudent(userIDFromSessionOrDB);
            if (student) {
              // 4 if you are a student sorry, you don't get to hear it
              log.warn("isValidRequest nope - student " + userIDFromSessionOrDB+ " did not create the file, user #" + userForFile + " did.");
              valid = false;
            } else {
              // 3 if you are not the same, are you are teacher, then you can hear it
              if (DEBUG) log.info("isValidRequest OK, you're a teacher");
            }
          }
        }
      }
    }
    return valid;
  }

  private int getUserForFile(String requestURI, File file) {
    int userForFile = getUserForFile(requestURI, requestURI.substring(1));
    if (userForFile == -1) {
      log.warn("getUserForFile now trying " + file.getAbsolutePath());
      userForFile = getUserForWavFile(file.getAbsolutePath());
    }
    return userForFile;
  }

  @NotNull
  private File getFileFromRequest(HttpServletRequest request, String requestURI) throws UnsupportedEncodingException {
    String filename = URLDecoder.decode(request.getPathInfo().substring(1), "UTF-8");
    File file = new File(fixParent(requestURI), filename);

    if (DEBUG) log.info("file now " + file.getAbsolutePath() + " exists " + file.exists());

    if (!file.exists()) {
      filename = URLDecoder.decode(requestURI, "UTF-8");
      String parent = fixParent(requestURI);
      file = new File(parent, filename);
      if (DEBUG)  log.info("file 2 now " + file.getAbsolutePath() + " exists " + file.exists());
    }
    return file;
  }

  private int getUserForFile(String requestURI, String fileToFind) {
    if (DEBUG) log.info("getUserForFile checking owner of " + fileToFind);

    fileToFind = requestURI.startsWith("answers") ? requestURI.substring("answers".length()) : requestURI;
    fileToFind = fileToFind.startsWith("netprof") ? fileToFind.substring("netprof".length()) : fileToFind;
    if (DEBUG) log.info("getUserForFile checking now " + fileToFind);

    int answers = fileToFind.indexOf("answers");
    if (answers != -1) {
      fileToFind = fileToFind.substring(answers);
      if (DEBUG) log.info("getUserForFile test now " + fileToFind);
    }
    int userForFile = getUserForWavFile(fileToFind);
    return userForFile;
  }

  private int getUserForWavFile(String fileToFind) {
    fileToFind = fileToFind.length() > 4 ? fileToFind.substring(0, fileToFind.length() - 4) + ".wav" : fileToFind;
    if (DEBUG) log.info("testing '" + fileToFind + "'");
    return db.getProjectManagement().getUserForFile(fileToFind);
  }

  @NotNull
  private String fixParent(String requestURI) {
    String parent = "/opt/netprof";

    if (requestURI.contains("bestAudio")) {
      parent += "/bestAudio";
    } else if (requestURI.contains("answers")) {
      parent += "/answers";
    }
    return parent;
  }

  private void handleAccessFailure(final HttpServletRequest request,
                                   final ServletResponse response) throws IOException, ServletException {
    log.error("System access failed security filter! Request: {}", request.getRequestURI());
    try {
      response.setContentType("text/plain");
      response.getWriter().write("{ \"" + Constants.SESSION_EXPIRED_CODE + "\": true}");
    } catch (IOException e) {
      log.error("Error Writing to ouput stream! Request: {}", request.getRequestURI(), e);
    }
  }

  @Override
  public void destroy() {

  }

  protected int getUserIDFromSessionOrDB(HttpServletRequest httpRequest) throws DominoSessionException {
    if (securityManager == null) log.error("huh? no security manager?");
    return securityManager.getUserIDFromSessionLight(httpRequest);
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
        log.info("made security manager " +securityManager);
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
}
