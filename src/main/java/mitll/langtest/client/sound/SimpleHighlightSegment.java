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
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.InlineHTML;

import java.util.logging.Logger;

/**
 * Created by go22670 on 5/10/17.
 */
public class SimpleHighlightSegment extends InlineHTML implements IHighlightSegment {
  protected final Logger logger = Logger.getLogger("HighlightSegment");
  /**
   * @see #showHighlight
   */
  private String highlightColor;
  private final int length;
  private String background = null;
  private boolean highlighted = false;
  private boolean clickable = true;

  /**
   * @param content
   * @param highlightColor
   * @seex mitll.langtest.client.scoring.ClickableWords#makeClickableText
   * @see mitll.langtest.client.scoring.WordTable#addPhonesBelowWord2
   */
  public SimpleHighlightSegment(String content, String highlightColor) {
    super(content);
    this.highlightColor = highlightColor;
    //getElement().setId(content+"_s_"+id);
    this.length = content.length();
  }

  @Override
  public String getID() {
    return getElement().getId();
  }

  @Override
  public void setObscurable() {}

  @Override
  public boolean isObscurable() {
    return false;
  }

  /**
   * Not needed here.
   */
  @Override
  public boolean obscureText() { return true;}

  /**
   * Not needed here.
   */
  @Override
  public void restoreText() {}

  @Override
  public void setBackground(String background) {
    this.background = background;
    getElement().getStyle().setBackgroundColor(background);
  }

  @Override
  public void showHighlight() {
    highlighted = true;
    getElement().getStyle().setBackgroundColor(highlightColor);
  }

  @Override
  public void clearHighlight() {
    highlighted = false;
    if (background == null) {
      forceClearHighlight();
    } else {
      getElement().getStyle().setBackgroundColor(background);
    }
  }

  @Override
  public void forceClearHighlight() {
    getElement().getStyle().clearBackgroundColor();
  }

  @Override
  public void clearObscurable() {

  }

  @Override
  public void checkClearHighlight() {
    if (isHighlighted()) clearHighlight();
  }

  @Override
  public boolean isHighlighted() {
    return highlighted;
  }

  @Override
  public boolean isClickable() {
    return clickable;
  }

  @Override
  public HTML getClickable() {
    return this;
  }

  public void setClickable(boolean clickable) {
    this.clickable = clickable;
  }

  @Override
  public String getContent() {
    return getHTML();
  }

  public void setSouth(DivWidget widget) {
  }

  @Override
  public void setSouthScore(DivWidget widget) {
  }

  @Override
  public void clearSouth() {
  }

  @Override
  public DivWidget getNorth() {
    return null;
  }

  @Override
  public void setHighlightColor(String highlightColor) {
    this.highlightColor = highlightColor;
  }

  public int getLength() {
    return length;
  }

  public String toString() {
    return "seg '" + getHTML() + "' " + getLength() + " long";
  }
}
