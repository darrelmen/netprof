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
import mitll.langtest.server.LogAndNotify;
import mitll.langtest.server.PathHelper;
import mitll.langtest.server.ServerProperties;
import mitll.langtest.server.database.DatabaseImpl;
import org.apache.log4j.Logger;

import java.io.File;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 9/3/15.
 */
class MP3Support {
  private static final Logger logger = Logger.getLogger(MP3Support.class);
  private static final int SUFFIX_LENGTH = ("." + AudioTag.COMPRESSED_TYPE).length();
  private static final String WAV = ".wav";
  private final PathHelper pathHelper;
  private ServerProperties serverProperties;

  /**
   * @param pathHelper
   * @param serverProperties
   * @see AudioFileHelper#AudioFileHelper(PathHelper, ServerProperties, DatabaseImpl, LogAndNotify)
   */
  MP3Support(PathHelper pathHelper, ServerProperties serverProperties) {
    this.pathHelper = pathHelper;
    this.serverProperties = serverProperties;
  }

  /**
   * TODO : remove language??? will this is always work???
   * @param testAudioFile
   * @param language
   * @return
   * @see AudioFileHelper#getASRScoreForAudio
   */
  String dealWithMP3Audio(String testAudioFile, String language) {
    if (!testAudioFile.endsWith(WAV)) {
      String wavFile = removeSuffix(testAudioFile) + WAV;
      File test = pathHelper.getAbsoluteAudioFile(wavFile);
      if (!test.exists()) {
        logger.warn("\n\n\ndealWithMP3Audio : expecting audio file with wav extension, but didn't find " + test.getAbsolutePath());
      }
      return test.exists() ? test.getAbsolutePath() : getWavForMP3(testAudioFile, language);
    } else {
      return testAudioFile;
    }
  }

  private String removeSuffix(String audioFile) {
    return audioFile.substring(0, audioFile.length() - SUFFIX_LENGTH);
  }

  /**
   * Looks under the answer dir -- should it?
   * @param audioFile
   * @param language
   * @return
   * @see #dealWithMP3Audio(String, String)
   * @see mitll.langtest.server.LangTestDatabaseImpl#getImageForAudioFile
   */
//  String getWavForMP3(String audioFile, String language) {
// //   String installPath = ;//pathHelper.getInstallPath();
//    return getWavForMP3(audioFile, language);
//  }

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
  String getWavForMP3(String audioFile, String language) {
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
  }
}
