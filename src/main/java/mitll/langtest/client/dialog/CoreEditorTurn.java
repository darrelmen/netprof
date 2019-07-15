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

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.user.client.rpc.AsyncCallback;
import mitll.langtest.client.exercise.EditorServices;
import mitll.langtest.client.scoring.EnglishDisplayChoices;
import mitll.langtest.client.scoring.PhonesChoices;
import mitll.langtest.client.scoring.SimpleTurn;
import mitll.langtest.shared.dialog.DialogExChangeResponse;
import mitll.langtest.shared.exercise.ClientExercise;
import mitll.langtest.shared.project.Language;
import mitll.langtest.shared.project.OOVWordsAndUpdate;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

class CoreEditorTurn extends SimpleTurn implements IFocusListener, AddDeleteListener {
  private final Logger logger = Logger.getLogger("CoreEditorTurn");

  private static final boolean DEBUG_UPDATE_TEXT = false;

  private CoreVocabEditor coreVocabEditor;
  private EditableTurnHelper editableTurnHelper;
  private EditableTurnHelper englishEditableTurnHelper;
  private TurnAddDelete turnAddDelete;
  private EditorServices<?> controller;
  private final int projID;
  private final int dialogID;

  private static final int WIDTH_TO_USE = 210;

  private static final boolean DEBUG = false;
  private boolean onlyOneTurn;

  private String prev = "";
  private String engprev = "";


  /**
   * @param controller
   * @param coreVocabEditor
   * @param vocab
   * @param language
   * @param projID
   * @param dialogID
   * @param onlyOneTurn
   * @see CoreVocabEditor#reallyGetTurnPanel
   */
  CoreEditorTurn(EditorServices<?> controller,
                 CoreVocabEditor coreVocabEditor,
                 ClientExercise vocab,
                 Language language,
                 int projID,
                 int dialogID,
                 boolean onlyOneTurn) {
    super(vocab, ITurnContainer.COLUMNS.RIGHT, false, controller.getLanguageInfo());
    this.onlyOneTurn = onlyOneTurn;
    this.controller = controller;
    this.coreVocabEditor = coreVocabEditor;

    this.projID = projID;
    this.dialogID = dialogID;

    if (DEBUG) logger.info("core " + vocab.getID() + " " + vocab.getForeignLanguage());
    if (coreVocabEditor == null) logger.warning("huh? how can that be??\n\n\n");

    prev = vocab.getForeignLanguage();
    engprev = vocab.getEnglish();

    this.turnAddDelete = new TurnAddDelete(this, 21);

    this.editableTurnHelper = new EditableTurnHelper(language, vocab.hasEnglishAttr(), vocab.getForeignLanguage(),
        this, false) {
      @Override
      protected int getTextBoxWidth() {
        return WIDTH_TO_USE;
      }
    };
    editableTurnHelper.setPlaceholder(ITurnContainer.COLUMNS.RIGHT);
    editableTurnHelper.setPlaceholder("Core Vocabulary");

    this.englishEditableTurnHelper = new EditableTurnHelper(language, true, vocab.getEnglish(),
        this, false) {
      @Override
      protected int getTextBoxWidth() {
        return WIDTH_TO_USE;
      }
    };
    englishEditableTurnHelper.setPlaceholder("English Translation...");
  }

  private Button deleteButton;

  @Override
  public DivWidget addWidgets(boolean showFL, boolean showALTFL, PhonesChoices phonesChoices, EnglishDisplayChoices englishDisplayChoices) {
    //logger.info("addWidgets : got '" + exercise.getForeignLanguage() + "' for " + exercise.getID());
    DivWidget widgets = new DivWidget();
    DivWidget textBox1 = getTextBox();
    textBox1.addStyleName("topFiveMargin");
    widgets.add(textBox1);
    DivWidget textBox = englishEditableTurnHelper.getTextBox(false);

    widgets.add(textBox);
    styleMe(widgets);
    add(widgets);
    addStyleName("inlineFlex");
    DivWidget buttons = new DivWidget();
    add(buttons);
    buttons.addStyleName("inlineFlex");
    buttons.add(turnAddDelete.addAddTurnButton());

    buttons.add(deleteButton = turnAddDelete.addDeleteButton(onlyOneTurn));
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

  void enableDelete(boolean enable) {
    //logger.info("enabledDelete " + enable + " on " + getExID());
    deleteButton.setEnabled(enable);
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
      //  logger.warning("move focus???");
      coreVocabEditor.moveFocusToNext();
    }
  }

