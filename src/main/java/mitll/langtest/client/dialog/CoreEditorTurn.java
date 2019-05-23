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
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.user.client.rpc.AsyncCallback;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.scoring.EnglishDisplayChoices;
import mitll.langtest.client.scoring.PhonesChoices;
import mitll.langtest.client.scoring.SimpleTurn;
import mitll.langtest.shared.exercise.ClientExercise;
import mitll.langtest.shared.project.Language;
import mitll.langtest.shared.project.OOVWordsAndUpdate;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Logger;

class CoreEditorTurn extends SimpleTurn implements IFocusListener, AddDeleteListener {
  private final Logger logger = Logger.getLogger("MySimpleTurn");

  private CoreVocabEditor coreVocabEditor;
  private EditableTurnHelper editableTurnHelper;
  private TurnAddDelete turnAddDelete;
  private ExerciseController<?> controller;
  private  int dialogID;
  private static final boolean DEBUG = false;

  CoreEditorTurn(ExerciseController<?> controller,
                 CoreVocabEditor coreVocabEditor,
                 ClientExercise vocab,
                 Language language,
                 boolean isInterpreter,
                 int dialogID) {
    super(vocab, ITurnContainer.COLUMNS.RIGHT, false);
    this.controller = controller;
    this.coreVocabEditor = coreVocabEditor;
    this.dialogID = dialogID;

    prev = vocab.getForeignLanguage();
    this.turnAddDelete = new TurnAddDelete(this);

    this.editableTurnHelper = new EditableTurnHelper(language, this, vocab, this) {
      @Override
      protected int getTextBoxWidth() {
        return 270;
      }
    };
    editableTurnHelper.setPlaceholder(isInterpreter, ITurnContainer.COLUMNS.RIGHT);
  }

  @Override
  public DivWidget addWidgets(boolean showFL, boolean showALTFL, PhonesChoices phonesChoices, EnglishDisplayChoices englishDisplayChoices) {
//      HTML html = new HTML(exercise.getForeignLanguage());
//      html.addStyleName("flfont");
//      html.getElement().getStyle().setPadding(10, Style.Unit.PX);
    logger.info("addWidgets : got '" + exercise.getForeignLanguage() + "' for " + exercise.getID());
    DivWidget widgets = new DivWidget();
    widgets.add(getTextBox());
    styleMe(widgets);
    add(widgets);
    addStyleName("inlineFlex");
    add(turnAddDelete.addAddTurnButton());
    add(turnAddDelete.addDeleteButton(false));
    return widgets;
  }

  @Override
  public void grabFocus() {
    editableTurnHelper.grabFocus();
  }

  @Override
  public void gotPlus() {
    coreVocabEditor.addTurnForSameSpeaker(this);
  }

  @Override
  public void gotMinus() {
    coreVocabEditor.deleteCurrentTurnOrPair(this);
  }

  /**
   * TODO Move focus to next...
   */
  @Override
  public void deleteGotFocus() {
    if (coreVocabEditor.isLast(this)) {  // since you may be about to click it
      if (DEBUG) logger.info("deleteGotFocus - ");
      grabFocus();
    } else {
      logger.warning("move focus???");
      //   coreVocabEditor.moveFocusToNext();
    }
  }

  /**
   * TODO add english
   *
   * @return
   */
  @NotNull
  protected DivWidget getTextBox() {
    DivWidget textBoxContainer = editableTurnHelper.getTextBox();
    // textBoxContainer.add(getTurnFeedback());
    return textBoxContainer;
  }

  private String prev = "";

  @Override
  public void gotBlur() {
//    coreVocabEditor.gotBlur(this);

    String s = editableTurnHelper.getContent();//SimpleHtmlSanitizer.sanitizeHtml(contentTextBox.getText()).asString();
    if (s.equals(prev)) {
      if (DEBUG) logger.info("gotBlur " + getExID() + " skip unchanged " + prev);
    } else {
      prev = s;
      //  int audioID = getAudioID();
      if (DEBUG) logger.info("gotBlur " + getExID() + " = " + prev);
      updateText(s, this, false);
    }
  }

  private void updateText(String s, CoreEditorTurn outer, boolean moveToNextTurn) {
    int projectID = controller.getProjectID();
    if (projectID != -1) {
      final int exID = getExID();

      logger.info("updateText : Checking " + s + " on " + projectID + " for " + exID);

      // talk to the audio service first to determine the oov
      controller.getAudioService().isValid(projectID, exID, s, new AsyncCallback<OOVWordsAndUpdate>() {
        @Override
        public void onFailure(Throwable caught) {
          controller.handleNonFatalError("isValid on text...", caught);

          String exceptionAsString = ExceptionHandlerDialog.getExceptionAsString(caught);
          logger.info("logException stack " + exceptionAsString);
        }

        @Override
        public void onSuccess(OOVWordsAndUpdate result) {
          logger.info("updateText : onSuccess " + result);

          showOOVResult(result);

          updateTextViaExerciseService(projectID, exID, s, moveToNextTurn, outer);
        }
      });
    }
  }

  private void updateTextViaExerciseService(int projectID, int exID, String s, boolean moveToNextTurn, CoreEditorTurn outer) {
    controller.getExerciseService().updateText(projectID, dialogID, exID, -1, s, new AsyncCallback<OOVWordsAndUpdate>() {
      @Override
      public void onFailure(Throwable caught) {
        controller.handleNonFatalError("updating text...", caught);

        String exceptionAsString = ExceptionHandlerDialog.getExceptionAsString(caught);
        logger.info("logException stack " + exceptionAsString);
      }

      @Override
      public void onSuccess(OOVWordsAndUpdate result) {
        // showOOVResult(result);
        logger.info("OK, update was " + result);
        if (moveToNextTurn) {
          // turnContainer.gotForward(outer);
        }
      }
    });
  }

  private void showOOVResult(OOVWordsAndUpdate result) {
    if (!result.getOov().isEmpty()) {
      StringBuilder builder = new StringBuilder();
      builder.append(result.isPossible() ? "No pronunciation for " : "You can't use these words ");
      result.getOov().forEach(oov -> builder.append(oov).append(" "));
      //turnFeedback.setText(builder.toString());
    } else {
      //turnFeedback.setText("");
    }
  }

  @Override
  public void gotFocus() {
    logger.info("gotFocus...");
  }

  @Override
  public void gotKey(KeyUpEvent event) {
    NativeEvent ne = event.getNativeEvent();

    String s = editableTurnHelper.getContent();//SimpleHtmlSanitizer.sanitizeHtml(contentTextBox.getText()).asString();
    if (ne.getKeyCode() == KeyCodes.KEY_ENTER) {
      ne.preventDefault();
      ne.stopPropagation();

      logger.info("gotKey : got enter on " + this.getExID());// + " : " + columns);

      if (s.equals(prev)) {
//        turnContainer.gotForward(this);
      } else {
        prev = s;
        logger.info("gotBlur " + getExID() + " = " + prev);
        updateText(s, this, true);
      }
    }
  }
}
