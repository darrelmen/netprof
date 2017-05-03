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

  public String toString() {
    return set.size() + " segments " + getLength() + " long ";
  }
}
