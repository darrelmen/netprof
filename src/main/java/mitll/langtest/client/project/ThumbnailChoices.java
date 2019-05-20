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

package mitll.langtest.client.project;

import com.github.gwtbootstrap.client.ui.Heading;
import com.github.gwtbootstrap.client.ui.Section;
import com.github.gwtbootstrap.client.ui.Thumbnail;
import com.github.gwtbootstrap.client.ui.constants.Placement;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.FocusWidget;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.scoring.UnitChapterItemHelper;
import mitll.langtest.client.user.BasicDialog;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Set;

/**
 * @see mitll.langtest.client.banner.DialogExerciseList
 */
public class ThumbnailChoices {
  private static final int CHOICE_WIDTH = 170;//180;//190;//195;
  private final BasicDialog basicDialog = new BasicDialog();

  @NotNull
  public Section getScrollingSection() {
    final Section section = new Section("section");
    section.setHeight("100%");
    return section;
  }

  @NotNull
  public Thumbnail getThumbnail() {
    Thumbnail thumbnail = new Thumbnail();
    thumbnail.setWidth(getChoiceWidth() + "px");
    thumbnail.setSize(2);
    return thumbnail;
  }

  protected int getChoiceWidth() {
    return CHOICE_WIDTH;
  }

  @NotNull
  public Heading getChoiceLabel(int size, String name, boolean setLineHeight) {
    Heading label = new Heading(size, name);
    label.addStyleName("floatLeft");
    label.setWidth("100%");
    if (setLineHeight) {
      label.getElement().getStyle().setLineHeight(25, Style.Unit.PX);
    }
    {
      Widget subtitle = label.getWidget(0);
      subtitle.addStyleName("floatLeft");
      subtitle.setWidth("100%");
      subtitle.addStyleName("topFiveMargin");
    }
    return label;
  }

  @NotNull
  public String truncate(String columnText, int maxLengthId) {
    if (columnText.length() > maxLengthId) columnText = columnText.substring(0, maxLengthId - 3) + "...";
    return columnText;
  }

  public void addPopover(FocusWidget button, Map<String, String> props, Placement placement) {
    Set<String> typeOrder = props.keySet();
    UnitChapterItemHelper<?> ClientExerciseUnitChapterItemHelper = new UnitChapterItemHelper<>(typeOrder);
    button.addMouseOverHandler(event -> showPopover(props, button, typeOrder, ClientExerciseUnitChapterItemHelper, placement));
  }

  /**
   * @see ProjectChoices#showPopoverUsual(String, Widget, Set, UnitChapterItemHelper)
   * @param props
   * @param button
   * @param typeOrder
   * @param unitChapterItemHelper
   * @param placement
   */
  void showPopover(Map<String, String> props,
                   Widget button,
                   Set<String> typeOrder,
                   UnitChapterItemHelper<?> unitChapterItemHelper, Placement placement) {
    basicDialog.showPopover(
        button,
        null,
        unitChapterItemHelper.getTypeToValue(typeOrder, props),
        placement);
  }
}