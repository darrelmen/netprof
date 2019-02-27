/*
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2019. Other request for this document shall be referred
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
 * © 2015-2019 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 */

package mitll.langtest.server.audio.imagewriter;

import mitll.langtest.server.audio.image.AudioImage;
import mitll.langtest.server.audio.image.ImageType;
import mitll.langtest.server.audio.tools.AudioFile;
import mitll.langtest.server.services.AudioServiceImpl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class SimpleImageWriter extends mitll.langtest.server.audio.imagewriter.BasicImageWriter {
  private static final Logger logger = LogManager.getLogger(mitll.langtest.server.audio.imagewriter.SimpleImageWriter.class);

  private static final long SLOW_AUDIO_WRITE_DURATION = 2000l;

  /**
   * @param audioFileName audio file to write images for
   * @param outdir        root directory for images
   * @param imageWidth    width of one tile
   * @param imageHeight   height of one tile
   * @param type
   * @param suffix
   * @return relative path to file on server
   * @see AudioServiceImpl#getImageForAudioFile
   */
  public String writeImage(String audioFileName, String outdir, int imageWidth, int imageHeight,
                           ImageType type, String suffix) {
    Map<ImageType, String> imageTypeStringMap =
        writeImageTypes(audioFileName, outdir, imageWidth, imageHeight, Collections.singleton(type), suffix);
    return imageTypeStringMap.get(type);
  }

  /**
   * Writes one image to a today directory (mm_dd_yy, e.g. 06_17_11).
   * TODO : worry about cleaning up old unused images (from past days)
   *
   * @param audioFileName audio file to write images for
   * @param outdir        root directory for images
   * @param imageWidth    width of one tile
   * @param imageHeight   height of one tile
   * @param suffix
   * @return relative path to file on server
   */
  private Map<ImageType, String> writeImageTypes(String audioFileName,
                                                 String outdir,
                                                 int imageWidth,
                                                 int imageHeight,
                                                 Collection<ImageType> imageTypes,
                                                 String suffix) {
    long start = System.currentTimeMillis();

    if (!audioFileName.endsWith(".wav")) {
      logger.error("huh? asking for images for " +audioFileName);

    }
    // open the audio file
    AudioFile audioFile;
    try {
      audioFile = new AudioFile(audioFileName);
    } catch (Exception e) {
      logger.error("writeImageTypes reading from " + audioFileName + " got " + e, e);
      return Collections.emptyMap();
    }

    int pps = getPixelPerSec(imageWidth, audioFile);
    // make the image directories
    Map<ImageType, File> helperToOutputDir = getImageDirs(outdir, imageTypes);

    float timeGridLineFraction = getTimeGridLineFraction(imageWidth);

    Map<ImageType, String> typeToPath = new HashMap<ImageType, String>();
    for (ImageType type : imageTypes) {
      String relativeFile = getFileForImageType(imageWidth, imageHeight, audioFile, helperToOutputDir.get(type),
          timeGridLineFraction, pps, type, "", suffix);
      typeToPath.put(type, relativeFile);
    }
    long now = System.currentTimeMillis();
    long diff3 = now - start;
    if (diff3 > SLOW_AUDIO_WRITE_DURATION) {
      logger.warn(" : writeImage was slow - it took " + diff3 + " millis.");
    }
    return typeToPath;
  }

  private String getFileForImageType(int imageWidth, int imageHeight, AudioFile audioFile,
                                     File imageDir,
                                     float timeGridLineFraction, int pps,
                                     ImageType type, String prefix, String suffix) {
    AudioImage audioImage =
        ImageFactory.makeImage(type, audioFile,
            imageWidth, imageHeight,
            timeGridLineFraction);
    return writeImageAndGetPathToFile(imageWidth, audioFile, imageDir, pps, prefix, suffix, audioImage);
  }

  private float getTimeGridLineFraction(int tileWidth) {
    return getTimeGridLineFraction(tileWidth, tileWidth);
  }

  /**
   * helps make grid labels legible
   *
   * @param tileWidth
   * @param fullImageWidth
   * @return
   */
  private float getTimeGridLineFraction(int tileWidth, float fullImageWidth) {
    int tileWidthToUse = Math.max(tileWidth, 512);  // helps make grid labels legible
    float gridLineFactor = Math.max(1, fullImageWidth / (float) tileWidthToUse);
    return 1f / (10f * gridLineFactor);
  }

  /**
   * Args:
   * 0: wav file
   * 1: output directory, optional, default to "."
   * 2: image width, optional default to 512
   * 3: image height, optional default to 128
   * 4: image type (waveform, spectrogram) optional default to waveform
   * 5: suffix (optional, default = "")
   * @param arg
   */
  public static void main(String[] arg) {
    if (arg.length == 0) {
      logger.warn("Usage: " +"Args:\n" +
          " 0: wav file path\n" +
          " 1: output directory, optional, default to \".\"\n" +
          " 2: image width,      optional default to 512\n" +
          " 3: image height,     optional default to 128\n" +
          " 4: image type (waveform, spectrogram) optional default to waveform\n" +
          " 5: suffix            optional, default = \"\"");
      return;
    }
    String wavAudioFile = arg[0];
    String outdir = null;
    try {
      outdir = arg[1];
    } catch (Exception e) {
      outdir = ".";
    }
    int width = 0;
    try {
      width = Integer.parseInt(arg[2]);
    } catch (Exception e) {
      width = 512;
    }
    int height = 0;
    try {
      height = Integer.parseInt(arg[3]);
    } catch (Exception e) {
      height = 128;
    }
    ImageType imageType1 = null;
    try {
      imageType1 = ImageType.valueOf(arg[4].toUpperCase());
    } catch (Exception e) {
      imageType1 = ImageType.WAVEFORM;
    }
    String suffix = null;
    try {
      suffix = arg[5];
    } catch (Exception e) {
      suffix = "";
    }

    String absolutePathToImage = new mitll.langtest.server.audio.imagewriter.SimpleImageWriter().writeImage(wavAudioFile, outdir,
        width, height, imageType1, suffix);
    logger.info("wrote to " + new File(absolutePathToImage).getAbsolutePath());
  }
}
