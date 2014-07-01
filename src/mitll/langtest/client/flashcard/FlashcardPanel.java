package mitll.langtest.client.flashcard;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.ButtonGroup;
import com.github.gwtbootstrap.client.ui.ButtonToolbar;
import com.github.gwtbootstrap.client.ui.ControlGroup;
import com.github.gwtbootstrap.client.ui.Heading;
import com.github.gwtbootstrap.client.ui.Icon;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.constants.IconSize;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.github.gwtbootstrap.client.ui.constants.ToggleType;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.FocusPanel;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.AudioTag;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.custom.KeyStorage;
import mitll.langtest.client.custom.MyFlashcardExercisePanelFactory;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.sound.SoundFeedback;
import mitll.langtest.shared.CommonExercise;

/**
 * Created by GO22670 on 6/26/2014.
 */
public class FlashcardPanel extends HorizontalPanel {
  public static final String WARN_NO_FLASH = "<font color='red'>Flash is not activated. " +
    "Do you have a flashblocker? Please add this site to its whitelist.</font>";

  protected static final int DELAY_MILLIS = 1000;


  private static final String WAV = ".wav";
  private static final String MP3 = "." + AudioTag.COMPRESSED_TYPE;
  protected static final String ON = "On";
  protected static final String OFF = "Off";
  private static final String SHOW = "SHOW";
  private static final String ENGLISH = "English";
  private static final String PLAY = "PLAY";
  private static final String BOTH = "Both";
  private static final int LEFT_MARGIN_FOR_FOREIGN_PHRASE = 17;

  protected final CommonExercise exercise;
  protected Widget english;
  protected Widget foreign;

  private final MyFlashcardExercisePanelFactory.MySoundFeedback soundFeedback;
  Widget cardPrompt;
  protected final boolean addKeyBinding;
  protected final ExerciseController controller;
  protected final ControlState controlState;
  private final Panel mainContainer;
  private Panel leftState;
  private Panel rightColumn;
  protected SoundFeedback.EndListener endListener;
  protected final String instance;

  /**
   *
   *
   * @param e
   * @param service
   * @param controller
   * @param soundFeedback
   * @param endListener
   * @param instance
   * @see mitll.langtest.client.custom.MyFlashcardExercisePanelFactory.StatsPracticePanel#StatsPracticePanel(mitll.langtest.shared.CommonExercise)
   *
   */
  public FlashcardPanel(final CommonExercise e, final LangTestDatabaseAsync service,
                                final ExerciseController controller, boolean addKeyBinding,
                                final ControlState controlState,
                                MyFlashcardExercisePanelFactory.MySoundFeedback soundFeedback,
                                SoundFeedback.EndListener endListener,
                                String instance) {
    this.addKeyBinding = addKeyBinding;
    this.exercise = e;
    this.controller = controller;
    this.controlState = controlState;
    this.endListener = endListener;
    this.instance = instance;
    //  addStyleName("centeringPractice");
    // System.out.println("BootstrapExercisePanel.instance = " + instance);

    controlState.setStorage(new KeyStorage(controller));

    HTML warnNoFlash = new HTML(WARN_NO_FLASH);
    this.soundFeedback = soundFeedback;
    DivWidget inner2 = new DivWidget();
    Panel contentMiddle = getMiddlePrompt(e,inner2);
    mainContainer = contentMiddle;

    DivWidget belowDiv = new DivWidget();
    belowDiv.addStyleName("topFiveMargin");
    Panel threePartContent = getThreePartContent(controlState, contentMiddle, belowDiv);
    DivWidget inner = new DivWidget();
    add(inner);

    inner.add(threePartContent);

    addRecordingAndFeedbackWidgets(e, service, controller, inner2);
    warnNoFlash.setVisible(false);
    inner.add(warnNoFlash);
    getElement().setId("BootstrapExercisePanel");

    addWidgetsBelow(belowDiv);
    if (controlState.isAudioOn() && mainContainer.isVisible()) {
      playRef();
    }
    addStyleName("leftFiftyMargin");
    DivWidget finalWidgets = getFinalWidgets();
    if (finalWidgets != null) inner2.add(finalWidgets);

  }

