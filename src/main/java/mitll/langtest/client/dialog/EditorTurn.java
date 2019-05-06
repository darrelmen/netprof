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
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.safehtml.shared.SimpleHtmlSanitizer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.UIObject;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.RecordAudioPanel;
import mitll.langtest.client.scoring.*;
import mitll.langtest.client.sound.AllHighlight;
import mitll.langtest.client.sound.IHighlightSegment;
import mitll.langtest.client.sound.PlayListener;
import mitll.langtest.shared.answer.AudioType;
import mitll.langtest.shared.exercise.ClientExercise;
import mitll.langtest.shared.project.Language;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.logging.Logger;

import static mitll.langtest.client.dialog.ListenViewHelper.COLUMNS.LEFT;
import static mitll.langtest.client.dialog.ListenViewHelper.COLUMNS.MIDDLE;
import static mitll.langtest.client.dialog.ListenViewHelper.SPEAKER_A;
import static mitll.langtest.client.dialog.ListenViewHelper.SPEAKER_B;


public class EditorTurn extends DivWidget implements ITurnPanel {
  private final Logger logger = Logger.getLogger("EditorTurn");

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
  private TextBox content;
  private NoFeedbackRecordAudioPanel<ClientExercise> recordAudioPanel;

 // private RecordAudioPanel recordAudioPanel;

  private static final boolean DEBUG = false;

