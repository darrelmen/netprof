package mitll.langtest.server;

import mitll.langtest.server.database.DLIUserDAO;
import mitll.langtest.server.database.DatabaseImpl;
import org.apache.log4j.Logger;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

/*  @Override
  public void init() {
    //this.pathHelper = new PathHelper(getServletContext());

    String config = servletContext.getInitParameter("config");
    this.relativeConfigDir = "config" + File.separator + config;
    this.configDir = pathHelper.getInstallPath() + File.separator + relativeConfigDir;

    readProperties(servletContext);
    setInstallPath(serverProps.getUseFile(), db);
  }*/
  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    String encodedFileName = request.getRequestURI();

    String pathInfo = request.getPathInfo();
    logger.debug("DownloadServlet.doGet : Request " + request.getQueryString() + " path "  + pathInfo +
      " uri " + request.getRequestURI() + "  " + request.getRequestURL() +  "  " + request.getServletPath());

    response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    DatabaseImpl db = readProperties();

    setInstallPath(db);

    if (encodedFileName.toLowerCase().contains("audio")) {
      String queryString = request.getQueryString();
      if (queryString.length() > 2) {
        queryString = queryString.substring(1, queryString.length() - 1);
      }
      String[] split = queryString.split(",");
      Map<String, Collection<String>> typeToSection = new HashMap<String, Collection<String>>();
      for (String section : split) {
        String[] split1 = section.split("=");
        String s = split1[1];
        if (!s.isEmpty()) {
          s = s.substring(1, s.length() - 1);
        }
        String key = split1[0];
        List<String> value = Arrays.asList(s.split(","));
       // logger.debug("\tkey " + key + "=" + value);
        typeToSection.put(key, value);
      }

      logger.debug("Selection " + typeToSection);
      String name = typeToSection.isEmpty() ? "audio" : db.getPrefix(typeToSection);
      response.setHeader("Content-Disposition", "attachment; filename=" + name);
      response.setContentType("application/zip");
      try {
        db.writeZip(response.getOutputStream(), typeToSection);
      } catch (Exception e) {
        logger.error("couldn't write zip?",e);
      }
    }
    else if (encodedFileName.toLowerCase().contains("users")) {
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

  private void setInstallPath(DatabaseImpl db) {
    ServletContext servletContext = getServletContext();
    String config = servletContext.getInitParameter("config");
    String relativeConfigDir = "config" + File.separator + config;
    String installPath = new PathHelper(servletContext).getInstallPath();
    String configDir = installPath + File.separator + relativeConfigDir;

    setInstallPath(serverProps.getUseFile(), db, installPath, relativeConfigDir,configDir);
  }

  private String setInstallPath(boolean useFile, DatabaseImpl db,String installPath,String relativeConfigDir,String configDir) {
    String lessonPlanFile = getLessonPlan(configDir);
    if (useFile && !new File(lessonPlanFile).exists()) logger.error("couldn't find lesson plan file " + lessonPlanFile);

    //String installPath = pathHelper.getInstallPath();
    db.setInstallPath(installPath, lessonPlanFile, serverProps.getLanguage(), useFile,
      relativeConfigDir+File.separator+serverProps.getMediaDir());

    return lessonPlanFile;
  }

  private String getLessonPlan(String configDir) {
    return configDir + File.separator + serverProps.getLessonPlan();
  }
}