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

package mitll.langtest.server.autocrt;

import mitll.langtest.server.audio.AudioFileHelper;
import mitll.langtest.server.audio.DecoderOptions;
import mitll.langtest.server.audio.SLFFile;
import mitll.langtest.server.scoring.AlignDecode;
import mitll.langtest.server.scoring.Scoring;
import mitll.langtest.server.scoring.SmallVocabDecoder;
import mitll.langtest.shared.answer.AudioAnswer;
import mitll.langtest.shared.exercise.CommonShell;
import mitll.langtest.shared.scoring.PretestScore;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.*;

/**
 * Normalize input and compare decoder output against possible results to determine whether
 * the text was correctly decoded or not.
 * <p>
 * For instance, the decoder could return only a subset of the expected tokens
 * and so the audio would be marked as incorrect.
 * <p/>
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 1/10/13
 * Time: 11:37 AM
 * To change this template use File | Settings | File Templates.
 */
public class DecodeCorrectnessChecker {
  private static final Logger logger = LogManager.getLogger(DecodeCorrectnessChecker.class);
  private final AlignDecode alignDecode;
  private final double minPronScore;
  private final SmallVocabDecoder svd = new SmallVocabDecoder();
  private static final boolean DEBUG = false;

  /**
   * @param alignDecode
   * @param minPronScore
   * @see AudioFileHelper#makeDecodeCorrectnessChecker()
   */
  public DecodeCorrectnessChecker(AlignDecode alignDecode, double minPronScore) {
    this.alignDecode = alignDecode;
    this.minPronScore = minPronScore;
  }

  /**
   * Decode the phrase from the exercise in {@link mitll.langtest.shared.exercise.CommonExercise#getForeignLanguage}
   *
   * @param commonExercise
   * @param audioFile
   * @param answer
   * @see mitll.langtest.server.LangTestDatabaseImpl#writeAudioFile
   * @see mitll.langtest.server.audio.AudioFileHelper#getAudioAnswer
   */
  public PretestScore getDecodeScore(CommonShell commonExercise,
                                     File audioFile,
                                     AudioAnswer answer,
                                     String language,
                                     DecoderOptions decoderOptions
  ) {
    Collection<String> foregroundSentences = getRefSentences(commonExercise, language, decoderOptions.isAllowAlternates());
    PretestScore decodeScore = getDecodeScore(audioFile, foregroundSentences, answer, decoderOptions);
    // log what happened
    logDecodeOutput(answer, foregroundSentences, commonExercise.getID());

    return decodeScore;
  }

  private void logDecodeOutput(AudioAnswer answer, Collection<String> foregroundSentences, int id) {
    String decodeOutput = answer.getDecodeOutput();
    double score = answer.getScore();

    if (answer.isCorrect()) {
//      logger.info("correct response for exercise #" + id +
//          " reco sentence was '" + decodeOutput + "' vs " + "'" + foregroundSentences + "' " +
//          "pron score was " + score + " answer " + answer);
    } else {
      int length = foregroundSentences.isEmpty() ? 0 : foregroundSentences.iterator().next().length();
      logger.info("getDecodeScore : incorrect response for exercise #" + id +
          " reco sentence was '" + decodeOutput + "' (" + decodeOutput.length() +
          ") vs " + "'" + foregroundSentences + "' (" + length +
          ") pron score was " + score);
    }
  }

  /**
   * So we need to process the possible decode sentences so that hydec can handle them.
   * <p/>
   * E.g. english is in UPPER CASE.
   * <p/>
   * Decode result is correct if all the tokens match (ignore case) any of the possibleSentences AND the score is
   * above the {@link #minPronScore} min score, typically in the 30s.
   * <p/>
   * If you want to see what the decoder output was, that's in {@link AudioAnswer#getDecodeOutput()}.
   * For instance if you wanted to show that for debugging purposes.
   * If you want to know whether the said the right word or not (which might have scored too low to be correct)
   * see {@link AudioAnswer#isSaidAnswer()}.
   *
   * @param audioFile         to score against
   * @param possibleSentences any of these can match and we'd call this a correct response
   * @param answer            holds the score, whether it was correct, the decode output, and whether one of the
   *                          possible sentences
   * @return PretestScore word/phone alignment with scores
   * @see #getDecodeScore
   */
  private PretestScore getDecodeScore(File audioFile, Collection<String> possibleSentences, AudioAnswer answer,
                                      DecoderOptions decoderOptions

  ) {
    List<String> lmSentences = removePunct(possibleSentences);
//    logger.debug("getDecodeScore " + possibleSentences + " : '" + lmSentences + "'");
    //making the transliteration empty as I don't think it is useful here
    PretestScore asrScoreForAudio = alignDecode.getASRScoreForAudio(audioFile, lmSentences, "", decoderOptions);

    String recoSentence =
        asrScoreForAudio != null && asrScoreForAudio.getRecoSentence() != null ?
            asrScoreForAudio.getRecoSentence().toLowerCase().trim() : "";
    // logger.debug("recoSentence is " + recoSentence + " (" +recoSentence.length()+ ")");

    boolean isCorrect = isCorrect(possibleSentences, recoSentence);
    double scoreForAnswer = (asrScoreForAudio == null || asrScoreForAudio.getHydecScore() == -1) ? -1 : asrScoreForAudio.getHydecScore();
    answer.setCorrect(isCorrect && scoreForAnswer > minPronScore);
    answer.setSaidAnswer(isCorrect);
    answer.setDecodeOutput(recoSentence);
    answer.setScore(scoreForAnswer);
    return asrScoreForAudio;
  }

