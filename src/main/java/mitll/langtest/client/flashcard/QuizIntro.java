package mitll.langtest.client.flashcard;

import com.github.gwtbootstrap.client.ui.*;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.banner.QuizHelper;
import mitll.langtest.client.dialog.DialogHelper;
import mitll.langtest.shared.custom.IUserList;

import java.util.Map;

public class QuizIntro extends DivWidget {
  private static final int HSIZE = 4;
  // private static final String LIGHTNING_ROUND = "Test your pronunciation!";
  private static final String GENDER_GROUP = "GenderGroup";
 // private static final String DRY_RUN = "Dry Run (Just Practice!)";
 // private static final String POLYGLOT_COMPETITION = "Timed Quiz";//"Polyglot Competition";
  private static final String START = "Start!";
 // private static final String CANCEL = "Cancel";
  private static final String YOU_ARE_NOT_REQUIRED = "You are not required to record all the items.";
  // private static final boolean SHOW_AUDIO_PROMPT = false;

  public enum MODE_CHOICE {NOT_YET, DRY_RUN, POLYGLOT}

/*
  public enum PROMPT_CHOICE {NOT_YET, PLAY, DONT_PLAY}
*/

/*  private static final int DRY_RUN_MINUTES = 1;
  private static final int ROUND_MINUTES = 10;

  private static final int DRY_NUM = 10;
  private static final int COMP_NUM = 100;*/

  private static final int MIN_POLYGLOT_SCORE = 35;

/*  public interface ModeChoiceListener {
    void gotMode(MODE_CHOICE choice);

    void gotPrompt(PROMPT_CHOICE choice);
  }*/

  private final Button closeButton;
  //private final ModeChoiceListener modeListener;

  public QuizIntro(Map<Integer, IUserList> idToList,
                   QuizHelper.QuizChoiceListener closeListener) {
    this(idToList, MIN_POLYGLOT_SCORE,
        closeListener);
  }

  /**
   * @param closeListener
   * @see mitll.langtest.client.banner.NewContentChooser#showPolyDialog
   */
  private QuizIntro(Map<Integer, IUserList> idToList,


                    int minScore,

                    QuizHelper.QuizChoiceListener closeListener) {
    //DivWidget container = new DivWidget();
    // this.modeListener = modeChoice;
    Widget row = new FluidRow();
    add(row);

    row = getContentRow(minScore, idToList);
    add(row);

    FluidRow row2 = new FluidRow();
    row2.addStyleName("topFiveMargin");
    row2.getElement().setId("buttonRow");
    this.closeButton = new DialogHelper(false).getCloseButton(START);
    row2.add(new Column(2, 10, closeButton));

    closeButton.addClickHandler(event -> {
      closeButton.setEnabled(false);
      closeListener.gotChoice(listID);
      closeButton.setEnabled(true);
    });

    add(row2);

//    closeButton = new DialogHelper(true)
//        .show(
//            LIGHTNING_ROUND,
//            Collections.emptyList(),
//            container,
//            START,
//            CANCEL,
//            closeListener, 550);
    closeButton.setType(ButtonType.SUCCESS);
    closeButton.setEnabled(false);
  }

  private Widget getContentRow(int minScore, Map<Integer, IUserList> idToList) {
    FluidContainer container = new FluidContainer();

    FluidRow row = new FluidRow();
    container.add(row);

    row.add(new Heading(HSIZE, "Netprof automatically scores your pronunciation relative to a native speaker."));
    row.add(new Heading(HSIZE, "In the flashcards that follow, record yourself saying the prompted phrases."));
    row.add(new Heading(HSIZE, "To record : "));
    row.add(new Heading(HSIZE, " * PRESS and HOLD the space bar while recording <i>or</i>"));
    row.add(new Heading(HSIZE, " * CLICK and HOLD the mouse button."));
    String text = "Scores above " + minScore + " advance automatically.";
    row.add(new Heading(HSIZE, "Press arrow keys to go to next or previous item. " + text));// (if you want to repeat an item)."));
    // row.add(new Heading(HSIZE, text));
    row.add(new Heading(HSIZE, YOU_ARE_NOT_REQUIRED));//, but your final score rewards completion."));

/*
    if (SHOW_AUDIO_PROMPT) {
      {
        container.add(new Heading(HSIZE, "Do you want to hear each item before recording?"));
        container.add(addPromptChoices());
      }
    }
*/

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

  int listID = -1;

  private Widget addModeChoices(Heading modeDep,
                                Map<Integer, IUserList> idToList) {
    DivWidget choiceDiv = new DivWidget();
    choiceDiv.setWidth("100%");
    idToList.forEach((k, v) -> {
      RadioButton choice = new RadioButton(GENDER_GROUP, v.getName());
      //boolean isDry = v.getName().equalsIgnoreCase("Dry Run");
//      if (isDry) {
//
//      }

      choice.addClickHandler(event -> {
        //modeListener.gotMode(MODE_CHOICE.DRY_RUN);
        int i = Math.max(1, v.getNumItems() / 10);
        modeDep.setText(getModeText(i, v.getNumItems()));
        listID = k;
        closeButton.setEnabled(true);
      });
      choiceDiv.add(choice);

    });

/*
    RadioButton male = new RadioButton(genderGroup, DRY_RUN);
    RadioButton female = new RadioButton(genderGroup, POLYGLOT_COMPETITION);


    {
//      male.setEnabled(true);
      male.addClickHandler(event -> {
        modeListener.gotMode(MODE_CHOICE.DRY_RUN);
        modeDep.setText(getModeText(dryMin, dryNum));
        closeButton.setEnabled(true);

      });
    }

    female.addClickHandler(event -> {
      modeListener.gotMode(MODE_CHOICE.POLYGLOT);
      modeDep.setText(getModeText(compMin, compNum));
      closeButton.setEnabled(true);
    });
*/


    //   choice.add(female);

    return choiceDiv;
  }

/*  private Widget addPromptChoices() {
    String genderGroup = "prompt";
    RadioButton playPrompt = new RadioButton(genderGroup, "Yes");
    RadioButton dontPlayPrompt = new RadioButton(genderGroup, "No");

    playPrompt.addClickHandler(event -> modeListener.gotPrompt(PROMPT_CHOICE.PLAY));
    dontPlayPrompt.addClickHandler(event -> modeListener.gotPrompt(PROMPT_CHOICE.DONT_PLAY));
    dontPlayPrompt.setValue(true);

    modeListener.gotPrompt(PROMPT_CHOICE.DONT_PLAY);

    DivWidget choice = new DivWidget();
    choice.setWidth("100%");
    choice.add(playPrompt);
    choice.add(dontPlayPrompt);

    return choice;
  }*/

  private String getModeText(int minutes, int num) {
    return "You have " +
        minutes +
        " minute" +
        (minutes > 1 ? "s" : "") +
        " to <i>try</i> to complete all " + num + " items.";
  }
}
