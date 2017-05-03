package mitll.langtest.client.sound;

import java.util.Collection;

/**
 * Created by go22670 on 4/26/17.
 */
public class AllHighlight implements IHighlightSegment {
  private final Collection<IHighlightSegment> set;

  public AllHighlight(Collection<IHighlightSegment> bulk) {
    this.set = bulk;
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
  public String getContent() {
    StringBuilder builder = new StringBuilder();
    for (IHighlightSegment seg : set) {
      builder.append(seg.getContent());
    }

    return builder.toString();
  }

  public String toString() {
    return set.size() + " segments " + getLength() + " long : " + getContent();
  }
}
