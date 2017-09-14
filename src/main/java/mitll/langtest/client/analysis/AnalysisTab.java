/*
 *
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2015. Other request for this document shall be referred
 * to DLIFLC.
 *
 * WARNING: This document may contain technical data whose export is restricted
 * by the Arms Export Control Act (AECA) or the Export Administration Act (EAA).
 * Transfer of this data by any means to a non-US person who is not eligible to
 * obtain export-controlled data is prohibited. By accepting this data, the consignee
 * agrees to honor the requirements of the AECA and EAA. DESTRUCTION NOTICE: For
 * unclassified, limited distribution documents, destroy by any method that will
 * prevent disclosure of the contents or reconstruction of the document.
 *
 * This material is based upon work supported under Air Force Contract No.
 * FA8721-05-C-0002 and/or FA8702-15-D-0001. Any opinions, findings, conclusions
 * or recommendations expressed in this material are those of the author(s) and
 * do not necessarily reflect the views of the U.S. Air Force.
 *
 * © 2015 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 *
 *
 */

package mitll.langtest.client.analysis;

import com.github.gwtbootstrap.client.ui.*;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.github.gwtbootstrap.client.ui.constants.ToggleType;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.banner.NewContentChooser;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.services.AnalysisService;
import mitll.langtest.client.services.AnalysisServiceAsync;
import mitll.langtest.shared.analysis.AnalysisReport;
import mitll.langtest.shared.analysis.PhoneReport;
import mitll.langtest.shared.analysis.WordScore;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.logging.Logger;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 10/21/15.
 */
public class AnalysisTab extends DivWidget {
  private final Logger logger = Logger.getLogger("AnalysisTab");
  private static final int MIN_HEIGHT = 325;
  /**
   * Got be the width to fit on a laptop screen for progress view - otherwise the phone plot
   * will wrap.
   *
   * @see #showWordScores
   */
  private static final int WORD_WIDTH = 450;

  private static final String WORDS = "Vocabulary";
  /**
   * @see #exampleHeader
   */
  private static final String WORDS_USING_SOUND = "Words using Sound";
  private static final String SOUNDS = "Sounds";
  private static final String SUBTITLE = "scores > 20";
  private final AnalysisServiceAsync analysisServiceAsync = GWT.create(AnalysisService.class);

  enum TIME_HORIZON {WEEK, MONTH, ALL}

  private final AnalysisPlot analysisPlot;
  private final ExerciseController controller;
  private TimeWidgets timeWidgets;
  private final Heading exampleHeader = new Heading(3, WORDS_USING_SOUND);

  /**
   * @param controller
   * @param showTab
   */
  public AnalysisTab(ExerciseController controller, final ShowTab showTab) {
    this(controller, showTab, 1, null,
        controller.getUser(), controller.getUserManager().getUserID(),
        -1);
  }

  /**
   * @param controller
   * @param userid
   * @param listid
   * @see AnalysisTab#AnalysisTab(ExerciseController, ShowTab)
   * @see UserContainer#gotClickOnItem
   */
  public AnalysisTab(final ExerciseController controller,
                     final ShowTab showTab,
                     int minRecordings,
                     DivWidget overallBottom,
                     int userid,
                     String userChosenID,
                     int listid) {
    getElement().setId("AnalysisTab");

    getElement().getStyle().setMarginTop(-10, Style.Unit.PX);
    setWidth("100%");
    this.controller = controller;

    Icon playFeedback = getPlayFeedback();

    boolean isTeacherView = overallBottom != null;
    analysisPlot = new AnalysisPlot(controller.getExerciseService(), userid, userChosenID, minRecordings,
        controller.getSoundManager(), playFeedback, listid, isTeacherView);

    Panel timeControls = getTimeControls(playFeedback);
    analysisPlot.setTimeWidgets(timeWidgets);

    add(timeControls);
    add(analysisPlot);

    DivWidget bottom = getBottom(isTeacherView);
    if (isTeacherView) { // are we in student or teacher view
      overallBottom.clear();
      overallBottom.add(bottom); // teacher
    } else {
      add(bottom); // student
    }

    final long then = System.currentTimeMillis();

    analysisServiceAsync.getPerformanceReportForUser(userid, minRecordings, listid, new AsyncCallback<AnalysisReport>() {
      @Override
      public void onFailure(Throwable caught) {

      }

      @Override
      public void onSuccess(AnalysisReport result) {
        useReport(result, then, userChosenID, listid, isTeacherView, showTab, bottom, userid, minRecordings);
      }
    });
  }

