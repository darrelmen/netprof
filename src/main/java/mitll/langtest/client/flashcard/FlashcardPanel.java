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

package mitll.langtest.client.flashcard;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.*;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.base.ProgressBarBase;
import com.github.gwtbootstrap.client.ui.constants.IconSize;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.github.gwtbootstrap.client.ui.constants.ToggleType;
import com.github.gwtbootstrap.client.ui.resources.ButtonSize;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.i18n.client.HasDirection;
import com.google.gwt.i18n.shared.WordCountDirectionEstimator;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.*;
import mitll.langtest.client.custom.KeyStorage;
import mitll.langtest.client.custom.TooltipHelper;
import mitll.langtest.client.custom.exercise.CommentBox;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.ExercisePanelFactory;
import mitll.langtest.client.initial.InitialUI;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.client.qc.QCNPFExercise;
import mitll.langtest.client.scoring.CommentAnnotator;
import mitll.langtest.client.sound.CompressedAudio;
import mitll.langtest.client.sound.SoundFeedback;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.ExerciseAnnotation;
import mitll.langtest.shared.exercise.MutableAnnotationExercise;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Logger;

import static mitll.langtest.server.audio.AudioConversion.FILE_MISSING;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 6/26/2014.
 */
public class FlashcardPanel<T extends CommonExercise & MutableAnnotationExercise> extends DivWidget implements TimerListener {
  private static final String MEANING = "Meaning";
  private final Logger logger = Logger.getLogger("FlashcardPanel");

  static final int PROGRESS_LEFT_MARGIN = 20;
  private static final int ADVANCE_DELAY = 2000;

  private static final int KEY_PRESS_WIDTH = 125;
  private static final String RIGHT_ARROW_KEY = "Right Arrow Key";

  private static final int CARD_HEIGHT = 362;

  /**
   * @see #addPlayingHighlight
   */
  private static final String PLAYING_AUDIO_HIGHLIGHT = "playingAudioHighlight";
  private static final String WARN_NO_FLASH = "<font color='red'>Flash is not activated. " +
      "Do you have a flashblocker? Please add this site to its whitelist.</font>";

  /**
   * @see #addControlsBelowAudio
   * @see #getKeyBindings()
   */
  private static final String ARROW_KEY_TIP = "<i><b>Space</b> to record. <b>Arrow keys</b> to advance or flip. <b>Enter</b> key to play audio.</i>";

  static final String ON = "On";
  static final String OFF = "Off";

  /**
   * @see #getShowGroup(ControlState)
   */
  private static final String SHOW = "SHOW";
  private static final String ENGLISH = "English";
  private static final String PLAY = "AUDIO";
  private static final String BOTH = "Both";
  /**
   *
   */
  private static final String CLICK_TO_FLIP = "Click to flip";
  private static final String SHUFFLE = "Shuffle";

  final T exercise;

  private Widget english;
  Widget foreign;

  private final MySoundFeedback soundFeedback;
  final boolean addKeyBinding;
  final ExerciseController controller;
  final ControlState controlState;
  private Panel mainContainer;
  private Panel leftState;
  private Panel rightColumn;
  private final SoundFeedback.EndListener endListener;
  final String instance;
  protected final ListInterface exerciseList;
  private DivWidget prevNextRow;
  boolean showOnlyEnglish = false;

  final FlashcardTimer timer;

  /**
   * @see #setAutoPlay
   */
  private Button autoPlay;

  /**
   * @param e
   * @param controller
   * @param soundFeedback
   * @param endListener
   * @param instance
   * @param exerciseList
   * @see ExercisePanelFactory#getExercisePanel(mitll.langtest.shared.exercise.Shell)
   */
  FlashcardPanel(final T e,
                 final ExerciseController controller,
                 boolean addKeyBinding,
                 final ControlState controlState,
                 MySoundFeedback soundFeedback,
                 SoundFeedback.EndListener endListener,
                 String instance,
                 ListInterface exerciseList) {
    this.addKeyBinding = addKeyBinding;
    this.exercise = e;
    this.controller = controller;
    this.controlState = controlState;
    this.endListener = endListener;
    this.instance = instance;
    this.exerciseList = exerciseList;
    this.timer = new FlashcardTimer(this);
    this.soundFeedback = soundFeedback;
    controlState.setStorage(new KeyStorage(controller));
  }

