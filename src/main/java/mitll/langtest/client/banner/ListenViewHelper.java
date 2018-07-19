package mitll.langtest.client.banner;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.CheckBox;
import com.github.gwtbootstrap.client.ui.Heading;
import com.github.gwtbootstrap.client.ui.Image;
import com.github.gwtbootstrap.client.ui.base.ComplexWidget;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.github.gwtbootstrap.client.ui.resources.ButtonSize;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.custom.ContentView;
import mitll.langtest.client.custom.INavigation;
import mitll.langtest.client.custom.IViewContaner;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.list.SelectionState;
import mitll.langtest.client.scoring.DialogExercisePanel;
import mitll.langtest.client.scoring.IRecordDialogTurn;
import mitll.langtest.client.scoring.PhonesChoices;
import mitll.langtest.client.scoring.RefAudioGetter;
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
public class ListenViewHelper<T extends DialogExercisePanel<ClientExercise>> implements ContentView, PlayListener, IListenView {
  public static final int HEADER_HEIGHT = 120;
  private final Logger logger = Logger.getLogger("ListenViewHelper");


  //private static final String HIGHLIGHT_COLOR = "green";
  private static final String VALUE = "value";
  private static final String SLIDER_MAX = "100";
  private static final String MAX = "max";
  private static final String MIN = "min";


  private static final String SLIDER_MIN = "0";
  private static final String TYPE = "type";
  private static final String RANGE = "range";
  private static final String INPUT = "input";

  private static final int ROW_WIDTH = 97;
  private static final String HEIGHT = 100 + "px";
  private static final String RIGHT_BKG_COLOR = "#4aa8eeb0";
  private static final String LEFT_COLOR = "#e7e6ec";

  protected final ExerciseController controller;
  protected final Map<Integer, AlignmentOutput> alignments = new HashMap<>();


  protected final List<T> bothTurns = new ArrayList<>();
  protected final List<T> leftTurnPanels = new ArrayList<>();
  protected final List<T> rightTurnPanels = new ArrayList<>();

  protected T currentTurn;
  protected CheckBox leftSpeakerBox, rightSpeakerBox;
  private ComplexWidget slider;
  private static final boolean DEBUG = false;

  /**
   * @param controller
   * @param myView
   * @see NewContentChooser#NewContentChooser(ExerciseController, IBanner)
   */
  ListenViewHelper(ExerciseController controller, IViewContaner viewContainer, INavigation.VIEWS myView) {
    this.controller = controller;
  }

  @Override
  public void showContent(Panel listContent, String instanceName, boolean fromClick) {
    DivWidget child = new DivWidget();
    child.setWidth("100%");
    listContent.add(child);
    listContent.setWidth("90%");

    bothTurns.clear();
    leftTurnPanels.clear();
    rightTurnPanels.clear();
    currentTurn = null;

    controller.getDialogService().getDialog(new SelectionState().getDialog(), new AsyncCallback<IDialog>() {
      @Override
      public void onFailure(Throwable caught) {
        // TODO fill in
      }

      @Override
      public void onSuccess(IDialog dialog) {
        //      Scheduler.get().scheduleDeferred(() -> showDialogGetRef(dialog, child));
        showDialogGetRef(dialog, child);
      }
    });
  }

  private void showDialogGetRef(IDialog dialog, DivWidget child) {
    showDialog(dialog, child);
    List<RefAudioGetter> getters = new ArrayList<>(bothTurns);
    getRefAudio(getters.iterator());
  }

  private void showDialog(IDialog dialog, DivWidget child) {
    child.add(getHeader(dialog));
    child.add(getSpeakerRow(dialog));
    child.add(getTurns(dialog));
  }

