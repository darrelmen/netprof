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

package mitll.langtest.client.custom.dialog;

import com.google.gwt.user.client.ui.MultiWordSuggestOracle;

import java.util.Collection;
import java.util.logging.Logger;

/**
 * Created by go22670 on 4/7/17.
 */
public class WordBoundsFactory {
  private final Logger logger = Logger.getLogger("WordBoundsFactory");

  /**
   * Returns a {@link MultiWordSuggestOracle.WordBounds} representing the first word in {@code
   * searchWords} that is found in candidate starting at {@code indexToStartAt}
   * or {@code null} if no words could be found.
   */
  public WordBounds findNextWord(String candidate, String[] searchWords, int indexToStartAt) {
    WordBounds firstWord = null;
    for (String word : searchWords) {
      if (word.isEmpty()) {
        //logger.warning("findNextWord got empty search term");
      }
      else {
        firstWord = getWordBounds(candidate, indexToStartAt, firstWord, word);
      }
    }
    return firstWord;

  }

  public WordBounds findNextWordList(String candidate, Collection<String> searchWords, int indexToStartAt) {
    WordBounds firstWord = null;
    for (String word : searchWords) {
      if (word.isEmpty()) {
        //logger.warning("findNextWord got empty search term");
      }
      else {
        firstWord = getWordBounds(candidate, indexToStartAt, firstWord, word);
      }
    }
    return firstWord;
  }

  private WordBounds getWordBounds(String candidate, int indexToStartAt, WordBounds firstWord, String word) {
    int index = candidate.indexOf(word, indexToStartAt);
    if (index != -1) {
      WordBounds newWord = new WordBounds(index, word.length());
      if (firstWord == null || newWord.compareTo(firstWord) < 0) {
        firstWord = newWord;
      }
    }
    return firstWord;
  }
}
