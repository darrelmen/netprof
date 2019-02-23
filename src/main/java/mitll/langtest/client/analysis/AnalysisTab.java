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

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.ListBox;
import com.github.gwtbootstrap.client.ui.*;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.github.gwtbootstrap.client.ui.constants.ToggleType;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.*;
import mitll.langtest.client.banner.NewContentChooser;
import mitll.langtest.client.custom.INavigation;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.flashcard.PolyglotPracticePanel;
import mitll.langtest.client.list.SelectionState;
import mitll.langtest.client.services.AnalysisService;
import mitll.langtest.client.services.AnalysisServiceAsync;
import mitll.langtest.shared.analysis.AnalysisReport;
import mitll.langtest.shared.analysis.AnalysisRequest;
import mitll.langtest.shared.analysis.PhoneSummary;
import mitll.langtest.shared.analysis.UserPerformance;
import mitll.langtest.shared.custom.TimeRange;
import mitll.langtest.shared.exercise.CommonShell;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Logger;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @see NewContentChooser#showProgress
 * @since 10/21/15.
 */
public class AnalysisTab extends DivWidget {
  private static final String TIME_SCALE = "timeScale";
  private static final String TIME_SCALE1 = "Time Scale : ";
  private final Logger logger = Logger.getLogger("AnalysisTab");

  private static final int MIN_HEIGHT = 325;
  private static final int MAX_WIDTH = 1050;

  private static final int MIN_WIDTH = 240;

  private static final String WORD_EXAMPLES = "WordExamples";

  private static final String REPORT_FOR_USER = "getting performance report for user";
  private static final int TIME_WINDOW_WIDTH = 92;

  /**
   * Got be the width to fit on a laptop screen for progress view - otherwise the phone plot
   * will wrap.
   *
   * @see #showWordScores
   */
  private static final String WORDS = "Vocabulary";
  /**
   * @see #exampleHeader
   */
  private static final String WORDS_USING_SOUND = "Words using Sound";
  /**
   * @see #getSoundsContainer
   */
  private static final String SOUNDS = "Sounds";
  private final AnalysisServiceAsync analysisServiceAsync = GWT.create(AnalysisService.class);
  private final int userid;

  private static final long MINUTE = 60 * 1000;
  static final long HOUR = 60 * MINUTE;
  static final long QUARTER = 6 * HOUR;

  static final long TENMIN_DUR = 10 * MINUTE;
  static final long DAY_DUR = 24 * HOUR;
  private static final long WEEK_DUR = 7 * DAY_DUR;
  private static final long MONTH_DUR = 4 * WEEK_DUR;
  static final long YEAR_DUR = 52 * WEEK_DUR;
  private static final long YEARS = 20 * YEAR_DUR;

  enum TIME_HORIZON {
    SESSION("Session", -1),
    DAY("Day", DAY_DUR),
    WEEK("Week", WEEK_DUR),
    MONTH("Month", MONTH_DUR),
    ALL("All", YEARS);

    private final String display;
    private final long offset;

    TIME_HORIZON(String display, long offset) {
      this.display = display;
      this.offset = offset;
    }

    String getDisplay() {
      return display;
    }

    public long getDuration() {
      return offset;
    }
  }

  private AnalysisPlot analysisPlot = null;
  private ExerciseLookup<CommonShell> exerciseLookup = null;
  private final ExerciseController controller;
  private TimeWidgets timeWidgets;
  private final Heading exampleHeader = getHeading(WORDS_USING_SOUND);
  private final int listid;

  private final boolean isPolyglot;
  private Button allChoice, dayChoice, weekChoice, sessionChoice, monthChoice;
  private ListBox timeScale;
  private final INavigation.VIEWS jumpView;
  private int itemColumnWidth = -1;

  /**
   * @param controller
   * @param isPolyglot
   * @param jumpView
   * @see NewContentChooser#showProgress
   * @see PolyglotPracticePanel#getScoreHistory
   */
  public AnalysisTab(ExerciseController controller,
                     boolean isPolyglot,
                     int req,
                     ReqCounter reqCounter,
                     INavigation.VIEWS jumpView) {
    this(controller,
        1,
        null,
        controller.getUser(),
        controller.getUserManager().getUserID(),
        -1,
        isPolyglot,
        req,
        reqCounter,
        jumpView);
  }

