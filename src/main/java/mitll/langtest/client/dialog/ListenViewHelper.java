package mitll.langtest.client.dialog;

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
import mitll.langtest.client.banner.IBanner;
import mitll.langtest.client.banner.NewContentChooser;
import mitll.langtest.client.custom.ContentView;
import mitll.langtest.client.custom.INavigation;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.list.SelectionState;
import mitll.langtest.client.scoring.PhonesChoices;
import mitll.langtest.client.scoring.RefAudioGetter;
import mitll.langtest.client.scoring.TurnPanel;
import mitll.langtest.client.sound.HeadlessPlayAudio;
import mitll.langtest.client.sound.PlayListener;
import mitll.langtest.shared.dialog.DialogMetadata;
import mitll.langtest.shared.dialog.DialogType;
import mitll.langtest.shared.dialog.IDialog;
import mitll.langtest.shared.exercise.ClientExercise;
import mitll.langtest.shared.exercise.ExerciseAttribute;
import mitll.langtest.shared.scoring.AlignmentOutput;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static com.google.gwt.dom.client.Style.Unit.PX;

/**
 * Created by go22670 on 4/5/17.
 */
public class ListenViewHelper<T extends TurnPanel>
    extends DialogView implements ContentView, PlayListener, IListenView {
  private final Logger logger = Logger.getLogger("ListenViewHelper");

  private static final String MIDDLE_COLOR = "#00800059";

  public enum COLUMNS {LEFT, MIDDLE, RIGHT, UNK}

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

  final ExerciseController controller;
  final Map<Integer, AlignmentOutput> alignments = new HashMap<>();

  final List<T> allTurns = new ArrayList<>();
  private final List<T> promptTurns = new ArrayList<>();
  final List<T> leftTurnPanels = new ArrayList<>();
  private final List<T> middleTurnPanels = new ArrayList<>();
  private final List<T> rightTurnPanels = new ArrayList<>();

  private T currentTurn;

  private CheckBox leftSpeakerBox = null;
  private CheckBox rightSpeakerBox = null;

  CheckBox speakerBoxes;
  private ComplexWidget slider;
  private Button playButton;
  private DivWidget dialogHeader;

  private static final boolean DEBUG = false;
  private static final boolean DEBUG_PLAY = false;
  /**
   *
   */
  protected int dialogID;
  boolean isInterpreter = false;

  /**
   * @param controller
   * @see NewContentChooser#NewContentChooser(ExerciseController, IBanner)
   */
  public ListenViewHelper(ExerciseController controller) {
    this.controller = controller;
  }

  /**
   * @param listContent
   * @param instanceName IGNORED HERE
   * @see NewContentChooser#showView(INavigation.VIEWS, boolean, boolean)
   */
  @Override
  public void showContent(Panel listContent, INavigation.VIEWS instanceName) {
    promptTurns.clear();
    allTurns.clear();
    leftTurnPanels.clear();
    middleTurnPanels.clear();
    rightTurnPanels.clear();
    currentTurn = null;

    int dialogFromURL = getDialogFromURL();
    controller.getDialogService().getDialog(dialogFromURL, new AsyncCallback<IDialog>() {
      @Override
      public void onFailure(Throwable caught) {
        controller.handleNonFatalError("getting dialogs", caught);
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

  /**
   * @param dialogID can be -1 if we just jump into a rehearse view without choosing a dialog first...
   * @param dialog
   * @param child
   */
  void showDialogGetRef(int dialogID, IDialog dialog, Panel child) {
    this.dialogID = dialog.getID();
    isInterpreter = dialog.getKind() == DialogType.INTERPRETER;

    showDialog(dialogID, dialog, child);

    getRefAudio(new ArrayList<RefAudioGetter>(allTurns).iterator());
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
      child.add(new HTML("hmmm can't find dialog #" + dialogID + " in database"));
    } else {
      child.add(dialogHeader = new DialogHeader(controller, getPrevView(), getNextView()).getHeader(dialog));

      DivWidget controlAndSpeakers = new DivWidget();
      styleControlRow(controlAndSpeakers);
      child.add(controlAndSpeakers);

      controlAndSpeakers.add(getControls());
      controlAndSpeakers.add(getSpeakerRow(dialog));

      child.add(getTurns(dialog));
    }
  }

  @NotNull
  private DivWidget getSpeakerRow(IDialog dialog) {
    DivWidget rowOne = new DivWidget();
    rowOne.getElement().setId("speakerRow");
    rowOne.getElement().getStyle().setMarginTop(5, PX);

    {
      String label = dialog.getSpeakers().get(0);
      if (label == null) label = "A";

      if (isInterpreter) {
        Heading w = new Heading(4, label);

        DivWidget left = new DivWidget();
        left.add(w);
        left.setWidth("100px");

        styleLeftSpeaker(w);

        left.addStyleName("floatLeft");
        left.addStyleName("bubble");
        left.addStyleName("leftbubble");
        //left.addStyleName("leftFiveMargin");
        left.getElement().getStyle().setBackgroundColor(LEFT_COLOR);

        rowOne.add(left);
      } else {
        leftSpeakerBox = addLeftSpeaker(rowOne, label);
      }
    }

    {
      if (isInterpreter) {
        String label2 = dialog.getSpeakers().get(2);
        if (label2 == null) label2 = "B";

        Heading w = new Heading(4, label2);

        DivWidget right = new DivWidget();
        right.setWidth("100px");
        right.add(w);
        right.addStyleName("bubble");
        right.addStyleName("rightbubble");
        right.addStyleName("floatRight");
        //left.addStyleName("leftFiveMargin");
        right.getElement().getStyle().setBackgroundColor(RIGHT_BKG_COLOR);
        styleRightSpeaker(w);
        rowOne.add(right);
      }
    }

    {
      String label1 = dialog.getSpeakers().get(1);
      if (label1 == null) label1 = "Interpreter";

      if (isInterpreter) {
        Heading w = new Heading(4, "Interpreter");

        DivWidget middle = new DivWidget();
        middle.addStyleName("bubble");
        middle.setWidth(240 + "px");
        middle.setHeight("44px");
        middle.getElement().getStyle().setMarginTop(0, PX);
        middle.getElement().getStyle().setMarginBottom(0, PX);
        middle.getElement().getStyle().setProperty("marginLeft", "auto");
        middle.getElement().getStyle().setProperty("marginRight", "auto");
        middle.add(w);
        styleLabel(w);
        w.getElement().getStyle().setMarginLeft(43, PX);

        middle.getElement().getStyle().setBackgroundColor(MIDDLE_COLOR);
        rowOne.add(middle);
      } else {
        rightSpeakerBox = addRightSpeaker(rowOne, label1);
      }
    }


    return rowOne;
  }

  private void styleControlRow(DivWidget rowOne) {
    rowOne.addStyleName("cardBorderShadow");
    Style style = rowOne.getElement().getStyle();
    style.setProperty("position", "sticky");
    style.setTop(0, PX);

    setControlRowHeight(rowOne);

//    rowOne.setWidth(97 + "%");
    style.setMarginTop(10, PX);
    style.setMarginBottom(10, PX);
    style.setZIndex(1000);
  }

  void setControlRowHeight(DivWidget rowOne) {
    rowOne.setHeight(getControlRowHeight() + "px");
  }

  private int getControlRowHeight() {
    return 105;
  }

  private CheckBox addLeftSpeaker(DivWidget rowOne, String label) {
    CheckBox checkBox = new CheckBox(label, true);
    setLeftTurnSpeakerInitial(checkBox);
    styleLeftSpeaker(checkBox);

    checkBox.addValueChangeHandler(event -> speakerOneCheck(event.getValue()));

    rowOne.add(getLeftSpeakerDiv(checkBox));
    return checkBox;
  }

  private void styleLeftSpeaker(UIObject checkBox) {
//    checkBox.addStyleName("floatLeft");
//    checkBox.addStyleName("leftFiveMargin");
//    checkBox.addStyleName("leftSpeaker");
//
//    checkBox.getElement().getStyle().setBackgroundColor(LEFT_COLOR);
//    checkBox.getElement().getStyle().setMarginLeft(43,PX);
//    checkBox.getElement().getStyle().setFontSize(32,PX);

    styleLabel(checkBox);
  }

  @NotNull
  DivWidget getLeftSpeakerDiv(CheckBox checkBox) {
    DivWidget rightDiv = new DivWidget();
    rightDiv.add(checkBox);
    return rightDiv;
  }

  CheckBox addRightSpeaker(DivWidget rowOne, String label) {
    CheckBox checkBox = new CheckBox(label, true);

    setRightTurnInitialValue(checkBox);
    styleRightSpeaker(checkBox);

    checkBox.addValueChangeHandler(event -> speakerTwoCheck(event.getValue()));

    rowOne.add(getRightSpeakerDiv(checkBox));
    return checkBox;
  }

  private void styleRightSpeaker(UIObject checkBox) {
    // Style style = checkBox.getElement().getStyle();
    //  style.setBackgroundColor(RIGHT_BKG_COLOR);

//    checkBox.addStyleName("rightSpeaker");
//    checkBox.addStyleName("rightAlign");
//    checkBox.addStyleName("floatRight");
//    checkBox.addStyleName("rightFiveMargin");
    styleLabel(checkBox);
  }

  private void styleLabel(UIObject checkBox) {
    checkBox.getElement().getStyle().setMarginLeft(43, PX);
    checkBox.getElement().getStyle().setFontSize(32, PX);
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

  void setRightTurnInitialValue(CheckBox checkBox) {
    checkBox.setValue(true);
  }

  void speakerOneCheck(Boolean value) {
    if (!value && !isRightSpeakerSelected()) {
      setRightSpeaker();
    }
  }

  private void setRightSpeaker() {
    setRightSpeaker(true);
  }

  void setRightSpeaker(boolean value) {
    if (rightSpeakerBox != null) {
      rightSpeakerBox.setValue(value);
    }
  }

  void speakerTwoCheck(Boolean value) {
    if (!value && !isLeftSpeakerSelected()) {
      selectLeftSpeaker();
    }
  }

  private void selectLeftSpeaker() {
    setLeftSpeaker(true);
  }

  void setLeftSpeaker(boolean val) {
    if (leftSpeakerBox != null)
      leftSpeakerBox.setValue(val);
  }

  private Boolean isLeftSpeakerSelected() {
    return leftSpeakerBox == null || leftSpeakerBox.getValue();
  }

  private Boolean isRightSpeakerSelected() {
    return rightSpeakerBox == null || rightSpeakerBox.getValue();
  }

  /**
   * @param dialog
   * @return
   * @see #showDialog
   */
  @NotNull
  DivWidget getTurns(IDialog dialog) {
    DivWidget rowOne = new DivWidget();
    rowOne.getElement().setId("turnContainer");
    rowOne.getElement().getStyle().setOverflow(Style.Overflow.HIDDEN);
    rowOne.getElement().getStyle().setMarginTop(10, PX);
    rowOne.addStyleName("cardBorderShadow");
    rowOne.getElement().getStyle().setMarginBottom(10, PX);

    List<String> speakers = dialog.getSpeakers();
    //logger.info("speakers " + speakers);

    Map<String, List<ClientExercise>> speakerToEx = dialog.groupBySpeaker();
    String middle = speakers.get(1);
    List<ClientExercise> middleTurns = speakerToEx.get(middle);

    String left = speakers.get(0);
    String right = speakers.get(2);
/*    logger.info("for speaker " + left + " got " + speakerToEx.get(left).size());
    logger.info("for speaker " + middle + " got " + middleTurns.size());
    logger.info("for speaker " + right + " got " + speakerToEx.get(right).size());*/

    dialog.getExercises().forEach(clientExercise -> {
      COLUMNS columnForEx = getColumnForEx(left, right, clientExercise);
      //    logger.info("ex " + clientExercise.getID() + " " + clientExercise.getEnglish() + " " + clientExercise.getForeignLanguage() + " : " + columnForEx);

      addTurn(rowOne, columnForEx, clientExercise);
    });

    markFirstTurn();

    return rowOne;
  }

  private COLUMNS getColumnForEx(String left, String right, ClientExercise clientExercise) {
    List<ExerciseAttribute> collect = getSpeakerAttributes(clientExercise);
    if (collect.isEmpty()) {
      logger.warning("no speaker " + clientExercise);
      return COLUMNS.UNK;
    } else {
      return getColumnForSpeaker(left, right, collect);
    }
  }

  @NotNull
  private COLUMNS getColumnForSpeaker(String left, String right, List<ExerciseAttribute> collect) {
    String speaker = getSpeaker(collect);
    COLUMNS columns;
    if (speaker.equalsIgnoreCase(left)) {
      columns = COLUMNS.LEFT;
    } else if (speaker.equalsIgnoreCase(right)) {
      columns = COLUMNS.RIGHT;
    } else {
      columns = COLUMNS.MIDDLE;
    }
    return columns;
  }

  private String getSpeaker(List<ExerciseAttribute> collect) {
    ExerciseAttribute exerciseAttribute = collect.get(0);
    return exerciseAttribute.getValue();
  }

  @NotNull
  private List<ExerciseAttribute> getSpeakerAttributes(ClientExercise clientExercise) {
    return clientExercise.getAttributes().stream().filter(exerciseAttribute -> exerciseAttribute.getProperty().equalsIgnoreCase(DialogMetadata.SPEAKER.name())).collect(Collectors.toList());
  }


  /**
   * @param rowOne
   * @param columns
   * @param clientExercise
   * @see
   */
  private void addTurn(DivWidget rowOne, COLUMNS columns, ClientExercise clientExercise) {
    T turn = getTurnPanel(clientExercise, columns);

    if (columns == COLUMNS.RIGHT) {
      rightTurnPanels.add(turn);
      promptTurns.add(turn);
    } else if (columns == COLUMNS.LEFT) {
      leftTurnPanels.add(turn);
      promptTurns.add(turn);
    } else if (columns == COLUMNS.MIDDLE) {
      middleTurnPanels.add(turn);
    }

    allTurns.add(turn);

    rowOne.add(turn);
  }

  private void markFirstTurn() {
    if (!allTurns.isEmpty()) {
      setCurrentTurn(allTurns.get(0));
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

              if (Scheduler.get() != null) {
                Scheduler.get().scheduleDeferred(() -> {
                  if (true) {
                    getRefAudio(iterator);
                  } else {
//              /
                  }
                });
              }
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
  T getTurnPanel(ClientExercise clientExercise, COLUMNS columns) {
    T turn = reallyGetTurnPanel(clientExercise, columns);
    turn.addWidgets(true, false, PhonesChoices.HIDE);
    turn.addPlayListener(this);
    turn.addDomHandler(event -> gotTurnClick(turn), ClickEvent.getType());
    return turn;
  }

  @NotNull
  T reallyGetTurnPanel(ClientExercise clientExercise, COLUMNS columns) {

    TurnPanel widgets = new TurnPanel(
        clientExercise,
        controller,
        null,
        alignments,
        this,
        columns);
    if (columns == COLUMNS.MIDDLE) {
      widgets.addStyleName("inlineFlex");
      widgets.setWidth("100%");
    }
    return (T) widgets;
  }

  private boolean gotTurnClick = false;

  void gotTurnClick(T turn) {
    gotTurnClick = true;
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
  DivWidget getControls() {
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

    {
      Icon w = new Icon(IconType.VOLUME_UP);
      w.addStyleName("leftTenMargin");
      rowOne.add(w);
      rowOne.add(slider = getSlider());
    }

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

  @Override
  public int getDialogSessionID() {
    return -1;
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

    List<T> seq = getAllTurns();

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

    List<T> seq = getAllTurns();

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
  private void clearHighlightAndRemoveMark() {
    logger.info("clearHighlight on " + currentTurn);
    currentTurn.resetAudio();
    currentTurn.clearHighlight();
    removeMarkCurrent();
  }

  /**
   * @see #getControls
   */
  void gotPlay() {
    //   logger.info("got click on play ");

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

  /**
   * @return true if changed turn to next one
   * @see #gotPlay
   */
  boolean setTurnToPromptSide() {
    Boolean leftSpeakerSet = isLeftSpeakerSet();
    Boolean rightSpeakerSet = isRightSpeakerSet();
    if (leftSpeakerSet && rightSpeakerSet) {
      if (DEBUG || true) logger.info("setTurnToPromptSide both speakers ");
      return false;
    } else if (
        leftSpeakerSet && !leftTurnPanels.contains(currentTurn) ||  // current turn is not the prompt set
            rightSpeakerSet && !rightTurnPanels.contains(currentTurn)
    ) {
      if (DEBUG || true) logger.info("setTurnToPromptSide setNextTurnForSide ");

      setNextTurnForSide();
      return true;
    } else {
      return false;
    }
  }

  /**
   * TODO : not sure if this is right?
   * Wrap around if on last turn.
   */
  void setNextTurnForSide() {
    removeMarkCurrent();
    int i = allTurns.indexOf(currentTurn);

    if (currentTurn == null) {
      logger.warning("setNextTurnForSide no current turn");
    } else {
      if (DEBUG) logger.info("setNextTurnForSide current turn for ex " + currentTurn.getExID());
    }

    int nextIndex = (i + 1 == allTurns.size()) ? 0 : i + 1;

    if (DEBUG) logger.info("setNextTurnForSide " + i + " next " + nextIndex);

    setCurrentTurn(allTurns.get(nextIndex));
  }


  boolean onLastTurn() {
    return isLast(currentTurn);
  }

  private boolean isLast(T currentTurn) {
    List<T> seq = getAllTurns();
    return seq.indexOf(currentTurn) == seq.size() - 1;
  }

  boolean onFirstPromptTurn() {
    return getPromptSeq().indexOf(currentTurn) == 0;
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
      report(ts);
      return ts;
    }
  }

  List<T> getAllTurns() {
    return allTurns;
  }

  private void report(List<T> allTurns) {
    StringBuilder builder = new StringBuilder();
    allTurns.forEach(turn -> builder.append(turn.getExID()).append(", "));
    logger.info("seq " + builder);
  }

  /**
   * The other RESPONDING side of the conversation.
   *
   * @return
   */
  List<T> getRespSeq() {
    if (isInterpreter) return middleTurnPanels;
    else {
      boolean leftSpeaker = isLeftSpeakerSet();
      boolean rightSpeaker = isRightSpeakerSet();
      return leftSpeaker ? rightTurnPanels : rightSpeaker ? leftTurnPanels : null;
    }
  }

  Boolean isLeftSpeakerSet() {
    return isLeftSpeakerSelected();
  }

  Boolean isRightSpeakerSet() {
    return isRightSpeakerSelected();
  }


  /**
   * @see #gotTurnClick
   * @see #gotPlay()
   */
  void playCurrentTurn() {
    if (currentTurn != null) {
      if (DEBUG_PLAY) logger.info("playCurrentTurn " + currentTurn);
      boolean didPause = currentTurn.doPlayPauseToggle();
      if (didPause) {
        if (DEBUG_PLAY) logger.info("playCurrentTurn did pause " + currentTurn);
        setPlayButtonToPlay();
      } else {
        if (DEBUG_PLAY) logger.info("playCurrentTurn maybe did play " + currentTurn);
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
      if (DEBUG) {
        logger.info("playStarted - turn " + currentTurn);
      }
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
      if (DEBUG) {
        logger.info("playStopped for turn " + currentTurn);
      }

      setPlayButtonToPlay();
      removeMarkCurrent();
      currentTurnPlayEnded(false);
    } else {
      logger.info("playStopped - no current turn.");
    }
  }

  /**
   * @param wasRecording
   * @see #playStopped
   */
  void currentTurnPlayEnded(boolean wasRecording) {
    if (DEBUG) {
      logger.info("currentTurnPlayEnded (listen) - turn " + currentTurn.getExID() + " gotTurnClick " + gotTurnClick);
    }

    if (gotTurnClick) {
      gotTurnClick = false;
    } else {
      T next = getNext();
      if (DEBUG && next != null) {
        logger.info("next turn " + next.getExID());
      }
      makeNextVisible();

      if (next == null) {
        if (DEBUG) logger.info("OK stop");
      } else {
        removeMarkCurrent();
        setCurrentTurn(next);
        playCurrentTurn();
      }
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

  /**
   * @seex #setPlayButtonIcon
   * @see #playStarted
   */
  void setPlayButtonToPause() {
    playButton.setIcon(IconType.PAUSE);
  }

  /**
   * @see #playCurrentTurn()
   */
  void setPlayButtonToPlay() {
    playButton.setIcon(IconType.PLAY);
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
    List<T> seq = getAllTurns();
    int i = seq.indexOf(currentTurn);
    int i1 = i + 1;
    if (DEBUG) logger.info("getNext current " + i + " next " + i1);

    if (i1 > seq.size() - 1) {
      return null;
    } else {
      T widgets = seq.get(i1);
      if (DEBUG) logger.info("getNext current at " + i1 + " will be ex #" + widgets.getExID());
      return widgets;
    }
  }

  private T getPrev() {
    List<T> seq = getAllTurns();
    int i = seq.indexOf(currentTurn);
    int i1 = i - 1;

    if (i1 > -1) {
      return seq.get(i1);
    } else {
      return null;
    }
  }
}
