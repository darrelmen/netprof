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

package mitll.langtest.client.sound;

import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.i18n.client.HasDirection;
import com.google.gwt.safehtml.shared.annotations.IsSafeHtml;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Widget;

import java.util.logging.Logger;

/**
 * Created by go22670 on 4/25/17.
 */
public class HighlightSegment extends DivWidget implements IHighlightSegment {
  public static final String FLOAT_RIGHT = "floatRight";
  protected final Logger logger = Logger.getLogger("HighlightSegment");

  public static final String RGB_51_51_51 = "rgb(51, 51, 51)";
  private static final String FLOAT_LEFT = "floatLeft";
  private static final String INLINE_BLOCK_STYLE_ONLY = "inlineBlockStyleOnly";

  private static final String UNDERLINE = "underline";

  private String highlightColor;
  private final int length;
  private String background = null;
  private boolean highlighted = false;
  private boolean clickable = true;

  private final DivWidget north;
  private DivWidget south;
  private final String content;
  private final HTML span;

  /**
   * @param id
   * @param content
   * @see mitll.langtest.client.scoring.WordTable#getWordLabel
   */
  public HighlightSegment(int id, String content) {
    this(id, content, HasDirection.Direction.LTR, true, true, DEFAULT_HIGHLIGHT, true);
  }

  /**
   * @param id
   * @param html
   * @param dir
   * @param addSouth
   * @param showPhones
   * @param highlightColor
   * @param addFloatLeft
   * @see mitll.langtest.client.scoring.ClickableWords#makeClickableText
   */
  public HighlightSegment(int id, @IsSafeHtml String html, HasDirection.Direction dir, boolean addSouth,
                          boolean showPhones, String highlightColor, boolean addFloatLeft) {
    this.highlightColor = highlightColor;
    DivWidget north;
    add(north = new DivWidget());

    getElement().getStyle().setDisplay(Style.Display.INLINE_BLOCK);

    this.span = new HTML(html, dir);

    this.content = html;
    boolean isLTR = dir == HasDirection.Direction.LTR;
    span.getElement().setId("highlight_" + id + "_" + html +"_dir_"+dir);

    configureNorth(id, north, isLTR, span, addFloatLeft);
    this.north = north;

    south = new DivWidget();

    addMouseOverHandler(mouseOverEvent -> south.addStyleName(UNDERLINE));
    addMouseOutHandler(mouseOutEvent -> south.removeStyleName(UNDERLINE));

    if (addSouth) {
      add(south);
      configureSouth(id, south, isLTR, showPhones, addFloatLeft);
    }

    length = html.length();
  }

  private boolean shouldObscure = false;
  private boolean didObscure = false;

  @Override
  public void setObscurable() {
    shouldObscure = true;
  }

  @Override
  public boolean obscureText() {
    if (shouldObscure) {
      Style style = this.span.getElement().getStyle();
      style.setColor("gray");
      style.setBackgroundColor("gray");

     // logger.info("obscureText did obscure on " +this);
      didObscure = true;
    }
    return didObscure;
  }

  @Override
  public void restoreText() {
    if (shouldObscure && didObscure) {
    //  logger.info("restoreText on " +this);
      Style style = this.span.getElement().getStyle();
      style.setColor(RGB_51_51_51);
      style.clearBackgroundColor();
      didObscure = false;
    }
    //else {
//      logger.warning("no restore color?");
   // }
  }

  private HandlerRegistration addMouseOutHandler(MouseOutHandler handler) {
    return addDomHandler(handler, MouseOutEvent.getType());
  }

  private HandlerRegistration addMouseOverHandler(MouseOverHandler handler) {
    return addDomHandler(handler, MouseOverEvent.getType());
  }

  /**
   * @param id
   * @param north
   * @param isLTR
   * @param span
   * @param addFloatLeft
   * @see #HighlightSegment(int, String, HasDirection.Direction, boolean, boolean, String, boolean)
   */
  private void configureNorth(int id, DivWidget north, boolean isLTR, Widget span, boolean addFloatLeft) {
    north.add(span);
    north.getElement().setId("Highlight_North_" + id);

    if (isLTR && addFloatLeft) {
      north.addStyleName(FLOAT_LEFT);
    } else {
      north.addStyleName(INLINE_BLOCK_STYLE_ONLY);
    }
    north.addStyleName(isLTR ? "wordSpacerRight" : "wordSpacerLeft");
  }

  private void configureSouth(int id, Widget south, boolean isLTR, boolean ensureHeight, boolean addFloatLeft) {
    if (isLTR && addFloatLeft) {
      south.addStyleName(FLOAT_LEFT);
    }
    Element element = south.getElement();
    Style style = element.getStyle();
    style.setClear(Style.Clear.BOTH);

    if (ensureHeight)
      style.setHeight(20, Style.Unit.PX); // if all the phones are there but one, make sure it has height

    element.setId("Highlight_South_" + id);
  }

  public int getLength() {
    return length;
  }

  public void setBackground(String background) {
    this.background = background;
    getSpanStyle().setBackgroundColor(background);
  }

  @Override
  public String getID() {
    return span.getElement().getId();
  }

  @Override
  public void showHighlight() {
    highlighted = true;
    getSpanStyle().setBackgroundColor(highlightColor);
    if (didObscure) {
      getSpanStyle().setColor(highlightColor);
    }
  }

  /**
   * @param highlightColor
   * @see mitll.langtest.client.scoring.RecordDialogExercisePanel#showWordScore
   */
  @Override
  public void setHighlightColor(String highlightColor) {
    this.highlightColor = highlightColor;
  }

  @Override
  public void clearHighlight() {
    highlighted = false;
    if (background == null) {
      if (!didObscure) {
        getSpanStyle().clearBackgroundColor();
      }
    } else {
      getSpanStyle().setBackgroundColor(background);
    }
  }

  @Override
  public void checkClearHighlight() {
    if (isHighlighted()) clearHighlight();
  }

  private Style getSpanStyle() {
    return span.getElement().getStyle();
  }

  @Override
  public boolean isHighlighted() {
    return highlighted;
  }

  @Override
  public boolean isClickable() {
    return clickable;
  }

  @Override
  public HTML getClickable() {
    return span;
  }

  /**
   * @param clickable
   * @see mitll.langtest.client.scoring.ClickableWords#makeClickableText
   */
  @Override
  public void setClickable(boolean clickable) {
    this.clickable = clickable;
  }

  @Override
  public String getContent() {
    return content;
  }

  public void setSouth(DivWidget widget) {
    south = widget;

    south.addDomHandler(event -> addStyleName(UNDERLINE), MouseOverEvent.getType());
    south.addDomHandler(event -> removeStyleName(UNDERLINE), MouseOutEvent.getType());
  }

  public void setSouthScore(DivWidget widget) {
    south.clear();
    south.add(widget);
  }

  @Override
  public void clearSouth() {
    remove(south);
  }

  @Override
  public DivWidget getNorth() {
    return north;
  }


  public String toString() {
    return //"#" + id + " " +
        "'" + content + "'" + (getLength() > 1 ? " (" + getLength() + ")" : "") + (clickable ? "" : " NC");// + " color " + colorBefore + " bkg " + bkgColorBefore;
  }
}