  private void useReport(AnalysisReport result, long then, String userChosenID, int listid, boolean isTeacherView,
                         ShowTab showTab, DivWidget bottom, int userid, int minRecordings) {
    long now = System.currentTimeMillis();

    if (now - then > 200) {
      logger.info("took " + (now - then) + " to get report");
    }

    long then2 = now;
    Scheduler.get().scheduleDeferred(() -> analysisPlot.showUserPerformance(result.getUserPerformance(), userChosenID, listid, isTeacherView));

    now = System.currentTimeMillis();
    if (now - then2 > 200) {
      logger.info("took " + (now - then2) + " to show plot");
    }
    long then3 = now;

    Scheduler.get().scheduleDeferred(() ->
        showWordScores(result.getWordScores(), controller, analysisPlot, showTab, bottom, userid, minRecordings, listid,
            result.getPhoneReport()));

//    showWordScores(result.getWordScores(), controller, analysisPlot, showTab, bottom, userid, minRecordings, listid,
//        result.getPhoneReport());

    now = System.currentTimeMillis();
    if (now - then3 > 200) {
      logger.info("took " + (now - then3) + " to show word scores");
    }
    Scheduler.get().scheduleDeferred(() -> analysisPlot.populateExerciseMap(controller.getExerciseService(), userid));
  }

  @NotNull
  private Icon getPlayFeedback() {
    Icon playFeedback = new Icon(IconType.VOLUME_UP);
    playFeedback.addStyleName("leftFiveMargin");
    playFeedback.setVisible(false);
    return playFeedback;
  }

  /**
   * Why want a nine pixel left margin?
   *
   * @return
   */
  @NotNull
  private DivWidget getBottom(boolean isTeacherView) {
    DivWidget bottom = new DivWidget();
    bottom.addStyleName("inlineFlex");
    bottom.getElement().setId("bottom");
    //bottom.addStyleName("floatLeftAndClear");
    if (!isTeacherView) bottom.getElement().getStyle().setMarginLeft(9, Style.Unit.PX);
    return bottom;
  }

  @NotNull
  private Panel getTimeControls(Widget playFeedback) {
    Panel timeControls = new HorizontalPanel();
    timeControls.add(getTimeGroup());
    timeControls.add(getTimeWindowStepper());
    timeControls.add(playFeedback);
    return timeControls;
  }

