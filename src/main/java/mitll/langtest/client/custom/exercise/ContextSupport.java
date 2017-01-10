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

package mitll.langtest.client.custom.exercise;

import mitll.langtest.shared.exercise.CommonExercise;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

class ContextSupport<T extends CommonExercise> {
  private Logger logger = Logger.getLogger("ContextSupport");

  private static final String HIGHLIGHT_START = "<span style='background-color:#5bb75b;color:black'>"; //#5bb75b
  private static final String HIGHLIGHT_END = "</span>";

  /**
   * @param context
   * @param foreignLanguage
   * @return html with underlines on the item text
   * @see
   */
  String getHighlightedItemInContext(String context, String foreignLanguage) {
    String trim = foreignLanguage.trim();
    String toFind = removePunct(trim);

    // split on spaces, find matching words if no contigious overlap
    int i = context.indexOf(toFind);
    if (i == -1) { // maybe mixed case - 'where' in Where is the desk?
      String str = toFind.toLowerCase();
      i = context.toLowerCase().indexOf(str);
      logger.info("Got " + i + " for " + str + " in " + context);
    }
    int end = i + toFind.length();
    if (i > -1) {
      //  if (debug) logger.info("marking underline from " + i + " to " + end + " for '" + toFind + "' in '" + trim + "'");
      context = context.substring(0, i) + HIGHLIGHT_START + context.substring(i, end) + HIGHLIGHT_END + context.substring(end);

      //  if (debug) logger.info("context " + context);

    } else {
      //if (debug) logger.info("NOT marking underline from " + i + " to " + end);
      //if (debug) logger.info("trim   " + trim + " len " + trim.length());
      //if (debug) logger.info("toFind " + toFind + " len " + trim.length());

      Collection<String> tokens = getTokens(trim);
      int startToken;
      int endToken = 0;
      StringBuilder builder = new StringBuilder();

      boolean b = allMatch(context, tokens);
      if (!b) {
        tokens = findLongest(context, tokens);
      }
      String lowerContext = context.toLowerCase();
      for (String token : tokens) {
        //if (debug) logger.info("getHighlightedItemInContext Check token '" + token + "'");
        startToken = lowerContext.indexOf(token, endToken);
        if (startToken != -1) {
          builder.append(context.substring(endToken, startToken));
          builder.append(HIGHLIGHT_START);
          builder.append(context.substring(startToken, endToken = startToken + token.length()));
          builder.append(HIGHLIGHT_END);
        } else {
//          if (debug)
//            logger.info("getHighlightedItemInContext from " + endToken + " couldn't find token '" + token + "' len " + token.length() + " in '" + context + "'");
        }
      }
      builder.append(context.substring(endToken));
//      if (debug) logger.info("before " + context + " after " + builder.toString());
      context = builder.toString();
    }
    return context;
  }

  private Collection<String> findLongest(String context, Collection<String> tokens) {
    List<String> tList = new ArrayList<>(tokens);
    List<String> highest = null;
    int score = 0;
    context = context.toLowerCase();
    for (int i = 0; i < tokens.size(); i++) {
      List<String> choice = tList.subList(i, tList.size());
      int scoreForChoice = getCharsMatched(context, choice);
      if (scoreForChoice > score) {
        highest = choice;
        score = scoreForChoice;
        //    logger.info("findLongest Got " + score + " for " + new HashSet<>(choice));
      } else {
        //  logger.info("findLongest Got " + score + " vs " + highest);
      }
    }
    return highest == null ? tList : highest;
  }


  private boolean allMatch(String context, Collection<String> tokens) {
    int startToken;
    int endToken = 0;
    context = context.toLowerCase();

    for (String token : tokens) {
      // logger.info("getHighlightedItemInContext Check token '" + token + "'");
      startToken = context.indexOf(token, endToken);
      if (startToken == -1) {
        // logger.info("getHighlightedItemInContext Check token '" + token + "' not after end " +endToken);
        return false;
      } else {
        endToken = startToken + token.length();
      }
    }
    return true;
  }

  private int getCharsMatched(String context, Collection<String> tokens) {
    int startToken;
    int endToken = 0;
    int total = 0;

    context = context.toLowerCase();

    for (String token : tokens) {
      //logger.info("getHighlightedItemInContext Check token '" + token + "'");
      startToken = context.indexOf(token, endToken);
      if (startToken == -1) {
        return total;
      } else {
        endToken = startToken + token.length();
        total += token.length();
      }
    }
    return total;
  }

  /**
   * @param sentence
   * @return
   * @see #getHighlightedItemInContext(String, String)
   */
  private Collection<String> getTokens(String sentence) {
    List<String> all = new ArrayList<>();
    sentence = removePunct(sentence);
    for (String untrimedToken : sentence.split(CommentNPFExercise.SPACE_REGEX)) { // split on spaces
      String tt = untrimedToken.replaceAll(CommentNPFExercise.PUNCT_REGEX, ""); // remove all punct
      String token = tt.trim();  // necessary?
      if (token.length() > 0) {
        all.add(token);
      }
    }

    return all;
  }

  protected String removePunct(String t) {
    return t.replaceAll(CommentNPFExercise.PUNCT_REGEX, "");
  }
}
