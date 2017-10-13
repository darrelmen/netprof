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
import org.jetbrains.annotations.NotNull;

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
  private static final String REFERENCE_AUDIO = "reference audio";
  private final File bestDir;
  private final boolean foundBestDir;

  /**
   * Do some sanity checks...
   *
   * @param properties
   * @see mitll.langtest.server.services.AudioServiceImpl#init
   */
  public PathWriter(ServerProperties properties) {
    bestDir = new File(properties.getMediaDir());
    foundBestDir = bestDir.exists();

    doSanityCheckOnDir(bestDir, REFERENCE_AUDIO);
  }

  public void doSanityCheckOnDir(File bestDir, String dirTitle) {
    if (!bestDir.exists()) {
      logger.warn("If on hydra or hydra2, please make " + dirTitle + " directory " + bestDir.getAbsolutePath());
    } else {
      if (!bestDir.canRead()) {
        logger.error("Please make " + dirTitle + " directory " + bestDir.getAbsolutePath() + " READABLE -" +
            "\n\tchmod -R a+rw " + bestDir.getAbsolutePath());
      }
      if (!bestDir.canWrite()) {
        logger.error("Please make " + dirTitle + " directory " + bestDir.getAbsolutePath() + " WRITEABLE -" +
            "\n\tchmod -R a+rw " + bestDir.getAbsolutePath());
      }
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
   * @param trackInfo        mark the mp3 meta data with this title
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
                                      ServerProperties serverProperties,
                                      TrackInfo trackInfo) {

    // this path will look like
    // /opt/netprof/bestAudio/LANG/bestAudio/1234
    String commonAudioPrefix = bestDir +
        File.separator + language.toLowerCase() +
        File.separator;
    File bestDirForExercise = new File(
        commonAudioPrefix + ServerProperties.BEST_AUDIO
        , "" + exid);
    if (!bestDirForExercise.exists() && (foundBestDir && !bestDirForExercise.mkdirs())) {
      if (!bestDirForExercise.exists()) {
        logger.warn("getPermanentAudioPath huh? couldn't make " + bestDirForExercise.getAbsolutePath()); // need chmod, not writeable
      }
    }
    File destination = new File(bestDirForExercise, destFileName);
    logger.debug("getPermanentAudioPath : copying from" +
        "\n\tprefix  " + commonAudioPrefix +
        "\n\twav     " + wavFileRef +
        "\n\tto dest " + destination.getAbsolutePath());

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
        logger.error("\ngetPermanentAudioPath : huh? " + destination + " is empty???");
      }
      logger.debug("getPermanentAudioPath : *not* normalizing levels for " + destination.getAbsolutePath());
    }
    String s = getAudioConversion(serverProperties).writeCompressedVersions(destination, overwrite, trackInfo);

  //  String audioBaseDir = serverProperties.getAudioBaseDir();
    String audioBaseDir = commonAudioPrefix;
    String relPath = destination.getAbsolutePath().substring(audioBaseDir.length());
    logger.info("getPermanentAudioPath " +
        "\n\tbase      " + audioBaseDir +
        "\n\twrote to  " + s +
        "\n\trel path  " + relPath
    );
    return relPath;
  }

  @NotNull
  private AudioConversion getAudioConversion(ServerProperties serverProperties) {
    return new AudioConversion(serverProperties.shouldTrimAudio(), serverProperties.getMinDynamicRange());
  }

  /**
   * MUST HAVE ACCESS TO AUDIO FILES  - only on hydra1 or hydra2
   *
   * @param fileRef
   * @param serverProperties
   * @param destination
   * @see #getPermanentAudioPath
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
    getAudioConversion(serverProperties).normalizeLevels(destination);
  }
}
