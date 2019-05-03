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
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FocusWidget;
import com.google.gwt.user.client.ui.UIObject;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.scoring.*;
import mitll.langtest.client.sound.AllHighlight;
import mitll.langtest.client.sound.IHighlightSegment;
import mitll.langtest.client.sound.PlayListener;
import mitll.langtest.shared.exercise.ClientExercise;
import mitll.langtest.shared.project.Language;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.logging.Logger;

public class EditorTurn extends DivWidget implements ITurnPanel {
  private final Logger logger = Logger.getLogger("EditorTurn");

  private TurnPanelDelegate turnPanelDelegate;
  private ClientExercise clientExercise;

  private Language language;
  private ExerciseController<?> controller;
  private ITurnContainer<EditorTurn> turnContainer;
  private int dialogID;
  private String prev = "";
  private ListenViewHelper.COLUMNS columns;

  /**
   * @param clientExercise
   * @param columns
   * @param rightJustify
   * @param language
   * @param controller
   * @see DialogEditor#makeTurnPanel(ClientExercise, ListenViewHelper.COLUMNS, boolean)
   */
  EditorTurn(final ClientExercise clientExercise,
             ListenViewHelper.COLUMNS columns,
             boolean rightJustify,
             Language language,
             ExerciseController<?> controller,
             ITurnContainer<EditorTurn> turnContainer,
             int dialogID) {
    logger.info("turn " + dialogID + " : " + clientExercise.getID() + " : '" + clientExercise.getForeignLanguage() + "' has english " + clientExercise.hasEnglishAttr());

    this.columns = columns;

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
    this.language = language;
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
    return clientExercise.getID();
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

  private TextBox content;

  public ListenViewHelper.COLUMNS getColumns() {
    return columns;
  }

  @Override
  public void addWidgets(boolean showFL, boolean showALTFL, PhonesChoices phonesChoices,
                         EnglishDisplayChoices englishDisplayChoices) {
    DivWidget wrapper = new DivWidget();
    wrapper.getElement().setId("Wrapper_" + getExID());
    tryOne(wrapper);

/*
    //DivWidget w = new DivWidget();
    FocusWidget w = new MyFocus(){};

//    w.getElement().getStyle().setProperty("contenteditable", "true");
    this.content = w;

    String foreignLanguage = clientExercise.getForeignLanguage();
    if (foreignLanguage.isEmpty()) {
      String s = clientExercise.hasEnglishAttr() ? "English..." : language.toDisplay() + " translation.";
      PlaceholderHelper placeholderHelper = GWT.create(PlaceholderHelper.class);
      placeholderHelper.setPlaceholer(w.getElement(), s);
//      w.setPlaceholder(s);
    } else {
//      w.setText(foreignLanguage);
      w.getElement().setInnerText(foreignLanguage);
      prev = foreignLanguage;
    }
    w.addBlurHandler(event -> gotBlur());

//    w.addDomHandler(event -> gotBlur(), BlurEvent.getType());

    w.addKeyUpHandler(this::gotKey);
//    addDomHandler(this::gotKey, KeyUpEvent.getType());
    w.addStyleName("leftTenMargin");
    w.addStyleName("rightTenMargin");
    w.addStyleName("topFiveMargin");
    wrapper.add(w);
*/

    styleMe(wrapper);
    add(wrapper);

    if (columns == ListenViewHelper.COLUMNS.MIDDLE) {
      addStyleName("inlineFlex");

      {
        Button w = new Button();
        addPressAndHoldStyle(w);
        w.addClickHandler(event -> gotPlus());
        w.setIcon(IconType.PLUS);
        w.addStyleName("topFiveMargin");
        w.addStyleName("leftFiveMargin");
        w.setType(ButtonType.SUCCESS);
        //   w.getElement().getStyle().setBackgroundColor("#0171bc");
        add(w);
      }

      {
        Button w = new Button();
        addPressAndHoldStyle(w);
        w.addClickHandler(event -> gotMinus());

        w.setIcon(IconType.MINUS);
        w.addStyleName("topFiveMargin");
        w.addStyleName("leftFiveMargin");

        //turnContainer.
        //  w.getElement().getStyle().setBackgroundColor("#0171bc");
        add(w);
      }

      {
        Button w = new Button();
        addPressAndHoldStyle(w);

        w.addClickHandler(event -> gotOtherSpeaker());

        w.setIcon(IconType.ARROW_RIGHT);
        w.addStyleName("topFiveMargin");
        w.addStyleName("leftFiveMargin");
        //  w.getElement().getStyle().setBackgroundColor("#0171bc");
        add(w);
      }
    }

  }

  private void gotPlus() {
    turnContainer.addTurnForSameSpeaker();
  }

  private void gotMinus() {
    turnContainer.deleteCurrentTurnOrPair();
  }

  private void gotOtherSpeaker() {
  turnContainer.addTurnForOtherSpeaker();
  }

  private void addPressAndHoldStyle(UIObject postAudioRecordButton) {
    Style style = postAudioRecordButton.getElement().getStyle();
    style.setProperty("borderRadius", "18px");
    style.setPadding(8, Style.Unit.PX);
    style.setWidth(19, Style.Unit.PX);
    style.setMarginRight(5, Style.Unit.PX);
    style.setHeight(19, Style.Unit.PX);
  }

  private void tryOne(DivWidget wrapper) {
    // TODO : instead, make this a div contenteditable!
    TextBox w = new TextBox();

    w.getElement().getStyle().setFontSize(16, Style.Unit.PX);

    w.setId("TextBox_" + getExID());
    w.setWidth("92%");
    this.content = w;

    String foreignLanguage = clientExercise.getForeignLanguage();
    if (foreignLanguage.isEmpty()) {
      w.setPlaceholder(clientExercise.hasEnglishAttr() ? "English..." : language.toDisplay() + " translation.");
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

/*    DivWidget spanContainer = new DivWidget();
    this.spanContainer = spanContainer;
    spanContainer.getElement().getStyle().setOverflow(Style.Overflow.HIDDEN);
    //  spanContainer.addStyleName("floatLeft");
    FlowPanel span = new FlowPanel("span") {
      @Override
      protected void onLoad() {
        super.onLoad();
        logger.info("onLoad width is " + getOffsetWidth());
      }

      @Override
      protected void onAttach() {
        super.onAttach();
        logger.info("onAttach width is " + getOffsetWidth());

      }
    };
    hiddenPartner = span;
    spanContainer.add(span);
    span.setHeight("5px");
//      span.getElement().getStyle().setMarginLeft(10, Style.Unit.PX);
    // span.getElement().getStyle().setProperty("display", "inherit");
    //    span.getElement().getStyle().setDisplay(Style.Display.BLOCK);
    w.addKeyUpHandler(event -> {
      span.getElement().setInnerHTML(w.getText());
      syncWidthOfVisible();
    });

    span.getElement().setInnerHTML(w.getText());*/

    //   Scheduler.get().scheduleDeferred((Command) () -> w.setWidth(span.getOffsetWidth() + "px"));

    //  add(spanContainer);
  }

  private void gotFocus() {
    logger.info("gotFocus " + getExID());
    turnContainer.setCurrentTurnTo(this);
  }

  /*public void syncWidthOfVisible() {
    content.setWidth(hiddenPartner.getOffsetWidth() + "px");
  }*/

  private void gotKey(KeyUpEvent event) {
    NativeEvent ne = event.getNativeEvent();
    int keyCode = ne.getKeyCode();
    boolean isEnter = keyCode == KeyCodes.KEY_ENTER;
    if (isEnter) {
      ne.preventDefault();
      ne.stopPropagation();

      logger.info("got enter!");

      turnContainer.gotForward();
//      userHitEnterKey(button);
    }
  }


  private void gotBlur() {
    // String s = SimpleHtmlSanitizer.sanitizeHtml(content.getElement().getInnerText()).asString();
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
          logger.info("OK, update was " + result);
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
    //super.styleMe(wrapper);
    turnPanelDelegate.styleMe(wrapper);
    wrapper.setWidth("98%");
    //flClickableRow.getElement().getStyle().setOverflow(Style.Overflow.HIDDEN);
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