  void addWidgets(T e, ExerciseController controller, ControlState controlState) {
    final DivWidget middleVert = new DivWidget();
    middleVert.getElement().setId("middle_vert_container");

    Panel contentMiddle = getCardContent();
    DivWidget firstRow = getFirstRow(controller);
    DivWidget cardPrompt = getCardPrompt(e);
    cardPrompt.insert(firstRow, 0);
    getMiddlePrompt(cardPrompt, contentMiddle, middleVert);
    mainContainer = contentMiddle;

    prevNextRow = new DivWidget();
    prevNextRow.getElement().setId("prevNextRow");
    prevNextRow.addStyleName("topFiveMargin");

    DivWidget lowestRow = new DivWidget();

    Panel threePartContent = getThreePartContent(controlState, contentMiddle, prevNextRow, lowestRow);
    DivWidget inner = new DivWidget();
    inner.getElement().setId("threePartContent_Container");
    add(inner);

    inner.add(threePartContent);

    //  logger.info("Adding recording widgets to " + middleVert.getElement().getExID());
    Scheduler.get().scheduleDeferred((Command) () -> addRecordingAndFeedbackWidgets(getID(), controller, middleVert));
    //  logger.info("After adding recording widgets to " + middleVert.getElement().getExID());
    middleVert.add(getFinalWidgets());

    {
      HTML warnNoFlash = new HTML(WARN_NO_FLASH);
      warnNoFlash.setVisible(false);
      inner.add(warnNoFlash);
    }

    addPrevNextWidgets(prevNextRow);
    addRowBelowPrevNext(lowestRow);

    playRefOrAutoPlay();
  }

  private void playRefOrAutoPlay() {
    Scheduler.get().scheduleDeferred((Command) () -> {
      maybePlayRef(controlState);
      doAutoPlay(controlState);
    });
  }

  @Override
  protected void onDetach() {
    super.onDetach();
    stopPlayback();
  }

  private void wasHidden() {
    cancelAdvanceTimer();
    stopPlayback();
  }

  void stopPlayback() {
    getSoundFeedback().clear();
  }

  /**
   * Worry about whether audio play is turned on at all.
   *
   * @param controlState
   */
  private void maybePlayRef(ControlState controlState) {
    if (isAudioOn(controlState) && isTabVisible()) {
      if (!controlState.isAutoPlay()) {
        // logger.info("maybePlayRef : audio on, so playing ref");
        playRef();
      }
      //else {
      //    logger.info("maybePlayRef auto advance on, so not playing ref here");
      //}
    }
    //else {
    //logger.info("maybePlayRef tab not visible - so no audio.");
    //}
  }

  private boolean isAudioOn(ControlState controlState) {
    return controlState.isAudioOn();
  }

  /**
   * TODO: Needed???
   *
   * @return
   * @see BootstrapExercisePanel#playRefAndGoToNext
   * @see #maybePlayRef
   */
  boolean isTabVisible() {
    return mainContainer.isVisible() && !isHidden(foreign) && isVisible(mainContainer);
  }

  private boolean isVisible(Widget w) {
    while (w.getElement().hasParentElement()) {
      //String display = w.getElement().getStyle().getDisplay();
      //  logger.info("isVisible check " + w.getElement().getId() + " vis " + w.isVisible() + " display " + display);
      if (/*!w.isVisible() || display.equals("none") ||*/ w.getOffsetWidth() == 0) {
        //  logger.info("isVisible check " + w.getElement().getId() + " vis " + w.isVisible() + " offset width " + w.getOffsetWidth());
        return false;
      } else if (w.getElement().getId().equals(InitialUI.ROOT_VERTICAL_CONTAINER)) {
        return true;
      }
      w = w.getParent();
    }
//    logger.info("\tisVisible check " + w.getElement().getId());
    return w.isVisible();
  }

  private int getID() {
    return exercise.getID();
  }

  private CommentBox commentBox;

  /**
   * @see FlashcardTimer#scheduleIn
   */
  void loadNext() {
    if (exerciseList.onLast()) {
      exerciseList.loadFirst();
    } else {
      exerciseList.loadNext();
    }
  }

  /**
   * @param controller
   * @return
   * @see #FlashcardPanel
   */
  DivWidget getFirstRow(ExerciseController controller) {
    commentBox = new CommentBox(exercise.getID(), controller, new CommentAnnotator() {
      @Override
      public void addIncorrectComment(int exid, String field, String commentToPost) {
        addAnnotation(field, ExerciseAnnotation.TYPICAL.INCORRECT, commentToPost);
      }

      @Override
      public void addCorrectComment(int exid, String field) {
        addAnnotation(field, ExerciseAnnotation.TYPICAL.CORRECT, "");
      }
    }, exercise,
        true);

    DivWidget firstRow = new DivWidget();

    addCommentBox(firstRow);

    firstRow.getElement().setId("firstRow");
    return firstRow;
  }

  void addCommentBox(DivWidget firstRow) {
    firstRow.add(getCommentDiv());
  }

  @NotNull
  DivWidget getCommentDiv() {
    DivWidget left = new DivWidget();
    boolean rtlContent = isRTLContent(exercise.getForeignLanguage());
    left.add(commentBox.getEntry(QCNPFExercise.FOREIGN_LANGUAGE, null, exercise.getAnnotation(QCNPFExercise.FOREIGN_LANGUAGE), true, rtlContent));
    left.addStyleName("floatLeftAndClear");
    left.getElement().setId("leftCommentBoxContainer");
    return left;
  }

  private boolean isRTLContent(String content) {
    return getDirection(content) == HasDirection.Direction.RTL;
  }

  private HasDirection.Direction getDirection(String content) {
    return WordCountDirectionEstimator.get().estimateDirection(content);
  }

