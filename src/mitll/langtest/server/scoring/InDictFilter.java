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

import mitll.langtest.server.audio.AudioFileHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 12/23/15.
 */
public class InDictFilter {
  private static final Logger logger = LogManager.getLogger(InDictFilter.class);

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
    return audioFileHelper.checkLTSOnForeignPhrase(phrase, "");
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
