/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

package mitll.langtest.client.analysis;

import com.github.gwtbootstrap.client.ui.*;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.github.gwtbootstrap.client.ui.constants.ToggleType;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.custom.Navigation;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.shared.analysis.PhoneReport;
import mitll.langtest.shared.analysis.WordScore;

import java.util.List;
import java.util.logging.Logger;

/**
 * Created by go22670 on 10/21/15.
 */
public class AnalysisTab extends DivWidget {
  private final Logger logger = Logger.getLogger("AnalysisTab");

  private static final String WORDS = "Words";
  private static final String WORDS_USING_SOUND = "Words using Sound";
  private static final String SOUNDS = "Sounds";
  private static final String SUBTITLE = "scores > 20";
  private boolean isNarrow = false;

  enum TIME_HORIZON {WEEK, MONTH, ALL}

  private final AnalysisPlot analysisPlot;
  private final ExerciseController controller;
  private TimeWidgets timeWidgets;
  private Heading exampleHeader = new Heading(3, WORDS_USING_SOUND);

  /**
   * @param service
   * @param controller
   * @param userid
   * @param minRecordings
   * @see Navigation#showAnalysis()
   * @see UserContainer#gotClickOnItem
   */
  public AnalysisTab(final LangTestDatabaseAsync service, final ExerciseController controller, final int userid,
                     final ShowTab showTab, String userChosenID, int minRecordings, DivWidget overallBottom) {
    this.controller = controller;
    getElement().setId("AnalysisTab");
    Icon playFeedback = new Icon(IconType.PLAY);
    analysisPlot = new AnalysisPlot(service, userid, userChosenID, minRecordings, controller.getSoundManager(), playFeedback);

    Panel timeControls = new HorizontalPanel();
    timeControls.add(getTimeGroup());
    timeControls.add(getTimeWindowStepper());

    playFeedback.addStyleName("leftFiveMargin");
    // timeControls.add(playFeedback);
    playFeedback.setVisible(false);

    add(timeControls);
    add(analysisPlot);

    Panel bottom = new HorizontalPanel();
    bottom.getElement().setId("bottom");
    bottom.addStyleName("floatLeft");

    if (overallBottom != null) { // are we in student or teacher view
      overallBottom.add(bottom); // teacher
      isNarrow = true;
    } else {
      add(bottom); // student
    }

    getWordScores(service, controller, userid, showTab, analysisPlot, bottom, minRecordings);
  }

  private Panel getTimeWindowStepper() {
    Panel stepper = getStepperContainer();

    Button prevButton = getPrevButton();
    stepper.add(prevButton);

    HTML currentDate = getCurrentTimeWindow();
    stepper.add(currentDate);

    Button nextButton = getNextButton();
    stepper.add(nextButton);

    timeWidgets = new TimeWidgets(prevButton, nextButton, currentDate, allChoice, weekChoice, monthChoice);
    return stepper;
  }

  private Panel getStepperContainer() {
    Panel stepper = new HorizontalPanel();
    stepper.getElement().setId("stepper");
    stepper.addStyleName("inlineBlockStyleOnly");
    stepper.addStyleName("topMargin");
    stepper.addStyleName("leftTenMargin");
    return stepper;
  }

  private HTML getCurrentTimeWindow() {
    HTML currentDate = new HTML();
    currentDate.setWidth("80px");
    currentDate.addStyleName("boxShadow");
    currentDate.addStyleName("leftFiveMargin");
    currentDate.addStyleName("topFiveMargin");
    currentDate.addStyleName("rightFiveMargin");
    return currentDate;
  }