  /**
   * @return
   */
  @NotNull
  private DivWidget getTextBox() {
    return editableTurnHelper.getTextBox(false);
  }

  @Override
  public void gotBlur() {
    coreVocabEditor.gotBlur(this);

    String s = getContent();
    if (s.equals(prev)) {
      if (DEBUG) logger.info("gotBlur " + getExID() + " maybe skip unchanged " + prev);

      String eng = englishEditableTurnHelper.getContent();
      if (!eng.equals(engprev)) {
        engprev = eng;
        maybeCreateFirst(s, false, true);
      }
    } else {
      prev = s;
      if (DEBUG) logger.info("gotBlur " + getExID() + " = " + prev);
      maybeCreateFirst(s, false, false);
    }
  }

  private void updateEnglish(boolean moveOn) {
    if (projID != -1) {
      updateEnglishText(projID, getExID(), englishEditableTurnHelper.getContent(), moveOn);
    }
  }

  @Override
  public String getContent() {
    return editableTurnHelper.getContent();
  }

  @Override
  public void gotKey(KeyUpEvent event) {
    NativeEvent ne = event.getNativeEvent();

    String s = getContent();
    if (ne.getKeyCode() == KeyCodes.KEY_ENTER) {
      ne.preventDefault();
      ne.stopPropagation();

      if (DEBUG) logger.info("gotKey : got enter on " + this.getExID());// + " : " + columns);

      if (s.equals(prev)) {
        String eng = englishEditableTurnHelper.getContent();
        if (!eng.equals(engprev)) {
          engprev = eng;
          maybeCreateFirst(s, true, true);
        } else {
          // logger.warning("deal with making a new ex");
          coreVocabEditor.gotForward(this);
        }
      } else {
        prev = s;
        if (DEBUG) logger.info("gotBlur " + getExID() + " = " + prev);
        maybeCreateFirst(s, true, false);
      }
    }
  }

  /**
   * @param s
   * @param moveToNextTurn
   * @param isEnglish
   * @see #gotBlur()
   * @see #gotKey(KeyUpEvent)
   */
  private void maybeCreateFirst(String s, boolean moveToNextTurn, boolean isEnglish) {
    if (getExID() == -1) {
      controller.getDialogService().addEmptyCoreExercise(dialogID, -1, new AsyncCallback<DialogExChangeResponse>() {
        @Override
        public void onFailure(Throwable caught) {
          controller.handleNonFatalError("addEmptyCoreExercise ?", caught);
        }

        @Override
        public void onSuccess(DialogExChangeResponse result) {
          if (result.getChanged().isEmpty()) {
            logger.warning("maybeCreateFirst huh? didn't add");
          } else {
            exercise = result.getChanged().get(0);
            if (DEBUG) logger.info("maybeCreateFirst ex now " + exercise + " move turn to next " + moveToNextTurn);

            int size = coreVocabEditor.getAllTurns().size();
            //  logger.info("maybeCreateFirst Got " + size);
            if (size == 1) {
              coreVocabEditor.getAllTurns().get(0).enableDelete(true);
            }

            if (isEnglish) {
              updateEnglish(moveToNextTurn);
            } else {
              updateText(s, moveToNextTurn);
            }
          }
        }
      });
    } else {
      if (isEnglish) {
        updateEnglish(moveToNextTurn);
      } else {
        updateText(s, moveToNextTurn);
      }
    }
  }