  private void addAnnotation(final String field, final ExerciseAnnotation.TYPICAL status, final String commentToPost) {
    controller.getQCService().addAnnotation(exercise.getID(), field, status.toString(), commentToPost,
        new AsyncCallback<Void>() {
          @Override
          public void onFailure(Throwable caught) {
            controller.handleNonFatalError("adding annotation in flashcard", caught);
          }

          @Override
          public void onSuccess(Void result) {
/*        System.out.println("\t" + new Date() + " : onSuccess : posted to server " + exercise.getOldID() +
            " field '" + field + "' commentLabel '" + commentToPost + "' is " + status);*/
          }
        });
  }

  private HTML clickToFlip;
  private DivWidget clickToFlipContainer;

  /**
   * @return
   * @see #FlashcardPanel
   */
  private DivWidget getFinalWidgets() {
    clickToFlipContainer = new DivWidget();
    setClickToFlipHeight(clickToFlipContainer);
    clickToFlip = new HTML(CLICK_TO_FLIP);

    clickToFlip.addStyleName("dontSelect");
    clickToFlipContainer.addStyleName("dontSelect");

    clickToFlip.addStyleName("floatRight");
    clickToFlip.getElement().getStyle().setFontWeight(Style.FontWeight.BOLDER);
    clickToFlipContainer.getElement().getStyle().setVisibility(controlState.showBoth() ? Style.Visibility.HIDDEN : Style.Visibility.VISIBLE);

    Icon w = new Icon(IconType.UNDO);
    w.addStyleName("floatRight");
    w.addStyleName("leftFiveMargin");
    setMarginTop(clickToFlip, w);
    clickToFlipContainer.add(w);
    clickToFlipContainer.add(clickToFlip);
    return clickToFlipContainer;
  }

  void setMarginTop(HTML clickToFlip, Widget icon) {
    clickToFlip.getElement().getStyle().setMarginTop(82, Style.Unit.PX);
    icon.getElement().getStyle().setMarginTop(84, Style.Unit.PX);
  }

  void setClickToFlipHeight(DivWidget clickToFlipContainer) {
    clickToFlipContainer.setHeight("100px");
  }

  void addRecordingAndFeedbackWidgets(int exerciseID, ExerciseController controller, Panel contentMiddle) {
    if (controller.getProjectStartupInfo().isHasModel()) {
      logger.warning("addRecordingAndFeedbackWidgets : adding empty recording and feedback widgets " + this.getClass());
    }
  }

  /**
   * Left side, middle content, and right side
   *
   * @param controlState
   * @param contentMiddle
   * @param belowDiv
   * @return
   * @see #FlashcardPanel
   */
  Panel getThreePartContent(ControlState controlState,
                                    Panel contentMiddle,
                                    DivWidget belowDiv,
                                    DivWidget lowestRow) {
    DivWidget horiz = new DivWidget();
    horiz.addStyleName("inlineFlex");
    horiz.getElement().setId("left-content-right_container");

    if ((leftState = getLeftState()) != null) {
      DivWidget leftC = new DivWidget();
      leftC.setWidth(140 + "px");
      leftC.add(leftState);
      horiz.add(leftC);
    }

    // TODO : lose the grid here...

    int basicNumRows = 2;
    int rows = lowestRow != null ? basicNumRows + 1 : basicNumRows;

    Grid grid = new Grid(rows, 1);
    int row = 0;
    grid.setWidget(row++, 0, contentMiddle);
    grid.setWidget(row++, 0, belowDiv);
    if (lowestRow != null) grid.setWidget(row++, 0, lowestRow);

    horiz.add(grid);

    rightColumn = getRightColumn(controlState);
    horiz.add(rightColumn);
    return horiz;
  }

  /**
   * The card prompt is inside the inner widget, which is inside the contentMiddle panel...
   *
   * @param inner
   * @return
   * @paramx e
   * @see #FlashcardPanel
   */
  private void getMiddlePrompt(Widget cardPrompt, Panel contentMiddle, DivWidget inner) {
    inner.add(cardPrompt);

    contentMiddle.add(inner);
    contentMiddle.addStyleName("cardBorderShadow");
    contentMiddle.addStyleName("minWidthFifty");
  }

  /**
   * Card content is focusable, so we can click on it...
   *
   * @return
   * @see #FlashcardPanel
   */
  private Panel getCardContent() {
    final ClickableSimplePanel contentMiddle = new ClickableSimplePanel();

    contentMiddle.setHeight(CARD_HEIGHT + "px");
    contentMiddle.getElement().setId("Focusable_content");

    contentMiddle.addClickHandler(event -> gotCardClick(contentMiddle));
    //  addMouseOverHandler(contentMiddle, event -> gotMouseOver());
    return contentMiddle;
  }

/*  protected void gotMouseOver() {

  }

  private HandlerRegistration addMouseOverHandler(Widget container, MouseOverHandler handler) {
    return container.addDomHandler(handler, MouseOverEvent.getType());
  }*/

