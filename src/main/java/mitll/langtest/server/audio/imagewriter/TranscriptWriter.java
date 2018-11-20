/*
 *
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2015. Other request for this document shall be referred
 * to DLIFLC.
 *
 * WARNING: This document may contain technical data whose export is restricted
 * by the Arms Export Control Act (AECA) or the Export Administration Act (EAA).
 * Transfer of this data by any means to a non-US person who is not eligible to
 * obtain export-controlled data is prohibited. By accepting this data, the consignee
 * agrees to honor the requirements of the AECA and EAA. DESTRUCTION NOTICE: For
 * unclassified, limited distribution documents, destroy by any method that will
 * prevent disclosure of the contents or reconstruction of the document.
 *
 * This material is based upon work supported under Air Force Contract No.
 * FA8721-05-C-0002 and/or FA8702-15-D-0001. Any opinions, findings, conclusions
 * or recommendations expressed in this material are those of the author(s) and
 * do not necessarily reflect the views of the U.S. Air Force.
 *
 * © 2015 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 *
 *
 */

package mitll.langtest.server.audio.imagewriter;

import mitll.langtest.server.audio.image.AudioImage;
import mitll.langtest.server.audio.image.ImageType;
import mitll.langtest.server.audio.image.TranscriptEvent;
import mitll.langtest.server.audio.image.TranscriptReader;
import mitll.langtest.server.audio.tools.AudioFile;
import mitll.langtest.server.scoring.Scoring;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 2/23/15.
 */
public class TranscriptWriter extends mitll.langtest.server.audio.imagewriter.BasicImageWriter {
  private static final Logger logger = LogManager.getLogger(mitll.langtest.server.audio.imagewriter.TranscriptWriter.class);

  /**
   * @param audioFileName
   * @param outdir
   * @param imageWidth
   * @param imageHeight
   * @param imageTypes
   * @param scoreScalar
   * @param useScoreToColorBkg
   * @param prefix
   * @param suffix
   * @return
   * @see mitll.langtest.server.scoring.Scoring#writeTranscripts
   */
  public EventAndFileInfo writeTranscripts(String audioFileName, String outdir, int imageWidth, int imageHeight,
                                           Map<ImageType, String> imageTypes, float scoreScalar,
                                           boolean useScoreToColorBkg, String prefix, String suffix,
                                           boolean usePhone,
                                           Map<String, String> phoneToDisplay) {
    Map<ImageType, Map<Float, TranscriptEvent>> typeToEvent = getImageTypeMapMap(imageTypes, usePhone, phoneToDisplay);
    //    logger.debug("prefix " + prefix);
    return getEventAndFileInfo(audioFileName, outdir, imageWidth, imageHeight, imageTypes.keySet(),
        scoreScalar, useScoreToColorBkg, prefix, suffix, typeToEvent);
  }

  /**
   * @param audioFileName
   * @param outdir
   * @param imageWidth
   * @param imageHeight
   * @param imageTypes1
   * @param scoreScalar
   * @param useScoreToColorBkg
   * @param prefix
   * @param suffix
   * @param typeToEvent
   * @return
   * @see #writeTranscripts
   * @see Scoring#writeTranscriptsCached
   */
  public EventAndFileInfo getEventAndFileInfo(String audioFileName, String outdir, int imageWidth, int imageHeight,
                                              Collection<ImageType> imageTypes1, float scoreScalar,
                                              boolean useScoreToColorBkg, String prefix, String suffix,
                                              Map<ImageType, Map<Float, TranscriptEvent>> typeToEvent) {
    // logger.debug("prefix " + prefix);
    Map<ImageType, String> imageTypeStringMap =
        writeTranscriptImageTypes(audioFileName, outdir, imageWidth, imageHeight, imageTypes1, typeToEvent, scoreScalar, useScoreToColorBkg, prefix, suffix);
    return new EventAndFileInfo(imageTypeStringMap, typeToEvent);
  }

  private final TranscriptReader transcriptReader = new TranscriptReader();

  /**
   * @param imageTypes
   * @param usePhone
   * @param phoneToDisplay
   * @return
   * @throws IOException
   * @see #writeTranscripts
   */
  public Map<ImageType, Map<Float, TranscriptEvent>> getImageTypeMapMap(Map<ImageType, String> imageTypes,
                                                                        boolean usePhone,
                                                                        Map<String, String> phoneToDisplay) {

    Map<ImageType, Map<Float, TranscriptEvent>> typeToEvent = new HashMap<>();
    for (Map.Entry<ImageType, String> o : imageTypes.entrySet()) {
      ImageType imageType = o.getKey();
      boolean reallyUsePhone = imageType.equals(ImageType.PHONE_TRANSCRIPT) && usePhone;
      typeToEvent.put(imageType, transcriptReader.readEventsFromString(o.getValue(), reallyUsePhone, phoneToDisplay));
    }
    return typeToEvent;
  }