  /**
   * Convert dashes into spaces and remove periods, and other punct
   *
   * @param answerSentences
   * @param recoSentence
   * @return
   */
  private boolean isCorrect(Collection<String> answerSentences, String recoSentence) {
    if (DEBUG) logger.debug("isCorrect - expected '" + answerSentences + "' vs heard '" + recoSentence + "'");

    List<String> recoTokens = svd.getTokens(recoSentence);
    for (String answer : answerSentences) {
      String converted = answer.replaceAll("-", " ").replaceAll("\\.\\.\\.", " ").replaceAll("\\.", "").replaceAll(":", "").toLowerCase();
      if (DEBUG) logger.debug("isCorrect - converted '" + converted + "' vs '" + answer + "'");

      List<String> answerTokens = svd.getTokens(converted);
      if (answerTokens.size() == recoTokens.size()) {
        boolean same = true;
        for (int i = 0; i < answerTokens.size() && same; i++) {
          String s = answerTokens.get(i);
          String anotherString = recoTokens.get(i);
          if (DEBUG)
            logger.debug("comparing '" + s + "' " + s.length() + " to '" + anotherString + "' " + anotherString.length());
          same = s.equalsIgnoreCase(anotherString);
          if (!same) {
            if (DEBUG)
              logger.debug("comparing '" + s + "' " + s.length() + " to '" + anotherString + "' " + anotherString.length());
          }
        }
        if (same) return true;
      } else {
        if (DEBUG) logger.debug("not same number of tokens " + answerTokens + " " +
            answerTokens.size() + " vs " + recoTokens + " " + recoTokens.size());
      }
    }
    return false;
  }

  /**
   * @param toDecode        exercise to get text from
   * @param language        mandarin and possibly toDecode languages require special segmentation
   * @param allowAlternates true if we want to decode against multiple possible paths
   * @return possible paths for the decoder
   * @see #getDecodeScore
   */
  private Collection<String> getRefSentences(CommonShell toDecode, String language, boolean allowAlternates) {
    if (allowAlternates) {
      Set<String> ret = new HashSet<>();
      for (String alt : toDecode.getRefSentences()) ret.add(getPhraseToDecode(alt, language));
      return ret;
    } else {
      String phraseToDecode = getPhraseToDecode(toDecode.getForeignLanguage(), language);
/*      logger.debug("(" + language +
          ") for " + other.getOldID() + " fl is '" + other.getForeignLanguage() + "' / '" +other.getForeignLanguage().trim()+
          "' -> '" + phraseToDecode +          "'");*/
      return Collections.singleton(phraseToDecode);
    }
  }

  /**
   * Special rule for mandarin - break it up into characters
   *
   * @param rawRefSentence
   * @param language
   * @return
   */
  private String getPhraseToDecode(String rawRefSentence, String language) {
    return language.equalsIgnoreCase("mandarin") && !rawRefSentence.trim().equalsIgnoreCase(SLFFile.UNKNOWN_MODEL) ?
        Scoring.getSegmented(rawRefSentence.trim().toUpperCase()) :
        rawRefSentence.trim().toUpperCase();
  }

  /**
   * @see #getFlashcardAnswer(File, Collection, AudioAnswer, boolean, boolean)
   * @param possibleSentences
   * @return
   * @see #getDecodeScore
   */
  private List<String> removePunct(Collection<String> possibleSentences) {
    List<String> foreground = new ArrayList<String>();
    for (String ref : possibleSentences) {
      foreground.add(removePunct(ref));
    }
    return foreground;
  }

  /**
   * Replace elipsis with space. Then remove all punct.
   * Replace commas with spaces.
   * <p>
   * Deal with forward slashes like in english.
   *
   * @param t
   * @return
   */
  private String removePunct(String t) {
    return t.replaceAll("\\.\\.\\.", " ").replaceAll("/", " ").replaceAll(",", " ").replaceAll("\\p{P}", "");
  }
}