  private void gotCardClick(ClickableSimplePanel contentMiddle) {
    boolean englishHidden = isHidden(english);
    //  logger.info("content click " + englishHidden);
    setAutoPlay(false);
    flipCard();

    controller.logEvent(contentMiddle,
        "flashcard_itself", exercise, "flip card to show " + (englishHidden ? " english" : getLanguage()));
  }

  /**
   * @param b
   * @see #addAudioBindings
   * @see #getAudioOffButton
   * @see #getAudioOnButton
   * @see #getCardContent
   * @see #playRefAndGoToNext
   */
  void setAutoPlay(boolean b) {
    if (!b) {
      //   logger.info("setAutoPlay false");
      if (autoPlay != null) autoPlay.setActive(false);
      controlState.setAutoPlayOn(false);
      cancelAdvanceTimer();
    }
  }

  /**
   * @param path
   * @param delayMillis
   * @param useCheck
   * @see #playRefAndGoToNextIfSet
   */
  private void playRefAndGoToNext(String path, final int delayMillis, boolean useCheck) {
    //  logger.info("playRefAndGoToNext " + path + " is song playing " + isSongPlaying + " delay " + delayMillis);
    if (!isValid(path)) {
      if (isTabVisible()) {
        checkThenLoadNextOnTimer(1000);
      } else {
        logger.info("\tplayRefAndGoToNext tab is not visible");
      }
    } else {
      if (isAudioOn()) {
        playAudioAndAdvance(path, delayMillis, useCheck);
      } else {
        timer.scheduleIn(ADVANCE_DELAY);
      }
    }
  }

  private void playAudioAndAdvance(String path, int delayMillis, boolean useCheck) {
    getSoundFeedback().queueSong(getPath(path), new SoundFeedback.EndListener() {
      @Override
      public void songStarted() {
        addPlayingHighlight(foreign);
        if (endListener != null) {
          endListener.songStarted();
        }
      }

      @Override
      public void songEnded() {
        if (endListener != null) endListener.songEnded();
        cancelAdvanceTimer();

        if (isTabVisible()) {
          if (delayMillis > 0) {
            if (useCheck) {
              checkThenLoadNextOnTimer(delayMillis);
            } else {
              timer.scheduleIn(delayMillis);
            }
          } else {
            loadNext();
          }
        }
      }
    });
  }

  private boolean isValid(String path) {
    return path != null && !path.isEmpty() && !path.contains(FILE_MISSING);
  }

  private void checkThenLoadNextOnTimer(int delayMillis) {
    if (controlState.isAutoPlay()) {
      //   logger.info("checkThenLoadNextOnTimer " + delayMillis);
      // boolean b =
      timer.scheduleIn(delayMillis);
    }
  }

  /**
   *
   */
  void flipCard() {
    if (clickToFlip.isVisible()) {
      boolean showEnglish = isShowEnglish();
      if (!showEnglish || !controlState.showForeign()) {
        toggleVisibility(english);
        toggleVisibility(foreign);
        if (!isHidden(foreign) && isAudioOn()) {
          playRef();
        }
      }
    }
  }

  private boolean isShowEnglish() {
    return controlState.showEnglish();
  }

  private void toggleVisibility(Widget english) {
    Style style = english.getElement().getStyle();
    boolean hidden = style.getVisibility().equals("hidden");
    style.setVisibility(hidden ? Style.Visibility.VISIBLE : Style.Visibility.HIDDEN);
  }

  private boolean isHidden(Widget english) {
    return english.getElement().getStyle().getVisibility().equals("hidden");
  }

  /**
   * @param vis
   */
  void setMainContentVisible(boolean vis) {
    if (leftState != null) {
      leftState.setVisible(vis);
      leftState.getParent().setVisible(vis);
    }
    mainContainer.setVisible(vis);
    rightColumn.setVisible(vis);
  }

  /**
   * @param controlState
   * @return
   * @see #getThreePartContent(ControlState, Panel, DivWidget, DivWidget)
   */
  Panel getRightColumn(final ControlState controlState) {
    Panel rightColumn = new DivWidget();
    rightColumn.addStyleName("leftTenMargin");
    rightColumn.add(getAudioGroup(controlState));
    addControlsBelowAudio(controlState, rightColumn);

    rightColumn.add(getKeyBinding());

    return rightColumn;
  }

  @NotNull
  Widget getKeyBinding() {
    Widget child = new HTML(getKeyBindings());
    child.getElement().getStyle().setMarginTop(25, Style.Unit.PX);
    child.setWidth(KEY_PRESS_WIDTH + "px");
    return child;
  }

  String getKeyBindings() {
    return ARROW_KEY_TIP;
  }

  void addControlsBelowAudio(ControlState controlState, Panel rightColumn) {
    rightColumn.add(getShowGroup(controlState));

    Widget feedbackGroup = getFeedbackGroup(controlState);
    if (feedbackGroup != null) rightColumn.add(feedbackGroup);

    rightColumn.add(getShuffleButton(controlState));
    Button child = autoPlay = getAutoPlayButton(controlState);
    DivWidget autoPlayContainer = new DivWidget();
    autoPlayContainer.setWidth("100%");
    autoPlayContainer.add(child);
    rightColumn.add(autoPlayContainer);
  }

