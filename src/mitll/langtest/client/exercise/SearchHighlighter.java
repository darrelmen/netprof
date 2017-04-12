package mitll.langtest.client.exercise;

import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.client.ui.MultiWordSuggestOracle;
import mitll.langtest.shared.exercise.CommonShell;

import java.util.Locale;
import java.util.logging.Logger;

/**
 * Created by go22670 on 4/5/17.
 */
public class SearchHighlighter {
  private final Logger logger = Logger.getLogger("SearchHighlighter");

  private static final String STRONG = "strong";
  /**
   * Regular expression used to collapse all whitespace in a query string.
   */
  private static final String NORMALIZE_TO_SINGLE_WHITE_SPACE = "\\s+";

  String getQueryHighlightString(String formattedSuggestion, String query) {
    String lowerCaseSuggestion = normalizeSuggestion(formattedSuggestion);
    String[] searchWords = normalizeSearch(query).split(WHITESPACE_STRING);

//    logger.info("lower " + lowerCaseSuggestion);
//    for (String w : searchWords) logger.info("\ts '" + w + "'");
    return getHighlightedString(searchWords, formattedSuggestion, lowerCaseSuggestion);
  }

  /**
   * Normalize the search key by making it lower case, removing multiple spaces,
   * apply whitespace masks, and make it lower case.
   */
  private String normalizeSearch(String search) {
    // Use the same whitespace masks and case normalization for the search
    // string as was used with the candidate values.
    search = normalizeSuggestion(search);

    // Remove all excess whitespace from the search string.
    search = search.replaceAll(NORMALIZE_TO_SINGLE_WHITE_SPACE, WHITESPACE_STRING);

    return search.trim();
  }

  /**
   * Takes the formatted suggestion, makes it lower case and blanks out any
   * existing whitespace for searching.
   */
  private String normalizeSuggestion(String formattedSuggestion) {
    // Formatted suggestions should already have normalized whitespace. So we
    // can skip that step.

    // Lower case suggestion.
    formattedSuggestion = formattedSuggestion.toLowerCase(Locale.ROOT);

    // Apply whitespace.
//    if (whitespaceChars != null) {
//      for (int i = 0; i < whitespaceChars.length; i++) {
//        char ignore = whitespaceChars[i];
//        formattedSuggestion = formattedSuggestion.replace(ignore,
//            WHITESPACE_CHAR);
//      }
//    }
    return formattedSuggestion;
  }


  /**
   * @param searchWords
   * @param suggestion
   * @param lowerCaseSuggestion
   * @return
   * @see SearchTypeahead#getSuggestion(String[], CommonShell)
   */
  public String getHighlightedString(String[] searchWords, String suggestion, String lowerCaseSuggestion) {
    // Create strong search string.
    SafeHtmlBuilder accum = new SafeHtmlBuilder();

    // logger.info(" suggestion '" + suggestion+"'");
    //  logger.info(" lowerCase  '" + lowerCaseSuggestion+"'");

    int cursor = 0;
    int index = 0;
    int n = 100;
    while (n-- > 0) {
      WordBounds wordBounds = findNextWord(lowerCaseSuggestion, searchWords, index);
      //  logger.info("\tat " + index + " got " + wordBounds);
      if (wordBounds == null) {
        break;
      }
//      if (wordBounds.startIndex == 0 ||
//          WHITESPACE_CHAR == lowerCaseSuggestion.charAt(wordBounds.startIndex - 1)) {
      String part1 = suggestion.substring(cursor, wordBounds.startIndex);
      String part2 = suggestion.substring(wordBounds.startIndex, wordBounds.endIndex);
      cursor = wordBounds.endIndex;
      accum.appendEscaped(part1);
      accum.appendHtmlConstant("<" + STRONG + ">");
      accum.appendEscaped(part2);
      accum.appendHtmlConstant("</" + STRONG + ">");
//      }
      index = wordBounds.endIndex;
    }

//    // Check to make sure the search was found in the string.
//    if (cursor == 0) {
//      continue;
//    }

    accum.appendEscaped(suggestion.substring(cursor));

    // logger.info(" formatted     " + suggestion);
    String s = accum.toSafeHtml().asString();
    // logger.info(" safe          " + s);

    return s;
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

    WordBounds(int startIndex, int length) {
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

    public String toString() {
      return "[" + startIndex + "-" + endIndex + "]";
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
      if (word.isEmpty()){
        //logger.warning("got empty search term");
      }
      else {
        int index = candidate.indexOf(word, indexToStartAt);
        if (index != -1) {
          WordBounds newWord = new WordBounds(index, word.length());
          if (firstWord == null || newWord.compareTo(firstWord) < 0) {
            firstWord = newWord;
          }
        }
      }
    }
    return firstWord;
  }

  private static final String WHITESPACE_STRING = " ";
}