  /**
   * @param s
   * @param moveToNextTurn
   * @see #maybeCreateFirst(String, boolean, boolean)
   */
  private void updateText(String s, boolean moveToNextTurn) {
    if (projID != -1) {
      final int exID = getExID();
      if (DEBUG) logger.info("updateText : Checking " + s + " on " + projID + " for " + exID);

      // talk to the audio service first to determine the oov
      controller.getAudioService().isValid(projID, exID, getSanitized(s), new AsyncCallback<OOVWordsAndUpdate>() {
        @Override
        public void onFailure(Throwable caught) {
          controller.handleNonFatalError("isValid on text...", caught);

          String exceptionAsString = ExceptionHandlerDialog.getExceptionAsString(caught);
          logger.info("logException stack " + exceptionAsString);
        }

        @Override
        public void onSuccess(OOVWordsAndUpdate result) {
          if (DEBUG_UPDATE_TEXT) logger.info("updateText : onSuccess " + result);

          showOOVResult(result);

          updateTextViaExerciseService(projID, exID, s, result.getNormalizedText(), moveToNextTurn);
        }
      });
    }
  }

  private String getSanitized(String s) {
    return englishEditableTurnHelper.getSanitized(s);
  }

  /**
   * TODO : consider handling OOV.
   *
   * @param projectID
   * @param exID
   * @param s
   * @param moveToNextTurn
   * @param outer
   */
  private void updateTextViaExerciseService(int projectID, int exID, String s, String normalized, boolean moveToNextTurn) {
    CoreEditorTurn outer = this;
    String sanitized = getSanitized(s);
    String sanitized2 = getSanitized(normalized);

    if (DEBUG) {
      logger.info("updateTextViaExerciseService " + exID +
          "\n\tcontent    " + s +
          "\n\tsanitized  " + sanitized +
          "\n\tnormalized " + normalized +
          "\n\tsanitized  " + sanitized2
      );
    }

    controller.getExerciseService().updateText(projectID, dialogID, exID, -1, sanitized, sanitized2, new AsyncCallback<OOVWordsAndUpdate>() {
      @Override
      public void onFailure(Throwable caught) {
        controller.handleNonFatalError("updating text...", caught);

        String exceptionAsString = ExceptionHandlerDialog.getExceptionAsString(caught);
        logger.info("logException stack " + exceptionAsString);
      }

      @Override
      public void onSuccess(OOVWordsAndUpdate result) {
        // showOOVResult(result);
        if (DEBUG) logger.info("updateTextViaExerciseService OK, update was " + result);

        coreVocabEditor.setHighlights();
        if (moveToNextTurn) {
          coreVocabEditor.gotForward(outer);
        }

        syncAudioService(result.isDidUpdate(), projectID, exID);
      }
    });
  }

  private void updateEnglishText(int projectID, int exID, String s, boolean moveToNextTurn) {
    CoreEditorTurn outer = this;
    controller.getExerciseService().updateEnglishText(projectID, dialogID, exID, getSanitized(s), new AsyncCallback<Boolean>() {
      @Override
      public void onFailure(Throwable caught) {
        controller.handleNonFatalError("updating english text...", caught);

        String exceptionAsString = ExceptionHandlerDialog.getExceptionAsString(caught);
        logger.info("logException stack " + exceptionAsString);
      }

      @Override
      public void onSuccess(Boolean result) {
        // showOOVResult(result);
        if (DEBUG) logger.info("updateEnglishText OK, update was " + result + " move to next " + moveToNextTurn);
        //coreVocabEditor.setHighlights();
        if (moveToNextTurn) {
          coreVocabEditor.gotForward(outer);
        }
        syncAudioService(result, projectID, exID);
      }
    });
  }

  private void syncAudioService(boolean success, int projectID, int exID) {
    if (success) {
      Set<Integer> singleton = new HashSet<>();
      singleton.add(exID);
      controller.getAudioService().refreshExercises(projectID, singleton, new AsyncCallback<Void>() {
        @Override
        public void onFailure(Throwable caught) {
          controller.handleNonFatalError("refreshing exercise on hydra", caught);
        }

        @Override
        public void onSuccess(Void result) {
          logger.info("OK, updated " + exID + " on hydra/hydra2");
        }
      });
    }
  }

  /**
   * TODO : actually show oov?
   *
   * @param result
   */
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
    coreVocabEditor.setCurrentTurnTo(this);
  }
}
