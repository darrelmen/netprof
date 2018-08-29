package mitll.langtest.client.banner;

import com.github.gwtbootstrap.client.ui.*;
import com.github.gwtbootstrap.client.ui.base.ComplexWidget;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.github.gwtbootstrap.client.ui.resources.ButtonSize;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.UIObject;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.custom.ContentView;
import mitll.langtest.client.custom.INavigation;
import mitll.langtest.client.custom.TooltipHelper;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.list.SelectionState;
import mitll.langtest.client.scoring.PhonesChoices;
import mitll.langtest.client.scoring.RefAudioGetter;
import mitll.langtest.client.scoring.TurnPanel;
import mitll.langtest.client.sound.HeadlessPlayAudio;
import mitll.langtest.client.sound.PlayListener;
import mitll.langtest.shared.dialog.IDialog;
import mitll.langtest.shared.exercise.ClientExercise;
import mitll.langtest.shared.scoring.AlignmentOutput;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.logging.Logger;

import static com.google.gwt.dom.client.Style.Unit.PX;

/**
 * Created by go22670 on 4/5/17.
 */
public class ListenViewHelper<T extends TurnPanel<ClientExercise>> extends DialogView implements ContentView, PlayListener, IListenView {
  private final Logger logger = Logger.getLogger("ListenViewHelper");

  private static final String VALUE = "value";
  private static final String SLIDER_MAX = "100";
  private static final String MAX = "max";
  private static final String MIN = "min";


  private static final String SLIDER_MIN = "0";
  private static final String TYPE = "type";
  private static final String RANGE = "range";
  private static final String INPUT = "input";

  private static final String RIGHT_BKG_COLOR = "#4aa8eeb0";
  private static final String LEFT_COLOR = "#e7e6ec";

  protected final ExerciseController controller;
  protected final Map<Integer, AlignmentOutput> alignments = new HashMap<>();

  final List<T> bothTurns = new ArrayList<>();
  final List<T> leftTurnPanels = new ArrayList<>();
  private final List<T> rightTurnPanels = new ArrayList<>();

  private T currentTurn;
  CheckBox leftSpeakerBox, rightSpeakerBox;
  private ComplexWidget slider;
  private Button playButton;
  private DivWidget dialogHeader;

  private static final boolean DEBUG = false;

  /**
   * @param controller
   * @see NewContentChooser#NewContentChooser(ExerciseController, IBanner)
   */
  ListenViewHelper(ExerciseController controller) {
    this.controller = controller;
  }

  /**
   * @param listContent
   * @param instanceName IGNORED HERE
   * @param fromClick
   * @see NewContentChooser#showView(INavigation.VIEWS, boolean, boolean)
   */
  @Override
  public void showContent(Panel listContent, INavigation.VIEWS instanceName, boolean fromClick) {
    bothTurns.clear();
    leftTurnPanels.clear();
    rightTurnPanels.clear();
    currentTurn = null;

    int dialogFromURL = getDialogFromURL();
    controller.getDialogService().getDialog(dialogFromURL, new AsyncCallback<IDialog>() {
      @Override
      public void onFailure(Throwable caught) {
        // TODO fill in
      }

      @Override
      public void onSuccess(IDialog dialog) {
        showDialogGetRef(dialogFromURL, dialog, listContent);
      }
    });
  }

  private int getDialogFromURL() {
    return new SelectionState().getDialog();
  }

  protected void showDialogGetRef(int dialogID, IDialog dialog, Panel child) {
    showDialog(dialogID, dialog, child);
    getRefAudio(new ArrayList<RefAudioGetter>(bothTurns).iterator());
  }


  /**
   * Main method for showing the three sections
   *
   * @param dialogID
   * @param dialog
   * @param child
   * @see #showDialogGetRef
   */
  private void showDialog(int dialogID, IDialog dialog, Panel child) {
    if (dialog == null) {
      child.add(new HTML("hmmm can't find dialog #" + dialogID +      " in database"));
    } else {
      child.add(dialogHeader = new DialogHeader(controller, getPrevView(), getNextView()).getHeader(dialog));
      child.add(getSpeakerRow(dialog));
      child.add(getTurns(dialog));
    }
  }

  @NotNull
  private DivWidget getSpeakerRow(IDialog dialog) {
    DivWidget rowOne = new DivWidget();
    styleControlRow(rowOne);

    leftSpeakerBox = addLeftSpeaker(rowOne, dialog.getSpeakers().get(0));

    rowOne.add(getControls());

    rightSpeakerBox = addRightSpeaker(rowOne, dialog.getSpeakers().get(1));

    return rowOne;
  }

