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
import mitll.langtest.server.audio.tools.AudioFile;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.stream.FileImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 2/23/15.
 */
class BasicImageWriter {
  private static final Logger logger = LogManager.getLogger(mitll.langtest.server.audio.imagewriter.BasicImageWriter.class);

  private static final float JPG_COMPRESSION_QUALITY = 0.3f;

  String getImagePath(AudioFile audioFile, String prefix, String suffix) {
    int numFrames = audioFile.getBuffer().length;
    String realPrefix = prefix.isEmpty() ? "" : prefix + "_";
    String realSuffix = suffix.isEmpty() ? "" : "_" + suffix;
    return realPrefix + audioFile.getName().substring(0, audioFile.getName().length() - 4) + "_" + numFrames + realSuffix;
  }

  String getImagePath(File imageDir, int pps, AudioImage.IMAGE_FORMAT format, String imagePrefix) {
    String suffix = "_0.";
    return getImagePrefix(imageDir, imagePrefix, pps) + suffix + format.toString().toLowerCase();
  }

  private String getImagePrefix(File imageDir, String noSuffix, int zoomLevel) {
    return imageDir.getPath() + File.separator + noSuffix + "_" + zoomLevel;
  }

  int getPixelPerSec(float imageWidth, AudioFile audioFile) {
    float audioLenSeconds = getAudioLenSeconds(audioFile);

    // scale pps so that when mult by the audio len it generates an image of the desired width
    float currentPixelPerSec = 100.0f;
    currentPixelPerSec *= imageWidth / (currentPixelPerSec * audioLenSeconds);

    /*    if (false) logger.debug("for " + audioFile.getName() + " fps " +fps +" buffer len " + displayEnd +
" audio len " + audioLenSeconds + " pps " + currentPixelPerSec);*/
    return (int) currentPixelPerSec;
  }

  private float getAudioLenSeconds(AudioFile audioFile) {
    int displayEnd = audioFile.getBuffer().length;
    float fps = audioFile.getFPS();
    return getLen(0, displayEnd, fps);
  }

  private float toSeconds(int idx, float fps) {
    return (float) idx / fps;
  }

  private float getLen(int displayStart, int displayEnd, float fps) {
    return toSeconds(displayEnd - displayStart, fps);
  }

  Map<ImageType, File> getImageDirs(String outdir, Collection<ImageType> imageTypes) {
    int zoomLevel = 1;
    File outDirectory = new File(outdir);

    if (!outDirectory.mkdir() && !outDirectory.exists()) {
      logger.error("writeImage couldn't make " + outdir);
    }

    String outdir1 = setupTodayOutputDir(outdir);
    return makeImageDirectories(outdir1, imageTypes, zoomLevel);
  }

  private String setupTodayOutputDir(String outdir) {
    //  DateFormat dateFormat = new SimpleDateFormat("MM_dd_yy");
    DateFormat dateFormat = new SimpleDateFormat("MM_yy");
    String today = dateFormat.format(new Date());
    outdir = outdir + File.separator + today;
    /*boolean mkdir =*/
    new File(outdir).mkdir();
    return outdir;
  }

  private Map<ImageType, File> makeImageDirectories(String outdir, Collection<ImageType> imageTypes, int zoomLevel) {
    File zoomDir = new File(outdir + File.separator + "zoom_" + zoomLevel);
    if (!zoomDir.mkdir() && !zoomDir.exists()) logger.error("makeImageDirectories couldn't make " + zoomDir);

    Map<ImageType, File> helperToOutputDir = new HashMap<ImageType, File>();
    for (ImageType type : imageTypes) {
      File writeDir = new File(zoomDir + File.separator + type.toString().toLowerCase());
      if (!writeDir.mkdir() && !writeDir.exists()) logger.error("makeImageDirectories couldn't make " + writeDir);
      helperToOutputDir.put(type, writeDir);
    }
    return helperToOutputDir;
  }

