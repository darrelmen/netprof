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

package mitll.langtest.server.scoring;

import mitll.langtest.shared.scoring.ImageOptions;
import mitll.langtest.shared.scoring.PretestScore;

import java.text.Collator;
import java.util.Collection;
import java.util.List;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 */
public interface ASR {
  Collator getCollator();

  boolean isDictEmpty();

  boolean validLTS(String foreignLanguagePhrase, String transliteration);

  PhoneInfo getBagOfPhones(String foreignLanguagePhrase);

  SmallVocabDecoder getSmallVocabDecoder();

  String createHydraDict(String transcript, String transliteration);

  String getUsedTokens(Collection<String> lmSentences, List<String> background);

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
                           boolean usePhoneToDisplay);

  boolean isAvailable();

}
