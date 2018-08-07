package mitll.langtest.client.scoring;

import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.dom.client.Style;
import mitll.langtest.client.banner.IListenView;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.client.scoring.DialogExercisePanel;
import mitll.langtest.shared.exercise.ClientExercise;
import mitll.langtest.shared.scoring.AlignmentOutput;

import java.util.Map;

public class TurnPanel<T extends ClientExercise> extends DialogExercisePanel<T> {
  //private ListenViewHelper listenViewHelper;
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
  //  this.listenViewHelper = listenViewHelper;
    this.isRight = isRight;
    getElement().getStyle().setOverflow(Style.Overflow.HIDDEN);
    getElement().getStyle().setClear(Style.Clear.BOTH);
    if (isRight) addStyleName("floatRight");
    else addStyleName("floatLeft");
  }

//    @NotNull
//    protected DivWidget doStyle(DivWidget flEntry) {
//       this.bubble = rightSide;
//      rightSide.getElement().setId("rightSideBubble_" + getExID());
//      rightSide.addStyleName("bubble");
//       //  styleMe(rightSide);
//      return rightSide;
//    }

  protected void styleMe(DivWidget wrapper) {
    this.bubble = wrapper;
    wrapper.getElement().setId("bubble_" + getExID());
    wrapper.addStyleName("bubble");

    //Style style = wrapper.getElement().getStyle();
    //   if (isRight) {
//      style.setFloat(Style.Float.RIGHT);
//      style.setTextAlign(Style.TextAlign.RIGHT);
//      style.setBackgroundColor(RIGHT_BKG_COLOR);
    wrapper.addStyleName(isRight ? "rightbubble" : "leftbubble");
    // } else {
    // wrapper.addStyleName("leftbubble");
//
//      style.setFloat(Style.Float.LEFT);
//      style.setTextAlign(Style.TextAlign.LEFT);
//      style.setBackgroundColor(LEFT_COLOR);
//    }

    flClickableRow.getElement().getStyle().setOverflow(Style.Overflow.HIDDEN);
    //  wrapper.addStyleName("bubble");
    addMarginStyle();

  }

  public void removeMarkCurrent() {
    //   logger.info("removeMarkCurrent on " + getExID());
    bubble.getElement().getStyle().setBorderColor("white");
  }

  public void markCurrent() {
    // logger.info("markCurrent on " + getExID());
    bubble.getElement().getStyle().setBorderColor(HIGHLIGHT_COLOR);
  }
}
