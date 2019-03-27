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

package mitll.langtest.client.custom.dialog;

import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.list.ListInterface;
import org.jetbrains.annotations.NotNull;

public class DominoLinkNotice {

  private static final String CLICK_HERE_TO_GO_TO_DOMINO = "Need to edit the text? Click here.";

  /**
   * @return
   * @see #addFields(ListInterface, Panel)
   */
  @NotNull
  public DivWidget getDominoEditInfo(ExerciseController controller) {
    DivWidget h = new DivWidget();
    h.addStyleName("leftFiveMargin");
    h.addStyleName("bottomFiveMargin");
    h.addStyleName("inlineFlex");

    HTML child1 = getHint();

    h.add(child1);

    h.add(getAnchor(controller));
    return h;
  }

  @NotNull
  public HTML getHint() {
    return new HTML("To edit the item text : copy the domino id, go into domino, find and edit the item, and then sync.");
  }

  @NotNull
  public Anchor getAnchor(ExerciseController controller) {
    Anchor child = new Anchor(CLICK_HERE_TO_GO_TO_DOMINO);
    child.addStyleName("leftFiveMargin");
    child.setTarget("_blank");
    child.setHref(controller.getProps().getDominoURL());
    return child;
  }
}
