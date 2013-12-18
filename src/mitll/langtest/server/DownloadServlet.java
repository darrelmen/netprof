package mitll.langtest.server;

import mitll.langtest.server.database.DLIUserDAO;
import mitll.langtest.server.database.DatabaseImpl;
import org.apache.log4j.Logger;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 12/17/13
 * Time: 4:57 PM
 * To change this template use File | Settings | File Templates.
 */
@SuppressWarnings("serial")
public class DownloadServlet extends HttpServlet {
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
    else {
      response.setHeader("Content-Disposition", "attachment; filename=results");
      db.getResultDAO().toXLSX(response.getOutputStream());
    }
    response.getOutputStream().close();
  }

  private DatabaseImpl readProperties() {
    return readProperties(getServletContext());
  }

  private DatabaseImpl readProperties(ServletContext servletContext) {
    String config = servletContext.getInitParameter("config");
    String relativeConfigDir = "config" + File.separator + config;
    PathHelper pathHelper = new PathHelper(getServletContext());
    String configDir = pathHelper.getInstallPath() + File.separator + relativeConfigDir;
    ServerProperties serverProps = new ServerProperties();
    serverProps.readPropertiesFile(servletContext, configDir);
    String h2DatabaseFile = serverProps.getH2Database();

    return makeDatabaseImpl(h2DatabaseFile,configDir,relativeConfigDir,serverProps);
  }

  private DatabaseImpl makeDatabaseImpl(String h2DatabaseFile, String configDir, String relativeConfigDir, ServerProperties serverProperties) {
    //logger.debug("word pairs " +  serverProps.isWordPairs() + " language " + serverProps.getLanguage() + " config dir " + relativeConfigDir);
    return new DatabaseImpl(configDir, h2DatabaseFile, relativeConfigDir,  serverProperties);
  }
}