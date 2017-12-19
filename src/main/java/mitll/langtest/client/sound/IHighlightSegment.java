package mitll.langtest.client.sound;

import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Widget;

/**
 * Created by go22670 on 4/25/17.
 */
public interface IHighlightSegment {
  void setBackground(String background);

  String getID();
  void setBlue();
  void clearBlue();
  boolean isHighlighted();
  int getLength();

  boolean isClickable();

  HTML getClickable();

  void setClickable(boolean clickable);

  String getContent();

  void setSouth(Widget widget);

  Widget asWidget();

  void clearSouth();

  DivWidget getNorth();
}
