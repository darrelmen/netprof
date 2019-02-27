/*
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2019. Other request for this document shall be referred
 * to DLIFLC.
 *
 * WARNING: This document may contain technical data whose export is restricted
 * by the Arms Export Control Act (AECA) or the Export Administration Act (EAA).
 * Transfer of this data by any means to a non-US person who is not eligible to
 * obtain export-controlled data is prohibited. By accepting this data, the consignee
 * agrees to honor the requirements of the AECA and EAA. DESTRUCTION NOTICE: For
 * unclassified, limited distribution documents, destroy by any method that will
 * prevent disclosure of the contents or reconstruction of the document.
 *
 * This material is based upon work supported under Air Force Contract No.
 * FA8721-05-C-0002 and/or FA8702-15-D-0001. Any opinions, findings, conclusions
 * or recommendations expressed in this material are those of the author(s) and
 * do not necessarily reflect the views of the U.S. Air Force.
 *
 * © 2015-2019 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 */

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

  public static final String FLOAT_LEFT = "floatLeft";
  public static final String INLINE_BLOCK_STYLE_ONLY = "inlineBlockStyleOnly";

  private final Collection<IHighlightSegment> set;

  private final DivWidget north;
  private final DivWidget south;

  /**
   * @param bulk
   * @param addFloatLeft
   * @see mitll.langtest.client.scoring.TwoColumnExercisePanel#matchEventSegmentToClickable
   * @see mitll.langtest.client.scoring.TwoColumnExercisePanel#matchSegmentToWidgetForAudio
   */
  public AllHighlight(Collection<IHighlightSegment> bulk, boolean addFloatLeft) {
    //logger.info("making all highlight for " + bulk);
    getElement().setId("AllHighlight_" + bulk.size());
    if (addFloatLeft) {
      addStyleName(FLOAT_LEFT);
    } else {
      addStyleName(INLINE_BLOCK_STYLE_ONLY);
    }
    getElement().getStyle().setMarginRight(3, Style.Unit.PX);

    this.set = bulk;

    add(north = new DivWidget());
    if (addFloatLeft) {
      north.addStyleName(FLOAT_LEFT);
      //logger.info("add float left to " + getElement().getId());
    } else {
      addStyleName(INLINE_BLOCK_STYLE_ONLY);
    }
    north.getElement().setId("all_highlight_north_bulk_" + bulk.size());

    set.forEach(iHighlightSegment -> {
      iHighlightSegment.asWidget().removeFromParent();
      north.add(iHighlightSegment.getNorth());
      iHighlightSegment.clearSouth();
    });

    add(south = new DivWidget());
    south.getElement().setId("all_highlight_south_bulk_" + bulk.size());
    if (addFloatLeft) {
      south.addStyleName(FLOAT_LEFT);
    } else {
      addStyleName(INLINE_BLOCK_STYLE_ONLY);
    }
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

  /**
   * @see mitll.langtest.client.scoring.WordTable#getDivWord
   * @param widget
   */
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
  public boolean obscureText() {
    boolean didIt=true;
    for (IHighlightSegment segment:set) {
      didIt &= segment.obscureText();
    }
    return didIt;
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
