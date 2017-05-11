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
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Created by go22670 on 3/23/17.
 */
public class ClickableWords<T extends CommonExercise> {
  private final Logger logger = Logger.getLogger("ClickableWords");
  private static final String CONTEXTMATCH = "contextmatch";

  private static final String STRONG = "strong";
  public static final double THRESHOLD = 0.3;

  private boolean isJapanese = false;

  private final T exercise;
  private boolean hasClickableAsian = false;
  private static final String MANDARIN = "Mandarin";
  private static final String JAPANESE = "Japanese";
  public static final String DEFAULT_SPEAKER = "Default Speaker";
  private final ListInterface listContainer;
  private WordBoundsFactory factory = new WordBoundsFactory();

  private static final boolean DEBUG = false;

  /**
   * @param listContainer
   * @param exercise
   * @param language
   */
  ClickableWords(ListInterface listContainer, T exercise, String language) {
    this.listContainer = listContainer;
    this.exercise = exercise;
    isJapanese = language.equalsIgnoreCase(JAPANESE);
    this.hasClickableAsian = language.equalsIgnoreCase(MANDARIN);// || language.equals(KOREAN) || isJapanese;
  }

  /**
   * Split the value into tokens, and make each one clickable - so we can search on it.
   *
   * @param value
   * @param clickables
   * @param isSimple
   * @param addRightMargin
   * @see TwoColumnExercisePanel#getEntry
   */
  DivWidget getClickableWords(String value,
                              boolean isFL,
                              boolean isTranslit,
                              boolean isMeaning,
                              List<IHighlightSegment> clickables,
                              boolean isSimple,
                              boolean addRightMargin) {
    boolean flLine = isFL || (isJapanese && isTranslit);
    boolean isChineseCharacter = flLine && hasClickableAsian;
    List<String> tokens = getTokens(value, flLine, isChineseCharacter);

    HasDirection.Direction dir = WordCountDirectionEstimator.get().estimateDirection(value);

    boolean addRight = (isSimple && !isChineseCharacter) || addRightMargin;
    return getClickableDiv(tokens, isSimple, isMeaning, addRight, dir, clickables);
  }

  @NotNull
  private DivWidget getClickableDiv(List<String> tokens, boolean isSimple, boolean isMeaning,
                                    boolean addRightMargin, HasDirection.Direction dir, List<IHighlightSegment> clickables) {
    List<IHighlightSegment> segmentsForTokens = getSegmentsForTokens(isMeaning, isSimple, tokens, dir);
    clickables.addAll(segmentsForTokens);


    return getClickableDivFromSegments(segmentsForTokens, addRightMargin);
  }

  private List<IHighlightSegment> getSegmentsForTokens(boolean isMeaning, boolean isSimple, List<String> tokens, HasDirection.Direction dir) {
    List<IHighlightSegment> segments = new ArrayList<>();
    int id = 0;
    for (String token : tokens) {
      IHighlightSegment segment = makeClickableText(isMeaning, dir, token, false, id++, isSimple);
      segments.add(segment);
    }
    return segments;
  }

