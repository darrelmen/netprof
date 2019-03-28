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

import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Style;
import com.google.gwt.i18n.client.HasDirection;
import com.google.gwt.i18n.shared.WordCountDirectionEstimator;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.client.ui.HTML;
import mitll.langtest.client.custom.dialog.WordBounds;
import mitll.langtest.client.custom.dialog.WordBoundsFactory;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.client.sound.HighlightSegment;
import mitll.langtest.client.sound.IHighlightSegment;
import mitll.langtest.shared.exercise.ClientExercise;
import mitll.langtest.shared.project.Language;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.logging.Logger;

/**
 * Created by go22670 on 3/23/17.
 */
public class ClickableWords {
  private final Logger logger = Logger.getLogger("ClickableWords");

  private static final boolean DEBUG_CHINESE = false;
  private static final boolean DEBUG_CHINESE2 = false;
  private static final String SEARCHMATCH = "searchmatch";

  private static final String CLICKABLE_ROW = "clickableRow";
  private static final String CONTEXTMATCH = "contextmatch";

  private static final String STRONG = "strong";
  private static final String END_STRONG = "</" + STRONG + ">";
  private static final String START_STRONG = "<" + STRONG + ">";

  private static final char FULL_WIDTH_ZERO = '\uFF10';
  private static final char ZERO = '0';

  private boolean isJapanese = false;
  private boolean isUrdu = false;

  private final int exercise;

  private boolean hasClickableAsian = false;

  private final ListInterface listContainer;
  private final WordBoundsFactory factory = new WordBoundsFactory();
  private final int fontSize;

  private final String highlightColor;
  private final boolean addFloatLeft;

  private static final boolean DEBUG = false;

  /**
   * @param listContainer
   * @param exercise
   * @param language
   * @param fontSize
   * @param highlightColor
   * @param addFloatLeft
   * @see RefAudioGetter#addWidgets
   * @see DialogExercisePanel#makeClickableWords
   */
  public ClickableWords(ListInterface listContainer, int exercise, Language language, int fontSize, String highlightColor, boolean addFloatLeft) {
    this.listContainer = listContainer;
    this.exercise = exercise;
    this.highlightColor = highlightColor;

    isJapanese = language == Language.JAPANESE;
    isUrdu = language == Language.URDU;
    this.hasClickableAsian =
        language == Language.MANDARIN ||
            //  language.equalsIgnoreCase(Language.KOREAN.name()) ||
            isJapanese;
    this.fontSize = fontSize;
    this.addFloatLeft = addFloatLeft;

 //   logger.info("addFloatLeft ex " + exercise + " = " + addFloatLeft);
  }

  /**
   * Split the value into tokens, and make each one clickable - so we can search on it.
   *
   * @param value
   * @param fieldType         fl, meaning, english, etc.
   * @param clickables
   * @param isRTL
   * @param tokensForMandarin
   * @see DialogExercisePanel#getFLEntry(ClientExercise)
   * @see TwoColumnExercisePanel#getFLEntry
   */
  DivWidget getClickableWords(String value,
                              FieldType fieldType,
                              List<IHighlightSegment> clickables,
                              boolean isRTL,
                              List<String> tokensForMandarin) {
    boolean flLine = fieldType == FieldType.FL || (isJapanese && fieldType == FieldType.TRANSLIT);
    boolean isChineseCharacter = flLine && hasClickableAsian;
    HasDirection.Direction dir = isRTL ? HasDirection.Direction.RTL :
        WordCountDirectionEstimator.get().estimateDirection(value);

  /*  logger.info("getClickableWords " +
        "\n\tvalue  " +value +
        "\n\tfor    " +fieldType +
        "\n\ttokens " +tokensForMandarin +
        "\n\tis chinese " +isChineseCharacter);*/

    return getClickableDiv(
        // isChineseCharacter ? tokensForMandarin :
        getTokens(value, isChineseCharacter, tokensForMandarin),
        getSearchTokens(isChineseCharacter), dir, clickables, fieldType);
  }

  /**
   * TODO: consider getting tokens for search string
   *
   * @param isChineseCharacter
   * @return
   */
  @NotNull
  private List<String> getSearchTokens(boolean isChineseCharacter) {
    return listContainer == null ? Collections.emptyList() : getTokens(listContainer.getTypeAheadText().toLowerCase(), isChineseCharacter, null);
  }

