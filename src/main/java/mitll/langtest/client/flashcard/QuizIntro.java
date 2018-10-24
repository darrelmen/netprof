package mitll.langtest.client.flashcard;

import com.github.gwtbootstrap.client.ui.*;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.banner.QuizHelper;
import mitll.langtest.client.dialog.DialogHelper;
import mitll.langtest.shared.custom.IUserList;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class QuizIntro extends DivWidget {
  private static final int HSIZE = 4;
  /**
   *
   */
  private static final String START = "Start!";
  public static final String DRY_RUN = "Dry Run";
  private static final String FLASHCARDS = "quiz";
  private static final String WELCOME_USER_ = "Welcome ";

  public enum MODE_CHOICE {NOT_YET, DRY_RUN, POLYGLOT}

  private static final int NATIVE_THRESHOLD = 70;
  private final Button closeButton;

  /**
   * @see QuizHelper.MyPracticeFacetExerciseList
   * @param idToList
   * @param closeListener
   * @param userID
   */
  public QuizIntro(Map<Integer, IUserList> idToList,
                   QuizHelper.QuizChoiceListener closeListener,
                   String userID) {
    add(getContentRow(idToList, userID));

    addStyleName("cardBorderShadow");

    this.closeButton = new DialogHelper(false).getCloseButton(START);

    closeButton.addClickHandler(event -> {
      closeButton.setEnabled(false);
      closeListener.gotChoice(listID);
      closeButton.setEnabled(true);
    });

    {
      FluidRow row2 = new FluidRow();
      row2.addStyleName("topFiveMargin");
      row2.getElement().setId("buttonRow");
      row2.add(new Column(2, 10, closeButton));
      add(row2);
    }

    closeButton.setType(ButtonType.SUCCESS);
    closeButton.setEnabled(false);
  }

  private Widget getContentRow(Map<Integer, IUserList> idToList, String userID) {
    FluidContainer container = new FluidContainer();

    FluidRow row = new FluidRow();


    Heading welcome = new Heading(3, WELCOME_USER_);
    Heading welcome2 = new Heading(3, userID);
    welcome2.getElement().getStyle().setColor("blue");

    DivWidget h = new DivWidget();
    h.setWidth("100%");
    h.addStyleName("floatLeftAndClear");
    h.add(welcome);
    welcome.addStyleName("floatLeft");
    h.add(welcome2);
    welcome2.addStyleName("leftFiveMargin");
    welcome2.addStyleName("floatLeft");

    //h.addStyleName("bottomFiveMargin");

    container.add(h);
    container.add(row);

    addBottom(welcome);
    row.add(new Heading(HSIZE, "Netprof automatically scores your pronunciation relative to a native speaker."));
    Heading w = new Heading(HSIZE, "In the " + FLASHCARDS + " that follows, record yourself saying the prompted phrases.");
    row.add(w);
    addBottom(w);
    row.add(new Heading(HSIZE, "To record : "));
    row.add(new Heading(HSIZE, " * PRESS and HOLD the space bar while recording <i>or</i>"));
    Heading w1 = new Heading(HSIZE, " * CLICK and HOLD the mouse button.");
    row.add(w1);
    addBottom(w1);
    Heading w2 = new Heading(HSIZE, "Native speakers score in the range of " +
        NATIVE_THRESHOLD +
        " and above.");
    row.add(w2);
    addBottom(w2);

    // row.add(new Heading(HSIZE, "Scores above " + minScore + " advance automatically."));
    row.add(new Heading(HSIZE, "Press arrow keys to go to next or previous item. "));// (if you want to repeat an item)."));
      row.add(new Heading(HSIZE, "Click on a dot in the graph below to repeat to an item."));
    //  row.add(new Heading(HSIZE, YOU_ARE_NOT_REQUIRED));//, but your final score rewards completion."));

    container.add(new Heading(HSIZE, "Please choose a quiz : "));

    {
      Heading modeDep = getUserNotice();
      Heading modeDep2 = getUserNotice();
      container.add(addModeChoices(modeDep, modeDep2, idToList));
      container.add(modeDep);
      container.add(modeDep2);
    }
    return container;
  }

  @NotNull
  private Heading getUserNotice() {
    Heading modeDep = new Heading(HSIZE, "");
    modeDep.setHeight(14 + "px");
    modeDep.getElement().getStyle().setColor("blue");
    return modeDep;
  }

  private void addBottom(Heading w) {
    w.getElement().getStyle().setMarginBottom(25, Style.Unit.PX);
  }

  private int listID = -1;
  private final List<IUserList> choicesAdded = new ArrayList<>();

  private Widget addModeChoices(Heading modeDep, Heading modeDep2, Map<Integer, IUserList> idToList) {
    DivWidget choiceDiv = new DivWidget();
    choiceDiv.setWidth("100%");

    choicesAdded.clear();
    ListBox choices = new ListBox();

    choices.addItem("-- no choice yet --");
    choiceDiv.add(choices);
    choices.addChangeHandler(event -> {
      if (choices.getSelectedIndex() == 0) {
        modeDep.setText("");
        listID = -1;
        closeButton.setEnabled(false);
      } else {
        IUserList iUserList = choicesAdded.get(choices.getSelectedIndex() - 1);
        onQuizChoice(modeDep, modeDep2, iUserList.getID(), iUserList);
      }
    });
    idToList.forEach((k, v) -> {
      if (isDry(v)) {
        maybeAddChoice(choices, v);
      }
    });

    idToList.forEach((k, v) -> {
      if (!isDry(v)) {
        maybeAddChoice(choices, v);
      }
    });

    return choiceDiv;
  }

  private boolean isDry(IUserList v) {
    return v.getName().startsWith(DRY_RUN);
  }

  private void maybeAddChoice(ListBox choices, IUserList v) {
    if (v.getNumItems() > 0) {
      choices.addItem(v.getName());
      choicesAdded.add(v);
    }
  }

  private void onQuizChoice(Heading modeDep, Heading modeDep2, Integer k, IUserList v) {
    int numItems = v.getNumItems();
    modeDep.setText(getModeText(v.getRoundTimeMinutes(), numItems));
    modeDep2.setText("Scores above " + v.getMinScore() + " advance automatically.");
    listID = k;
    closeButton.setEnabled(true);
  }

  private String getModeText(int minutes, int num) {
    return "You have " +
        minutes +
        " minute" +
        (minutes > 1 ? "s" : "") +
        " to try to complete all " + num + " items.";
  }
}
