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
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.i18n.client.HasDirection;
import com.google.gwt.i18n.shared.WordCountDirectionEstimator;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.*;
import mitll.langtest.client.InitialUI;
import mitll.langtest.client.custom.KeyStorage;
import mitll.langtest.client.custom.TooltipHelper;
import mitll.langtest.client.custom.exercise.CommentBox;
import mitll.langtest.client.dialog.KeyPressHelper;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.ExercisePanelFactory;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.client.qc.QCNPFExercise;
import mitll.langtest.client.scoring.CommentAnnotator;
import mitll.langtest.client.sound.CompressedAudio;
import mitll.langtest.client.sound.SoundFeedback;
import mitll.langtest.shared.exercise.ExerciseAnnotation;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.MutableAnnotationExercise;

import java.util.logging.Logger;

import static mitll.langtest.server.audio.AudioConversion.FILE_MISSING;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 6/26/2014.
 */
public class FlashcardPanel<T extends CommonExercise & MutableAnnotationExercise>  extends HorizontalPanel {
  private final Logger logger = Logger.getLogger("FlashcardPanel");

  private static final int CARD_HEIGHT = 362;//320;

  /**
   * @see #addPlayingHighlight
   */
  private static final String PLAYING_AUDIO_HIGHLIGHT = "playingAudioHighlight";
  private static final String WARN_NO_FLASH = "<font color='red'>Flash is not activated. " +
      "Do you have a flashblocker? Please add this site to its whitelist.</font>";
  private static final String ARROW_KEY_TIP = "<i>Use arrow keys to advance or flip.</i>";

  static final String ON = "On";
  static final String OFF = "Off";

  /**
   * @see #getShowGroup(ControlState)
   */
  private static final String SHOW = "START WITH";
  private static final String ENGLISH = "English";
  private static final String PLAY = "AUDIO";
  private static final String BOTH = "Both";
  private static final String CLICK_TO_FLIP = "Click to flip";
  private static final String SHUFFLE = "Shuffle";

  final T exercise;
  private Timer currentTimer = null;
  private Widget english;
  Widget foreign;

