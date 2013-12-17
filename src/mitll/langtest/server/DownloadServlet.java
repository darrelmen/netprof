package mitll.langtest.server;

import mitll.langtest.server.database.DLIUserDAO;
import mitll.langtest.server.database.DatabaseImpl;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 12/17/13
 * Time: 4:57 PM
 * To change this template use File | Settings | File Templates.
 */
public class DownloadServlet extends HttpServlet {
  private static final Logger logger = Logger.getLogger(DownloadServlet.class);

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    String encodedFileName = request.getRequestURI();
   // System.out.println("uri " + encodedFileName);

    String pathInfo = request.getPathInfo();
    logger.debug("Request " + request.getQueryString() + " path "  + pathInfo +
      " uri " + request.getRequestURI() + "  " + request.getRequestURL() +  "  " + request.getServletPath());


    response.setContentType("application/vnd.ms-excel");
    DatabaseImpl db = readProperties();

    if (encodedFileName.toLowerCase().contains("users")) {
      response.setHeader("Content-Disposition", "attachment; filename=users.xlsx");
      db.getUserDAO().toXLSX(response.getOutputStream(), new DLIUserDAO(db));
    }
    else {
      response.setHeader("Content-Disposition", "attachment; filename=results.xlsx");
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

    DatabaseImpl db = makeDatabaseImpl(h2DatabaseFile,configDir,relativeConfigDir,serverProps);
    return db;
  }

  private DatabaseImpl makeDatabaseImpl(String h2DatabaseFile, String configDir, String relativeConfigDir, ServerProperties serverProperties) {
    //logger.debug("word pairs " +  serverProps.isWordPairs() + " language " + serverProps.getLanguage() + " config dir " + relativeConfigDir);
    return new DatabaseImpl(configDir, h2DatabaseFile, relativeConfigDir,  serverProperties);
  }
}