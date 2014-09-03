package mitll.langtest.server;

import mitll.langtest.server.audio.AudioConversion;
import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.shared.AudioAttribute;
import mitll.langtest.shared.CommonExercise;
import net.sf.json.JSONObject;
import org.apache.log4j.Logger;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServlet;
import java.io.*;

/**
 * Created by GO22670 on 4/8/2014.
 */
public class DatabaseServlet extends HttpServlet {
  private static final Logger logger = Logger.getLogger(DatabaseServlet.class);
  private static final int BUFFER_SIZE = 4096;

/*  protected DatabaseImpl readProperties() {
    return readProperties(getServletContext());
  }*/

/*
  private DatabaseImpl readProperties(ServletContext servletContext) {
    PathHelper pathHelper = new PathHelper(getServletContext());

    return getDatabase(servletContext, pathHelper);
  }
*/

  protected ServerProperties serverProps;
  protected String relativeConfigDir;
  protected String configDir;
  protected PathHelper pathHelper;

/*  protected DatabaseImpl getDatabase(ServletContext servletContext, PathHelper pathHelper) {
    String config = servletContext.getInitParameter("config");
    String relativeConfigDir = "config" + File.separator + config;
    String configDir = pathHelper.getInstallPath() + File.separator + relativeConfigDir;

    ServerProperties serverProps = getServerProperties(servletContext, configDir);
    this.serverProps = serverProps;
    String h2DatabaseFile = serverProps.getH2Database();

    return makeDatabaseImpl(h2DatabaseFile,configDir,relativeConfigDir,serverProps,pathHelper);
  }*/

/*  private ServerProperties getServerProperties(ServletContext servletContext, String configDir) {
    ServerProperties serverProps = new ServerProperties();

    serverProps.readPropertiesFile(servletContext, configDir);
    return serverProps;
  }*/

/*  private DatabaseImpl makeDatabaseImpl(String h2DatabaseFile, String configDir, String relativeConfigDir, ServerProperties serverProperties,PathHelper pathHelper) {
    //logger.debug("word pairs " +  serverProps.isWordPairs() + " language " + serverProps.getLanguage() + " config dir " + relativeConfigDir);
    return new DatabaseImpl(configDir, relativeConfigDir, h2DatabaseFile, serverProperties, pathHelper, true);
  }*/

  protected boolean ensureMP3(CommonExercise byID, PathHelper pathHelper, String configDir) {
    return ensureMP3(byID.getRefAudio(), pathHelper, configDir);
  }

  private boolean ensureMP3(String wavFile, PathHelper pathHelper, String configDir) {
    if (wavFile != null) {
      String parent = pathHelper.getInstallPath();
      //logger.debug("ensureMP3 : wav " + wavFile + " under " + parent);

      AudioConversion audioConversion = new AudioConversion();
      if (!audioConversion.exists(wavFile, parent)) {
        logger.warn("can't find " + wavFile + " under " + parent + " trying config... ");
        parent = configDir;
      }
      if (!audioConversion.exists(wavFile, parent)) {
        logger.error("huh? can't find " + wavFile + " under " + parent);
      }
      String filePath = audioConversion.ensureWriteMP3(wavFile, parent, false);
      return new File(filePath).exists();
    }
    else {
      return false;
    }
  }

  protected void setPaths() {
    pathHelper = getPathHelper();
    String config = getServletContext().getInitParameter("config");
    this.relativeConfigDir = "config" + File.separator + config;
    //logger.debug("rel " + relativeConfigDir);
    this.configDir = getConfigDir();
  }

  protected PathHelper getPathHelper() {
    return new PathHelper(getServletContext());
  }

  protected String getConfigDir() {
    return getConfigDir(pathHelper);
  }

  protected String getConfigDir(PathHelper pathHelper) {
    String config = getServletContext().getInitParameter("config");
    return pathHelper.getInstallPath() + File.separator + "config" + File.separator + config;
  }

  protected void ensureMP3s(CommonExercise byID) {  ensureMP3(byID, pathHelper, configDir);  }

  protected void writeToFile(InputStream inputStream, File saveFile) throws IOException {
    // opens an output stream for writing file
    copyToOutput(inputStream, new FileOutputStream(saveFile));
  }

  protected void copyToOutput(InputStream inputStream, OutputStream outputStream) throws IOException {
    byte[] buffer = new byte[BUFFER_SIZE];
    int bytesRead = -1;
    // logger.debug("Receiving data...");

    while ((bytesRead = inputStream.read(buffer)) != -1) {
      outputStream.write(buffer, 0, bytesRead);
    }

    // System.out.println("Data received.");
    outputStream.close();
    inputStream.close();
  }

  protected JSONObject getJsonForExercise(CommonExercise exercise) {
    JSONObject ex = new JSONObject();
    ex.put("id", exercise.getID());
    ex.put("fl", exercise.getForeignLanguage());
    ex.put("tl", exercise.getTransliteration());
    ex.put("en", exercise.getEnglish());
    ex.put("ct", exercise.getContext());
    AudioAttribute latestContext = exercise.getLatestContext();
    ex.put("ctref", latestContext == null ? "NO" : latestContext.getAudioRef());
    ex.put("ref", exercise.hasRefAudio() ? exercise.getRefAudio() : "NO");
    return ex;
  }
}
