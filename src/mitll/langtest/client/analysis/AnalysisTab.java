/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

package mitll.langtest.client.analysis;

import com.github.gwtbootstrap.client.ui.*;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.constants.ToggleType;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
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
  private static final String WORDS = "Words";
  private static final String WORDS_USING_SOUND = "Words using Sound";
  private static final String SOUNDS = "Sounds";
  private static final String SUBTITLE = "scores > 20";
  private final Logger logger = Logger.getLogger("AnalysisTab");
  private boolean isNarrow = false;

  enum TIME_HORIZON {WEEK, MONTH, ALL}

  ;

  TIME_HORIZON timeHorizon = TIME_HORIZON.ALL;
  AnalysisPlot analysisPlot;

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
    getElement().setId("AnalysisTab");
    analysisPlot = new AnalysisPlot(service, userid, userChosenID, minRecordings,
        controller.getSoundManager());
    analysisPlot.addStyleName("cardBorderShadow");
    analysisPlot.getElement().getStyle().setMargin(10, Style.Unit.PX);

    add(getTimeGroup(controller));
    add(analysisPlot);

    Panel bottom = new HorizontalPanel();
    bottom.getElement().setId("bottom");
    bottom.addStyleName("floatLeft");

    if (overallBottom != null) {
      overallBottom.add(bottom);
      isNarrow = true;
    } else {
      add(bottom);
    }

    getWordScores(service, controller, userid, showTab, analysisPlot, bottom, minRecordings);
  }

  /**
   * @param controlState
   * @return
   * @see #getRightColumn(mitll.langtest.client.flashcard.ControlState)
   */
  private Widget getTimeGroup(ExerciseController controller) {
    //  ControlGroup group = new ControlGroup("TIME");// + " " + controller.getLanguage().toUpperCase());

//    Icon widget = new Icon(IconType.VOLUME_UP);
//    widget.addStyleName("leftFiveMargin");
//    group.add(widget);
//

    ButtonToolbar w = new ButtonToolbar();
    w.addStyleName("topFiveMargin");
    //group.add(w);
    ButtonGroup buttonGroup = new ButtonGroup();
    w.add(buttonGroup);

    buttonGroup.setToggle(ToggleType.RADIO);
    buttonGroup.add(getWeekChoice(controller));
    buttonGroup.add(getMonthChoice(controller));
    buttonGroup.add(getAllChoice(controller));

    return buttonGroup;
  }

  private Button getWeekChoice(ExerciseController controller) {
    Button onButton = new Button("Week");
    onButton.getElement().setId("WeekChoice");
    controller.register(onButton);

    onButton.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        logger.warning("got WEEK click");
        analysisPlot.setTimeHorizon(TIME_HORIZON.WEEK);
      }
    });

    return onButton;
  }

  private Button getMonthChoice(ExerciseController controller) {
    Button onButton = new Button("Month");
    onButton.getElement().setId("MonthChoice");
    controller.register(onButton);

    onButton.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        logger.warning("got MONTH click");

        analysisPlot.setTimeHorizon(TIME_HORIZON.MONTH);

      }
    });

    return onButton;
  }

  private Button getAllChoice(ExerciseController controller) {
    Button onButton = new Button("All");
    onButton.getElement().setId("AllChoice");
    controller.register(onButton);

    onButton.setActive(true);

    onButton.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        analysisPlot.setTimeHorizon(TIME_HORIZON.ALL);
      }
    });

    return onButton;
  }

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
        final WordContainer wordContainer = new WordContainer(controller, analysisPlot, showTab);
        Panel tableWithPager = wordContainer.getTableWithPager(wordScores);

        DivWidget wordsContainer = getWordContainerDiv(tableWithPager, "WordsContainer", new Heading(3, WORDS, SUBTITLE));
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
    DivWidget vert = new DivWidget();
    vert.getElement().setId(containerID);
    vert.addStyleName("floatLeft");
    vert.add(w);
    vert.add(tableWithPager);
    return vert;
  }

  private void getPhoneReport(LangTestDatabaseAsync service,
                              final ExerciseController controller,
                              int userid,
                              final Panel lowerHalf,
                              AnalysisPlot analysisPlot,
                              final ShowTab showTab,
                              final int minRecordings) {
    final PhoneExampleContainer exampleContainer = new PhoneExampleContainer(controller, analysisPlot, showTab);
    final PhonePlot phonePlot = new PhonePlot();
    final PhoneContainer phoneContainer = new PhoneContainer(controller, exampleContainer, phonePlot, isNarrow);

    service.getPhoneScores(userid, minRecordings, new AsyncCallback<PhoneReport>() {
      @Override
      public void onFailure(Throwable throwable) {
        logger.warning("Got " + throwable);
      }

      @Override
      public void onSuccess(PhoneReport phoneReport) {
        // logger.info("getPhoneScores " + phoneReport);
        // #1 - phones
        Panel phones = phoneContainer.getTableWithPager(phoneReport);
        DivWidget sounds = getSoundsContainer(phones);
        lowerHalf.add(sounds);

        // #2 - word examples
        Panel examples = exampleContainer.getTableWithPager();
        DivWidget wordExamples = getWordExamples(examples);
        lowerHalf.add(wordExamples);

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
    DivWidget wordExamples = getWordContainerDiv(examples, "WordExamples", new Heading(3, WORDS_USING_SOUND));
    wordExamples.getElement().getStyle().setMarginLeft(5, Style.Unit.PX);
    return wordExamples;
  }
}
