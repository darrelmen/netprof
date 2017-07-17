package mitll.langtest.client.sound;

import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.user.client.ui.InlineHTML;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.scoring.TwoColumnExercisePanel;

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

  /**
   * @param content
   * @see mitll.langtest.client.scoring.ClickableWords#makeClickableText
   * @see mitll.langtest.client.scoring.WordTable#addPhonesBelowWord2
   */
  public SimpleHighlightSegment(String content, int id) {
    super(content);
    getElement().setId(content+"_s_"+id);
    this.length = content.length();
  }

  @Override
  public String getID() {
    return getElement().getId();
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

  @Override
  public void clearSouth() {
  }

  @Override
  public DivWidget getNorth() {
    return null;
  }

  public String toString() {
    return "seg '" + getHTML() + "' " + getLength() + " long";
  }
}
