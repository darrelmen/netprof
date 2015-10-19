package mitll.langtest.server.autocrt;

import mitll.langtest.server.audio.SLFFile;
import mitll.langtest.server.scoring.AutoCRTScoring;
import mitll.langtest.server.scoring.Scoring;
import mitll.langtest.server.scoring.SmallVocabDecoder;
import mitll.langtest.shared.AudioAnswer;
import mitll.langtest.shared.CommonExercise;
import mitll.langtest.shared.scoring.PretestScore;
import org.apache.log4j.Logger;

import java.io.File;
import java.util.*;

/**
 * AutoCRT support -- basically wrapping Jacob's work that lives in mira.jar <br></br>
 * Does some work to make a lm and lattice file suitable for doing small vocabulary decoding.
 * <p/>
 * User: GO22670
 * Date: 1/10/13
 * Time: 11:37 AM
 * To change this template use File | Settings | File Templates.
 */
public class AutoCRT {
  private static final Logger logger = Logger.getLogger(AutoCRT.class);
  private final AutoCRTScoring autoCRTScoring;
  private final double minPronScore;
  private final SmallVocabDecoder svd = new SmallVocabDecoder();
  private static final boolean DEBUG = false;

  /**
   * @param autoCRTScoring
   * @param minPronScore
   * @see mitll.langtest.server.audio.AudioFileHelper#makeAutoCRT
   */
  public AutoCRT(AutoCRTScoring autoCRTScoring, double minPronScore) {
    this.autoCRTScoring = autoCRTScoring;
    this.minPronScore = minPronScore;
  }

  /**
   * Decode the phrase from the exercise in {@link mitll.langtest.shared.CommonExercise#getForeignLanguage}
   *
   * @param commonExercise
   * @param audioFile
   * @param answer
   * @param canUseCache
   * @param allowAlternates
   * @param useOldSchool
   * @see mitll.langtest.server.LangTestDatabaseImpl#writeAudioFile
   * @see mitll.langtest.server.audio.AudioFileHelper#getAudioAnswer
   */
  public PretestScore getFlashcardAnswer(CommonExercise commonExercise, File audioFile, AudioAnswer answer,
                                         String language, boolean canUseCache, boolean allowAlternates, boolean useOldSchool) {
    Collection<String> foregroundSentences = getRefSentences(commonExercise, language, allowAlternates);
    PretestScore flashcardAnswer = getFlashcardAnswer(audioFile, foregroundSentences, answer, canUseCache, useOldSchool);

    // log what happened
    if (answer.isCorrect()) {
      logger.info("correct response for exercise #" + commonExercise.getID() +
          " reco sentence was '" + answer.getDecodeOutput() + "' vs " + "'" + foregroundSentences + "' " +
          "pron score was " + answer.getScore() + " answer " + answer);
    } else {
      int length = foregroundSentences.isEmpty() ? 0 : foregroundSentences.iterator().next().length();
      logger.info("getFlashcardAnswer : incorrect response for exercise #" + commonExercise.getID() +
          " reco sentence was '" + answer.getDecodeOutput() + "' (" + answer.getDecodeOutput().length() +
          ") vs " + "'" + foregroundSentences + "' (" + length +
          ") pron score was " + answer.getScore());
    }
    return flashcardAnswer;
  }

  /**
   * So we need to process the possible decode sentences so that hydec can handle them.
   * <p/>
   * E.g. english is in UPPER CASE.
   * <p/>
   * Decode result is correct if all the tokens match (ignore case) any of the possibleSentences AND the score is
   * above the {@link #minPronScore} min score, typically in the 30s.
   * <p/>
   * If you want to see what the decoder output was, that's in {@link mitll.langtest.shared.AudioAnswer#getDecodeOutput()}.
   * For instance if you wanted to show that for debugging purposes.
   * If you want to know whether the said the right word or not (which might have scored too low to be correct)
   * see {@link mitll.langtest.shared.AudioAnswer#isSaidAnswer()}.
   *
   * @param audioFile         to score against
   * @param possibleSentences any of these can match and we'd call this a correct response
   * @param answer            holds the score, whether it was correct, the decode output, and whether one of the
   *                          possible sentences
   * @param canUseCache
   * @param useOldSchool
   * @return PretestScore word/phone alignment with scores
   * @paramx firstPronLength
   * @see #getFlashcardAnswer
   */
  private PretestScore getFlashcardAnswer(File audioFile, Collection<String> possibleSentences, AudioAnswer answer,
                                          boolean canUseCache, boolean useOldSchool) {
    List<String> lmSentences = removePunct(possibleSentences);
//    logger.debug("getFlashcardAnswer " + possibleSentences + " : '" + lmSentences + "'");
    PretestScore asrScoreForAudio = autoCRTScoring.getASRScoreForAudio(audioFile, lmSentences, canUseCache, useOldSchool);

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
    if (DEBUG) logger.debug("isCorrect - expected '" + answerSentences + "' vs heard '" + recoSentence +"'");

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
          if (DEBUG)  logger.debug("comparing '" + s + "' " +s.length()+ " to '" + anotherString  +"' "  +anotherString.length());
          same = s.equalsIgnoreCase(anotherString);
          if (!same) {
            if (DEBUG) logger.debug("comparing '" + s + "' " + s.length() + " to '" + anotherString + "' " + anotherString.length());
          }
        }
        if (same) return true;
      } else {
        if (DEBUG) logger.debug("not same number of tokens " + answerTokens + " " + answerTokens.size() + " vs " + recoTokens + " " + recoTokens.size());
      }
    }
    return false;
  }

  /**
   * @param other
   * @param allowAlternates
   * @return
   * @see #getFlashcardAnswer
   */
  private Collection<String> getRefSentences(CommonExercise other, String language, boolean allowAlternates) {
    if (allowAlternates) {
      Set<String> ret = new HashSet<>();
      for (String alt : other.getRefSentences()) ret.add(getPhraseToDecode(alt, language));
      return ret;
    } else {
      String phraseToDecode = getPhraseToDecode(other.getForeignLanguage(), language);
/*      logger.debug("(" + language +
          ") for " + other.getID() + " fl is '" + other.getForeignLanguage() + "' / '" +other.getForeignLanguage().trim()+
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
   *
   *  Deal with forward slashes like in english.
   * @param t
   * @return
   */
  private String removePunct(String t) {
    return t.replaceAll("\\.\\.\\.", " ").replaceAll("/", " ").replaceAll("\\p{P}", "");
  }
}