  private Button getShuffleButton(final ControlState controlState) {
    final Button shuffle = new Button(SHUFFLE);
    shuffle.setToggle(true);
    shuffle.setIcon(IconType.RANDOM);
    shuffle.addClickHandler(event -> {
      boolean shuffleOn = !shuffle.isToggled();
      controlState.setSuffleOn(shuffleOn);
      gotShuffleClick(shuffleOn);
    });
    shuffle.setActive(controlState.isShuffle());
    return shuffle;
  }

  /**
   * @param controlState
   * @return
   * @see #getRightColumn
   */
  private Button getAutoPlayButton(final ControlState controlState) {
    final Button autoPlay = new Button("Auto");
    autoPlay.addStyleName("topFiveMargin");
    autoPlay.setToggle(true);
    autoPlay.setIcon(IconType.PLAY);

    // logger.info("getAutoPlayButton auto play state " + controlState.isAutoPlay());
    autoPlay.addClickHandler(event -> {
      boolean autoOn = !autoPlay.isToggled();
      //   logger.info("\tgetAutoPlayButton auto play state " + autoOn);
      controlState.setAutoPlayOn(autoOn);
      gotAutoPlay(autoOn);
    });
    autoPlay.setActive(controlState.isAutoPlay());
    // logger.info("auto play active " + autoPlay.isActive());

    return autoPlay;
  }

  private void doAutoPlay(ControlState controlState) {
    if (controlState.isAutoPlay()) {
      //  logger.info("BootstrapExercisePanel auto play so going to next");
      playRefAndGoToNextIfSet();
    }
    //else {
    //  logger.info("BootstrapExercisePanel auto play OFF ");
    //}
  }

  Widget getFeedbackGroup(ControlState controlState) {
    return null;
  }

  /**
   * So if an auto play timer is in progress, stop it first.
   *
   * @param b
   */
  void gotShuffleClick(boolean b) {
    //logger.info("got shuffle click = " + b);
    wasHidden();
    exerciseList.setShuffle(b);
  }

  void gotAutoPlay(boolean b) {
    if (b) playRefAndGoToNextIfSet();
    else cancelAdvanceTimer();
  }

  void cancelAdvanceTimer() {
    timer.cancelTimer();
  }

  void playRefAndGoToNextIfSet() {
    playRefAndGoToNext(getRefAudioToPlay(), BootstrapExercisePanel.DELAY_MILLIS, true);
  }

  Panel getLeftState() {
    return null;
  }

  /**
   * Widgets below the card are a left button, a progress bar, and a right button.
   *
   * @param toAddTo
   * @see #FlashcardPanel
   */
  private void addPrevNextWidgets(Panel toAddTo) {
    toAddTo.add(getPrevButton());
    toAddTo.add(getProgressBarWidget());
    toAddTo.add(getNextButton());
  }

  void setPrevNextVisible(boolean val) {
    prevNextRow.setVisible(val);
  }

  void addRowBelowPrevNext(DivWidget lowestRow) {
  }

  private Button getPrevButton() {
    final Button left = new Button();
    controller.register(left, getID(), "prev button");
    left.setIcon(IconType.CARET_LEFT);
    left.addStyleName("floatLeftAndClear");
    left.setSize(ButtonSize.LARGE);
    new TooltipHelper().addTooltip(left, "Left Arrow Key");
    left.addClickHandler(event -> gotPrevClick(left));
    return left;
  }

  /**
   * @param left
   * @see #getPrevButton
   */
  private void gotPrevClick(Button left) {
    left.setEnabled(false);
    exerciseList.loadPrev();
  }

  private DivWidget getProgressBarWidget() {
    DivWidget vp = new DivWidget();
    vp.setWidth("78%");
    vp.getElement().getStyle().setMarginLeft(PROGRESS_LEFT_MARGIN, Style.Unit.PCT);

    {
      int complete = exerciseList.getComplete();
      int firstValue = Math.max(1, complete + 1);
      Heading child = new Heading(6, firstValue + " of " + exerciseList.getSize());
      child.getElement().getStyle().setMarginLeft(39, Style.Unit.PCT);
      vp.add(child);
    }
    {
      ProgressBar progressBar = new ProgressBar(ProgressBarBase.Style.DEFAULT);
      showAdvance(exerciseList, progressBar);
      progressBar.addStyleName("progressBar");
      vp.add(progressBar);
    }
    return vp;
  }

  private void showAdvance(ListInterface exerciseList, ProgressBar progressBar) {
    int complete = exerciseList.getComplete();
    int i = (complete == -1 ? 1 : complete + 1);
    double percent = 100d * ((double) i / (double) exerciseList.getSize());
    progressBar.setPercent(percent);
  }

