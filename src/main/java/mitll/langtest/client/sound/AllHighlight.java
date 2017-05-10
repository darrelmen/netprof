package mitll.langtest.client.sound;

import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.user.client.ui.InlineHTML;
import com.google.gwt.user.client.ui.Widget;

import java.util.Collection;
import java.util.logging.Logger;

/**
 * Created by go22670 on 4/26/17.
 */
public class AllHighlight extends DivWidget implements IHighlightSegment {
  protected final Logger logger = Logger.getLogger("AllHighlight");

  private final Collection<IHighlightSegment> set;

  private DivWidget north;
  private DivWidget south;
  private int id;
  String content;

  public AllHighlight(Collection<IHighlightSegment> bulk) {
    this.set = bulk;
    add(north =new DivWidget());
    addAll();
    add(south =new DivWidget());
  }

  public void addAll() {
    north.clear();
    for (IHighlightSegment seg : set) north.add(seg.asWidget());
  }

  @Override
  public void setBackground(String background) {

  }

  @Override
  public void setBlue() {
    set.forEach(IHighlightSegment::setBlue);
  }

  @Override
  public void clearBlue() {
    set.forEach(IHighlightSegment::clearBlue);
  }

  @Override
  public boolean isHighlighted() {
    return !set.isEmpty() && set.iterator().next().isHighlighted();
  }

  @Override
  public int getLength() {
    int total = 0;
    for (IHighlightSegment seg : set) total += seg.getLength();
    return total;
  }

  @Override
  public boolean isClickable() {
    boolean clickable = true;
    for (IHighlightSegment seg : set) {
      if (seg.isClickable()) {
        clickable = false;
        break;
      }
    }
    return clickable;
  }

  @Override
  public InlineHTML getClickable() {
    throw new IllegalArgumentException("don't call me");
  }

  @Override
  public void setClickable(boolean clickable) {
    throw new IllegalArgumentException("don't call me");
  }

  @Override
  public String getContent() {
    StringBuilder builder = new StringBuilder();
    for (IHighlightSegment seg : set) {
      builder.append(seg.getContent());
    }

    return builder.toString();
  }

  public void setSouth(Widget widget) {
    south.clear();
    south.add(widget);
  }

  @Override
  public Widget getParent() {
    return set.iterator().next().getParent();
  }

  public String toString() {
    return set.size() + " segments " + getLength() + " long : " + getContent();
  }
}
