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
    set.stream().forEach(IHighlightSegment::setBlue);
  }

  @Override
  public void clearBlue() {
    set.stream().forEach(IHighlightSegment::clearBlue);
  }
}
