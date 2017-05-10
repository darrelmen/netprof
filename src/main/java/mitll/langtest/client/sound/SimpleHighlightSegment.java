package mitll.langtest.client.sound;

import com.google.gwt.safehtml.shared.annotations.IsSafeHtml;
import com.google.gwt.user.client.ui.InlineHTML;
import com.google.gwt.user.client.ui.Widget;

import java.util.logging.Logger;

/**
 * Created by go22670 on 5/10/17.
 */
public class SimpleHighlightSegment extends InlineHTML implements IHighlightSegment {
  protected final Logger logger = Logger.getLogger("HighlightSegment");

  private static final String BLUE = "#2196F3";
  private final int length;
  private String background = null;
  private boolean highlighted = false;
  private boolean clickable = true;

  public SimpleHighlightSegment(@IsSafeHtml String html, Direction dir) {
    super(html, dir);
    length = html.length();
  }

  /**
   * @param content
   * @see mitll.langtest.client.scoring.WordTable#addPhonesBelowWord2
   */
  public SimpleHighlightSegment(String content) {
    super(content);
    this.length = content.length();
  }

  public int getLength() {
    return length;
  }

  @Override
  public void setBackground(String background) {
    this.background = background;
    getElement().getStyle().setBackgroundColor(background);
  }

  @Override
  public void setBlue() {
    highlighted = true;
    getElement().getStyle().setBackgroundColor(BLUE);
  }

  @Override
  public void clearBlue() {
    highlighted = false;
    if (background == null) {
      getElement().getStyle().clearBackgroundColor();
    } else {
      getElement().getStyle().setBackgroundColor(background);
    }
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
    return this;
  }

  public void setClickable(boolean clickable) {
    this.clickable = clickable;
  }

  @Override
  public String getContent() {
    return getHTML();
  }

  public void setSouth(Widget widget) {
  }

  public String toString() {
    return "segment (" + getHTML() + ") " + getLength() + " long";
  }
}