  /**
   * @param tokens
   * @param searchTokens
   * @param dir
   * @param clickables
   * @param fieldType
   * @return
   * @see #getClickableWords
   */
  @NotNull
  private DivWidget getClickableDiv(List<String> tokens,
                                    List<String> searchTokens,
                                    HasDirection.Direction dir,
                                    List<IHighlightSegment> clickables,
                                    FieldType fieldType
  ) {
    List<IHighlightSegment> segmentsForTokens = getSegmentsForTokens(tokens, searchTokens, dir, fieldType);
    clickables.addAll(segmentsForTokens);

    return getClickableDivFromSegments(segmentsForTokens, dir == HasDirection.Direction.RTL);
  }

  /**
   * @param tokens
   * @param searchTokens
   * @param dir
   * @param fieldType
   * @return
   */
  private List<IHighlightSegment> getSegmentsForTokens(List<String> tokens,
                                                       List<String> searchTokens,
                                                       HasDirection.Direction dir,
                                                       FieldType fieldType) {
    List<IHighlightSegment> segments = new ArrayList<>();
    int id = 0;
    Iterator<String> searchIterator = searchTokens.iterator();
    String searchToken = searchIterator.hasNext() ? searchIterator.next() : null;

    for (String token : tokens) {
      boolean searchMatch = isSearchMatch(token, searchToken);

      segments.add(makeClickableText(dir, token, searchMatch ? searchToken : null, false, id++, fieldType, true));

      if (searchMatch) {
        if (searchIterator.hasNext()) {
          searchToken = searchIterator.next();
        } else {
          searchIterator = searchTokens.iterator(); // start over!
          if (searchIterator.hasNext()) {
            searchToken = searchIterator.next();
          }
        }
      }
    }
    return segments;
  }

  /**
   * @param segmentsForTokens
   * @param isRTL
   * @return a clickable row
   * @see #getClickableDiv(List, List, HasDirection.Direction, List, FieldType)
   */
  @NotNull
  private DivWidget getClickableDivFromSegments(List<IHighlightSegment> segmentsForTokens, boolean isRTL) {
    DivWidget horizontal = getClickableDiv(isRTL);
    horizontal.getElement().setId(CLICKABLE_ROW);

/*
    if (!didIt) {
      didIt = true;
      intercept();
    }
*/

    segmentsForTokens.forEach(segment -> {
      horizontal.add(segment.asWidget());
      if (isRTL) {
        segment.getClickable().addStyleName("floatRight");
      }
    });

    return horizontal;
  }

/*
  private static boolean didIt = false;
  public native boolean intercept() */
/*-{
      var clipboard =
          {
              data: '',
              intercept: false,
              hook: function (evt) {
                  if (clipboard.intercept) {
                      evt.preventDefault();
                      evt.clipboardData.setData('text/plain', clipboard.data);
                      clipboard.intercept = false;
                      console.log("got data " + clipboard.data);
                      clipboard.data = '';
                  }
              }
          };
      window.addEventListener('copy', clipboard.hook);
  }-*//*
;
*/


  DivWidget getClickableDiv(boolean isRTL) {
    DivWidget horizontal = new DivWidget();
    horizontal.addStyleName("leftFiveMargin");
    if (isRTL) {
      setDirectionToRTL(horizontal);
    }
    return horizontal;
  }

