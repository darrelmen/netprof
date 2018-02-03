package mitll.langtest.client.flashcard;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.FluidRow;
import com.github.gwtbootstrap.client.ui.Heading;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import mitll.langtest.client.dialog.DialogHelper;

import java.util.Collections;

class PolyglotDialog {
  private static final int HSIZE = 4;
  public static final String LIGHTNING_ROUND = "Lightning Round!";

  /**
   * @param num
   * @param closeListener
   * @see StatsFlashcardFactory#showPolyDialog
   */
  PolyglotDialog(int minutes, int num, int minScore, DialogHelper.CloseListener closeListener) {
    DivWidget container = new DivWidget();

    FluidRow row = new FluidRow();
    // Heading w = new Heading(4, "Lightning Round!");
    //  w.addStyleName("blueColor");
    // row.add(w);
    container.add(row);

    row = getContentRow(minutes, num, minScore);
    container.add(row);

    Button closeButton = new DialogHelper(true)
        .show(
            LIGHTNING_ROUND,
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
    row.add(new Heading(HSIZE, "You have " +
        minutes +
        " minute" +
        (minutes > 1 ? "s" : "") +
        " to <i>try</i> to complete all " + num + " items."));
    row.add(new Heading(HSIZE, "You are not required to complete all the items, but your final score combines both pronunciation quality and number attempted."));
    Heading w = new Heading(5, "");
    w.setWidth("100%");
    row.add(w);
    row.add(new Heading(HSIZE, "You will advance automatically to the next item when your score is above " + minScore +
        "."));

    row.add(new Heading(HSIZE - 1, "Click start to begin."));
    return row;
  }
}