  public AnalysisTab(final ExerciseController controller,
                     int minRecordings,
                     DivWidget overallBottom,
                     int userid,
                     String userChosenID,
                     int listid,
                     boolean isPolyglot,
                     int req,
                     ReqCounter reqCounter,
                     INavigation.VIEWS jumpView) {
    this(controller,
        overallBottom, userChosenID, isPolyglot,
        reqCounter, jumpView,
        new AnalysisRequest()
            .setUserid(userid)
            .setMinRecordings(minRecordings)
            .setListid(listid)
            .setReqid(req)
            .setDialogID(new SelectionState().getDialog()),
        MAX_WIDTH, true);
  }

  /**
   * @param controller
   * @param overallBottom non-null for teacher view
   * @param isPolyglot
   * @param maxWidth
   * @param showPlot
   * @see AnalysisTab#AnalysisTab
   * @see UserContainer#changeSelectedUser
   */
  public AnalysisTab(final ExerciseController controller,
                     DivWidget overallBottom,
                     String userChosenID,
                     boolean isPolyglot,
                     ReqCounter reqCounter,
                     INavigation.VIEWS jumpView,
                     AnalysisRequest analysisRequest,
                     int maxWidth,
                     boolean showPlot) {
    this.userid = analysisRequest.getUserid();
    this.listid = analysisRequest.getListid();
    this.isPolyglot = isPolyglot;
    setWidth("100%");
    addStyleName("leftFiveMargin");
    this.controller = controller;
    this.jumpView = jumpView;
    setMinWidth(this, 950);

    boolean isTeacherView = overallBottom != null;

    AnalysisPlot analysisPlot = showPlot ? addAnalysisPlot(controller, isPolyglot, isTeacherView ? 700 : maxWidth, isTeacherView) : null;

    if (!showPlot) {
      exerciseLookup = new CommonShellCache<>(controller.getMessageHelper());
    }

    DivWidget bottom = getBottom(isTeacherView);

    if (isTeacherView) { // are we in student or teacher view
      overallBottom.clear();
      overallBottom.add(bottom); // teacher
    } else {
      //setMinWidth(bottom,950);
      // bottom.getElement().getStyle().setProperty("maxWidth", maxWidth + "px");
      setMaxWidth(bottom, maxWidth);

      add(bottom); // student
    }

    final long then = System.currentTimeMillis();
    //  logger.info("request " + analysisRequest);

    analysisServiceAsync.getPerformanceReportForUser(analysisRequest, new AsyncCallback<AnalysisReport>() {
      @Override
      public void onFailure(Throwable caught) {
        controller.handleNonFatalError(REPORT_FOR_USER, caught);
      }

      @Override
      public void onSuccess(AnalysisReport result) {
        long now = System.currentTimeMillis();
        long total = now - then;
        String took = total > 20 ? "\n\ttook   " + total : "";
        logger.info("getPerformanceReportForUser userid " + userid + " req " + analysisRequest.getReqid() +
            took +
            "\n\tserver " + result.getServerTime() +
            "\n\tclient " + (total - result.getServerTime()));

        //    logger.info("result " + result);

        if (reqCounter.getReq() != result.getReq() + 1) {
          logger.info("getPerformanceReportForUser : skip " + reqCounter.getReq() + " vs " + result.getReq());
        } else {
          useReport(result, then, userChosenID, isTeacherView, bottom, new ReqInfo(analysisRequest), analysisPlot, exerciseLookup);
        }
      }
    });

  }

  @Override
  protected void onUnload() {
    super.onUnload();

    if (wordContainer != null) wordContainer.onUnload();
    if (exampleContainer != null) exampleContainer.onUnload();
  }

  /**
   * @param itemColumnWidth
   * @return
   * @see SessionContainer#getAnalysisTab
   */
  AnalysisTab setItemColumnWidth(int itemColumnWidth) {
    this.itemColumnWidth = itemColumnWidth;
    return this;
  }