  private Button getPrevButton() {
    final Button left = new Button();
    controller.register(left, "prevTimeWindow");
    left.setIcon(IconType.CARET_LEFT);
    left.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        analysisPlot.gotPrevClick(timeWidgets);
      }
    });
    left.setEnabled(false);
    return left;
  }

  private Button getNextButton() {
    final Button right = new Button();
    right.setIcon(IconType.CARET_RIGHT);
    controller.register(right, "timeWindowAdvance");
    right.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        analysisPlot.gotNextClick(timeWidgets);
      }
    });
    right.setEnabled(false);
    return right;
  }

  private Button allChoice;
  private Button weekChoice;
  private Button monthChoice;

  /**
   * @return
   * @see
   */
  private Widget getTimeGroup() {
    ButtonToolbar w = new ButtonToolbar();
    ButtonGroup buttonGroup = new ButtonGroup();
    w.add(buttonGroup);
    buttonGroup.addStyleName("topMargin");
    buttonGroup.addStyleName("leftTenMargin");
    buttonGroup.setToggle(ToggleType.RADIO);
    buttonGroup.add(weekChoice = getWeekChoice());
    buttonGroup.add(monthChoice = getMonthChoice());
    allChoice = getAllChoice();
    buttonGroup.add(allChoice);

    return buttonGroup;
  }

  private Button getWeekChoice() {
    return getButton(controller, getClickHandler(TIME_HORIZON.WEEK), "Week");
  }

  private Button getButton(ExerciseController controller, ClickHandler handler, String week) {
    Button onButton = new Button(week);
    onButton.getElement().setId(week + "Choice");
    controller.register(onButton);
    onButton.addClickHandler(handler);

    return onButton;
  }

  private Button getMonthChoice() {
    return getButton(controller, getClickHandler(TIME_HORIZON.MONTH), "Month");
  }

  private ClickHandler getClickHandler(final TIME_HORIZON month) {
    return new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        analysisPlot.setTimeHorizon(month, timeWidgets);
      }
    };
  }

  private Button getAllChoice() {
    Button all = getButton(controller, getClickHandler(TIME_HORIZON.ALL), "All");
    all.setActive(true);
    return all;
  }

  /**
   * @param service
   * @param controller
   * @param userid
   * @param showTab
   * @param analysisPlot
   * @param lowerHalf
   * @param minRecordings
   */
  private void getWordScores(final LangTestDatabaseAsync service, final ExerciseController controller,
                             final int userid, final ShowTab showTab, final AnalysisPlot analysisPlot,
                             final Panel lowerHalf, final int minRecordings) {
    service.getWordScores(userid, minRecordings, new AsyncCallback<List<WordScore>>() {
      @Override
      public void onFailure(Throwable throwable) {
        logger.warning("Got " + throwable);
      }

      @Override
      public void onSuccess(List<WordScore> wordScores) {
        Heading wordsTitle = new Heading(3, WORDS, SUBTITLE);
        final WordContainer wordContainer = new WordContainer(controller, analysisPlot, showTab, wordsTitle);
        Panel tableWithPager = wordContainer.getTableWithPager(wordScores);

        DivWidget wordsContainer = getWordContainerDiv(tableWithPager, "WordsContainer", wordsTitle);
        wordsContainer.addStyleName("cardBorderShadow");
        wordsContainer.getElement().getStyle().setMargin(10, Style.Unit.PX);

        lowerHalf.add(wordsContainer);

        DivWidget soundsDiv = getSoundsDiv();

        lowerHalf.add(soundsDiv);
        getPhoneReport(service, controller, userid, soundsDiv, analysisPlot, showTab, minRecordings);
      }
    });
  }

  private DivWidget getSoundsDiv() {
    DivWidget soundsDiv = new DivWidget();
    soundsDiv.getElement().setId("soundsDiv");
    soundsDiv.getElement().getStyle().setProperty("minHeight", 325, Style.Unit.PX);
    soundsDiv.addStyleName("cardBorderShadow");
    soundsDiv.addStyleName("floatRight");
    soundsDiv.getElement().getStyle().setMargin(10, Style.Unit.PX);
    return soundsDiv;
  }

  private DivWidget getWordContainerDiv(Panel tableWithPager, String containerID, Heading w) {
    DivWidget wordsContainer = new DivWidget();
    wordsContainer.getElement().setId(containerID);
    wordsContainer.addStyleName("floatLeft");
    wordsContainer.add(w);
    wordsContainer.add(tableWithPager);
    return wordsContainer;
  }

  private void getPhoneReport(LangTestDatabaseAsync service,
                              final ExerciseController controller,
                              int userid,
                              final Panel lowerHalf,
                              AnalysisPlot analysisPlot,
                              final ShowTab showTab,
                              final int minRecordings) {
    final PhoneExampleContainer exampleContainer = new PhoneExampleContainer(controller, showTab, exampleHeader);
    final PhonePlot phonePlot = new PhonePlot();
    final PhoneContainer phoneContainer = new PhoneContainer(controller, exampleContainer, phonePlot, isNarrow);
    analysisPlot.addListener(phoneContainer);

    service.getPhoneScores(userid, minRecordings, new AsyncCallback<PhoneReport>() {
      @Override
      public void onFailure(Throwable throwable) {
        logger.warning("Got " + throwable);
      }

      @Override
      public void onSuccess(PhoneReport phoneReport) {
        // #1 - phones
        Panel phones = phoneContainer.getTableWithPager(phoneReport);
        lowerHalf.add(getSoundsContainer(phones));

        // #2 - word examples
        Panel examples = exampleContainer.getTableWithPager();
        lowerHalf.add(getWordExamples(examples));

        // #3 - phone plot
        phonePlot.addStyleName("topMargin");
        phonePlot.addStyleName("floatLeft");
        lowerHalf.add(phonePlot);

        phoneContainer.showExamplesForSelectedSound();
      }
    });
  }

  private DivWidget getSoundsContainer(Panel phones) {
    DivWidget sounds = new DivWidget();
    sounds.getElement().setId("SoundsContainer");
    sounds.add(new Heading(3, SOUNDS));
    sounds.addStyleName("floatLeft");
    sounds.add(phones);
    return sounds;
  }


  private DivWidget getWordExamples(Panel examples) {
    DivWidget wordExamples = getWordContainerDiv(examples, "WordExamples", exampleHeader);
    wordExamples.getElement().getStyle().setMarginLeft(5, Style.Unit.PX);
    return wordExamples;
  }
}
