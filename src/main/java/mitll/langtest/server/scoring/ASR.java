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

package mitll.langtest.server.scoring;

import mitll.langtest.server.audio.AudioFileHelper;
import mitll.langtest.server.database.audio.IAudioDAO;
import mitll.langtest.server.database.exercise.ISection;
import mitll.langtest.server.database.exercise.Project;
import mitll.langtest.shared.scoring.ImageOptions;
import mitll.langtest.shared.scoring.PretestScore;

import java.text.Collator;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface ASR {
  String UNKNOWN_MODEL = "UNKNOWNMODEL";

  Collator getCollator();

  boolean isDictEmpty();

  boolean validLTS(String foreignLanguagePhrase, String transliteration);

  Collection<String> getKaldiOOV(String fl);

  /**
   * @param fl
   * @param transliteration
   * @return
   * @see AudioFileHelper#checkLTSOnForeignPhrase(String, String)
   */
//  Collection<String> getOOV(String fl, String transliteration);

//  PhoneInfo getBagOfPhones(String foreignLanguagePhrase);

  CheckLTS getCheckLTSHelper();

  SmallVocabDecoder getSmallVocabDecoder();

  boolean isAvailableCheckNow();


  /**
   * @param testAudioDir
   * @param testAudioFileNoSuffix
   * @param sentence
   * @param lmSentences
   * @param imageOutDir
   * @param decode
   * @param useCache
   * @param prefix
   * @param precalcScores
   * @param usePhoneToDisplay
   * @param kaldi
   * @return
   * @see AlignDecode#getASRScoreForAudio
   */
  PretestScore scoreRepeat(String testAudioDir,
                           String testAudioFileNoSuffix,
                           String sentence,
                           Collection<String> lmSentences,
                           String transliteration,
                           String imageOutDir,
                           ImageOptions imageOptions,
                           boolean decode,
                           boolean useCache,
                           String prefix,
                           PrecalcScores precalcScores,
                           boolean usePhoneToDisplay, boolean kaldi);

  void setAvailable();

  /**
   * @return
   * @see AudioFileHelper#isHydraAvailable
   */
  boolean isAvailable();

  /**
   * JUST FOR TESTING
   *
   * @param audioPath
   * @param transcript
   * @param transliteration
   * @param lmSentences
   * @param tmpDir
   * @param decode
   * @param end
   * @return
   */
  HydraOutput runHydra(String audioPath,
                       String transcript,
                       String transliteration,
                       Collection<String> lmSentences,
                       String tmpDir,
                       boolean decode,
                       int end);

  /**
   * @param cleaned
   * @param transliteration
   * @param possibleProns
   * @return
   * @see AudioFileHelper#getHydraDict
   */
  TransNormDict getHydraDict(String cleaned, String transliteration, List<WordAndProns> possibleProns);

  /**
   * @param transcript
   * @param transliteration
   * @return
   * @see mitll.langtest.server.database.userexercise.SlickUserExerciseDAO#getExercises(Collection, List, ISection, Project, Map, Map, boolean)
   */
  List<String> getTokens(String transcript, String transliteration);

  /**
   * @param transcript
   * @param transliteration
   * @return
   * @see mitll.langtest.server.database.audio.SlickTrainingAudioDAO#checkAndAddAudio(Collection, IAudioDAO)
   */
  String getNormTranscript(String transcript, String transliteration);

  /**
   * @param input
   * @return
   * @see AudioFileHelper#getSegmented(String)
   */
  String getSegmented(String input);

  IPronunciationLookup getPronunciationLookup();
}