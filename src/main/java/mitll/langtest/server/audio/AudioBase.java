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
  private File makeTempDir(String prefix) throws IOException {
    String prefix1 = "AudioConversion_makeTempDir_for_" + prefix;
    if (DEBUG) logger.info("makeTempDir " + prefix);
    Path audioConversion = Files.createTempDirectory(prefix1);
    if (DEBUG) logger.info("makeTempDir made " + audioConversion);

    File file = audioConversion.toFile();

    if (DEBUG) logger.info("makeTempDir made " + file.getAbsolutePath());

    return file;
  }

  String makeTempFile(String prefix) throws IOException {
    return makeTempDir(prefix) + File.separator + "temp" + prefix + WAV;
  }

  void copyAndDeleteOriginal(String srcFile, File replacement) throws IOException {
    copyAndDeleteOriginal(new File(srcFile), replacement);
  }

//  private void copyAndDeleteOriginal(File srcFile, File replacement) throws IOException {
//    FileUtils.copyFile(srcFile, replacement);
//    if (Trimmer.DEBUG)
//      Trimmer.logger.debug("copyAndDeleteOriginal " + srcFile.getAbsolutePath() + " to " + replacement.getAbsolutePath());
//    // cleanup
//    deleteParentTempDir(srcFile);
//  }

  private void deleteParentTempDir(File srcFile) throws IOException {
    FileUtils.deleteDirectory(new File(srcFile.getParent()));
  }

  void copyAndDeleteOriginal(File srcFile, File replacement) throws IOException {
    FileUtils.copyFile(srcFile, replacement);
    if (DEBUG)
      logger.debug("copyAndDeleteOriginal " + srcFile.getAbsolutePath() + " to " + replacement.getAbsolutePath());
    // cleanup
    deleteParentTempDir(srcFile);
  }
}
