package mitll.langtest.client.flashcard;

import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.shared.exercise.CommonAnnotatable;

class HidePolyglotPanel extends PolyglotPracticePanel {
  private static final String ARROW_KEY_TIP = "<i><b>Space</b> to record. <b>Arrow keys</b> to advance or go back.</i>";

  public HidePolyglotPanel(PolyglotFlashcardContainer statsFlashcardFactory,
                           ControlState controlState,
                           ExerciseController controller,
                           MySoundFeedback soundFeedback,
                           PolyglotDialog.PROMPT_CHOICE prompt,
                           CommonAnnotatable e,
                           StickyState stickyState,
                           ListInterface exerciseListToUse) {
    super(statsFlashcardFactory, controlState, controller, soundFeedback, prompt, e, stickyState, exerciseListToUse);
  }

  Panel getRightColumn(final ControlState controlState) {
    Panel rightColumn = new DivWidget();
    rightColumn.addStyleName("leftTenMargin");
    rightColumn.add(getKeyBinding());
    return rightColumn;
  }

  /**
   * no comment
   * @param firstRow
   */

  @Override
  void addCommentBox(DivWidget firstRow) {

  }

  String getKeyBindings() {
    return ARROW_KEY_TIP;
  }
}
