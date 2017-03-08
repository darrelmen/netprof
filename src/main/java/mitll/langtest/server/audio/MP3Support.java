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

import mitll.langtest.client.AudioTag;
import mitll.langtest.server.PathHelper;
import mitll.langtest.server.scoring.AlignDecode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 9/3/15.
 */
class MP3Support {
  private static final Logger logger = LogManager.getLogger(MP3Support.class);
  private static final int SUFFIX_LENGTH = ("." + AudioTag.COMPRESSED_TYPE).length();
  private static final String WAV = ".wav";
  private final PathHelper pathHelper;

  /**
   * @param pathHelper
   * @see AudioFileHelper#AudioFileHelper
   */
  MP3Support(PathHelper pathHelper) {
    this.pathHelper = pathHelper;
  }

  /**
   * TODO : remove language??? will this is always work???
   *
   * @param testAudioFile
   * @return
   * @see AlignDecode#getASRScoreForAudio
   */
  String dealWithMP3Audio(String testAudioFile) {
   // logger.debug("dealWithMP3Audio " + language + " testAudio " + testAudioFile);
    if (!testAudioFile.endsWith(WAV)) {
      String wavFile = removeSuffix(testAudioFile) + WAV;
      File test1 = new File(wavFile);
      if (test1.exists()) {
        return wavFile;
      } else {
        File test = pathHelper.getAbsoluteAudioFile(wavFile);
        if (test.exists()) {
//          logger.info("dealWithMP3Audio found file at " + test.getAbsolutePath());
        }
        else {
          logger.warn("\n\n\ndealWithMP3Audio : expecting audio file with wav extension, but didn't find " + test.getAbsolutePath());
        }
        return test.exists() ? test.getAbsolutePath() : testAudioFile;
      }
    } else {
      return testAudioFile;
    }
  }

  private String removeSuffix(String audioFile) {
    return audioFile.substring(0, audioFile.length() - SUFFIX_LENGTH);
  }

  /**
   * Ultimately does lame --decode from.mp3 to.wav
   * <p>
   * Worries about converting from either a relative path to an absolute path (given the webapp install location)
   * or if audioFile is a URL, converting it to a relative path before making an absolute path.
   * <p>
   * Gotta be a better way...
   *
   * @param audioFile to convert
   * @return
   * @see #getWavForMP3(String, String)
   */
/*  String getWavForMP3(String audioFile, String language) {
    assert (audioFile.endsWith(".mp3"));
    String absolutePath = pathHelper.getAbsoluteAnswerAudioFile(audioFile, language).getAbsolutePath();

    if (!new File(absolutePath).exists())
      logger.error("getWavForMP3 : expecting file at " + absolutePath);
    else {
      AudioConversion audioConversion = new AudioConversion(serverProperties);
      File file = audioConversion.convertMP3ToWav(absolutePath);
      if (file.exists()) {
        String orig = audioFile;
        audioFile = file.getAbsolutePath();
        logger.info("\n\ngetWavForMP3 : from " + orig + " wrote to " + file + " or " + audioFile);
      } else {
        logger.error("getImageForAudioFile : can't find " + file.getAbsolutePath());
      }
    }
    assert (audioFile.endsWith(WAV));
    return audioFile;
  }*/
}