  /**
   * @return
   * @see #AnalysisTab
   */
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
        analysisPlot.gotPrevClick();
      }
    });
    left.setEnabled(false);
    return left;
  }

  private Button getNextButton() {
    final Button right = new Button();
    right.setIcon(IconType.CARET_RIGHT);
    controller.register(right, "timeWindowAdvance");
    right.addClickHandler(event -> analysisPlot.gotNextClick());
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
    return event -> analysisPlot.setTimeHorizon(month);
  }

  private Button getAllChoice() {
    Button all = getButton(controller, getClickHandler(TIME_HORIZON.ALL), "All");
    all.setActive(true);
    return all;
  }

  private void showWordScores(List<WordScore> wordScores,
                              ExerciseController controller,
                              AnalysisPlot analysisPlot,
                              ShowTab showTab,
                              Panel lowerHalf,
                              int userid,
                              int minRecordings,
                              int listid,
                              PhoneReport phoneReport) {
    {
      // logger.info("showWordScores " + wordScores.size());
      Panel tableWithPager = getWordContainer(wordScores, controller, analysisPlot, showTab,
          new Heading(3, WORDS, SUBTITLE));

      tableWithPager.setWidth(WORD_WIDTH + "px");

      DivWidget wordsContainer = getWordContainerDiv(tableWithPager, "WordsContainer",
          new Heading(3, WORDS, SUBTITLE));
      wordsContainer.addStyleName("cardBorderShadow");
      lowerHalf.add(wordsContainer);
    }

    DivWidget soundsDiv = getSoundsDiv();

    lowerHalf.add(soundsDiv);
    getPhoneReport(phoneReport,
        controller,
        soundsDiv, analysisPlot, showTab);
  }

  private Panel getWordContainer(List<WordScore> wordScores,
                                 ExerciseController controller,
                                 AnalysisPlot analysisPlot,
                                 ShowTab showTab,
                                 Heading wordsTitle) {
    return new WordContainer(controller, analysisPlot, showTab, wordsTitle).getTableWithPager(wordScores);
  }

  private DivWidget getSoundsDiv() {
    DivWidget soundsDiv = new DivWidget();
    soundsDiv.getElement().setId("soundsDiv");
    soundsDiv.getElement().getStyle().setProperty("minHeight", MIN_HEIGHT, Style.Unit.PX);
    soundsDiv.addStyleName("cardBorderShadow");
    soundsDiv.addStyleName("floatRight");
    //  soundsDiv.addStyleName("inlineFlex");
    //soundsDiv.getElement().getStyle().setMargin(10, Style.Unit.PX);
    soundsDiv.addStyleName("leftFiveMargin");
    return soundsDiv;
  }

  private DivWidget getWordContainerDiv(Panel tableWithPager, String containerID, Heading w) {
    DivWidget wordsContainer = new DivWidget();
    wordsContainer.getElement().setId(containerID);
    wordsContainer.addStyleName("floatLeftAndClear");
    wordsContainer.add(w);
    wordsContainer.add(tableWithPager);
    return wordsContainer;
  }

  /**
   * @param controller
   * @param lowerHalf
   * @param analysisPlot
   * @param showTab
   * @see #showWordScores
   */
  private void getPhoneReport(PhoneReport phoneReport,
                              final ExerciseController controller,
                              final Panel lowerHalf,
                              AnalysisPlot analysisPlot,
                              final ShowTab showTab//,
  ) {
    final PhoneExampleContainer exampleContainer =
        new PhoneExampleContainer(controller, analysisPlot, showTab, exampleHeader);

    final PhonePlot phonePlot = new PhonePlot();
    final PhoneContainer phoneContainer = new PhoneContainer(controller, exampleContainer, phonePlot);
    analysisPlot.addListener(phoneContainer);
    showPhoneReport(phoneReport, phoneContainer, lowerHalf, exampleContainer, phonePlot);
  }

  private void showPhoneReport(PhoneReport phoneReport, PhoneContainer phoneContainer, Panel lowerHalf, PhoneExampleContainer exampleContainer, PhonePlot phonePlot) {
    // #1 - phones
    Panel phones = phoneContainer.getTableWithPager(phoneReport);
    lowerHalf.add(getSoundsContainer(phones));

    // #2 - word examples
    lowerHalf.add(getWordExamples(exampleContainer.getTableWithPager()));

    // #3 - phone plot
    phonePlot.addStyleName("topMargin");
    phonePlot.addStyleName("floatLeftAndClear");
    lowerHalf.add(phonePlot);

    phoneContainer.showExamplesForSelectedSound();
  }

  private DivWidget getSoundsContainer(Panel phones) {
    DivWidget sounds = new DivWidget();
    sounds.getElement().setId("SoundsContainer");
    sounds.add(new Heading(3, SOUNDS));
    sounds.addStyleName("floatLeftAndClear");
    sounds.add(phones);
    return sounds;
  }


  private DivWidget getWordExamples(Panel examples) {
    DivWidget wordExamples = getWordContainerDiv(examples, "WordExamples", exampleHeader);
    wordExamples.getElement().getStyle().setMarginLeft(5, Style.Unit.PX);
    wordExamples.getElement().getStyle().setProperty("minHeight", 325, Style.Unit.PX);

    return wordExamples;
  }
}
