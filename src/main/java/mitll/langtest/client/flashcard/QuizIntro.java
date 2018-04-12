package mitll.langtest.client.flashcard;

import com.github.gwtbootstrap.client.ui.*;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.banner.QuizHelper;
import mitll.langtest.client.dialog.DialogHelper;
import mitll.langtest.shared.custom.IUserList;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class QuizIntro extends DivWidget {
  private static final int HSIZE = 4;
  //  private static final String GENDER_GROUP = "GenderGroup";
  private static final String START = "Start!";
  private static final String YOU_ARE_NOT_REQUIRED = "You are not required to record all the items.";

  public enum MODE_CHOICE {NOT_YET, DRY_RUN, POLYGLOT}

  private static final int MIN_POLYGLOT_SCORE = 35;
  private final Button closeButton;

  public QuizIntro(Map<Integer, IUserList> idToList,
                   QuizHelper.QuizChoiceListener closeListener, String userID) {
    this(idToList, MIN_POLYGLOT_SCORE, closeListener, userID);
  }

  /**
   * @param closeListener
   * @see mitll.langtest.client.banner.NewContentChooser#showPolyDialog
   */
  private QuizIntro(Map<Integer, IUserList> idToList,
                    int minScore,
                    QuizHelper.QuizChoiceListener closeListener, String userID) {
    add(getContentRow(minScore, idToList, userID));

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

  private Widget getContentRow(int minScore, Map<Integer, IUserList> idToList, String userID) {
    FluidContainer container = new FluidContainer();

    FluidRow row = new FluidRow();
    container.add(row);

    Heading welcome = new Heading(3, "Welcome User " + userID);
    row.add(welcome);
    addBottom(welcome);
    row.add(new Heading(HSIZE, "Netprof automatically scores your pronunciation relative to a native speaker."));
    Heading w = new Heading(HSIZE, "In the flashcards that follow, record yourself saying the prompted phrases.");
    row.add(w);
    addBottom(w);
    row.add(new Heading(HSIZE, "To record : "));
    row.add(new Heading(HSIZE, " * PRESS and HOLD the space bar while recording <i>or</i>"));
    Heading w1 = new Heading(HSIZE, " * CLICK and HOLD the mouse button.");
    row.add(w1);
    addBottom(w1);
    Heading w2 = new Heading(HSIZE, "Native speakers score in the range of 75 and above.");
    row.add(w2);
    addBottom(w2);

    row.add(new Heading(HSIZE, "Scores above " + minScore + " advance automatically."));
    row.add(new Heading(HSIZE, "Press arrow keys to go to next or previous item. "));// (if you want to repeat an item)."));
    row.add(new Heading(HSIZE, "Click on an item in the chart to jump to that item."));// (if you want to repeat an item)."));
    row.add(new Heading(HSIZE, YOU_ARE_NOT_REQUIRED));//, but your final score rewards completion."));

    container.add(new Heading(HSIZE, "Please choose a quiz : "));

    {
      Heading modeDep = new Heading(HSIZE, "");
      modeDep.setHeight(14 + "px");
      modeDep.getElement().getStyle().setColor("blue");
      container.add(addModeChoices(modeDep, idToList));
      container.add(modeDep);
    }
    return container;
  }

  private void addBottom(Heading w) {
    w.getElement().getStyle().setMarginBottom(25, Style.Unit.PX);
  }

  private int listID = -1;
  private List<IUserList> choicesAdded = new ArrayList<>();

  private Widget addModeChoices(Heading modeDep, Map<Integer, IUserList> idToList) {
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
        onQuizChoice(modeDep, iUserList.getID(), iUserList);
      }
    });
    idToList.forEach((k, v) -> {
      boolean isDry = v.getName().startsWith("Dry Run");
      if (isDry) {
        maybeAddChoice(choices, modeDep, choiceDiv, k, v);
      }
    });

    idToList.forEach((k, v) -> {
      boolean isDry = v.getName().startsWith("Dry Run");
      if (!isDry) {
        maybeAddChoice(choices, modeDep, choiceDiv, k, v);
      }
    });

    return choiceDiv;
  }

  private void maybeAddChoice(ListBox choices, Heading modeDep, DivWidget choiceDiv, Integer k, IUserList v) {
    if (v.getNumItems() > 0) {
      choices.addItem(v.getName());
      choicesAdded.add(v);
      //addChoice(modeDep, choiceDiv, k, v);
    }
  }

/*  private void addChoice(Heading modeDep, DivWidget choiceDiv, Integer k, IUserList v) {
    RadioButton choice = new RadioButton(GENDER_GROUP, v.getName());
    choice.addClickHandler(event -> {
      onQuizChoice(modeDep, k, v);
    });
    choiceDiv.add(choice);
  }*/

  private void onQuizChoice(Heading modeDep, Integer k, IUserList v) {
    int numItems = v.getNumItems();
    int i = Math.max(1, numItems / 10);
    modeDep.setText(getModeText(i, numItems));
    listID = k;
    closeButton.setEnabled(true);
  }

  private String getModeText(int minutes, int num) {
    return "You have " +
        minutes +
        " minute" +
        (minutes > 1 ? "s" : "") +
        " to <i>try</i> to complete all " + num + " items.";
  }
}