  HTML clickToFlip;
  DivWidget clickToFlipContainer;
  protected DivWidget getFinalWidgets() {
	  clickToFlipContainer= new DivWidget();
	  clickToFlipContainer.setHeight("100px");
    clickToFlip = new HTML("Click to flip");

    clickToFlip.addStyleName("dontSelect");
    clickToFlipContainer.addStyleName("dontSelect");

    //boolean isEnglish = controlState.getShowState().equals(ControlState.ENGLISH);
    //boolean isForeign = controlState.getShowState().equals(ControlState.FOREIGN);
    //setTitleOfClick(controlState.isEnglish(), controlState.isForeign(), true);
    clickToFlip.addStyleName("floatRight");
    clickToFlip.getElement().getStyle().setFontWeight(Style.FontWeight.BOLDER);
    clickToFlip.getElement().getStyle().setMarginTop(82, Style.Unit.PX);
    //clickToFlipContainer.setVisible(!controlState.showBoth());
  clickToFlipContainer.getElement().getStyle().setVisibility(controlState.showBoth() ? Style.Visibility.HIDDEN : Style.Visibility.VISIBLE);

    Icon w = new Icon(IconType.UNDO);
    w.addStyleName("floatRight");
    w.addStyleName("leftFiveMargin");
    w.getElement().getStyle().setMarginTop(84, Style.Unit.PX);
    clickToFlipContainer.add(w);
    clickToFlipContainer.add(clickToFlip);
    //this.clickToFlipContainer = widget;
    return clickToFlipContainer;
  }

/*  private void setTitleOfClick(boolean isEnglish, boolean isForeign, boolean show) {
    String show1 = show ? "show" :"hide";
    if (clickToFlip != null) {
      if (isEnglish && !isForeign) {
        clickToFlip.setText("Click to " +
          show1 +
          " " + controller.getLanguage());
      } else if (!isEnglish && isForeign) {
        clickToFlip.setText("Click to " +
          show1 +
          " English");
      }
    }
  }*/

/*  private Panel getCenteredWrapper(Widget recordButton) {
    Panel recordButtonRow = new FluidRow();
    Paragraph recordButtonContainer = new Paragraph();
    recordButtonContainer.addStyleName("alignCenter");
    recordButtonContainer.add(recordButton);
    recordButton.addStyleName("alignCenter");
    recordButtonRow.add(new Column(12, recordButtonContainer));
    return recordButtonRow;
  }*/

  protected void addRecordingAndFeedbackWidgets(CommonExercise e, LangTestDatabaseAsync service, ExerciseController controller, Panel contentMiddle) {

  }

  /**
   * Left side, middle content, and right side
   * @param controlState
   * @param contentMiddle
   * @param belowDiv
   * @return
   */
  private Panel getThreePartContent(ControlState controlState, Panel contentMiddle, DivWidget belowDiv) {
    Panel horiz = new HorizontalPanel();

    leftState = getLeftState();
    if (leftState != null) horiz.add(leftState);

    Grid grid = new Grid(2, 1);
    grid.setWidget(0, 0, contentMiddle);
    grid.setWidget(1, 0, belowDiv);
    horiz.add(grid);

    rightColumn = getRightColumn(controlState);
    horiz.add(rightColumn);
    return horiz;
  }

  private Panel getMiddlePrompt(CommonExercise e,DivWidget inner) {
    cardPrompt = getCardPrompt(e);
    cardPrompt.getElement().setId("cardPrompt");
    inner.add(cardPrompt);

   // inner.add(getFinalWidgets());
    Panel contentMiddle = getCardContent();
    contentMiddle.add(inner);

    contentMiddle.addStyleName("cardBorderShadow");
    contentMiddle.addStyleName("minWidthFifty");


    return contentMiddle;
  }

