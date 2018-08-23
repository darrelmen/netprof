package mitll.langtest.client.sound;

import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.InlineHTML;

import java.util.logging.Logger;

/**
 * Created by go22670 on 5/10/17.
 */
public class SimpleHighlightSegment extends InlineHTML implements IHighlightSegment {
  protected final Logger logger = Logger.getLogger("HighlightSegment");
  /**
   * @see #showHighlight
   */
  // private static final String DEFAULT_HIGHLIGHT = "#2196F3";

  private String highlightColor;
  private final int length;
  private String background = null;
  private boolean highlighted = false;
  private boolean clickable = true;

  /**
   * @param content
   * @param highlightColor
   * @seex mitll.langtest.client.scoring.ClickableWords#makeClickableText
   * @see mitll.langtest.client.scoring.WordTable#addPhonesBelowWord2
   */
  public SimpleHighlightSegment(String content, String highlightColor) {
    super(content);
    this.highlightColor = highlightColor;
    //getElement().setId(content+"_s_"+id);
    this.length = content.length();
  }

  @Override
  public String getID() {
    return getElement().getId();
  }

  /**
   * Not needed here.
   */
  @Override
  public void obscureText() {}

  /**
   * Not needed here.
   */
  @Override
  public void restoreText() {}

  @Override
  public void setBackground(String background) {
    this.background = background;
    getElement().getStyle().setBackgroundColor(background);
  }

  @Override
  public void showHighlight() {
    highlighted = true;
    getElement().getStyle().setBackgroundColor(highlightColor);
  }

  @Override
  public void clearHighlight() {
    highlighted = false;
    if (background == null) {
      getElement().getStyle().clearBackgroundColor();
    } else {
      getElement().getStyle().setBackgroundColor(background);
    }
  }

  @Override
  public void checkClearHighlight() {
    if (isHighlighted()) clearHighlight();
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
    return this;
  }

  public void setClickable(boolean clickable) {
    this.clickable = clickable;
  }

  @Override
  public String getContent() {
    return getHTML();
  }

  public void setSouth(DivWidget widget) {
  }

  @Override
  public void setSouthScore(DivWidget widget) {
  }

  @Override
  public void clearSouth() {
  }

  @Override
  public DivWidget getNorth() {
    return null;
  }

  @Override
  public void setHighlightColor(String highlightColor) {
    this.highlightColor = highlightColor;
  }

  public int getLength() {
    return length;
  }

  public String toString() {
    return "seg '" + getHTML() + "' " + getLength() + " long";
  }
}
