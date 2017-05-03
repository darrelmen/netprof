package mitll.langtest.client.sound;

import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.safehtml.shared.annotations.IsSafeHtml;
import com.google.gwt.user.client.ui.InlineHTML;

import java.util.List;
import java.util.TreeMap;
import java.util.logging.Logger;

/**
 * Created by go22670 on 4/25/17.
 */
public class HighlightSegment extends InlineHTML implements IHighlightSegment {
  protected final Logger logger = Logger.getLogger("HighlightSegment");

  private static final String BLUE = "#2196F3";
  private final int length;
  private String background = null;
  private boolean highlighted = false;

  public HighlightSegment(@IsSafeHtml String html, Direction dir) {
    super(html, dir);
    length = html.length();
  }

  /**
   * @param content
   * @see mitll.langtest.client.scoring.WordTable#addPhonesBelowWord2(List, DivWidget, AudioControl, TreeMap)
   */
  public HighlightSegment(String content) {
    super(content);
    this.length = content.length();
  }

  public int getLength() {
    return length;
  }

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
//      logger.warning("no background color???");
      getElement().getStyle().clearBackgroundColor();
    } else {
      //    logger.info("set background " + background);
      getElement().getStyle().setBackgroundColor(background);
    }
  }

  @Override
  public boolean isHighlighted() {
    return highlighted;//getElement().getStyle().getBackgroundColor().equals(BLUE);
  }

  public String toString() {
    return  "one segment (" +getHTML()+ ")" + getLength() + " long ";
  }
}
