package mitll.langtest.client.flashcard;

import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.shared.exercise.ClientExercise;
import mitll.langtest.shared.exercise.CommonShell;

import java.util.logging.Logger;

class HidePolyglotPanel<L extends CommonShell, T extends ClientExercise> extends PolyglotPracticePanel<L, T> {
  private final Logger logger = Logger.getLogger("HidePolyglotPanel");

  /**
   * @param statsFlashcardFactory
   * @param controlState
   * @param controller
   * @param soundFeedback
   * @param e
   * @param stickyState
   * @param exerciseListToUse
   * @param showAudio
   * @see HidePolyglotFactory#getFlashcard
   */
  HidePolyglotPanel(PolyglotFlashcardContainer statsFlashcardFactory,
                    ControlState controlState,
                    ExerciseController controller,
                    MySoundFeedback soundFeedback,
                    T e,
                    StickyState stickyState,
                    ListInterface<L, T> exerciseListToUse,
                    int minPoly,
                    boolean showAudio) {
    super(statsFlashcardFactory, controlState, controller, soundFeedback, e, stickyState, exerciseListToUse, minPoly, showAudio);
  }

  Panel getRightColumn(final ControlState controlState) {
    Panel rightColumn = new DivWidget();
    rightColumn.addStyleName("leftTenMargin");

    if (showAudio) {
      rightColumn.add(getAudioGroup(controlState));
      addControlsBelowAudio(controlState, rightColumn);
    } else {
      if (logger != null) {
        logger.info("not showing audio for ");
      }
    }
    rightColumn.add(getKeyBinding());
    return rightColumn;
  }

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
    if (showAudio) {
      return super.getHasAudioIndicator(hasRefAudio);
    } else {
      return null;
    }
  }
}
