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
import com.google.gwt.event.dom.client.*;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.*;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.custom.KeyStorage;
import mitll.langtest.client.custom.TooltipHelper;
import mitll.langtest.client.custom.exercise.CommentBox;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.ExercisePanelFactory;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.client.qc.QCNPFExercise;
import mitll.langtest.client.scoring.CommentAnnotator;
import mitll.langtest.client.scoring.GoodwaveExercisePanel;
import mitll.langtest.client.sound.CompressedAudio;
import mitll.langtest.client.sound.SoundFeedback;
import mitll.langtest.shared.exercise.AnnotationExercise;
import mitll.langtest.shared.exercise.AudioRefExercise;
import mitll.langtest.shared.exercise.CommonShell;
import mitll.langtest.shared.exercise.MutableAnnotationExercise;

import java.util.logging.Logger;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 6/26/2014.
 */
class FlashcardPanel<T extends CommonShell & AudioRefExercise & AnnotationExercise & MutableAnnotationExercise> extends HorizontalPanel {
  private final Logger logger = Logger.getLogger("FlashcardPanel");

  static final String PLAYING_AUDIO_HIGHLIGHT = "playingAudioHighlight";
  private static final String WARN_NO_FLASH = "<font color='red'>Flash is not activated. " +
      "Do you have a flashblocker? Please add this site to its whitelist.</font>";

  static final int DELAY_MILLIS = 1000;

  static final String ON = "On";
  static final String OFF = "Off";
  private static final String SHOW = "START WITH";
  private static final String ENGLISH = "English";
  private static final String PLAY = "AUDIO";
  private static final String BOTH = "Both";
  private static final String CLICK_TO_FLIP = "Click to flip";
  private static final String SHUFFLE = "Shuffle";

  final T exercise;
  Widget english;
  Widget foreign;

  private final MySoundFeedback soundFeedback;
  final boolean addKeyBinding;
  final ExerciseController controller;
  final ControlState controlState;
  private final Panel mainContainer;
  private Panel leftState;
  private Panel rightColumn;
  final SoundFeedback.EndListener endListener;
  final String instance;
  final ListInterface exerciseList;
  private final DivWidget prevNextRow;
  final LangTestDatabaseAsync service;
  boolean showOnlyEnglish = false;

  /**
   * @param e
   * @param service
   * @param controller
   * @param soundFeedback
   * @param endListener
   * @param instance
   * @param exerciseList
   * @see ExercisePanelFactory#getExercisePanel(mitll.langtest.shared.exercise.Shell)
   */
  public FlashcardPanel(final T e, final LangTestDatabaseAsync service,
                        final ExerciseController controller, boolean addKeyBinding,
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
    this.service = service;
    controlState.setStorage(new KeyStorage(controller));

    this.soundFeedback = soundFeedback;
    final DivWidget inner2 = new DivWidget();
    inner2.getElement().setId("middle_vert_container");

    Panel contentMiddle = getCardContent();
    DivWidget firstRow = getFirstRow(controller);
    //  contentMiddle.add(contentRow);
    DivWidget cardPrompt = getCardPrompt(e);
    cardPrompt.insert(firstRow, 0);
    getMiddlePrompt(cardPrompt, contentMiddle, inner2);
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

    //  logger.info("Adding recording widgets to " + inner2.getElement().getId());
    Scheduler.get().scheduleDeferred(new Command() {
      public void execute() {
        addRecordingAndFeedbackWidgets(getID(), service, controller, inner2);
      }
    });

    //  logger.info("After adding recording widgets to " + inner2.getElement().getId());

    inner2.add(getFinalWidgets());

    HTML warnNoFlash = new HTML(WARN_NO_FLASH);
    warnNoFlash.setVisible(false);
    inner.add(warnNoFlash);

    getElement().setId("BootstrapExercisePanel");

    addPrevNextWidgets(prevNextRow);

    addRowBelowPrevNext(lowestRow);
    if (controlState.isAudioOn() && mainContainer.isVisible() && !isHidden(foreign)) {
      playRef();
    }
  }

  int getID() {
    return exercise.getID();
  }

  private CommentBox commentBox;

  /**
   * @param controller
   * @return
   * @see #FlashcardPanel
   */
  DivWidget getFirstRow(ExerciseController controller) {
    commentBox = new CommentBox(exercise.getID(), controller, new CommentAnnotator() {
      @Override
      public void addIncorrectComment(String commentToPost, String field) {
        addAnnotation(field, GoodwaveExercisePanel.INCORRECT, commentToPost);
      }

      @Override
      public void addCorrectComment(String field) {
        addAnnotation(field, GoodwaveExercisePanel.CORRECT, "");
      }
    }, exercise);

    DivWidget left = new DivWidget();
    left.add(commentBox.getEntry(QCNPFExercise.FOREIGN_LANGUAGE, null, exercise.getAnnotation(QCNPFExercise.FOREIGN_LANGUAGE)));
    left.addStyleName("floatLeft");
    left.getElement().setId("leftCommentBoxContainer");
    // left.setWidth("50%");

    DivWidget firstRow = new DivWidget();
    firstRow.add(left);
    firstRow.getElement().setId("firstRow");
    return firstRow;
  }

