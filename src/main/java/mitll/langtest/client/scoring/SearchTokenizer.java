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

package mitll.langtest.client.scoring;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

public class SearchTokenizer {
  private final Logger logger = Logger.getLogger("SearchTokenizer");

  private static final boolean DEBUG = false;
  private static final boolean DEBUG_CHINESE = false;
  private static final boolean DEBUG_CHINESE2 = false;
  private static final char FULL_WIDTH_ZERO = '\uFF10';
  private static final char ZERO = '0';
  private static final String P_Z = "\\p{Z}+";

  /**
   * @param value
   * @param dictTokens
   * @return
   * @see #getClickableWords(String, FieldType, List, boolean, List)
   * @see #getClickableWordsHighlight(String, String, FieldType, List, boolean, List)
   */
  @NotNull
  public List<String> getTokens(String value, boolean isChineseCharacter, List<String> dictTokens) {
    List<String> tokens = new ArrayList<>();
    if (isChineseCharacter) {
      if (dictTokens == null) {
        for (int i = 0, n = value.length(); i < n; i++) {
          char character = value.charAt(i);
          tokens.add(Character.toString(character));
        }
      } else {
        tokens = getChineseMatches(value, dictTokens);
      }
    } else {
      value = value.replaceAll("/", " / ");  // so X/Y becomes X / Y
      tokens = getTokensOnSpace(value);
    }

    return tokens;
  }

  /**
   * xy? ab?
   * xy|ab
   * <p>
   * efgh
   * ef|gh
   *
   * @param value
   * @param dictTokens
   * @return
   */
  private List<String> getChineseMatches(String value, List<String> dictTokens) {
    List<String> tokens = new ArrayList<>();
    int d = 0;
    int dd = 0;
    String prodToken = "";
    if (DEBUG_CHINESE) logger.info("getChineseMatches " + value + " vs " + dictTokens);
    for (int i = 0, n = value.length(); i < n; i++) {
      Character character = value.charAt(i);
      if (DEBUG_CHINESE) logger.info("getChineseMatches " + character + " at " + i + "/" + n);

      String dict = d < dictTokens.size() ? dictTokens.get(d) : null;
      if (dict != null) {
        Character dc = dict.charAt(dd);

        int length = dict.length();
        if (isMatch(character, dc)) {

          if (DEBUG_CHINESE) logger.info("match " + character + " = " + dc);

          if (!prodToken.isEmpty() && dd == 0) {
            tokens.add(prodToken);
            if (DEBUG_CHINESE) logger.info("start over, addToken " + prodToken);
            prodToken = "";
          }
          prodToken += character;
          dd++;

          if (dd == length) {  // end of the dict token
            d++;
            dd = 0;
            dict = d < dictTokens.size() ? dictTokens.get(d) : null;
            if (DEBUG_CHINESE) logger.info("dict token now " + dict);

            if (dict != null) {
              dc = dict.charAt(dd);
              Character next = i + 1 < n ? value.charAt(i + 1) : null;
              if (DEBUG_CHINESE) logger.info("2 compare " + next + " vs " + dc);
              if (next != null && isMatch(next, dc)) {  // match on next token
                tokens.add(prodToken);
                if (DEBUG_CHINESE) logger.info("addToken " + prodToken);
                prodToken = "";
              }
            }
//            tokens.add(prodToken);
//            prodToken = "";
//
//            d++;
//            dd = 0;
          }
        } else {  // e.g. ? vs chinese character
          if (DEBUG_CHINESE) logger.info("no match " + character + " != " + dc);

          String test = "" + character;
          if (test.replaceAll(P_Z, "").isEmpty()) {
            tokens.add(prodToken);
            if (DEBUG_CHINESE) logger.info("got space, add token " + prodToken);

            prodToken = "";
//          } else if (removePunct(test).isEmpty()) { // it's a punct!
//            prodToken += character;
          } else {// it's a punct!
            prodToken += character;
            if (DEBUG_CHINESE) logger.info("prodToken now " + prodToken);

          }
        }
      } else {
        prodToken += character;
      }

      //tokens.add(character.toString());
    }
    if (!prodToken.isEmpty())
      tokens.add(prodToken);

    if (DEBUG_CHINESE2) logger.info("getChineseMatches " + value + " vs " + dictTokens + " = " + tokens);
    return tokens;
  }

  private boolean isMatch(Character character, Character dc) {
    boolean equals = character.equals(dc);
    if (!equals && isNumber(character)) {
      equals = getFullCharacter(character).equals(dc);
      if (DEBUG_CHINESE) logger.info("isMatch " + character + " vs " + dc + " = " + equals);
    }
    return equals;
  }

  private Character getFullCharacter(char c) {
    int full = FULL_WIDTH_ZERO + (c - ZERO);
    return (char) full;
  }

