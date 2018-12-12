package mitll.langtest.client.scoring;

import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.dom.client.Style;
import mitll.langtest.client.banner.IListenView;
import mitll.langtest.client.banner.ListenViewHelper;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.shared.exercise.ClientExercise;
import mitll.langtest.shared.scoring.AlignmentOutput;

import java.util.Map;

/**
 * @param <T>
 * @see mitll.langtest.client.banner.ListenViewHelper#reallyGetTurnPanel
 */
public class TurnPanel<T extends ClientExercise> extends DialogExercisePanel<T> {
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

    else if (columns == ListenViewHelper.COLUMNS.LEFT) addStyleName("floatLeft");
  }

  protected void styleMe(DivWidget wrapper) {
    this.bubble = wrapper;
    wrapper.getElement().setId("bubble_" + getExID());
    wrapper.addStyleName("bubble");
    if (columns == ListenViewHelper.COLUMNS.LEFT) wrapper.addStyleName("leftbubble");
    else if (columns == ListenViewHelper.COLUMNS.RIGHT) wrapper.addStyleName("rightbubble");
    else if (columns == ListenViewHelper.COLUMNS.MIDDLE) wrapper.addStyleName("middlebubble");

    flClickableRow.getElement().getStyle().setOverflow(Style.Overflow.HIDDEN);
    addMarginStyle();
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
