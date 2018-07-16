package mitll.langtest.client.scoring;

import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Style;
import com.google.gwt.i18n.client.HasDirection;
import com.google.gwt.i18n.shared.WordCountDirectionEstimator;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.custom.dialog.WordBounds;
import mitll.langtest.client.custom.dialog.WordBoundsFactory;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.client.sound.HighlightSegment;
import mitll.langtest.client.sound.IHighlightSegment;
import mitll.langtest.shared.project.Language;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

/**
 * Created by go22670 on 3/23/17.
 */
public class ClickableWords/*<T extends CommonShell> */ {
  private final Logger logger = Logger.getLogger("ClickableWords");
  private static final String SEARCHMATCH = "searchmatch";

  private static final String CLICKABLE_ROW = "clickableRow";
  private static final String CONTEXTMATCH = "contextmatch";

  private static final String STRONG = "strong";
  private static final String END_STRONG = "</" + STRONG + ">";
  private static final String START_STRONG = "<" + STRONG + ">";

  private boolean isJapanese = false;
  private boolean isUrdu = false;

  private int exercise;

  private boolean hasClickableAsian = false;
  private static final String MANDARIN = "Mandarin";
  private static final String JAPANESE = "Japanese";

  private ListInterface listContainer;
  private final WordBoundsFactory factory = new WordBoundsFactory();
  private int fontSize;

  private static final boolean DEBUG = false;
  //private boolean showPhones = true;
  private String highlightColor;

  /**
   * @see mitll.langtest.client.flashcard.BootstrapExercisePanel#showRecoOutput
   */
  public ClickableWords() {
  }

  /**
   * @param listContainer
   * @param exercise
   * @param language
   * @param fontSize
   * @param highlightColor
   * @see RefAudioGetter#addWidgets
   */
  ClickableWords(ListInterface listContainer, int exercise, String language, int fontSize, String highlightColor) {
    this.listContainer = listContainer;
    this.exercise = exercise;
    this.highlightColor = highlightColor;

    isJapanese = language.equalsIgnoreCase(JAPANESE);
    isUrdu = language.equalsIgnoreCase("Urdu");
    this.hasClickableAsian =
        language.equalsIgnoreCase(MANDARIN) || language.equalsIgnoreCase(Language.KOREAN.name()) || isJapanese;
    this.fontSize = fontSize;
  }

  /**
   * Split the value into tokens, and make each one clickable - so we can search on it.
   *
   * @param value
   * @param clickables
   * @param isRTL
   * @see TwoColumnExercisePanel#getFLEntry
   */
  DivWidget getClickableWords(String value,
                              FieldType fieldType,
                              List<IHighlightSegment> clickables,
                              boolean isRTL) {
    //boolean isFL = fieldType == FieldType.FL;
    boolean flLine = fieldType == FieldType.FL || (isJapanese && fieldType == FieldType.TRANSLIT);
    boolean isChineseCharacter = flLine && hasClickableAsian;
    HasDirection.Direction dir = isRTL ? HasDirection.Direction.RTL :
        WordCountDirectionEstimator.get().estimateDirection(value);

    return getClickableDiv(
        getTokens(value, isChineseCharacter),
        getSearchTokens(isChineseCharacter), dir, clickables, fieldType);
  }