  /**
   * @param controller
   * @param isPolyglot
   * @param maxWidth
   * @param isTeacherView
   * @return
   * @see #AnalysisTab(ExerciseController, DivWidget, String, boolean, ReqCounter, INavigation.VIEWS, AnalysisRequest, int, boolean)
   */
  private AnalysisPlot addAnalysisPlot(ExerciseController controller, boolean isPolyglot, int maxWidth, boolean isTeacherView) {
    Widget playFeedback = getPlayFeedback();
    analysisPlot = new AnalysisPlot(controller.getExerciseService(), userid,
        controller.getSoundManager(), playFeedback, controller,
        controller.getMessageHelper(),
        isPolyglot, maxWidth);
    exerciseLookup = analysisPlot;
    {
      Panel timeControls = getTimeControls(playFeedback, isTeacherView);
      analysisPlot.setTimeWidgets(timeWidgets);
      add(timeControls);
    }

    add(analysisPlot);
    return analysisPlot;
  }

  public static class ReqInfo {
    private final int userid;
    private final int minRecordings;
    private final int listid;
    private final int dialogID;
    private final int dialogSessionID;

    ReqInfo(AnalysisRequest req) {
      this(req.getUserid(), req.getMinRecordings(), req.getListid(), req.getDialogID(), req.getDialogSessionID());
    }

    /**
     * @param userid
     * @param minRecordings
     * @param listid
     */
    ReqInfo(int userid, int minRecordings, int listid, int dialogID, int dialogSessionID) {
      this.userid = userid;
      this.minRecordings = minRecordings;
      this.listid = listid;
      this.dialogID = dialogID;
      this.dialogSessionID = dialogSessionID;
    }

    public int getUserid() {
      return userid;
    }

    int getMinRecordings() {
      return minRecordings;
    }

    public int getListid() {
      return listid;
    }

    public int getDialogID() {
      return dialogID;
    }

    public int getDialogSessionID() {
      return dialogSessionID;
    }
  }

  /**
   * @param result
   * @param then
   * @param userChosenID
   * @param isTeacherView
   * @param bottom
   * @param reqInfo
   * @see #AnalysisTab(ExerciseController, DivWidget, String, boolean, ReqCounter, INavigation.VIEWS, AnalysisRequest, int, boolean)
   */
  private void useReport(AnalysisReport result,
                         long then,
                         String userChosenID,
                         boolean isTeacherView,
                         DivWidget bottom,
                         ReqInfo reqInfo,
                         AnalysisPlot<CommonShell> analysisPlot,
                         ExerciseLookup<CommonShell> exerciseLookup) {
    long now = System.currentTimeMillis();

    PhoneSummary phoneSummary = result.getPhoneSummary();
    if (phoneSummary == null) {
      logger.warning("useReport : phone report is null?");
      phoneSummary = new PhoneSummary();
    }

/*    if (now - then > 2) {
      logger.info("useReport " +
              "\n\ttook   " + (now - then) + " to get report" +
              "\n\tfor    " + userid + " " + userChosenID +
              "\n\twords  " + result.getNumScores() +
              "\n\tphones " + phoneSummary.getPhoneToAvgSorted().size()
          //+
          //"\n\tphones word and score " + phoneSummary.getPhoneToBigrams().values().size()
      );
    }*/
    long then2 = now;

    UserPerformance userPerformance = result.getUserPerformance();
    if (analysisPlot != null) {
      Scheduler.get().scheduleDeferred(() -> {
        if (userPerformance != null) { // TODO : not sure how this could be false
          analysisPlot.showUserPerformance(userPerformance, userChosenID, listid, isTeacherView);

          TIME_HORIZON value = TIME_HORIZON.values()[getTimeHorizonFromStorage()];

          //   logger.info("Set time horizon " + value);
          analysisPlot.setTimeHorizon(value);
        }
      });
    }

    now = System.currentTimeMillis();
    if (now - then2 > 200) {
      logger.info("useReport took " + (now - then2) + " to show plot");
    }
    long then3 = now;

    if (userPerformance != null) {
      showWordScores(result.getNumScores(), controller,
          analysisPlot,
          bottom,
          phoneSummary, reqInfo,
          userPerformance.getTimeWindow());
    }

    now = System.currentTimeMillis();
    if (now - then3 > 200) {
      logger.info("useReport took " + (now - then3) + " to show word scores");
    }

 /*   if (analysisPlot != null) {
      Scheduler.get().scheduleDeferred(() -> analysisPlot.populateExerciseMap(controller.getExerciseService(), userid));
    }*/
  }