  /**
   * @param clientExercise
   * @param columns
   * @param prevColumn
   * @param rightJustify
   * @param controller
   * @see ListenViewHelper#makeTurnPanel(ClientExercise, ListenViewHelper.COLUMNS, ListenViewHelper.COLUMNS, boolean)
   */
  EditorTurn(final ClientExercise clientExercise,
             ListenViewHelper.COLUMNS columns,
             ListenViewHelper.COLUMNS prevColumn,
             boolean rightJustify,
             ExerciseController<?> controller,
             ITurnContainer<EditorTurn> turnContainer,
             int dialogID,
             boolean isFirstTurn) {
    if (DEBUG) {
      logger.info("turn " + dialogID + " : " + clientExercise.getID() + " : '" + clientExercise.getForeignLanguage() + "' has english " + clientExercise.hasEnglishAttr() + " : " + columns);
    }

    this.columns = columns;

    this.prevColumn = prevColumn;

    this.isFirstTurn = isFirstTurn;
    //addStyleName("flfont");

    turnPanelDelegate = new TurnPanelDelegate(clientExercise, this, columns, rightJustify) {
      @Override
      protected void addMarginStyle(Style style2) {
        style2.setMarginLeft(15, Style.Unit.PX);
        style2.setMarginRight(10, Style.Unit.PX);
        // style2.setMarginTop(7, Style.Unit.PX);
        style2.setMarginBottom(0, Style.Unit.PX);
      }
    };


    setWidth("50%");
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
        getElement().getStyle().setClear(Style.Clear.BOTH);
      }
    }
  }

  @Override
  public int getExID() {
    return clientExercise == null ? -1 : clientExercise.getID();
  }

  @Override
  public void addPlayListener(PlayListener playListener) {

  }

  @Override
  public boolean doPause() {
    return false;
  }

  @Override
  public void resetAudio() {

  }

  @Override
  public boolean isPlaying() {
    return false;
  }

  @Override
  public void clearHighlight() {

  }

  @Override
  public boolean doPlayPauseToggle() {
    return false;
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
        new ContinuousDialogRecordAudioPanel(clientExercise, controller, null, null, new IRecordResponseListener() {
          @Override
          public void usePartial(StreamResponse response) {

          }

          @Override
          public Widget myGetPopupTargetWidget() {
            return null;
          }
        });


  //  RecordAudioPanel recordAudioPanel = new RecordAudioPanel(clientExercise, controller, this, 0, false, AudioType.REGULAR, true);
//    SimpleRecordAudioPanel<ClientExercise> recordPanel =
//        new SimpleRecordAudioPanel<>(controller, clientExercise, null, true, null, null);
    this.recordAudioPanel = recordPanel;


  recordPanel.addWidgets();

    // DivWidget flContainer = getHorizDiv();

//    if (isRight()) {
//      addStyleName("floatRight");
//    } else if (isLeft()) {
//      flContainer.addStyleName("floatLeft");
//    }

    PostAudioRecordButton postAudioRecordButton = null;
    DivWidget buttonContainer = new DivWidget();
    buttonContainer.setId("recordButtonContainer_" + getExID());
buttonContainer.add(recordAudioPanel);
    // add  button
    if (true) {
      {
//        postAudioRecordButton = getPostAudioWidget(recordPanel, true);
//        buttonContainer.add(postAudioRecordButton);
//        RecorderPlayAudioPanel playAudioPanel = recordPanel.getPlayAudioPanel();
//        playAudioPanel.showPlayButton();
//        buttonContainer.add(playAudioPanel.getPlayButton());
        // buttonContainer.add(recordPanel.getPl );
        buttonContainer.getElement().getStyle().setMarginTop(3, Style.Unit.PX);
      }
//      {
//        Emoticon w = getEmoticonPlaceholder();
//        emoticon = w;
//        flContainer.add(w);
//      }
    }

    // add(flContainer);

    wrapper.add(buttonContainer);


    if (postAudioRecordButton != null) {
      addPressAndHoldStyle(postAudioRecordButton);
    }

    DivWidget textBoxContainer = new DivWidget();

    addTextBox(textBoxContainer);
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

  private void addPressAndHoldStyle(PostAudioRecordButton postAudioRecordButton) {
    Style style = postAudioRecordButton.getElement().getStyle();
    style.setProperty("borderRadius", "18px");
    style.setPadding(8, Style.Unit.PX);
    style.setWidth(19, Style.Unit.PX);
    style.setMarginRight(5, Style.Unit.PX);
    style.setHeight(19, Style.Unit.PX);
  }

  public void cancelRecording() {
    recordAudioPanel.cancelRecording();
  }

  /**
   * @return
   * @see RefAudioGetter#addWidgets(boolean, boolean, PhonesChoices, EnglishDisplayChoices)
   */
  @NotNull
  private DivWidget getHorizDiv() {
    DivWidget flContainer = new DivWidget();
    flContainer.addStyleName("inlineFlex");
    Style style = flContainer.getElement().getStyle();
    style.setMarginTop(15, Style.Unit.PX);

    if (isMiddle() && clientExercise.hasEnglishAttr()) {
      style.setProperty("marginLeft", "auto");
    }
//    else {
//      logger.info("setmargin NOT left  auto on " + getExID());
//    }

    flContainer.getElement().setId("RecordDialogExercisePanel_horiz");
    return flContainer;
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

  /**
   * @param wrapper
   * @see #addWidgets(boolean, boolean, PhonesChoices, EnglishDisplayChoices)
   */
  private void addTextBox(DivWidget wrapper) {
    // TODO : instead, make this a div contenteditable!
    TextBox w = new TextBox();

    w.getElement().getStyle().setFontSize(16, Style.Unit.PX);

    w.setId("TextBox_" + getExID());
    w.setWidth(88 + "%");
    this.content = w;

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

    //w.setWidth("50%");
    wrapper.add(w);
  }

  private void gotFocus() {
    logger.info("gotFocus " + getExID());
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
    String s = SimpleHtmlSanitizer.sanitizeHtml(content.getText()).asString();
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

          }

          @Override
          public void onSuccess(Boolean result) {
            //logger.info("OK, update was " + result);
            turnContainer.gotForward(outer);
          }
        });
      }
    } else if (isMiddle()) {
      int length = s.split(" ").length;
      //  logger.info("num tokens " + length);

      if (length > 10) {
        content.getElement().getStyle().setBackgroundColor("red");
      } else if (length > 7) {
        content.getElement().getStyle().setBackgroundColor("yellow");
      } else {
        content.getElement().getStyle().setBackgroundColor("white");
      }
    }
  }

  private void gotBlur() {
    String s = SimpleHtmlSanitizer.sanitizeHtml(content.getText()).asString();
    if (s.equals(prev)) {
      logger.info("gotBlur " + getExID() + " skip unchanged " + prev);
    } else {
      prev = s;

      logger.info("gotBlur " + getExID() + " = " + prev);

      controller.getExerciseService().updateText(dialogID, getExID(), s, new AsyncCallback<Boolean>() {
        @Override
        public void onFailure(Throwable caught) {

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
    if (content == null) {
      logger.info("grabFocus no content yet for " + getExID());
    } else {
      //   logger.info("grabFocus on " + getExID());
      content.setFocus(true);
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
    wrapper.setWidth("98%");
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

  @Override
  public void addClickHandler(ClickHandler clickHandler) {
    turnPanelDelegate.addClickHandler(clickHandler);
  }
}
