package mitll.langtest.server;

import mitll.langtest.server.audio.AudioConversion;
import mitll.langtest.shared.AudioAttribute;
import mitll.langtest.shared.CommonExercise;
import net.sf.json.JSONObject;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpServlet;
import java.io.*;

/**
 * Created by GO22670 on 4/8/2014.
 */
public class DatabaseServlet extends HttpServlet {
  private static final Logger logger = Logger.getLogger(DatabaseServlet.class);
  private static final int BUFFER_SIZE = 4096;
  private static final String NO = "NO";

  protected ServerProperties serverProps;
  protected String relativeConfigDir;
  protected String configDir;
  protected PathHelper pathHelper;

  /**
   * @see #ensureMP3s
   * @param byID
   * @param pathHelper
   * @param configDir
   * @return
   */
  private boolean ensureMP3(CommonExercise byID, PathHelper pathHelper, String configDir) {
    return ensureMP3(byID.getRefAudio(), pathHelper, configDir, byID.getForeignLanguage());
  }

  /**
   * @see mitll.langtest.server.ScoreServlet#getAnswer
   * @param wavFile
   * @param title
   */
  protected void ensureMP3(String wavFile, String title) {
    ensureMP3(wavFile, pathHelper, configDir, title);
  }

  private boolean ensureMP3(String wavFile, PathHelper pathHelper, String configDir, String title) {
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
      String filePath = audioConversion.ensureWriteMP3(wavFile, parent, false, title);
      return new File(filePath).exists();
    } else {
      return false;
    }
  }

  protected void setPaths() {
    pathHelper = getPathHelper();
    String config = getServletContext().getInitParameter("config");
    this.relativeConfigDir = "config" + File.separator + config;
   // logger.debug("setPaths rel " + relativeConfigDir  + " pathHelper " + pathHelper);
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

  /**
   * @see mitll.langtest.server.ScoreServlet#getJsonArray
   * @param byID
   */
  protected void ensureMP3s(CommonExercise byID) {
    ensureMP3(byID, pathHelper, configDir);
  }

  protected void writeToFile(InputStream inputStream, File saveFile) throws IOException {
    // opens an output stream for writing file
    copyToOutput(inputStream, new FileOutputStream(saveFile));
  }

  protected void copyToOutput(InputStream inputStream, OutputStream outputStream) throws IOException {
    byte[] buffer = new byte[BUFFER_SIZE];
    int bytesRead;

    while ((bytesRead = inputStream.read(buffer)) != -1) {
      outputStream.write(buffer, 0, bytesRead);
    }

    outputStream.close();
    inputStream.close();
  }

  /**
   * Make json for an exercise
   *
   * @param exercise
   * @return
   */
  protected JSONObject getJsonForExercise(CommonExercise exercise) {
    JSONObject ex = new JSONObject();
    ex.put("id", exercise.getID());
    ex.put("fl", exercise.getForeignLanguage());
    ex.put("tl", exercise.getTransliteration());
    ex.put("en", exercise.getEnglish());
    ex.put("ct", exercise.getContext());
    ex.put("ctr", exercise.getContextTranslation());
    AudioAttribute latestContext = exercise.getLatestContext(true);
    if (latestContext != null) {
      ensureMP3(latestContext.getAudioRef(), exercise.getContext());
    }
    ex.put("ctmref", latestContext == null ? NO : latestContext.getAudioRef());
    latestContext = exercise.getLatestContext(false);
    if (latestContext != null) {
      ensureMP3(latestContext.getAudioRef(), exercise.getContext());
    }
    ex.put("ctfref", latestContext == null ? NO : latestContext.getAudioRef());
    ex.put("ref", exercise.hasRefAudio() ? exercise.getRefAudio() : NO);

    addLatestRefs(exercise, ex);

    return ex;
  }

  /**
   * Male/female reg/slow speed
   * @param exercise
   * @param ex
   */
  private void addLatestRefs(CommonExercise exercise, JSONObject ex) {
    String mr = null, ms = null, fr = null, fs = null;
    long mrt = 0, mst = 0, frt = 0, fst = 0;

    for (AudioAttribute audioAttribute : exercise.getAudioAttributes()) {
      long timestamp = audioAttribute.getTimestamp();

      if (audioAttribute.isMale()) {
        if (audioAttribute.isRegularSpeed()) {
          if (timestamp >= mrt) {
            mrt = timestamp;
            mr = audioAttribute.getAudioRef();
          }
        } else if (audioAttribute.isSlow()) {
          if (timestamp >= mst) {
            mst = timestamp;
            ms = audioAttribute.getAudioRef();
          }
        }
      } else {
        if (audioAttribute.isRegularSpeed()) {
          if (timestamp >= frt) {
            frt = timestamp;
            fr = audioAttribute.getAudioRef();
          }
        } else if (audioAttribute.isSlow()) {
          if (timestamp >= fst) {
            fst = timestamp;
            fs = audioAttribute.getAudioRef();
          }
        }
      }
    }

    // male regular speed reference audio (m.r.r.)
    String foreignLanguage = exercise.getForeignLanguage();

    if (mr != null) {
      ensureMP3(mr, foreignLanguage);
    }
    ex.put("mrr", mr == null ? NO : mr);

    if (ms != null) {
      ensureMP3(ms, foreignLanguage);
    }
    ex.put("msr", ms == null ? NO : ms);

    if (fr != null) {
      ensureMP3(fr, foreignLanguage);
    }
    ex.put("frr", fr == null ? NO : fr);

    if (fs != null) {
      ensureMP3(fs, foreignLanguage);
    }
    ex.put("fsr", fs == null ? NO : fs);
  }
}
