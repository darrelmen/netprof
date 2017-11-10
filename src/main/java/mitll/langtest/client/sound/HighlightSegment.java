package mitll.langtest.client.sound;

import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.dom.client.Style;
import com.google.gwt.i18n.client.HasDirection;
import com.google.gwt.safehtml.shared.annotations.IsSafeHtml;
import com.google.gwt.user.client.Element;
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
  private final String content;
  private final InlineHTML span;

  /**
   * @param id
   * @param content
   * @see mitll.langtest.client.scoring.WordTable#addPhonesBelowWord2
   */
  public HighlightSegment(int id, String content) {
    this(id, content, HasDirection.Direction.LTR, true, true);
  }

  /**
   * @param id
   * @param html
   * @param dir
   * @param addSouth
   * @param showPhones
   * @see mitll.langtest.client.scoring.ClickableWords#makeClickableText
   */
  public HighlightSegment(int id, @IsSafeHtml String html, HasDirection.Direction dir, boolean addSouth, boolean showPhones) {
    DivWidget north;
    add(north = new DivWidget());

    getElement().getStyle().setDisplay(Style.Display.INLINE_BLOCK);

    this.span = new InlineHTML(html, dir);
    span.getElement().setId("highlight_" + id + "_" + html);

    this.content = html;
    boolean isLTR = dir == HasDirection.Direction.LTR;

    configureNorth(id, north, isLTR, span);
    this.north = north;

    if (addSouth) {
      add(south = new DivWidget());
      configureSouth(id, south, isLTR, showPhones);
    } else {
      south = null;
    }
    length = html.length();
  }

  private void configureSouth(int id, DivWidget south, boolean isLTR, boolean ensureHeight) {
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

  private void configureNorth(int id, DivWidget north, boolean isLTR, InlineHTML span) {
    north.add(span);
    north.getElement().setId("Highlight_North_" + id);

    String floatDir = isLTR ? "floatLeft" : "floatRight";
    north.addStyleName(floatDir);

    if (isLTR) {
      north.addStyleName("wordSpacerRight");
    } else {
      north.addStyleName("wordSpacerLeft");
      north.addStyleName("rtlMarginBottom");
    }
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

  public String toString() {
    return //"#" + id + " " +
        "'" + content + "'" + (getLength() > 1 ? " (" + getLength() + ")" : "") + (clickable ? "" : " NC");
  }
}
