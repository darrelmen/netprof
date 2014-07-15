package mitll.langtest.server;

import com.google.common.io.Files;
import mitll.langtest.server.audio.AudioFileHelper;
import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.shared.scoring.PretestScore;
import net.sf.json.JSONObject;
import org.apache.log4j.Logger;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Map;

/**
 *
 * All in support of Liz tethered iOS app.
 *
 * User: GO22670
 */
@SuppressWarnings("serial")
public class ScoreServlet extends DatabaseServlet {
  private static final Logger logger = Logger.getLogger(ScoreServlet.class);
  static final int BUFFER_SIZE = 4096;

  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    String pathInfo = request.getPathInfo();
    logger.debug("ScoreServlet.doPost : Request " + request.getQueryString() + " path " + pathInfo +
      " uri " + request.getRequestURI() + "  " + request.getRequestURL() + "  " + request.getServletPath());

    response.setContentType("application/json");

    // Gets file name for HTTP header
    String fileName = request.getHeader("fileName");
    String word = request.getHeader("word");

    File tempDir = Files.createTempDir();
    File saveFile = new File(tempDir + File.separator+ fileName);

    // prints out all header values
/*    logger.debug("===== Begin headers =====");
    Enumeration<String> names = request.getHeaderNames();
    while (names.hasMoreElements()) {
      String headerName = names.nextElement();
      System.out.println(headerName + " = " + request.getHeader(headerName));
    }
    logger.debug("===== End headers =====\n");*/

    // opens input stream of the request for reading data
    InputStream inputStream = request.getInputStream();

    // opens an output stream for writing file
    FileOutputStream outputStream = new FileOutputStream(saveFile);

    byte[] buffer = new byte[BUFFER_SIZE];
    int bytesRead = -1;
    logger.debug("Receiving data...");

    while ((bytesRead = inputStream.read(buffer)) != -1) {
      outputStream.write(buffer, 0, bytesRead);
    }

   // System.out.println("Data received.");
    outputStream.close();
    inputStream.close();

    logger.debug("File written to: " + saveFile.getAbsolutePath());

    //String testAudioFile = "C:\\Users\\go22670\\DLITest\\merge\\bootstrap\\war\\config\\english\\bestAudio\\4\\Fast.wav";
    AudioFileHelper audioFileHelper = getAudioFileHelper();
    PretestScore book = getASRScoreForAudio(audioFileHelper, saveFile.getAbsolutePath(), word);

    // TODO : return json equivalent for pretestscore

    JSONObject jsonObject = new JSONObject();
    jsonObject.put("score",book.getHydecScore());
    logger.debug("score for " + word + " and " + saveFile.getName() + " = " + book);
    ServletOutputStream outputStream1 = response.getOutputStream();
    outputStream1.println(jsonObject.toString());

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
   // logger.debug("getASRScoreForAudio " +testAudioFile);

    PretestScore asrScoreForAudio = null;
    try {
      asrScoreForAudio = audioFileHelper.getASRScoreForAudio(-1, testAudioFile, sentence, 128, 128, false,
        false, Files.createTempDir().getAbsolutePath(), serverProps.useScoreCache(), "");
    } catch (Exception e) {
      logger.error("got "+e,e);
    }

    return asrScoreForAudio;
  }
}