  private boolean isNumber(char c) {
    return c >= ZERO && c <= '9';
  }

  @NotNull
  private List<String> getTokensOnSpace(String value) {
    return Arrays.asList(value.split(GoodwaveExercisePanel.SPACE_REGEX));
  }

  /**
   * Korean feedback said no partial matches
   *
   * @param tokens
   * @param highlightTokens
   * @return index of first matching token
   * @see ObscureRecordDialogExercisePanel#getClickableWordsHighlight
   */
  @NotNull
  int getMatchingHighlightAll(List<String> tokens, List<String> highlightTokens) {
    List<String> realHighlight = new ArrayList<>();
    int numToFind = highlightTokens.size();

    int searchStart = 0;
    int candidateStart = -1;
    //  int returnedStart = 0;
    int startIndex = -1;

    int numTokens = tokens.size();

    if (DEBUG) logger.info("getMatchingHighlightAll : numToFind " + numToFind);

    while (realHighlight.size() < numToFind && searchStart < numTokens - realHighlight.size()) {
      Iterator<String> hIter = highlightTokens.iterator();
      String toFind = hIter.next();

      if (DEBUG)
        logger.info("getMatchingHighlightAll : find '" + toFind + "' at " + searchStart + " index " + startIndex);
      for (int i = searchStart; i < numTokens && startIndex == -1; i++) {
        String longer = tokens.get(i);
        if (isMatch(longer, toFind)) {
          if (candidateStart == -1) {
            candidateStart = i;
          }
          startIndex = i;
          if (DEBUG) logger.info("\tmatch for '" + longer + "'");
        } else {
          if (DEBUG) logger.info("\tno match for '" + longer + "'");
        }
      }

      if (startIndex > -1) { // found first match
        if (DEBUG) logger.info("getMatchingHighlightAll at " + startIndex + " find " + toFind);
        while (toFind != null &&
            startIndex < numTokens &&
            isMatch(tokens.get(startIndex++), toFind)) {
          realHighlight.add(toFind);
          toFind = hIter.hasNext() ? hIter.next() : null;
          if (DEBUG)
            logger.info("\tgetMatchingHighlightAll now at " + startIndex + " find " + toFind + " so far " + realHighlight.size());

        }
      }

      if (realHighlight.size() < numToFind) {
        if (DEBUG)
          logger.info("\tgetMatchingHighlightAll start over : numToFind " + numToFind + " vs " + realHighlight.size());
        realHighlight.clear();
        searchStart++;
        candidateStart = -1;
      } else {
        if (DEBUG)
          logger.info("\tgetMatchingHighlightAll numToFind " + numToFind + " vs " + realHighlight.size() + " start " + searchStart);

      }
    }

    return candidateStart == -1 ? 0 : candidateStart;
  }

  /**
   * @param longer
   * @param shorter
   * @return
   * @see #getClickableWordsHighlight
   * @see #getMatchingHighlightAll
   * @see #makeClickableText
   */
  protected boolean isMatch(String longer, String shorter) {
    if (shorter.isEmpty()) {
      return false;
    } else {
      String context = removePunct(longer.toLowerCase());
      String vocab = removePunct(shorter.toLowerCase());
      // if (DEBUG) logger.info("context " + context + " longer " + longer);
      boolean equals = context.equals(vocab);
      //   if (DEBUG) logger.info("isMatch : context '" + context + "' vocab '" + longer +"' = " + equals);

      boolean b = equals || (context.contains(vocab) && !vocab.isEmpty());
/*      if (b && DEBUG)
        logger.info("isMatch match '" + longer + "' '" + shorter + "' context '" + context + "' vocab '" + vocab + "'");
     */
      return b;
    }
  }

  /**
   * Not sure why we're doing this... again...
   * <p>
   * First is russian accent mark.
   * russian hyphen
   * Chinese punctuation marks, spanish punct marks
   * horizontal ellipsis...
   * reverse solidus
   * aprostophe...
   * <p>
   * right single quote
   *
   * @param t
   * @return
   */
  protected String removePunct(String t) {
    return t
        .replaceAll(GoodwaveExercisePanel.PUNCT_REGEX, "")
        .replaceAll(GoodwaveExercisePanel.SPACE_REGEX, "")
        .replaceAll("\\u00ED", "i")
        // .replaceAll("\\u00E9", "\\u0435")

        .replaceAll("[\\u0301\\u0022\\u0027\\uFF01-\\uFF0F\\uFF1A-\\uFF1F\\u3002\\u300A\\u300B\\u003F\\u00BF\\u002E\\u002C\\u002D\\u0021\\u20260\\u005C\\u2013\\u2019]", "");
  }
}