  @NotNull
  private DivWidget getSpeakerRow(IDialog dialog) {
    DivWidget rowOne = new DivWidget();
    rowOne.addStyleName("cardBorderShadow");

    rowOne.setHeight("40px");
    rowOne.setWidth(97 + "%");
    rowOne.getElement().getStyle().setMarginTop(10, PX);

    List<String> speakers = dialog.getSpeakers();
    {
      String label = speakers.get(0);
      CheckBox checkBox = new CheckBox(label, true);
      setLeftTurnSpeakerInitial(checkBox);
      checkBox.addStyleName("floatLeft");
      checkBox.addStyleName("leftFiveMargin");
      checkBox.addStyleName("leftSpeaker");
      checkBox.getElement().getStyle().setBackgroundColor(LEFT_COLOR);

      checkBox.addValueChangeHandler(event -> speakerOneCheck(event.getValue()));
      leftSpeakerBox = checkBox;

      DivWidget rightDiv=new DivWidget();
      rightDiv.add(checkBox);
      rowOne.add(rightDiv);
    }

    rowOne.add(getControls());
    {
      String label = speakers.get(1);
      CheckBox checkBox = new CheckBox(label, true);

      setRightTurnInitialValue(checkBox);
      Style style = checkBox.getElement().getStyle();
      style.setBackgroundColor(RIGHT_BKG_COLOR);
      checkBox.addStyleName("rightSpeaker");

      checkBox.addValueChangeHandler(event -> speakerTwoCheck(event.getValue()));
      checkBox.addStyleName("rightAlign");
      checkBox.addStyleName("floatRight");
      checkBox.addStyleName("rightFiveMargin");
      rightSpeakerBox = checkBox;

      DivWidget rightDiv=new DivWidget();
      rightDiv.add(checkBox);
      rowOne.add(rightDiv);
    }

    rowOne.getElement().getStyle().setMarginBottom(10, PX);
    return rowOne;
  }

  private void setLeftTurnSpeakerInitial(CheckBox checkBox) {
    checkBox.setValue(true);
  }

  protected void setRightTurnInitialValue(CheckBox checkBox) {
    checkBox.setValue(true);
  }

  protected void speakerOneCheck(Boolean value) {
    logger.info("speaker one now " + value);
    //leftSpeaker = value;

//    if (!rightSpeaker) {
//      rightSpeaker = true;
//      rightSpeakerBox.setValue(true);
//    }

    setPlayButtonToPlay();
  }


  protected void speakerTwoCheck(Boolean value) {
    logger.info("speaker two now " + value);
//    rightSpeaker = value;
//
//    if (!leftSpeaker) {
//      leftSpeaker = true;
//      leftSpeakerBox.setValue(true);
//    }
    setPlayButtonToPlay();
  }

  @NotNull
  private DivWidget getHeader(IDialog dialog) {
    DivWidget outer = new DivWidget();

    {
      DivWidget row = new DivWidget();
      row.addStyleName("cardBorderShadow");
      row.setHeight(HEADER_HEIGHT + "px");
      row.setWidth(ROW_WIDTH + "%");
      row.addStyleName("inlineFlex");

      row.add(getLeftArrow());
      row.add(getRightArrow());

      {
        com.google.gwt.user.client.ui.Image flag = getFlag(dialog.getImageRef());
        flag.addStyleName("floatLeft");
        row.add(flag);
      }

      DivWidget vert = new DivWidget();
      vert.getElement().setId("vert");
      row.add(vert);
      vert.addStyleName("leftTenMargin");

      {
        DivWidget titleDiv = new DivWidget();
        titleDiv.addStyleName("titleBlue");
        titleDiv.add(getFLTitle(dialog));
        vert.add(titleDiv);
      }

      {
        DivWidget titleDiv = new DivWidget();
        titleDiv.getElement().getStyle().setBackgroundColor("#dff4fc");
        titleDiv.add(getHeading(5, dialog.getEnglish()));
        vert.add(titleDiv);
      }

      {
        DivWidget oreintDiv = new DivWidget();
        Heading w1 = new Heading(5, dialog.getOrientation());
        w1.addStyleName("wrapword");

        oreintDiv.add(w1);
        vert.add(oreintDiv);
      }
      outer.add(row);
    }
    return outer;
  }

  @NotNull
  private Heading getFLTitle(IDialog dialog) {
    return getHeading(3, dialog.getForeignLanguage());
  }

  @NotNull
  private Heading getHeading(int size, String foreignLanguage) {
    Heading w1 = new Heading(size, foreignLanguage);//, dialog.getEnglish());
    w1.getElement().getStyle().setMarginTop(0, PX);
    w1.getElement().getStyle().setMarginBottom(5, PX);
    return w1;
  }

  @NotNull
  private com.google.gwt.user.client.ui.Image getFlag(String cc) {
    com.google.gwt.user.client.ui.Image image = new com.google.gwt.user.client.ui.Image(cc);
    image.setHeight(HEIGHT);
    image.setWidth(HEIGHT);
    return image;
  }


