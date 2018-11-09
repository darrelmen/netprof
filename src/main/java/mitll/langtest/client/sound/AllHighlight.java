package mitll.langtest.client.sound;

import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.HTML;

import java.util.Collection;
import java.util.logging.Logger;

/**
 * Created by go22670 on 4/26/17.
 */
public class AllHighlight extends DivWidget implements IHighlightSegment {
  protected final Logger logger = Logger.getLogger("AllHighlight");

  private final Collection<IHighlightSegment> set;

  private final DivWidget north;
  private final DivWidget south;

  /**
   * @param bulk
   * @see mitll.langtest.client.scoring.TwoColumnExercisePanel#matchEventSegmentToClickable
   * @see mitll.langtest.client.scoring.TwoColumnExercisePanel#matchSegmentToWidgetForAudio
   */
  public AllHighlight(Collection<IHighlightSegment> bulk) {
    //logger.info("making all highlight for " + bulk);
    getElement().setId("AllHighlight_" + bulk.size());
    addStyleName("floatLeft");
    getElement().getStyle().setMarginRight(3, Style.Unit.PX);

    this.set = bulk;

    add(north = new DivWidget());
    north.addStyleName("floatLeft");
    north.getElement().setId("all_highlight_north_bulk_" + bulk.size());

    set.forEach(iHighlightSegment -> {
      iHighlightSegment.asWidget().removeFromParent();
      north.add(iHighlightSegment.getNorth());
      iHighlightSegment.clearSouth();
    });

    add(south = new DivWidget());
    south.getElement().setId("all_highlight_south_bulk_" + bulk.size());
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
  public void showHighlight() {
    set.forEach(IHighlightSegment::showHighlight);
  }

  @Override
  public void clearHighlight() {
    set.forEach(IHighlightSegment::clearHighlight);
  }

  @Override
  public void checkClearHighlight() {
    set.forEach(IHighlightSegment::checkClearHighlight);
  }

  @Override
  public void setHighlightColor(String highlightColor) {
    set.forEach(iHighlightSegment -> iHighlightSegment.setHighlightColor(highlightColor));
  }

  @Override
  public boolean isHighlighted() {
    return !set.isEmpty() && set.iterator().next().isHighlighted();
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
  public HTML getClickable() {
    throw new IllegalArgumentException("don't call me");
  }

  @Override
  public void setClickable(boolean clickable) {
    throw new IllegalArgumentException("don't call me");
  }

  @Override
  public String getContent() {
    StringBuilder builder = new StringBuilder();
    set.forEach(iHighlightSegment -> builder.append(iHighlightSegment.getContent()));
    return builder.toString();
  }

  /**
   * @param widget
   * @see mitll.langtest.client.scoring.TwoColumnExercisePanel#matchEventSegmentToClickable
   */
  public void setSouth(DivWidget widget) {
    south.clear();
    south.add(widget);
  }

  public void setSouthScore(DivWidget widget) {
    setSouth(widget);
  }

  public void clearSouth() {
//    logger.info("doing clear south");
    remove(south);
  }

  @Override
  public DivWidget getNorth() {
    return north;
  }

  @Override
  public void setObscurable() {
    set.forEach(IHighlightSegment::setObscurable);
  }

  @Override
  public void obscureText() {
    set.forEach(IHighlightSegment::obscureText);
  }

  @Override
  public void restoreText() {
    set.forEach(IHighlightSegment::restoreText);
  }

  /**
   * Just for toString really.
   *
   * @return
   */
  @Override
  public int getLength() {
    //int total = 0;
    //for (IHighlightSegment seg : set) total += seg.getLength();
    return set.stream().mapToInt(IHighlightSegment::getLength).sum();
    //return total;
  }

  public String toString() {
    return set.size() + " segments " + getLength() + " long : " + getContent();
  }
}
