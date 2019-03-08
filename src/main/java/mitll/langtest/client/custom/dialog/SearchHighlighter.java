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

import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.client.ui.MultiWordSuggestOracle;

import java.util.logging.Logger;

/**
 * Created by go22670 on 4/5/17.
 */
class SearchHighlighter {
  private final Logger logger = Logger.getLogger("SearchHighlighter");

  private static final String STRONG = "strong";
  /**
   * Regular expression used to collapse all whitespace in a query string.
   */
  //private static final String NORMALIZE_TO_SINGLE_WHITE_SPACE = "\\s+";

/*  String getQueryHighlightString(String formattedSuggestion, String query) {
    String lowerCaseSuggestion = normalizeSuggestion(formattedSuggestion);
    String[] searchWords = normalizeSearch(query).split(WHITESPACE_STRING);

//    logger.info("lower " + lowerCaseSuggestion);
//    for (String w : searchWords) logger.info("\ts '" + w + "'");
    return getHighlightedString(searchWords, formattedSuggestion, lowerCaseSuggestion);
  }*/

  /**
   * Normalize the search key by making it lower case, removing multiple spaces,
   * apply whitespace masks, and make it lower case.
   */
/*  private String normalizeSearch(String search) {
    // Use the same whitespace masks and case normalization for the search
    // string as was used with the candidate values.
    search = normalizeSuggestion(search);

    // Remove all excess whitespace from the search string.
    search = search.replaceAll(NORMALIZE_TO_SINGLE_WHITE_SPACE, WHITESPACE_STRING);

    return search.trim();
  }*/

  /**
   * Takes the formatted suggestion, makes it lower case and blanks out any
   * existing whitespace for searching.
   */
/*  private String normalizeSuggestion(String formattedSuggestion) {
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
  }*/

  private final boolean debug = false;

  private final WordBoundsFactory factory = new WordBoundsFactory();
  /**
   * @param searchWords
   * @param suggestion
   * @param lowerCaseSuggestion
   * @return
   * @see SearchTypeahead#getSuggestion
   */
  String getHighlightedString(String[] searchWords, String suggestion, String lowerCaseSuggestion) {
    // Create strong search string.
    SafeHtmlBuilder accum = new SafeHtmlBuilder();

    if (debug) logger.info(" getHighlightedString suggestion '" + suggestion + "'");
    //  logger.info(" lowerCase  '" + lowerCaseSuggestion+"'");

    int cursor = 0;
    int index = 0;
    int n = 100;
    while (n-- > 0) {
      WordBounds wordBounds = factory.findNextWord(lowerCaseSuggestion, searchWords, index);
      if (debug) logger.info("\tgetHighlightedString at " + index + " got " + wordBounds);
      if (wordBounds == null) {
        break;
      }
//      if (wordBounds.startIndex == 0 ||
//          WHITESPACE_CHAR == lowerCaseSuggestion.charAt(wordBounds.startIndex - 1)) {
      //String part1 = suggestion.substring(cursor, wordBounds.startIndex);
     // String part2 = suggestion.substring(wordBounds.startIndex, wordBounds.endIndex);
      String[] parts = wordBounds.getParts(suggestion, cursor);
      cursor = wordBounds.getEndIndex();
      accum.appendEscaped(parts[0]);
      accum.appendHtmlConstant("<" + STRONG + ">");
      accum.appendEscaped(parts[1]);
      accum.appendHtmlConstant("</" + STRONG + ">");
//      }
      index = wordBounds.getEndIndex();
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
   * Returns a {@link MultiWordSuggestOracle.WordBounds} representing the first word in {@code
   * searchWords} that is found in candidate starting at {@code indexToStartAt}
   * or {@code null} if no words could be found.
   */
/*  private WordBounds findNextWord(String candidate, String[] searchWords, int indexToStartAt) {
    WordBounds firstWord = null;
    for (String word : searchWords) {
      if (word.isEmpty()) logger.warning("findNextWord got empty search term");
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
  }*/

//  private static final String WHITESPACE_STRING = " ";
}