  /**
   * Only for context.
   * <p>
   * So we want to highlight the vocab item in the context sentence.
   * This is separate from marking which tokens are matches to a search token.
   *
   * @param contextSentence
   * @param highlight
   * @param clickables
   * @param tokens
   * @return
   * @see TwoColumnExercisePanel#getContext
   */
  public DivWidget getClickableWordsHighlight(String contextSentence,
                                              String highlight,
                                              FieldType fieldType,
                                              List<IHighlightSegment> clickables,
                                              boolean addClickableProps,
                                              List<String> contextTokensOpt,
                                              List<String> highlightTokensOpt,
                                              boolean isRTL) {
    DivWidget horizontal = new DivWidget();

    horizontal.getElement().setId("clickableWordsHighlightRow");
    horizontal.setWidth("100%");

    boolean isFL = fieldType == FieldType.FL;
    boolean flLine = isFL || (isJapanese && fieldType == FieldType.TRANSLIT);
    boolean isChineseCharacter = flLine && hasClickableAsian;

    List<String> tokens = getTokens(contextSentence, isChineseCharacter, contextTokensOpt);

    if (DEBUG) {
      logger.info("getClickableWordsHighlight " +
          "\n\tcontextSentence     '" + contextSentence + "'" +
          "\n\thighlight '" + highlight + "'" +
          "\n\ttokens     " + tokens.size());
    }

    // if the highlight token is not in the display, skip over it -

    List<String> highlightTokens = getTokens(highlight, isChineseCharacter, highlightTokensOpt);
    int highlightStartIndex = getMatchingHighlightAll(tokens, highlightTokens);

    Iterator<String> highlightIterator = highlightTokens.iterator();
    String highlightToFind = highlightIterator.hasNext() ? highlightIterator.next() : null;
    if (DEBUG)
      logger.info("getClickableWordsHighlight highlight start " + highlightStartIndex + " find " + highlightToFind);

    HasDirection.Direction dir = isRTL ? HasDirection.Direction.RTL : HasDirection.Direction.LTR;

    if (DEBUG) {
      logger.info("getClickableWordsHighlight exercise " + exercise + " dir " + dir +
          " isfL " + fieldType);
    }
    if ((dir == HasDirection.Direction.RTL) && isFL) {
      if (DEBUG) logger.info("\texercise " + exercise + " is RTL ");
      setDirectionToRTL(horizontal);
    }
    int id = 0;

    List<String> searchTokens = getSearchTokens(isChineseCharacter);

    Iterator<String> searchIterator = searchTokens.iterator();

    String searchToken = searchIterator.hasNext() ? searchIterator.next() : null;


    for (String token : tokens) {
      if (DEBUG)
        logger.info("getClickableWordsHighlight token '" + highlightToFind + "' and '" + token + "' " + id + " vs " + highlightStartIndex);

      boolean isHighlightMatch =
          //highlightStartIndex >= 0 &&
          id >= highlightStartIndex &&
              highlightToFind != null &&
              isMatch(token, highlightToFind);

      boolean searchMatch = isSearchMatch(token, searchToken);

      //  logger.info("got search match for " +searchToken + " in " +token);
      IHighlightSegment clickable = makeClickableText(
          dir,
          token,
          searchMatch ? searchToken : null,
          isHighlightMatch,
          id++,
          fieldType, addClickableProps);
      clickables.add(clickable);
      horizontal.add(clickable.asWidget());

      if (isHighlightMatch) {
        if (DEBUG) logger.info("getClickableWordsHighlight *highlight* '" + highlightToFind + "' = '" + token + "'");
        highlightToFind = highlightIterator.hasNext() ? highlightIterator.next() : null;
      }

      if (searchMatch) {
        if (searchIterator.hasNext()) {
          searchToken = searchIterator.next();
        } else {
          searchIterator = searchTokens.iterator(); // start over!
          if (searchIterator.hasNext()) {
            searchToken = searchIterator.next();
          }
        }
      }
//      else if (isHighlightMatch) {
//        logger.fine("getClickableWordsHighlight no highlight '" + highightToFind + "' vs '" + token + "' match at " + match);
//      }
    }

    horizontal.addStyleName("leftFiveMargin");
    return horizontal;
  }

  void setDirectionToRTL(DivWidget horizontal) {
    horizontal.getElement().getStyle().setProperty("direction", "rtl");
  }

