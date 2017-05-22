package mitll.langtest.client.sound;

import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.dom.client.Style;
import com.google.gwt.i18n.client.HasDirection;
import com.google.gwt.safehtml.shared.annotations.IsSafeHtml;
import com.google.gwt.user.client.ui.InlineHTML;
import com.google.gwt.user.client.ui.Widget;

import java.util.logging.Logger;

/**
 * Created by go22670 on 4/25/17.
 */
public class HighlightSegment extends DivWidget implements IHighlightSegment {
  protected final Logger logger = Logger.getLogger("HighlightSegment");

  private static final String BLUE = "#2196F3";
  private final int length;
  private String background = null;
  private boolean highlighted = false;
  private boolean clickable = true;
  private final DivWidget north, south;
  private final int id;
  private final String content;
  private final InlineHTML span;

  /**
   * @param id
   * @param content
   * @see mitll.langtest.client.scoring.WordTable#addPhonesBelowWord2
   */
  public HighlightSegment(int id, String content) {
    this(id, content, HasDirection.Direction.LTR);
  }

  /**
   * @param id
   * @param html
   * @param dir
   * @see mitll.langtest.client.scoring.ClickableWords#makeClickableText
   */
  public HighlightSegment(int id, @IsSafeHtml String html, HasDirection.Direction dir) {
    DivWidget north;
    add(north = new DivWidget());

    getElement().getStyle().setDisplay(Style.Display.INLINE_BLOCK);
    this.span = new InlineHTML(html, dir);
    this.content = html;
    north.add(span);
    north.getElement().setId("Highlight_North_" + id);
    north.addStyleName("floatLeft");
    this.north = north;
    span.getElement().setId("highlight_" + id + "_" + html);

    add(south = new DivWidget());

    if (dir == HasDirection.Direction.LTR) {
      south.addStyleName("floatLeft");
    }

    south.getElement().getStyle().setClear(Style.Clear.BOTH);
    south.getElement().setId("Highlight_South_" + id);

    length = html.length();
    this.id = id;
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
  public void setBlue() {
    highlighted = true;
    getSpanStyle().setBackgroundColor(BLUE);
  }

  @Override
  public void clearBlue() {
    highlighted = false;
    if (background == null) {
      getSpanStyle().clearBackgroundColor();
    } else {
      getSpanStyle().setBackgroundColor(background);
    }
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
  public InlineHTML getClickable() {
    return span;
  }

  @Override
  public void setClickable(boolean clickable) {
    this.clickable = clickable;
  }

  @Override
  public String getContent() {
    return content;
  }

  public void setSouth(Widget widget) {
    south.clear();
    south.add(widget);
  }

  @Override
  public void clearSouth() {
    //  logger.info("clearSouth...");
    remove(south);
  }

  @Override
  public DivWidget getNorth() {
    return north;
  }

  /**
   * @return
   * @see AllHighlight#AllHighlight
   */
/*  @Override
  public DivWidget getDivParent() {
    logger.info("getDivParent " + this);
    if (this.divParent == null) logger.warning("getDivParent huh??? parent is null for " + this);
    return this.divParent;
  }

  @Override
  public void setDivParent(DivWidget horizontal) {
    logger.info("setDivParent " + this);
    if (horizontal == null) logger.warning("setDivParent huh??? parent is null for " + this);
    this.divParent = horizontal;
  }*/
  public String toString() {
    return "id " + id + " '" + content + "' (" + getLength() + ")";
  }
}
