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

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

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

  public void init(FilterConfig filterConfig) throws ServletException {
    this.servletContext = filterConfig.getServletContext();
  }

  @Override
  public void doFilter(final ServletRequest request,
                       final ServletResponse response,
                       final FilterChain chain)
      throws IOException, ServletException {
    if (db == null) {
      findSharedDatabase(servletContext);
    }
    final HttpServletRequest httpRequest = (HttpServletRequest) request;
    try {
      int userIDFromSessionOrDB = getUserIDFromSessionOrDB(httpRequest);
//      log.info("doFilter found session user " + userIDFromSessionOrDB);

      // MUST do this -
      chain.doFilter(request, response);
    } catch (DominoSessionException dse) {
      log.warn("doFilter : nope - no session " + dse);
      handleAccessFailure(httpRequest, response);
    } catch (Exception e) {
      if (e.getClass().getCanonicalName().equals("org.apache.catalina.connector.ClientAbortException")) {
        log.info("User reload during request {}.", httpRequest.getRequestURL());
      } else {
        log.error("Unexpected exception during request {}.", httpRequest.getRequestURL(), e);
      }
    }
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
    return securityManager.getUserIDFromSession(httpRequest);
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