  /**
   * @param dialog
   * @return
   * @see #showDialog
   */
  @NotNull
  private DivWidget getTurns(IDialog dialog) {
    DivWidget rowOne = new DivWidget();

    rowOne.setWidth(97 + "%");
    rowOne.getElement().getStyle().setMarginTop(10, PX);

    List<String> speakers = dialog.getSpeakers();

    Map<String, List<ClientExercise>> speakerToEx = dialog.groupBySpeaker();
    String right = speakers.get(1);
    List<ClientExercise> rightTurns = speakerToEx.get(right);

    dialog.getExercises().forEach(clientExercise -> {
      // logger.info("ex " + clientExercise.getID() + " audio " + clientExercise.getAudioAttributes());
      boolean isRight = rightTurns != null && rightTurns.contains(clientExercise);

      T turn = getTurnPanel(clientExercise, isRight);

      if (isRight) rightTurnPanels.add(turn);
      else leftTurnPanels.add(turn);

      bothTurns.add(turn);
      rowOne.add(turn);
    });

    if (!bothTurns.isEmpty()) {
      currentTurn = bothTurns.get(0);
      logger.info("getTurns : markCurrent ");
      markCurrent();
    }


    rowOne.getElement().getStyle().setMarginBottom(10, PX);
    return rowOne;
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
            final int reqid = next.getReq();
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

  @NotNull
  private T getTurnPanel(ClientExercise clientExercise, boolean isRight) {
    T turn = reallyGetTurnPanel(clientExercise, isRight);
    turn.addWidgets(true, false, PhonesChoices.HIDE);
    turn.getElement()
        .getStyle().setClear(Style.Clear.BOTH);

    turn.addPlayListener(this);

    turn.addDomHandler(event -> gotCardClick(turn), ClickEvent.getType());

    return turn;
  }

  @NotNull
  protected T reallyGetTurnPanel(ClientExercise clientExercise, boolean isRight) {
    T widgets = (T) new DialogExercisePanel<>(clientExercise, controller, null, alignments, this);
    widgets.setIsRight(isRight);
    return widgets;
  }

  private void gotCardClick(T turn) {
    removeMarkCurrent();
    this.currentTurn = turn;
    playCurrentTurn();
  }

  private Button playButton;

  /**
   * TODO add playback rate
   *
   * @return
   */
  @NotNull
  private DivWidget getControls() {
    DivWidget rowOne = new DivWidget();
    rowOne.getElement().getStyle().setTextAlign(Style.TextAlign.CENTER);

    {
      Button widgets = new Button("", IconType.BACKWARD, event -> gotBackward());
      widgets.addStyleName("leftFiveMargin");
      rowOne.add(widgets);
      //Button backwardButton = widgets;
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
     // Button forwardButton = widgets2;
    }

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
    //   int value = getVolume();
    controller.getSoundManager().setVolume(getVolume());
    //  logger.info("got slider change " + value);
  }

  @Override
  public int getVolume() {
    return slider.getElement().getPropertyInt(VALUE);
  }

  @Override
  public void addScore(int exid, float score, IRecordDialogTurn recordDialogTurn) {

  }

  @NotNull
  private Widget getLeftArrow() {
    DivWidget buttonDiv = new DivWidget();
    Button widgets = new Button("", IconType.ARROW_LEFT, event -> gotGoBack());
    widgets.addStyleName("leftFiveMargin");
    widgets.addStyleName("rightTenMargin");
    buttonDiv.add(widgets);
    return buttonDiv;
  }

  @NotNull
  private Widget getRightArrow() {
    DivWidget buttonDiv = new DivWidget();
    Button widgets = new Button("", IconType.ARROW_RIGHT, event -> gotGoForward());
    widgets.addStyleName("leftFiveMargin");
    widgets.addStyleName("rightTenMargin");
    buttonDiv.add(widgets);
    return buttonDiv;
  }

  private void gotGoBack() {
    controller.getNavigation().show(INavigation.VIEWS.DIALOG);
  }

  private void gotGoForward() {
    controller.getNavigation().show(INavigation.VIEWS.REHEARSE);
  }

  private void gotBackward() {
    setPlayButtonToPlay();

    List<T> seq = getSeq();

    int i = seq.indexOf(currentTurn);
    int i1 = i - 1;

    boolean isPlaying = currentTurn.doPause();

    currentTurn.clearHighlight();
    removeMarkCurrent();
    if (i1 < 0) {
      currentTurn = seq.get(seq.size() - 1);
    } else {
      currentTurn = seq.get(i1);
    }
    markCurrent();
    if (isPlaying) playCurrentTurn();
  }

  protected List<T> getSeq() {
    boolean leftSpeaker = isLeftSpeakerSet();
    boolean rightSpeaker = isRightSpeakerSet();
    return (leftSpeaker && !rightSpeaker) ? leftTurnPanels : (!leftSpeaker && rightSpeaker) ? rightTurnPanels : bothTurns;
  }

  protected Boolean isRightSpeakerSet() {
    return rightSpeakerBox.getValue();
  }

  protected Boolean isLeftSpeakerSet() {
    return leftSpeakerBox.getValue();
  }

  private void gotForward() {
    setPlayButtonToPlay();

    List<T> seq = getSeq();

    int i = seq.indexOf(currentTurn);
    int i1 = i + 1;

    boolean isPlaying = currentTurn.doPause();

    currentTurn.clearHighlight();
    removeMarkCurrent();
    if (i1 > seq.size() - 1) {
      currentTurn = seq.get(0);
    } else {
      currentTurn = seq.get(i1);
    }
    markCurrent();
    if (isPlaying) playCurrentTurn();
  }

  protected void gotPlay() {
    if (currentTurn.isPlaying()) {
      playButton.setIcon(IconType.PAUSE);
    } else {
      setPlayButtonToPlay();
    }

    Boolean leftSpeakerSet = isLeftSpeakerSet();
    Boolean rightSpeakerSet = isRightSpeakerSet();
    if (leftSpeakerSet && rightSpeakerSet) {

    } else if (leftSpeakerSet && !leftTurnPanels.contains(currentTurn) || rightSpeakerSet && !rightTurnPanels.contains(currentTurn)) {
      removeMarkCurrent();

      int i = bothTurns.indexOf(currentTurn); // must be on right
      currentTurn = bothTurns.get(i + 1);
    }

    playCurrentTurn();
  }

  boolean onFirstTurn() {
    return getSeq().indexOf(currentTurn) == 0;
  }

  protected void playCurrentTurn() {
    if (currentTurn != null) {
      if (DEBUG) logger.info("playCurrentTurn - turn " + currentTurn.getExID());
      currentTurn.doPlayPauseToggle();
    }
  }


  @Override
  public void playStarted() {
    if (currentTurn != null) {
      if (DEBUG) logger.info("playStarted - turn " + currentTurn.getExID());
      playButton.setIcon(IconType.PAUSE);
      markCurrent();
    }
  }


  @Override
  public void playStopped() {
    if (currentTurn != null) {
      if (DEBUG) logger.info("playStopped - turn " + currentTurn.getExID());
      removeMarkCurrent();
      currentTurnPlayEnded();
      setPlayButtonToPlay();
    }
  }

  /**
   * @see #playStopped
   */
  protected void currentTurnPlayEnded() {
    if (DEBUG) logger.info("currentTurnPlayEnded - turn " + currentTurn.getExID());
    List<T> seq = getSeq();
    int i = seq.indexOf(currentTurn);
    int i1 = i + 1;
    removeMarkCurrent();
    if (i1 > seq.size() - 1) {
      if (DEBUG) logger.info("OK stop");
      currentTurn = seq.get(0);
      markCurrent();
    } else {
      currentTurn = seq.get(i1);
      playCurrentTurn();
    }
  }

  protected void setPlayButtonToPlay() {
    playButton.setIcon(IconType.PLAY);
  }

  protected void removeMarkCurrent() {
    //   logger.info("removeMarkCurrent on " + currentTurn.getExID());
    currentTurn.removeMarkCurrent();
//    currentTurn.getElement().getStyle().setBorderColor("white");
  }

  protected void markCurrent() {
    //logger.info("markCurrent on " + currentTurn.getExID());
    currentTurn.markCurrent();

    //currentTurn.getElement().getStyle().setBorderColor(HIGHLIGHT_COLOR);
  }

  @Override
  public void setSmiley(Image smiley, double total) {

  }
}
