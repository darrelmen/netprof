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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;

/**
 * Created by go22670 on 2/23/17.
 */
public class Trimmer extends AudioBase {
  private static final Logger logger = LogManager.getLogger(Trimmer.class);
  private static final double DIFF_THRESHOLD = 0.2;

  public static final String SIXTEEN_K_SUFFIX = "_16K";
  public static final String FILE_MISSING = "FILE_MISSING";
  private static final String T_VALUE = "" + 7;

  private static final boolean DEBUG = true;

  /**
   * Note that wavFile input will be changed if trim is successful.
   * <p>
   * If trimming doesn't really change the length, we leave it alone {@link #DIFF_THRESHOLD}.
   * <p>
   * Trimmed file will be empty if it's not successful.
   *
   * @param wavFile to trim silence from - will be replaced in place
   * @return
   * @see AudioConversion#convertBase64ToAudioFiles
   */
  TrimInfo trimSilence(AudioCheck audioCheck, final File wavFile) {
    if (DEBUG) logger.info("trimSilence " + wavFile.getAbsolutePath());
    if (!wavFile.exists()) {
      logger.error("trimSilence " + wavFile + " doesn't exist");
      return new TrimInfo();
    }
    try {
      long then = System.currentTimeMillis();
      String trimmed = doTrimSilence(wavFile.getAbsolutePath());

      double durationInSeconds = audioCheck.getDurationInSeconds(wavFile);
      double durationInSecondsTrimmed = audioCheck.getDurationInSeconds(trimmed);
      double diff = durationInSeconds - durationInSecondsTrimmed;
      if (durationInSecondsTrimmed > 0.1 && diff > DIFF_THRESHOLD) {
        copyAndDeleteOriginal(trimmed, wavFile);

        if (DEBUG) logger.debug("trimSilence (" + //props.getLanguage() +
            ")  convert original " + wavFile.getName() +
            " to trim wav file : " + durationInSeconds + " before, " + durationInSecondsTrimmed + " after.");

        long now = System.currentTimeMillis();
        if (now - then > 0) {
          logger.debug("trimSilence (" + //props.getLanguage() +
              "): took " + (now - then) + " millis to convert original " + wavFile.getName() +
              " to trim wav file : " + durationInSeconds + " before, " + durationInSecondsTrimmed + " after.");
        }
        return new TrimInfo(durationInSecondsTrimmed, true);
      } else {
        logger.info("trimSilence : took " + (System.currentTimeMillis() - then) + " millis to NOT convert original " + wavFile.getName() +
            " to trim wav file : " + durationInSeconds + " before, " + durationInSecondsTrimmed + " after.");

        return new TrimInfo(durationInSeconds, false);
      }
    } catch (IOException e) {
      logger.error("trimSilence on " + wavFile.getAbsolutePath() + " got " + e, e);
      return new TrimInfo();
    }
  }

  /**
   * TODO: Why all the stuff with a temp dir???
   * creating and deleting???
   * <p>
   * sox $sourcewav $outputwav vad -t 6 -p 0.20 reverse vad -t 6 -p 0.20 reverse
   *
   * @param pathToAudioFile
   * @return file that should be cleaned up
   * @throws IOException
   */
  private String doTrimSilence(String pathToAudioFile) throws IOException {
    final String tempTrimmed = makeTempFile("doTrimSilence");

    if (DEBUG)
      logger.info("doTrimSilence running sox on " + new File(pathToAudioFile).getAbsolutePath() + " to produce " + new File(tempTrimmed).getAbsolutePath());
    String trimBefore = "0.30";// + trimMillisBefore;
    String trimAfter = "0.30";// + trimMillisAfter;
    ProcessBuilder soxFirst = new ProcessBuilder(
        "sox",
        pathToAudioFile,
        tempTrimmed,
        "vad", "-t", T_VALUE, "-p", trimBefore, "reverse", "vad", "-t", T_VALUE, "-p", trimAfter, "reverse");

//    logger.error("doTrimSilence trim silence on " + pathToAudioFile);
//    String asRunnable = soxFirst.command().toString().replaceAll(",", " ");
    if (DEBUG) logger.info("doTrimSilence " + soxFirst.command());

    if (!new ProcessRunner().runProcess(soxFirst)) {
      //logger.info("tempDir Exists " + exists);
      logger.info("pathToAudioFile exists " + new File(pathToAudioFile).exists());
      logger.info("tempTrimmed exists     " + new File(tempTrimmed).exists());
      logger.error("couldn't do trim silence on " + pathToAudioFile);
      String asRunnable2 = soxFirst.command().toString().replaceAll(",", " ");
      logger.info("path " + asRunnable2);
    }
    if (DEBUG) logger.info("doTrimSilence finished " + soxFirst.command());

    return tempTrimmed;
  }

}
