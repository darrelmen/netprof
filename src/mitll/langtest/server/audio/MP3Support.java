package mitll.langtest.server.audio;

import mitll.langtest.client.AudioTag;
import mitll.langtest.server.LogAndNotify;
import mitll.langtest.server.PathHelper;
import mitll.langtest.server.ServerProperties;
import mitll.langtest.server.database.DatabaseImpl;
import org.apache.log4j.Logger;

import java.io.File;

/**
 * Created by go22670 on 9/3/15.
 */
public class MP3Support {
  private static final Logger logger = Logger.getLogger(MP3Support.class);
  private final PathHelper pathHelper;
  ServerProperties serverProperties;

  /**
   * @see AudioFileHelper#AudioFileHelper(PathHelper, ServerProperties, DatabaseImpl, LogAndNotify)
   * @param pathHelper
   * @param serverProperties
   */
  public MP3Support(PathHelper pathHelper, ServerProperties serverProperties) {
    this.pathHelper = pathHelper;
    this.serverProperties = serverProperties;
  }

  /**
   * @param testAudioFile
   * @return
   * @see AudioFileHelper#getASRScoreForAudio
   */
  public String dealWithMP3Audio(String testAudioFile) {
    if (testAudioFile.endsWith("." + AudioTag.COMPRESSED_TYPE)) {
      String noSuffix = removeSuffix(testAudioFile);
      String wavFile = noSuffix + ".wav";
      File test = pathHelper.getAbsoluteFile(wavFile);
      if (!test.exists()) {
        logger.warn("expecting audio file with wav extension, but didn't find " + test.getAbsolutePath());
      }
      return test.exists() ? test.getAbsolutePath() : getWavForMP3(testAudioFile);
    } else {
      return testAudioFile;
    }
  }

  private String removeSuffix(String audioFile) {
    return audioFile.substring(0, audioFile.length() - ("." + AudioTag.COMPRESSED_TYPE).length());
  }

  /**
   * @param audioFile
   * @return
   * @see #dealWithMP3Audio(String)
   * @see mitll.langtest.server.LangTestDatabaseImpl#getImageForAudioFile
   */
  public String getWavForMP3(String audioFile) {
    return getWavForMP3(audioFile, pathHelper.getInstallPath());
  }

  /**
   * Ultimately does lame --decode from.mp3 to.wav
   * <p>
   * Worries about converting from either a relative path to an absolute path (given the webapp install location)
   * or if audioFile is a URL, converting it to a relative path before making an absolute path.
   * <p>
   * Gotta be a better way...
   *
   * @param audioFile        to convert
   * @return
   * @see #getWavForMP3(String)
   */
  private String getWavForMP3(String audioFile, String installPath) {
    assert (audioFile.endsWith(".mp3"));
    String absolutePath = pathHelper.getAbsolute(installPath, audioFile).getAbsolutePath();

    if (!new File(absolutePath).exists())
      logger.error("getWavForMP3 : expecting file at " + absolutePath);
    else {
      AudioConversion audioConversion = new AudioConversion(serverProperties);
      File file = audioConversion.convertMP3ToWav(absolutePath);
      if (file.exists()) {
        String orig = audioFile;
        audioFile = file.getAbsolutePath();
        logger.info("\n\ngetWavForMP3 : from " + orig + " wrote to " + file + " or " + audioFile);
      } else {
        logger.error("getImageForAudioFile : can't find " + file.getAbsolutePath());
      }
    }
    assert (audioFile.endsWith(".wav"));
    return audioFile;
  }

}
