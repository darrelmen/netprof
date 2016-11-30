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

package mitll.langtest.server.audio;

import mitll.langtest.server.ServerProperties;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 4/15/2014.
 */
public class PathWriter {
  private static final Logger logger = LogManager.getLogger(PathWriter.class);

  //private static final String BEST_AUDIO = "bestAudio";
  private final File bestDir;

  public PathWriter(ServerProperties properties) {
    bestDir = new File(properties.getMediaDir());
    if (!bestDir.exists() && !bestDir.mkdir()) {
      if (!bestDir.exists()) logger.warn("huh? couldn't make " + bestDir.getAbsolutePath());
    }
  }

  /**
   * Skips copying files called FILE_MISSING {@link mitll.langtest.server.audio.AudioConversion#FILE_MISSING}
   *
   * @param wavFileRef
   * @param destFileName
   * @param overwrite
   * @param language
   * @param exid
   * @param title            mark the mp3 meta data with this title
   * @param artist           mark the mp3 meta data with this artist
   * @param serverProperties
   * @return relative path of file under bestAudio directory
   * @see mitll.langtest.server.database.custom.UserListManager#getRefAudioPath
   * @see mitll.langtest.server.services.AudioServiceImpl#addToAudioTable
   */
  public String getPermanentAudioPath(File wavFileRef,
                                      String destFileName,
                                      boolean overwrite,
                                      String language,
                                      int exid,
                                      String title,
                                      String artist,
                                      ServerProperties serverProperties) {
    File bestDirForExercise = new File(bestDir+File.separator+language.toLowerCase(), "" + exid);
    if (!bestDirForExercise.exists() && !bestDirForExercise.mkdirs()) {
      if (!bestDirForExercise.exists()) {
        logger.warn("huh? couldn't make " + bestDirForExercise.getAbsolutePath()); // need chmod, not writeable
      }
    }
    File destination = new File(bestDirForExercise, destFileName);
    logger.debug("getPermanentAudioPath : copying from " + wavFileRef +  " to " + destination.getAbsolutePath());

//    String bestAudioPath =
//        File.separator + language +
//        File.separator + BEST_AUDIO +
//        File.separator + exid +
//        File.separator + destFileName;
    //logger.debug("getPermanentAudioPath : dest path    " + bestDirForExercise.getPath() + " vs " +bestAudioPath);

    if (!wavFileRef.equals(destination) && !destFileName.equals(AudioConversion.FILE_MISSING)) {
      copyAndNormalize(wavFileRef, serverProperties, destination);
    } else {
      if (FileUtils.sizeOf(destination) == 0) {
        logger.error("\ngetRefAudioPath : huh? " + destination + " is empty???");
      }
      logger.debug("getPermanentAudioPath : *not* normalizing levels for " + destination.getAbsolutePath());
    }
//    ensureMP3(bestAudioPath, overwrite, title, artist, serverProperties);

    String s = new AudioConversion(serverProperties).writeMP3Easy(destination, overwrite, title, artist);

    String relPath = destination.getAbsolutePath().substring(serverProperties.getAudioBaseDir().length());
    logger.info("ensureMP3 " +
        "\n\twrote to " + s+
        "\n\trel path " + relPath
    );
    return relPath;
  }

  /**
   * MUST HAVE ACCESS TO AUDIO FILES  - only on pNetProf
   *
   * @see #getPermanentAudioPath(File, String, boolean, String, int, String, String, ServerProperties)
   * @param fileRef
   * @param serverProperties
   * @param destination
   */
  private void copyAndNormalize(File fileRef, ServerProperties serverProperties, File destination) {
    try {
      FileUtils.copyFile(fileRef, destination);
    } catch (IOException e) {
      logger.error("copyAndNormalize couldn't copy " + fileRef.getAbsolutePath() + " to " + destination.getAbsolutePath());
      if (!fileRef.exists()) {
        logger.error("\t source file doesn't exist at  " + fileRef.getAbsolutePath());
      }
    }
    // logger.debug("getPermanentAudioPath : normalizing levels for " + destination.getAbsolutePath());
    new AudioConversion(serverProperties).normalizeLevels(destination);
  }

  /**
   * @see #getPermanentAudioPath(File, String, boolean, String, int, String, String, ServerProperties)
   * @param wavFile
   * @param overwrite
   * @param title
   * @param artist
   * @param serverProperties
   */
  private void ensureMP3(String wavFile,
                         boolean overwrite,
                         String title,
                         String artist,
                         ServerProperties serverProperties) {
    if (wavFile != null) {
      String parent = serverProperties.getMediaDir();

      AudioConversion audioConversion = new AudioConversion(serverProperties);
      if (!audioConversion.exists(wavFile, parent)) {
        logger.warn("ensureMP3 can't find " + wavFile + " under " + parent);
    //    parent = pathHelper.getConfigDir();
        parent = serverProperties.getAnswerDir();
      }
      if (!audioConversion.exists(wavFile, parent)) {
        logger.error("ensureMP3 can't find " + wavFile + " under " + parent);
      }
      String s = audioConversion.ensureWriteMP3(wavFile, parent, overwrite, title, artist);
      logger.info("ensureMP3 wrote " + wavFile + " to " + s);
    } else {
      logger.warn("not converting wav to mp3???\n\n\n");
    }
  }

/*  private void ensureMP3Easy(PathHelper pathHelper,
                         String wavFile,
                         boolean overwrite,
                         String title,
                         String artist,
                         ServerProperties serverProperties) {
    if (wavFile != null) {
      if (new File(wavFile).exists()) {
        AudioConversion audioConversion = new AudioConversion(serverProperties);
        audioConversion.ensureWriteMP3(wavFile, parent, overwrite, title, artist);
      }
      else {
        logger.error("can't find " + wavFile);
      }
    } else {
      logger.warn("not converting wav to mp3???\n\n\n");
    }
  }*/
}
