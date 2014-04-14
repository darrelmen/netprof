package mitll.langtest.server;

import mitll.langtest.server.database.DLIUserDAO;
import mitll.langtest.server.database.DatabaseImpl;
import org.apache.log4j.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 12/17/13
 * Time: 4:57 PM
 * To change this template use File | Settings | File Templates.
 */
@SuppressWarnings("serial")
public class DownloadServlet extends DatabaseServlet {
  private static final Logger logger = Logger.getLogger(DownloadServlet.class);

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    String encodedFileName = request.getRequestURI();

    String pathInfo = request.getPathInfo();
    logger.debug("DownloadServlet.doGet : Request " + request.getQueryString() + " path "  + pathInfo +
      " uri " + request.getRequestURI() + "  " + request.getRequestURL() +  "  " + request.getServletPath());

    response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    DatabaseImpl db = readProperties();

    if (encodedFileName.toLowerCase().contains("users")) {
      response.setHeader("Content-Disposition", "attachment; filename=users");
      db.getUserDAO().toXLSX(response.getOutputStream(), new DLIUserDAO(db));
    }
    else if (encodedFileName.toLowerCase().contains("results")) {
      response.setHeader("Content-Disposition", "attachment; filename=results");
      db.getResultDAO().writeExcelToStream(db.getResultsWithGrades(),response.getOutputStream());
    }
    else {
      response.setHeader("Content-Disposition", "attachment; filename=events");
      db.getEventDAO().toXLSX(response.getOutputStream());
    }
    response.getOutputStream().close();
  }
}