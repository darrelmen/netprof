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

package mitll.langtest.server.filter;

import mitll.langtest.server.database.security.UserSecurityManager;
import org.apache.logging.log4j.ThreadContext;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Date;
import java.util.UUID;

/**
 * ForceNocacheFilter: Force the GWT nocache files to not cache.
 * <p>
 * <p>
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author Raymond Budd <a href=mailto:raymond.budd@ll.mit.edu>raymond.budd@ll.mit.edu</a>
 * @since Feb 6, 2014 8:44:28 AM
 */
public class ForceNocacheFilter implements Filter {
  @Override
  public void doFilter(final ServletRequest request,
                       final ServletResponse response, final FilterChain chain)
      throws IOException, ServletException {

    final HttpServletRequest httpRequest = (HttpServletRequest) request;
    final String requestUri = httpRequest.getRequestURI();
    HttpSession session = httpRequest.getSession(false);
    String sessionId;
    String loginId = "no-user";
    if (session != null) {
      sessionId = session.getId();
      Object loginO = session.getAttribute(UserSecurityManager.USER_SESSION_ATT);
      if (loginO != null) {
        loginId = loginO.toString();
      }
    } else {
      sessionId = httpRequest.getRequestedSessionId() + " - req-but-missing";
    }

    String remoteAddr = httpRequest.getHeader("X-FORWARDED-FOR");
    if (remoteAddr == null || remoteAddr.isEmpty()) {
      remoteAddr = request.getRemoteAddr();
    }

    // Add details to the thread context for use in logging.
    ThreadContext.put("sessionId", sessionId);
    ThreadContext.put("ipAddress", remoteAddr);
    ThreadContext.put("requestId", UUID.randomUUID().toString()); // Add the fishtag;
    ThreadContext.put("loginId", loginId);
    ThreadContext.put("hostName", request.getServerName());

    if (requestUri.contains(".nocache.")) {
      Date now = new Date();
      HttpServletResponse httpResponse = (HttpServletResponse) response;
      httpResponse.setDateHeader("Date", now.getTime());
      // one day old
      httpResponse.setDateHeader("Expires", now.getTime() - 86400000L);
      httpResponse.setHeader("Pragma", "no-cache");
      httpResponse.setHeader("Cache-control", "no-cache, no-store, must-revalidate");
    }


    chain.doFilter(request, response);
    ThreadContext.clearAll();
  }

  @Override
  public void destroy() {
  }

  @Override
  public void init(FilterConfig arg0) throws ServletException {
  }
}