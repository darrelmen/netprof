package mitll.langtest.client.sound;

import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.InlineHTML;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.shared.instrumentation.TranscriptSegment;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;
import java.util.logging.Logger;

/**
 * Created by go22670 on 4/26/17.
 */
public class AllHighlight extends DivWidget implements IHighlightSegment {
  protected final Logger logger = Logger.getLogger("AllHighlight");

  private final Collection<IHighlightSegment> set;

  private DivWidget north;
  private DivWidget south;

  /**
   * @param bulk
   */
  public AllHighlight(Collection<IHighlightSegment> bulk) {
    //logger.info("making all highlight for " + bulk);
    getElement().setId("AllHighlight_" + bulk.size());
    addStyleName("floatLeft");
    getElement().getStyle().setMarginRight(3, Style.Unit.PX);

    this.set = bulk;

    add(north = new DivWidget());
    north.addStyleName("floatLeft");
    north.getElement().setId("north_bulk_" + bulk.size());

    for (IHighlightSegment seg : set) {
      Widget w = seg.asWidget();
      w.removeFromParent();
      north.add(seg.getNorth());
      seg.clearSouth();
    }
    add(south = new DivWidget());
    south.getElement().setId("south_bulk_" + bulk.size());
    south.addStyleName("floatLeft");
    south.getElement().getStyle().setClear(Style.Clear.BOTH);
  }

  @Override
  public String getID() {
    return set.isEmpty() ? "empty" : set.iterator().next().getID();
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

  /**
   * @param widget
   * @see mitll.langtest.client.scoring.TwoColumnExercisePanel#matchEventSegmentToClickable(Iterator, TranscriptSegment, List, AudioControl, TreeMap)
   */
  public void setSouth(Widget widget) {
    south.clear();
    south.add(widget);
  }

  public void clearSouth() {
    logger.info("doing clear south");
    remove(south);
  }

  @Override
  public DivWidget getNorth() {
    return north;
  }
  public String toString() {
    return set.size() + " segments " + getLength() + " long : " + getContent();
  }
}
