/*
 *
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2015. Other request for this document shall be referred
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
 * © 2015 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 *
 *
 */

package mitll.langtest.client.custom;

import com.github.gwtbootstrap.client.ui.Tooltip;
import com.github.gwtbootstrap.client.ui.constants.Placement;
import com.google.gwt.user.client.ui.Widget;

import java.util.logging.Logger;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 5/16/2014.
 */
public class TooltipHelper {
  private final Logger logger = Logger.getLogger("TooltipHelper");

  /**
   * @param w
   * @param tip
   * @return
   * @see mitll.langtest.client.user.BasicDialog#addTooltip(com.google.gwt.user.client.ui.Widget, String)
   */
  public Tooltip addTooltip(Widget w, String tip) {
    return createAddTooltip(w, tip, Placement.RIGHT);
  }

  public Tooltip addTopTooltip(Widget w, String tip) {
    return createAddTooltip(w, tip, Placement.TOP);
  }

  /**
   * @param widget
   * @param tip
   * @param placement
   * @return
   * @see mitll.langtest.client.custom.exercise.NPFExercise#makeAddToList(mitll.langtest.shared.exercise.CommonExercise, mitll.langtest.client.exercise.ExerciseController)
   */
  public Tooltip createAddTooltip(Widget widget, String tip, Placement placement) {
    //  logger.info("createAddTooltip tooltip " + tip + " to " + widget.getElement().getId() + " place " + placement);
    Tooltip tooltip = new Tooltip();
    tooltip.setWidget(widget);
    tooltip.setText(tip);
    tooltip.setAnimation(true);
// As of 4/22 - bootstrap 2.2.1.0 -
// Tooltips have an bug which causes the cursor to
// toggle between finger and normal when show delay
// is configured.

    tooltip.setShowDelay(500);
    tooltip.setHideDelay(500);

    tooltip.setPlacement(placement);
    tooltip.reconfigure();
    return tooltip;
  }
}
