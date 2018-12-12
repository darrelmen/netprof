package mitll.langtest.client.scoring;

import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.banner.IListenView;
import mitll.langtest.client.banner.ListenViewHelper;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.client.sound.AllHighlight;
import mitll.langtest.client.sound.IHighlightSegment;
import mitll.langtest.shared.exercise.ClientExercise;
import mitll.langtest.shared.scoring.AlignmentOutput;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Does left, right, or middle justify
 * @param <T>
 * @see mitll.langtest.client.banner.ListenViewHelper#reallyGetTurnPanel
 */
public class TurnPanel<T extends ClientExercise> extends DialogExercisePanel<T> {
  private static final String FLOAT_LEFT = "floatLeft";
  //  private final Logger logger = Logger.getLogger("TurnPanel");

  final ListenViewHelper.COLUMNS columns;
  private DivWidget bubble;
  private static final String HIGHLIGHT_COLOR = "green";

  public TurnPanel(final T clientExercise,
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