  private void styleControlRow(DivWidget rowOne) {
    rowOne.addStyleName("cardBorderShadow");
    Style style = rowOne.getElement().getStyle();
    style.setProperty("position", "sticky");
    style.setTop(0, PX);
    rowOne.setHeight(getControlRowHeight() + "px");
    rowOne.setWidth(97 + "%");
    style.setMarginTop(10, PX);
    style.setMarginBottom(10, PX);
    style.setZIndex(1000);
  }

  int getControlRowHeight() {
    return 40;
  }

  private CheckBox addLeftSpeaker(DivWidget rowOne, String label) {
    CheckBox checkBox = new CheckBox(label, true);
    setLeftTurnSpeakerInitial(checkBox);
    checkBox.addStyleName("floatLeft");
    checkBox.addStyleName("leftFiveMargin");
    checkBox.addStyleName("leftSpeaker");
    checkBox.getElement().getStyle().setBackgroundColor(LEFT_COLOR);

    checkBox.addValueChangeHandler(event -> speakerOneCheck(event.getValue()));

    rowOne.add(getLeftSpeakerDiv(checkBox));
    return checkBox;
  }

  @NotNull
  protected DivWidget getLeftSpeakerDiv(CheckBox checkBox) {
    DivWidget rightDiv = new DivWidget();
    rightDiv.add(checkBox);
    return rightDiv;
  }

  private CheckBox addRightSpeaker(DivWidget rowOne, String label) {
    CheckBox checkBox = new CheckBox(label, true);

    setRightTurnInitialValue(checkBox);
    Style style = checkBox.getElement().getStyle();
    style.setBackgroundColor(RIGHT_BKG_COLOR);

    checkBox.addStyleName("rightSpeaker");
    checkBox.addStyleName("rightAlign");
    checkBox.addStyleName("floatRight");
    checkBox.addStyleName("rightFiveMargin");

    checkBox.addValueChangeHandler(event -> speakerTwoCheck(event.getValue()));

    rowOne.add(getRightSpeakerDiv(checkBox));
    return checkBox;
  }

  @NotNull
  DivWidget getRightSpeakerDiv(CheckBox checkBox) {
    DivWidget rightDiv = new DivWidget();
    rightDiv.add(checkBox);
    return rightDiv;
  }

  private void setLeftTurnSpeakerInitial(CheckBox checkBox) {
    checkBox.setValue(true);
  }

  protected void setRightTurnInitialValue(CheckBox checkBox) {
    checkBox.setValue(true);
  }

  protected void speakerOneCheck(Boolean value) {
//    setPlayButtonToPlay();
    if (!value && !rightSpeakerBox.getValue()) {
      rightSpeakerBox.setValue(true);
    }
  }

  protected void speakerTwoCheck(Boolean value) {
    //  setPlayButtonToPlay();
    if (!value && !leftSpeakerBox.getValue()) {
      leftSpeakerBox.setValue(true);
    }
  }


  /**
   * @param dialog
   * @return
   * @see #showDialog
   */
  @NotNull
  protected DivWidget getTurns(IDialog dialog) {
    DivWidget rowOne = new DivWidget();

//    turnContainer = rowOne;
    rowOne.getElement().setId("turnContainer");
    rowOne.setWidth(97 + "%");
    rowOne.getElement().getStyle().setOverflow(Style.Overflow.HIDDEN);
    rowOne.getElement().getStyle().setMarginTop(10, PX);
    rowOne.addStyleName("cardBorderShadow");
//
//    rowOne.getElement().getStyle().setPaddingBottom(50, PX);
    rowOne.getElement().getStyle().setMarginBottom(10, PX);

    List<String> speakers = dialog.getSpeakers();

    Map<String, List<ClientExercise>> speakerToEx = dialog.groupBySpeaker();
    String right = speakers.get(1);
    List<ClientExercise> rightTurns = speakerToEx.get(right);

    dialog.getExercises().forEach(clientExercise -> {
      // logger.info("ex " + clientExercise.getID() + " audio " + clientExercise.getAudioAttributes());
      addTurn(rowOne, rightTurns, clientExercise);
    });

    markFirstTurn();

    return rowOne;
  }

  private void addTurn(DivWidget rowOne, List<ClientExercise> rightTurns, ClientExercise clientExercise) {
    boolean isRight = rightTurns != null && rightTurns.contains(clientExercise);

    T turn = getTurnPanel(clientExercise, isRight);

    if (isRight) rightTurnPanels.add(turn);
    else leftTurnPanels.add(turn);

    bothTurns.add(turn);
    rowOne.add(turn);
  }

