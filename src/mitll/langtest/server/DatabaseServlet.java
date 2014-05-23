package mitll.langtest.server;

import mitll.langtest.server.database.DatabaseImpl;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServlet;
import java.io.File;

/**
 * Created by GO22670 on 4/8/2014.
 */
public class DatabaseServlet extends HttpServlet {
  protected DatabaseImpl readProperties() {
    return readProperties(getServletContext());
  }

  private DatabaseImpl readProperties(ServletContext servletContext) {
    PathHelper pathHelper = new PathHelper(getServletContext());

    return getDatabase(servletContext, pathHelper);
  }

  protected ServerProperties serverProps;
  protected DatabaseImpl getDatabase(ServletContext servletContext, PathHelper pathHelper) {
    String config = servletContext.getInitParameter("config");
    String relativeConfigDir = "config" + File.separator + config;
    String configDir = pathHelper.getInstallPath() + File.separator + relativeConfigDir;

    ServerProperties serverProps = getServerProperties(servletContext, configDir);
    this.serverProps = serverProps;
    String h2DatabaseFile = serverProps.getH2Database();

    return makeDatabaseImpl(h2DatabaseFile,configDir,relativeConfigDir,serverProps,pathHelper);
  }

  private ServerProperties getServerProperties(ServletContext servletContext, String configDir) {
    ServerProperties serverProps = new ServerProperties();

    serverProps.readPropertiesFile(servletContext, configDir);
    return serverProps;
  }

  private DatabaseImpl makeDatabaseImpl(String h2DatabaseFile, String configDir, String relativeConfigDir, ServerProperties serverProperties,PathHelper pathHelper) {
    //logger.debug("word pairs " +  serverProps.isWordPairs() + " language " + serverProps.getLanguage() + " config dir " + relativeConfigDir);
    return new DatabaseImpl(configDir, relativeConfigDir, h2DatabaseFile, serverProperties, pathHelper, true);
  }
}
