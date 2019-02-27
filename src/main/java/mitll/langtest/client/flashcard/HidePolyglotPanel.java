/*
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2019. Other request for this document shall be referred
 * to DLIFLC.
 *
 * WARNING: This document may contain technical data whose export is restricted
 * by the Arms Export Control Act (AECA) or the Export Administration Act (EAA).
 * Transfer of this data by any means to a non-US person who is not eligible to
 * obtain export-controlled data is prohibited. By accepting this data, the consignee
 * agrees to honor the requirements of the AECA and EAA. DESTRUCTION NOTICE: For
 * unclassified, limited distribution documents, destroy by any method that will
 * prevent disclosure of the contents or reconstruction of the document.
 *
 * This material is based upon work supported under Air Force Contract No.
 * FA8721-05-C-0002 and/or FA8702-15-D-0001. Any opinions, findings, conclusions
 * or recommendations expressed in this material are those of the author(s) and
 * do not necessarily reflect the views of the U.S. Air Force.
 *
 * © 2015-2019 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 */

package mitll.langtest.client.flashcard;

import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.custom.INavigation;
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
   * @paramx showAudio
   * @see HidePolyglotFactory#getFlashcard
   */
  HidePolyglotPanel(PolyglotFlashcardContainer statsFlashcardFactory,
                    ControlState controlState,
                    ExerciseController controller,
                    MySoundFeedback soundFeedback,
                    T e,
                    StickyState stickyState,
                    ListInterface<L, T> exerciseListToUse, INavigation.VIEWS instance
  ) {
    super(statsFlashcardFactory, controlState, controller, soundFeedback, e, stickyState, exerciseListToUse, instance);
  }

  /**
   * @param controlState
   * @return
   * @see FlashcardPanel#getThreePartContent
   */
  Panel getRightColumn(final ControlState controlState) {
    Panel rightColumn = new DivWidget();
    rightColumn.addStyleName("leftTenMargin");

    if (quizSpec.isShowAudio()) {
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
