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
import com.github.gwtbootstrap.client.ui.TextBox;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.safehtml.shared.SimpleHtmlSanitizer;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Focusable;
import com.google.gwt.user.client.ui.UIObject;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.banner.SessionManager;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.scoring.*;
import mitll.langtest.client.sound.AllHighlight;
import mitll.langtest.client.sound.IHighlightSegment;
import mitll.langtest.shared.answer.AudioAnswer;
import mitll.langtest.shared.answer.Validity;
import mitll.langtest.shared.exercise.AudioAttribute;
import mitll.langtest.shared.exercise.ClientExercise;
import mitll.langtest.shared.project.Language;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.logging.Logger;

import static mitll.langtest.client.dialog.ListenViewHelper.COLUMNS.LEFT;
import static mitll.langtest.client.dialog.ListenViewHelper.COLUMNS.MIDDLE;
import static mitll.langtest.client.dialog.ListenViewHelper.SPEAKER_A;
import static mitll.langtest.client.dialog.ListenViewHelper.SPEAKER_B;

/**
 *
 */
public class EditorTurn extends PlayAudioExercisePanel implements ITurnPanel, IRehearseView, IRecordingTurnPanel {
  private final Logger logger = Logger.getLogger("EditorTurn");

  public static final int TURN_WIDTH = 97;
  public static final int RIGHT_TURN_RIGHT_MARGIN = 153;

  public static final int HEIGHT_AND_WIDTH = 22;

  private TurnPanelDelegate turnPanelDelegate;
  private ClientExercise clientExercise;

  private Language language;
  private ExerciseController<?> controller;
  private ITurnContainer<EditorTurn> turnContainer;
  private int dialogID;
  private String prev = "";
  private ListenViewHelper.COLUMNS columns, prevColumn;
  private boolean isFirstTurn;
  private TextBox contentTextBox;
  private NoFeedbackRecordAudioPanel<ClientExercise> recordAudioPanel;

  private SessionManager sessionManager;

  private static final boolean DEBUG = false;

  /**
   * @param clientExercise
   * @param columns
   * @param prevColumn
   * @param rightJustify
   * @param controller
   * @see ListenViewHelper#makeTurnPanel(ClientExercise, ListenViewHelper.COLUMNS, ListenViewHelper.COLUMNS, boolean, int)
   */
  EditorTurn(final ClientExercise clientExercise,
             ListenViewHelper.COLUMNS columns,
             ListenViewHelper.COLUMNS prevColumn,
             boolean rightJustify,
             ExerciseController<?> controller,
             ITurnContainer<EditorTurn> turnContainer,
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

    turnPanelDelegate = new TurnPanelDelegate(clientExercise, this, columns, rightJustify) {
      @Override
      protected void addMarginStyle(Style style2) {
        style2.setMarginLeft(15, Style.Unit.PX);
        boolean useBigRightMargin = columns == ListenViewHelper.COLUMNS.RIGHT && turnContainer.isInterpreter();
        style2.setMarginRight(useBigRightMargin ? RIGHT_TURN_RIGHT_MARGIN : 10, Style.Unit.PX);
        // style2.setMarginTop(7, Style.Unit.PX);
        style2.setMarginBottom(0, Style.Unit.PX);
      }
    };

    Style style = getElement().getStyle();
    style.setProperty("minWidth", "500px");

//    setWidth((columns == MIDDLE ? 84 : 50) + "%");

    this.dialogID = dialogID;
    this.clientExercise = clientExercise;
    this.language = controller.getLanguageInfo();
    this.controller = controller;
    this.turnContainer = turnContainer;
    setId("EditorTurn_" + getExID());

    if (columns == ListenViewHelper.COLUMNS.MIDDLE) {
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
  public String getText() {
    return getExID() + " " + clientExercise.getForeignLanguage();
  }

  @Override
  public int getExID() {
    return clientExercise == null ? -1 : clientExercise.getID();
  }

  @Override
  public void clearHighlight() {
  }

  public ListenViewHelper.COLUMNS getColumn() {
    return columns;
  }

  @Override
  public void addWidgets(boolean showFL,
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
            return null;
          }

        }) {
          @Override
          protected boolean shouldAddToAudioTable() {
            return true;
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
        AudioAttribute next = clientExercise.getAudioAttributes().iterator().next();
        if (DEBUG) {
          logger.info("addWidgets :binding " + next + " to play for turn for " + getExID());
        }
        playAudioPanel.rememberAudio(next);
        playAudioPanel.setEnabled(true);
      }
      playAudioPanel.showPlayButton();

      Widget playButton = playAudioPanel.getPlayButton();

//      ((HasFocusHandlers) playButton).addFocusHandler(event -> grabFocus());
      ((Focusable) playButton).setTabIndex(-1);

      buttonContainer.add(playButton);
      playButton.addStyleName("floatRight");
      addPressAndHoldStyleForRecordButton(playButton);
      buttonContainer.getElement().getStyle().setMarginTop(3, Style.Unit.PX);
    }

