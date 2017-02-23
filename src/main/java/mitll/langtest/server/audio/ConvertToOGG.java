package mitll.langtest.server.audio;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;

/**
 * Created by go22670 on 2/23/17.
 */
class ConvertToOGG extends AudioBase {
  private static final Logger logger = LogManager.getLogger(ConvertToOGG.class);
  private static final boolean DEBUG = true;
  private static final boolean SPEW = true;
  private static final String OGG = ".ogg";

  boolean writeOGG(File absolutePathToWav, boolean overwrite, TrackInfo trackInfo) {
    String oggFile = absolutePathToWav.getAbsolutePath().replace(WAV, OGG);
    File ogg = new File(oggFile);
    if (!ogg.exists() || overwrite) {
      if (DEBUG)
        logger.debug("writeOGG : doing ogg conversion for " + absolutePathToWav);

      if (DEBUG) logger.debug("writeOGG run ogg on " + absolutePathToWav + " making " + oggFile);

      if (!convertToOGGFileAndCheck(getOggenc(), absolutePathToWav.getAbsolutePath(), oggFile, trackInfo)) {
        logger.error("writeOGG ogg File missing for " + absolutePathToWav);
        return false;
      }
    }
    return true;
  }

  private String getOggenc() {
/*    String property = System.getProperty("os.name").toLowerCase();
    boolean isMac = property.contains("mac");
    boolean isWin = property.contains("win");

    String oggEncPath = isMac ? "bin/macos/oggenc" : isWin ? "bin\\win32\\oggenc.exe" : "/usr/bin/oggenc";
    File file = new File(oggEncPath);
    if (!file.exists()) {
      logger.error("can't find oggenc at " + file.getAbsolutePath());
    }*/
    //  oggEncPath = "oggenc";
    return "oggenc";
  }

  /**
   * @param oggPath
   * @param pathToAudioFile
   * @param oggFile
   * @param trackInfo
   * @return
   * @see #writeCompressedVersions
   */
  private boolean convertToOGGFileAndCheck(String oggPath, String pathToAudioFile, String oggFile, TrackInfo trackInfo) {
    if (DEBUG) logger.debug("convertToOGGFileAndCheck convert " + pathToAudioFile + " to " + oggFile);
    String title = trackInfo.getTitle();
    String author = trackInfo.getArtist();
    if (title != null && title.length() > 30) {
      title = title.substring(0, 30);
    }
    if (title == null) title = "";
    ProcessBuilder oggProx = new ProcessBuilder(oggPath, pathToAudioFile,
        "-o", oggFile,
        "-t", title,
        "-a", author,
        "-c", trackInfo.getComment());
    try {
      //logger.debug("running lame" + oggProx.command());
      new ProcessRunner().runProcess(oggProx);
    } catch (IOException e) {
      //  logger.error("Couldn't run " + oggProx);
      logger.error("for " + oggProx + " got " + e, e);
    }

    File testFile = new File(oggFile);
    if (!testFile.exists()) {
      if (!new File(pathToAudioFile).exists()) {
        if (SPEW) logger.error("huh? source file " + pathToAudioFile + " doesn't exist?");
      } else {
        logger.error("didn't write OGG : " + testFile.getAbsolutePath() +
            " exe path " + oggPath +
            " command was " + oggProx.command());
        try {
          if (!new ProcessRunner().runProcess(oggProx, true)) {
            return false;
          }
        } catch (IOException e) {
          logger.error("for " + oggProx + " got " + e, e);
        }

      }
      return false;
    }
    return true;
  }
}
