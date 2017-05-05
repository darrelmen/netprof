package mitll.langtest.client.scoring;

import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Style;
import com.google.gwt.i18n.client.HasDirection;
import com.google.gwt.i18n.shared.WordCountDirectionEstimator;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.client.ui.InlineHTML;
import mitll.langtest.client.custom.dialog.WordBounds;
import mitll.langtest.client.custom.dialog.WordBoundsFactory;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.client.sound.HighlightSegment;
import mitll.langtest.client.sound.IHighlightSegment;
import mitll.langtest.shared.exercise.CommonExercise;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.logging.Logger;

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
  private boolean hasClickable = false;
  private static final String MANDARIN = "Mandarin";
  private static final String KOREAN = "Korean";
  private static final String JAPANESE = "Japanese";
  public static final String DEFAULT_SPEAKER = "Default Speaker";
  private final ListInterface listContainer;
  private WordBoundsFactory factory = new WordBoundsFactory();

  /**
   * @param listContainer
   * @param exercise
   * @param language
   */
  ClickableWords(ListInterface listContainer, T exercise, String language) {
    this.listContainer = listContainer;
    this.exercise = exercise;
    isJapanese = language.equalsIgnoreCase(JAPANESE);
    this.hasClickable = language.equalsIgnoreCase(MANDARIN) || language.equals(KOREAN) || isJapanese;
  }

  /**
   * @param value
   * @param clickables
   * @see TwoColumnExercisePanel#getEntry
   */
  DivWidget getClickableWords(String value,
                              boolean isFL,
                              boolean isTranslit,
                              boolean isMeaning,
                              List<IHighlightSegment> clickables) {
    DivWidget horizontal = new DivWidget();
    horizontal.getElement().getStyle().setDisplay(Style.Display.INLINE_BLOCK);

    boolean flLine = isFL || (isJapanese && isTranslit);
    boolean isChineseCharacter = flLine && hasClickable;
    List<String> tokens = getTokens(value, flLine, isChineseCharacter);

    HasDirection.Direction dir = WordCountDirectionEstimator.get().estimateDirection(value);
    for (String token : tokens) {
      HighlightSegment w = makeClickableText(isMeaning, dir, token, isChineseCharacter, false);
      clickables.add(w);
      horizontal.add(w);
    }

    horizontal.addStyleName("leftFiveMargin");

    return horizontal;
  }


  /**
   * So we want to highlight the vocab item in the context sentence.
   * This is separate from marking which tokens are matches to a search token.
   *
   * @param value
   * @param highlight
   * @param isFL
   * @param isTranslit
   * @param isMeaning
   * @param clickables
   * @return
   * @see TwoColumnExercisePanel#getContext
   */
  DivWidget getClickableWordsHighlight(String value,
                                       String highlight,
                                       boolean isFL,
                                       boolean isTranslit,
                                       boolean isMeaning,
                                       List<IHighlightSegment> clickables) {
    DivWidget horizontal = new DivWidget();
    horizontal.getElement().getStyle().setDisplay(Style.Display.INLINE_BLOCK);

    boolean flLine = isFL || (isJapanese && isTranslit);
    boolean isChineseCharacter = flLine && hasClickable;

    List<String> tokens = getTokens(value, flLine, isChineseCharacter);

    // if the highlight token is not in the display, skip over it -
    List<String> realHighlight = getMatchingHighlight(tokens, getTokens(highlight, flLine, isChineseCharacter));
//    logger.info("getClickableWordsHighlight real " + realHighlight);

    Iterator<String> iterator = realHighlight.iterator();
    String toFind = iterator.hasNext() ? iterator.next() : null;

    HasDirection.Direction dir = WordCountDirectionEstimator.get().estimateDirection(value);
    //  int match = 0;
    for (String token : tokens) {
      boolean isMatch = toFind != null && isMatch(token, toFind);
      HighlightSegment clickable = makeClickableText(isMeaning, dir, token, isChineseCharacter, isMatch);
      clickables.add(clickable);
      horizontal.add(clickable);
      //  match++;
      if (isMatch) {
        //  logger.info("getClickableWordsHighlight highlight '" + toFind + "' = '" + token + "' at " + match);
        toFind = iterator.hasNext() ? iterator.next() : null;
      }
//      else if (isMatch) {
//        logger.fine("getClickableWordsHighlight no highlight '" + toFind + "' vs '" + token + "' match at " + match);
//      }
    }

    horizontal.addStyleName("leftFiveMargin");
    return horizontal;
  }

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

  private boolean isMatch(String token, String next) {
    if (next.isEmpty()) {
      return false;
    } else {
      String context = removePunct(token.toLowerCase());
      String vocab = removePunct(next.toLowerCase());
      boolean b = context.equals(vocab) || (context.contains(vocab) && !vocab.isEmpty());
      // if (b) logger.info("match '" + token + "' '" + next + "' context '" + context + "' vocab '" + vocab + "'");
      return b;// && ((float) vocab.length() / (float) context.length()) > THRESHOLD);
    }
  }

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
    }

    if (isRTL(exercise) && flLine) {
      Collections.reverse(tokens);
    }
    return tokens;
  }

  private boolean isRTL(T exercise) {
    return isRTLContent(exercise.getForeignLanguage());
  }

  private boolean isRTLContent(String content) {
    return WordCountDirectionEstimator.get().estimateDirection(content) == HasDirection.Direction.RTL;
  }


  /**
   * @param isMeaning
   * @param dir
   * @param html             a token that can be clicked on to search on it
   * @param chineseCharacter
   * @param isContextMatch
   * @return
   * @see #getClickableWordsHighlight
   */
  private HighlightSegment makeClickableText(boolean isMeaning,
                                             HasDirection.Direction dir,
                                             final String html,
                                             boolean chineseCharacter,
                                             boolean isContextMatch) {
    final HighlightSegment highlightSegment = new HighlightSegment(html, dir);
    highlightSegment.addStyleName("Instruction-data-with-wrap-keep-word");

    if (isContextMatch) highlightSegment.addStyleName(CONTEXTMATCH);
    if (isMeaning) highlightSegment.addStyleName("englishFont");
    if (!chineseCharacter) highlightSegment.addStyleName("rightFiveMargin");


    String searchToken = listContainer.getTypeAheadText().toLowerCase();
    if (isMatch(html, searchToken)) {
      showSearchMatch(dir, html, highlightSegment, searchToken);
    }

    boolean empty = removePunct(html).isEmpty();
    if (empty) {
      highlightSegment.setClickable(false);
    } else {
      highlightSegment.getElement().getStyle().setCursor(Style.Cursor.POINTER);
      highlightSegment.addClickHandler(clickEvent -> Scheduler.get().scheduleDeferred(() -> putTextInSearchBox(html)));
      highlightSegment.addMouseOverHandler(mouseOverEvent -> highlightSegment.addStyleName("underline"));
      highlightSegment.addMouseOutHandler(mouseOutEvent -> highlightSegment.removeStyleName("underline"));
    }

    return highlightSegment;
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
   * @param t
   * @return
   */
  protected String removePunct(String t) {
    return t
        .replaceAll(GoodwaveExercisePanel.PUNCT_REGEX, "")
        .replaceAll("[\\p{M}\\uFF01-\\uFF0F\\uFF1A-\\uFF1F\\u3002\\u003F\\u00BF\\u002E\\u002C\\u0021\\u20260\\u005C\\u2013]", "");
  }
}