  /**
   * @return
   * @see #addPrevNextWidgets
   */
  private Button getNextButton() {
    final Button right = new Button();
    right.setIcon(IconType.CARET_RIGHT);
    new TooltipHelper().addTooltip(right, RIGHT_ARROW_KEY);
    controller.register(right, getID(), "next button");

    right.addStyleName("floatRight");
    right.setSize(ButtonSize.LARGE);
    right.getElement().getStyle().setMarginTop(-30, Style.Unit.PX);
    right.addClickHandler(event -> {
      right.setEnabled(false);
      gotClickOnNext();
    });
    return right;
  }

  void gotClickOnNext() {
    exerciseList.loadNext();
  }

  /**
   * @see #getAudioGroup(mitll.langtest.client.flashcard.ControlState)
   * @see #getQuestionContent
   */
  private void playRefLater() {
    Scheduler.get().scheduleDeferred((Command) this::playRef);
  }

  /**
   * @param controlState
   * @return
   * @see #getRightColumn(mitll.langtest.client.flashcard.ControlState)
   */
  ControlGroup getAudioGroup(final ControlState controlState) {
    ControlGroup group = new ControlGroup(PLAY);
    Icon widget = new Icon(IconType.VOLUME_UP);
    widget.addStyleName("leftFiveMargin");
    group.add(widget);
    ButtonToolbar w = new ButtonToolbar();
    group.add(w);
    ButtonGroup buttonGroup = new ButtonGroup();
    w.add(buttonGroup);

    buttonGroup.setToggle(ToggleType.RADIO);
    buttonGroup.add(getAudioOnButton(controlState));
    buttonGroup.add(getAudioOffButton());

    return group;
  }

  private Button getAudioOnButton(final ControlState controlState) {
    Button onButton = new Button(ON);
    onButton.getElement().setId(PLAY + "_On");
    controller.register(onButton, getID());

    onButton.addClickHandler(event -> {
      setAutoPlay(false);
      if (!isAudioOn(controlState)) {
        playRefLater();
      }
      rememberAudioOnChoice(controlState, true);
    });
    onButton.setActive(isAudioOn(controlState));
    //logger.info("audio on button " + onButton.isActive());
    return onButton;
  }

  private Button getAudioOffButton() {
    Button offButton = new Button(OFF);
    offButton.getElement().setId(PLAY + "_Off");
    offButton.addClickHandler(event -> {
      setAutoPlay(false);
      abortPlayback();
      rememberAudioOnChoice(controlState, false);
    });
    offButton.setActive(!isAudioOn());
    controller.register(offButton, getID());
    return offButton;
  }

  private void rememberAudioOnChoice(ControlState controlState, boolean state) {
    controlState.setAudioOn(state);
  }

  void abortPlayback() {
  }

  boolean isAudioOn() {
    return isAudioOn(controlState);
  }

  /**
   * @param controlState
   * @return
   * @see #getRightColumn(ControlState)
   */
  private ControlGroup getShowGroup(final ControlState controlState) {
    ControlGroup group = new ControlGroup(SHOW);
    ButtonToolbar w = new ButtonToolbar();
    group.add(w);
    ButtonGroup buttonGroup = new ButtonGroup();
    buttonGroup.setVertical(true);
    buttonGroup.setToggle(ToggleType.RADIO);
    w.add(buttonGroup);

    showOnlyFL = getOn(controlState);
    buttonGroup.add(showOnlyFL);
    both = getBoth(controlState);
    buttonGroup.add(both);
    showEnglish = getOff(controlState);
    buttonGroup.add(showEnglish);

    return group;
  }

  private Button showOnlyFL, both, showEnglish;

  /**
   * both is null in polyglot view.
   *
   * @return
   */
  boolean selectShowFL() {
    if (both != null && both.isActive()) {
      showOnlyFL.click();
      showOnlyFL.setActive(true);
      both.setActive(false);
      showEnglish.setActive(false);
      return true;
    } else {
      return false;
    }
  }

  private Button getOn(final ControlState controlState) {
    Button onButton = new Button(controller.getLanguage());
    onButton.getElement().setId("Show_On_" + controller.getLanguage());
    controller.register(onButton, getID());

    onButton.addClickHandler(event -> showForeign(controlState));
    onButton.setActive(controlState.showForeign() && !controlState.showBoth());
    return onButton;
  }

  private void showForeign(ControlState controlState) {
    if (!controlState.isForeign()) {
      controlState.setShowState(ControlState.FOREIGN);
      showEnglishOrForeign();
    }
  }

  private String getLanguage() {
    return controller.getLanguage();
  }

  private Button getOff(final ControlState controlState) {
    Button showEnglish = new Button(isSiteEnglish() ? MEANING : ENGLISH);
    showEnglish.getElement().setId("Show_English");
    controller.register(showEnglish, getID());

    showEnglish.addClickHandler(event -> gotClickShowEnglish(controlState));
    showEnglish.setActive(controlState.showEnglish() && !controlState.showBoth());
    return showEnglish;
  }

  private void gotClickShowEnglish(ControlState controlState) {
    if (!controlState.isEnglish()) {
      controlState.setShowState(ControlState.ENGLISH);
      showEnglishOrForeign();
    }
  }

