package mitll.langtest.server.audio;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Created by go22670 on 2/23/17.
 */
public class AudioBase {
  private static final Logger logger = LogManager.getLogger(AudioBase.class);

  public static final String WAV = ".wav";
  private static final boolean DEBUG = false;

  /**
   * @param prefix
   * @return
   * @throws IOException
   * @see #makeTempFile(String)
   */
  protected File makeTempDir(String prefix) throws IOException {
    String prefix1 = "AudioConversion_makeTempDir_for_" + prefix;
    if (DEBUG) logger.info("makeTempDir " + prefix);
    Path audioConversion = Files.createTempDirectory(prefix1);
    if (DEBUG) logger.info("makeTempDir made " + audioConversion);

    File file = audioConversion.toFile();

    if (DEBUG) logger.info("makeTempDir made " + file.getAbsolutePath());

    return file;
  }

  protected String makeTempFile(String prefix) throws IOException {
    return makeTempDir(prefix) + File.separator + "temp" + prefix + WAV;
  }

  protected void copyAndDeleteOriginal(String srcFile, File replacement) throws IOException {
    copyAndDeleteOriginal(new File(srcFile), replacement);
  }

//  private void copyAndDeleteOriginal(File srcFile, File replacement) throws IOException {
//    FileUtils.copyFile(srcFile, replacement);
//    if (Trimmer.DEBUG)
//      Trimmer.logger.debug("copyAndDeleteOriginal " + srcFile.getAbsolutePath() + " to " + replacement.getAbsolutePath());
//    // cleanup
//    deleteParentTempDir(srcFile);
//  }

  void deleteParentTempDir(File srcFile) throws IOException {
    FileUtils.deleteDirectory(new File(srcFile.getParent()));
  }

  protected void copyAndDeleteOriginal(File srcFile, File replacement) throws IOException {
    FileUtils.copyFile(srcFile, replacement);
    if (DEBUG)
      logger.debug("copyAndDeleteOriginal " + srcFile.getAbsolutePath() + " to " + replacement.getAbsolutePath());
    // cleanup
    deleteParentTempDir(srcFile);
  }
}
