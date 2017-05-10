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
  private final DivWidget north;
  private final DivWidget south;
  private final int id;
  private final String content;
  private final InlineHTML span;

  /**
   * @param id
   * @param html
   * @param dir
   * @see mitll.langtest.client.scoring.ClickableWords#makeClickableText
   */
  public HighlightSegment(int id, @IsSafeHtml String html, HasDirection.Direction dir) {
    add(north = new DivWidget());
    add(south = new DivWidget());
    south.setWidth("100%");
    south.addStyleName("floatLeft");
    getElement().getStyle().setDisplay(Style.Display.INLINE_BLOCK);
    this.span = new InlineHTML(html, dir);
    this.content = html;
    north.add(span);
    length = html.length();
    this.id = id;
  }

  /**
   * @param id
   * @param content
   * @see mitll.langtest.client.scoring.WordTable#addPhonesBelowWord2
   */
  public HighlightSegment(int id, String content) {
    this(id, content, HasDirection.Direction.LTR);
  }

  public int getLength() {
    return length;
  }

  public void setBackground(String background) {
    this.background = background;
    getSpanStyle().setBackgroundColor(background);
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

  public String toString() {
    return "'" + content + "' (" + getLength() + ")";
  }

  public void setSouth(Widget widget) {
    south.clear();
    south.add(widget);
  }
}
