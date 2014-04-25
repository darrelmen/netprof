package mitll.langtest.server;

import com.google.common.io.Files;
import mitll.langtest.server.audio.AudioFileHelper;
import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.shared.scoring.PretestScore;
import org.apache.log4j.Logger;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Map;

/**
 * TODO : support a post of an audio file
 * TODO : return json back
 *
 * All in support of Liz semi-tethered iOS app.
 *
 * User: GO22670
 */
@SuppressWarnings("serial")
public class ScoreServlet extends DatabaseServlet {
  private static final Logger logger = Logger.getLogger(ScoreServlet.class);
  private AudioFileHelper audioFileHelper;

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    String encodedFileName = request.getRequestURI();

    String pathInfo = request.getPathInfo();
    logger.debug("ScoreServlet.doGet : Request " + request.getQueryString() + " path " + pathInfo +
      " uri " + request.getRequestURI() + "  " + request.getRequestURL() + "  " + request.getServletPath());

    // TODO change to json
    response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    audioFileHelper = getAudioFileHelper();

    Map<String, String[]> parameterMap = request.getParameterMap();
    logger.debug("map " + parameterMap);
    Enumeration<String> parameterNames = request.getParameterNames();
    logger.debug("params " + parameterNames);

    // TODO : support posted file
    // TODO : support posted sentence

    String testAudioFile = "C:\\Users\\go22670\\DLITest\\merge\\bootstrap\\war\\config\\english\\bestAudio\\4\\Fast.wav";
    PretestScore book = getASRScoreForAudio(audioFileHelper, testAudioFile, "book");

    // TODO : return json equivalent for pretestscore

    logger.debug("book " + book);

    response.getOutputStream().close();
  }

  private AudioFileHelper getAudioFileHelper() {
    ServletContext servletContext = getServletContext();
    PathHelper pathHelper = new PathHelper(servletContext);
    DatabaseImpl db = getDatabase(servletContext, pathHelper);
    return new AudioFileHelper(pathHelper, serverProps, db, null);
  }

  /**
   * Do alignment of audio file against sentence.
   *
   * @param audioFileHelper
   * @param testAudioFile
   * @param sentence
   * @return
   */
  public PretestScore getASRScoreForAudio(AudioFileHelper audioFileHelper, String testAudioFile, String sentence) {

    logger.debug("getASRScoreForAudio " +testAudioFile);

    PretestScore asrScoreForAudio = null;
    try {
      asrScoreForAudio = audioFileHelper.getASRScoreForAudio(-1, testAudioFile, sentence, 128, 128, false,
        false, Files.createTempDir().getAbsolutePath(), serverProps.useScoreCache());
    } catch (Exception e) {

      logger.error("got "+e,e);
    }

    return asrScoreForAudio;
  }
}