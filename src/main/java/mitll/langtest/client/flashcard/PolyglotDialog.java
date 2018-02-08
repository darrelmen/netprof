package mitll.langtest.client.flashcard;

import com.github.gwtbootstrap.client.ui.*;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.dialog.DialogHelper;

import java.util.Collections;

import static mitll.langtest.client.flashcard.StatsFlashcardFactory.MIN_POLYGLOT_SCORE;

public class PolyglotDialog {
  private static final int HSIZE = 4;
  private static final String LIGHTNING_ROUND = "Test your pronunciation!";

  public enum MODE_CHOICE {NOT_YET, DRY_RUN, POLYGLOT}

  public interface ModeChoiceListener {
    void gotMode(MODE_CHOICE choice);
  }

  Button closeButton;
  private ModeChoiceListener modeListener;

  /**
   * @param closeListener
   * @paramx num
   * @see StatsFlashcardFactory#showPolyDialog
   */
  public PolyglotDialog(int dryMin, int dryNum,
                 int compMin, int compNum,
                 int minScore,
                 DialogHelper.CloseListener closeListener,
                 ModeChoiceListener modeChoice) {
    DivWidget container = new DivWidget();

    this.modeListener = modeChoice;
    Widget row = new FluidRow();
    // Heading w = new Heading(4, "Lightning Round!");
    //  w.addStyleName("blueColor");
    // row.add(w);
    container.add(row);

    row = getContentRow(dryMin, dryNum, compMin, compNum, minScore);
    container.add(row);

    closeButton = new DialogHelper(true)
        .show(
            LIGHTNING_ROUND,
            Collections.emptyList(),
            container,
            "Start!",
            "Cancel",
            closeListener, 550);
    closeButton.setType(ButtonType.SUCCESS);
    closeButton.setEnabled(false);
  }

  private Widget getContentRow(int dryMin, int dryNum, int compMin, int compNum, int minScore ) {
    FluidContainer container = new FluidContainer();
   // Heading w1 = new Heading(HSIZE, "Netprof scores your pronunciation!");
   // w1.getElement().getStyle().setMarginBottom(20, Style.Unit.PX);
    FluidRow row = new FluidRow();
    container.add(row);
   // row.add(w1);

    row.add(new Heading(HSIZE, "Netprof automatically scores your pronunciation relative to a native speaker."));
    row.add(new Heading(HSIZE, "In the flashcards that follow, record yourself saying the prompted phrases."));
    row.add(new Heading(HSIZE, "To record : "));
    row.add(new Heading(HSIZE, " * PRESS and HOLD the space bar while recording <i>or</i>"));
    row.add(new Heading(HSIZE, " * CLICK and HOLD the mouse button."));
    row.add(new Heading(HSIZE, "Press arrow keys to go to next or previous item (if you want to repeat an item)."));
    row.add(new Heading(HSIZE, "Scores above " + minScore +
        " advance automatically to the next item."));
    row.add(new Heading(HSIZE, "You are not required to complete all the items, but your final score rewards completion."));
    //row.add(new Heading(HSIZE, "The dry run choice lets you practice recording."));

    row.add(new Heading(HSIZE, "Please choose either : "));
    Heading modeDep = new Heading(HSIZE+1, "");//No selection yet.");
    container.add(addModeChoices(dryMin, dryNum, compMin, compNum, modeDep));

    container.add(modeDep);//getModeText(minutes, num)));
    Heading w = new Heading(5, "");
    w.setWidth("100%");
    container.add(w);

//    row.add(new Heading(HSIZE, "You will advance automatically to the next item when your score is above " + minScore +
//        "."));

    container.add(new Heading(HSIZE - 1, "Click start to begin."));
    return container;
  }

  private Widget addModeChoices(int dryMin, int dryNum, int compMin, int compNum, Heading modeDep) {
    /**
     *
     */
    String GENDER_GROUP = "GenderGroup";
    RadioButton male = new RadioButton(GENDER_GROUP, "Dry Run (Just Practice!)");
    RadioButton female = new RadioButton(GENDER_GROUP, "Polyglot Competition");
    male.setEnabled(true);
  //  female.addStyleName("leftFiveMargin");
    DivWidget choice = new DivWidget();
//    choice.addStyleName("inlineFlex");
    choice.setWidth("100%");
    choice.add(male);
    choice.add(female);
    male.addClickHandler(event -> {
      modeListener.gotMode(MODE_CHOICE.DRY_RUN);
      modeDep.setText(getModeText(dryMin, dryNum));
      closeButton.setEnabled(true);

    });
    female.addClickHandler(event -> {
      modeListener.gotMode(MODE_CHOICE.POLYGLOT);
      modeDep.setText(getModeText(compMin, compNum));
      closeButton.setEnabled(true);

    });
    return choice;
  }

  private String getModeText(int minutes, int num) {
    return "You have " +
        minutes +
        " minute" +
        (minutes > 1 ? "s" : "") +
        " to <i>try</i> to complete all " + num + " items.";
  }
}
