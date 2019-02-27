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

import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.server.rpc.RPC;
import com.google.gwt.user.server.rpc.RPCRequest;
import com.google.gwt.user.server.rpc.XsrfProtectedServiceServlet;
import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.server.database.DatabaseServices;
import mitll.langtest.shared.common.DominoSessionException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import static mitll.hlt.domino.server.util.DominoLog.TIMING;
import static mitll.hlt.domino.server.util.DominoLog.elapsedMS;

public class SecureXSRFPServiceServlet extends XsrfProtectedServiceServlet {
  private static final long serialVersionUID = -3315801260055188323L;
  private static final Logger log = LogManager.getLogger();
  private IUserSecurityManager securityManager;
  private DatabaseServices db;
  /** The name of the user id attribute in the session */
  private static final String XSRF_COOKIE_NAME = "JSESSIONID";
  private static final String DATABASE_REFERENCE = "databaseReference";

  public SecureXSRFPServiceServlet() {
    super(XSRF_COOKIE_NAME);
  }

  @Override
  public String processCall(String payload) throws SerializationException {
    if (db == null) {
      findSharedDatabase(getServletContext());
    }

    long startMS = System.currentTimeMillis();
    try {
      int userIDFromSessionOrDB = getUserIDFromSessionOrDB(getThreadLocalRequest());
    } catch (Exception e) {
      return failCall(payload);
    }

    //log.debug("Checking current user {}", () -> currentUser);
    // getThreadLocalRequest().setAttribute(SecurityManager.USER_REQUEST_ATT, currentUser);
    log.debug(TIMING, "User check for " +
            "{} " +
            "took {} ms",
        //() -> currentUser,
        () -> elapsedMS(startMS));
    return super.processCall(payload);

  }

  private String failCall(String payload) throws SerializationException {
    RPCRequest rpcReq = RPC.decodeRequest(payload, this.getClass(), this);
    String responseVal = RPC.encodeResponseForFailure(rpcReq.getMethod(),
        new DominoSessionException(), rpcReq.getSerializationPolicy());
    log.info("Failing call! Response: {}", responseVal);
    return responseVal;
  }

  @Override
  protected void doUnexpectedFailure(Throwable ex) {
    log.info("Look at exception {}", ex.getClass().getCanonicalName());
    if (ex.getClass().getCanonicalName().equals("org.apache.catalina.connector.ClientAbortException")) {
      log.info("User reload during request.", ex);
    } else {
      log.error("Got document service Exception!", ex);
      // This may not be necessary in production, but some exceptions
      // traces did not include full cause details when running in dev mode.
      if (ex.getCause() != null) {
        log.warn("Tracing exception cause!", ex.getCause());
      }
    }
    super.doUnexpectedFailure(ex);
  }

  protected int getUserIDFromSessionOrDB(HttpServletRequest httpRequest) throws mitll.langtest.shared.common.DominoSessionException {
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
        log.info("found security manager " + securityManager);
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
 