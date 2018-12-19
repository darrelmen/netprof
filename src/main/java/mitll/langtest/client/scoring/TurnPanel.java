package mitll.langtest.client.scoring;

import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.dialog.IListenView;
import mitll.langtest.client.dialog.ListenViewHelper;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.client.sound.AllHighlight;
import mitll.langtest.client.sound.AudioControl;
import mitll.langtest.client.sound.IHighlightSegment;
import mitll.langtest.shared.exercise.ClientExercise;
import mitll.langtest.shared.exercise.ExerciseAttribute;
import mitll.langtest.shared.instrumentation.TranscriptSegment;
import mitll.langtest.shared.scoring.AlignmentOutput;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Does left, right, or middle justify
 *
 * @param <T>
 * @see ListenViewHelper#reallyGetTurnPanel
 */
public class TurnPanel extends DialogExercisePanel<ClientExercise> {
  private final Logger logger = Logger.getLogger("TurnPanel");

  private static final String FLOAT_LEFT = "floatLeft";

  final ListenViewHelper.COLUMNS columns;
  private DivWidget bubble;
  private static final String HIGHLIGHT_COLOR = "green";

  /**
   * @param clientExercise
   * @param controller
   * @param listContainer
   * @param alignments
   * @param listenView
   * @param columns
   * @see ListenViewHelper#reallyGetTurnPanel
   */
  public TurnPanel(final ClientExercise clientExercise,
                   final ExerciseController controller,
                   final ListInterface<?, ?> listContainer,
                   Map<Integer, AlignmentOutput> alignments,
                   IListenView listenView,
                   ListenViewHelper.COLUMNS columns) {
    super(clientExercise, controller, listContainer, alignments, listenView);
    this.columns = columns;

    Style style = getElement().getStyle();
    style.setOverflow(Style.Overflow.HIDDEN);
    style.setClear(Style.Clear.BOTH);

    if (columns == ListenViewHelper.COLUMNS.RIGHT) addStyleName("floatRight");
    else if (columns == ListenViewHelper.COLUMNS.LEFT) addStyleName(FLOAT_LEFT);
  }

  @Override
  boolean shouldShowPhones() {
    List<ExerciseAttribute> speaker = exercise.getAttributes().stream().filter(exerciseAttribute -> exerciseAttribute.getProperty().equals("SPEAKER")).collect(Collectors.toList());
    boolean hasEnglishAttr = exercise.hasEnglishAttr();
    boolean b = !hasEnglishAttr && isInterpreterTurn(speaker);
//    if (b)
//      logger.info("ex " + exercise.getID() + " " + exercise.getEnglish() + " " + exercise.getForeignLanguage() + " Got show phones " + b);
    return b;
  }

  private boolean isInterpreterTurn(List<ExerciseAttribute> speaker) {
    boolean isInterpreterTurn = false;

    if (!speaker.isEmpty()) {
      ExerciseAttribute exerciseAttribute = speaker.get(0);
      isInterpreterTurn = exerciseAttribute.getValue().equals("I");
    }
    return isInterpreterTurn;
  }

  @Override
  @NotNull
  protected DivWidget getPhoneDivBelowWord(TranscriptSegment wordSegment,
                                           List<TranscriptSegment> phonesInWord,
                                           AudioControl audioControl,
                                           TreeMap<TranscriptSegment, IHighlightSegment> phoneMap, boolean simpleLayout) {
    return new WordTable().getPhoneDivBelowWord(audioControl, phoneMap, phonesInWord, true, wordSegment, false);
  }


  protected void styleMe(DivWidget wrapper) {
    this.bubble = wrapper;
    wrapper.getElement().setId("bubble_" + getExID());
    wrapper.addStyleName("bubble");

    // decide on left right or middle justify
    {
      if (columns == ListenViewHelper.COLUMNS.LEFT) wrapper.addStyleName("leftbubble");
      else if (columns == ListenViewHelper.COLUMNS.RIGHT) wrapper.addStyleName("rightbubble");
      else if (columns == ListenViewHelper.COLUMNS.MIDDLE) {
        wrapper.addStyleName("middlebubble");
        wrapper.getElement().getStyle().setTextAlign(Style.TextAlign.CENTER);
      }
    }

    flClickableRow.getElement().getStyle().setOverflow(Style.Overflow.HIDDEN);
    addMarginStyle();
  }

  @Override
  protected void addFloatLeft(Widget w) {
    if (shouldAddFloatLeft()) {
      w.addStyleName(FLOAT_LEFT);
    }
  }

  @Override
  protected boolean shouldAddFloatLeft() {
    return (columns != ListenViewHelper.COLUMNS.MIDDLE);
  }

  @NotNull
  protected AllHighlight getAllHighlight(Collection<IHighlightSegment> flclickables) {
    return new AllHighlight(flclickables, columns != ListenViewHelper.COLUMNS.MIDDLE);
  }

  public void removeMarkCurrent() {
    setBorderColor("white");
  }

  public void markCurrent() {
    setBorderColor(HIGHLIGHT_COLOR);
  }

  public boolean hasCurrentMark() {
    return bubble.getElement().getStyle().getBorderColor().equalsIgnoreCase(HIGHLIGHT_COLOR);
  }

  private void setBorderColor(String white) {
    bubble.getElement().getStyle().setBorderColor(white);
  }
}
