package mitll.langtest.client.scoring;

import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Style;
import com.google.gwt.i18n.client.HasDirection;
import com.google.gwt.i18n.shared.WordCountDirectionEstimator;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.client.ui.InlineHTML;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.custom.dialog.WordBounds;
import mitll.langtest.client.custom.dialog.WordBoundsFactory;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.client.sound.HighlightSegment;
import mitll.langtest.client.sound.IHighlightSegment;
import mitll.langtest.client.sound.SimpleHighlightSegment;
import mitll.langtest.shared.exercise.CommonExercise;
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
public class ClickableWords<T extends CommonExercise> {
  private final Logger logger = Logger.getLogger("ClickableWords");
  private static final String SEARCHMATCH = "searchmatch";

  private static final String CLICKABLE_ROW = "clickableRow";
  private static final String CONTEXTMATCH = "contextmatch";

  private static final String STRONG = "strong";
  private static final String END_STRONG = "</" + STRONG + ">";
  private static final String START_STRONG = "<" + STRONG + ">";
  public static final double THRESHOLD = 0.3;

  private boolean isJapanese = false;

  private T exercise;
  private boolean hasClickableAsian = false;
  private static final String MANDARIN = "Mandarin";
  private static final String JAPANESE = "Japanese";
  public static final String DEFAULT_SPEAKER = "Default Speaker";
  private ListInterface listContainer;
  private WordBoundsFactory factory = new WordBoundsFactory();
  private int fontSize;

  private static final boolean DEBUG = false;

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
   */
  ClickableWords(ListInterface listContainer, T exercise, String language, int fontSize) {
    this.listContainer = listContainer;
    this.exercise = exercise;
    isJapanese = language.equalsIgnoreCase(JAPANESE);
    this.hasClickableAsian = language.equalsIgnoreCase(MANDARIN) || language.equalsIgnoreCase(Language.KOREAN.name()) || isJapanese;
    this.fontSize = fontSize;
  }

  /**
   * Split the value into tokens, and make each one clickable - so we can search on it.
   *
   * @param value
   * @param clickables
   * @param isSimple
   * @param addRightMargin
   * @param isRTL
   * @see TwoColumnExercisePanel#getEntry
   */
  DivWidget getClickableWords(String value,
                              TwoColumnExercisePanel.FieldType fieldType,
                              List<IHighlightSegment> clickables,
                              boolean isSimple,
                              boolean addRightMargin,
                              boolean isRTL) {
    boolean isFL = fieldType == TwoColumnExercisePanel.FieldType.FL;
    boolean flLine = isFL || (isJapanese && fieldType == TwoColumnExercisePanel.FieldType.TRANSLIT);
    boolean isChineseCharacter = flLine && hasClickableAsian;
    List<String> tokens = getTokens(value, isChineseCharacter);

    List<String> searchTokens = getSearchTokens(isChineseCharacter);

    HasDirection.Direction dir = isRTL ? HasDirection.Direction.RTL :
        WordCountDirectionEstimator.get().estimateDirection(value);

    boolean addRight = (isSimple && !isChineseCharacter) || addRightMargin;
    return getClickableDiv(tokens, searchTokens, isSimple, addRight, dir, clickables, fieldType);
  }

  @NotNull
  public List<String> getSearchTokens(boolean isChineseCharacter) {
    String searchToken = listContainer.getTypeAheadText().toLowerCase();
    return getTokens(searchToken, isChineseCharacter);
  }

  @NotNull
  private DivWidget getClickableDiv(List<String> tokens,
                                    List<String> searchTokens,
                                    boolean isSimple,
                                    boolean addRightMargin,
                                    HasDirection.Direction dir,
                                    List<IHighlightSegment> clickables,
                                    TwoColumnExercisePanel.FieldType fieldType
  ) {
    List<IHighlightSegment> segmentsForTokens = getSegmentsForTokens(isSimple, tokens, searchTokens, dir, fieldType);
    clickables.addAll(segmentsForTokens);

    return getClickableDivFromSegments(segmentsForTokens, addRightMargin, dir == HasDirection.Direction.RTL);
  }