  @NotNull
  private List<String> getSearchTokens(boolean isChineseCharacter) {
    return listContainer == null ? new ArrayList<>() : getTokens(listContainer.getTypeAheadText().toLowerCase(), isChineseCharacter);
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

      segments.add(makeClickableText(dir, token, searchMatch ? searchToken : null, false, id++, fieldType));

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
      setDirection(horizontal);
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
   * @return
   * @see TwoColumnExercisePanel#getContext
   */
  DivWidget getClickableWordsHighlight(String contextSentence,
                                       String highlight,
                                       FieldType fieldType,
                                       List<IHighlightSegment> clickables) {
    DivWidget horizontal = new DivWidget();

    horizontal.getElement().setId("clickableWordsHighlightRow");
    horizontal.setWidth("100%");

    boolean isFL = fieldType == FieldType.FL;
    boolean flLine = isFL || (isJapanese && fieldType == FieldType.TRANSLIT);
    boolean isChineseCharacter = flLine && hasClickableAsian;

    List<String> tokens = getTokens(contextSentence, isChineseCharacter);

    if (DEBUG) {
      logger.info("getClickableWordsHighlight " +
          "\n\tcontextSentence     '" + contextSentence + "'" +
          "\n\thighlight '" + highlight + "'" +
          "\n\ttokens     " + tokens.size());
    }

    // if the highlight token is not in the display, skip over it -

    List<String> highlightTokens = getTokens(highlight, isChineseCharacter);
    int highlightStartIndex = getMatchingHighlightAll(tokens, highlightTokens);

    Iterator<String> highlightIterator = highlightTokens.iterator();
    String highlightToFind = highlightIterator.hasNext() ? highlightIterator.next() : null;
    if (DEBUG)
      logger.info("getClickableWordsHighlight highlight start " + highlightStartIndex + " find " + highlightToFind);

    HasDirection.Direction dir = WordCountDirectionEstimator.get().estimateDirection(contextSentence);

    if (DEBUG) {
      logger.info("getClickableWordsHighlight exercise " + exercise + " dir " + dir +
          " isfL " + fieldType);
    }
    if ((dir == HasDirection.Direction.RTL) && isFL) {
      if (DEBUG) logger.info("\texercise " + exercise + " is RTL ");
      setDirection(horizontal);
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
          fieldType);
      clickables.add(clickable);
      Widget w = clickable.asWidget();
      horizontal.add(w);

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

  public void setDirection(DivWidget horizontal) {
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
   * @see #getClickableWordsHighlight(String, String, FieldType, List)
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

  private boolean isSearchMatch(String first, String search) {
    if (search == null || search.isEmpty()) {
      return false;
    } else {
      String context = removePunct(first.toLowerCase());
      String lcSearch = removePunct(search.toLowerCase());
      return (context.contains(lcSearch));
    }
  }

  /**
   * @param value
   * @param isChineseCharacter
   * @return
   * @seex #getClickableWords(String, TwoColumnExercisePanel.FieldType, List, boolean, boolean)
   * @seex #getClickableWordsHighlight(String, String, TwoColumnExercisePanel.FieldType, List, boolean)
   */
  @NotNull
  private List<String> getTokens(String value, boolean isChineseCharacter) {
    List<String> tokens = new ArrayList<>();
    if (isChineseCharacter) {
      for (int i = 0, n = value.length(); i < n; i++) {
        Character character = value.charAt(i);
        tokens.add(character.toString());
      }
    } else {
      value = value.replaceAll("/", " / ");  // so X/Y becomes X / Y
      tokens = new ArrayList<>(Arrays.asList(value.split(GoodwaveExercisePanel.SPACE_REGEX)));
    }

    return tokens;
  }

  public boolean isRTL(String fl) {
    return isRTLContent(fl);
  }

  private boolean isRTLContent(String content) {
    return WordCountDirectionEstimator.get().estimateDirection(content) == HasDirection.Direction.RTL;
  }

  /**
   * @param dir            text direction
   * @param html           a token that can be clicked on to search on it
   * @param isContextMatch
   * @param id
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
      FieldType fieldType) {
    final IHighlightSegment highlightSegmentDiv = new HighlightSegment(id, html, dir,
        false,
        false,
        highlightColor);

    HTML highlightSegment = highlightSegmentDiv.getClickable();
    if (fieldType == FieldType.FL) {
      if (dir == HasDirection.Direction.RTL) {
        String bigflfont = "bigflfont";
        if (isUrdu) bigflfont = "urdubigflfont";
        highlightSegment.addStyleName(bigflfont);
      } else {
        highlightSegment.addStyleName("flfont");
        if (fontSize != 24) {
          highlightSegment.getElement().getStyle().setFontSize(fontSize, Style.Unit.PX);
        }
      }
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
    } else {
      highlightSegment.getElement().getStyle().setCursor(Style.Cursor.POINTER);
      highlightSegment.addClickHandler(clickEvent -> Scheduler.get().scheduleDeferred(() -> putTextInSearchBox(removePunct)));
      highlightSegment.addMouseOverHandler(mouseOverEvent -> highlightSegment.addStyleName("underline"));
      highlightSegment.addMouseOutHandler(mouseOutEvent -> highlightSegment.removeStyleName("underline"));
    }

    return highlightSegmentDiv;
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
      logger.warning("\n\n\nshowSearchMatch can't find  '" + searchToken + "' in '" + html + "'");
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
