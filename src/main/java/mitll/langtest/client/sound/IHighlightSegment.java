package mitll.langtest.client.sound;

import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Widget;

/**
 * Created by go22670 on 4/25/17.
 */
public interface IHighlightSegment {
  /**
   * @see #showHighlight
   */
  String DEFAULT_HIGHLIGHT = "#2196F3";

  String getID();

  void setHighlightColor(String highlightColor);
  void setBackground(String background);

  void showHighlight();
  void checkClearHighlight();
  void clearHighlight();
  boolean isHighlighted();

  void setObscurable();
  void obscureText();
  void restoreText();

  int getLength();

  boolean isClickable();

  HTML getClickable();

  void setClickable(boolean clickable);

  String getContent();

  void setSouth(DivWidget widget);
  void setSouthScore(DivWidget widget);

  Widget asWidget();

  void clearSouth();

  DivWidget getNorth();
}
