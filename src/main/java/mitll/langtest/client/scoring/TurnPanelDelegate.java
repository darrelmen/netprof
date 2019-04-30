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

package mitll.langtest.client.scoring;

import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.dialog.ListenViewHelper;
import mitll.langtest.client.sound.AllHighlight;
import mitll.langtest.client.sound.IHighlightSegment;
import mitll.langtest.shared.exercise.ClientExercise;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * Does left, right, or middle justify
 *
 * @param <T>
 * @see ListenViewHelper#reallyGetTurnPanel
 */
public class TurnPanelDelegate implements ITurnMarking {
  //private final Logger logger = Logger.getLogger("TurnPanel");
  private static final String FLOAT_LEFT = "floatLeft";

  private final ListenViewHelper.COLUMNS columns;
  private DivWidget bubble;
  private static final String HIGHLIGHT_COLOR = "green";
  private boolean rightJustify;

  private ClientExercise exercise;
  private DivWidget widget;

  /**
   * @param clientExercise
   * @param controller
   * @param listContainer
   * @param alignments
   * @param listenView
   * @param columns
   * @param rightJustify
   * @see ListenViewHelper#reallyGetTurnPanel
   */
  protected TurnPanelDelegate(final ClientExercise clientExercise,
                           DivWidget widget,
                           ListenViewHelper.COLUMNS columns, boolean rightJustify) {
    this.exercise = clientExercise;
    this.columns = columns;
    this.rightJustify = rightJustify;
    this.widget = widget;
    Style style = widget.getElement().getStyle();
    style.setOverflow(Style.Overflow.HIDDEN);
    style.setClear(Style.Clear.BOTH);

    if (columns == ListenViewHelper.COLUMNS.RIGHT) widget.addStyleName("floatRight");
    else if (columns == ListenViewHelper.COLUMNS.LEFT) widget.addStyleName(FLOAT_LEFT);
  }

  /**
   * @param wrapper
   * @see RefAudioGetter#addWidgets(boolean, boolean, PhonesChoices, EnglishDisplayChoices)
   */
  public void styleMe(DivWidget wrapper) {
    this.bubble = wrapper;
    wrapper.getElement().setId("bubble_" + exercise.getID());
    wrapper.addStyleName("bubble");

    // decide on left right or middle justify
    {
      if (columns == ListenViewHelper.COLUMNS.LEFT) wrapper.addStyleName("leftbubble");
      else if (columns == ListenViewHelper.COLUMNS.RIGHT) wrapper.addStyleName("rightbubble");
      else if (columns == ListenViewHelper.COLUMNS.MIDDLE) {
        Style style = wrapper.getElement().getStyle();

        String middlebubble2 = "middlebubble2";
        if (exercise.hasEnglishAttr()) {
          middlebubble2 = "middlebubbleRight";
          if (rightJustify) {
            style.setProperty("marginLeft", "auto");
          }
        }

        wrapper.addStyleName(middlebubble2);
        style.setTextAlign(Style.TextAlign.CENTER);
      }
    }

    // flClickableRow.getElement().getStyle().setOverflow(Style.Overflow.HIDDEN);
    addMarginStyle(widget.getElement().getStyle());
  }

  protected void addMarginStyle(Style style2) {
    style2.setMarginLeft(15, Style.Unit.PX);
    style2.setMarginRight(10, Style.Unit.PX);
    style2.setMarginTop(7, Style.Unit.PX);
    style2.setMarginBottom(0, Style.Unit.PX);
  }

  //@Override
  @Override
  public void addFloatLeft(Widget w) {
    if (shouldAddFloatLeft()) {
      w.addStyleName(FLOAT_LEFT);
    }
  }

  //@Override
  @Override
  public boolean shouldAddFloatLeft() {
    return (columns != ListenViewHelper.COLUMNS.MIDDLE);
  }

  @NotNull
  protected AllHighlight getAllHighlight(Collection<IHighlightSegment> flclickables) {
    return new AllHighlight(flclickables, columns != ListenViewHelper.COLUMNS.MIDDLE);
  }

  @Override
  public boolean isMiddle() {
    return columns == ListenViewHelper.COLUMNS.MIDDLE;
  }

  @Override
  public boolean isLeft() {
    return columns == ListenViewHelper.COLUMNS.LEFT;
  }

  @Override
  public boolean isRight() {
    return columns == ListenViewHelper.COLUMNS.RIGHT;
  }

  /**
   * @see ListenViewHelper#removeMarkCurrent
   */
  public void removeMarkCurrent() {
    setBorderColor("white");
  }

  /**
   * @see ListenViewHelper#markCurrent
   */
  public void markCurrent() {
    setBorderColor(HIGHLIGHT_COLOR);
  }

  public boolean hasCurrentMark() {
    return bubble.getElement().getStyle().getBorderColor().equalsIgnoreCase(HIGHLIGHT_COLOR);
  }

  private void setBorderColor(String white) {
    bubble.getElement().getStyle().setBorderColor(white);
  }

  public void makeVisible() {
    widget.getElement().scrollIntoView();
  }

  @Override
  public void addClickHandler(ClickHandler clickHandler) {
    widget.addDomHandler(clickHandler, ClickEvent.getType());
  }
}
