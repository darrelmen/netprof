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
import mitll.langtest.shared.project.ProjectType;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Logger;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 10/21/15.
 */
public class AnalysisTab extends DivWidget {
  public static final String REPORT_FOR_USER = "getting performance report for user";
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
  /**
   * @see #getSoundsContainer
   */
  private static final String SOUNDS = "Sounds";
  private static final String SUBTITLE = "";
  private final AnalysisServiceAsync analysisServiceAsync = GWT.create(AnalysisService.class);
  private final int userid;

  /**
   *
   */
  private Heading scoreHeader;

  private static final long MINUTE = 60 * 1000;
  static final long HOUR = 60 * MINUTE;
  static final long QUARTER = 6 * HOUR;

  private static final long TENMIN_DUR = 10 * MINUTE;
  static final long DAY_DUR = 24 * HOUR;
  private static final long WEEK_DUR = 7 * DAY_DUR;
  private static final long MONTH_DUR = 4 * WEEK_DUR;
  static final long YEAR_DUR = 52 * WEEK_DUR;
  private static final long YEARS = 20 * YEAR_DUR;


  enum TIME_HORIZON {
    SESSION("Session", -1),
    TENMIN("Minute", TENMIN_DUR),
    WEEK("Week", WEEK_DUR),
    MONTH("Month", MONTH_DUR),
    ALL("All", YEARS);

    private String display;
    private long offset;

    TIME_HORIZON(String display, long offset) {
      this.display = display;
      this.offset = offset;
    }

    public String getDisplay() {
      return display;
    }

    public long getDuration() {
      return offset;
    }
  }

  private final AnalysisPlot analysisPlot;
  private final ExerciseController controller;
  private TimeWidgets timeWidgets;
  private final Heading exampleHeader = getHeading(WORDS_USING_SOUND);
  private final int listid;

  private final boolean isPolyglot;

  /**
   * @param controller
   * @param isPolyglot
   * @paramx showTab
   * @see NewContentChooser#showProgress
   */
  public AnalysisTab(ExerciseController controller,
                     boolean isPolyglot,
                     int req,
                     ReqCounter reqCounter) {
    this(controller,
        1,
        null,
        controller.getUser(),
        controller.getUserManager().getUserID(),
        -1,
        isPolyglot,
        req, reqCounter);
  }

  /**
   * @param controller
   * @param userid
   * @param listid
   * @param isPolyglot
   * @param req
   * @see AnalysisTab#AnalysisTab
   * @see UserContainer#gotClickOnItem
   */
  public AnalysisTab(final ExerciseController controller,
                     int minRecordings,
                     DivWidget overallBottom,
                     int userid,
                     String userChosenID,
                     int listid,
                     boolean isPolyglot,
                     int req,
                     ReqCounter reqCounter) {
    this.userid = userid;
    this.listid = listid;
    this.isPolyglot = isPolyglot;
    getElement().getStyle().setMarginTop(-10, Style.Unit.PX);
    setWidth("100%");
    addStyleName("leftFiveMargin");
    this.controller = controller;

    Icon playFeedback = getPlayFeedback();

    boolean isTeacherView = overallBottom != null;
    analysisPlot = new AnalysisPlot(controller.getExerciseService(), userid,
        controller.getSoundManager(), playFeedback, controller,
        controller.getMessageHelper(), isTeacherView,
        controller.getProjectStartupInfo().getProjectType() == ProjectType.POLYGLOT);

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

    analysisServiceAsync.getPerformanceReportForUser(userid, minRecordings, listid, req, new AsyncCallback<AnalysisReport>() {
      @Override
      public void onFailure(Throwable caught) {
        controller.handleNonFatalError(REPORT_FOR_USER, caught);
      }

      @Override
      public void onSuccess(AnalysisReport result) {
        if (reqCounter.getReq() != result.getReq() + 1) {
          logger.info("getPerformanceReportForUser : skip " + reqCounter.getReq() + " vs " + result.getReq());
        } else {
          useReport(result, then, userChosenID, isTeacherView, bottom, new ReqInfo(userid, minRecordings, listid));
        }
      }
    });
  }

  public static class ReqInfo {
    private int userid;
    private int minRecordings;
    private int listid;

    ReqInfo(int userid, int minRecordings, int listid) {
      this.userid = userid;
      this.minRecordings = minRecordings;
      this.listid = listid;
    }

