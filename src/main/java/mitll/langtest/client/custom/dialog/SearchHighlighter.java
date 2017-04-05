package mitll.langtest.client.custom.dialog;

import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.client.ui.MultiWordSuggestOracle;

/**
 * Created by go22670 on 4/5/17.
 */
public class SearchHighlighter {
  private static final String STRONG = "strong";

  public String getHighlightedString(String[] searchWords, String suggestion, String lowerCaseSuggestion) {
    // Create strong search string.
    SafeHtmlBuilder accum = new SafeHtmlBuilder();

    int cursor = 0;
    int index = 0;
    while (true) {
      WordBounds wordBounds = findNextWord(lowerCaseSuggestion, searchWords, index);
      if (wordBounds == null) {
        break;
      }
      if (wordBounds.startIndex == 0 ||
          WHITESPACE_CHAR == lowerCaseSuggestion.charAt(wordBounds.startIndex - 1)) {
        String part1 = suggestion.substring(cursor, wordBounds.startIndex);
        String part2 = suggestion.substring(wordBounds.startIndex,
            wordBounds.endIndex);
        cursor = wordBounds.endIndex;
        accum.appendEscaped(part1);
        accum.appendHtmlConstant("<" + STRONG + ">");
        accum.appendEscaped(part2);
        accum.appendHtmlConstant("</" + STRONG + ">");
      }
      index = wordBounds.endIndex;
    }

//    // Check to make sure the search was found in the string.
//    if (cursor == 0) {
//      continue;
//    }

    accum.appendEscaped(suggestion.substring(cursor));

    // logger.info(resp.getID() + " formatted     " + suggestion);
    return accum.toSafeHtml().asString();
  }



  /**
   * A class reresenting the bounds of a word within a string.
   * <p>
   * The bounds are represented by a {@code startIndex} (inclusive) and
   * an {@code endIndex} (exclusive).
   */
  private static class WordBounds implements Comparable<WordBounds> {

    final int startIndex;
    final int endIndex;

    public WordBounds(int startIndex, int length) {
      this.startIndex = startIndex;
      this.endIndex = startIndex + length;
    }

    public int compareTo(WordBounds that) {
      int comparison = this.startIndex - that.startIndex;
      if (comparison == 0) {
        comparison = that.endIndex - this.endIndex;
      }
      return comparison;
    }
  }


  /**
   * Returns a {@link MultiWordSuggestOracle.WordBounds} representing the first word in {@code
   * searchWords} that is found in candidate starting at {@code indexToStartAt}
   * or {@code null} if no words could be found.
   */
  private WordBounds findNextWord(String candidate, String[] searchWords, int indexToStartAt) {
    WordBounds firstWord = null;
    for (String word : searchWords) {
      int index = candidate.indexOf(word, indexToStartAt);
      if (index != -1) {
        WordBounds newWord = new WordBounds(index, word.length());
        if (firstWord == null || newWord.compareTo(firstWord) < 0) {
          firstWord = newWord;
        }
      }
    }
    return firstWord;
  }


  private static final char WHITESPACE_CHAR = ' ';
  private static final String WHITESPACE_STRING = " ";
}
