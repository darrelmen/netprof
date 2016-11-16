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

import mitll.langtest.server.PathHelper;
import mitll.langtest.server.ServerProperties;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 4/15/2014.
 */
public class PathWriter {
  private static final Logger logger = Logger.getLogger(PathWriter.class);

  private static final String BEST_AUDIO = "bestAudio";

  /**
   * Skips copying files called FILE_MISSING {@link mitll.langtest.server.audio.AudioConversion#FILE_MISSING}
   *
   * @param pathHelper
   * @param fileRef
   * @param destFileName
   * @param overwrite
   * @param id
   * @param serverProperties
   * @param trackInfo
   * @return path of file under bestAudio directory
   * @see mitll.langtest.server.database.custom.UserListManager#getRefAudioPath
   * @see mitll.langtest.server.LangTestDatabaseImpl#addToAudioTable
   */
  public String getPermanentAudioPath(PathHelper pathHelper,
                                      File fileRef, String destFileName, boolean overwrite,
                                      String id, ServerProperties serverProperties, TrackInfo trackInfo) {
    final File bestDir = pathHelper.getAbsoluteFile(BEST_AUDIO);
    if (!bestDir.exists() && !bestDir.mkdir()) {
      if (!bestDir.exists()) logger.warn("huh? couldn't make " + bestDir.getAbsolutePath());
    }
    File bestDirForExercise = new File(bestDir, id);
    if (!bestDirForExercise.exists() && !bestDirForExercise.mkdir()) {
      if (!bestDirForExercise.exists()) logger.warn("huh? couldn't make " + bestDirForExercise.getAbsolutePath());
    }
    File destination = new File(bestDirForExercise, destFileName);
    //logger.debug("getPermanentAudioPath : copying from " + fileRef +  " to " + destination.getAbsolutePath());
    String bestAudioPath = BEST_AUDIO + File.separator + id + File.separator + destFileName;
    //logger.debug("getPermanentAudioPath : dest path    " + bestDirForExercise.getPath() + " vs " +bestAudioPath);
    if (!fileRef.equals(destination) && !destFileName.equals(AudioConversion.FILE_MISSING)) {
      copyAndNormalize(fileRef, serverProperties, destination);
    } else {
      if (FileUtils.sizeOf(destination) == 0) {
        logger.error("\ngetRefAudioPath : huh? " + destination + " is empty???");
      }
      logger.debug("getPermanentAudioPath : *not* normalizing levels for " + destination.getAbsolutePath());
    }
    ensureMP3(pathHelper, bestAudioPath, overwrite, serverProperties, trackInfo);
    return bestAudioPath;
  }

  public void copyAndNormalize(File fileRef, ServerProperties serverProperties, File destination) {
    try {
      FileUtils.copyFile(fileRef, destination);
    } catch (IOException e) {
      logger.error("couldn't copy " +fileRef.getAbsolutePath() + " to " + destination.getAbsolutePath());
    }

    // logger.debug("getPermanentAudioPath : normalizing levels for " + destination.getAbsolutePath());
    new AudioConversion(serverProperties).normalizeLevels(destination);
  }

  private void ensureMP3(PathHelper pathHelper, String wavFile, boolean overwrite, ServerProperties serverProperties, TrackInfo trackInfo) {
    if (wavFile != null) {
      String parent = pathHelper.getInstallPath();

      AudioConversion audioConversion = new AudioConversion(serverProperties);
      if (!audioConversion.exists(wavFile, parent)) {
        parent = pathHelper.getConfigDir();
      }
      if (!audioConversion.exists(wavFile, parent)) {
        logger.error("can't find " + wavFile + " under " + parent);
      }
      audioConversion.ensureWriteMP3(wavFile, parent, overwrite, trackInfo);
    } else {
      logger.warn("not converting wav to mp3???\n\n\n");
    }
  }
}
