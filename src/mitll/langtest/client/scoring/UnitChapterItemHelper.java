package mitll.langtest.client.scoring;

import com.github.gwtbootstrap.client.ui.Heading;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.shared.exercise.CommonShell;

import java.util.Collection;
import java.util.logging.Logger;

/**
 * Created by go22670 on 3/11/16.
 */
public class UnitChapterItemHelper<T extends CommonShell> {
  private Logger logger = Logger.getLogger("UnitChapterItemHelper");

  /**
   * @see mitll.langtest.client.exercise.WaveformExercisePanel#addInstructions
   */
  public static final int HEADING_FOR_UNIT_LESSON = 4;
  public static final String CORRECT = "correct";
  public static final String ITEM = "Item";

  Collection<String> typeOrder;

  public UnitChapterItemHelper(Collection<String> typeOrder) {
    this.typeOrder = typeOrder;
  }

  public Panel addUnitChapterItem(T exercise, Panel vp) {
    Widget itemHeader = getItemHeader(exercise);
    if (exercise.getUnitToValue().isEmpty()) {
      return null;
    } else {
      Panel unitLessonForExercise = getUnitLessonForExercise(exercise);
      unitLessonForExercise.add(itemHeader);
      vp.add(unitLessonForExercise);
      return unitLessonForExercise;
    }
  }

  /**
   * @param e
   * @return
   * @see GoodwaveExercisePanel#getQuestionContent
   */
  private Widget getItemHeader(T e) {
   // logger.info("got " + e + " and " + e.getDisplayID());

    Heading w = new Heading(HEADING_FOR_UNIT_LESSON, ITEM, e.getDisplayID());
    w.getElement().setId("ItemHeading");
    return w;
  }

  /**
   * Show unit and chapter info for every item.
   *
   * @return
   * @see GoodwaveExercisePanel#getQuestionContent
   */
  private Panel getUnitLessonForExercise(T exercise) {
    Panel flow = new HorizontalPanel();
    flow.getElement().setId("getUnitLessonForExercise_unitLesson");
    flow.addStyleName("leftFiveMargin");
    //logger.info("getUnitLessonForExercise " + exercise + " unit value " +exercise.getUnitToValue());

    for (String type : typeOrder) {
      Heading child = new Heading(HEADING_FOR_UNIT_LESSON, type, exercise.getUnitToValue().get(type));
      child.addStyleName("rightFiveMargin");
      flow.add(child);
    }
    return flow;
  }
}
