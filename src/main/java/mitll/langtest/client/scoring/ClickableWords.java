package mitll.langtest.client.scoring;

import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.i18n.client.HasDirection;
import com.google.gwt.i18n.shared.WordCountDirectionEstimator;
import com.google.gwt.user.client.ui.InlineHTML;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.custom.exercise.CommentNPFExercise;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.shared.exercise.CommonExercise;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Created by go22670 on 3/23/17.
 */
public class ClickableWords<T extends CommonExercise> {
  private boolean isJapanese = false;

  private final T exercise;
  private boolean hasClickable = false;
  private static final String MANDARIN = "Mandarin";
  private static final String KOREAN = "Korean";
  private static final String JAPANESE = "Japanese";
  public static final String DEFAULT_SPEAKER = "Default Speaker";
  private static final String MEANING = "Meaning";
  private final ListInterface listContainer;

  public ClickableWords(ListInterface listContainer, T exercise, String language) {
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
  public DivWidget getClickableWords(String value,
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

  public DivWidget getClickableWordsHighlight(String value,
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

    Iterator<String> iterator = highlightTokens.iterator();
    String current = iterator.next();

    HasDirection.Direction dir = WordCountDirectionEstimator.get().estimateDirection(value);
    for (String token : tokens) {
      boolean isMatch = isMatch(token, current);

      if (isMatch && iterator.hasNext()) {
        current = iterator.next();
      }
      horizontal.add(makeClickableText(isMeaning, dir, token, isChineseCharacter, isMatch));
    }

    horizontal.addStyleName("leftFiveMargin");

    return horizontal;
  }

  private boolean isMatch(String token, String next) {
    String context = removePunct(token.toLowerCase());
    String vocab = removePunct(next.toLowerCase());
    return context.equals(vocab) || (context.startsWith(vocab) && ((float)vocab.length()/(float)context.length()) > 0.5);
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
      tokens = Arrays.asList(value.split(CommentNPFExercise.SPACE_REGEX));
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
    String toShow = addStyle ? "<u>" + html + "</u>" : html;
    final InlineHTML w = new InlineHTML(toShow, dir);

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
    return t.replaceAll(CommentNPFExercise.PUNCT_REGEX, "");
  }

}