  private Button getBoth(final ControlState controlState) {
    Button both = new Button(BOTH);
    both.getElement().setId("Show_Both_" + controller.getLanguage() + "_and_English");
    controller.register(both, getID());

    both.addClickHandler(event -> gotClickOnBoth(controlState));
    both.setActive(controlState.showBoth());
    return both;
  }

  private void gotClickOnBoth(ControlState controlState) {
    if (!controlState.showBoth()) {
      controlState.setShowState(ControlState.BOTH);
      showEnglishOrForeign();
    }
  }

  /**
   * Make a row to show the question content (the prompt or stimulus)
   * and the space bar and feedback widgets beneath it.
   *
   * @param e
   * @return
   * @see #FlashcardPanel
   */
  private DivWidget getCardPrompt(T e) {
    DivWidget questionContent = getQuestionContent(e);
    questionContent.getElement().setId("cardPrompt");
    questionContent.addStyleName("cardContent");
    return questionContent;
  }

  /**
   * If there's no english sentence, we use the foreign language phrase
   *
   * @param e
   * @return
   * @see #getCardPrompt
   */
  private DivWidget getQuestionContent(T e) {
    String foreignSentence = e.getForeignLanguage();

    String englishTranslations = e.getEnglish();
    if (isSiteEnglish() && !e.getMeaning().isEmpty()) {
      englishTranslations = e.getMeaning();
    }
    boolean usedForeign = false;
    if (englishTranslations.isEmpty()) {
      englishTranslations = foreignSentence;
      usedForeign = true;
    }

    DivWidget div = new DivWidget();
    div.getElement().setId("QuestionContentFieldContainer");
    div.addStyleName("blockStyle");
    {
      Widget englishPhrase = makeEnglishPhrase(englishTranslations);
      english = englishPhrase;
      moveEnglishForComment(englishPhrase);

      englishPhrase.setWidth("100%");

      div.add(englishPhrase);
    }

    foreign = getForeignLanguageContent(foreignSentence, e.hasRefAudio());

    if (!usedForeign) {
      div.add(foreign);
    }
    showEnglishOrForeign();

    return div;
  }

  void moveEnglishForComment(Widget englishPhrase) {
    englishPhrase.getElement().getStyle().setMarginLeft(-20, Style.Unit.PX);
  }

  private Widget makeEnglishPhrase(String englishSentence) {
    Heading englishHeading = new Heading(1, englishSentence);
    englishHeading.getElement().setId("EnglishPhrase");
    DivWidget widgets = new DivWidget();
    widgets.add(englishHeading);
    widgets.getElement().setId("EnglishPhrase_container");
    return widgets;
  }

  private boolean isSiteEnglish() {
    return getLanguage().equals("English");
  }

  /**
   * TODO : remove table...
   *
   * @param foreignSentence
   * @param hasRefAudio
   * @return
   * @see #getQuestionContent
   */
  private Widget getForeignLanguageContent(String foreignSentence, boolean hasRefAudio) {
    Panel hp = new DivWidget();
    hp.addStyleName("inlineFlex");
    hp.setWidth("100%");
    Widget flContainer = getFLContainer(foreignSentence);
    DivWidget flDiv = new DivWidget();
    flDiv.add(flContainer);
    hp.add(flDiv);

    Widget hasAudioIndicator = getHasAudioIndicator(hasRefAudio);
    if (hasAudioIndicator != null) {
      hp.add(hasAudioIndicator);
    }

    FocusPanel flPhraseContainer = new FocusPanel();   // TODO : remove???
    flPhraseContainer.getElement().setId("FLPhrase_container");
    {
      DivWidget centeringRow = getCenteringRow();
      centeringRow.add(hp);
      flPhraseContainer.add(centeringRow);
    }

    addAudioBindings(flPhraseContainer);
    return flPhraseContainer;
  }

  Widget getHasAudioIndicator(boolean hasRefAudio) {
    Icon audioIndicator = new Icon(IconType.VOLUME_UP);
    audioIndicator.setSize(IconSize.TWO_TIMES);

    if (!hasRefAudio) {
      audioIndicator.getElement().getStyle().setColor("red");
    }

    Panel simple = new SimplePanel();
    simple.add(audioIndicator);
    simple.addStyleName("leftTenMargin");

    return simple;
  }

  @NotNull
  private Widget getFLContainer(String foreignSentence) {
    Heading foreignLanguageContent = new Heading(1, foreignSentence);
    Style style = foreignLanguageContent.getElement().getStyle();
    style.setTextAlign(Style.TextAlign.CENTER);
    style.setProperty("fontFamily", "sans-serif");
    return foreignLanguageContent;
  }

  private DivWidget getCenteringRow() {
    DivWidget status = new DivWidget();
    //status.getElement().setId("statusRow");
    status.addStyleName("alignCenter");
    status.addStyleName("inlineBlockStyleOnly");
    return status;
  }