  /**
   * Korean feedback said no partial matches
   *
   * @param tokens
   * @param highlightTokens
   * @return
   */
  @NotNull
  private int getMatchingHighlightAll(List<String> tokens, List<String> highlightTokens) {
    List<String> realHighlight = new ArrayList<>();
    int numToFind = highlightTokens.size();

    int searchStart = 0;
    int startIndex = -1;

    int numTokens = tokens.size();
    while (realHighlight.size() < numToFind && searchStart < numTokens - realHighlight.size()) {
      Iterator<String> hIter = highlightTokens.iterator();
      String toFind = hIter.next();

      //  logger.info("getMatchingHighlightAll : find '" + toFind + "'");
      for (int i = searchStart; i < numTokens && startIndex == -1; i++) {
        String longer = tokens.get(i);
        if (isMatch(longer, toFind)) {
          startIndex = i;
        } else {
          //        logger.info("\tno match for '" + longer + "'");
        }
      }

      if (startIndex > -1) { // found first match
//        logger.info("getMatchingHighlightAll at " + startIndex);
        while (toFind != null &&
            startIndex < numTokens &&
            isMatch(tokens.get(startIndex++), toFind)) {
          realHighlight.add(toFind);
          toFind = hIter.hasNext() ? hIter.next() : null;
        }
      }

      if (realHighlight.size() < numToFind) {
        realHighlight.clear();
        searchStart++;
      }
    }

    return searchStart;
  }

