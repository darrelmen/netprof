package mitll.langtest.server.autocrt;

import mitll.langtest.server.audio.SLFFile;
import mitll.langtest.server.scoring.AutoCRTScoring;
import mitll.langtest.server.scoring.SmallVocabDecoder;
import mitll.langtest.shared.AudioAnswer;
import mitll.langtest.shared.CommonExercise;
import mitll.langtest.shared.scoring.PretestScore;
import mitll.langtest.server.scoring.ASRScoring;
import org.apache.log4j.Logger;

import java.io.File;
import java.util.*;

/**
 * AutoCRT support -- basically wrapping Jacob's work that lives in mira.jar <br></br>
 * Does some work to make a lm and lattice file suitable for doing small vocabulary decoding.
 *
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

  /**
   * @see mitll.langtest.server.audio.AudioFileHelper#makeAutoCRT
   * @param db
   * @param minPronScore
   */
  public AutoCRT(AutoCRTScoring db, double minPronScore) {
    this.autoCRTScoring = db;
    this.minPronScore = minPronScore;
  }

  /**
   * Decode the phrase from the exercise in {@link mitll.langtest.shared.CommonExercise#getForeignLanguage}
   *
   * @see mitll.langtest.server.LangTestDatabaseImpl#writeAudioFile
   * @seex mitll.langtest.server.audio.AudioFileHelper#getAudioAnswer(String, int, int, int, java.io.File, mitll.langtest.server.audio.AudioCheck.ValidityAndDur, String, boolean, mitll.langtest.client.LangTestDatabase)
   * @seex mitll.langtest.server.audio.AudioFileHelper#getFlashcardAnswer(mitll.langtest.shared.Exercise, java.io.File, mitll.langtest.shared.AudioAnswer)
   * @param commonExercise
   * @param audioFile
   * @param answer
   */
  public PretestScore getFlashcardAnswer(CommonExercise commonExercise, File audioFile, AudioAnswer answer, String language) {
    Collection<String> foregroundSentences = getRefSentences(commonExercise, language);
    int firstPronLength = commonExercise.getFirstPron().size();
    PretestScore flashcardAnswer = getFlashcardAnswer(audioFile, foregroundSentences, answer, firstPronLength);

    // log what happened
    if (answer.isCorrect()) {
      logger.info("correct response for exercise #" +commonExercise.getID() +
          " reco sentence was '" + answer.getDecodeOutput() + "' vs " + "'"+foregroundSentences +"' " +
          "pron score was " + answer.getScore() + " answer " + answer);
    }
    else {
      int length = foregroundSentences.isEmpty() ? 0 : foregroundSentences.iterator().next().length();
      logger.info("getFlashcardAnswer : incorrect response for exercise #" +commonExercise.getID() +
          " reco sentence was '" + answer.getDecodeOutput() + "'(" +answer.getDecodeOutput().length()+
          ") vs " + "'"+foregroundSentences +"'(" + length +
          ") pron score was " + answer.getScore());
    }
    return flashcardAnswer;
  }

  /**
   * @see mitll.langtest.server.audio.AudioFileHelper#getFlashcardAnswer(java.io.File, String)
   *
   * @param audioFile
   * @param foregroundSentence
   * @param answer
   * @param firstPronLength
   * @return
   */
  public PretestScore getFlashcardAnswer(File audioFile, String foregroundSentence, AudioAnswer answer, String language,
                                         int firstPronLength) {

    Set<String> phraseToDecode = Collections.singleton(getPhraseToDecode(foregroundSentence, language));
    return getFlashcardAnswer(audioFile,
        phraseToDecode, answer, firstPronLength);
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
   *  For instance if you wanted to show that for debugging purposes.
   * If you want to know whether the said the right word or not (which might have scored too low to be correct)
   * see {@link mitll.langtest.shared.AudioAnswer#isSaidAnswer()}.
   *
   * @param audioFile         to score against
   * @param possibleSentences any of these can match and we'd call this a correct response
   * @param answer            holds the score, whether it was correct, the decode output, and whether one of the
   *                          possible sentences
   * @param firstPronLength
   * @return PretestScore word/phone alignment with scores
   */
  private PretestScore getFlashcardAnswer(File audioFile, Collection<String> possibleSentences, AudioAnswer answer,
                                          int firstPronLength) {
    PretestScore asrScoreForAudio = autoCRTScoring.getASRScoreForAudio(audioFile, removePunct(possibleSentences),
        firstPronLength);

    String recoSentence =
      asrScoreForAudio != null && asrScoreForAudio.getRecoSentence() != null ?
        asrScoreForAudio.getRecoSentence().toLowerCase().trim() : "";
    // logger.debug("recoSentence is " + recoSentence + "(" +recoSentence.length()+ ")");

    boolean isCorrect = isCorrect(possibleSentences, recoSentence);
    double scoreForAnswer = (asrScoreForAudio == null || asrScoreForAudio.getHydecScore() == -1) ?
        -1 : asrScoreForAudio.getHydecScore();
    answer.setCorrect(isCorrect && scoreForAnswer > minPronScore);
    answer.setSaidAnswer(isCorrect);
    answer.setDecodeOutput(recoSentence);
    answer.setScore(scoreForAnswer);
    return asrScoreForAudio;
  }

  /**
   * Convert dashes into spaces and remove periods, and other punct
   * @param answerSentences
   * @param recoSentence
   * @return
   */
  private boolean isCorrect(Collection<String> answerSentences, String recoSentence) {
   // logger.debug("iscorrect - answer " + answerSentences + " vs " + recoSentence);

    List<String> recoTokens = svd.getTokens(recoSentence);
    for (String answer : answerSentences) {
      String converted = answer.replaceAll("-", " ").replaceAll("\\.\\.\\.", " ").replaceAll("\\.", "").replaceAll(":", "").toLowerCase();
     // logger.debug("iscorrect - converted " + converted + " vs " + answer);

      List<String> answerTokens = svd.getTokens(converted);
      if (answerTokens.size() == recoTokens.size()) {
        boolean same = true;
        for (int i = 0; i < answerTokens.size() && same; i++) {
          String s = answerTokens.get(i);
          String anotherString = recoTokens.get(i);
      //    logger.debug("comparing '" + s + "' " +s.length()+ " to '" + anotherString  +"' "  +anotherString.length());
          same = s.equalsIgnoreCase(anotherString);
        }
        if (same) return true;
      }
     // else {
       // logger.debug("not same number of tokens " + answerTokens + " " + answerTokens.size() + " vs " + recoTokens + " " + recoTokens.size());
     // }
    }
    return false;
  }

  /**
   * @see #getFlashcardAnswer
   * @param other
   * @return
   */
  private Collection<String> getRefSentences(CommonExercise other, String language) {
    return Collections.singleton(getPhraseToDecode(other.getForeignLanguage(),language));
  }

  /**
   * Special rule for mandarin - break it up into characters
   * @param rawRefSentence
   * @param language
   * @return
   */
  private String getPhraseToDecode(String rawRefSentence, String language) {
    return language.equalsIgnoreCase("mandarin") && !rawRefSentence.trim().equalsIgnoreCase(SLFFile.UNKNOWN_MODEL) ?
        ASRScoring.getSegmented(rawRefSentence.trim().toUpperCase()) :
        rawRefSentence.trim().toUpperCase();
  }

  private List<String> removePunct(Collection<String> possibleSentences) {
    List<String> foreground = new ArrayList<String>();
    for (String ref : possibleSentences) {
      foreground.add(removePunct(ref));
    }
    return foreground;
  }

  /**
   * Replace elipsis with space. Then remove all punct.
   * @param t
   * @return
   */
  private String removePunct(String t) {
    return t.replaceAll("\\.\\.\\."," ").replaceAll("\\p{P}","");
  }
}
