/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

package mitll.langtest.server.audio;

import mitll.langtest.server.PathHelper;
import mitll.langtest.server.ServerProperties;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;

/**
 * Created by GO22670 on 4/15/2014.
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
   * @param title            mark the mp3 meta data with this title
   * @param artist
   * @param serverProperties
   * @return path of file under bestAudio directory
   * @see mitll.langtest.server.database.custom.UserListManager#getRefAudioPath
   * @see mitll.langtest.server.LangTestDatabaseImpl#addToAudioTable(int, String, mitll.langtest.shared.CommonExercise, String, mitll.langtest.shared.AudioAnswer)
   */
  public String getPermanentAudioPath(PathHelper pathHelper, File fileRef, String destFileName, boolean overwrite,
                                      String id, String title, String artist, ServerProperties serverProperties) {
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
    String s = BEST_AUDIO + File.separator + id + File.separator + destFileName;
    //logger.debug("getPermanentAudioPath : dest path    " + bestDirForExercise.getPath() + " vs " +s);
    if (!fileRef.equals(destination) && !destFileName.equals(AudioConversion.FILE_MISSING)) {
      try {
        FileUtils.copyFile(fileRef, destination);
      } catch (IOException e) {
        logger.error("couldn't copy " +fileRef.getAbsolutePath() + " to " + destination.getAbsolutePath());
      }

      //logger.debug("getPermanentAudioPath : normalizing levels for " + destination.getAbsolutePath());
      new AudioConversion(serverProperties).normalizeLevels(destination);
    } else {
      if (FileUtils.sizeOf(destination) == 0) {
        logger.error("\ngetRefAudioPath : huh? " + destination + " is empty???");
      }
      logger.debug("getPermanentAudioPath : *not* normalizing levels for " + destination.getAbsolutePath());
    }
    ensureMP3(pathHelper, s, overwrite, title, artist, serverProperties);
    return s;
  }

  private void ensureMP3(PathHelper pathHelper, String wavFile, boolean overwrite, String title, String artist,
                         ServerProperties serverProperties) {
    if (wavFile != null) {
      String parent = pathHelper.getInstallPath();

      AudioConversion audioConversion = new AudioConversion(serverProperties);
      if (!audioConversion.exists(wavFile, parent)) {
        parent = pathHelper.getConfigDir();
      }
      if (!audioConversion.exists(wavFile, parent)) {
        logger.error("can't find " + wavFile + " under " + parent);
      }
      audioConversion.ensureWriteMP3(wavFile, parent, overwrite, title, artist);
    } else {
      logger.warn("not converting wav to mp3???\n\n\n");
    }
  }
}
