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
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.List;

/**
 * AttachSecurityFilter: A security filter that ensures a user is allowed
 * to access a particular file.
 *
 * @author Raymond Budd <a href="mailto:raymond.budd@ll.mit.edu">raymond.budd@ll.mit.edu</a>
 * @since Jul 29, 2014 9:46:37 AM
 */

public class AttachSecurityFilter implements Filter {
  private static final Logger log = LogManager.getLogger(AttachSecurityFilter.class);

  private static final String DATABASE_REFERENCE = "databaseReference";
  private static final String ANSWERS = "answers";
  private static final String SLASH_ANSWERS = "/" + ANSWERS;
  private static final String NETPROF = "netprof";
  private static final String OPT = "/opt";
  private static final String BEST_AUDIO = "bestAudio";
  private static final String WAV = ".wav";
  private static final int WAV_LEN = WAV.length();
  public static final String DIALOG = "dialog";

  private DatabaseServices db;

  private IUserSecurityManager securityManager;
  private ServletContext servletContext;
  private String webappName = NETPROF;

  private static final boolean DEBUG = false;
  private static final boolean DEBUG_RESPONSE = false;
  private static final boolean DEBUG_RESPONSE_DETAIL = false;

  public void init(FilterConfig filterConfig) {
    this.servletContext = filterConfig.getServletContext();

    String contextPath = servletContext.getContextPath();

   // log.info("context        '" + contextPath + "'");
    String realContextPath = servletContext == null ? "" : servletContext.getRealPath(servletContext.getContextPath());
   // log.info("realContextPath " + realContextPath);

    List<String> pathElements = Arrays.asList(realContextPath.split(realContextPath.contains("\\") ? "\\\\" : "/"));
   // log.info("pathElements    " + pathElements);

    webappName = pathElements.get(pathElements.size() - 1);
   // log.info("webappName " + webappName);

    if (!webappName.equalsIgnoreCase(DIALOG) || !webappName.equalsIgnoreCase(NETPROF)) {
      log.warn("huh? unexpected : app name is " + webappName);
    }

//    if (DEBUG) log.info("found servlet context " + servletContext);
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
    } else {
      if (DEBUG) log.info("doFilter : req for " + ((HttpServletRequest) request).getRequestURI());
//      if (DEBUG) log.info("doFilter : chain is " + chain);

      final HttpServletRequest httpRequest = (HttpServletRequest) request;
      try {
        if (!isValidRequest(httpRequest)) {
          throw new DominoSessionException("not allowed");
        }
        // MUST do this -
        chain.doFilter(request, response);

        // if (DEBUG) log.info("doFilter after chain doFilter " + httpRequest.getRequestURI());
      } catch (DominoSessionException dse) {
        log.warn("doFilter : nope - no session " + dse.getMessage() + " req for : " + ((HttpServletRequest) request).getRequestURI());
        handleAccessFailure(httpRequest, response);
      } catch (Exception e) {
        StringBuffer requestURL = httpRequest.getRequestURL();
        if (e.getClass().getCanonicalName().equals("org.apache.catalina.connector.ClientAbortException")) {
          log.info("doFilter : User reload during request {}.", requestURL);
        } else {
          log.error("doFilter : Unexpected exception during request {}.", requestURL, e);
        }
      }
    }
  }

  /**
   * Any student audio undergoes more thorough checking...
   *
   * @param request
   * @return
   * @throws DominoSessionException
   * @throws UnsupportedEncodingException
   */
  @NotNull
  private boolean isValidRequest(HttpServletRequest request) throws DominoSessionException, UnsupportedEncodingException {
    String requestURI = request.getRequestURI();
    if (requestURI.isEmpty()) return false; // protect substring(1)

    boolean valid = true;
    int userIDFromSessionOrDB = getUserIDFromSessionOrDB(request);

    if (DEBUG)
      log.info("isValidRequest : found session user " + userIDFromSessionOrDB + " req for '" + requestURI + "'");
    //   log.warn("doGet : found session user " + userIDFromSessionOrDB + " req for path : " + request.getPathInfo());

    if (requestURI.contains(SLASH_ANSWERS)) {
      String testPath = removeNetprof(requestURI.substring(1));
      int userForFile = getUserForWavFile(testPath);

      if (userForFile == -1) {
        userForFile = getUserForFileTryHarder(request, requestURI, testPath);
      }

      valid = isAllowedToGet(userIDFromSessionOrDB, userForFile);
      //      } else {
//        log.warn("isValidRequest couldn't find file for " + requestURI);
//      }
    }
    return valid;
  }

  private int getUserForFileTryHarder(HttpServletRequest request, String requestURI, String testPath) throws UnsupportedEncodingException {
    int userForFile;
    if (DEBUG) log.info("isValidRequest got answers " + requestURI + " couldn't find user for " + testPath);
    File file = getFileFromRequest(request, requestURI);

//    if (!file.exists()) {
//      log.warn("isValidRequest couldn't find file for " + requestURI);
//    }
//    if (DEBUG) log.info("isValidRequest got answers " + requestURI);
    // 1 who recorded the audio?
    userForFile = getUserForFile(requestURI, file);

    if (userForFile == -1) {
      log.warn("isValidRequest not sure who recorded this file " + requestURI);
    }
    return userForFile;
  }

  private boolean isAllowedToGet(int userIDFromSessionOrDB, int userForFile) {
    if (userForFile == userIDFromSessionOrDB) {
      // 2 are you the same person? if so you get to hear it
      if (DEBUG_RESPONSE_DETAIL) log.info("isAllowedToGet OK, it's your file.");
      return true;
    } else {
      if (db.getUserDAO().isStudent(userIDFromSessionOrDB)) {
        if (userForFile == -1) {
          // 5 we somehow couldn't figure out who had recorded the file initially - perhaps an old Japanese file?
          log.warn("isAllowedToGet OK : you are a student " + userIDFromSessionOrDB + ", who did not create the file, but an unknown user #" + userForFile + " did so we'll allow it.");
          return true;
        } else {
          // 4 if you are a student sorry, you don't get to hear it
          log.warn("isAllowedToGet nope - student " + userIDFromSessionOrDB + " did not create the file, user #" + userForFile + " did.");
          return false;
        }
      } else {
        // 3 if you are not the same, are you are teacher, then you can hear it
        if (DEBUG_RESPONSE) log.info("isAllowedToGet OK, you're a teacher or higher");
        return true;
      }
    }
  }

  @NotNull
  private File getFileFromRequest(HttpServletRequest request, String requestURI) throws UnsupportedEncodingException {
    String pathInfo = request.getPathInfo();
    if (pathInfo == null) {
//      log.warn("getFileFromRequest no path info for " + request);
      pathInfo = requestURI;
    }
    String filename = pathInfo.isEmpty() ? "" : URLDecoder.decode(pathInfo.substring(1), "UTF-8");

    File file = new File(OPT, filename);

    if (file.exists()) {
      if (DEBUG) log.info("getFileFromRequest OK found " + file.getAbsolutePath());
      return file;
    } else {
      String parent1 = fixParent(requestURI);

      if (DEBUG) log.info("getFileFromRequest parent1 " + parent1 + " for " + requestURI);

      file = new File(parent1, filename);

      if (DEBUG) log.info("getFileFromRequest file now " + file.getAbsolutePath() + " exists " + file.exists());

      if (!file.exists()) {
        filename = URLDecoder.decode(requestURI, "UTF-8");
        String parent = fixParent(requestURI);
        file = new File(parent, filename);
        if (DEBUG) log.info("getFileFromRequest file 2 now " + file.getAbsolutePath() + " exists " + file.exists());
      }
      return file;
    }
  }

  @NotNull
  private String fixParent(String requestURI) {
    String parent = OPT + "/" + NETPROF;

    if (requestURI.contains(BEST_AUDIO)) {
      parent += "/" + BEST_AUDIO;
    } else if (requestURI.contains(ANSWERS)) {
      parent += SLASH_ANSWERS;
    }
    return parent;
  }

  private int getUserForFile(String requestURI, File file) {
    int userForFile = getUserForFileAbsolute(file);
    if (userForFile == -1) {
      userForFile = getUserForFile(requestURI);
    }
    return userForFile;
  }

  private int getUserForFileAbsolute(File file) {
    String absolutePath = file.getAbsolutePath();
    //log.info("getUserForFile now trying full path " + absolutePath);
    return getUserForWavFile(absolutePath);
  }

  private int getUserForFile(String requestURI) {
//    if (DEBUG) log.info("getUserForFile checking owner of " + fileToFind);
    String fileToFind = requestURI.startsWith(ANSWERS) ? requestURI.substring(ANSWERS.length()) : requestURI;
    fileToFind = removeNetprof(fileToFind);
    if (DEBUG) log.info("getUserForFile user for " + fileToFind);
   // return getUserForWavFile(removeAnswers(fileToFind));
    log.info("getUserForFile checking owner of " + requestURI + " actually " + fileToFind);
    return getUserForWavFile(fileToFind);
  }

