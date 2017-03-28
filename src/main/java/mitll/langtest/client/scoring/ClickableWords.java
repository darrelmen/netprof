package mitll.langtest.client.scoring;

import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.i18n.client.HasDirection;
import com.google.gwt.i18n.shared.WordCountDirectionEstimator;
import com.google.gwt.user.client.ui.InlineHTML;
import mitll.langtest.client.custom.exercise.CommentNPFExercise;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.shared.exercise.CommonExercise;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.logging.Logger;

/**
 * Created by go22670 on 3/23/17.
 */
public class ClickableWords<T extends CommonExercise> {
  public static final double THRESHOLD = 0.3;
  private final Logger logger = Logger.getLogger("ClickableWords");

  private boolean isJapanese = false;

  private final T exercise;
  private boolean hasClickable = false;
  private static final String MANDARIN = "Mandarin";
  private static final String KOREAN = "Korean";
  private static final String JAPANESE = "Japanese";
  public static final String DEFAULT_SPEAKER = "Default Speaker";
  //private static final String MEANING = "Meaning";
  private final ListInterface listContainer;

  ClickableWords(ListInterface listContainer, T exercise, String language) {
    this.listContainer = listContainer;
    this.exercise = exercise;
    isJapanese = language.equalsIgnoreCase(JAPANESE);
    this.hasClickable = language.equalsIgnoreCase(MANDARIN) || language.equals(KOREAN) || isJapanese;
  }

  /**
   * @paramx label
   * @paramx value
   * @paramx nameValueRow
   * @seex #getContentWidget(String, String, boolean)
   */
  DivWidget getClickableWords(String value,
                              boolean isFL,
                              boolean isTranslit,
                              boolean isMeaning) {
    DivWidget horizontal = new DivWidget();
    horizontal.getElement().getStyle().setDisplay(Style.Display.INLINE_BLOCK);

    boolean flLine = isFL || (isJapanese && isTranslit);
    boolean isChineseCharacter = flLine && hasClickable;
    List<String> tokens = getTokens(value, flLine, isChineseCharacter);

    HasDirection.Direction dir = WordCountDirectionEstimator.get().estimateDirection(value);
    for (String token : tokens) {
      horizontal.add(makeClickableText(isMeaning, dir, token, isChineseCharacter, false));
    }

    horizontal.addStyleName("leftFiveMargin");

    return horizontal;
  }

  DivWidget getClickableWordsHighlight(String value,
                                       String highlight,
                                       boolean isFL,
                                       boolean isTranslit,
                                       boolean isMeaning) {
    DivWidget horizontal = new DivWidget();
    horizontal.getElement().getStyle().setDisplay(Style.Display.INLINE_BLOCK);

    boolean flLine = isFL || (isJapanese && isTranslit);
    boolean isChineseCharacter = flLine && hasClickable;

    List<String> tokens = getTokens(value, flLine, isChineseCharacter);

    List<String> highlightTokens = getTokens(highlight, flLine, isChineseCharacter);

    // if the highlight token is not in the display, skip over it -
    List<String> realHighlight = new ArrayList<>();

    Iterator<String> hIter = highlightTokens.iterator();

    int index = 0;

    for (; hIter.hasNext(); ) {
      String toFind = hIter.next();

      for (int i = index; i < tokens.size(); i++) {
        String next = tokens.get(i);
        if (isMatch(next,toFind)) {
          index = i;
          realHighlight.add(toFind);
          //logger.info("- found '" + toFind + "' = '" +next+ "' at " + index);

          break;
        }
      }
    }

//    logger.info("real " + realHighlight);

    Iterator<String> iterator = realHighlight.iterator();
    String toFind = iterator.hasNext() ? iterator.next() : null;

    HasDirection.Direction dir = WordCountDirectionEstimator.get().estimateDirection(value);
    for (String token : tokens) {
      boolean isMatch = toFind != null && isMatch(token, toFind);

      if (isMatch && iterator.hasNext()) {
        toFind = iterator.next();
       // logger.info("- highlight '" + toFind + "' = '" +token+ "'");
      }
      else {
       // logger.fine("-  no highlight '" + toFind + "' vs '" +token+ "'");
      }

      horizontal.add(makeClickableText(isMeaning, dir, token, isChineseCharacter, isMatch));
    }

    horizontal.addStyleName("leftFiveMargin");

    return horizontal;
  }

