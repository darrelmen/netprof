package mitll.langtest.client.project;

import com.github.gwtbootstrap.client.ui.TabPane;
import com.github.gwtbootstrap.client.ui.TabPanel;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.constants.LabelType;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.dialog.DialogHelper;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.shared.exercise.DominoUpdateItem;
import mitll.langtest.shared.exercise.DominoUpdateResponse;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

public class ResponseModal {
  private static final Logger log = Logger.getLogger(ResponseModal.class.getName());
  private static final String NOTHING = "Nothing";
  private static final String DO_YOU_WANT_TO_CONTINUE = "Do you want to continue?";

  private final DominoUpdateResponse result;
  private final ExerciseController controller;

  /**
   * @param result
   */
  ResponseModal(DominoUpdateResponse result,
                DialogHelper.CloseListener closeListener,
                ExerciseController controller) {
    this.result = result;
    this.closeListener = closeListener;
    this.controller = controller;
  }

  private final DialogHelper.CloseListener closeListener;

  void prepareContentWidget() {
    DivWidget cDivWidget = new DivWidget();

    List<DominoUpdateItem> added = new ArrayList<>();
    List<DominoUpdateItem> changed = new ArrayList<>();
    List<DominoUpdateItem> deleted = new ArrayList<>();
    for (DominoUpdateItem item : result.getUpdates()) {
      if (item.getStatus() == DominoUpdateItem.ITEM_STATUS.ADD) {
        added.add(item);
      } else if (item.getStatus() == DominoUpdateItem.ITEM_STATUS.CHANGE) {
        changed.add(item);
      } else if (item.getStatus() == DominoUpdateItem.ITEM_STATUS.DELETE) {
        deleted.add(item);
      }
    }

    DivWidget upper = new DivWidget();
    DivWidget row = new DivWidget();
    row.setWidth("100%");
    upper.add(row);

    {
      Label label1 = getLabel("Changes since last sync on");// <b>" + result.getTimestamp() + "</b>");
      row.add(label1);
      label1.addStyleName("floatLeft");

      com.github.gwtbootstrap.client.ui.Label modified = new com.github.gwtbootstrap.client.ui.Label(LabelType.IMPORTANT, result.getTimestamp());
      modified.addStyleName("leftFiveMargin");
      modified.addStyleName("floatLeft");
      row.add(modified);
      row.addStyleName("floatLeftAndClear");
      row.addStyleName("bottomFiveMargin");
    }


    Label w = getLabel(" ");
    w.addStyleName("floatLeftAndClear");
    upper.add(w);

    Label label = getLabel("This update would make the following changes.");
    label.addStyleName("bottomFiveMargin");
    upper.add(label);
    upper.add(getLabel(" "));
    upper.add(getLabel(added.size() + " items would be added."));
    upper.add(getLabel(changed.size() + " items would be changed."));
    upper.add(getLabel(deleted.size() + " items would be deleted."));

    upper.setHeight("110px");
    cDivWidget.add(upper);
    TabPanel tp = new TabPanel();
    tp.addStyleName("bottomFiveMargin");
    tp.setHeight("400px");

    cDivWidget.add(tp);
    tp.add(getReportTab("Added", added));
    tp.add(getReportTab("Changed", changed));
    tp.add(getReportTab("Deleted", deleted));

    tp.selectTab(0);
    tp.addStyleName("cardBorderShadow");

    new DialogHelper(true)
        .show(
            DO_YOU_WANT_TO_CONTINUE, cDivWidget, closeListener, 600, 1000);
  }

  @NotNull
  private Label getLabel(String messageStr) {
    Label msgLabel = new Label(messageStr);
    msgLabel.addStyleName("bulk-update-modal-msg-label");
    return msgLabel;
  }

  protected int addLabel(Grid g, int row, int col, String string, boolean isCentered) {
    SafeHtmlBuilder shb = new SafeHtmlBuilder();
    shb.appendEscapedLines(string);
    HTML idlblVal = new HTML(shb.toSafeHtml());
    g.setWidget(row, col, idlblVal);
    if (isCentered) {
      g.getCellFormatter().addStyleName(row, col, "centered-col");
    }
    return ++col;
  }

  private TabPane getReportTab(String prefix, Collection<DominoUpdateItem> items) {
    int numMatches = items.size();
    String suffix = numMatches == 0 ? "" : " (" + numMatches + ")";
    TabPane dwPane = new TabPane(prefix + suffix);

    if (numMatches == 0) {
      com.github.gwtbootstrap.client.ui.Label w = new com.github.gwtbootstrap.client.ui.Label(NOTHING + " " + prefix.toLowerCase() + ".");
      w.addStyleName("topFiveMargin");
      w.setType(LabelType.INFO);
      dwPane.add(w);
    } else {
      log.info("getUnmatchedRows Num unmatched : " + numMatches);
      Panel tableWithPager = new DominoUpdateResponseSimplePagingContainer(controller, prefix).getTableWithPager(items);
      dwPane.add(tableWithPager);
    }
    return dwPane;
  }
}