  @NotNull
  private Widget getPlayFeedback() {
    Icon playFeedback = new Icon(IconType.VOLUME_UP);
    playFeedback.addStyleName("leftFiveMargin");

    DivWidget div = new DivWidget();
    div.addStyleName("topFiveMargin");
    div.add(playFeedback);
    div.setVisible(false);

    return div;
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
    //  bottom.addStyleName("inlineFlex");
    bottom.setWidth("100%");
    bottom.getElement().setId("analysisTabBottom");
    setMaxWidth(bottom, MAX_WIDTH);

    if (!isTeacherView) {
      bottom.getElement().getStyle().setMarginLeft(9, Style.Unit.PX);
    }
    return bottom;
  }

  private void setMaxWidth(DivWidget bottom, int max) {
    bottom.getElement().getStyle().setProperty("maxWidth", max + "px");
  }

  /**
   * @param playFeedback
   * @return
   */
  @NotNull
  private Panel getTimeControls(Widget playFeedback, boolean isTeacherView) {
    Panel timeControls = new HorizontalPanel();
    timeControls.add(isTeacherView ? makeTimeScaleBox() : getTimeGroup());
    timeControls.add(getTimeWindowStepper());
    timeControls.add(playFeedback);
    return timeControls;
  }

  /**
   * @return
   * @see #getTimeControls(Widget, boolean)
   */
  private Panel getTimeWindowStepper() {
    Panel stepper = getStepperContainer();

    Button prevButton = getPrevButton();
    stepper.add(prevButton);

    HTML currentDate = getCurrentTimeWindow();
    stepper.add(currentDate);

    Button nextButton = getNextButton();
    stepper.add(nextButton);

    Heading scoreHeader = new Heading(3);
    scoreHeader.addStyleName("leftFiveMargin");
    scoreHeader.getElement().getStyle().setMarginTop(-5, Style.Unit.PX);
    scoreHeader.getElement().getStyle().setMarginBottom(0, Style.Unit.PX);
    stepper.add(scoreHeader);

    timeWidgets = new TimeWidgets(prevButton, nextButton, currentDate,

        allChoice,
        dayChoice,
        weekChoice,
        monthChoice,
        sessionChoice,

        scoreHeader, timeScale);
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
    currentDate.setWidth(TIME_WINDOW_WIDTH + "px");
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


  /**
   * @return
   * @see
   */
  private Widget getTimeGroup() {
    ButtonToolbar w = new ButtonToolbar();

    ButtonGroup buttonGroup = new ButtonGroup();
    buttonGroup.addStyleName("topMargin");
    buttonGroup.addStyleName("leftTenMargin");
    buttonGroup.setToggle(ToggleType.RADIO);

    w.add(buttonGroup);

    TIME_HORIZON stored = TIME_HORIZON.values()[getTimeHorizonFromStorage()];


    buttonGroup.add(sessionChoice = getButtonChoice(TIME_HORIZON.SESSION, stored));
//    if (isPolyglot) {
//      sessionChoice.setActive(true);
//    }

    buttonGroup.add(dayChoice = getButtonChoice(TIME_HORIZON.DAY, stored));
    buttonGroup.add(weekChoice = getButtonChoice(TIME_HORIZON.WEEK, stored));
    buttonGroup.add(monthChoice = getButtonChoice(TIME_HORIZON.MONTH, stored));
    buttonGroup.add(allChoice = getAllChoice(stored));

    return buttonGroup;
  }

  private ListBox makeTimeScaleBox() {
    timeScale = new ListBox();
    timeScale.setWidth(160 + "px");
    timeScale.addStyleName("leftTenMargin");
    timeScale.getElement().getStyle().setMarginTop(10, Style.Unit.PX);
    timeScale.addChangeHandler(event -> gotTimeScaleChange());

    for (TIME_HORIZON horizon : TIME_HORIZON.values()) {
      timeScale.addItem(TIME_SCALE1 + horizon.getDisplay());
    }

    Scheduler.get().scheduleDeferred(() -> {


      //  int timeHorizonFromStorage = getTimeHorizonFromStorage();
      timeScale.setSelectedIndex(getTimeHorizonFromStorage());

//        TIME_HORIZON value = TIME_HORIZON.values()[timeHorizonFromStorage];

//        logger.info("Set time horizon " + value);
//        analysisPlot.setTimeHorizon(value);


    });

    return timeScale;
  }

  private int getTimeHorizonFromStorage() {
    if (isPolyglot) {
      return 0;
    } else {
      int index = TIME_HORIZON.values().length - 1;
      String timeScale = controller.getStorage().getValue(TIME_SCALE);
      if (timeScale != null && !timeScale.isEmpty()) {
        TIME_HORIZON time_horizon = TIME_HORIZON.valueOf(timeScale);
        // logger.info("found " + time_horizon);
        int i = 0;
        for (TIME_HORIZON time_horizon1 : TIME_HORIZON.values()) {
          if (time_horizon1 == time_horizon) {
            index = i;
            break;
          }
          i++;
        }

      }
      return index;
    }
  }

  private void gotTimeScaleChange() {
    int selectedIndex = timeScale.getSelectedIndex();
    TIME_HORIZON horizon = TIME_HORIZON.values()[selectedIndex];

    controller.getStorage().storeValue(TIME_SCALE, horizon.name());
    analysisPlot.setTimeHorizon(horizon);
  }

  private Button getButtonChoice(TIME_HORIZON week, TIME_HORIZON stored) {
    return getButton(controller, getClickHandler(week), week.getDisplay(), stored == week);
  }

  private Button getAllChoice(TIME_HORIZON stored) {
    Button all = getButtonChoice(TIME_HORIZON.ALL, stored);
    // all.setActive(isActive);
    return all;
  }

  private Button getButton(ExerciseController controller, ClickHandler handler, String week, boolean isActive) {
    Button onButton = new Button(week);
    onButton.setActive(isActive);
    onButton.getElement().setId(week + "Choice");
    controller.register(onButton);
    onButton.addClickHandler(handler);

    return onButton;
  }

  private ClickHandler getClickHandler(final TIME_HORIZON month) {
    return event -> {
      analysisPlot.setTimeHorizon(month);
      controller.getStorage().storeValue(TIME_SCALE, month.name());
    };
  }

  /**
   * @param controller
   * @param analysisPlot
   * @param lowerHalf
   * @param phoneReport
   * @param timeWindow
   * @see #useReport
   */
  private void showWordScores(
      int numScores,
      ExerciseController controller,
      AnalysisPlot<CommonShell> analysisPlot,
      Panel lowerHalf,
      PhoneSummary phoneReport,
      ReqInfo reqInfo,
      TimeRange timeWindow) {
    {
      Heading wordsTitle = getHeading(WORDS);

      Panel tableWithPager = getWordContainer(
          reqInfo,
          numScores,
          controller,
          analysisPlot,
          wordsTitle,
          timeWindow);
      {
        DivWidget wordsContainer =
            getWordContainerDiv(tableWithPager, "WordsContainer", wordsTitle, analysisPlot != null);
        wordsContainer.addStyleName("cardBorderShadow");
        wordsContainer.getElement().getStyle().setOverflow(Style.Overflow.HIDDEN);
        wordsContainer.addStyleName("bottomFiveMargin");
        lowerHalf.add(wordsContainer);
      }
    }

    {
      DivWidget soundsDiv = getSoundsDiv(analysisPlot == null);
      lowerHalf.add(soundsDiv);

      getPhoneReport(phoneReport,
          controller,
          soundsDiv,
          analysisPlot,
          reqInfo);
    }
  }

  @NotNull
  private Heading getHeading(String words) {
    Heading wordsTitle = new Heading(3, words);
    wordsTitle.getElement().getStyle().setMarginTop(0, Style.Unit.PX);
    wordsTitle.getElement().getStyle().setMarginBottom(5, Style.Unit.PX);
    return wordsTitle;
  }

  /**
   * @param controller
   * @param analysisPlot
   * @param wordsTitle
   * @return
   * @paramx itemColumnWidth
   * @see #showWordScores(int, ExerciseController, AnalysisPlot, Panel, PhoneSummary, ReqInfo, TimeRange)
   */
  private Panel getWordContainer(
      ReqInfo reqInfo,
      int numResults,
      ExerciseController controller,
      AnalysisPlot<CommonShell> analysisPlot,
      Heading wordsTitle,

      TimeRange timeRange) {

    //logger.info("getWordContainer item width " + itemColumnWidth);

    WordContainerAsync wordContainer =
        new WordContainerAsync(reqInfo, controller, wordsTitle,
            numResults, analysisServiceAsync, timeRange, jumpView, itemColumnWidth);
    this.wordContainer = wordContainer;
    if (analysisPlot != null) {
      analysisPlot.addListener(wordContainer);
    }
    return wordContainer.getTableWithPager(numResults < 50);
  }

  private DivWidget getSoundsDiv(boolean addLeftRightMargin) {
    DivWidget soundsDiv = new DivWidget();
    soundsDiv.getElement().setId("soundsDiv");
    soundsDiv.getElement().getStyle().setProperty("minHeight", MIN_HEIGHT, Style.Unit.PX);
    soundsDiv.addStyleName("cardBorderShadow");
 /*   if (addLeftRightMargin) {
      soundsDiv.addStyleName("leftFiveMargin");
      soundsDiv.addStyleName("rightFiveMargin");
    }*/
    soundsDiv.addStyleName("inlineFlex");
    return soundsDiv;
  }

  protected DivWidget getWordContainerDiv(Panel tableWithPager, String containerID, Heading heading, boolean addLeftMargin) {
    DivWidget wordsContainer = new DivWidget();
    wordsContainer.getElement().setId(containerID);
    wordsContainer.add(heading);
    wordsContainer.add(tableWithPager);
    return wordsContainer;
  }

  private PhoneExampleContainer exampleContainer;
  private WordContainerAsync wordContainer;

  /**
   * @param controller
   * @param lowerHalf
   * @param analysisPlot
   * @param reqInfo
   * @see #showWordScores
   */
  private void getPhoneReport(PhoneSummary phoneReport,
                              final ExerciseController controller,
                              final Panel lowerHalf,
                              AnalysisPlot analysisPlot,
                              ReqInfo reqInfo) {
    // logger.info("GetPhoneReport " + phoneReport);

    final PhoneExampleContainer exampleContainer = new PhoneExampleContainer(controller, exampleHeader, jumpView);
    this.exampleContainer = exampleContainer;

    final BigramContainer bigramContainer =
        new BigramContainer(controller, exampleContainer, analysisServiceAsync, reqInfo);
    final PhoneContainer phoneContainer =
        new PhoneContainer(controller, bigramContainer, analysisServiceAsync, reqInfo);

    if (analysisPlot != null) {
      analysisPlot.addListener(phoneContainer);
    }

    showPhoneReport(phoneReport, phoneContainer, bigramContainer, lowerHalf, exampleContainer);
  }

  private void showPhoneReport(PhoneSummary phoneReport,
                               PhoneContainer phoneContainer,
                               BigramContainer bigramContainer,
                               Panel lowerHalf,
                               PhoneExampleContainer exampleContainer) {
    // #1 - phones
    lowerHalf.add(getSoundsContainer(phoneContainer.getTableWithPager(phoneReport)));

    lowerHalf.add(getBigramContainer(bigramContainer.getTableWithPager()));

    // #2 - word examples
    lowerHalf.add(getWordExamples(exampleContainer.getTableWithPager()));

    phoneContainer.showExamplesForSelectedSound();
  }

  private DivWidget getSoundsContainer(Panel phones) {
    return getContainer(phones, /*"SoundsContainer",*/ SOUNDS);
  }

  private DivWidget getBigramContainer(Panel phones) {
    return getContainer(phones, /*"BigramContainer",*/ "Context");
  }

  @NotNull
  private DivWidget getContainer(Panel phones, /*String bigramContainer,*/ String context) {
    DivWidget sounds = new DivWidget();
    //  sounds.getElement().setId(bigramContainer);
    sounds.add(getHeading(context));
    sounds.add(phones);
    setMinWidth(sounds, MIN_WIDTH);

    return sounds;
  }

  private void setMinWidth(UIObject horiz1, int min) {
    horiz1.getElement().getStyle().setProperty("minWidth", min + "px"); // so they wrap nicely
  }


  private DivWidget getWordExamples(Panel examples) {
    DivWidget wordExamples = getWordContainerDiv(examples, WORD_EXAMPLES, exampleHeader, true);
    wordExamples.getElement().getStyle().setMarginLeft(5, Style.Unit.PX);
    wordExamples.getElement().getStyle().setProperty("minHeight", MIN_HEIGHT, Style.Unit.PX);
    return wordExamples;
  }
}
