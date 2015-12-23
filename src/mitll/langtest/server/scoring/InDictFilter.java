package mitll.langtest.server.scoring;

import mitll.langtest.server.audio.AudioFileHelper;

import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;

/**
 * Created by go22670 on 12/23/15.
 */
public class InDictFilter {
  private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(InDictFilter.class);

  private final AudioFileHelper audioFileHelper;

  public InDictFilter(AudioFileHelper audioFileHelper) {
    this.audioFileHelper = audioFileHelper;
  }

  /**
   * @param phrases
   * @return
   * @see mitll.langtest.server.audio.AudioFileHelper#getValidPhrases(java.util.Collection)
   */
  public Collection<String> getValidPhrases(Collection<String> phrases) {
    return getValidSentences(phrases);
  }

  /**
   * @param phrase
   * @return
   * @see #isValid(String)
   */
  private boolean isPhraseInDict(String phrase) {
    return audioFileHelper.checkLTS(phrase);
  }

  /**
   * @param sentences
   * @return lc of sentence...?
   * @see #getValidPhrases(java.util.Collection)
   */
  private Collection<String> getValidSentences(Collection<String> sentences) {
    // logger.info("getValidSentences checking " + sentences.size() + " sentences");
    Set<String> filtered = new TreeSet<String>();
    Set<String> skipped = new TreeSet<String>();

    for (String sentence : sentences) {
      boolean valid = allValid(sentence);
      if (!valid) {
        sentence = audioFileHelper.getSmallVocabDecoder().toFull(sentence);
        valid = allValid(sentence);
      }
      if (valid) {
        filtered.add(sentence.toLowerCase());
      } else {
        skipped.add(sentence);
        logger.warn("getValidSentences : skipping '" + sentence + "' which is not in dictionary.");
      }
    }

    if (!skipped.isEmpty()) {
      logger.warn("getValidSentences : skipped " + skipped.size() + " of " + sentences.size() +
          " sentences, skipped : " + skipped);
    } else {
      //  logger.debug("getValidSentences : found " + filtered.size() + " from sentences : " + sentences.size());
    }

    return filtered;
  }

  private boolean allValid(String sentence) {
    Collection<String> tokens = audioFileHelper.getSmallVocabDecoder().getTokens(sentence);
    if (tokens.isEmpty() && !sentence.isEmpty()) logger.error("huh? no tokens from " + sentence);

    return allTokensValid(tokens);
  }

  private boolean allTokensValid(Collection<String> tokens) {
    boolean valid = true;
    for (String token : tokens) {
      if (!isValid(token)) {
        logger.warn("\tgetValidSentences : token '" + token + "' is not in dictionary.");
        valid = false;
      } else {
//        logger.info("token '" + token +  "' is in dict");
      }
    }
    return valid;
  }

  /**
   * @param token
   * @return
   * @see #getValidSentences(java.util.Collection)
   */
  private boolean isValid(String token) {
    return /*checkToken(token) &&*/ isPhraseInDict(token);
  }
}