    wrapper.add(buttonContainer);

    addPressAndHoldStyleForRecordButton(postAudioRecordButton);
    postAudioRecordButton.addFocusHandler(event -> grabFocus());

    DivWidget textBoxContainer = new DivWidget();

    textBoxContainer.add(contentTextBox = addTextBox());

    wrapper.add(textBoxContainer);
    styleMe(wrapper);
    wrapper.addStyleName("inlineFlex");
    add(wrapper);

    if (columns == MIDDLE || !turnContainer.isInterpreter()) {
      addStyleName("inlineFlex");
      addAddTurnButton();
      addDeleteButton();
      addOtherTurn();
    }
  }

  static final String BLUE_INACTIVE_COLOR = "#0171bc";

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

  private void addOtherTurn() {
    Button w = new Button();
    addPressAndHoldStyle(w);
    w.setType(ButtonType.INFO);

    w.addClickHandler(event -> gotOtherSpeaker());
    ListenViewHelper.COLUMNS toUseForArrow = columns;
    if (toUseForArrow == MIDDLE) {
      toUseForArrow = prevColumn;
    }
    //  logger.info("the column is " + toUseForArrow);
    w.setIcon(toUseForArrow == LEFT ? IconType.ARROW_RIGHT : IconType.ARROW_LEFT);
    w.addStyleName("topFiveMargin");
    w.addStyleName("leftFiveMargin");
    w.addFocusHandler(event -> turnContainer.moveFocusToNext());

    add(w);
  }

  private void addDeleteButton() {
    Button w = new Button();
    addPressAndHoldStyle(w);
    w.addClickHandler(event -> gotMinus());
    w.setType(ButtonType.WARNING);

    w.setIcon(IconType.MINUS);
    w.addStyleName("topFiveMargin");
    w.addStyleName("leftFiveMargin");

    // can't blow away the first turn!
    w.setEnabled(!isFirstTurn);
    w.addFocusHandler(event -> turnContainer.moveFocusToNext());

    add(w);
  }

  private void addAddTurnButton() {
    Button w = new Button();
    addPressAndHoldStyle(w);
    w.addClickHandler(event -> gotPlus());
    w.setIcon(IconType.PLUS);
    w.addStyleName("topFiveMargin");
    w.addStyleName("leftFiveMargin");
    w.setType(ButtonType.SUCCESS);
    w.addFocusHandler(event -> turnContainer.moveFocusToNext());
    add(w);
  }

  private void gotPlus() {
    turnContainer.addTurnForSameSpeaker(this);
  }

  private void gotMinus() {
    turnContainer.deleteCurrentTurnOrPair(this);
  }

  private void gotOtherSpeaker() {
    turnContainer.addTurnForOtherSpeaker(this);
  }

  private void addPressAndHoldStyle(UIObject postAudioRecordButton) {
    Style style = postAudioRecordButton.getElement().getStyle();
    style.setProperty("borderRadius", 21 + "px");
    style.setPadding(9, Style.Unit.PX);
    style.setWidth(26, Style.Unit.PX);
    style.setMarginRight(5, Style.Unit.PX);
    style.setHeight(20, Style.Unit.PX);
  }

  private void addPressAndHoldStyleForRecordButton(UIObject postAudioRecordButton) {
    Style style = postAudioRecordButton.getElement().getStyle();
    style.setProperty("borderRadius", "18px");
    style.setPadding(8, Style.Unit.PX);
    style.setWidth(19, Style.Unit.PX);
    style.setMarginRight(5, Style.Unit.PX);
    style.setHeight(19, Style.Unit.PX);
  }


  /**
   * @param wrapper
   * @see #addWidgets(boolean, boolean, PhonesChoices, EnglishDisplayChoices)
   */
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
        placeholder = (columns == LEFT ? SPEAKER_A : SPEAKER_B) + " says...";
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

  private void gotFocus() {
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
  private void gotKey(KeyUpEvent event) {
    NativeEvent ne = event.getNativeEvent();
    int keyCode = ne.getKeyCode();
    boolean isEnter = keyCode == KeyCodes.KEY_ENTER;
    String s = SimpleHtmlSanitizer.sanitizeHtml(contentTextBox.getText()).asString();
    if (isEnter) {
      ne.preventDefault();
      ne.stopPropagation();

      logger.info("got enter on " + this.getExID() + " : " + columns);

      if (s.equals(prev)) {
        turnContainer.gotForward(this);
      } else {
        prev = s;

        EditorTurn outer = this;
        //logger.info("gotBlur " + getExID() + " = " + prev);

        controller.getExerciseService().updateText(dialogID, getExID(), s, new AsyncCallback<Boolean>() {
          @Override
          public void onFailure(Throwable caught) {
            controller.handleNonFatalError("updating text...", caught);
          }

          @Override
          public void onSuccess(Boolean result) {
            //logger.info("OK, update was " + result);
            turnContainer.gotForward(outer);
          }
        });
      }
    } else {
      int length = s.split(" ").length;
      //  logger.info("num tokens " + length);

      if (length > 10) {
        contentTextBox.getElement().getStyle().setBackgroundColor("red");
      } else if (length > 7) {
        contentTextBox.getElement().getStyle().setBackgroundColor("yellow");
      } else {
        contentTextBox.getElement().getStyle().setBackgroundColor("white");
      }
    }
  }

  private void gotBlur() {
    String s = SimpleHtmlSanitizer.sanitizeHtml(contentTextBox.getText()).asString();
    if (s.equals(prev)) {
      if (DEBUG) logger.info("gotBlur " + getExID() + " skip unchanged " + prev);
    } else {
      prev = s;

      if (DEBUG) logger.info("gotBlur " + getExID() + " = " + prev);

      controller.getExerciseService().updateText(dialogID, getExID(), s, new AsyncCallback<Boolean>() {
        @Override
        public void onFailure(Throwable caught) {
          controller.handleNonFatalError("updating text...", caught);
        }

        @Override
        public void onSuccess(Boolean result) {
          //logger.info("OK, update was " + result);
        }
      });
    }
  }

  /**
   * @see DialogEditor#grabFocus
   */
  @Override
  public void grabFocus() {
    if (contentTextBox == null) {
      logger.info("grabFocus no contentTextBox yet for " + getExID());
    } else {
      //   logger.info("grabFocus on " + getExID());
      contentTextBox.setFocus(true);
    }
//    logger.info("hiddenPartner " + hiddenPartner.getOffsetWidth());
//    syncWidthOfVisible();
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

  @NotNull
  protected AllHighlight getAllHighlight(Collection<IHighlightSegment> flclickables) {
    return new AllHighlight(flclickables, !turnPanelDelegate.isMiddle());
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
   * @see ListenViewHelper#getTurnPanel
   * @param clickHandler
   */
  @Override
  public void addClickHandler(ClickHandler clickHandler) {
    turnPanelDelegate.addClickHandler(clickHandler);
  }

  @Override
  public void useResult(AudioAnswer audioAnswer) {
    logger.info("useResult got " + audioAnswer);
    if (audioAnswer.isValid()) {
      String audioRef = audioAnswer.getAudioAttribute().getAudioRef();
      logger.info("useResult got back " + audioRef);
      rememberAudio(audioAnswer.getAudioAttribute());
      recordAudioPanel.getPlayAudioPanel().setEnabled(true);

      ((Button) getPlayButton()).setType(ButtonType.SUCCESS);
      //  doBlinkAnimation(getPlayButton(), "good-blink-target");
      // getPlayButton().getElement().getStyle().setBackgroundColor("green");
    }
  }

  private Widget getPlayButton() {
    return recordAudioPanel.getPlayAudioPanel().getPlayButton();
  }

  @Override
  public void useInvalidResult(int exid) {
//    logger.info("show feedback about what bad happened?");
    recordAudioPanel.getPlayAudioPanel().setEnabled(false);
//    doBlinkAnimation(getPostAudioRecordButton(), "blink-target");
    ((Button) getPlayButton()).setType(ButtonType.WARNING);

//    getPlayButton().getElement().getStyle().setBackgroundColor("red");
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

}
