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

package mitll.langtest.client.scoring;

import com.github.gwtbootstrap.client.ui.Heading;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.constants.Placement;
import com.google.gwt.dom.client.Style;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.client.ui.InlineLabel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.UIObject;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.exercise.WaveformExercisePanel;
import mitll.langtest.client.user.BasicDialog;
import mitll.langtest.shared.exercise.Details;
import mitll.langtest.shared.exercise.HasID;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.logging.Logger;

import static mitll.langtest.client.exercise.WaveformExercisePanel.PARENT_ITEM;

public class UnitChapterItemHelper<T extends HasID & Details> {
  private final Logger logger = Logger.getLogger("UnitChapterItemHelper");

  private static final String BLANK = "blank";
  private static final int MAXLEN = 10;
  /**
   * @see mitll.langtest.client.exercise.WaveformExercisePanel#addInstructions
   */
  private static final int HEADING_FOR_UNIT_LESSON = 4;
  private static final String ITEM = "Item";
  private static final String ID = "ID";
  private static final String TIME = "Time";
  private static final String DOMINO_ID = "Doc Id";
  private static final String NP_ID = "NP Import ID";

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
  public UIObject addUnitChapterItem(T exercise, Panel vp) {
    Widget itemHeader = getItemHeader(exercise);
    if (exercise.getUnitToValue().isEmpty()) {
      vp.add(itemHeader);
      return itemHeader;
    } else {
      Panel unitLessonForExercise = getUnitLessonForExercise(exercise);
      unitLessonForExercise.add(itemHeader);
      vp.add(unitLessonForExercise);
      return unitLessonForExercise;
    }
  }

  /**
   * @param e
   * @return
   * @see GoodwaveExercisePanel#getQuestionContent
   */
  private Widget getItemHeader(HasID e) {
    DivWidget widget = new DivWidget();
    widget.addStyleName("inlineFlex");
    addProminentLabel(widget, ITEM, "" + e.getID(), false);
    return widget;
//        new Heading(HEADING_FOR_UNIT_LESSON, ITEM, "" + e.getID());
  }

  protected SafeHtml getSafeHtml(String columnText) {
    return new SafeHtmlBuilder().appendHtmlConstant(columnText).toSafeHtml();
  }

  private String getUnitChapterLabel(Map<String, String> unitToValue) {
    List<String> sections = new ArrayList<>();
    for (String type : typeOrder) {
      String subtext = unitToValue.get(type);
      if (subtext != null && !subtext.isEmpty()) {
        sections.add(subtext);
      }
      if (sections.size() == 2) break;
    }
    return String.join(" > ", sections);
  }

  /**
   * Show unit and chapter info for every item.
   *
   * @return
   * @see #addUnitChapterItem
   */
  private Panel getUnitLessonForExercise(T exercise) {
    // Panel flow = new HorizontalPanel();
 //   logger.info("getUnitLessonForExercise for " + exercise);
    Panel flow = new DivWidget();
    flow.addStyleName("inlineFlex");
    //flow.getElement().setId("getUnitLessonForExercise_unitLesson");
    flow.addStyleName("leftFiveMargin");
    // logger.info("getUnitLessonForExercise " + exercise + " unit value " +exercise.getUnitToValue());

    boolean found = false;
    for (String type : typeOrder) {
      String subtext = exercise.getUnitToValue().get(type);
      //   logger.info("getUnitLessonForExercise type " + type + " = " + subtext);
      if (subtext != null && !subtext.isEmpty() && !subtext.equalsIgnoreCase(BLANK)) {
        boolean markProminent = type.equalsIgnoreCase(WaveformExercisePanel.DOMINO_PROJECT);

        if (type.equalsIgnoreCase(PARENT_ITEM)) {
          found = true;
        } else {
          addProminentLabel(flow, type, subtext, markProminent);
        }
      }
    }

    int dominoID = exercise.getDominoID();
    String oldID = exercise.getOldID();

    boolean showDomino = dominoID > 0;
    if (showDomino) {
      addProminentLabel(flow, DOMINO_ID, "" + dominoID, true);
    }

    if (found) {
      String subtext = exercise.getUnitToValue().get(PARENT_ITEM);
      addProminentLabel(flow, PARENT_ITEM, subtext, false);
    }

    if (!showDomino && !oldID.isEmpty()) {
      addProminentLabel(flow, NP_ID, oldID, true);
    }

    return flow;
  }