//  @NotNull
//  private String removeAnswers(String fileToFind) {
//    int answers = fileToFind.indexOf(ANSWERS);
//    if (answers != -1) {
//      fileToFind = fileToFind.substring(answers + ANSWERS.length());
//
//      answers = fileToFind.indexOf(ANSWERS);
//      if (answers != -1) {
//        fileToFind = fileToFind.substring(answers);
//      }
//      if (DEBUG) log.info("getUserForFile test " + fileToFind);
//    }
//    return fileToFind;
//  }


  @NotNull
  private String removeNetprof(String fileToFind) {
    return fileToFind.startsWith(webappName) ? fileToFind.substring(webappName.length()) : fileToFind;
  }

  private int getUserForWavFile(String fileToFind) {
    fileToFind = fileToFind.length() > WAV_LEN ? fileToFind.substring(0, fileToFind.length() - WAV_LEN) + WAV : fileToFind;
    int userForFile = db.getProjectManagement().getUserForFile(fileToFind);
    if (DEBUG) {
      log.info("getUserForWavFile : testing '" + fileToFind + "' found user #" + userForFile);
    }
    return userForFile;
  }

  /**
   * @param request
   * @param response
   * @see #doFilter
   */
  private void handleAccessFailure(final HttpServletRequest request, final ServletResponse response) {
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
//        log.info("made security manager " + securityManager);
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
//      log.debug("getDatabase : found existing database reference " + db + " under " + servletContext);
    } else {
      log.warn("getDatabase : no existing db reference yet - config error?");
    }
    return db;
  }
}