  private void addAnnotation(final String field, final String status, final String commentToPost) {
    service.addAnnotation(exercise.getID(), field, status, commentToPost, controller.getUser(), new AsyncCallback<Void>() {
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

  void addRecordingAndFeedbackWidgets(int exerciseID, LangTestDatabaseAsync service, ExerciseController controller, Panel contentMiddle) {
    boolean noModel = controller.getProps().isNoModel();
    if (!noModel)
      logger.warning("addRecordingAndFeedbackWidgets : adding empty recording and feedback widgets " + this.getClass());
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
                                    DivWidget belowDiv, DivWidget lowestRow) {
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
  private Panel getMiddlePrompt(Widget cardPrompt, Panel contentMiddle, DivWidget inner) {
    inner.add(cardPrompt);

    contentMiddle.add(inner);
    contentMiddle.addStyleName("cardBorderShadow");
    contentMiddle.addStyleName("minWidthFifty");

    return contentMiddle;
  }

  /**
   * Card content is focusable, so we can click on it...
   *
   * @return
   * @see #FlashcardPanel
   */
  private Panel getCardContent() {
    final ClickableSimplePanel contentMiddle = new ClickableSimplePanel();

    contentMiddle.setHeight("320px");
    contentMiddle.getElement().setId("Focusable_content");
    contentMiddle.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        //System.out.println("---> click on card to flip...");

        if (clickToFlip.isVisible()) {
          boolean showEnglish = controlState.showEnglish();
          if (!showEnglish || !controlState.showForeign()) {
            toggleVisibility(english);
            toggleVisibility(foreign);
            boolean englishHidden = isHidden(english);
            controller.logEvent(contentMiddle, "flashcard_itself", exercise, "flip card to show " + (englishHidden ? " english" : controller.getLanguage()));
            if (!isHidden(foreign) && controlState.isAudioOn()) {
              playRef();
            }
          }
        }
      }
    });
    return contentMiddle;
  }

  private void toggleVisibility(Widget english) {
    Style style = english.getElement().getStyle();
    String visibility = style.getVisibility();
    boolean hidden = visibility.equals("hidden");
    style.setVisibility(hidden ? Style.Visibility.VISIBLE : Style.Visibility.HIDDEN);
  }

  private boolean isHidden(Widget english) {
    Style style = english.getElement().getStyle();
    String visibility = style.getVisibility();
    return visibility.equals("hidden");
  }

  void setMainContentVisible(boolean vis) {
    if (leftState != null) leftState.setVisible(vis);
    mainContainer.setVisible(vis);
    rightColumn.setVisible(vis);
  }

  private Panel getRightColumn(final ControlState controlState) {
    Panel rightColumn = new VerticalPanel();

    rightColumn.add(getAudioGroup(controlState));
    if (!isSiteEnglish()) {
      rightColumn.add(getShowGroup(controlState));
    }
    Widget feedbackGroup = getFeedbackGroup(controlState);
    if (feedbackGroup != null) rightColumn.add(feedbackGroup);
    final Button shuffle = new Button(SHUFFLE);
    shuffle.setToggle(true);
    shuffle.setIcon(IconType.RANDOM);
    shuffle.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        boolean shuffleOn = !shuffle.isToggled();

        //System.out.println("shuffle onClick " + shuffleOn);
        controlState.setSuffleOn(shuffleOn);
        gotShuffleClick(shuffleOn);
      }
    });
    shuffle.setActive(controlState.isShuffle());

    rightColumn.add(shuffle);

    rightColumn.addStyleName("leftTenMargin");
    return rightColumn;
  }

  Widget getFeedbackGroup(ControlState controlState) {
    return null;
  }

  void gotShuffleClick(boolean b) {
    exerciseList.setShuffle(b);
  }

  Panel getLeftState() {
    return null;
  }

  /**
   * Widgets below the card are a left button, a progress bar, and a right button.
   *
   * @param toAddTo
   * @see #FlashcardPanel(mitll.langtest.shared.exercise.CommonExercise, mitll.langtest.client.LangTestDatabaseAsync, mitll.langtest.client.exercise.ExerciseController, boolean, ControlState, MySoundFeedback, mitll.langtest.client.sound.SoundFeedback.EndListener, String, mitll.langtest.client.list.ListInterface)
   */
  private void addPrevNextWidgets(Panel toAddTo) {
    toAddTo.add(getPrevButton());
    toAddTo.add(getProgressBarWidget());
    toAddTo.add(getNextButton());
  }

  public void setPrevNextVisible(boolean val) {
    prevNextRow.setVisible(val);
  }

  void addRowBelowPrevNext(DivWidget lowestRow) {
  }

  private Button getPrevButton() {
    final Button left = new Button();
    controller.register(left, getID(), "prev button");
    left.setIcon(IconType.CARET_LEFT);
    left.addStyleName("floatLeft");
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

    onButton.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        if (!controlState.isAudioOn()) {
          playRefLater();
        }
        controlState.setAudioOn(true);
      }
    });
    onButton.setActive(controlState.isAudioOn());
    return onButton;
  }

  private Button getAudioOffButton() {
    Button offButton = new Button(OFF);
    offButton.getElement().setId(PLAY + "_Off");
    offButton.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        controlState.setAudioOn(false);
      }
    });
    offButton.setActive(!controlState.isAudioOn());
    controller.register(offButton, getID());
    return offButton;
  }


  private ControlGroup getShowGroup(final ControlState controlState) {
    ControlGroup group = new ControlGroup(SHOW);
    ButtonToolbar w = new ButtonToolbar();
    group.add(w);
    ButtonGroup buttonGroup = new ButtonGroup();
    buttonGroup.setVertical(true);
    buttonGroup.setToggle(ToggleType.RADIO);
    w.add(buttonGroup);

    buttonGroup.add(getOn(controlState));
    buttonGroup.add(getBoth(controlState));
    buttonGroup.add(getOff(controlState));

    return group;
  }

  private Button getOn(final ControlState controlState) {
    Button onButton = new Button(controller.getLanguage());
    onButton.getElement().setId("Show_On_" + controller.getLanguage());
    controller.register(onButton, getID());

    onButton.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        if (!controlState.isForeign()) {
          controlState.setShowState(ControlState.FOREIGN);
          //System.out.println("getOn : now on " + controlState);
          showEnglishOrForeign();
        }
      }
    });
    onButton.setActive(controlState.showForeign() && !controlState.showBoth());
    return onButton;
  }

  private Button getOff(final ControlState controlState) {
    Button showEnglish = new Button(ENGLISH);
    showEnglish.getElement().setId("Show_English");
    controller.register(showEnglish, getID());

    showEnglish.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        if (!controlState.isEnglish()) {
          controlState.setShowState(ControlState.ENGLISH);
          //System.out.println("getOff : now  " + controlState);
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

    String englishSentence = e.getEnglish();
    boolean usedForeign = false;
    if (englishSentence.isEmpty()) {
      englishSentence = foreignSentence;
      usedForeign = true;
    }
    FocusPanel widgets = makeEnglishPhrase(englishSentence);
    widgets.getElement().getStyle().setMarginLeft(-20, Style.Unit.PX);
    widgets.setWidth("100%");
    DivWidget div = new DivWidget();
    div.getElement().setId("QuestionContentFieldContainer");
    div.addStyleName("blockStyle");
    div.add(widgets);

    foreign = getForeignLanguageContent(foreignSentence, e.hasRefAudio());

    if (!usedForeign) {
      div.add(foreign);
    }
    if (isSiteEnglish()) {
      if (getRefAudioToPlay() != null) {
        addAudioBindings(widgets);
      }
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

  boolean isSiteEnglish() {
    return controller.getLanguage().equals("English");
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

    FocusPanel container = new FocusPanel();   // TODO : remove???
    container.getElement().setId("FLPhrase_container");

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
    container.add(centeringRow);

    addAudioBindings(container);
    return container;
  }

/*
  private void setForeignLanguageContentText(String text) { this.foreignLanguageContent.setText(text); }
*/

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
        // System.out.println("---> click on audio playback panel...");
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
   * @see #FlashcardPanel(mitll.langtest.shared.exercise.CommonExercise, mitll.langtest.client.LangTestDatabaseAsync, mitll.langtest.client.exercise.ExerciseController, boolean, ControlState, MySoundFeedback, mitll.langtest.client.sound.SoundFeedback.EndListener, String, mitll.langtest.client.list.ListInterface)
   */
  private void playRef() {
    String refAudioToPlay = getRefAudioToPlay();

    if (refAudioToPlay != null) {
      playRef(refAudioToPlay);
    }
  }

  /**
   * @return
   * @see #playRef()
   */
  String getRefAudioToPlay() {
    //System.out.println(getElement().getId() + " playing audio for " +exercise.getOldID());
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
    //  System.out.println("playRef... ---------- " + exercise.getOldID() + " path " + path );
    path = getPath(path);
    final Widget textWidget = isSiteEnglish() ? english : foreign;
    getSoundFeedback().queueSong(path, new SoundFeedback.EndListener() {
      @Override
      public void songStarted() {
        textWidget.addStyleName(PLAYING_AUDIO_HIGHLIGHT);
        endListener.songStarted();
      }

      @Override
      public void songEnded() {
        removePlayingHighlight(textWidget);
        endListener.songEnded();
      }
    });
  }

  void removePlayingHighlight(Widget textWidget) {
    textWidget.removeStyleName(PLAYING_AUDIO_HIGHLIGHT);
  }

 // private CompressedAudio compressedAudio = new CompressedAudio();

  protected String getPath(String path) {
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
}