  protected Panel getCardContent() {
    FocusPanel contentMiddle = new FocusPanel();
    contentMiddle.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        if (clickToFlip.isVisible()) {
          if (!controlState.showEnglish() || !controlState.showForeign()) {
            toggleVisibility(english);
       //   }
         // if (!controlState.showForeign()) {
            toggleVisibility(foreign);
            if (isHidden(foreign) && controlState.isAudioOn()) {
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
    //setTitleOfClick(controlState.isEnglish(),controlState.isForeign(),!hidden);
  }

  private boolean isHidden(Widget english) {
    Style style = english.getElement().getStyle();
    String visibility = style.getVisibility();
    return visibility.equals("hidden");
  }

  protected void setMainContentVisible(boolean vis) {
    if (leftState != null) leftState.setVisible(vis);
    mainContainer.setVisible(vis);
    rightColumn.setVisible(vis);
  }

  private Panel getRightColumn(final ControlState controlState) {
    Panel rightColumn = new VerticalPanel();

    if (!isSiteEnglish()) {
      rightColumn.add(getShowGroup(controlState));
    }
    rightColumn.add(getAudioGroup(controlState));
    Widget feedbackGroup = getFeedbackGroup(controlState);
    if (feedbackGroup != null) rightColumn.add(feedbackGroup);
    final Button shuffle = new Button("Shuffle");
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

  protected Widget getFeedbackGroup(ControlState controlState) { return null;  }
  protected void gotShuffleClick(boolean b) {}
  protected Panel getLeftState() { return null; }
  protected void addWidgetsBelow(Panel toAddTo) {}

  /**
   * @see #getAudioGroup(mitll.langtest.client.flashcard.ControlState)
   * @see #getQuestionContent
   */
  private void playRefLater() {
   // System.out.println("playRefLater... ---------- " + exercise.getID());
    Scheduler.get().scheduleDeferred(new Command() {
      public void execute() {
        playRef();
      }
    });
  }

  /**
   * @see #getRightColumn(mitll.langtest.client.flashcard.ControlState)
   * @param controlState
   * @return
   */
  private ControlGroup getAudioGroup(final ControlState controlState) {
    ControlGroup group = new ControlGroup(PLAY + " " + controller.getLanguage().toUpperCase());
    ButtonToolbar w = new ButtonToolbar();
    group.add(w);
    ButtonGroup buttonGroup = new ButtonGroup();
    w.add(buttonGroup);

    buttonGroup.setToggle(ToggleType.RADIO);
    Button onButton = new Button(ON);
    onButton.getElement().setId(PLAY+"_On");
    controller.register(onButton, exercise.getID());
    buttonGroup.add(onButton);

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


    Button offButton = new Button(OFF);
    offButton.getElement().setId(PLAY+"_Off");
    controller.register(offButton, exercise.getID());

    buttonGroup.add(offButton);

    offButton.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        controlState.setAudioOn(false);
      }
    });
    offButton.setActive(!controlState.isAudioOn());

    return group;
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
    buttonGroup.add(getOff(controlState));
    buttonGroup.add(getBoth(controlState));

    return group;
  }

  private Button getOn(final ControlState controlState) {
    Button onButton = new Button(controller.getLanguage());
    onButton.getElement().setId("Show_On_" + controller.getLanguage());
    controller.register(onButton, exercise.getID());

    onButton.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        if (!controlState.isForeign()) {
          controlState.setShowState(ControlState.FOREIGN);
          System.out.println("getOn : now on " + controlState);
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
    controller.register(showEnglish, exercise.getID());

    showEnglish.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        if (!controlState.isEnglish()) {
          controlState.setShowState(ControlState.ENGLISH);
          System.out.println("getOff : now  " + controlState);
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
    controller.register(both, exercise.getID());

    both.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        if (!controlState.showBoth()) {
          controlState.setShowState(ControlState.BOTH);
          System.out.println("getBoth now  " + controlState);
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
   * @sexe #BootstrapExercisePanel
   */
  private Panel getCardPrompt(CommonExercise e) {
    Panel questionContent = getQuestionContent(e);
    questionContent.addStyleName("cardContent");
    return questionContent;
  }

  /**
   * If there's no english sentence, we use the foreign language phrase
   * @param e
   * @return
   */
  private Panel getQuestionContent(CommonExercise e) {
    String foreignSentence = e.getForeignLanguage();

    String englishSentence = e.getEnglish();
    boolean usedForeign = false;
    if (englishSentence.isEmpty()) {
      englishSentence = foreignSentence;
      usedForeign = true;
    }
    Heading englishHeading = new Heading(1, englishSentence);
    englishHeading.getElement().setId("EnglishPhrase");
    FocusPanel widgets = new FocusPanel();
    widgets.add(englishHeading);
    english = widgets;
    english.getElement().setId("EnglishPhrase_container");

    DivWidget div = new DivWidget();
    div.addStyleName("blockStyle");
    div.add(english);

    foreign = getForeignLanguageContent(foreignSentence,  e.hasRefAudio());

    if (!usedForeign) {
      div.add(foreign);
    }
    if (isSiteEnglish()) {
      if (getRefAudioToPlay() != null) {
        addAudioBindings2(widgets);
      }
    }

    showEnglishOrForeign();

    return div;
  }

  protected boolean isSiteEnglish() {
    return controller.getLanguage().equals("English");
  }

  /**
   * @see #getQuestionContent(mitll.langtest.shared.CommonExercise)
   * @param foreignSentence
   * @param hasRefAudio
   * @return
   */
  private Widget getForeignLanguageContent(String foreignSentence, boolean hasRefAudio) {
    Heading heading = new Heading(1, foreignSentence);
    heading.getElement().setId("FLPhrase");
    heading.getElement().getStyle().setMarginLeft(LEFT_MARGIN_FOR_FOREIGN_PHRASE, Style.Unit.PX);
    FocusPanel container = new FocusPanel();   // TODO : remove???
    container.getElement().setId("FLPhrase_container");

    Panel hp = new HorizontalPanel();
    hp.add(heading);
    if (hasRefAudio) {
      Icon w = new Icon(IconType.VOLUME_UP);
      w.setSize(IconSize.TWO_TIMES);
      Panel simple = new SimplePanel();
      simple.add(w);
      simple.addStyleName("leftTenMargin");
      hp.add(simple);
    }
    DivWidget centeringRow = getCenteringRow();
    centeringRow.add(hp);
    container.add(centeringRow);

    addAudioBindings2(container);
    return container;
  }

  DivWidget getCenteringRow() {
    DivWidget status = new DivWidget();
    status.getElement().setId("statusRow");
    status.addStyleName("alignCenter");
    status.addStyleName("inlineBlockStyle");
    return status;
  }

  /**
   * @see #getForeignLanguageContent(String, boolean)
   * @see #getQuestionContent(mitll.langtest.shared.CommonExercise)
   * @param focusPanel
   */
  private void addAudioBindings2(final FocusPanel focusPanel) {
    focusPanel.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        playRefLater();
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
    System.out.println("show english or foreign " + controlState);

    if (controlState.showBoth()) {
      showBoth();
    } else if (controlState.isEnglish()) {
      english.setHeight("100%");
     // english.setVisible(true);
      english.getElement().getStyle().setVisibility(Style.Visibility.VISIBLE);
     // foreign.setVisible(false);
      foreign.getElement().getStyle().setVisibility(Style.Visibility.HIDDEN);

      if (clickToFlip != null) {
//        clickToFlipContainer.setVisible(true);
        clickToFlipContainer.getElement().getStyle().setVisibility(Style.Visibility.VISIBLE);
      }
    } else if (controlState.isForeign()) {
      foreign.setHeight("100%");
//      english.setVisible(false);
      english.getElement().getStyle().setVisibility(Style.Visibility.HIDDEN);

      //foreign.setVisible(true);
      foreign.getElement().getStyle().setVisibility(Style.Visibility.VISIBLE);

      if (clickToFlip != null) {
        //clickToFlipContainer.setVisible(true);
        clickToFlipContainer.getElement().getStyle().setVisibility(Style.Visibility.VISIBLE);
      }
    }
    else {
      System.err.println("huh? show english or foreign " + controlState);

    }
    //setTitleOfClick(controlState.isEnglish(), controlState.isForeign(), true);
  }

  private void showBoth() {
    //english.setHeight("50%");
   // foreign.setHeight("50%");
//    english.setVisible(true);
 //   foreign.setVisible(true);
    english.getElement().getStyle().setVisibility(Style.Visibility.VISIBLE);
    foreign.getElement().getStyle().setVisibility(Style.Visibility.VISIBLE);

    if (clickToFlip != null) {
      //clickToFlipContainer.setVisible(false);
      clickToFlipContainer.getElement().getStyle().setVisibility(Style.Visibility.HIDDEN);

    }
  }

  protected MyFlashcardExercisePanelFactory.MySoundFeedback getSoundFeedback() {
    return soundFeedback;
  }

  /**
   * @see #playRef()
   * @return
   */
  protected String getRefAudioToPlay() {
    //System.out.println(getElement().getId() + " playing audio for " +exercise.getID());
    String path = exercise.getRefAudio();
    if (path == null) {
      path = exercise.getSlowAudioRef(); // fall back to slow audio
    }
    return path;
  }

  /**
   * @see #playRefLater()
   */
  private void playRef() {
    String refAudioToPlay = getRefAudioToPlay();

    if (refAudioToPlay != null) {
      playRef(refAudioToPlay);
    }
  }

  /**
   * @see #playRef()
   * @param path
   */
  private void playRef(String path) {
  //  System.out.println("playRef... ---------- " + exercise.getID() + " path " + path );

    path = getPath(path);
    final Widget textWidget = isSiteEnglish() ? english : foreign;
    getSoundFeedback().queueSong(path, new SoundFeedback.EndListener() {
      @Override
      public void songStarted() {
        textWidget.addStyleName("playingAudioHighlight");
        endListener.songStarted();
      }

      @Override
      public void songEnded() {
        removePlayingHighlight(textWidget);
        endListener.songEnded();
      }
    });
  }

  protected void removePlayingHighlight(Widget textWidget) {
    textWidget.removeStyleName("playingAudioHighlight");
  }

  protected String getPath(String path) {
    path = (path.endsWith(WAV)) ? path.replace(WAV, MP3) : path;
    path = ensureForwardSlashes(path);
    return path;
  }

  private String ensureForwardSlashes(String wavPath) { return wavPath.replaceAll("\\\\", "/"); }
}
