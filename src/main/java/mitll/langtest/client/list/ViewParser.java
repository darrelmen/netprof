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

package mitll.langtest.client.list;

import mitll.langtest.client.custom.INavigation;
import mitll.langtest.shared.exercise.OOV;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Logger;

import static mitll.langtest.client.custom.INavigation.VIEWS.*;

public class ViewParser {
  private final Logger logger = Logger.getLogger("SelectionState");

  private static final String DRILL = "Drill";
  private static final String PRACTICE = "Practice";

  /**
   * Do something smarter!
   * @param instance
   * @return
   */
  @NotNull
  public INavigation.VIEWS getView(String instance) {
    try {
      String rawInstance = instance;

      if (rawInstance.equalsIgnoreCase(CORE_REHEARSE.toString())) {
        return INavigation.VIEWS.CORE_REHEARSE;
      } else if (rawInstance.equalsIgnoreCase(REHEARSE.toString())) {
        return INavigation.VIEWS.REHEARSE;
      } else if (rawInstance.equalsIgnoreCase(PERFORM_PRESS_AND_HOLD.toString())) {
        return INavigation.VIEWS.PERFORM_PRESS_AND_HOLD;
      } else if (rawInstance.equalsIgnoreCase(PERFORM.toString())) {
        return PERFORM;
      } else if (rawInstance.equalsIgnoreCase(OOV_EDITOR.toString())) {
        return OOV_EDITOR;
      } else {
        instance = instance.replaceAll(" ", "_");

        if (instance.equalsIgnoreCase(DRILL)) instance = PRACTICE.toUpperCase();
        if (instance.equalsIgnoreCase(DIALOG.toString())) instance = INavigation.VIEWS.DIALOG.name();

        if (rawInstance.equalsIgnoreCase(CORE_REHEARSE.toString())) {
          return INavigation.VIEWS.REHEARSE;
        } else if (rawInstance.equalsIgnoreCase(PERFORM.toString())) {
          return PERFORM;
        } else {
          return instance.isEmpty() ? INavigation.VIEWS.NONE : INavigation.VIEWS.valueOf(instance.toUpperCase());
        }
      }

    } catch (IllegalArgumentException e) {
      logger.warning("getView : hmm, couldn't parse " + instance);
      return INavigation.VIEWS.NONE;
    }
  }
}