  /**
   * @param imageWidth
   * @param audioFile
   * @param imageDir
   * @param pps
   * @param prefix
   * @param suffix
   * @param audioImage
   * @return
   * @see TranscriptWriter#getFileForTranscriptImageType(int, int, AudioFile, File, int, Map, float, ImageType, boolean, String, String)
   */
  String writeImageAndGetPathToFile(int imageWidth, AudioFile audioFile, File imageDir, int pps,
                                    String prefix, String suffix, AudioImage audioImage) {
    String imagePrefix = getImagePath(audioFile, prefix, suffix);
    File imageFile = writeOneImage(imageWidth, JPG_COMPRESSION_QUALITY, imagePrefix, pps, audioImage, imageDir);

    // postcondition check
    if (!imageFile.exists()) {
      logger.error("huh? '" + imageFile.getAbsolutePath() + "' doesn't exist?");
    }

    return getImagePath(imageDir, pps, audioImage.getFormat(), imagePrefix);
  }

  private File writeOneImage(int tileWidth, float jpgQuality,
                             String noSuffix,
                             int pps,
                             AudioImage audioImage, File imageDir) {
    String imageFilePrefix = getImagePrefix(imageDir, noSuffix, pps);
    return writeTile(0, tileWidth, imageFilePrefix, audioImage, jpgQuality);
  }

  /**
   * @param j          index of tile
   * @param tileWidth  image width for this tile
   * @param image      complete image
   * @param jpgQuality controls quality/compression
   * @return file name written
   * @see #writeOneImage
   */
  private File writeTile(int j, int tileWidth, String imagePrefix, AudioImage image, float jpgQuality) {
    int xStart = j * tileWidth;

    try {
      BufferedImage tile = image.getSubimage(xStart, tileWidth);
      AudioImage.IMAGE_FORMAT imageFormat = image.getFormat();
      return writeImageGivenPrefix(imagePrefix, j, imageFormat, jpgQuality, tile);
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  private File writeImageGivenPrefix(String imagePrefix, int j,
                                     AudioImage.IMAGE_FORMAT imageFormat, float jpgQuality, BufferedImage tile) throws IOException {
    String imageFile = imagePrefix + "_" + j + "." + imageFormat;
    // logger.error("writing " +imageFile + " xs " + xStart + " ys " +0 + " xe " + sliceWidth + " ye " +ym);
    File outputfile = new File(imageFile);
    writeImageToFile(imageFormat, jpgQuality, outputfile, tile);
    return outputfile;
  }

  private void writeImageToFile(AudioImage.IMAGE_FORMAT imageFormat, float jpgQuality,
                                File outputfile, BufferedImage tile) throws IOException {
    if (imageFormat == AudioImage.IMAGE_FORMAT.JPG) {
      writeJPG(tile, outputfile, jpgQuality);
    } else {
      if (!ImageIO.write(tile, imageFormat.toString(), outputfile)) {
        logger.error("don't know how to write an image of type " + imageFormat);
      }
    }
  }

  /**
   * Can't just do ImageIO.write because we want to tell it the compression quality.
   *
   * @param imageToWrite       image data to write to file
   * @param outputFile         file to write data to
   * @param compressionQuality ranges between 0 and 1, 0-lowest, 1-highest quality
   * @see #writeImageToFile(mitll.langtest.server.audio.image.AudioImage.IMAGE_FORMAT, float, File, BufferedImage)
   */
  private void writeJPG(BufferedImage imageToWrite, File outputFile, float compressionQuality) {
    javax.imageio.ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
    ImageWriteParam iwp = writer.getDefaultWriteParam();
    iwp.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
    iwp.setCompressionQuality(compressionQuality);

    try {
      FileImageOutputStream output = new FileImageOutputStream(outputFile);
      writer.setOutput(output);
      IIOImage image = new IIOImage(imageToWrite, null, null);
      writer.write(null, image, iwp);
      writer.dispose();
      output.flush();
      output.close();
    } catch (IOException e) {
      logger.error("got " + e.getMessage() + " writing " + outputFile, e);
    }
  }
}
