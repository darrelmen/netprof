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

import com.github.gwtbootstrap.client.ui.TextBox;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.safehtml.shared.SimpleHtmlSanitizer;
import com.google.gwt.user.client.rpc.AsyncCallback;
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
  ExerciseController<?> controller;
  ITurnContainer turnContainer;

  /**
   * @param clientExercise
   * @param columns
   * @param rightJustify
   * @param language
   * @param controller
   * @see DialogEditor#makeTurnPanel(ClientExercise, ListenViewHelper.COLUMNS, boolean)
   */
  public EditorTurn(final ClientExercise clientExercise, ListenViewHelper.COLUMNS columns,
                    boolean rightJustify, Language language, ExerciseController<?> controller,
                    ITurnContainer turnContainer) {
    turnPanelDelegate = new TurnPanelDelegate(clientExercise, this, columns, rightJustify);
    this.clientExercise = clientExercise;
    this.language = language;
    this.controller = controller;
    this.turnContainer = turnContainer;
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

  @Override
  public void addWidgets(boolean showFL, boolean showALTFL, PhonesChoices phonesChoices,
                         EnglishDisplayChoices englishDisplayChoices) {
    DivWidget wrapper = new DivWidget();

    {

      // TODO : instead, make this a div contenteditable!
      TextBox w = new TextBox();
      this.content = w;

      String foreignLanguage = clientExercise.getForeignLanguage();
      if (foreignLanguage.isEmpty()) {
        w.setPlaceholder(clientExercise.hasEnglishAttr() ? "English..." : language.toDisplay() + "...");
      } else {
        w.setText(foreignLanguage);
      }
      w.addBlurHandler(event -> gotBlur());
      w.addKeyUpHandler(this::gotKey);
      w.addStyleName("leftTenMargin");
      w.addStyleName("rightTenMargin");
      w.addStyleName("topFiveMargin");
      wrapper.add(w);
    }

    styleMe(wrapper);
    add(wrapper);
  }

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
    String s = SimpleHtmlSanitizer.sanitizeHtml(content.getText()).asString();
    controller.getExerciseService().updateText(getExID(), s, new AsyncCallback<Boolean>() {
      @Override
      public void onFailure(Throwable caught) {

      }

      @Override
      public void onSuccess(Boolean result) {
        logger.info("OK, update was " + result);
      }
    });
  }

  @Override
  public void grabFocus() {
    content.setFocus(true);
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