  /**
   * Writes one image to a today directory (mm_dd_yy, e.g. 06_17_11).
   * TODO : worry about cleaning up old unused images (from past days)
   *
   * @param audioFileName      audio file to write images for
   * @param outdir             root directory for images
   * @param imageWidth         width of one tile
   * @param imageHeight        height of one tile
   * @param typeToEvent        map of time (seconds) to {@link mitll.langtest.server.audio.image.TranscriptEvent}
   * @param scoreScalar        allows us to scale the scores, e.g. a scalar = 2 means 0.5 scores are colored as 1.0
   * @param useScoreToColorBkg
   * @param prefix
   * @param suffix
   * @return relative path to file on server
   * @see #getEventAndFileInfo(String, String, int, int, Collection, float, boolean, String, String, Map)
   */
  private Map<ImageType, String> writeTranscriptImageTypes(String audioFileName,
                                                           String outdir,
                                                           int imageWidth, int imageHeight,
                                                           Collection<ImageType> imageTypes,
                                                           Map<ImageType, Map<Float, TranscriptEvent>> typeToEvent,
                                                           float scoreScalar, boolean useScoreToColorBkg, String prefix, String suffix) {
    long start = System.currentTimeMillis();

    // open the audio file
    AudioFile audioFile;
    try {
      audioFile = new AudioFile(audioFileName);
    } catch (IOException e) {
      logger.error("got " + e, e);
      return null;
    }

    int pps = getPixelPerSec(imageWidth, audioFile);
    // make the image directories
    Map<ImageType, File> helperToOutputDir = getImageDirs(outdir, imageTypes);

    Map<ImageType, String> typeToPath = new HashMap<>();
    for (ImageType type : imageTypes) {
      File imageDir = helperToOutputDir.get(type);
      String relativeFile = getFileForTranscriptImageType(imageWidth, imageHeight, audioFile,
          imageDir,
          pps, typeToEvent.get(type), scoreScalar, type, useScoreToColorBkg, prefix, suffix);
      typeToPath.put(type, relativeFile);
    }
    long now = System.currentTimeMillis();
    long diff3 = now - start;
    if (diff3 > 70L) {
      logger.info("writeTranscriptImageTypes took " + diff3 + " millis.");
    }
    return typeToPath;
  }

  /**
   * @param imageWidth
   * @param imageHeight
   * @param audioFile
   * @param imageDir
   * @param pps
   * @param events
   * @param scoreScalar
   * @param type
   * @param useScoreToColorBkg
   * @param prefix
   * @param suffix
   * @return
   * @see #writeTranscriptImageTypes(String, String, int, int, Collection, Map, float, boolean, String, String)
   */
  private String getFileForTranscriptImageType(int imageWidth, int imageHeight, AudioFile audioFile,
                                               File imageDir,
                                               int pps,
                                               Map<Float, TranscriptEvent> events, float scoreScalar,
                                               ImageType type, boolean useScoreToColorBkg,
                                               String prefix, String suffix) {
    AudioImage.IMAGE_FORMAT format = type == ImageType.SPECTROGRAM ? AudioImage.IMAGE_FORMAT.JPG : AudioImage.IMAGE_FORMAT.PNG;
    String imagePath = getImagePath(audioFile, imageDir, pps, format, prefix, suffix);
    if (new File(imagePath).exists()) {
      //  logger.debug("file already exists at " + new File(imagePath).getAbsolutePath());
      return imagePath;
    } else {
      // logger.debug("no file at " + new File(imagePath).getAbsolutePath());
    }

    AudioImage audioImage =
        ImageFactory.makeTranscriptImage(type, audioFile,
            imageWidth, imageHeight,
            events, scoreScalar, useScoreToColorBkg);
    return writeImageAndGetPathToFile(imageWidth, audioFile, imageDir, pps, prefix, suffix, audioImage);
  }

  /**
   * @param audioFile
   * @param imageDir
   * @param pps
   * @param format
   * @param prefix
   * @param suffix
   * @return
   * @see #getFileForTranscriptImageType(int, int, mitll.langtest.server.audio.tools.AudioFile, File, int, Map, float, mitll.langtest.server.audio.image.ImageType, boolean, String, String)
   */
  private String getImagePath(AudioFile audioFile, File imageDir, int pps, AudioImage.IMAGE_FORMAT format, String prefix, String suffix) {
    String imagePrefix = getImagePath(audioFile, prefix, suffix);
    return getImagePath(imageDir, pps, format, imagePrefix);
  }
}