  private boolean isMatch(String token, String next) {
    String context = removePunct(token.toLowerCase());
    String vocab   = removePunct(next.toLowerCase());
    return context.equals(vocab) || (context.startsWith(vocab) && !vocab.isEmpty());// && ((float) vocab.length() / (float) context.length()) > THRESHOLD);
  }

/*  private boolean isHardMatch(String token, String next) {
    String context = removePunct(token.toLowerCase());
    String vocab   = removePunct(next.toLowerCase());
    return context.equals(vocab);
  }*/

  @NotNull
  private List<String> getTokens(String value, boolean flLine, boolean isChineseCharacter) {
    List<String> tokens = new ArrayList<>();
    if (isChineseCharacter) {
      for (int i = 0, n = value.length(); i < n; i++) {
        char c = value.charAt(i);
        Character character = c;
        final String html = character.toString();
        tokens.add(html);
      }
    } else {
      tokens = new ArrayList<>(Arrays.asList(value.split(CommentNPFExercise.SPACE_REGEX)));
    }

    if (isRTL(exercise) && flLine) {
      Collections.reverse(tokens);
    }
    return tokens;
  }

  protected boolean isRTL(T exercise) {
    return isRTLContent(exercise.getForeignLanguage());
  }


  private boolean isRTLContent(String content) {
    return WordCountDirectionEstimator.get().estimateDirection(content) == HasDirection.Direction.RTL;
  }

  private InlineHTML makeClickableText(boolean isMeaning,
                                       HasDirection.Direction dir,
                                       final String html, boolean chineseCharacter,
                                       boolean addStyle) {
    String toShow = html;//addStyle ? "<u>" + html + "</u>" : html;
    final InlineHTML w = new InlineHTML(toShow, dir);

    if (addStyle) w.addStyleName("contextmatch");

    String typeAheadText = listContainer.getTypeAheadText().toLowerCase();
    if (isMatch(html, typeAheadText)) {//html.toLowerCase().contains(typeAheadText) && ((float)typeAheadText.length()/(float)html.length()) > THRESHOLD) {
      w.addStyleName("searchmatch");
    }

    String noPunct = removePunct(html);
    if (!noPunct.isEmpty()) {
      w.getElement().getStyle().setCursor(Style.Cursor.POINTER);
      w.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent clickEvent) {
          Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
            public void execute() {
              String s1 = html.replaceAll(CommentNPFExercise.PUNCT_REGEX, " ").replaceAll("’", " ");

          //    logger.info("from " + html);
          //    logger.info("to   " + s1);

              String s2 = s1.split(CommentNPFExercise.SPACE_REGEX)[0].toLowerCase();

            //  logger.info("finally   " + s2);

              listContainer.searchBoxEntry(s2);
            }
          });
        }
      });
      w.addMouseOverHandler(new MouseOverHandler() {
        @Override
        public void onMouseOver(MouseOverEvent mouseOverEvent) {
          w.addStyleName("underline");
        }
      });
      w.addMouseOutHandler(new MouseOutHandler() {
        @Override
        public void onMouseOut(MouseOutEvent mouseOutEvent) {
          w.removeStyleName("underline");
        }
      });
    }

    w.addStyleName("Instruction-data-with-wrap-keep-word");
    if (isMeaning) {
      w.addStyleName("englishFont");
    }
    if (!chineseCharacter) w.addStyleName("rightFiveMargin");

    return w;
  }

  protected String removePunct(String t) {
    return t.replaceAll(CommentNPFExercise.PUNCT_REGEX, "").replaceAll("\\p{M}", "");
  }

}
