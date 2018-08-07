package mitll.langtest.client.scoring;

import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.dom.client.Style;
import mitll.langtest.client.banner.IListenView;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.shared.exercise.ClientExercise;
import mitll.langtest.shared.scoring.AlignmentOutput;

import java.util.Map;

public class TurnPanel<T extends ClientExercise> extends DialogExercisePanel<T> {
  protected final boolean isRight;
  private DivWidget bubble;
  private static final String HIGHLIGHT_COLOR = "green";

  public TurnPanel(final T clientExercise,
                   final ExerciseController controller,
                   final ListInterface<?, ?> listContainer,
                   Map<Integer, AlignmentOutput> alignments,
                   IListenView listenView,
                   boolean isRight) {
    super(clientExercise, controller, listContainer, alignments, listenView);
    this.isRight = isRight;
    getElement().getStyle().setOverflow(Style.Overflow.HIDDEN);
    getElement().getStyle().setClear(Style.Clear.BOTH);
    if (isRight) addStyleName("floatRight");
    else addStyleName("floatLeft");
  }

  protected void styleMe(DivWidget wrapper) {
    this.bubble = wrapper;
    wrapper.getElement().setId("bubble_" + getExID());
    wrapper.addStyleName("bubble");
    wrapper.addStyleName(isRight ? "rightbubble" : "leftbubble");
    flClickableRow.getElement().getStyle().setOverflow(Style.Overflow.HIDDEN);
    addMarginStyle();
  }

  public void removeMarkCurrent() {
    setBorderColor("white");
  }

  public void markCurrent() {
    setBorderColor(HIGHLIGHT_COLOR);
  }

  private void setBorderColor(String white) {
    bubble.getElement().getStyle().setBorderColor(white);
  }
}
