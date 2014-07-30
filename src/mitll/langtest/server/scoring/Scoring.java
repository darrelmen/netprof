package mitll.langtest.server.scoring;

import audio.image.ImageType;
import audio.image.TranscriptEvent;
import audio.image.TranscriptReader;
import audio.imagewriter.ImageWriter;
import mitll.langtest.shared.scoring.NetPronImageType;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
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
  private static final Logger logger = Logger.getLogger(Scoring.class);

  protected static final float SCORE_SCALAR = 1.0f;// / 0.15f;
  private static final String SCORING = "scoring";

  protected final String scoringDir;
  protected final String deployPath;

  /**
   * @see ASRScoring#ASRScoring
   *
   * @param  deployPath
   */
  protected Scoring(String deployPath) {
    this.deployPath = deployPath;
    this.scoringDir = getScoringDir(deployPath);
  }

  private static String getScoringDir(String deployPath) { return deployPath + File.separator + SCORING; }

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
   * @param prefix
   * @param suffix
   * @param decode if true don't bother to write out images for word and phone
   * @return map of image type to image path, suitable using in setURL on a GWT Image (must be relative to deploy location)
   */
  protected ImageWriter.EventAndFileInfo writeTranscripts(String imageOutDir, int imageWidth, int imageHeight,
                                                          String audioFileNoSuffix, boolean useScoreToColorBkg,
                                                          String prefix, String suffix, boolean decode) {
    String pathname = audioFileNoSuffix + ".wav";
    pathname = prependDeploy(pathname);
    if (!new File(pathname).exists()) {
      logger.error("writeTranscripts : can't find " + pathname);
      return new ImageWriter.EventAndFileInfo();
    }
    imageOutDir = deployPath + File.separator + imageOutDir;

    boolean foundATranscript = false;
    // These may not all exist. The speech file is created only by multisv right now.
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

    if (decode || imageWidth < 0) {  // hack to skip image generation
      return getEventInfo(typeToFile);
    } else {
      return new ImageWriter().writeTranscripts(pathname,
        imageOutDir, imageWidth, imageHeight, typeToFile, SCORE_SCALAR, useScoreToColorBkg, prefix, suffix);
    }
  }

  private ImageWriter.EventAndFileInfo getEventInfo(Map<ImageType, String> imageTypes) {
    Map<ImageType, Map<Float, TranscriptEvent>> typeToEvent = new HashMap<ImageType, Map<Float, TranscriptEvent>>();
    try {
      for (Map.Entry<ImageType, String> o : imageTypes.entrySet()) {
        typeToEvent.put(o.getKey(), new TranscriptReader().readEventsFromFile(o.getValue()));
      }
      return new ImageWriter.EventAndFileInfo(new HashMap<ImageType, String>(), typeToEvent);
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }
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
    if (typeToImageFile == null) {
      logger.error("huh? typeToImageFile is null?");
    }
    else {
      for (Map.Entry<ImageType, String> kv : typeToImageFile.entrySet()) {
        String name = kv.getKey().toString();
        NetPronImageType key = NetPronImageType.valueOf(name);
        String filePath = kv.getValue();
        if (filePath.startsWith(deployPath)) {
          filePath = filePath.substring(deployPath.length()); // make it a relative path
        } else {
          logger.error("expecting image " + filePath + "\tto be under " + deployPath);
        }

        filePath = filePath.replaceAll("\\\\", "/");
        if (filePath.startsWith("/")) {
          //System.out.println("removing initial slash from " + filePath);
          filePath = filePath.substring(1);
        }
        sTypeToImage.put(key, filePath);
      }
    }
    return sTypeToImage;
  }

  private String prependDeploy(String pathname) {
    if (!new File(pathname).exists()) {
       pathname = deployPath + File.separator + pathname;
    }
    return pathname;
  }
}
