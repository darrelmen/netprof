/*
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2019. Other request for this document shall be referred
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
 * © 2015-2019 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 */

package mitll.langtest.client.dialog;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.Icon;
import com.github.gwtbootstrap.client.ui.base.ComplexWidget;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.github.gwtbootstrap.client.ui.resources.ButtonSize;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.banner.IBanner;
import mitll.langtest.client.banner.NewContentChooser;
import mitll.langtest.client.custom.ContentView;
import mitll.langtest.client.custom.INavigation;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.scoring.ITurnPanel;
import mitll.langtest.client.scoring.RefAudioGetter;
import mitll.langtest.client.scoring.TurnPanel;
import mitll.langtest.client.sound.HeadlessPlayAudio;
import mitll.langtest.client.sound.PlayAudioPanel;
import mitll.langtest.client.sound.PlayListener;
import mitll.langtest.shared.dialog.DialogType;
import mitll.langtest.shared.dialog.IDialog;
import mitll.langtest.shared.exercise.ClientExercise;
import mitll.langtest.shared.scoring.AlignmentOutput;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.logging.Logger;

/**
 * Created by go22670 on 4/5/17.
 */
public class ListenViewHelper<T extends ITurnPanel>
    extends TurnViewHelper<T>
    implements ContentView, PlayListener, IListenView, ITurnContainer<T> {
  private final Logger logger = Logger.getLogger("ListenViewHelper");

  private static final String VALUE = "value";
  private static final String SLIDER_MAX = "100";
  private static final String MAX = "max";
  private static final String MIN = "min";


  private static final String SLIDER_MIN = "0";
  private static final String TYPE = "type";
  private static final String RANGE = "range";
  private static final String INPUT = "input";

  final Map<Integer, AlignmentOutput> alignments = new HashMap<>();

  private final List<T> promptTurns = new ArrayList<>();
  final List<T> leftTurnPanels = new ArrayList<>();
  private final List<T> middleTurnPanels = new ArrayList<>();
  private final List<T> rightTurnPanels = new ArrayList<>();

  private ComplexWidget slider;
  /**
   * @see #setPlayButtonToPlay()
   * @see #setPlayButtonToPause()
   */
  private Button playButton;
  private Button playYourselfButton;

  /**
   * @see #isDoRehearse
   * @see #gotHearYourself
   */
  private boolean doRehearse = true;

  private static final boolean DEBUG = false;
  private static final boolean DEBUG_BLUR = false;
  private static final boolean DEBUG_DETAIL = false;
  private static final boolean DEBUG_PLAY = false;
  private static final boolean DEBUG_NEXT = false;

  /**
   * @param controller
   * @param thisView
   * @see NewContentChooser#NewContentChooser(ExerciseController, IBanner)
   */
  public ListenViewHelper(ExerciseController controller, INavigation.VIEWS thisView) {
    super(controller, thisView);
  }

  public boolean isInterpreter() {
    return dialog.getKind() == DialogType.INTERPRETER;
  }

  @Override
  protected void clearTurnLists() {
    super.clearTurnLists();
    clearColumnTurnLists();
    setCurrentTurn(null);
  }

  void clearColumnTurnLists() {
    promptTurns.clear();
    leftTurnPanels.clear();
    middleTurnPanels.clear();
    rightTurnPanels.clear();
  }

  @NotNull
  public DivWidget getTurns(IDialog dialog) {
    DivWidget turns = super.getTurns(dialog);
    markFirstTurn();
    return turns;
  }

  @Override
  protected void removeFromAllPanels(T toRemove) {
    leftTurnPanels.remove(toRemove);
    rightTurnPanels.remove(toRemove);
    promptTurns.remove(toRemove);
    middleTurnPanels.remove(toRemove);
  }

  protected void showDialog(int dialogID, IDialog dialog, Panel child) {
    super.showDialog(dialogID, dialog, child);
    if (dialog != null) {
      List<RefAudioGetter> refAudioGetters = new ArrayList<>(allTurns);
      if (DEBUG) logger.info("showDialogGetRef : Get ref audio for " + refAudioGetters.size());
      getRefAudio(refAudioGetters.iterator());
    }
  }

  private void getRefAudio(final Iterator<RefAudioGetter> iterator) {
    if (iterator.hasNext()) {
      // RefAudioGetter next = iterator.next();
      //logger.info("getRefAudio asking next panel...");

//      if (false) {
//        logger.info("getRefAudio : skip stale req for panel...");
//      } else {
      iterator.next().getRefAudio(() -> {
        if (iterator.hasNext()) {
          //     logger.info("\tgetRefAudio panel complete...");
          //   final int reqid = next.getReq();
          if (true) {
            if (Scheduler.get() != null) {
              Scheduler.get().scheduleDeferred(() -> {
                // if (true) {
                getRefAudio(iterator);
                // }
                //else {
//              /
                // }
              });
            }
          }
        } else {
          //   logger.info("\tgetRefAudio all panels complete...");
        }
      });
    }
    // }
  }

  /**
   * @param isRight
   * @param clientExercise
   * @param prevColumn
   * @param index
   * @return
   * @see #addTurn
   */
  @NotNull
  @Override
  T getTurnPanel(ClientExercise clientExercise, COLUMNS columns, COLUMNS prevColumn, int index) {
//    T turn = reallyGetTurnPanel(clientExercise, columns, prevColumn, index);
//    turn.addWidgets(true, false, PhonesChoices.HIDE, EnglishDisplayChoices.SHOW);
    T turn = super.getTurnPanel(clientExercise, columns, prevColumn, index);
    if (!turn.addPlayListener(this)) logger.warning("didn't add the play listener...");
    turn.addClickHandler(event -> {
      //    logger.info("got event " + event.getClass());
      gotTurnClick(turn);
    });
    return turn;
  }

  /**
   * @param clientExercise
   * @param columns
   * @param prevColumn
   * @param index
   * @return
   * @see #getTurnPanel
   */
  @NotNull
  @Override
  protected T reallyGetTurnPanel(ClientExercise clientExercise, COLUMNS columns, COLUMNS prevColumn, int index) {
    boolean rightJustify = columns == ITurnContainer.COLUMNS.MIDDLE &&
        getView() == INavigation.VIEWS.LISTEN && clientExercise.hasEnglishAttr();

    T widgets = makeTurnPanel(clientExercise, columns, prevColumn, rightJustify, index);
//    widgets.asWidget().getElement().getStyle().setProperty("tabindex", "" + index);
//
//    logger.info("reallyGetTurnPanel set tab index to " +index);

    return widgets;
  }

  /**
   * TODO how to do this without casting????
   *
   * @param clientExercise
   * @param columns
   * @param prevColumn
   * @param rightJustify
   * @param index
   * @return
   */
  @NotNull
  protected T makeTurnPanel(ClientExercise clientExercise, COLUMNS columns, COLUMNS prevColumn, boolean rightJustify, int index) {
    T t = (T) new TurnPanel(
        clientExercise,
        controller,
        null,
        alignments,
        this,
        columns,
        prevColumn, rightJustify);

    return t;
  }

  @Override
  protected void addTurnForEachExercise(DivWidget rowOne, String left, String right, List<ClientExercise> exercises) {
    super.addTurnForEachExercise(rowOne, left, right, exercises);
    populateColumnTurnLists();
  }

  private void populateColumnTurnLists() {
    allTurns.forEach(turn -> addToColumnPanelLists(
        (turn.isLeft() ? COLUMNS.LEFT : turn.isRight() ? COLUMNS.RIGHT : COLUMNS.MIDDLE), turn));
  }

  private void addToColumnPanelLists(COLUMNS columns, T turn) {
    if (columns == COLUMNS.RIGHT) {
      rightTurnPanels.add(turn);
      promptTurns.add(turn);
    } else if (columns == COLUMNS.LEFT) {
      leftTurnPanels.add(turn);
      promptTurns.add(turn);
    } else if (columns == COLUMNS.MIDDLE) {
      middleTurnPanels.add(turn);
    }
  }

  /**
   * @param turn
   */
  void gotTurnClick(T turn) {
    if (DEBUG) logger.info("\n\n ------- gotTurnClick " + turn.getExID());

    setPlayButtonToPlay();

    removeMarkCurrent();
    setCurrentTurn(turn);
    playCurrentTurn();
  }

  /**
   * TODO add playback rate
   * <p>
   * Prev, play, next buttons
   *
   * @return
   */
  @NotNull
  @Override
  DivWidget getControls() {
    DivWidget rowOne = new DivWidget();
    rowOne.getElement().setId("controls");
    alignCenter(rowOne);

    {
      Button widgets = new Button("", IconType.BACKWARD, event -> gotBackward());
      widgets.addStyleName("leftFiveMargin");
      rowOne.add(widgets);
    }

    {
      Button widgets1 = new Button("", IconType.PLAY, event -> gotClickOnPlay());
      widgets1.setType(ButtonType.SUCCESS);
      widgets1.setSize(ButtonSize.LARGE);
      widgets1.addStyleName("leftFiveMargin");
      widgets1.getElement().setId("playButton");
      rowOne.add(widgets1);
      playButton = widgets1;
    }


    {
      Button widgets1 = new Button("Yourself", IconType.PLAY, event -> gotPlayYourself());
      widgets1.setActive(false);
      widgets1.setEnabled(false);
      widgets1.setType(ButtonType.SUCCESS);
      widgets1.getElement().setId("playYourselfButton");

      widgets1.setSize(ButtonSize.LARGE);
      widgets1.addStyleName("leftFiveMargin");
      if (addPlayYourself()) {
        rowOne.add(widgets1);
      }
      playYourselfButton = widgets1;
      playYourselfButton.setVisible(getView() != INavigation.VIEWS.LISTEN);
    }

    {
      Button widgets2 = new Button("", IconType.FORWARD, event -> gotForward(null));
      widgets2.addStyleName("leftFiveMargin");
      rowOne.add(widgets2);
    }

    {
      Icon w = new Icon(IconType.VOLUME_UP);
      w.addStyleName("leftTenMargin");
      rowOne.add(w);
      rowOne.add(slider = getSlider());
    }

    return rowOne;
  }

  private void alignCenter(DivWidget rowOne) {
    rowOne.getElement().getStyle().setTextAlign(Style.TextAlign.CENTER);
  }

  /**
   * Gotta make one. Nothing in gwt bootstrap...
   *
   * @return
   */
  @NotNull
  private ComplexWidget getSlider() {
    ComplexWidget input = new ComplexWidget(INPUT);

    input.getElement().setPropertyString(TYPE, RANGE);
    input.getElement().setPropertyString(MIN, SLIDER_MIN);
    input.getElement().setPropertyString(MAX, SLIDER_MAX);
    input.getElement().setPropertyString(VALUE, SLIDER_MAX);
    input.addDomHandler(event -> gotSliderChange(), ChangeEvent.getType());
    input.setWidth("150px");
    input.addStyleName("leftFiveMargin");
    return input;
  }

  private void gotSliderChange() {
    controller.getSoundManager().setVolume(getVolume());
  }

  @Override
  public int getVolume() {
    return slider.getElement().getPropertyInt(VALUE);
  }

  @Override
  public void gotBlur(T widgets) {
    boolean last = isLast(widgets);
    if (last) {
      if (widgets.isDeleting()) {
        if (DEBUG_BLUR) logger.info("gotBlur ignore blur of " + widgets.getExID());

      } else {
//        String exceptionAsString = ExceptionHandlerDialog.getExceptionAsString(new Exception("gotBlur"));
//        logger.info("logException stack " + exceptionAsString);

        if (DEBUG_BLUR) {
          logger.info("gotBlur got blur of '" + widgets.getText() +
              " : " + widgets.isDeleting());
        }

        moveFocusToNext();
      }
    }
  }

  @Override
  public int getDialogSessionID() {
    return -1;
  }

  void gotBackward() {
    setPlayButtonToPlay();

    List<T> seq = getAllTurns();

    int i = getIndexOfCurrentTurn();
    int i1 = i - 1;

    boolean isPlaying = getCurrentTurn().doPause();

    clearHighlightAndRemoveMark();

    if (!makePrevVisible()) {
      //makeVisible(currentTurn);
      //  logger.info("gotBackward make current turn visible " + currentTurn.getExID() + " at " + i);
      makeVisible(seq.get(seq.size() - 1));
      // turnContainer.getElement().setScrollTop(300);
      //   turnContainer.getParent().getElement().setScrollTop(0);
    }

    if (i1 < 0) {
      setCurrentTurn(seq.get(seq.size() - 1));
    } else {
      setCurrentTurn(seq.get(i1));
    }
    markCurrent();


    if (isPlaying) {
      if (getView() == INavigation.VIEWS.LISTEN) {
        playCurrentTurn();
      }
    }
  }

  @Override
  public void gotForward(T editorTurn) {
    boolean isPlaying = getCurrentTurn().doPause();
    super.gotForward(editorTurn);
    afterChangeTurns(isPlaying);
  }

  /**
   * If the focus changed, it must be from a user interaction?
   *
   * @param newTurn
   * @see EditorTurn#gotFocus
   */
  public void setCurrentTurnTo(T newTurn) {
    if (DEBUG_NEXT) logger.info("setCurrentTurnTo - " + newTurn.getExID());
    //  setGotTurnClick(true);
    setPlayButtonToPlay();

    boolean isPlaying = getCurrentTurn().doPause();
    if (DEBUG_NEXT) logger.info("setCurrentTurnTo current turn paused " + isPlaying);

    /*int i = */
//    beforeChangeTurns();
//    setCurrentTurn(newTurn);
//    markCurrent();
    //   logger.info("setCurrentTurnTo ex #" + currentTurn.getExID());
    // afterChangeTurns(isPlaying);

    super.setCurrentTurnTo(newTurn);
  }

  /**
   * @return
   * @see #setCurrentTurnTo
   */
  @Override
  protected int beforeChangeTurns() {
    setPlayButtonToPlay();

    int i = getIndexOfCurrentTurn();

    if (DEBUG_NEXT) {
      logger.info("beforeChangeTurns current at #" + i + " : " + blurb());
    }

    clearHighlightAndRemoveMark();

//    if (!makeNextVisible()) {
//      if (DEBUG_NEXT) {
//        logger.info("beforeChangeTurns : make header visible");
//      }
//      if (dialogHeader == null) {
//        makeVisible(speakerRow);
//        //  logger.info("no dialog header?");
//      } else {
//        makeVisible(dialogHeader);  // make the top header visible...
//      }
//    }

    return super.beforeChangeTurns();
//    return i;
  }

  private void afterChangeTurns(boolean isPlaying) {
    if (DEBUG) logger.info("afterChangeTurns isPlaying " + isPlaying);
    markCurrent();
    if (isPlaying) playCurrentTurn();
  }

  /**
   * @see ITurnContainer#gotForward(EditorTurn)
   * @see #gotBackward()
   */
  private void clearHighlightAndRemoveMark() {
    if (DEBUG) logger.info("clearHighlight on " + getExID());

    getCurrentTurn().resetAudio();
    getCurrentTurn().clearHighlight();
    removeMarkCurrent();
  }

  /**
   * @see #getControls
   */
  void gotClickOnPlay() {
    if (DEBUG) logger.info("gotClickOnPlay got click on play ");
    firstStepsWhenPlay();

    playCurrentTurn();
  }

  /**
   * Toggle the button state.
   * Maybe go back to first turn
   */
  void firstStepsWhenPlay() {
    gotRehearse();
    togglePlayState();
    setHearYourself(false);
    ifOnLastJumpBackToFirst();
  }

  private void gotPlayYourself() {
    gotHearYourself();

    togglePlayState();

    if (DEBUG) logger.info("gotClickOnPlay got click on play yourself");

    ifOnLastJumpBackToFirst();

    playCurrentTurn();
  }

  /**
   * what state is indicated by the play button - playing or paused?
   * if paused, don't continue when audio ends or says it stopped.
   */
  void togglePlayState() {
    if (sessionGoingNow) {
      setPlayButtonToPlay();
    } else {
      setPlayButtonToPause();
    }
  }


  public void setSessionGoingNow(boolean val) {
    sessionGoingNow = val;
  }

  /**
   * @return
   * @see #gotClickOnPlay
   * @see #setNextTurnForSide
   * @see #currentTurnPlayEnded()
   * @see RehearseViewHelper#startRecordingTurn
   */
  boolean isDoRehearse() {
    return doRehearse;
  }

  /**
   * @param enabled
   * @see #gotClickOnPlay
   */
  void setHearYourself(boolean enabled) {
    playYourselfButton.setEnabled(enabled);
  }

  protected boolean addPlayYourself() {
    return true;
  }

  protected void gotRehearse() {
    doRehearse = true;
    if (DEBUG) logger.info("gotRehearse : doRehearse = " + doRehearse);
  }

  /**
   * @see #gotPlayYourself()
   */
  protected void gotHearYourself() {
    doRehearse = false;
    if (DEBUG) logger.info("gotHearYourself : doRehearse = " + doRehearse);
  }

  /**
   * @see #gotClickOnPlay()
   */
  void ifOnLastJumpBackToFirst() {
    T currentTurn = getCurrentTurn();


    boolean last = isLast(currentTurn);
    if (last) logger.info("ifOnLastJumpBackToFirst : OK, on last - let's consider going back to start");

    if (last && currentTurn != null
    //    && !currentTurn.hasCurrentMark()
    ) {
      logger.info("ifOnLastJumpBackToFirst mark first turn\n\n\n");
      removeMarkCurrent();
      markFirstTurn();
    }
  }


  boolean onFirstPromptTurn() {
    return getPromptSeq().indexOf(getCurrentTurn()) == 0;
  }

  /**
   * Spoken or prompt sequence
   *
   * @return
   */
  List<T> getPromptSeq() {
    if (isInterpreter) {
      return promptTurns;
    } else {
      boolean leftSpeaker = isLeftSpeakerSet();
      boolean rightSpeaker = isRightSpeakerSet();
      List<T> ts = (leftSpeaker && !rightSpeaker) ? leftTurnPanels : (!leftSpeaker && rightSpeaker) ? rightTurnPanels : allTurns;
      // logger.info("getPromptSeq " + ts.size());
      if (DEBUG_DETAIL) report("prompts", ts);
      return ts;
    }
  }

  /**
   * The other RESPONDING side of the conversation.
   *
   * @return
   */
  List<T> getRespSeq() {
    if (isInterpreter) {
      return middleTurnPanels;
    } else {
      boolean leftSpeaker = isLeftSpeakerSet();
      boolean rightSpeaker = isRightSpeakerSet();
      return leftSpeaker ? rightTurnPanels : rightSpeaker ? leftTurnPanels : null;
    }
  }

  boolean isMiddle(T turn) {
    return middleTurnPanels.contains(turn);
  }

  boolean isFirstMiddle(T turn) {
    return middleTurnPanels.indexOf(turn) == 0;
  }

  boolean isFirstPrompt(T turn) {
    return leftTurnPanels.indexOf(turn) == 0;
  }

  Boolean isLeftSpeakerSet() {
    return true;
  }

  Boolean isRightSpeakerSet() {
    return false;
  }

  /**
   * If there is a current turn and there is audio,
   * if the audio is playing, pause it,
   * if the audio is paused, play it, and mark it as current(?)
   *
   * @see #gotTurnClick
   * @see #gotClickOnPlay()
   */
  void playCurrentTurn() {
    T currentTurn = getCurrentTurn();
    if (currentTurn != null) {
      if (DEBUG_PLAY) logger.info("playCurrentTurn " + blurb());
      if (currentTurn.hasAudio()) {
        boolean didPause = currentTurn.doPlayPauseToggle();
        if (didPause) {
          if (DEBUG_PLAY) logger.info("playCurrentTurn did pause " + blurb());
          //setPlayButtonToPlay();
        } else {
          if (DEBUG_PLAY) {
            logger.info("playCurrentTurn maybe did play " + blurb());
          }
          currentTurn.markCurrent();
        }
      } else {
        currentTurn.showNoAudioToPlay();
      }

    } else {
      logger.warning("playCurrentTurn no current turn?");
    }
  }

  private String blurb() {
    return getExID() + " '" + getCurrentTurn().getText() + "'";
  }

  /**
   * TODO: consider other feedback to indicate audio is playing...
   *
   * @see HeadlessPlayAudio#startPlaying
   */
  @Override
  public void playStarted() {
    if (getCurrentTurn() != null) {
      if (DEBUG) {
        logger.info("playStarted - turn " + blurb());
      }
      //setPlayButtonToPause();
      markCurrent();
    }
  }

  /**
   * When the prompt finishes playing, move on to the next turn.
   *
   * @seex mitll.langtest.client.sound.PlayAudioPanel#setPlayLabel
   * @see HeadlessPlayAudio#songFinished
   * @see HeadlessPlayAudio#tellListenersPlayStopped
   * @see PlayAudioPanel#pause
   */
  @Override
  public void playStopped() {
    if (getCurrentTurn() != null) {
      if (DEBUG) {
        logger.info("playStopped for turn " + blurb());
      }

      //setPlayButtonToPlay();

//      if (isGotTurnClick()) {
//        logger.info("playStopped ignore since click?");
//      } else
      if (isSessionGoingNow()) {
        removeMarkCurrent();
        currentTurnPlayEnded();
      } else {
        if (!isDoRehearse())
          logger.info("playStopped ignore since session has ended (via play button click)");
      }
    } else {
      logger.info("playStopped - no current turn.");
    }
  }

  /**
   * @see #playStopped
   */
  void currentTurnPlayEnded() {
    if (DEBUG) {
      logger.info("currentTurnPlayEnded - turn " + getExID() + " sessionGoingNow " + sessionGoingNow);

//      String exceptionAsString = ExceptionHandlerDialog.getExceptionAsString(new Exception("got turn click"));
//      logger.info("logException stack " + exceptionAsString);

    }

    T next = getNext();
    if (DEBUG && next != null) {
      logger.info("currentTurnPlayEnded next turn " + next.getExID());
    }
    if (!makeNextVisible()) {
      if (DEBUG) logger.info("currentTurnPlayEnded didn't make next visible...");
    }

    if (next == null) {
      if (DEBUG) logger.info("currentTurnPlayEnded OK stop");
      if (DEBUG) logger.info("currentTurnPlayEnded : markCurrent ");
      markCurrent();
      makeVisible(getCurrentTurn());
      setPlayButtonToPlay();
    } else {
      removeMarkCurrent();
      setCurrentTurn(next);
      playCurrentTurn();
    }
  }

  private boolean sessionGoingNow;

  /**
   * @seex #setPlayButtonIcon
   * @see #playStarted
   */
  private void setPlayButtonToPause() {
    getPlayButtonToUse().setIcon(IconType.PAUSE);
    sessionGoingNow = true;
  }

  /**
   * @see #playCurrentTurn()
   */
  void setPlayButtonToPlay() {
    getPlayButtonToUse().setIcon(IconType.PLAY);
    sessionGoingNow = false;
  }

  boolean isSessionGoingNow() {
    return sessionGoingNow;
  }

  private Button getPlayButtonToUse() {
    Button widgets = doRehearse ? this.playButton : playYourselfButton;
    if (DEBUG) {
      logger.info("getPlayButtonToUse doRehearse " + doRehearse + " : " + widgets.getElement().getId());
    }
    return widgets;
  }
}