  private List<IHighlightSegment> getSegmentsForTokens(boolean isSimple,
                                                       List<String> tokens,

                                                       List<String> searchTokens,

                                                       HasDirection.Direction dir,
                                                       TwoColumnExercisePanel.FieldType fieldType) {
    List<IHighlightSegment> segments = new ArrayList<>();
    int id = 0;
    Iterator<String> searchIterator = searchTokens.iterator();
    String searchToken = searchIterator.hasNext() ? searchIterator.next() : null;

    for (String token : tokens) {
      boolean searchMatch = isSearchMatch(token, searchToken);

      segments.add(makeClickableText(dir, token, searchMatch ? searchToken : null, false, id++, isSimple, fieldType));

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
   * @param addRightMargin
   * @param isRTL
   * @return a clickable row
   */
  @NotNull
  private DivWidget getClickableDivFromSegments(List<IHighlightSegment> segmentsForTokens, boolean addRightMargin, boolean isRTL) {
    DivWidget horizontal = new DivWidget();
    horizontal.getElement().setId(CLICKABLE_ROW);
    horizontal.getElement().getStyle().setDisplay(Style.Display.INLINE_BLOCK);

    if (isRTL) {
      setDirection(horizontal);
    }
    // int i = 0;
    for (IHighlightSegment segment : segmentsForTokens) {
      horizontal.add(segment.asWidget());
      // logger.info("adding token " + (i++) + " " + segment);
      InlineHTML clickable = segment.getClickable();
      if (addRightMargin) {
        clickable.addStyleName("rightFiveMargin");
      }
      if (isRTL) clickable.addStyleName("floatRight");
    }

    horizontal.addStyleName("leftFiveMargin");

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
   * @param isSimple
   * @return
   * @see TwoColumnExercisePanel#getContext
   */
  DivWidget getClickableWordsHighlight(String contextSentence,
                                       String highlight,
                                       //        String searchToken,
                                       TwoColumnExercisePanel.FieldType fieldType,
                                       List<IHighlightSegment> clickables,
                                       boolean isSimple) {
    DivWidget horizontal = new DivWidget();

    horizontal.getElement().setId("clickableWordsHighlightRow");
    horizontal.setWidth("100%");

    boolean isFL = fieldType == TwoColumnExercisePanel.FieldType.FL;
    boolean flLine = isFL || (isJapanese && fieldType == TwoColumnExercisePanel.FieldType.TRANSLIT);
    boolean isChineseCharacter = flLine && hasClickableAsian;

    List<String> tokens = getTokens(contextSentence, isChineseCharacter);

    if (DEBUG) {
      logger.info("getClickableWordsHighlight " +
          "\n\tcontextSentence     '" + contextSentence + "'" +
          "\n\thighlight '" + highlight + "'" +
          "\n\ttokens     " + tokens.size());
    }

    // if the highlight token is not in the display, skip over it -
    List<String> realHighlight = getMatchingHighlight(tokens, getTokens(highlight, isChineseCharacter));

    if (DEBUG) logger.info("getClickableWordsHighlight real " + realHighlight);

    Iterator<String> highlightIterator = realHighlight.iterator();
    String toFind = highlightIterator.hasNext() ? highlightIterator.next() : null;

    HasDirection.Direction dir = WordCountDirectionEstimator.get().estimateDirection(contextSentence);

    if (DEBUG) {
      logger.info("getClickableWordsHighlight exercise " + exercise.getID() + " dir " + dir +
          " isfL " + fieldType);
    }
    if ((dir == HasDirection.Direction.RTL) && isFL) {
      if (DEBUG) logger.info("exercise " + exercise.getID() + " is RTL ");
      setDirection(horizontal);
    }
    int id = 0;

    List<String> searchTokens = getSearchTokens(isChineseCharacter);

    Iterator<String> searchIterator = searchTokens.iterator();

    String searchToken = searchIterator.hasNext() ? searchIterator.next() : null;

    for (String token : tokens) {
      if (DEBUG) logger.info("getClickableWordsHighlight token '" + toFind + "' and '" + token + "'");

      boolean isMatch = toFind != null && isMatch(token, toFind);
      boolean searchMatch = isSearchMatch(token, searchToken);

     //  logger.info("got search match for " +searchToken + " in " +token);
      IHighlightSegment clickable = makeClickableText(
          dir,
          token,
          searchMatch ? searchToken : null,
          isMatch,
          id++,
          isSimple, fieldType);
      clickables.add(clickable);
      Widget w = clickable.asWidget();
      w.addStyleName("rightFiveMargin");
      horizontal.add(w);

      if (isMatch) {
        if (DEBUG) logger.info("getClickableWordsHighlight highlight '" + toFind + "' = '" + token + "'");
        toFind = highlightIterator.hasNext() ? highlightIterator.next() : null;
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
//      else if (isMatch) {
//        logger.fine("getClickableWordsHighlight no highlight '" + toFind + "' vs '" + token + "' match at " + match);
//      }
    }

    horizontal.addStyleName("leftFiveMargin");
    return horizontal;
  }

  public void setDirection(DivWidget horizontal) {
    horizontal.getElement().getStyle().setProperty("direction", "rtl");
  }

  /**
   * @param tokens
   * @param highlightTokens
   * @return
   * @see #getClickableWords
   */
  @NotNull
  private List<String> getMatchingHighlight(List<String> tokens, List<String> highlightTokens) {
    List<String> realHighlight = new ArrayList<>();
    Iterator<String> hIter = highlightTokens.iterator();

    int index = 0;

    for (; hIter.hasNext(); ) {
      String toFind = hIter.next();

      for (int i = index; i < tokens.size(); i++) {
        String next = tokens.get(i);
        if (isMatch(next, toFind)) {
          index = i;
          realHighlight.add(toFind);
          //logger.info("- found '" + toFind + "' = '" +next+ "' at " + index);
          break;
        }
      }
    }
    return realHighlight;
  }

  /**
   * @param longer
   * @param shorter
   * @return
   * @see #getClickableWordsHighlight(String, String, TwoColumnExercisePanel.FieldType, List, boolean)
   * @see #getMatchingHighlight
   * @see #makeClickableText
   */
  private boolean isMatch(String longer, String shorter) {
    if (shorter.isEmpty()) {
      return false;
    } else {
      String context = removePunct(longer.toLowerCase());
      String vocab = removePunct(shorter.toLowerCase());
      // if (DEBUG) logger.info("context " + context + " longer " + longer);
      boolean b = context.equals(vocab) || (context.contains(vocab) && !vocab.isEmpty());

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
      return   (context.contains(lcSearch));
    }
  }

  /**
   * @param value
   * @param isChineseCharacter
   * @return
   * @see #getClickableWords(String, TwoColumnExercisePanel.FieldType, List, boolean, boolean, boolean)
   * @see #getClickableWordsHighlight(String, String, TwoColumnExercisePanel.FieldType, List, boolean)
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

  public boolean isRTL(T exercise) {
    return isRTLContent(exercise.getForeignLanguage());
  }

  private boolean isRTLContent(String content) {
    return WordCountDirectionEstimator.get().estimateDirection(content) == HasDirection.Direction.RTL;
  }

  /**
   * @param dir
   * @param html           a token that can be clicked on to search on it
   * @param isContextMatch
   * @param id
   * @param isSimple
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
      boolean isSimple,
      TwoColumnExercisePanel.FieldType fieldType) {
    final IHighlightSegment highlightSegmentDiv = isSimple ?
        new SimpleHighlightSegment(html, id) :
        new HighlightSegment(id, html, dir);

    InlineHTML highlightSegment = highlightSegmentDiv.getClickable();
    if (fieldType == TwoColumnExercisePanel.FieldType.FL) {
      if (dir == HasDirection.Direction.RTL) {
        highlightSegment.addStyleName("bigflfont");
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
    if (fieldType == TwoColumnExercisePanel.FieldType.MEANING) highlightSegment.addStyleName("englishFont");

    if (searchToken != null) {
      showSearchMatch(dir, html, highlightSegment, searchToken);
    }

    boolean empty = removePunct(html).isEmpty();
    if (empty) {
      highlightSegmentDiv.setClickable(false);
    } else {
      highlightSegment.getElement().getStyle().setCursor(Style.Cursor.POINTER);
      highlightSegment.addClickHandler(clickEvent -> Scheduler.get().scheduleDeferred(() -> putTextInSearchBox(removePunct(html))));
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
  private void showSearchMatch(HasDirection.Direction dir, String html, InlineHTML w, String searchToken) {
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
    String s1 = html.replaceAll(GoodwaveExercisePanel.PUNCT_REGEX, " ").replaceAll("’", " ");
    String s2 = s1.split(GoodwaveExercisePanel.SPACE_REGEX)[0].toLowerCase();
    listContainer.searchBoxEntry(s2);
  }

  /**
   * Chinese punctuation marks, spanish punct marks
   * horizontal ellipsis...
   * reverse solidus
   *
   * @param t
   * @return
   */
  protected String removePunct(String t) {
    return t
        .replaceAll(GoodwaveExercisePanel.PUNCT_REGEX, "")
        .replaceAll("\\u00ED", "i")
        .replaceAll("[\\0022\\uFF01-\\uFF0F\\uFF1A-\\uFF1F\\u3002\\u003F\\u00BF\\u002E\\u002C\\u0021\\u20260\\u005C\\u2013]", "");
  }
}
