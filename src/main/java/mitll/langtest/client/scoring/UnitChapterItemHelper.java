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

package mitll.langtest.client.scoring;

import com.github.gwtbootstrap.client.ui.Heading;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.shared.exercise.CommonExercise;

import java.util.Collection;
import java.util.logging.Logger;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 3/11/16.
 */
public class UnitChapterItemHelper<T extends CommonExercise> {
  private final Logger logger = Logger.getLogger("UnitChapterItemHelper");

  /**
   * @see mitll.langtest.client.exercise.WaveformExercisePanel#addInstructions
   */
  private static final int HEADING_FOR_UNIT_LESSON = 4;
 // public static final String CORRECT = "correct";
  private static final String ITEM = "Item";

  private final Collection<String> typeOrder;

  /**
   * @see GoodwaveExercisePanel#getQuestionContent
   * @param typeOrder
   */
  public UnitChapterItemHelper(Collection<String> typeOrder) {
    this.typeOrder = typeOrder;
  }

  /**
   * @see GoodwaveExercisePanel#getQuestionContent
   * @param exercise
   * @param vp
   * @return
   */
  public Panel addUnitChapterItem(T exercise, Panel vp) {
    Widget itemHeader = getItemHeader(exercise);
    if (exercise.getUnitToValue().isEmpty()) {
      return null;
    } else {
      Panel unitLessonForExercise = getUnitLessonForExercise(exercise);
      unitLessonForExercise.add(itemHeader);
      vp.add(unitLessonForExercise);
      return unitLessonForExercise;
    }
  }

  /**
   * Prefer domino id
   * @param e
   * @return
   * @see GoodwaveExercisePanel#getQuestionContent
   */
  private Widget getItemHeader(T e) {
    // logger.info("got " + e + " and " + e.getDominoID());
    int dominoID = e.getDominoID();
    int idToUse = dominoID != -1 ? dominoID : e.getID();
    Heading w = new Heading(HEADING_FOR_UNIT_LESSON, ITEM, "" + idToUse + "/" + e.getOldID());
    w.getElement().setId("ItemHeading");
    return w;
  }

  /**
   * Show unit and chapter info for every item.
   *
   * @return
   * @see GoodwaveExercisePanel#getQuestionContent
   */
  private Panel getUnitLessonForExercise(T exercise) {
    Panel flow = new HorizontalPanel();
    flow.getElement().setId("getUnitLessonForExercise_unitLesson");
    flow.addStyleName("leftFiveMargin");
    logger.info("getUnitLessonForExercise " + exercise + " unit value " +exercise.getUnitToValue());

    for (String type : typeOrder) {
      String subtext = exercise.getUnitToValue().get(type);
      if (subtext != null && !subtext.isEmpty()) {
        Heading child = new Heading(HEADING_FOR_UNIT_LESSON, type, subtext);
        child.addStyleName("rightFiveMargin");
        flow.add(child);
      }
    }
    return flow;
  }
}