  /**
   * @param longer
   * @param shorter
   * @return
   * @see #getClickableWordsHighlight
   * @see #getMatchingHighlightAll
   * @see #makeClickableText
   */
  private boolean isMatch(String longer, String shorter) {
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

  private boolean isSearchMatch(String first, String toSearchFor) {
    if (toSearchFor == null || toSearchFor.isEmpty()) {
      return false;
    } else {
      String context = removePunct(first.toLowerCase());
      String lcSearch = removePunct(toSearchFor.toLowerCase());
      return (context.contains(lcSearch));
    }
  }

//
//  boolean isExactMatch(String first, String toSearchFor) {
//    String context = removePunct(first.toLowerCase());
//    String lcSearch = removePunct(toSearchFor.toLowerCase());
//    return (context.equalsIgnoreCase(lcSearch));
//  }

  /**
   * @param value
   * @param dictTokens
   * @return
   * @see #getClickableWords(String, FieldType, List, boolean, List)
   * @see #getClickableWordsHighlight(String, String, FieldType, List, boolean, List)
   */
  @NotNull
  private List<String> getTokens(String value, boolean isChineseCharacter, List<String> dictTokens) {
    List<String> tokens = new ArrayList<>();
    if (isChineseCharacter) {
      if (dictTokens == null) {
        for (int i = 0, n = value.length(); i < n; i++) {
          Character character = value.charAt(i);
          tokens.add(character.toString());
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

  private static final String P_Z = "\\p{Z}+";

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
    int offset = c - ZERO;
    int full = FULL_WIDTH_ZERO + offset;
    return Character.valueOf((char) full);
  }

  private boolean isNumber(char c) {
    return c >= ZERO && c <= '9';
  }

  @NotNull
  private List<String> getTokensOnSpace(String value) {
    return Arrays.asList(value.split(GoodwaveExercisePanel.SPACE_REGEX));
  }

  /**
   * @param dir               text direction
   * @param html              a token that can be clicked on to search on it
   * @param searchToken
   * @param isContextMatch
   * @param id                to label highlight segment
   * @param fieldType         if meaning, use english font, if FL, choose the font
   * @param addClickableProps
   * @return
   * @see #getClickableWords
   * @see #getClickableWordsHighlight
   */
  private IHighlightSegment makeClickableText(
      HasDirection.Direction dir,
      final String html,
      String searchToken,
      boolean isContextMatch,
      int id,
      FieldType fieldType,
      boolean addClickableProps) {
    if (DEBUG) {
      logger.info("makeClickableText " + html + " token " + searchToken + " dir " + dir + " add float left " + addFloatLeft);

    }
    final IHighlightSegment highlightSegmentDiv = new HighlightSegment(id, html, dir,
        false,
        false,
        highlightColor, addFloatLeft);

    HTML highlightSegment = highlightSegmentDiv.getClickable();
    if (fieldType == FieldType.FL) {
      setFont(dir, highlightSegment);
    } else {
      highlightSegment.addStyleName("Instruction-data-with-wrap-keep-word");
    }

    if (isContextMatch) highlightSegment.addStyleName(CONTEXTMATCH);
    if (fieldType == FieldType.MEANING) highlightSegment.addStyleName("englishFont");

    if (searchToken != null) {
      showSearchMatch(dir, html, highlightSegment, searchToken);
    }

    String removePunct = removePunct(html);
    //  logger.info("makeClickableText text = '" + removePunct + "' original = '" + html + "'");
    boolean empty = removePunct.isEmpty();
    if (empty) {
      //  logger.info("makeClickableText for '" + html + "' not clickable");
      highlightSegmentDiv.setClickable(false);
    } else if (addClickableProps) {
      addClickableProperties(highlightSegment, removePunct);
    }

    return highlightSegmentDiv;
  }

  private void addClickableProperties(HTML highlightSegment, String removePunct) {
    highlightSegment.getElement().getStyle().setCursor(Style.Cursor.POINTER);
    highlightSegment.addClickHandler(clickEvent -> Scheduler.get().scheduleDeferred(() -> putTextInSearchBox(removePunct)));
    highlightSegment.addMouseOverHandler(mouseOverEvent -> highlightSegment.addStyleName("underline"));
    highlightSegment.addMouseOutHandler(mouseOutEvent -> highlightSegment.removeStyleName("underline"));
  }

  /**
   * Use bigflfont for RTL.
   *
   * @param dir
   * @param highlightSegment
   */
  private void setFont(HasDirection.Direction dir, HTML highlightSegment) {
    if (dir == HasDirection.Direction.RTL) {
      String bigflfont = isUrdu ? "urdubigflfont" : "bigflfont";
      highlightSegment.addStyleName(bigflfont);
    } else {
      highlightSegment.addStyleName("flfont");
      if (fontSize != 24) {
        Style style = highlightSegment.getElement().getStyle();
        style.setFontSize(fontSize, Style.Unit.PX);
        style.setLineHeight(fontSize + 8, Style.Unit.PX);
      }
    }
  }

  /**
   * @param dir
   * @param html
   * @param w
   * @param searchToken
   * @see #makeClickableText
   */
  private void showSearchMatch(HasDirection.Direction dir, String html, HTML w, String searchToken) {
    String[] toFind = new String[1];
    toFind[0] = searchToken;
    WordBounds wordBounds = factory.findNextWord(html.toLowerCase(), toFind, 0);

    if (wordBounds == null) {
      logger.warning("showSearchMatch can't find " +
          "\n\ttoken '" + searchToken + "' " +
          "\n\tin    '" + html + "'");

      w.addStyleName(SEARCHMATCH);
    } else {
      // logger.info("showSearchMatch word bounds " + wordBounds + " for '" + searchToken + "' in '" + html + "'");
      w.setHTML(getSearchHighlightedHTML(html, wordBounds), dir);
    }
  }

  private SafeHtml getSearchHighlightedHTML(String html, WordBounds wordBounds) {
    List<String> parts = wordBounds.getTriple(html);

    SafeHtmlBuilder accum = new SafeHtmlBuilder();

    accum.appendEscaped(parts.get(0));
    accum.appendHtmlConstant(START_STRONG);
    accum.appendEscaped(parts.get(1));
    accum.appendHtmlConstant(END_STRONG);
    accum.appendEscaped(parts.get(2));

    return accum.toSafeHtml();
  }

  private void putTextInSearchBox(String html) {
    // logger.info("putTextInSearchBox original " + html);
    String s1 = html.replaceAll(GoodwaveExercisePanel.PUNCT_REGEX, " ").replaceAll("’", " ");
    String s2 = s1.split(GoodwaveExercisePanel.SPACE_REGEX)[0].toLowerCase();
    // logger.info("putTextInSearchBox after    " + s2);
    if (listContainer != null) {
      listContainer.searchBoxEntry(s2);
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
  private String removePunct(String t) {
    return t
        .replaceAll(GoodwaveExercisePanel.PUNCT_REGEX, "")
        .replaceAll(GoodwaveExercisePanel.SPACE_REGEX, "")
        .replaceAll("\\u00ED", "i")
        // .replaceAll("\\u00E9", "\\u0435")

        .replaceAll("[\\u0301\\u0022\\u0027\\uFF01-\\uFF0F\\uFF1A-\\uFF1F\\u3002\\u300A\\u300B\\u003F\\u00BF\\u002E\\u002C\\u002D\\u0021\\u20260\\u005C\\u2013\\u2019]", "");
  }
}
