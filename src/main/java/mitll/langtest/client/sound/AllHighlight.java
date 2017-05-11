package mitll.langtest.client.sound;

import com.github.gwtbootstrap.client.ui.base.DivWidget;
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
  //private String content;

  /**
   *
   * @param bulk
   */
  public AllHighlight(Collection<IHighlightSegment> bulk) {
    logger.info("making all highlight for " + bulk);

    this.set = bulk;
    add(north = new DivWidget());

    DivWidget divParent = set.iterator().next().getDivParent();
    //Widget parent = getParent();
    //   addAll();
    for (IHighlightSegment seg : set) {
      Widget w = seg.asWidget();
      w.removeFromParent();
      north.add(w);
    }
    add(south = new DivWidget());

    if (divParent != null) {
      divParent.add(this);
    }
    else {
      logger.warning("no parent for " + bulk);
    }
//    ((Panel) parent).add(this);
  }

//  public void addAll() {
//    north.clear();
//    for (IHighlightSegment seg : set) north.add(seg.asWidget());
//  }

  @Override
  public void setBackground(String background) {}

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
   * @see mitll.langtest.client.scoring.TwoColumnExercisePanel#matchEventSegmentToClickable(Iterator, TranscriptSegment, List, AudioControl, TreeMap)
   * @param widget
   */
  public void setSouth(Widget widget) {
    south.clear();
    south.add(widget);
  }

  @Override
  public DivWidget getDivParent() {
    return null;
  }

  @Override
  public void setDivParent(DivWidget horizontal) {
    throw new IllegalArgumentException("don't call me");
  }

  public String toString() {
    return set.size() + " segments " + getLength() + " long : " + getContent();
  }
}
