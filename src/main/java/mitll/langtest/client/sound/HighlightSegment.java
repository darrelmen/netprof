package mitll.langtest.client.sound;

import com.google.gwt.safehtml.shared.annotations.IsSafeHtml;
import com.google.gwt.user.client.ui.InlineHTML;

/**
 * Created by go22670 on 4/25/17.
 */
public class HighlightSegment extends InlineHTML implements IHighlightSegment {
  private String background = null;

  public HighlightSegment(@IsSafeHtml String html, Direction dir) {
    super(html, dir);
  }

  public HighlightSegment(String content) {
    super(content);
  }

  @Override
  public void setBlue() {
    getElement().getStyle().setBackgroundColor("#2196F3");
  }

  @Override
  public void clearBlue() {
    if (background == null) {
      getElement().getStyle().clearBackgroundColor();
    } else {
      getElement().getStyle().setBackgroundColor(background);
    }
  }

  public void setBackground(String background) {
    this.background = background;
    getElement().getStyle().setBackgroundColor(background);
  }
}
