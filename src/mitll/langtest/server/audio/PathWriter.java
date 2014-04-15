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
/*

  public PathWriter() {
  }
*/

  /**
   * @see mitll.langtest.server.database.custom.UserListManager#getRefAudioPath(mitll.langtest.shared.custom.UserExercise, java.io.File, String, boolean)
   * @param pathHelper
   * @param fileRef
   * @param destFileName
   * @param overwrite
   * @param id
   * @return
   */
  public String getPermanentAudioPath(PathHelper pathHelper, File fileRef, String destFileName, boolean overwrite, String id) {
    final File bestDir = pathHelper.getAbsoluteFile(BEST_AUDIO);
    if (!bestDir.exists() && !bestDir.mkdir()) {
      if (!bestDir.exists()) logger.warn("huh? couldn't make " + bestDir.getAbsolutePath());
    }
    File bestDirForExercise = new File(bestDir, id);
    if (!bestDirForExercise.exists() && !bestDirForExercise.mkdir()) {
      if (!bestDirForExercise.exists()) logger.warn("huh? couldn't make " + bestDirForExercise.getAbsolutePath());
    }
    File destination = new File(bestDirForExercise, destFileName);
    //logger.debug("getRefAudioPath : copying from " + fileRef +  " to " + destination.getAbsolutePath());
    String s = BEST_AUDIO + File.separator + id + File.separator + destFileName;
    //logger.debug("getRefAudioPath : dest path    " + bestDirForExercise.getPath() + " vs " +s);
    if (!fileRef.equals(destination)) {
      new FileCopier().copy(fileRef.getAbsolutePath(), destination.getAbsolutePath());

      new AudioConversion().normalizeLevels(destination);
    } else {
      if (FileUtils.size(destination.getAbsolutePath()) == 0)
        logger.error("\ngetRefAudioPath : huh? " + destination + " is empty???");
    }
    ensureMP3(pathHelper, s, overwrite);
    return s;
  }


  private void ensureMP3(PathHelper pathHelper, String wavFile, boolean overwrite) {
    if (wavFile != null) {
      new AudioConversion().ensureWriteMP3(wavFile, pathHelper.getInstallPath(), overwrite);
    }
  }
}