    public int getUserid() {
      return userid;
    }

    public int getMinRecordings() {
      return minRecordings;
    }

    public int getListid() {
      return listid;
    }
  }

  /**
   * @param result
   * @param then
   * @param userChosenID
   * @param isTeacherView
   * @param bottom
   * @param reqInfo
   * @see #AnalysisTab(ExerciseController, int, DivWidget, int, String, int, boolean, int, ReqCounter)
   */
  private void useReport(AnalysisReport result,
                         long then,
                         String userChosenID,
                         boolean isTeacherView,
                         DivWidget bottom,
                         ReqInfo reqInfo) {
    long now = System.currentTimeMillis();

    PhoneReport phoneReport = result.getPhoneReport();
    if (phoneReport == null) {
      logger.warning("useReport : phone report is null?");
      phoneReport = new PhoneReport();
    }

    if (now - then > 2) {
      logger.info("useReport took " + (now - then) + " to get report" +
          "\n\tfor    " + userid + " " + userChosenID +
          "\n\twords  " + result.getNumScores() +
          "\n\tphones " + phoneReport.getPhoneToAvgSorted().size() +
          "\n\tphones word and score " + phoneReport.getPhoneToWordAndScoreSorted().values().size()
      );
    }
    PhoneReport fphoneReport = phoneReport;

    long then2 = now;
    Scheduler.get().scheduleDeferred(() -> analysisPlot.showUserPerformance(result.getUserPerformance(), userChosenID, listid, isTeacherView));

    now = System.currentTimeMillis();
    if (now - then2 > 200) {
      logger.info("useReport took " + (now - then2) + " to show plot");
    }
    long then3 = now;

    showWordScores(result.getNumScores(), controller, analysisPlot, bottom, fphoneReport, reqInfo);

    now = System.currentTimeMillis();
    if (now - then3 > 200) {
      logger.info("useReport took " + (now - then3) + " to show word scores");
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
   * @see #AnalysisTab
   */
  @NotNull
  private DivWidget getBottom(boolean isTeacherView) {
    DivWidget bottom = new DivWidget();
    bottom.addStyleName("inlineFlex");
    bottom.setWidth("100%");
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

    if (isPolyglot) {
      Heading scoreHeader = new Heading(3);
      scoreHeader.addStyleName("leftFiveMargin");
      scoreHeader.getElement().getStyle().setMarginTop(-5, Style.Unit.PX);
      scoreHeader.getElement().getStyle().setMarginBottom(0, Style.Unit.PX);
      stepper.add(this.scoreHeader = scoreHeader);
      //    logger.info("add score header");
    }

    timeWidgets = new TimeWidgets(prevButton, nextButton, currentDate, allChoice,
        weekChoice,
        monthChoice,
        sessionChoice,
        scoreHeader);
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
    currentDate.setWidth(130 + "px");
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
    left.addClickHandler(event -> analysisPlot.gotPrevClick());
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
  // private Button minuteChoice;
  private Button sessionChoice;
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

    if (isPolyglot) {
      buttonGroup.add(sessionChoice = getButtonChoice(TIME_HORIZON.SESSION));
      sessionChoice.setActive(true);
    }

    if (!isPolyglot) {
      buttonGroup.add(weekChoice = getButtonChoice(TIME_HORIZON.WEEK));
      buttonGroup.add(monthChoice = getButtonChoice(TIME_HORIZON.MONTH));
    }
    allChoice = getAllChoice();
    buttonGroup.add(allChoice);

    return buttonGroup;
  }

  private Button getButtonChoice(TIME_HORIZON week) {
    return getButton(controller, getClickHandler(week), week.getDisplay());
  }

  private Button getAllChoice() {
    Button all = getButtonChoice(TIME_HORIZON.ALL);
    all.setActive(!isPolyglot);
    return all;
  }

  private Button getButton(ExerciseController controller, ClickHandler handler, String week) {
    Button onButton = new Button(week);
    onButton.getElement().setId(week + "Choice");
    controller.register(onButton);
    onButton.addClickHandler(handler);

    return onButton;
  }

  private ClickHandler getClickHandler(final TIME_HORIZON month) {
    return event -> analysisPlot.setTimeHorizon(month);
  }

  /**
   * @param controller
   * @param analysisPlot
   * @param lowerHalf
   * @param phoneReport
   * @paramx showTab
   * @see #useReport
   */
  private void showWordScores(
      int numScores,
      ExerciseController controller,
      AnalysisPlot analysisPlot,
      Panel lowerHalf,
      PhoneReport phoneReport,
      ReqInfo reqInfo) {
    {
      Heading wordsTitle = getHeading(WORDS);

      Panel tableWithPager = getWordContainer(
          reqInfo,
          numScores,
          controller, analysisPlot,
          wordsTitle);

      tableWithPager.setWidth(WORD_WIDTH + "px");

      {
        DivWidget wordsContainer = getWordContainerDiv(tableWithPager, "WordsContainer", wordsTitle);
        wordsContainer.addStyleName("cardBorderShadow");
        lowerHalf.add(wordsContainer);
      }
    }

    {
      DivWidget soundsDiv = getSoundsDiv();

      lowerHalf.add(soundsDiv);
      getPhoneReport(phoneReport,
          controller,
          soundsDiv, analysisPlot);
    }
  }

  @NotNull
  private Heading getHeading(String words) {
    Heading wordsTitle = new Heading(3, words);
    wordsTitle.getElement().getStyle().setMarginTop(0, Style.Unit.PX);
    return wordsTitle;
  }

  /**
   * @param controller
   * @param analysisPlot
   * @param wordsTitle
   * @return
   * @paramx showTab
   * @paramx wordScores
   */
  private Panel getWordContainer(
                                 ReqInfo reqInfo,
                                 int numResults,
                                 ExerciseController controller,
                                 AnalysisPlot analysisPlot,

                                 Heading wordsTitle) {
    WordContainerAsync wordContainer = new WordContainerAsync(reqInfo, controller, analysisPlot, wordsTitle,
        numResults, analysisServiceAsync);
    return wordContainer.getTableWithPager();
  }

  private DivWidget getSoundsDiv() {
    DivWidget soundsDiv = new DivWidget();
    soundsDiv.getElement().setId("soundsDiv");
    soundsDiv.getElement().getStyle().setProperty("minHeight", MIN_HEIGHT, Style.Unit.PX);
    soundsDiv.addStyleName("cardBorderShadow");
    soundsDiv.addStyleName("leftFiveMargin");
    soundsDiv.addStyleName("inlineFlex");
    return soundsDiv;
  }

  private DivWidget getWordContainerDiv(Panel tableWithPager, String containerID, Heading heading) {
    DivWidget wordsContainer = new DivWidget();
    wordsContainer.getElement().setId(containerID);
    wordsContainer.add(heading);
    wordsContainer.add(tableWithPager);
    return wordsContainer;
  }

  /**
   * @param controller
   * @param lowerHalf
   * @param analysisPlot
   * @see #showWordScores
   */
  private void getPhoneReport(PhoneReport phoneReport,
                              final ExerciseController controller,
                              final Panel lowerHalf,
                              AnalysisPlot analysisPlot) {
    final PhoneExampleContainer exampleContainer = new PhoneExampleContainer(controller, analysisPlot, exampleHeader);

    final PhonePlot phonePlot = new PhonePlot();
    final PhoneContainer phoneContainer = new PhoneContainer(controller, exampleContainer, phonePlot, analysisServiceAsync, listid, userid);

    analysisPlot.addListener(phoneContainer);

    showPhoneReport(phoneReport, phoneContainer, lowerHalf, exampleContainer, phonePlot);
  }

  private void showPhoneReport(PhoneReport phoneReport,
                               PhoneContainer phoneContainer, Panel lowerHalf,
                               PhoneExampleContainer exampleContainer,
                               PhonePlot phonePlot) {
    // #1 - phones
    // Panel phones = phoneContainer.getTableWithPager(phoneReport);
    lowerHalf.add(getSoundsContainer(phoneContainer.getTableWithPager(phoneReport)));

    // #2 - word examples
    lowerHalf.add(getWordExamples(exampleContainer.getTableWithPager()));

    if (!isPolyglot) {
      // #3 - phone plot
      phonePlot.addStyleName("topMargin");
      // phonePlot.addStyleName("floatLeftAndClear");
      lowerHalf.add(phonePlot);
      logger.info("adding phone plot");
    }

    phoneContainer.showExamplesForSelectedSound();
  }

  private DivWidget getSoundsContainer(Panel phones) {
    DivWidget sounds = new DivWidget();
    sounds.getElement().setId("SoundsContainer");
    sounds.add(getHeading(SOUNDS));
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
