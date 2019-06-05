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

import mitll.langtest.client.dialog.PerformViewHelper;
import mitll.langtest.client.sound.IHighlightSegment;
import mitll.langtest.shared.exercise.ClientExercise;
import mitll.langtest.shared.exercise.CommonShell;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

class ObscureHelper {
  private final Logger logger = Logger.getLogger("ObscureHelper");

  private static final boolean DEBUG_OVERLAP = false;

  private final int exid;
  private final String foreignLanguage;
  private final List<IHighlightSegment> flclickables;

  /**
   * @see SimpleTurn#maybeObscure(Set)
   * @param exid
   * @param foreignLanguage
   * @param flclickables
   */
  ObscureHelper(int exid, String foreignLanguage, List<IHighlightSegment> flclickables) {
    this.exid = exid;
    this.foreignLanguage = foreignLanguage;
    this.flclickables = flclickables;
  }

  /**
   * Rules:
   * <p>
   * 1) don't obscure everything
   * 2) obscure something
   * 3) Don't obscure more than one or two or three? words?
   * 4) if choosing only two out of all of them, choose the longest ones?
   * 3) if you have a choice, don't obscure first token? ?
   *
   * @param coreVocab
   * @see PerformViewHelper#getTurnPanel
   * Or should we use exact match?
   */
  void maybeSetObscure(List<? extends CommonShell> coreVocabs) {
    Set<String> coreVocab = new HashSet<>();

    coreVocabs.forEach(ex -> coreVocab.add(ex.getForeignLanguage().trim()));

    maybeObscure(coreVocab);
  }

  void maybeObscure(Set<String> coreVocab) {
    Collection<IHighlightSegment> obscureCandidates = getObscureCandidates(coreVocab);

    if (DEBUG_OVERLAP) logger.info("maybeSetObscure got " + coreVocab + " found " + obscureCandidates);

    obscureCandidates.forEach(IHighlightSegment::setObscurable);
  }

  /**
   * Get all segments that need to be blacked out.
   *
   * Should be case insensitive.
   *
   * @param coreVocab
   * @return
   * @see #maybeSetObscure(List)
   */
  private Collection<IHighlightSegment> getObscureCandidates(Collection<String> coreVocab) {
    if (DEBUG_OVERLAP) logger.info("getCandidates for " + exid + " " + foreignLanguage);

    Set<IHighlightSegment> candidates = new HashSet<>();

    // String foreignLanguage = exercise.getForeignLanguage();
    String foreignLanguage = this.foreignLanguage.toLowerCase();

    List<String> overlaps = coreVocab
        .stream()
        .map(String::toLowerCase)
        .filter(foreignLanguage::contains)
        .collect(Collectors.toList());

    Set<IHighlightSegment> toObscure = new HashSet<>();

    if (DEBUG_OVERLAP)
      logger.info("getObscureCandidates : from " + coreVocab.size() + " found " + overlaps.size() + " overlaps");

    overlaps.forEach(overlap -> {
      List<IHighlightSegment> clickableWordsHighlight = getClickableWordsHighlight(flclickables, overlap);
      if (!clickableWordsHighlight.isEmpty()) {
        if (DEBUG_OVERLAP) logger.info("\tgetObscureCandidates : match " + overlap + " =  " + clickableWordsHighlight);
      }
      toObscure.addAll(clickableWordsHighlight);
    });

    for (IHighlightSegment segment : flclickables) {
      if (toObscure.contains(segment)) {
        candidates.add(segment);
      }
    }

    if (DEBUG_OVERLAP) logger.info("getObscureCandidates : return " + candidates);

    return candidates;
  }

  /**
   * @param candidates
   * @param highlight
   * @return
   * @see #getObscureCandidates(Collection)
   */
  private List<IHighlightSegment> getClickableWordsHighlight(List<IHighlightSegment> candidates, String highlight) {
    boolean isChineseCharacter = false; // TODO : check language

    SearchTokenizer searchTokenizer = new SearchTokenizer();

    List<String> tokens = new ArrayList<>();
    for (IHighlightSegment candidate : candidates) {
      tokens.add(candidate.getContent());
    }
    if (DEBUG_OVERLAP) {
      logger.info("getClickableWordsHighlight " +
          //    "\n\tcontextSentence     '" + contextSentence + "'" +
          "\n\thighlight '" + highlight + "'" +
          "\n\ttokens    " + tokens +
          "\n\t# tokens  " + candidates.size());
    }

    // if the highlight token is not in the display, skip over it -

    List<String> highlightTokens = searchTokenizer.getTokens(highlight, isChineseCharacter, new ArrayList<>());

    if (DEBUG_OVERLAP) {
      logger.info("getClickableWordsHighlight " +
          //    "\n\tcontextSentence     '" + contextSentence + "'" +
          "\n\thighlight '" + highlightTokens + "'");
    }
    int highlightStartIndex = searchTokenizer.getMatchingHighlightAll(tokens, highlightTokens);

    Iterator<String> highlightIterator = highlightTokens.iterator();
    String highlightToFind = highlightIterator.hasNext() ? highlightIterator.next() : null;

    if (DEBUG_OVERLAP) {
      logger.info("getClickableWordsHighlight highlight start " + highlightStartIndex + " find " + highlightToFind);
    }

    List<IHighlightSegment> overlaps = new ArrayList<>();

    if (highlightStartIndex == -1 || highlightToFind == null) {

    } else {
      boolean isHighlightMatch = true;
      boolean maybeFoundAllTokens = false;
      while (isHighlightMatch && !maybeFoundAllTokens) {
        String token = tokens.get(highlightStartIndex);

        isHighlightMatch = searchTokenizer.isMatch(token, highlightToFind);

        if (isHighlightMatch) {
          if (DEBUG_OVERLAP)
            logger.info("getClickableWordsHighlight *highlight* '" + highlightToFind + "' = '" + token + "'");

          overlaps.add(candidates.get(highlightStartIndex));

          if (highlightIterator.hasNext()) {
            highlightToFind = highlightIterator.next();
            highlightStartIndex++;
          } else {
            maybeFoundAllTokens = true;
            // OK match
          }
        } else {
          maybeFoundAllTokens = true;
        }
      }
    }

    if (DEBUG_OVERLAP) {
      logger.info("getClickableWordsHighlight maybeFoundAllTokens " + overlaps.size() + " vs " + highlightTokens.size());
    }

    return (overlaps.size() == highlightTokens.size()) ? overlaps : Collections.emptyList();
  }

}
