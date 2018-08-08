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
import mitll.langtest.shared.exercise.ClientExercise;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Set;

public class ThumbnailChoices {
  private static final int CHOICE_WIDTH = 170;//180;//190;//195;
  private final BasicDialog basicDialog = new BasicDialog();

  @NotNull
  public Section getScrollingSection() {
    final Section section = new Section("section");
    section.getElement().getStyle().setOverflow(Style.Overflow.SCROLL);
    section.setHeight("100%");
    section.getElement().getStyle().setMarginTop(-8, Style.Unit.PX);

    return section;
  }

  @NotNull
  public Thumbnail getThumbnail() {
    Thumbnail thumbnail = new Thumbnail();
    thumbnail.setWidth(CHOICE_WIDTH + "px");
    thumbnail.setSize(2);
    return thumbnail;
  }

  @NotNull
  public Heading getChoiceLabel(int size, String name) {
    Heading label = new Heading(size, name);
    label.addStyleName("floatLeft");
    label.setWidth("100%");
    label.getElement().getStyle().setLineHeight(25, Style.Unit.PX);

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
    UnitChapterItemHelper<ClientExercise> ClientExerciseUnitChapterItemHelper = new UnitChapterItemHelper<>(typeOrder);
    button.addMouseOverHandler(event -> showPopover(props, button, typeOrder, ClientExerciseUnitChapterItemHelper, placement));
  }

  void showPopover(Map<String, String> props,
                   Widget button,
                   Set<String> typeOrder,
                   UnitChapterItemHelper<ClientExercise> unitChapterItemHelper, Placement placement) {
    basicDialog.showPopover(
        button,
        null,
        unitChapterItemHelper.getTypeToValue(typeOrder, props),
        placement);
  }
}
