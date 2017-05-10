package mitll.langtest.client.sound;

import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.user.client.ui.InlineHTML;
import com.google.gwt.user.client.ui.Widget;

/**
 * Created by go22670 on 4/25/17.
 */
public interface IHighlightSegment {
  void setBackground(String background);

  void setBlue();
  void clearBlue();
  boolean isHighlighted();
  int getLength();

  boolean isClickable();

  InlineHTML getClickable();

  void setClickable(boolean clickable);

  String getContent();

 // void addSouth();
  //DivWidget getSouth();
  void setSouth(Widget widget);

  Widget getParent();
  Widget asWidget();
  /*  Widget getParent();

  void removeFromParent();
  void setSouth(Widget south);

  */
}
