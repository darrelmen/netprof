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
import mitll.langtest.server.scoring.ASR;
import mitll.langtest.server.scoring.AlignDecode;
import mitll.langtest.server.scoring.PrecalcScores;
import mitll.langtest.server.scoring.SmallVocabDecoder;
import mitll.langtest.shared.answer.AudioAnswer;
import mitll.langtest.shared.exercise.ClientExercise;
import mitll.langtest.shared.scoring.DecoderOptions;
import mitll.langtest.shared.scoring.PretestScore;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

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

  public static final String UNKNOWN_MODEL = ASR.UNKNOWN_MODEL;
  public static final String MANDARIN = "mandarin";
  public static final String FRENCH = "french";

  private final AlignDecode alignDecode;
  private final double minPronScore;
  private final SmallVocabDecoder svd;
  private static final boolean DEBUG = false;

  /**
   * @param alignDecode
   * @param minPronScore
   * @param smallVocabDecoder
   * @see AudioFileHelper#makeDecodeCorrectnessChecker
   */
  public DecodeCorrectnessChecker(AlignDecode alignDecode, double minPronScore, SmallVocabDecoder smallVocabDecoder) {
    this.alignDecode = alignDecode;
    this.minPronScore = minPronScore;
    this.svd = smallVocabDecoder;
  }

  /**
   * Decode the phrase from the exercise in {@link mitll.langtest.shared.exercise.CommonExercise#getForeignLanguage}
   *
   * @param commonExercise
   * @param audioFile
   * @param answer
   * @param precalcScores
   * @see mitll.langtest.server.services.AudioServiceImpl#writeAudioFile
   * @see mitll.langtest.server.audio.AudioFileHelper#getAudioAnswer
   */
  public PretestScore getDecodeScore(ClientExercise commonExercise,
                                     File audioFile,
                                     AudioAnswer answer,
                                     String language,
                                     DecoderOptions decoderOptions,
                                     PrecalcScores precalcScores) {
    Collection<String> foregroundSentences = getRefSentences(commonExercise, language, decoderOptions.isAllowAlternates());

    boolean b = isAsianLanguage(language);
//    logger.info("is asian lang (" + language + ")" + b);

    boolean b1 = language.equalsIgnoreCase(FRENCH);
    boolean removeAllPunct = !b1;
    logger.info("getDecodeScore : " + language + " : is french " + b1 + " remove all punct " + removeAllPunct);
    PretestScore decodeScore = getDecodeScore(audioFile, foregroundSentences, answer, decoderOptions, precalcScores, b, removeAllPunct);
    // log what happened
    logDecodeOutput(answer, foregroundSentences, commonExercise.getID());

    return decodeScore;
  }

  private boolean isAsianLanguage(String language) {
    return language.equalsIgnoreCase(MANDARIN) ||
        language.equalsIgnoreCase("japanese") ||
        language.equalsIgnoreCase("korean");
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
   * @param precalcScores
   * @param isMandarinEtAl
   * @param removeAllPunct
   * @return PretestScore word/phone alignment with scores
   * @see #getDecodeScore
   */
  private PretestScore getDecodeScore(File audioFile,
                                      Collection<String> possibleSentences,
                                      AudioAnswer answer,
                                      DecoderOptions decoderOptions,

                                      PrecalcScores precalcScores,
                                      boolean isMandarinEtAl,
                                      boolean removeAllPunct) {
    List<String> lmSentences = removePunct(possibleSentences, removeAllPunct);
//    logger.debug("getDecodeScore " + possibleSentences + " : '" + lmSentences + "'");
    //making the transliteration empty as I don't think it is useful here
    PretestScore asrScoreForAudio = alignDecode.getASRScoreForAudio(answer.getReqid(), audioFile, lmSentences,
        "", decoderOptions, precalcScores);

    String recoSentence =
        asrScoreForAudio != null && asrScoreForAudio.getRecoSentence() != null ?
            asrScoreForAudio.getRecoSentence().toLowerCase().trim() : "";

    //    logger.debug("recoSentence is " + recoSentence + " (" + recoSentence.length() + ")");

    boolean isCorrect = isCorrect(possibleSentences, recoSentence, isMandarinEtAl, removeAllPunct);

    if (!isCorrect) {
      logger.debug("recoSentence (not correct) is '" + recoSentence + "' length = (" + recoSentence.length() + ")");
    }

    double scoreForAnswer = (asrScoreForAudio == null || asrScoreForAudio.getHydecScore() == -1) ? -1 : asrScoreForAudio.getHydecScore();
    answer.setCorrect(isCorrect && scoreForAnswer > minPronScore);
    answer.setSaidAnswer(isCorrect);
    answer.setDecodeOutput(recoSentence);
    answer.setScore(scoreForAnswer);
    return asrScoreForAudio;
  }

  private List<String> removePunct(Collection<String> possibleSentences, boolean removeAllPunct) {
    return removeAllPunct ? removePunct(possibleSentences) : removePunctFrench(possibleSentences);
  }

  /**
   * Convert dashes into spaces and remove periods, and other punct
   *
   * @param expectedAnswers
   * @param recoSentence
   * @param isMandarinEtAl
   * @param removeAllAccents
   * @return
   */
  private boolean isCorrect(Collection<String> expectedAnswers, String recoSentence, boolean isMandarinEtAl, boolean removeAllAccents) {
    if (DEBUG) {
      logger.debug("isCorrect - expected  '" + expectedAnswers + "' vs heard '" + recoSentence + "', is asian lang = " + isMandarinEtAl);
    }

    for (String answer : expectedAnswers) {
      String converted = getPunctRemoved(answer);

      if (DEBUG && !converted.equalsIgnoreCase(answer)) {
        logger.debug("isCorrect - converted '" + converted + "' vs '" + answer + "'");
      }

      List<String> answerTokens = svd.getTokensAllLanguages(isMandarinEtAl, converted, removeAllAccents);
      List<String> recoTokens = svd.getTokensAllLanguages(isMandarinEtAl, recoSentence, removeAllAccents);
      if (answerTokens.size() == recoTokens.size()) {
        boolean same = true;
        for (int i = 0; i < answerTokens.size() && same; i++) {
          String expected = answerTokens.get(i);
          String reco = recoTokens.get(i);
          if (DEBUG)
            logger.debug("isCorrect comparing '" + expected + "' " + expected.length() + " to '" + reco + "' " + reco.length());
          same = expected.equalsIgnoreCase(reco);
          if (!same) {
            if (DEBUG || true)
              logger.debug("isCorrect NO MATCH " +
                  "\n\tcomparing '" + expected + "' " + expected.length() +
                  "\n\tto        '" + reco + "' " + reco.length());
          }
        }
        if (same) return true;
      } else {
        if (DEBUG) logger.debug("isCorrect not same number of tokens " + answerTokens + " " +
            answerTokens.size() + " vs " + recoTokens + " " + recoTokens.size());
      }
    }
    return false;
  }

  @NotNull
  private String getPunctRemoved(String answer) {
    return answer
        .replaceAll("-", " ")
        .replaceAll("\\.\\.\\.", " ")
        .replaceAll("\\.", "")
        .replaceAll(":", "")
        .replaceAll("\u3002\u3066", " ") // japanese comma, japanese period
        .toLowerCase();
  }

  /**
   * @param toDecode        exercise to get text from
   * @param language        mandarin and possibly toDecode languages require special segmentation
   * @param allowAlternates true if we want to decode against multiple possible paths
   * @return possible paths for the decoder
   * @see #getDecodeScore
   */
  private Collection<String> getRefSentences(ClientExercise toDecode, String language, boolean allowAlternates) {
    if (allowAlternates) {
      Set<String> ret = new HashSet<>();
      toDecode.asCommon().getRefSentences().forEach(alt -> ret.add(getPhraseToDecode(alt, language)));
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
   * <p>
   * Added hack for spanish to replace Ud. with usted
   *
   * @param rawRefSentence
   * @param language
   * @return
   */
  public String getPhraseToDecode(String rawRefSentence, String language) {
    if (language.equalsIgnoreCase("spanish")) {
      //     logger.info("raw before " + rawRefSentence);
      rawRefSentence = rawRefSentence
          .replaceAll("Ud.", "usted")
          .replaceAll("Uds.", "ustedes");
      // logger.info("raw after   " + rawRefSentence);
    }

    // logger.info("raw (" +language+  ") after   " + rawRefSentence);

    return isAsianLanguage(language) && !rawRefSentence.trim().equalsIgnoreCase(UNKNOWN_MODEL) ?
        svd.getSegmented(rawRefSentence.trim().toUpperCase(), !isFrench(language)) :
        rawRefSentence.trim().toUpperCase();
  }

  private boolean isFrench(String language) {
    return language.equalsIgnoreCase(FRENCH);
  }

  /**
   * @param possibleSentences
   * @return
   * @see #getDecodeScore
   */
  private List<String> removePunct(Collection<String> possibleSentences) {
    List<String> foreground = new ArrayList<String>(possibleSentences.size());
    possibleSentences.forEach(ref -> foreground.add(removePunct(ref)));
    return foreground;
  }

  private List<String> removePunctFrench(Collection<String> possibleSentences) {
    List<String> foreground = new ArrayList<String>(possibleSentences.size());
    possibleSentences.forEach(ref -> foreground.add(removePunctFrench(ref)));
    return foreground;
  }

  /**
   * TODO : Do we want to do this for French???
   * <p>
   * Replace elipsis with space. Then remove all punct.
   * Replace commas with spaces.
   * <p>
   * Deal with forward slashes like in english.
   *
   * @param t
   * @return
   */
  private String removePunct(String t) {
    return t
        .replaceAll("\\.\\.\\.", " ")
        .replaceAll("/", " ")
        .replaceAll(",", " ")
        .replaceAll("\\p{P}", "");
  }

  private String removePunctFrench(String t) {
    return t
        .replaceAll("\\.\\.\\.", " ")
        .replaceAll("/", " ")
        .replaceAll(",", " ")
        .replaceAll("[.?]", " ");
  }
}
