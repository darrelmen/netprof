package mitll.langtest.server.scoring;

import Utils.Log;
import audio.image.ImageType;
import audio.imagewriter.ImageWriter;
import mitll.langtest.shared.scoring.NetPronImageType;
import org.apache.log4j.Logger;
import pronz.dirs.Dirs;
import pronz.speech.ASRParameters;

import java.io.File;
import java.util.Collections;
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
public class Scoring {
  private static Logger logger = Logger.getLogger(Scoring.class);

  private static final String WINDOWS_CONFIGURATIONS = "windowsConfig";
  private static final String LINUX_CONFIGURATIONS = "mtexConfig";
  public static final float SCORE_SCALAR = 1.0f / 0.15f;

  protected Dirs dirs;
/*  private static final float MIN_AUDIO_SECONDS = 0.3f;
  private static final float MAX_AUDIO_SECONDS = 15.0f;*/

  protected String scoringDir;
  protected String os;
  protected String configFullPath;
  protected String deployPath;

  /**
   * @see ASRScoring#ASRScoring(String)
   * @param deployPath
   */
  public Scoring(String deployPath) {
    this.deployPath = deployPath;
    String property = System.getProperty("os.name").toLowerCase();
    this.os = property.contains("win") ? "win32" : property
        .contains("mac") ? "macos"
        : property.contains("linux") ? System
        .getProperty("os.arch").contains("64") ? "linux64"
        : "linux" : "linux";

    scoringDir = deployPath + File.separator + "scoring";

    configFullPath = scoringDir + File.separator + (os.equals("win32") ? WINDOWS_CONFIGURATIONS : LINUX_CONFIGURATIONS);   // TODO point at os specific config file

    dirs = pronz.dirs.Dirs$.MODULE$.apply(scoringDir + File.separator
        + "tmp", "", scoringDir, new Log(null, true));
  }

  /**
   * Given an audio file without a suffix, check if there are label files, and if so, for each one,
   * write out a transcript image file.  Write them to the imageOutDir, and use the specified width and height.
   *
   * @see DTWScoring#score(String, String, String, java.util.Collection, String, int, int)
   * @see ASRScoring#scoreRepeatExercise
   *
   * @param imageOutDir
   * @param imageWidth
   * @param imageHeight
   * @param audioFileNoSuffix
   * @return map of image type to image path, suitable using in setURL on a GWT Image (must be relative to deploy location)
   */
  protected Map<NetPronImageType, String> writeTranscripts(String imageOutDir, int imageWidth, int imageHeight, String audioFileNoSuffix) {
    String pathname = audioFileNoSuffix + ".wav";
    pathname = prependDeploy(pathname);
    if (!new File(pathname).exists()) {
      logger.error("writeTranscripts : can't find " + pathname);
      return Collections.emptyMap();
    }
    imageOutDir = deployPath + File.separator + imageOutDir;

    // These may not all exist. The speech file is created only by multisv
    // right now.
    String phoneLabFile  = prependDeploy(audioFileNoSuffix + ".phones.lab");
    String speechLabFile = prependDeploy(audioFileNoSuffix + ".speech.lab");
    String wordLabFile   = prependDeploy(audioFileNoSuffix + ".words.lab");
    Map<ImageType, String> typeToFile = new HashMap<ImageType, String>();

    if (new File(phoneLabFile).exists()) {
      typeToFile.put(ImageType.PHONE_TRANSCRIPT, phoneLabFile);
    //  System.out.println("writeTranscripts found " + new File(phoneLabFile).getAbsolutePath());
    }
    if (new File(wordLabFile).exists()) {
      typeToFile.put(ImageType.WORD_TRANSCRIPT, wordLabFile);
    //  System.out.println("writeTranscripts found " + new File(wordLabFile).getAbsolutePath());
    }
    if (new File(speechLabFile).exists()) {
      typeToFile.put(ImageType.SPEECH_TRANSCRIPT, speechLabFile);
     // System.out.println("writeTranscripts found " + new File(speechLabFile).getAbsolutePath());
    }

    Map<ImageType, String> typeToImageFile = new ImageWriter().writeTranscripts(pathname, imageOutDir, imageWidth, imageHeight, typeToFile, SCORE_SCALAR);
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
    logger.debug("image map is " + sTypeToImage);
    return sTypeToImage;
  }

  private String prependDeploy(String pathname) {
    if (!new File(pathname).exists()) {
       pathname = deployPath + File.separator + pathname;
    }
    return pathname;
  }
}
