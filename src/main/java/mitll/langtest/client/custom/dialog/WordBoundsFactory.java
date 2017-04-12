package mitll.langtest.client.custom.dialog;

import com.google.gwt.user.client.ui.MultiWordSuggestOracle;

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
        logger.warning("findNextWord got empty search term");
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
