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
import com.github.gwtbootstrap.client.ui.constants.Placement;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.InlineLabel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.user.BasicDialog;
import mitll.langtest.shared.exercise.CommonExercise;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Map;
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
  private static final String ITEM = "Item";

  private final Collection<String> typeOrder;

  /**
   * @param typeOrder
   * @see GoodwaveExercisePanel#getQuestionContent
   */
  public UnitChapterItemHelper(Collection<String> typeOrder) {
    this.typeOrder = typeOrder;
  }

  /**
   * @param exercise
   * @param vp
   * @return
   * @see GoodwaveExercisePanel#getQuestionContent
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
   *
   * @param e
   * @return
   * @see GoodwaveExercisePanel#getQuestionContent
   */
  private Widget getItemHeader(T e) {
    // logger.info("got " + e + " and " + e.getDominoID());
//    int dominoID = e.getDominoID();
//    int idToUse = dominoID != -1 ? dominoID : e.getID();
    String subtext = getID(e);// + (e.getOldID().isEmpty() ? "" : "/" + e.getOldID());
    Heading w = new Heading(HEADING_FOR_UNIT_LESSON, ITEM, subtext);
    // w.getElement().setId("ItemHeading");
    return w;
  }

/*  public Widget getSmall(T e) {
    FlowPanel fp = new FlowPanel("small");
    int dominoID = e.getDominoID();
    int idToUse = dominoID != -1 ? dominoID : e.getID();
    fp.getElement().setInnerText("" + idToUse);// + "/" + e.getOldID());
    return fp;
  }*/

/*
  public Widget getSmall2(T e) {
    FlowPanel fp = new FlowPanel("small");
    String text = getID(e);
    fp.getElement().setInnerText(text);// + "/" + e.getOldID());
    new TooltipHelper().addTooltip(fp, getUnitLessonForExercise2(e));
    return fp;
  }
*/

  @NotNull
  private String getID(T e) {
    return  ""+ e.getID();
//    int dominoID = e.getDominoID();
//    int idToUse = dominoID != -1 ? dominoID : e.getID();
//    return "" + idToUse;
  }

  private InlineLabel getLabel(T e) {
    return new InlineLabel(getID(e));
  }

  /**
   * Show unit and chapter info for every item.
   *
   * @return
   * @see #addUnitChapterItem(CommonExercise, Panel)
   */
  private Panel getUnitLessonForExercise(T exercise) {
    Panel flow = new HorizontalPanel();
    flow.getElement().setId("getUnitLessonForExercise_unitLesson");
    flow.addStyleName("leftFiveMargin");
    // logger.info("getUnitLessonForExercise " + exercise + " unit value " +exercise.getUnitToValue());

    for (String type : typeOrder) {
      String subtext = exercise.getUnitToValue().get(type);
      if (subtext != null && !subtext.isEmpty()) {
        Heading child = new Heading(HEADING_FOR_UNIT_LESSON, type, subtext);
        child.addStyleName("rightFiveMargin");
        flow.add(child);
      }
    }
    if (exercise.getDominoID() > 0) {
      Heading child = new Heading(HEADING_FOR_UNIT_LESSON, "Domino ID", "" + exercise.getDominoID());
      child.addStyleName("rightFiveMargin");
      flow.add(child);
    }
    return flow;
  }

  private String getUnitLessonForExercise2(T exercise) {
    return getTypeToValue(this.typeOrder, exercise.getUnitToValue());
  }

  @NotNull
  public String getTypeToValue(Collection<String> typeOrder, Map<String, String> unitToValue) {
    StringBuilder builder = new StringBuilder();
    for (String type : typeOrder) {
      String subtext = unitToValue.get(type);
      if (subtext != null && !subtext.isEmpty()) {
        String html =
            "<span>" +
                "<h5>" + type + "<small style='margin-left:5px'>" + subtext + "</small>" +
                "</h5>" +
                "</span>";
        builder.append(html);
      }
    }
    return builder.toString();
  }

  public InlineLabel showPopup(T exercise) {
    InlineLabel itemHeader = getLabel(exercise);
    showPopup(itemHeader, getUnitLessonForExercise2(exercise));
    return itemHeader;
  }

  public void showPopup(InlineLabel label, String toShow) {
    label.addMouseOverHandler(event -> new BasicDialog().showPopover(
        label,
        null,
        toShow,
        Placement.LEFT));
  }
}