  /**
   * @param focusPanel
   * @see #getForeignLanguageContent
   * @see #getQuestionContent
   */
  private void addAudioBindings(final FocusPanel focusPanel) {
  //  logger.info("addAudioBindings : click on audio playback panel...");
    focusPanel.addClickHandler(this::onClickOnCard);
    focusPanel.addMouseOverHandler(event -> focusPanel.addStyleName("mouseOverHighlight"));
    focusPanel.addMouseOutHandler(event -> focusPanel.removeStyleName("mouseOverHighlight"));
    focusPanel.addFocusHandler(event -> {
      //  logger.warning("addAudioBindings set focus false on " +focusPanel.getElement().getId());
      focusPanel.setFocus(false);
    });
  }

  /**
   * What about click to flip?
   *
   * @param event
   */
  private void onClickOnCard(ClickEvent event) {
    setAutoPlay(false);
    playRefLater();
    event.getNativeEvent().stopPropagation();
  }

  /**
   * @see #getBoth
   * @see #getQuestionContent
   */
  void showEnglishOrForeign() {
    if (controlState.showBoth()) {
      showBoth();
      showOnlyEnglish = false;
    } else if (controlState.isEnglish()) {
      english.setHeight("100%");
      showEnglish();
      foreign.getElement().getStyle().setVisibility(Style.Visibility.HIDDEN);

      showClickToFlip();
      showOnlyEnglish = true;
    } else if (controlState.isForeign()) {
      showOnlyEnglish = false;

      foreign.setHeight("100%");
      english.getElement().getStyle().setVisibility(Style.Visibility.HIDDEN);

      showForeign();

      showClickToFlip();
    }
  }

  void showBoth() {
    showEnglish();
    showForeign();

    hideClickToFlip();
  }

  private void showClickToFlip() {
    if (clickToFlipContainer != null) {
      //logger.info("show click to flip");
      clickToFlipContainer.getElement().getStyle().setVisibility(Style.Visibility.VISIBLE);
    }
  }

  void hideClickToFlip() {
    if (clickToFlipContainer != null) {
      //  logger.info("hide click to flip");
      clickToFlipContainer.getElement().getStyle().setVisibility(Style.Visibility.HIDDEN);
    }
  }

  void showForeign() {
    foreign.getElement().getStyle().setVisibility(Style.Visibility.VISIBLE);
  }

  void showEnglish() {
    english.getElement().getStyle().setVisibility(Style.Visibility.VISIBLE);
  }

  MySoundFeedback getSoundFeedback() {
    return soundFeedback;
  }

  /**
   * @see #playRefLater()
   * @see #getCardContent()
   * @see #FlashcardPanel
   */
  void playRef() {

    String refAudioToPlay = getRefAudioToPlay();
    if (isValid(refAudioToPlay)) {
      playRef(refAudioToPlay);
    }
  }

  /**
   * @return
   * @see #playRef
   */
  String getRefAudioToPlay() {
    String path = exercise.getRefAudio();
    if (path == null) {
      path = exercise.getSlowAudioRef(); // fall back to slow audio
    }
    return path;
  }

  /**
   * @param path
   * @see #playRef()
   */
  private void playRef(String path) {
    // logger.info("playRef ---------- " + exercise.getID() + " path " + path);
/*
 String exceptionAsString = ExceptionHandlerDialog.getExceptionAsString(new Exception());
      logger.info("playRef : logException stack " + exceptionAsString);
      */

    path = getPath(path);
    final Widget textWidget = foreign;
    getSoundFeedback().queueSong(path,
        new SoundFeedback.EndListener() {
          @Override
          public void songStarted() {
            addPlayingHighlight(textWidget);
            if (endListener != null) endListener.songStarted();
          }

          @Override
          public void songEnded() {
            removePlayingHighlight(textWidget);
            if (endListener != null) endListener.songEnded();
          }
        });
  }

  private void addPlayingHighlight(Widget textWidget) {
    //  logger.info("addPlayingHighlight add playing highlight");
    textWidget.addStyleName(PLAYING_AUDIO_HIGHLIGHT);

  }

  /**
   * @param textWidget
   * @see BootstrapExercisePanel#removePlayingHighlight(Widget)
   */
  void removePlayingHighlight(Widget textWidget) {
    //logger.info("removePlayingHighlight remove playing highlight");
    textWidget.removeStyleName(PLAYING_AUDIO_HIGHLIGHT);
  }

  private String getPath(String path) {
    return CompressedAudio.getPath(path);
  }

  /**
   * @return
   * @see BootstrapExercisePanel#getAnswerWidget
   */
  boolean otherReasonToIgnoreKeyPress() {
    return commentBox.isPopupShowing();
  }

  @Override
  public void timerFired() {
    loadNext();
  }

  @Override
  public void timerCancelled() {
    removePlayingHighlight(foreign);
  }

  public void onSetComplete() {
  }

  public void stopRecording() {

  }

  protected void rememberCurrentExercise() {

  }

  private class ClickableSimplePanel extends SimplePanel {
    HandlerRegistration addClickHandler(ClickHandler handler) {
      return addDomHandler(handler, ClickEvent.getType());
    }
  }
}
