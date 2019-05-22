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

package mitll.langtest.client.dialog;

import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.custom.INavigation;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.scoring.IFocusable;
import mitll.langtest.client.scoring.SimpleTurn;
import mitll.langtest.shared.dialog.IDialog;
import mitll.langtest.shared.exercise.ClientExercise;
import mitll.langtest.shared.exercise.Exercise;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;

import static mitll.langtest.client.custom.INavigation.VIEWS.LISTEN;

public class CoreVocabEditor extends TurnViewHelper<SimpleTurn> implements IFocusable {
  private final boolean isInModal;

  public CoreVocabEditor(ExerciseController controller, INavigation.VIEWS thisView, IDialog theDialog) {
    super(controller, thisView);
    setDialog(theDialog);
    isInModal = theDialog != null;
  }

  @NotNull
  protected INavigation.VIEWS getNextView() {
    return LISTEN;
  }

  @Override
  protected void addDialogHeader(IDialog dialog, Panel child) {
    child.add(dialogHeader =
        new DialogHeader(controller,
            INavigation.VIEWS.CORE_EDITOR, isInModal ? null : getPrevView(), isInModal ? null : getNextView()).getHeader(dialog));
  }

  @NotNull
  protected DivWidget getTurns(IDialog dialog) {
    DivWidget turns = super.getTurns(dialog);
    DivWidget leftRight = new DivWidget();
    leftRight.setWidth("100%");
    leftRight.addStyleName("inlineFlex");

    leftRight.add(turns);
    turns.setWidth("60%");
    DivWidget right = new DivWidget();
    right.setWidth("40%");
    styleTurnContainer(right);

    leftRight.add(right);
    dialog.getCoreVocabulary().forEach(vocab -> {
      right.add(new MySimpleTurn(vocab));
    });
    if (dialog.getCoreVocabulary().isEmpty()) {
      right.add(new SimpleTurn(new Exercise(), ITurnContainer.COLUMNS.RIGHT, false));

    }
    return leftRight;
  }

  protected void addControls(DivWidget controlAndSpeakers) {
  }

  @Override
  public void grabFocus() {
  }

  /**
   * Don't show english.
   *
   * @param dialog
   * @param rowOne
   * @param left
   * @param right
   */
  @Override
  protected void addTurnPerExercise(IDialog dialog, DivWidget rowOne, String left, String right) {
    rowOne.clear();
    List<ClientExercise> exercises = dialog.getExercises();
    exercises = exercises.stream().filter(exercise ->
        !exercise.hasEnglishAttr() && !exercise.getForeignLanguage().isEmpty()).collect(Collectors.toList());
    addTurnForEachExercise(rowOne, left, right, exercises);
  }

  @NotNull
  @Override
  protected SimpleTurn reallyGetTurnPanel(ClientExercise clientExercise, ITurnContainer.COLUMNS columns, ITurnContainer.COLUMNS prevColumn, int index) {
    return new SimpleTurn(clientExercise, columns, false);
  }

  private static class MySimpleTurn extends SimpleTurn {
    public MySimpleTurn(ClientExercise vocab) {
      super(vocab, ITurnContainer.COLUMNS.RIGHT, false);
    }

    /**
     * TODO : use EditableTurnHelper to do text box and events
     *
     * @return
     */
    @NotNull
    private DivWidget getTextBox() {
      DivWidget textBoxContainer = new DivWidget();
      textBoxContainer.getElement().getStyle().setMarginBottom(10, Style.Unit.PX);
      //textBoxContainer.add(contentTextBox = addTextBox());
      // textBoxContainer.add(getTurnFeedback());
      return textBoxContainer;
    }
  }
}
