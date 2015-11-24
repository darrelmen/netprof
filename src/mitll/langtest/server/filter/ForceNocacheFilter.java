/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

package mitll.langtest.server.filter;


import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;


/**
 * ForceNocacheFilter: Force the GWT nocache files to not cache.
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
  }

  @Override
  public void destroy() {}

  @Override
  public void init(FilterConfig arg0) throws ServletException {}
}