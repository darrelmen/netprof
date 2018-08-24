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
  protected final Logger logger = Logger.getLogger("HighlightSegment");

  public static final String UNDERLINE = "underline";

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
    this(id, content, HasDirection.Direction.LTR, true, true, DEFAULT_HIGHLIGHT);
  }

  /**
   * @param id
   * @param html
   * @param dir
   * @param addSouth
   * @param showPhones
   * @param highlightColor
   * @see mitll.langtest.client.scoring.ClickableWords#makeClickableText
   */
  public HighlightSegment(int id, @IsSafeHtml String html, HasDirection.Direction dir, boolean addSouth,
                          boolean showPhones, String highlightColor) {
    this.highlightColor = highlightColor;
    DivWidget north;
    add(north = new DivWidget());

    getElement().getStyle().setDisplay(Style.Display.INLINE_BLOCK);

    this.span = new HTML(html, dir);
    span.getElement().setId("highlight_" + id + "_" + html);

    this.content = html;
    boolean isLTR = dir == HasDirection.Direction.LTR;

    configureNorth(id, north, isLTR, span);
    this.north = north;

    south = new DivWidget();

    addMouseOverHandler(mouseOverEvent -> south.addStyleName(UNDERLINE));
    addMouseOutHandler(mouseOutEvent -> south.removeStyleName(UNDERLINE));

    if (addSouth) {
      add(south);
      configureSouth(id, south, isLTR, showPhones);
    }

    length = html.length();
  }

  private boolean didObscure = false;

  @Override
  public void obscureText() {
    Style style = this.span.getElement().getStyle();
    style.setColor("gray");
    style.setBackgroundColor("gray");
    didObscure = true;
  }

  @Override
  public void restoreText() {
    if (didObscure) {
      Style style = this.span.getElement().getStyle();
      style.setColor("rgb(51, 51, 51)");
      style.clearBackgroundColor();
      didObscure = false;
    } else {
//      logger.warning("no restore color?");
    }
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
   * @see #HighlightSegment(int, String, HasDirection.Direction, boolean, boolean, String)
   */
  private void configureNorth(int id, DivWidget north, boolean isLTR, Widget span) {
    north.add(span);
    north.getElement().setId("Highlight_North_" + id);

    String floatDir = isLTR ? "floatLeft" : "floatRight";
    north.addStyleName(floatDir);
    north.addStyleName(isLTR ? "wordSpacerRight" : "wordSpacerLeft");
  }

  private void configureSouth(int id, Widget south, boolean isLTR, boolean ensureHeight) {
    if (isLTR) {
      south.addStyleName("floatLeft");
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

  @Override
  public void setHighlightColor(String highlightColor) {
    this.highlightColor = highlightColor;
  }

  public String toString() {
    return //"#" + id + " " +
        "'" + content + "'" + (getLength() > 1 ? " (" + getLength() + ")" : "") + (clickable ? "" : " NC");// + " color " + colorBefore + " bkg " + bkgColorBefore;
  }
}