  private void addProminentLabel(Panel flow, String npId, String oldID, boolean markProminent) {
    Heading child = new Heading(HEADING_FOR_UNIT_LESSON, npId);
    child.getElement().getStyle().setColor("gray");
    child.getElement().getStyle().setFontWeight(Style.FontWeight.NORMAL);

    child.addStyleName("rightFiveMargin");

    flow.add(child);

    Heading npid = new Heading(HEADING_FOR_UNIT_LESSON, oldID);
    npid.addStyleName("rightFiveMargin");
    if (markProminent) {
      npid.getElement().getStyle().setColor("blue");
    }
    flow.add(npid);
  }

  InlineLabel showPopup(T exercise) {
    InlineLabel itemHeader = getLabel(exercise);
    showPopup(itemHeader, getUnitLessonForExercise2(exercise));
    return itemHeader;
  }

  /**
   * Polyglot > Regular Verb
   * 123456789012345678901234567890
   *
   * @param e
   * @return
   */
  private InlineLabel getLabel(Details e) {
    String unitChapterLabel = getUnitChapterLabel(e.getUnitToValue());
    if (unitChapterLabel.length() > MAXLEN) unitChapterLabel = unitChapterLabel.substring(0, MAXLEN) + "...";
    InlineLabel inlineLabel = new InlineLabel(unitChapterLabel);
    inlineLabel.getElement().getStyle().setWhiteSpace(Style.WhiteSpace.NOWRAP);
    return inlineLabel;
  }

  private void showPopup(InlineLabel label, String toShow) {
    label.addMouseOverHandler(event -> new BasicDialog().showPopover(
        label,
        null,
        toShow,
        Placement.LEFT));
  }

  private final DateTimeFormat format = DateTimeFormat.getFormat("MMM d, yy h:mm a");

  /**
   * @param exercise
   * @return
   * @see #showPopup
   */
  private String getUnitLessonForExercise2(T exercise) {
    long update = exercise.getUpdateTime();
    String updateTime = this.format.format(new Date(update));
    return getTypeToValue(this.typeOrder, exercise.getUnitToValue(), exercise.getID(), exercise.getDominoID(), exercise.getOldID(), updateTime);
  }

  public String getTypeToValue(Collection<String> typeOrder, Map<String, String> unitToValue) {
    return getTypeToValue(typeOrder, unitToValue, -1, -1, "", "");
  }

  @NotNull
  private String getTypeToValue(Collection<String> typeOrder, Map<String, String> unitToValue, int id,
                                int dominoID, String oldID, String updateTime) {
    StringBuilder builder = new StringBuilder();
    for (String type : typeOrder) {
      String subtext = unitToValue.get(type);
      // logger.info("type " + type + " = " + subtext);
      if (subtext != null && !subtext.isEmpty()) {
        String subtext1 = truncate(subtext,25);
        builder.append(getTypeAndValue(type, subtext1));
      }
    }
    if (id > 0) {
      builder.append(getTypeAndValue(ID, "" + id));
    }

    boolean showDomino = dominoID > 0;

    if (showDomino) {
      builder.append(getTypeAndValue(DOMINO_ID, "" + dominoID));
    }
    if (!showDomino && !oldID.isEmpty()) {
      builder.append(getTypeAndValue(NP_ID, oldID));
    }
    if (!updateTime.isEmpty()) {
      builder.append(getTypeAndValue(TIME, updateTime));
    }
    return builder.toString();
  }

  @NotNull
  protected String truncate(String columnText, int maxLengthId) {
    if (columnText.length() > maxLengthId) columnText = columnText.substring(0, maxLengthId - 3) + "...";
    return columnText;
  }

  @NotNull
  private String getTypeAndValue(String type, String subtext) {
    return "<span>" +
        "<h5>" + "<small>" + type + "</small>" +
        "<span style='margin-left:5px;float:right'>" + subtext + "</span>" +
        "</h5>" +
        "</span>";
  }
}