  private void markFirstTurn() {
    if (!bothTurns.isEmpty()) {
      setCurrentTurn(bothTurns.get(0));
      //   logger.info("getTurns : markCurrent ");
      markCurrent();
      makeVisible(currentTurn);
    }
  }

  /**
   * @param toMakeCurrent
   * @see #markFirstTurn()
   */
  void setCurrentTurn(T toMakeCurrent) {
    this.currentTurn = toMakeCurrent;
  }

  T getCurrentTurn() {
    return currentTurn;
  }

  void makeVisible(UIObject currentTurn) {
    currentTurn.getElement().scrollIntoView();
  }

  private void getRefAudio(final Iterator<RefAudioGetter> iterator) {
    if (iterator.hasNext()) {
      RefAudioGetter next = iterator.next();
      //logger.info("getRefAudio asking next panel...");

      if (false) {
        logger.info("getRefAudio : skip stale req for panel...");
      } else {
        next.getRefAudio(() -> {
          if (iterator.hasNext()) {
            //     logger.info("\tgetRefAudio panel complete...");
            //   final int reqid = next.getReq();
            if (true) {
              Scheduler.get().scheduleDeferred(() -> {
                if (true) {
                  getRefAudio(iterator);
                } else {
//              /
                }
              });
            }
          } else {
            //   logger.info("\tgetRefAudio all panels complete...");
          }
        });
      }
    }
  }

  /**
   * @param clientExercise
   * @param isRight
   * @return
   */
  @NotNull
  protected T getTurnPanel(ClientExercise clientExercise, boolean isRight) {
    T turn = reallyGetTurnPanel(clientExercise, isRight);
    turn.addWidgets(true, false, PhonesChoices.HIDE);
    turn.addPlayListener(this);
    turn.addDomHandler(event -> gotTurnClick(turn), ClickEvent.getType());
    return turn;
  }

  @NotNull
  protected T reallyGetTurnPanel(ClientExercise clientExercise, boolean isRight) {
    T widgets = (T) new TurnPanel<>(
        clientExercise,
        controller,
        null,
        alignments,
        this,
        isRight);
    return widgets;
  }

  protected void gotTurnClick(T turn) {
    removeMarkCurrent();
    setCurrentTurn(turn);
    playCurrentTurn();
  }

