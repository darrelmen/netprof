package mitll.langtest.server.audio;

import audio.tools.FileCopier;
import mitll.langtest.server.PathHelper;
import org.apache.log4j.Logger;
import org.h2.store.fs.FileUtils;

import java.io.File;

/**
 * Created by GO22670 on 4/15/2014.
 */
public class PathWriter {
  private static final Logger logger = Logger.getLogger(PathWriter.class);

  private static final String BEST_AUDIO = "bestAudio";

  /**
   * Skips copying files called FILE_MISSING {@link mitll.langtest.server.audio.AudioConversion#FILE_MISSING}
   * @see mitll.langtest.server.database.custom.UserListManager#getRefAudioPath
   * @see mitll.langtest.server.LangTestDatabaseImpl#addToAudioTable(int, String, mitll.langtest.shared.CommonExercise, String, mitll.langtest.shared.AudioAnswer)
   * @param pathHelper
   * @param fileRef
   * @param destFileName
   * @param overwrite
   * @param id
   * @param title
   * @return
   */
  public String getPermanentAudioPath(PathHelper pathHelper, File fileRef, String destFileName, boolean overwrite, String id, String title) {
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
      new FileCopier().copy(fileRef.getAbsolutePath(), destination.getAbsolutePath());

      logger.debug("getPermanentAudioPath : normalizing levels for " + destination.getAbsolutePath());

      new AudioConversion().normalizeLevels(destination);
    } else {
      if (FileUtils.size(destination.getAbsolutePath()) == 0) {
        logger.error("\ngetRefAudioPath : huh? " + destination + " is empty???");
      }

      logger.debug("getPermanentAudioPath : *not* normalizing levels for " + destination.getAbsolutePath());

    }
    ensureMP3(pathHelper, s, overwrite, title);
    return s;
  }


  private void ensureMP3(PathHelper pathHelper, String wavFile, boolean overwrite, String title) {
    if (wavFile != null) {
      String parent = pathHelper.getInstallPath();

      AudioConversion audioConversion = new AudioConversion();
      if (!audioConversion.exists(wavFile, parent)) {
        parent = pathHelper.getConfigDir();
      }
      if (!audioConversion.exists(wavFile,parent)) {
        logger.error("can't find " + wavFile + " under "  +parent);
      }
      audioConversion.ensureWriteMP3(wavFile, parent, overwrite, title);
    }
    else {
      logger.warn("not converting wav to mp3???\n\n\n");
    }
  }
}
