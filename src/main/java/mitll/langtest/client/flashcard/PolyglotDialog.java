package mitll.langtest.client.flashcard;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.FluidRow;
import com.github.gwtbootstrap.client.ui.Heading;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import mitll.langtest.client.dialog.DialogHelper;

import java.util.Collections;

class PolyglotDialog {
  /**
   * @param num
   * @param closeListener
   * @see StatsFlashcardFactory#showPolyDialog
   */
  PolyglotDialog(int minutes, int num, int minScore, DialogHelper.CloseListener closeListener) {
    DivWidget container = new DivWidget();

    FluidRow row = new FluidRow();
    Heading w = new Heading(4, "Lightning Round!");
    w.addStyleName("blueColor");
    row.add(w);
    container.add(row);

    row = getContentRow(minutes, num,minScore);
    container.add(row);

    String title = "Lightning Round";

    Button closeButton = new DialogHelper(true)
        .show(
            title,
            Collections.emptyList(),
            container,
            "Start!",
            "Cancel",
            closeListener, 550);
    closeButton.setType(ButtonType.SUCCESS);
    closeButton.setEnabled(true);
  }

  private FluidRow getContentRow(int minutes, int num, int minScore) {
    FluidRow row;
    row = new FluidRow();
    row.add(new Heading(5, "You have " +
        minutes +
        " minutes to try to complete all " + num + " items."));
    row.add(new Heading(5, "You are not required to complete all the items, but your final score combines both pronunciation quality and number attempted."));
    Heading w = new Heading(5, "");
    w.setWidth("100%");
    row.add(w);
    row.add(new Heading(5, "You will advance automatically to the next item when your score is above " +minScore+
        "."));

    row.add(new Heading(4, "Click start to begin -->"));
    return row;
  }
}
