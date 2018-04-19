package mitll.langtest.client.flashcard;

import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.shared.exercise.CommonAnnotatable;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.CommonShell;

 class HidePolyglotPanel<L extends CommonShell, T extends CommonExercise> extends PolyglotPracticePanel<L, T> {
  /**
   * @param statsFlashcardFactory
   * @param controlState
   * @param controller
   * @param soundFeedback
   * @param prompt
   * @param e
   * @param stickyState
   * @param exerciseListToUse
   * @see HidePolyglotFactory#getCurrentFlashcard
   */
   HidePolyglotPanel(PolyglotFlashcardContainer statsFlashcardFactory,
                           ControlState controlState,
                           ExerciseController controller,
                           MySoundFeedback soundFeedback,
                           PolyglotDialog.PROMPT_CHOICE prompt,
                           CommonAnnotatable e,
                           StickyState stickyState,
                           ListInterface<L, T> exerciseListToUse) {
    super(statsFlashcardFactory, controlState, controller, soundFeedback, prompt, e, stickyState, exerciseListToUse);
  }

  Panel getRightColumn(final ControlState controlState) {
    Panel rightColumn = new DivWidget();
    rightColumn.addStyleName("leftTenMargin");

    if (controller.getUserState().isAdmin()) {
      rightColumn.add(getAudioGroup(controlState));
      addControlsBelowAudio(controlState, rightColumn);
    }
    rightColumn.add(getKeyBinding());
    return rightColumn;
  }

 /*  @Override
   protected Panel getThreePartContent(ControlState controlState, Panel contentMiddle, DivWidget belowDiv, DivWidget lowestRow) {
     Panel threePartContent = super.getThreePartContent(controlState, contentMiddle, belowDiv, lowestRow);
     threePartContent.getElement().getStyle().setMarginTop(-16, Style.Unit.PX);
     return threePartContent;
   }*/

   /**
   * No comment - no move to the left...
   *
   * @param englishPhrase
   */
  @Override
  void moveEnglishForComment(Widget englishPhrase) {
  }

  /**
   * no comment
   *
   * @param firstRow
   */
  @Override
  void addCommentBox(DivWidget firstRow) {
    getCommentDiv();
  }

  /**
   * No audio play indicator, since it's not available. Thanks Jonathan S.
   *
   * @param hasRefAudio
   * @return
   */
  Widget getHasAudioIndicator(boolean hasRefAudio) {
    if (controller.getUserState().isAdmin()) {
      return super.getHasAudioIndicator(hasRefAudio);
    } else {
      return null;
    }
  }
}