  @NotNull
  public DivWidget getClickableDivFromSegments(List<IHighlightSegment> segmentsForTokens, boolean addRightMargin) {
    DivWidget horizontal = new DivWidget();
    horizontal.getElement().setId("clickableRow");
    horizontal.getElement().getStyle().setDisplay(Style.Display.INLINE_BLOCK);

    int i = 0;
    for (IHighlightSegment segment : segmentsForTokens) {
      horizontal.add(segment.asWidget());
      logger.info("adding token " + (i++) + " " + segment);
      if (addRightMargin) {
        segment.getClickable().addStyleName("rightFiveMargin");
      }
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
   * @param value
   * @param highlight
   * @param isFL
   * @param isTranslit
   * @param isMeaning
   * @param clickables
   * @param isSimple
   * @return
   * @see TwoColumnExercisePanel#getContext
   */
  DivWidget getClickableWordsHighlight(String value,
                                       String highlight,
                                       boolean isFL,
                                       boolean isTranslit,
                                       boolean isMeaning,
                                       List<IHighlightSegment> clickables,
                                       boolean isSimple) {
    DivWidget horizontal = new DivWidget();

    horizontal.getElement().setId("clickableWordsHightlightRow");
    boolean flLine = isFL || (isJapanese && isTranslit);
    boolean isChineseCharacter = flLine && hasClickableAsian;

    List<String> tokens = getTokens(value, flLine, isChineseCharacter);

    if (DEBUG) {
      logger.info("getClickableWordsHighlight " +
          "\n\tvalue     '" + value + "'" +
          "\n\thighlight '" + highlight + "'" +
          "\n\ttokens     " + tokens.size());
    }

    // if the highlight token is not in the display, skip over it -
    List<String> realHighlight = getMatchingHighlight(tokens, getTokens(highlight, flLine, isChineseCharacter));

    if (DEBUG) logger.info("getClickableWordsHighlight real " + realHighlight);

    Iterator<String> iterator = realHighlight.iterator();
    String toFind = iterator.hasNext() ? iterator.next() : null;

    HasDirection.Direction dir = WordCountDirectionEstimator.get().estimateDirection(value);

    int id = 0;
    for (String token : tokens) {
      boolean isMatch = toFind != null && isMatch(token, toFind);
      IHighlightSegment clickable = makeClickableText(isMeaning, dir, token, isMatch, id++, isSimple);
      clickables.add(clickable);
      Widget w = clickable.asWidget();
      w.addStyleName("rightFiveMargin");
      horizontal.add(w);
      //  match++;
      if (isMatch) {
        if (DEBUG) logger.info("getClickableWordsHighlight highlight '" + toFind + "' = '" + token + "'");
        toFind = iterator.hasNext() ? iterator.next() : null;
      }
//      else if (isMatch) {
//        logger.fine("getClickableWordsHighlight no highlight '" + toFind + "' vs '" + token + "' match at " + match);
//      }
    }

    horizontal.addStyleName("leftFiveMargin");
    return horizontal;
  }

  /**
   * @param tokens
   * @param highlightTokens
   * @return
   * @see #getClickableWords(String, boolean, boolean, boolean, List, boolean, boolean)
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
   * @param token
   * @param next
   * @return
   */
  private boolean isMatch(String token, String next) {
    if (next.isEmpty()) {
      return false;
    } else {
      String context = removePunct(token.toLowerCase());
      String vocab = removePunct(next.toLowerCase());
      // if (DEBUG) logger.info("context " + context + " token " + token);
      boolean b = context.equals(vocab) || (context.contains(vocab) && !vocab.isEmpty());

/*      if (b && DEBUG)
        logger.info("isMatch match '" + token + "' '" + next + "' context '" + context + "' vocab '" + vocab + "'");
     */
      return b;// && ((float) vocab.length() / (float) context.length()) > THRESHOLD);
    }
  }

/*  private String stripAccents(final String input) {
    if(input == null) {
      return null;
    }
    final Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");//$NON-NLS-1$
    final StringBuilder decomposed = new StringBuilder(Normalizer.normalize(input, Normalizer.Form.NFD));
    convertRemainingAccentCharacters(decomposed);
    // Note that this doesn't correctly remove ligatures...
    return pattern.matcher(decomposed).replaceAll(StringUtils.EMPTY);
  }

  private void convertRemainingAccentCharacters(StringBuilder decomposed) {
    for (int i = 0; i < decomposed.length(); i++) {
      if (decomposed.charAt(i) == '\u0141') {
        decomposed.deleteCharAt(i);
        decomposed.insert(i, 'L');
      } else if (decomposed.charAt(i) == '\u0142') {
        decomposed.deleteCharAt(i);
        decomposed.insert(i, 'l');
      }
    }
  }*/

  /**
   * Will reverse order of tokens if it's an RTL exercise...
   *
   * @param value
   * @param flLine
   * @param isChineseCharacter
   * @return
   */
  @NotNull
  private List<String> getTokens(String value, boolean flLine, boolean isChineseCharacter) {
    List<String> tokens = new ArrayList<>();
    if (isChineseCharacter) {
      for (int i = 0, n = value.length(); i < n; i++) {
        Character character = value.charAt(i);
        tokens.add(character.toString());
      }
    } else {
      tokens = new ArrayList<>(Arrays.asList(value.split(GoodwaveExercisePanel.SPACE_REGEX)));
      tokens = tokens.stream().filter(p -> !removePunct(p).isEmpty()).collect(Collectors.toList());
    }

    if (isRTL(exercise) && flLine) {
      logger.info("Reversing tokens " + tokens);
      Collections.reverse(tokens);
      logger.info("Reversing tokens now " + tokens);

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
   * @param isMeaning
   * @param dir
   * @param html           a token that can be clicked on to search on it
   * @param isContextMatch
   * @param id
   * @param isSimple
   * @return
   * @paramx chineseCharacter
   * @see #getClickableWords
   * @see #getClickableWordsHighlight
   */
  private IHighlightSegment makeClickableText(boolean isMeaning,
                                              HasDirection.Direction dir,
                                              final String html,
                                              //  boolean chineseCharacter,
                                              boolean isContextMatch,
                                              int id,
                                              boolean isSimple) {
    final IHighlightSegment highlightSegmentDiv = isSimple ?
        new SimpleHighlightSegment(html) :
        new HighlightSegment(id, html, dir);

    InlineHTML highlightSegment = highlightSegmentDiv.getClickable();
    highlightSegment.addStyleName("Instruction-data-with-wrap-keep-word");

    if (isContextMatch) highlightSegment.addStyleName(CONTEXTMATCH);
    if (isMeaning) highlightSegment.addStyleName("englishFont");
    // if (!chineseCharacter || isSimple) highlightSegment.addStyleName("rightFiveMargin");

    String searchToken = listContainer.getTypeAheadText().toLowerCase();
    if (isMatch(html, searchToken)) {
      showSearchMatch(dir, html, highlightSegment, searchToken);
    }

    boolean empty = removePunct(html).isEmpty();
    if (empty) {
      highlightSegmentDiv.setClickable(false);
    } else {
      highlightSegment.getElement().getStyle().setCursor(Style.Cursor.POINTER);
      highlightSegment.addClickHandler(clickEvent -> Scheduler.get().scheduleDeferred(() -> putTextInSearchBox(html)));
      highlightSegment.addMouseOverHandler(mouseOverEvent -> highlightSegment.addStyleName("underline"));
      highlightSegment.addMouseOutHandler(mouseOutEvent -> highlightSegment.removeStyleName("underline"));
    }

    return highlightSegmentDiv;
  }

  private void showSearchMatch(HasDirection.Direction dir, String html, InlineHTML w, String searchToken) {
    String[] toFind = new String[0];
    toFind[0] = searchToken;
    WordBounds wordBounds = factory.findNextWord(html.toLowerCase(), toFind, 0);

    if (wordBounds == null) {
      logger.info("makeClickableText can't find  '" + searchToken + "' in '" + html + "'");
      w.addStyleName("searchmatch");
    } else {
      //   logger.info("word bounds " + wordBounds + " for '" + searchToken + "' in '" + html+"'");
      w.setHTML(getSearchHighlightedHTML(html, wordBounds), dir);
    }
  }

  private SafeHtml getSearchHighlightedHTML(String html, WordBounds wordBounds) {
    List<String> parts = wordBounds.getTriple(html);
    SafeHtmlBuilder accum = new SafeHtmlBuilder();

    accum.appendEscaped(parts.get(0));
    accum.appendHtmlConstant("<" + STRONG + ">");
    accum.appendEscaped(parts.get(1));
    accum.appendHtmlConstant("</" + STRONG + ">");
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
        .replaceAll("[\\uFF01-\\uFF0F\\uFF1A-\\uFF1F\\u3002\\u003F\\u00BF\\u002E\\u002C\\u0021\\u20260\\u005C\\u2013]", "");
  }
}
