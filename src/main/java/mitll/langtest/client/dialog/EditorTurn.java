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
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Focusable;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.UIObject;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.banner.SessionManager;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.scoring.*;
import mitll.langtest.shared.answer.AudioAnswer;
import mitll.langtest.shared.answer.AudioType;
import mitll.langtest.shared.answer.Validity;
import mitll.langtest.shared.exercise.AudioAttribute;
import mitll.langtest.shared.exercise.ClientExercise;
import mitll.langtest.shared.project.OOVWordsAndUpdate;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.logging.Logger;

import static mitll.langtest.client.dialog.ITurnContainer.COLUMNS.MIDDLE;

/**
 *
 */
public class EditorTurn extends PlayAudioExercisePanel
    implements ITurnPanel, IRehearseView, IRecordingTurnPanel, IFocusListener, AddDeleteListener {
  private final Logger logger = Logger.getLogger("EditorTurn");

  private static final int TURN_WIDTH = 97;
  private static final int RIGHT_TURN_RIGHT_MARGIN = 153;

  private final TurnPanelDelegate turnPanelDelegate;
  private final ClientExercise clientExercise;

  //private final Language language;
  private final ExerciseController<?> controller;
  private final ITurnContainer<EditorTurn> turnContainer;
  //private final IEditableTurnContainer<EditorTurn> editableTurnContainer;
  private final int dialogID;
  private String prev = "";
  private final ITurnContainer.COLUMNS columns;
  private final ITurnContainer.COLUMNS prevColumn;
  private final boolean isFirstTurn;
  /**
   * @see #addWidgets(boolean, boolean, PhonesChoices, EnglishDisplayChoices)
   */
  //private TextBox contentTextBox;
  private HTML turnFeedback;
  private NoFeedbackRecordAudioPanel<ClientExercise> recordAudioPanel;

  private final SessionManager sessionManager;
  private boolean isDeleting = false;

  private EditableTurnHelper editableTurnHelper;
  private TurnAddDelete turnAddDelete;
  private static final boolean DEBUG = false;
  private IEditableTurnContainer<EditorTurn> editableTurnContainer;

  /**
   * @param clientExercise
   * @param columns
   * @param prevColumn
   * @param rightJustify
   * @param controller
   * @see DialogEditor#makeTurnPanel(ClientExercise, ListenViewHelper.COLUMNS, ListenViewHelper.COLUMNS, boolean, int)
   */
  EditorTurn(final ClientExercise clientExercise,
             ITurnContainer.COLUMNS columns,
             ITurnContainer.COLUMNS prevColumn,
             boolean rightJustify,
             ExerciseController<?> controller,
             ITurnContainer<EditorTurn> turnContainer,
             IEditableTurnContainer<EditorTurn> editableTurnContainer,
             int dialogID,
             boolean isFirstTurn,
             SessionManager sessionManager) {
    if (DEBUG) {
      logger.info("EditorTurn : turn " +
          "\n\tdialog " + dialogID +
          "\n\tex     " + clientExercise.getID() +
          "\n\tfl     '" + clientExercise.getForeignLanguage() + "' " +
          "\n\thas english " + clientExercise.hasEnglishAttr() +
          "\n\tcol    " + columns +
          "\n\taudio  " + clientExercise.getAudioAttributes()
      );
    }
    this.sessionManager = sessionManager;

    this.columns = columns;

    this.prevColumn = prevColumn;

    this.isFirstTurn = isFirstTurn;

    this.turnAddDelete = new TurnAddDelete(this, 26);
    this.editableTurnHelper = new EditableTurnHelper(controller.getLanguageInfo(), this, clientExercise, this);
    editableTurnHelper.setPlaceholder(turnContainer.isInterpreter(), columns);

    turnPanelDelegate = new TurnPanelDelegate(clientExercise, this, columns, rightJustify) {
      @Override
      protected void addMarginStyle(Style style2) {
        style2.setMarginLeft(15, Style.Unit.PX);
        boolean useBigRightMargin = columns == ITurnContainer.COLUMNS.RIGHT && turnContainer.isInterpreter();
        style2.setMarginRight(useBigRightMargin ? RIGHT_TURN_RIGHT_MARGIN : 10, Style.Unit.PX);
        // style2.setMarginTop(7, Style.Unit.PX);
        style2.setMarginBottom(0, Style.Unit.PX);
      }
    };

    Style style = getElement().getStyle();
    style.setProperty("minWidth", "500px");

    this.dialogID = dialogID;
    this.clientExercise = clientExercise;

    this.controller = controller;
    this.turnContainer = turnContainer;
    this.editableTurnContainer = editableTurnContainer;
    setId("EditorTurn_" + getExID());

    if (columns == MIDDLE) {
      if (clientExercise.hasEnglishAttr()) {
        addStyleName("floatRight");
      } else {
        addStyleName("floatLeft");
        style.setClear(Style.Clear.BOTH);
      }
    }
  }

  /**
   * If we paste in some text and then reload the page without a blur, don't loose the text!
   */
  @Override
  protected void onUnload() {
    super.onUnload();
    gotBlur();
  }

  @Override
  public String getContent() {
    return clientExercise.getForeignLanguage();
  }

  @Override
  public String getText() {
    return getExID() + " '" + clientExercise.getForeignLanguage() + "'";
  }

  @Override
  public int getExID() {
    return clientExercise == null ? -1 : clientExercise.getID();
  }

  @Override
  public void clearHighlight() {
  }

  /**
   * @return
   * @see DialogEditor#addTurnForSameSpeaker
   */
  public ITurnContainer.COLUMNS getColumn() {
    return columns;
  }

  @Override
  public DivWidget addWidgets(boolean showFL,
                              boolean showALTFL,
                              PhonesChoices phonesChoices,
                              EnglishDisplayChoices englishDisplayChoices) {
    DivWidget wrapper = new DivWidget();
    wrapper.getElement().setId("Wrapper_" + getExID());

    NoFeedbackRecordAudioPanel<ClientExercise> recordPanel =
        new ContinuousDialogRecordAudioPanel(clientExercise, controller, sessionManager, this, new IRecordResponseListener() {
          @Override
          public void usePartial(StreamResponse response) {
            // logger.info("addWidgets : Got partial..." + response);
          }

          @Override
          public Widget myGetPopupTargetWidget() {
            return recordAudioPanel;
          }

        }) {
          @Override
          protected boolean shouldAddToAudioTable() {
            return true;
          }

          @Override
          protected AudioType getAudioType() {
            return AudioType.REGULAR;
          }

          @Override
          public void startRecording() {
            //logger.info("startRecording...");
            super.startRecording();
            turnFeedback.setHTML("");
          }

          @Override
          public void showInvalidResultPopup(String message) {
            turnFeedback.setHTML(message);
          }
        };


    this.recordAudioPanel = recordPanel;

    recordPanel.addWidgets();

    PostAudioRecordButton postAudioRecordButton = null;
    DivWidget buttonContainer = new DivWidget();
    buttonContainer.setId("recordButtonContainer_" + getExID());

    {
      postAudioRecordButton = getPostAudioWidget(recordPanel, true);
      buttonContainer.add(postAudioRecordButton);


      RecorderPlayAudioPanel playAudioPanel = recordPanel.getPlayAudioPanel();

      setPlayAudio(playAudioPanel);

      if (clientExercise.getAudioAttributes().isEmpty()) {
        playAudioPanel.setEnabled(false);
      } else {
        AudioAttribute next = getLatestAudio();

        if (DEBUG || true) {
          logger.info("addWidgets :binding " + next + " to play for turn for " + getExID());
        }
        playAudioPanel.rememberAudio(next);
        playAudioPanel.setEnabled(true);
      }
      playAudioPanel.showPlayButton();

      buttonContainer.add(getPlayButton(playAudioPanel));
      buttonContainer.getElement().getStyle().setMarginTop(3, Style.Unit.PX);
    }

    wrapper.add(buttonContainer);

    addPressAndHoldStyleForRecordButton(postAudioRecordButton);
    turnAddDelete.removeFromTabSequence(postAudioRecordButton);

    wrapper.add(getTextBox());


    styleMe(wrapper);
    wrapper.addStyleName("inlineFlex");
    add(wrapper);

    if (columns == MIDDLE || !turnContainer.isInterpreter()) {
      addStyleName("inlineFlex");
      add(turnAddDelete.addAddTurnButton());
      add(turnAddDelete.addDeleteButton(isFirstTurn));
      addOtherTurn();
    }
    return wrapper;
  }

  @NotNull
  private Widget getPlayButton(RecorderPlayAudioPanel playAudioPanel) {
    Widget playButton = playAudioPanel.getPlayButton();
    turnAddDelete.removeFromTabSequence((Focusable) playButton);

    playButton.addStyleName("floatRight");
    addPressAndHoldStyleForRecordButton(playButton);
    return playButton;
  }

//  private void removeFromTabSequence(Focusable postAudioRecordButton) {
//    postAudioRecordButton.setTabIndex(-1);
//  }

  @NotNull
  private DivWidget getTextBox() {
    DivWidget textBoxContainer = editableTurnHelper.getTextBox();
    textBoxContainer.add(getTurnFeedback());
    return textBoxContainer;
  }

  private AudioAttribute getLatestAudio() {
    AudioAttribute latest = null;
    for (AudioAttribute audio : clientExercise.getAudioAttributes()) {
      if (latest == null || audio.getTimestamp() > latest.getTimestamp()) {
        latest = audio;
      }
    }
    return latest;
  }


  @NotNull
  private HTML getTurnFeedback() {
    HTML turnFeedback = new HTML("");

    Style style = turnFeedback.getElement().getStyle();

    style.setMarginTop(-12, Style.Unit.PX);
    style.setMarginLeft(12, Style.Unit.PX);
    style.setTextAlign(Style.TextAlign.LEFT);
    this.turnFeedback = turnFeedback;
    return turnFeedback;
  }

  private static final String BLUE_INACTIVE_COLOR = "#0171bc";

  @NotNull
  private PostAudioRecordButton getPostAudioWidget(NoFeedbackRecordAudioPanel<?> recordPanel, boolean enabled) {
    PostAudioRecordButton postAudioRecordButton = recordPanel.getPostAudioRecordButton();
    postAudioRecordButton.setEnabled(enabled);
    postAudioRecordButton.getElement().getStyle().setBackgroundColor(BLUE_INACTIVE_COLOR);
    return postAudioRecordButton;
  }

  @Override
  public boolean isRecording() {
    return getPostAudioRecordButton().isRecording();
  }

  @Override
  public void cancelRecording() {
    recordAudioPanel.cancelRecording();
  }

/*  private void addAddTurnButton() {
    Button w = getTripleButton();

    w.addClickHandler(event -> gotPlus());
    w.setIcon(IconType.PLUS);
    w.setType(ButtonType.SUCCESS);

    tripleButtonStyle(w);

    add(w);
  }*/

/*
  private void addDeleteButton() {
    Button w = getTripleButton();

    w.addClickHandler(event -> gotMinus());
    w.setType(ButtonType.WARNING);
    w.setIcon(IconType.MINUS);

    // can't blow away the first turn!
    w.setEnabled(!isFirstTurn);

    tripleFirstStyle(w);

    w.addFocusHandler(event -> deleteGotFocus());
    w.addBlurHandler(event -> deleteGotBlur());
    add(w);
  }
*/

/*  @NotNull
  private Button getTripleButton() {
    return new Button() {
      @Override
      protected void onAttach() {
        int tabIndex = getTabIndex();
        super.onAttach();

        if (-1 == tabIndex) {
          setTabIndex(-1);
        }
      }
    };
  }*/

  /**
   * So, if you get the focus and you're not last, move on to next
   * How can we distinguish between delete getting focus, just before buttton press
   */
  public void deleteGotFocus() {
    if (turnContainer.isLast(this)) {  // since you may be about to click it
      if (DEBUG) logger.info("deleteGotFocus - ");
      grabFocus();
    } else {
      turnContainer.moveFocusToNext();
    }
  }

/*
  private void deleteGotBlur() {
    if (turnContainer.isLast(this)) {  // since you may be about to click it
      if (DEBUG) logger.info("deleteGotBlur - ");
      // grabFocus();
    } else {
      if (DEBUG) logger.info("deleteGotBlur - not last ");
      //  turnContainer.moveFocusToNext();
    }
  }
*/


  private void addOtherTurn() {
    Button w = turnAddDelete.getTripleButton();

    w.setType(ButtonType.INFO);

    w.addClickHandler(event -> gotOtherSpeaker());

    ITurnContainer.COLUMNS toUseForArrow = columns == MIDDLE ? prevColumn : columns;
    //  logger.info("the column is " + toUseForArrow);
    w.setIcon(toUseForArrow == ITurnContainer.COLUMNS.LEFT ? IconType.ARROW_RIGHT : IconType.ARROW_LEFT);

    turnAddDelete.tripleButtonStyle(w);

    add(w);
  }

 /* private void tripleButtonStyle(Button w) {
    tripleFirstStyle(w);
    //   w.addFocusHandler(event -> turnContainer.moveFocusToNext());
    removeFromTabSequence(w);
//    logger.info("aftr " + getExID() + " " + w.getTabIndex());
  }*/

/*
  private void tripleFirstStyle(Button w) {
    addPressAndHoldStyle(w);

    w.addStyleName("topFiveMargin");
    w.addStyleName("leftFiveMargin");
  }
*/

  @Override
  public void gotPlus() {
    editableTurnContainer.addTurnForSameSpeaker(this);
  }

  @Override
  public void gotMinus() {
    editableTurnContainer.deleteCurrentTurnOrPair(this);
  }

  private void gotOtherSpeaker() {
    editableTurnContainer.addTurnForOtherSpeaker(this);
  }

/*
  private void addPressAndHoldStyle(UIObject postAudioRecordButton) {
    Style style = postAudioRecordButton.getElement().getStyle();
    style.setProperty("borderRadius", 21 + "px");
    style.setPadding(9, Style.Unit.PX);
    style.setWidth(26, Style.Unit.PX);
    style.setMarginRight(5, Style.Unit.PX);
    style.setHeight(20, Style.Unit.PX);
  }
*/

  private void addPressAndHoldStyleForRecordButton(UIObject postAudioRecordButton) {
    Style style = postAudioRecordButton.getElement().getStyle();
    style.setProperty("borderRadius", "18px");
    style.setPadding(8, Style.Unit.PX);
    style.setMarginRight(5, Style.Unit.PX);

    style.setWidth(19, Style.Unit.PX);
    style.setHeight(19, Style.Unit.PX);
  }

  /**
   * @param wrapper
   * @see #addWidgets(boolean, boolean, PhonesChoices, EnglishDisplayChoices)
   */
/*
  private TextBox addTextBox() {
    // TODO : instead, make this a div contenteditable!
    TextBox w = new TextBox();
    w.getElement().getStyle().setFontSize(16, Style.Unit.PX);
//    w.setId("TextBox_" + getExID());
    w.setWidth(350 + "px");

    String foreignLanguage = clientExercise.getForeignLanguage();
    if (foreignLanguage.isEmpty()) {
      String placeholder = clientExercise.hasEnglishAttr() ? "English... (" + getExID() +
          ")" : language.toDisplay() + " translation (" + getExID() +
          ")";
      if (!turnContainer.isInterpreter()) {
        placeholder = (columns == ITurnContainer.COLUMNS.LEFT ? SPEAKER_A : SPEAKER_B) + " says...";
      }
      w.setPlaceholder(placeholder);
    } else {
      w.setText(foreignLanguage);
      prev = foreignLanguage;
    }
    w.addBlurHandler(event -> gotBlur());
    w.addFocusHandler(event -> gotFocus());
    w.addKeyUpHandler(this::gotKey);

    w.addStyleName("leftTenMargin");
    w.addStyleName("rightTenMargin");
    w.addStyleName("topFiveMargin");

    return w;
  }
*/
  public void gotFocus() {
    if (DEBUG) {
      logger.info("gotFocus " + getExID());
    }
    turnContainer.setCurrentTurnTo(this);
  }

  /**
   * If the turn is an interpreter turn, warn when it gets too long to remember by marking it yellow
   * or red.
   *
   * @param event
   */
  public void gotKey(KeyUpEvent event) {
    NativeEvent ne = event.getNativeEvent();

    String s = editableTurnHelper.getContent();//SimpleHtmlSanitizer.sanitizeHtml(contentTextBox.getText()).asString();
    if (ne.getKeyCode() == KeyCodes.KEY_ENTER) {
      ne.preventDefault();
      ne.stopPropagation();

      logger.info("gotKey : got enter on " + this.getExID() + " : " + columns);

      if (s.equals(prev)) {
        turnContainer.gotForward(this);
      } else {
        prev = s;
        int audioID = getAudioID();
        logger.info("gotBlur " + getExID() + " = " + prev + " audio id " + audioID);
        updateText(s, this, audioID, true);
      }
    } else {
      int length = s.split(" ").length;
      //  logger.info("num tokens " + length);

      if (length > 10) {
        //contentTextBox.getElement().getStyle().setBackgroundColor("red");
        editableTurnHelper.setBackgroundColor("red");
        turnFeedback.setText("Really avoid long phrases.");
      } else if (length > 7) {
        // contentTextBox.getElement().getStyle().setBackgroundColor("yellow");
        editableTurnHelper.setBackgroundColor("yellow");
        turnFeedback.setText("Avoid long phrases.");
      } else {
        //  contentTextBox.getElement().getStyle().setBackgroundColor("white");
        editableTurnHelper.setBackgroundColor("white");
        turnFeedback.setText("");
      }
    }
  }

  /**
   * Tell container we lost the focus so we can maybe pre-empt the natural sequence and move
   * it to the first turn.
   */
  public void gotBlur() {
    turnContainer.gotBlur(this);
    String s = editableTurnHelper.getContent();//SimpleHtmlSanitizer.sanitizeHtml(contentTextBox.getText()).asString();
    if (s.equals(prev)) {
      if (DEBUG) logger.info("gotBlur " + getExID() + " skip unchanged " + prev);
    } else {
      prev = s;
      int audioID = getAudioID();
      if (DEBUG) logger.info("gotBlur " + getExID() + " = " + prev + " audio " + audioID);
      updateText(s, this, audioID, false);
    }
  }

  private void updateText(String s, EditorTurn outer, int audioID, boolean moveToNextTurn) {
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

          updateTextViaExerciseService(projectID, exID, audioID, s, moveToNextTurn, outer);
        }
      });
    }
  }

  private void updateTextViaExerciseService(int projectID, int exID, int audioID, String s, boolean moveToNextTurn, EditorTurn outer) {
    controller.getExerciseService().updateText(projectID, dialogID, exID, audioID, s, new AsyncCallback<OOVWordsAndUpdate>() {
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
          turnContainer.gotForward(outer);
        }
      }
    });
  }

  private void showOOVResult(OOVWordsAndUpdate result) {
    if (!result.getOov().isEmpty()) {
      StringBuilder builder = new StringBuilder();
      builder.append(result.isPossible() ? "No pronunciation for " : "You can't use these words ");
      result.getOov().forEach(oov -> builder.append(oov).append(" "));
      turnFeedback.setText(builder.toString());
    } else turnFeedback.setText("");
  }

  private int getAudioID() {
    // logger.info("has " + clientExercise.getAudioAttributes().size() + " audio attributes...");
    Collection<AudioAttribute> audioAttributes = clientExercise.getAudioAttributes();
    logger.info("getAudioID : audio attr " + audioAttributes);

    return audioAttributes.isEmpty() ? -1 : audioAttributes.iterator().next().getUniqueID();
  }

  /**
   * @see DialogEditor#grabFocus
   */
  @Override
  public void grabFocus() {
    editableTurnHelper.grabFocus();
//    if (contentTextBox == null) {
//      logger.info("grabFocus no contentTextBox yet for " + getText());
//    } else {
//      logger.info("grabFocus on " + getText());
//      contentTextBox.setFocus(true);
//    }
  }

  @Override
  public void getRefAudio(RefAudioListener listener) {

  }

  @Override
  public void setReq(int req) {

  }

  @Override
  public int getReq() {
    return 0;
  }

  /**
   * @param wrapper
   * @see RefAudioGetter#addWidgets(boolean, boolean, PhonesChoices, EnglishDisplayChoices)
   */
  private void styleMe(DivWidget wrapper) {
    turnPanelDelegate.styleMe(wrapper);
    if (columns == MIDDLE) {
      wrapper.getElement().getStyle().setMarginRight(0, Style.Unit.PX);
    }
    wrapper.setWidth(TURN_WIDTH + "%");
  }

  @Override
  public void addFloatLeft(Widget w) {
    turnPanelDelegate.addFloatLeft(w);
  }

  @Override
  public boolean shouldAddFloatLeft() {
    return turnPanelDelegate.shouldAddFloatLeft();
  }

  @Override
  public boolean isMiddle() {
    return turnPanelDelegate.isMiddle();
  }

  @Override
  public boolean isLeft() {
    return turnPanelDelegate.isLeft();
  }

  @Override
  public boolean isRight() {
    return turnPanelDelegate.isRight();
  }

  /**
   * @see ListenViewHelper#removeMarkCurrent
   */
  public void removeMarkCurrent() {
    turnPanelDelegate.removeMarkCurrent();
  }

  /**
   * @see ListenViewHelper#markCurrent
   */
  public void markCurrent() {
    turnPanelDelegate.markCurrent();
  }

  public boolean hasCurrentMark() {
    return turnPanelDelegate.hasCurrentMark();// bubble.getElement().getStyle().getBorderColor().equalsIgnoreCase(HIGHLIGHT_COLOR);
  }

  public void makeVisible() {
    turnPanelDelegate.makeVisible();
  }

  /**
   * @param clickHandler
   * @see ListenViewHelper#getTurnPanel
   */
  @Override
  public void addClickHandler(ClickHandler clickHandler) {
    turnPanelDelegate.addClickHandler(clickHandler);
  }

  /**
   * @param audioAnswer
   * @see ContinuousDialogRecordAudioPanel#useResult(AudioAnswer)
   */
  @Override
  public void useResult(AudioAnswer audioAnswer) {
    //  logger.info("useResult got " + audioAnswer);

    if (audioAnswer.isValid()) {
      turnFeedback.setHTML("");

      AudioAttribute audioAttribute = audioAnswer.getAudioAttribute();

      if (DEBUG) {
        String audioRef = audioAttribute.getAudioRef();
        logger.info("useResult (" + audioAttribute.getUniqueID() + ") got back " + audioRef);
      }

      rememberAudio(audioAttribute);
      clientExercise.getMutableAudio().addAudio(audioAttribute);
      recordAudioPanel.getPlayAudioPanel().setEnabled(true);

      ((Button) getPlayButton()).setType(ButtonType.SUCCESS);

      tellNetprofAudioHasChanged(audioAttribute.getUniqueID());

    } else {
      turnFeedback.setHTML(audioAnswer.getValidity().getPrompt());
    }
  }

  private void tellNetprofAudioHasChanged(int id) {
    controller.getExerciseService().refreshAudio(getExID(), new AsyncCallback<Void>() {
      @Override
      public void onFailure(Throwable caught) {
        controller.handleNonFatalError("refresh audio for " + getExID(), caught);
      }

      @Override
      public void onSuccess(Void result) {
        logger.info("tellNetprofAudioHasChanged : refreshed audio for " + getExID() + " and audio id " + id);
      }
    });
  }

  private Widget getPlayButton() {
    return recordAudioPanel.getPlayAudioPanel().getPlayButton();
  }

  @Override
  public void useInvalidResult(int exid) {
//    logger.info("show feedback about what bad happened?");
    recordAudioPanel.getPlayAudioPanel().setEnabled(false);
    ((Button) getPlayButton()).setType(ButtonType.WARNING);
  }

  private PostAudioRecordButton getPostAudioRecordButton() {
    return recordAudioPanel.getPostAudioRecordButton();
  }

  @Override
  public void addPacketValidity(Validity validity) {
    logger.info("addPacketValidity " + validity);
  }

  @Override
  public void stopRecording() {
    //logger.info("got stop recording...");
  }

  @Override
  public int getNumValidities() {
    return 0;
  }

  @Override
  public boolean isPressAndHold() {
    return true;
  }

  @Override
  public boolean isSimpleDialog() {
    return !turnContainer.isInterpreter();
  }

  @Override
  public int getVolume() {
    return turnContainer.getVolume();
  }

  @Override
  public int getDialogSessionID() {
    return 0;
  }

  @Override
  public void showNoAudioToPlay() {
    doBlinkAnimation(getPlayButton(), "blink-target");
  }

  private void doBlinkAnimation(Widget playButton, String style) {
    playButton.addStyleName(style);
    Timer timer = new Timer() {
      @Override
      public void run() {
        playButton.removeStyleName(style);
      }
    };
    timer.schedule(1000);
  }

  @Override
  public boolean isDeleting() {
    return isDeleting;
  }

  @Override
  public void setDeleting(boolean deleting) {
    isDeleting = deleting;
  }
}
