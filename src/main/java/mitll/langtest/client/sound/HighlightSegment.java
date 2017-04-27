package mitll.langtest.client.sound;

import com.google.gwt.safehtml.shared.annotations.IsSafeHtml;
import com.google.gwt.user.client.ui.InlineHTML;

import java.util.logging.Logger;

/**
 * Created by go22670 on 4/25/17.
 */
public class HighlightSegment extends InlineHTML implements IHighlightSegment {
  protected final Logger logger = Logger.getLogger("HighlightSegment");

  private static final String BLUE = "#2196F3";
  private String background = null;
  private boolean highlighted = false;

  public HighlightSegment(@IsSafeHtml String html, Direction dir) {
    super(html, dir);
  }

  public HighlightSegment(String content) {
    super(content);
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
      logger.warning("no background color???");
      getElement().getStyle().clearBackgroundColor();
    } else {
      logger.info("set background " + background);
      getElement().getStyle().setBackgroundColor(background);
    }
  }

  @Override
  public boolean isHighlighted() {
    return highlighted;//getElement().getStyle().getBackgroundColor().equals(BLUE);
  }
}
