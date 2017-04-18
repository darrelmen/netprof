package mitll.langtest.client.scoring;

import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.i18n.client.HasDirection;
import com.google.gwt.i18n.shared.WordCountDirectionEstimator;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.annotations.IsSafeHtml;
import com.google.gwt.user.client.ui.InlineHTML;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.custom.TooltipHelper;
import mitll.langtest.client.custom.dialog.WordBounds;
import mitll.langtest.client.custom.dialog.WordBoundsFactory;
import mitll.langtest.client.custom.exercise.CommentNPFExercise;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.client.services.ExerciseServiceAsync;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.ExerciseAnnotation;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.logging.Logger;

/**
 * Created by go22670 on 3/23/17.
 */
public class ClickableWords<T extends CommonExercise> {
  public static final String CONTEXTMATCH = "contextmatch";
  private final Logger logger = Logger.getLogger("ClickableWords");

  public static final double THRESHOLD = 0.3;

  private boolean isJapanese = false;

  private final T exercise;
  private boolean hasClickable = false;
  private static final String MANDARIN = "Mandarin";
  private static final String KOREAN = "Korean";
  private static final String JAPANESE = "Japanese";
  public static final String DEFAULT_SPEAKER = "Default Speaker";
  private final ListInterface listContainer;

  /**
   * @param listContainer
   * @param exercise
   * @param language
   * @param exerciseServiceAsync
   */
  ClickableWords(ListInterface listContainer, T exercise, String language, ExerciseServiceAsync exerciseServiceAsync) {
    this.listContainer = listContainer;
    this.exercise = exercise;
    isJapanese = language.equalsIgnoreCase(JAPANESE);
    this.hasClickable = language.equalsIgnoreCase(MANDARIN) || language.equals(KOREAN) || isJapanese;
  }

  /**
   * @paramx label
   * @paramx value
   * @paramx nameValueRow
   * @see TwoColumnExercisePanel#getEntry(String, String, ExerciseAnnotation, boolean, boolean, boolean, boolean)
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

    // if the highlight token is not in the display, skip over it -
    List<String> realHighlight = getMatchingHighlight(tokens,
        getTokens(highlight, flLine, isChineseCharacter));

//    logger.info("getClickableWordsHighlight real " + realHighlight);

    Iterator<String> iterator = realHighlight.iterator();
    String toFind = iterator.hasNext() ? iterator.next() : null;

    HasDirection.Direction dir = WordCountDirectionEstimator.get().estimateDirection(value);
    int match = 0;
    for (String token : tokens) {
      boolean isMatch = toFind != null && isMatch(token, toFind);
      horizontal.add(makeClickableText(isMeaning, dir, token, isChineseCharacter, isMatch));
      match++;

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

  private WordBoundsFactory factory = new WordBoundsFactory();
  private static final String STRONG = "strong";

  /**
   * @param isMeaning
   * @param dir
   * @param html
   * @param chineseCharacter
   * @param addStyle
   * @return
   */
  private InlineHTML makeClickableText(boolean isMeaning,
                                       HasDirection.Direction dir,
                                       final String html,
                                       boolean chineseCharacter,
                                       boolean addStyle) {
    final InlineHTML w = new InlineHTML(html, dir);
    //final MyClickable w = new MyClickable(toShow, dir);

    if (addStyle) w.addStyleName(CONTEXTMATCH);

    //String typeAheadText = listContainer.getTypeAheadText().toLowerCase();
    String typeAheadText = listContainer.getTypeAheadText();
    String searchToken = typeAheadText.toLowerCase();
    if (isMatch(html, searchToken)) {//html.toLowerCase().contains(typeAheadText) && ((float)typeAheadText.length()/(float)html.length()) > THRESHOLD) {
      String[] toFind = new String[0];
      toFind[0] = searchToken;

      WordBounds wordBounds = factory.findNextWord(html.toLowerCase(), toFind, 0);

      if (wordBounds == null) {
        logger.info("can't find  '" + searchToken + "' in '" + html + "'");
        w.addStyleName("searchmatch");
      } else {
        //   logger.info("word bounds " + wordBounds + " for '" + searchToken + "' in '" + html+"'");
        List<String> parts = wordBounds.getTriple(html);
        SafeHtmlBuilder accum = new SafeHtmlBuilder();

        accum.appendEscaped(parts.get(0));
        accum.appendHtmlConstant("<" + STRONG + ">");
        accum.appendEscaped(parts.get(1));
        accum.appendHtmlConstant("</" + STRONG + ">");
        accum.appendEscaped(parts.get(2));

        w.setHTML(accum.toSafeHtml(), dir);
      }
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
              String s2 = s1.split(CommentNPFExercise.SPACE_REGEX)[0].toLowerCase();
              listContainer.searchBoxEntry(s2);
            }
          });
        }
      });
      w.addMouseOverHandler(mouseOverEvent -> w.addStyleName("underline"));
      w.addMouseOutHandler(mouseOutEvent -> w.removeStyleName("underline"));
    }

    w.addStyleName("Instruction-data-with-wrap-keep-word");
    if (isMeaning) {
      w.addStyleName("englishFont");
    }
    if (!chineseCharacter) w.addStyleName("rightFiveMargin");

    return w;
  }

  private static class MyClickable extends InlineHTML {
    private boolean has = false;

    public MyClickable(@IsSafeHtml String html, Direction dir) {
      super(html, dir);
    }

    public void setHas(boolean val) {
      this.has = val;
    }

    public boolean isHas() {
      return has;
    }
  }

  /*  private void addTool(String toShow, boolean isFL, Widget w) {
      exerciseServiceAsync.getExerciseIds(new ExerciseListRequest().setPrefix(toShow),
          new AsyncCallback<ExerciseListWrapper<CommonShell>>() {
            @Override
            public void onFailure(Throwable caught) {

            }

            @Override
            public void onSuccess(ExerciseListWrapper<CommonShell> result) {
              if (!result.getExercises().isEmpty()) {
                CommonShell next = result.getExercises().iterator().next();
                if (isFL) {
                  logger.info("adding " + next.getEnglish());
                  addTooltip(next.getEnglish(), w);
                }
              }
            }
          });
    }*/
  private void addTooltip(String value, Widget span) {
    new TooltipHelper().addTooltip(span, value);
  }

  protected String removePunct(String t) {
    return t.replaceAll(CommentNPFExercise.PUNCT_REGEX, "").replaceAll("\\p{M}", "");
  }

}