  private final MySoundFeedback soundFeedback;
  final boolean addKeyBinding;
  final ExerciseController controller;
  final ControlState controlState;
  private final Panel mainContainer;
  private Panel leftState;
  private Panel rightColumn;
  private final SoundFeedback.EndListener endListener;
  final String instance;
  final ListInterface exerciseList;
  private final DivWidget prevNextRow;
  boolean showOnlyEnglish = false;

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
                 String instance, ListInterface exerciseList) {
    this.addKeyBinding = addKeyBinding;
    this.exercise = e;
    this.controller = controller;
    this.controlState = controlState;
    this.endListener = endListener;
    this.instance = instance;
    this.exerciseList = exerciseList;

    controlState.setStorage(new KeyStorage(controller));

    this.soundFeedback = soundFeedback;
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
    Scheduler.get().scheduleDeferred(new Command() {
      public void execute() {
        addRecordingAndFeedbackWidgets(getID(), controller, middleVert);
      }
    });
    //  logger.info("After adding recording widgets to " + middleVert.getElement().getExID());
    middleVert.add(getFinalWidgets());

    HTML warnNoFlash = new HTML(WARN_NO_FLASH);
    warnNoFlash.setVisible(false);
    inner.add(warnNoFlash);

    getElement().setId("FlashcardPanel");

    addPrevNextWidgets(prevNextRow);

    addRowBelowPrevNext(lowestRow);

    playRefOrAutoPlay();

    addKeyListener();
  }

  protected void addKeyListener() {
    if (addKeyBinding) {
      addKeyListener(controller, instance);
      // logger.info("FlashcardRecordButton : " + instance + " key is  " + listener.getName());
    }
  }

  private void playRefOrAutoPlay() {
    Scheduler.get().scheduleDeferred(new Command() {
      public void execute() {
        maybePlayRef(controlState);
        doAutoPlay(controlState);
      }
    });
  }

  private void wasHidden() {

    cancelTimer();
    getSoundFeedback().clear();
  }

  private void maybePlayRef(ControlState controlState) {
    //logger.info("maybePlayRef --- ");
    if (controlState.isAudioOn() && isTabVisible()) {
      if (!controlState.isAutoPlay()) {
        // logger.info("audio on, so playing ref");
      playRef();
      } else {
    //    logger.info("maybePlayRef auto advance on, so not playing ref here");
      }
    } else {
      //logger.info("maybePlayRef tab not visible - so no audio.");
    }
  }

  /**
   * TODO: Needed???
   * @return
   * @see BootstrapExercisePanel#playRefAndGoToNext
   * @see #maybePlayRef
   */
  private boolean isTabVisible() {
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
   * @see BootstrapExercisePanel#nextAfterDelay(boolean, String)
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
      public void addIncorrectComment(String commentToPost, String field) {
        addAnnotation(field, ExerciseAnnotation.TYPICAL.INCORRECT, commentToPost);
      }

      @Override
      public void addCorrectComment(String field) {
        addAnnotation(field, ExerciseAnnotation.TYPICAL.CORRECT, "");
      }
    }, exercise,
        true);

    DivWidget left = new DivWidget();
    boolean rtlContent = isRTLContent(exercise.getForeignLanguage());
    left.add(commentBox.getEntry(QCNPFExercise.FOREIGN_LANGUAGE, null, exercise.getAnnotation(QCNPFExercise.FOREIGN_LANGUAGE), true, rtlContent));
    left.addStyleName("floatLeftAndClear");
    left.getElement().setId("leftCommentBoxContainer");
    // left.setWidth("50%");

    DivWidget firstRow = new DivWidget();
    firstRow.add(left);
    firstRow.getElement().setId("firstRow");
    return firstRow;
  }

  private boolean isRTLContent(String content) {
    return WordCountDirectionEstimator.get().estimateDirection(content) == HasDirection.Direction.RTL;
  }

  private void addAnnotation(final String field, final ExerciseAnnotation.TYPICAL status, final String commentToPost) {
    controller.getQCService().addAnnotation(exercise.getID(), field, status.toString(), commentToPost,
        new AsyncCallback<Void>() {
      @Override
      public void onFailure(Throwable caught) {
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
  private Panel getThreePartContent(ControlState controlState,
                                    Panel contentMiddle,
                                    DivWidget belowDiv,
                                    DivWidget lowestRow) {
    Panel horiz = new HorizontalPanel();
    horiz.getElement().setId("left-content-right_container");

    leftState = getLeftState();
    if (leftState != null) horiz.add(leftState);

    int basicNumRows = 2;
    int rows = lowestRow != null ? basicNumRows + 1 : basicNumRows;
    Grid grid = new Grid(rows, 1);
    int row = 0;
    //grid.setWidget(row++, 0, contentRow);
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

    contentMiddle.setHeight(CARD_HEIGHT +
        "px");
    contentMiddle.getElement().setId("Focusable_content");
    contentMiddle.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        boolean englishHidden = isHidden(english);
        //  logger.info("content click " + englishHidden);
        setAutoPlay(false);
        controller.logEvent(contentMiddle, "flashcard_itself", exercise, "flip card to show " + (englishHidden ? " english" : getLanguage()));
        flipCard();
      }
    });
    return contentMiddle;
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
      autoPlay.setActive(false);
      controlState.setAutoPlayOn(false);
      cancelTimer();
    }
  }

  //private boolean isSongPlaying = false;

  /**
   * @param path
   * @param delayMillis
   * @param useCheck
   * @paramx correctPrompt
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
      if (controlState.isAudioOn()) {
        playAudioAndAdvance(path, delayMillis, useCheck);
      }
      else {
        loadNextOnTimer(2000);
      }
    }
  }

  private void playAudioAndAdvance(String path, int delayMillis, boolean useCheck) {
    getSoundFeedback().queueSong(getPath(path), new SoundFeedback.EndListener() {
      @Override
      public void songStarted() {
    //    isSongPlaying = true;
        addPlayingHighlight(foreign);
        if (endListener != null) {
          endListener.songStarted();
        }
      }

      @Override
      public void songEnded() {
      //  isSongPlaying = false;

        if (endListener != null) endListener.songEnded();
        cancelTimer();
        if (isTabVisible()) {
          if (delayMillis > 0) {
            if (useCheck) {
              checkThenLoadNextOnTimer(delayMillis);
            } else {
              loadNextOnTimer(delayMillis);
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
      boolean b = loadNextOnTimer(delayMillis);
    } else {
      //   logger.info("checkThenLoadNextOnTimer NOT AUTO PLAY " + delayMillis);
    }
  }

  /**
   * @param delay
   * @see BootstrapExercisePanel#goToNextAfter
   * @see BootstrapExercisePanel#nextAfterDelay(boolean, String)
   * @see StatsFlashcardFactory.StatsPracticePanel#nextAfterDelay(boolean, String)
   */
  boolean loadNextOnTimer(final int delay) {
    //   logger.info("loadNextOnTimer ----> load next on " + delay);
    if (isTimerNotRunning()) {
      currentTimer = new Timer() {
        @Override
        public void run() {
//          logger.info("loadNextOnTimer ----> at " + System.currentTimeMillis() + "  firing on " + currentTimer);
          loadNext();
        }
      };
      currentTimer.schedule(delay);
      return true;
    } else {
      //    logger.info("loadNextOnTimer ----> ignoring next current timer is running");
      return false;
    }
  }

  private boolean isTimerNotRunning() {
    return (currentTimer == null) || !currentTimer.isRunning();
  }

  void cancelTimer() {
    if (currentTimer != null) currentTimer.cancel();
    removePlayingHighlight(foreign);
  }

  void flipCard() {
    if (clickToFlip.isVisible()) {
      boolean showEnglish = controlState.showEnglish();
      if (!showEnglish || !controlState.showForeign()) {
        toggleVisibility(english);
        toggleVisibility(foreign);
        if (!isHidden(foreign) && controlState.isAudioOn()) {
          playRef();
        }
      }
    }
  }

  private void toggleVisibility(Widget english) {
    Style style = english.getElement().getStyle();
    boolean hidden = style.getVisibility().equals("hidden");
    style.setVisibility(hidden ? Style.Visibility.VISIBLE : Style.Visibility.HIDDEN);
  }

  private boolean isHidden(Widget english) {
    return english.getElement().getStyle().getVisibility().equals("hidden");
  }

  void setMainContentVisible(boolean vis) {
    if (leftState != null) leftState.setVisible(vis);
    mainContainer.setVisible(vis);
    rightColumn.setVisible(vis);
  }

  /**
   * @param controlState
   * @return
   * @see #getThreePartContent(ControlState, Panel, DivWidget, DivWidget)
   */
  private Panel getRightColumn(final ControlState controlState) {
    Panel rightColumn = new VerticalPanel();

    rightColumn.add(getAudioGroup(controlState));
    rightColumn.add(getShowGroup(controlState));

    Widget feedbackGroup = getFeedbackGroup(controlState);
    if (feedbackGroup != null) rightColumn.add(feedbackGroup);

    rightColumn.add(getShuffleButton(controlState));
    rightColumn.add(autoPlay = getAutoPlayButton(controlState));

    Widget child = new HTML(ARROW_KEY_TIP);
    child.getElement().getStyle().setMarginTop(25, Style.Unit.PX);
    rightColumn.add(child);
    rightColumn.addStyleName("leftTenMargin");
    return rightColumn;
  }

  private Button getShuffleButton(final ControlState controlState) {
    final Button shuffle = new Button(SHUFFLE);
    shuffle.setToggle(true);
    shuffle.setIcon(IconType.RANDOM);
    shuffle.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        boolean shuffleOn = !shuffle.isToggled();
        controlState.setSuffleOn(shuffleOn);
        gotShuffleClick(shuffleOn);
      }
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
    autoPlay.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        boolean autoOn = !autoPlay.isToggled();
        //   logger.info("\tgetAutoPlayButton auto play state " + autoOn);
        controlState.setAutoPlayOn(autoOn);
        gotAutoPlay(autoOn);
      }
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
    else cancelTimer();
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
    left.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        left.setEnabled(false);
        exerciseList.loadPrev();
      }
    });
    return left;
  }

  private DivWidget getProgressBarWidget() {
    DivWidget vp = new DivWidget();
    vp.setWidth("78%");
    vp.getElement().getStyle().setMarginLeft(17, Style.Unit.PCT);

    ProgressBar progressBar = new ProgressBar(ProgressBarBase.Style.DEFAULT);
    showAdvance(exerciseList, progressBar);
    progressBar.addStyleName("progressBar");

    Heading child = new Heading(6, Math.max(1, exerciseList.getComplete() + 1) + " of " + exerciseList.getSize());
    child.getElement().getStyle().setMarginLeft(39, Style.Unit.PCT);
    vp.add(child);
    vp.add(progressBar);
    return vp;
  }

  private Button getNextButton() {
    final Button right = new Button();
    right.setIcon(IconType.CARET_RIGHT);
    new TooltipHelper().addTooltip(right, "Right Arrow Key");
    controller.register(right, getID(), "next button");

    right.addStyleName("floatRight");
    right.setSize(ButtonSize.LARGE);
    right.getElement().getStyle().setMarginTop(-30, Style.Unit.PX);
    right.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        right.setEnabled(false);
        gotClickOnNext();
      }
    });
    return right;
  }

  void gotClickOnNext() {
    exerciseList.loadNext();
  }

  private void showAdvance(ListInterface exerciseList, ProgressBar progressBar) {
    int complete = exerciseList.getComplete();
    int i = (complete == -1 ? 1 : complete + 1);
    double percent = 100d * ((double) i / (double) exerciseList.getSize());
    progressBar.setPercent(percent);
  }

  /**
   * @see #getAudioGroup(mitll.langtest.client.flashcard.ControlState)
   * @see #getQuestionContent
   */
  private void playRefLater() {
    Scheduler.get().scheduleDeferred(new Command() {
      public void execute() {
        playRef();
      }
    });
  }

  /**
   * @param controlState
   * @return
   * @see #getRightColumn(mitll.langtest.client.flashcard.ControlState)
   */
  private ControlGroup getAudioGroup(final ControlState controlState) {
    ControlGroup group = new ControlGroup(PLAY);// + " " + controller.getLanguage().toUpperCase());
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
      if (!controlState.isAudioOn()) {
        playRefLater();
      }
      controlState.setAudioOn(true);
    });
    onButton.setActive(controlState.isAudioOn());
    //logger.info("audio on button " + onButton.isActive());
    return onButton;
  }

  private Button getAudioOffButton() {
    Button offButton = new Button(OFF);
    offButton.getElement().setId(PLAY + "_Off");
    offButton.addClickHandler(event -> {
      setAutoPlay(false);
      controlState.setAudioOn(false);
    });
    offButton.setActive(!controlState.isAudioOn());
    controller.register(offButton, getID());
    return offButton;
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

  protected boolean selectShowFL() {
    if (both.isActive()) {
      showOnlyFL.click();
      showOnlyFL.setActive(true);
      both.setActive(false);
      showEnglish.setActive(false);
      return true;
    } else {
      return false;
    }
    //showForeign(controlState);
  }

  private Button getOn(final ControlState controlState) {
    Button onButton = new Button(controller.getLanguage());
    onButton.getElement().setId("Show_On_" + controller.getLanguage());
    controller.register(onButton, getID());

    onButton.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        showForeign(controlState);
      }
    });
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
    String english = isSiteEnglish() ? "Meaning" : ENGLISH;
    Button showEnglish = new Button(english);
    showEnglish.getElement().setId("Show_English");
    controller.register(showEnglish, getID());

    showEnglish.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        if (!controlState.isEnglish()) {
          controlState.setShowState(ControlState.ENGLISH);
          showEnglishOrForeign();
        }
      }
    });
    showEnglish.setActive(controlState.showEnglish() && !controlState.showBoth());
    return showEnglish;
  }

  private Button getBoth(final ControlState controlState) {
    Button both = new Button(BOTH);
    both.getElement().setId("Show_Both_" + controller.getLanguage() + "_and_English");
    controller.register(both, getID());

    both.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        if (!controlState.showBoth()) {
          controlState.setShowState(ControlState.BOTH);
          //System.out.println("getBoth now  " + controlState);
          showEnglishOrForeign();
        }
      }
    });
    both.setActive(controlState.showBoth());
    return both;
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
    FocusPanel englishPhrase = makeEnglishPhrase(englishTranslations);
    englishPhrase.getElement().getStyle().setMarginLeft(-20, Style.Unit.PX);
    englishPhrase.setWidth("100%");
    DivWidget div = new DivWidget();
    div.getElement().setId("QuestionContentFieldContainer");
    div.addStyleName("blockStyle");
    div.add(englishPhrase);

    foreign = getForeignLanguageContent(foreignSentence, e.hasRefAudio());

    if (!usedForeign) {
      div.add(foreign);
    }
    showEnglishOrForeign();

    return div;
  }

  private FocusPanel makeEnglishPhrase(String englishSentence) {
    Heading englishHeading = new Heading(1, englishSentence);
    //  englishHeading.getElement().getStyle().setWidth(500, Style.Unit.PX);
    englishHeading.getElement().setId("EnglishPhrase");
    FocusPanel widgets = new FocusPanel();
    widgets.add(englishHeading);
    english = widgets;
    english.getElement().setId("EnglishPhrase_container");
    return widgets;
  }

  private boolean isSiteEnglish() {
    return getLanguage().equals("English");
  }

  /**
   * @param foreignSentence
   * @param hasRefAudio
   * @return
   * @see #getQuestionContent
   */
  private Widget getForeignLanguageContent(String foreignSentence, boolean hasRefAudio) {
    Heading foreignLanguageContent = new Heading(1, foreignSentence);
    foreignLanguageContent.getElement().setId("ForeignLanguageContent");
    foreignLanguageContent.getElement().getStyle().setTextAlign(Style.TextAlign.CENTER);

    FocusPanel flPhraseContainer = new FocusPanel();   // TODO : remove???
    flPhraseContainer.getElement().setId("FLPhrase_container");

    Panel hp = new HorizontalPanel();
    hp.add(foreignLanguageContent);
    Widget toShow;
    Icon w = new Icon(IconType.VOLUME_UP);
    w.setSize(IconSize.TWO_TIMES);
    toShow = w;

    if (!hasRefAudio) {
      w.getElement().getStyle().setColor("red");
    }

    Panel simple = new SimplePanel();
    simple.add(toShow);
    simple.addStyleName("leftTenMargin");
    hp.add(simple);
    DivWidget centeringRow = getCenteringRow();
    centeringRow.add(hp);
    flPhraseContainer.add(centeringRow);

    addAudioBindings(flPhraseContainer);
    return flPhraseContainer;
  }

  private DivWidget getCenteringRow() {
    DivWidget status = new DivWidget();
    status.getElement().setId("statusRow");
    status.addStyleName("alignCenter");
    status.addStyleName("inlineBlockStyleOnly");
    return status;
  }

  /**
   * @param focusPanel
   * @see #getForeignLanguageContent(String, boolean)
   * @see #getQuestionContent
   */
  private void addAudioBindings(final FocusPanel focusPanel) {
    focusPanel.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        logger.info("addAudioBindings : click on audio playback panel...");
        setAutoPlay(false);
        playRefLater();
        event.getNativeEvent().stopPropagation();
      }
    });
    focusPanel.addMouseOverHandler(new MouseOverHandler() {
      @Override
      public void onMouseOver(MouseOverEvent event) {
        focusPanel.addStyleName("mouseOverHighlight");
      }
    });
    focusPanel.addMouseOutHandler(new MouseOutHandler() {
      @Override
      public void onMouseOut(MouseOutEvent event) {
        focusPanel.removeStyleName("mouseOverHighlight");
      }
    });
    focusPanel.addFocusHandler(new FocusHandler() {
      @Override
      public void onFocus(FocusEvent event) {
        focusPanel.setFocus(false);
      }
    });
  }

  private void showEnglishOrForeign() {
    //System.out.println("show english or foreign " + controlState);
    if (controlState.showBoth()) {
      showBoth();
      showOnlyEnglish = false;
    } else if (controlState.isEnglish()) {
      english.setHeight("100%");
      showEnglish();
      foreign.getElement().getStyle().setVisibility(Style.Visibility.HIDDEN);

      if (clickToFlip != null) {
        clickToFlipContainer.getElement().getStyle().setVisibility(Style.Visibility.VISIBLE);
      }
      showOnlyEnglish = true;
    } else if (controlState.isForeign()) {
      showOnlyEnglish = false;

      foreign.setHeight("100%");
      english.getElement().getStyle().setVisibility(Style.Visibility.HIDDEN);

      showForeign();

      if (clickToFlip != null) {
        clickToFlipContainer.getElement().getStyle().setVisibility(Style.Visibility.VISIBLE);
      }
    }
  }

  private void showBoth() {
    showEnglish();
    showForeign();

    if (clickToFlip != null) {
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
  protected void playRef() {
    String refAudioToPlay = getRefAudioToPlay();
    if (isValid(refAudioToPlay)) {
      playRef(refAudioToPlay);
    }
  }

  /**
   * @return
   * @see #playRef()
   */
  String getRefAudioToPlay() {
    //System.out.println(getElement().getID() + " playing audio for " +exercise.getOldID());
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
    //logger.info("playRef ---------- " + exercise.getID() + " path " + path);
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
//            logger.info("playRef remove playing highlight on ");
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

  private class ClickableSimplePanel extends SimplePanel {
    public HandlerRegistration addClickHandler(ClickHandler handler) {
      return addDomHandler(handler, ClickEvent.getType());
    }
  }

  private void addKeyListener(ExerciseController controller, final String instance) {
    //     logger.info("FlashcardRecordButton.addKeyListener : using " + getElement().getExID() + " for " + instance);
    KeyPressHelper.KeyListener listener = new KeyPressHelper.KeyListener() {
      @Override
      public String getName() {
        return "FlashcardPanel_" + instance;
      }

      @Override
      public void gotPress(NativeEvent ne, boolean isKeyDown) {
        if (isKeyDown) {
          checkKeyDown(ne);
        }
      }

      public String toString() {
        return "KeyListener " + getName();
      }
    };
    controller.addKeyListener(listener);
  }

  private void checkKeyDown(NativeEvent event) {
    if (!shouldIgnoreKeyPress()) {
      int keyCode = event.getKeyCode();
      if (keyCode == KeyCodes.KEY_ALT || keyCode == KeyCodes.KEY_CTRL || keyCode == KeyCodes.KEY_ESCAPE || keyCode == KeyCodes.KEY_WIN_KEY) {
        //logger.info("key code is " + keyCode);
      } else {
        //logger.info("warn - key code is " + keyCode);
        if (keyCode == KeyCodes.KEY_LEFT) {
          exerciseList.loadPrev();
          event.stopPropagation();
        } else if (keyCode == KeyCodes.KEY_RIGHT) {
          if (!exerciseList.isPendingReq()) {
            gotClickOnNext();
          }
          event.stopPropagation();
        } else if (keyCode == KeyCodes.KEY_UP) {
          if (!selectShowFL()) {
            flipCard();
          }
          event.stopPropagation();
        } else if (keyCode == KeyCodes.KEY_DOWN) {
          if (!selectShowFL()) {
            flipCard();
          }
          event.stopPropagation();
        } else {
          // warnNotASpace();
        }
      }

    } else {
      //  logger.info("checkKeyDown ignoring key press... " + listener);
    }
  }

  private boolean shouldIgnoreKeyPress() {
    boolean b = !isAttached() || checkHidden(getElement().getId()) || controller.getUser() == -1;
    //if (b) {
    //logger.info("attached " + isAttached());
    //   logger.info("hidden   " + checkHidden(getElement().getExID()));
    //  logger.info("user     " + controller.getUser());
    // }
    return b;
  }

  private native boolean checkHidden(String id)  /*-{
      return $wnd.jQuery('#' + id).is(":hidden");
  }-*/;
}
