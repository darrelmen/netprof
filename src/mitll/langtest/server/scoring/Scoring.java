package mitll.langtest.server.scoring;

import audio.image.ImageType;
import audio.imagewriter.ImageWriter;
import mitll.langtest.shared.scoring.NetPronImageType;
import org.apache.log4j.Logger;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Base class for the two types of scoring : DTW (Dynamic Time Warping, using "sv") and ASR (Speech Recognition).
 *
 * Scoring (for now) goes through a thin layer in pronz (scala) to run a config file in hydec.
 *
 * TODO : In the future, we may want to make a new project that has just the hydec interface code in pronz.
 *
 * User: go22670
 * Date: 9/12/12
 * Time: 11:07 AM
 * To change this template use File | Settings | File Templates.
 */
public abstract class Scoring {
  private static Logger logger = Logger.getLogger(Scoring.class);

  private static final String WINDOWS_CONFIGURATIONS = "windowsConfig";
  private static final String LINUX_CONFIGURATIONS = "mtexConfig";
  protected static final float SCORE_SCALAR = 1.0f;// / 0.15f;
  private static final String SCORING = "scoring";

  protected String scoringDir;
  protected String os;
  protected String configFullPath;
  protected String deployPath;

  /**
   * @see ASRScoring#ASRScoring
   *
   * @param  deployPath
   */
  protected Scoring(String deployPath) {
    this.deployPath = deployPath;
    this.os = getOS();
    this.scoringDir = getScoringDir(deployPath);
    this.configFullPath = scoringDir + File.separator +
        (os.equals("win32") ?
        WINDOWS_CONFIGURATIONS :
        LINUX_CONFIGURATIONS);
  }

  public static String getScoringDir(String deployPath) { return deployPath + File.separator + SCORING; }

  private String getOS() {
    String property = System.getProperty("os.name").toLowerCase();
    return property.contains("win") ? "win32" : property
        .contains("mac") ? "macos"
        : property.contains("linux") ? System
        .getProperty("os.arch").contains("64") ? "linux64"
        : "linux" : "linux";
  }

  /**
   * Given an audio file without a suffix, check if there are label files, and if so, for each one,
   * write out a transcript image file.  Write them to the imageOutDir, and use the specified width and height.
   *
   * @see ASRScoring#scoreRepeatExercise
   *
   * @param imageOutDir
   * @param imageWidth
   * @param imageHeight
   * @param audioFileNoSuffix
   * @param useScoreToColorBkg
   * @return map of image type to image path, suitable using in setURL on a GWT Image (must be relative to deploy location)
   */
  protected ImageWriter.EventAndFileInfo writeTranscripts(String imageOutDir, int imageWidth, int imageHeight,
                                                          String audioFileNoSuffix, boolean useScoreToColorBkg) {
    String pathname = audioFileNoSuffix + ".wav";
    pathname = prependDeploy(pathname);
    if (!new File(pathname).exists()) {
      logger.error("writeTranscripts : can't find " + pathname);
      return new ImageWriter.EventAndFileInfo();
    }
    imageOutDir = deployPath + File.separator + imageOutDir;

    boolean foundATranscript = false;
    // These may not all exist. The speech file is created only by multisv
    // right now.
    String phoneLabFile  = prependDeploy(audioFileNoSuffix + ".phones.lab");
    Map<ImageType, String> typeToFile = new HashMap<ImageType, String>();
    if (new File(phoneLabFile).exists()) {
      typeToFile.put(ImageType.PHONE_TRANSCRIPT, phoneLabFile);
      foundATranscript = true;
    }

    String wordLabFile   = prependDeploy(audioFileNoSuffix + ".words.lab");
    if (new File(wordLabFile).exists()) {
      typeToFile.put(ImageType.WORD_TRANSCRIPT, wordLabFile);
      foundATranscript = true;
    }

    String speechLabFile = prependDeploy(audioFileNoSuffix + ".speech.lab");
    if (new File(speechLabFile).exists()) {
      foundATranscript = true;
      typeToFile.put(ImageType.SPEECH_TRANSCRIPT, speechLabFile);
    }
    if (!foundATranscript) {
      logger.error("no label files found, e.g. " + phoneLabFile);
    }

    return new ImageWriter().writeTranscripts(pathname,
        imageOutDir, imageWidth, imageHeight, typeToFile, SCORE_SCALAR, useScoreToColorBkg);
  }

  /**
   * Make sure the paths for each image are relative (don't include the deploy path prefix) and have
   * the slashes going in the right direction.<br></br>
   * I.e. make valid URLs.
   * @param typeToImageFile
   * @return map of image type to URL
   */
  protected Map<NetPronImageType, String> getTypeToRelativeURLMap(Map<ImageType, String> typeToImageFile) {
    Map<NetPronImageType, String> sTypeToImage = new HashMap<NetPronImageType, String>();
    for (Map.Entry<ImageType, String> kv : typeToImageFile.entrySet()) {
      String name = kv.getKey().toString();
      NetPronImageType key = NetPronImageType.valueOf(name);
      String filePath = kv.getValue();
      if (filePath.startsWith(deployPath)) {
        filePath = filePath.substring(deployPath.length()); // make it a relative path
      }
      else {
        logger.error("expecting image " +filePath + "\tto be under " +deployPath);
      }

      filePath = filePath.replaceAll("\\\\", "/");
      if (filePath.startsWith("/")) {
        //System.out.println("removing initial slash from " + filePath);
        filePath = filePath.substring(1);
      }
      sTypeToImage.put(key, filePath);
    }
    return sTypeToImage;
  }

  private String prependDeploy(String pathname) {
    if (!new File(pathname).exists()) {
       pathname = deployPath + File.separator + pathname;
    }
    return pathname;
  }

 // public abstract boolean isPhraseInDict(String phrase);

  //public String getScoringDir() { return scoringDir; }
}