  /**
   * TODO add playback rate
   *
   * Prev, play, next buttons
   *
   * @return
   */
  @NotNull
  private DivWidget getControls() {
    DivWidget rowOne = new DivWidget();
    rowOne.getElement().setId("controls");
    rowOne.getElement().getStyle().setTextAlign(Style.TextAlign.CENTER);
    {
      Button widgets = new Button("", IconType.BACKWARD, event -> gotBackward());
      widgets.addStyleName("leftFiveMargin");
      rowOne.add(widgets);
    }
    {
      Button widgets1 = new Button("", IconType.PLAY, event -> gotPlay());
      widgets1.setSize(ButtonSize.LARGE);
      widgets1.addStyleName("leftFiveMargin");
      rowOne.add(widgets1);
      playButton = widgets1;
    }

    {
      Button widgets2 = new Button("", IconType.FORWARD, event -> gotForward());
      widgets2.addStyleName("leftFiveMargin");
      rowOne.add(widgets2);
    }

    Icon w = new Icon(IconType.VOLUME_UP);
    w.addStyleName("leftTenMargin");
    rowOne.add(w);
    rowOne.add(slider = getSlider());

    return rowOne;
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

  @NotNull
  private Widget getLeftArrow() {
    DivWidget buttonDiv = new DivWidget();
    Button widgets = new Button("", IconType.ARROW_LEFT, event -> gotGoBack());
    new TooltipHelper().addTooltip(widgets, getPrevTooltip());

    widgets.addStyleName("leftFiveMargin");
    widgets.addStyleName("rightTenMargin");
    buttonDiv.add(widgets);
    return buttonDiv;
  }

  @NotNull
  private Widget getRightArrow() {
    DivWidget buttonDiv = new DivWidget();
    Button widgets = new Button("", IconType.ARROW_RIGHT, event -> gotGoForward());
    new TooltipHelper().addTooltip(widgets, getNextTooltip());
    widgets.addStyleName("leftFiveMargin");
    widgets.addStyleName("rightTenMargin");
    buttonDiv.add(widgets);
    return buttonDiv;
  }

  private void gotGoBack() {
    controller.getNavigation().show(getPrevView());
  }

  private String getPrevTooltip() {
    return "Go back to " + getPrevView().toString();
  }

  private String getNextTooltip() {
    return "Go ahead to " + getNextView().toString();
  }

  private void gotGoForward() {
    controller.getNavigation().show(getNextView());
  }

  @NotNull
  protected INavigation.VIEWS getPrevView() {
    return INavigation.VIEWS.STUDY;
  }

  @NotNull
  protected INavigation.VIEWS getNextView() {
    return INavigation.VIEWS.REHEARSE;
  }

  void gotBackward() {
    setPlayButtonToPlay();

    List<T> seq = getSeq();

    int i = seq.indexOf(currentTurn);
    int i1 = i - 1;

    boolean isPlaying = currentTurn.doPause();

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


    if (isPlaying) playCurrentTurn();
  }

  void gotForward() {
    setPlayButtonToPlay();

    List<T> seq = getSeq();

    int i = seq.indexOf(currentTurn);
    int i1 = i + 1;

    boolean isPlaying = currentTurn.doPause();

    clearHighlightAndRemoveMark();

    // makeVisible(currentTurn);
    if (!makeNextVisible()) {
      //  logger.info("gotForward : make current turn visible!");
      makeVisible(dialogHeader);  // make the top header visible...
    }

    if (i1 > seq.size() - 1) {
      setCurrentTurn(seq.get(0));
    } else {
      setCurrentTurn(seq.get(i1));
    }

    markCurrent();
    if (isPlaying) playCurrentTurn();
  }

  /**
   * @see #gotForward()
   * @see #gotBackward()
   */
  protected void clearHighlightAndRemoveMark() {
    //  logger.info("clearHighlight on " + currentTurn);
    currentTurn.resetAudio();
    currentTurn.clearHighlight();
    removeMarkCurrent();
  }

  /**
   * @see #getControls
   */
  protected void gotPlay() {
    //   logger.info("got click on play ");
    //  setPlayButtonIcon();

    if (!setTurnToPromptSide()) {
      T currentTurn = getCurrentTurn();
      boolean last = isLast(currentTurn);
      if (last) logger.info("OK, on last - let's consider going back to start");
      if (currentTurn != null && !currentTurn.hasCurrentMark()) {
        markFirstTurn();
      }
    }

    playCurrentTurn();
  }

  private boolean isLast(T currentTurn) {
    List<T> seq = getSeq();
    return seq.indexOf(currentTurn) == seq.size();
  }

  /**
   * @return true if changed turn to next one
   */
  boolean setTurnToPromptSide() {
    Boolean leftSpeakerSet = isLeftSpeakerSet();
    Boolean rightSpeakerSet = isRightSpeakerSet();
    if (leftSpeakerSet && rightSpeakerSet) {
      // logger.info("setTurnToPromptSide both speakers ");
      return false;
    } else if (
        leftSpeakerSet && !leftTurnPanels.contains(currentTurn) ||  // current turn is not the prompt set
            rightSpeakerSet && !rightTurnPanels.contains(currentTurn)
    ) {
      setNextTurnForSide();
      return true;
    } else {
      return false;
    }
  }

  /**
   * Wrap around if on last turn.
   */
  protected void setNextTurnForSide() {
    removeMarkCurrent();
    int i = bothTurns.indexOf(currentTurn); // must be on right

    if (currentTurn == null) logger.warning("no current turn");
    else logger.info("current turn for ex " + currentTurn.getExID());

    int nextIndex = (i + 1 == bothTurns.size()) ? 0 : i + 1;
    logger.info("setCurrentTurnForSide " + i + " next " + nextIndex);

    setCurrentTurn(bothTurns.get(nextIndex));
  }

  boolean onFirstTurn() {
    return getSeq().indexOf(currentTurn) == 0;
  }

  /**
   * Spoken or prompt sequence
   *
   * @return
   */
  protected List<T> getSeq() {
    boolean leftSpeaker = isLeftSpeakerSet();
    boolean rightSpeaker = isRightSpeakerSet();
    return (leftSpeaker && !rightSpeaker) ? leftTurnPanels : (!leftSpeaker && rightSpeaker) ? rightTurnPanels : bothTurns;
  }

  /**
   * The other RESPONDING side of the conversation.
   *
   * @return
   */
  List<T> getRespSeq() {
    boolean leftSpeaker = isLeftSpeakerSet();
    boolean rightSpeaker = isRightSpeakerSet();
    return leftSpeaker ? rightTurnPanels : rightSpeaker ? leftTurnPanels : null;
  }

  Boolean isLeftSpeakerSet() {
    return leftSpeakerBox.getValue();
  }

  Boolean isRightSpeakerSet() {
    return rightSpeakerBox.getValue();
  }

  /**
   * @see #gotTurnClick
   */
  void playCurrentTurn() {
    if (currentTurn != null) {
      if (DEBUG) logger.info("playCurrentTurn - turn " + currentTurn.getExID());
      boolean didPause = currentTurn.doPlayPauseToggle();
      if (didPause) {
        logger.info("playCurrentTurn did pause - turn " + currentTurn.getExID());
        setPlayButtonToPlay();
      } else {
        //logger.info("playCurrentTurn maybe did play");
      }
    } else {
      logger.warning("playCurrentTurn no current turn?");
    }
  }

  /**
   * @see HeadlessPlayAudio#startPlaying
   */
  @Override
  public void playStarted() {
    if (currentTurn != null) {
      if (DEBUG)
        logger.info("playStarted - turn " + currentTurn);
      setPlayButtonToPause();
      markCurrent();
    }
  }

  /**
   * When the prompt finishes playing, move on to the next turn.
   *
   * @seex mitll.langtest.client.sound.PlayAudioPanel#setPlayLabel
   * @see HeadlessPlayAudio#songFinished
   */
  @Override
  public void playStopped() {
    if (currentTurn != null) {
      if (DEBUG || true)
        logger.info("playStopped for turn " + currentTurn);

      setPlayButtonToPlay();

//      String exceptionAsString = ExceptionHandlerDialog.getExceptionAsString(new Exception("playStopped " + currentTurn.getExID()));
//      logger.info("logException stack " + exceptionAsString);

      removeMarkCurrent();
      currentTurnPlayEnded();
    }
  }

  /**
   * @see #playStopped
   */
  protected void currentTurnPlayEnded() {
    if (DEBUG) logger.info("currentTurnPlayEnded (listen) - turn " + currentTurn.getExID());
    T next = getNext();
    makeNextVisible();

    if (next == null) {
      if (DEBUG) logger.info("OK stop");
    } else {
      removeMarkCurrent();
      setCurrentTurn(next);
      playCurrentTurn();
    }
  }

  void makeCurrentTurnVisible() {
    makeVisible(currentTurn);
  }

  private boolean makeNextVisible() {
    T next = getNext();
    if (next != null) {
      makeVisible((T) next);
      return true;
    } else {
      return false;
    }
  }

  private boolean makePrevVisible() {
    T next = getPrev();
    if (next != null) {
      makeVisible((T) next);
      return true;
    } else return false;
  }

  private boolean playing = false;

  /**
   * @see #gotPlay
   * @see RehearseViewHelper#gotPlay
   */
  void setPlayButtonIcon() {
    if (playing) {
      setPlayButtonToPause();
    } else {
      setPlayButtonToPlay();
    }
  }

  /**
   * @see #playStarted
   * @see #setPlayButtonIcon
   */
  private void setPlayButtonToPause() {
    //  logger.info("setPlayButtonToPause");
    playButton.setIcon(IconType.PAUSE);
    playing = false;
  }

  /**
   * @see #playCurrentTurn()
   */
  void setPlayButtonToPlay() {
//    logger.info("setPlayButtonToPlay");

//    String exceptionAsString = ExceptionHandlerDialog.getExceptionAsString(new Exception("setPlayButtonToPlay " + currentTurn.getExID()));
//    logger.info("logException stack " + exceptionAsString);

    playButton.setIcon(IconType.PLAY);
    playing = true;
  }

  void removeMarkCurrent() {
    //   logger.info("removeMarkCurrent on " + currentTurn.getExID());
    currentTurn.removeMarkCurrent();
  }

  void markCurrent() {
    currentTurn.markCurrent();
  }

  /**
   * @return null if on last turn
   */
  private T getNext() {
    List<T> seq = getSeq();
    int i = seq.indexOf(currentTurn);
    int i1 = i + 1;

    if (i1 > seq.size() - 1) {
      return null;
    } else {
      return seq.get(i1);
    }
  }

  private T getPrev() {
    List<T> seq = getSeq();
    int i = seq.indexOf(currentTurn);
    int i1 = i - 1;

    if (i1 > -1) {
      return seq.get(i1);
    } else {
      return null;
    }
  }
}
