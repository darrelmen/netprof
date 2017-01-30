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

package mitll.langtest.client.qc;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.*;
import com.github.gwtbootstrap.client.ui.CheckBox;
import com.github.gwtbootstrap.client.ui.Label;
import com.github.gwtbootstrap.client.ui.TabPanel;
import com.github.gwtbootstrap.client.ui.TextBox;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.github.gwtbootstrap.client.ui.constants.ToggleType;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.safehtml.shared.SimpleHtmlSanitizer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.*;
import mitll.langtest.client.LangTest;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.custom.Navigation;
import mitll.langtest.client.custom.tabs.RememberTabAndContent;
import mitll.langtest.client.exercise.DefectEvent;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.NavigationHelper;
import mitll.langtest.client.exercise.PostAnswerProvider;
import mitll.langtest.client.gauge.ASRScorePanel;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.client.list.PagingExerciseList;
import mitll.langtest.client.scoring.ASRScoringAudioPanel;
import mitll.langtest.client.scoring.AudioPanel;
import mitll.langtest.client.scoring.GoodwaveExercisePanel;
import mitll.langtest.client.sound.CompressedAudio;
import mitll.langtest.client.sound.PlayListener;
import mitll.langtest.server.database.user.BaseUserDAO;
import mitll.langtest.shared.ExerciseAnnotation;
import mitll.langtest.shared.ExerciseFormatter;
import mitll.langtest.shared.exercise.*;
import mitll.langtest.shared.user.MiniUser;

import java.util.*;
import java.util.logging.Logger;

/**
 * Created with IntelliJ IDEA.
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 12/12/13
 * Time: 5:44 PM
 * To change this template use File | Settings | File Templates.
 */
public class QCNPFExercise<T extends CommonShell & AudioRefExercise & AnnotationExercise & ScoredExercise>
    extends GoodwaveExercisePanel<T> {
  public static final String UNINSPECTED_TOOLTIP = "Item has uninspected audio.";
  private Logger logger = Logger.getLogger("QCNPFExercise");

  private static final String VOCABULARY = "Vocabulary:";

  private static final String DEFECT = "Defect?";
  public static final String FOREIGN_LANGUAGE = "foreignLanguage";
  public static final String TRANSLITERATION = "transliteration";
  public static final String ALTFL = "alternate";
  public static final String ENGLISH = "english";
  public static final String MEANING = "meaning";
  public static final String CONTEXT = "context";
  public static final String ALTCONTEXT = "altcontext";
  public static final String CONTEXT_TRANSLATION = "context translation";

  private static final String REF_AUDIO = "refAudio";
  /**
   * @see #addApprovedButton(ListInterface, NavigationHelper)
   */
  private static final String APPROVED = "Mark Inspected";
  private static final String NO_AUDIO_RECORDED = "No Audio Recorded.";
  private static final String COMMENT = "Comment";

  private static final String COMMENT_TOOLTIP = "Comments are optional.";
  private static final String CHECKBOX_TOOLTIP = "Check to indicate this field has a defect.";
  private static final String APPROVED_BUTTON_TOOLTIP  = "Indicate item has no defects.";
  private static final String APPROVED_BUTTON_TOOLTIP2 = "Item has been marked with a defect";
  private static final String ATTENTION_LL = "Attention LL";
  private static final String MARK_FOR_LL_REVIEW = "Mark for review by Lincoln Laboratory.";

  private static final int DEFAULT_MALE_ID = -2;
  private static final int DEFAULT_FEMALE_ID = -3;
  private static final String MALE = "Male";
  private static final MiniUser DEFAULT_MALE = new MiniUser(DEFAULT_MALE_ID, 30, true, MALE, false);
  private static final String FEMALE = "Female";
  private static final MiniUser DEFAULT_FEMALE = new MiniUser(DEFAULT_FEMALE_ID, 30, false, FEMALE, false);

  private Set<String> incorrectFields;
  private List<RequiresResize> toResize;
  private Set<Widget> audioWasPlayed;
  private final ListInterface listContainer;
  private Button approvedButton;
  private Tooltip approvedTooltip;
  private Tooltip nextTooltip;
  private List<RememberTabAndContent> tabs;

  /**
   * @param e
   * @param controller
   * @param listContainer
   * @param instance
   * @see mitll.langtest.client.custom.content.NPFHelper#getFactory(PagingExerciseList, String, boolean)
   */
  public QCNPFExercise(T e, ExerciseController controller,
                       ListInterface<CommonShell> listContainer,
                       String instance) {
    super(e, controller, listContainer, 1.0f, false, instance);

    this.listContainer = listContainer;
  }

  @Override
  protected ASRScorePanel makeScorePanel(T e, String instance) {
    if (audioWasPlayed == null) {
      initAudioWasPlayed();
    }
    return null;
  }

  private void initAudioWasPlayed() {
    audioWasPlayed = new HashSet<Widget>();
    toResize = new ArrayList<RequiresResize>();
    incorrectFields = new HashSet<String>();
  }

  /**
   * @param div
   * @see GoodwaveExercisePanel#addUserRecorder
   * @see #getQuestionContent
   */
  @Override
  protected void addGroupingStyle(Widget div) {
    div.addStyleName("buttonGroupInset7");
  }

  @Override
  protected void addQuestionContentRow(T e, Panel hp) {
    super.addQuestionContentRow(e, hp);
    hp.addStyleName("questionContentPadding");
  }

  /**
   * @param controller
   * @param listContainer
   * @param addKeyHandler
   * @return
   * @see mitll.langtest.client.scoring.GoodwaveExercisePanel#GoodwaveExercisePanel
   */
  protected NavigationHelper<CommonShell> getNavigationHelper(ExerciseController controller,
                                                              final ListInterface<CommonShell> listContainer,
                                                              boolean addKeyHandler) {
    NavigationHelper<CommonShell> navHelper = new NavigationHelper<CommonShell>(exercise, controller,
        new PostAnswerProvider() {
          @Override
          public void postAnswers(ExerciseController controller, HasID completedExercise) {
            nextWasPressed(listContainer, completedExercise);
          }
        },
        listContainer, addKeyHandler) {
      /**
       * So only allow next button when all audio has been played
       * @param exercise
       */
      @Override
      protected void enableNext(HasID exercise) {
        if (audioWasPlayed == null) {
          initAudioWasPlayed();
        }
        boolean allPlayed = audioWasPlayed.size() == toResize.size();
        next.setEnabled(allPlayed);
      }
    };

    nextTooltip = addTooltip(navHelper.getNext(), audioWasPlayed.size() == toResize.size() ?
        "Click to indicate item has been reviewed." :
        UNINSPECTED_TOOLTIP);

    if (!instance.contains(Navigation.REVIEW) && !instance.contains(Navigation.COMMENT)) {
      approvedButton = addApprovedButton(listContainer, navHelper);
      if (controller.hasModel()) {
        addAttnLLButton(listContainer, navHelper);
      }
    }
    setApproveButtonState();
    return navHelper;
  }

  /**
   * @param listContainer
   * @param widgets
   * @return
   * @see #getNavigationHelper(mitll.langtest.client.exercise.ExerciseController, mitll.langtest.client.list.ListInterface, boolean)
   */
  private Button addApprovedButton(final ListInterface listContainer, NavigationHelper widgets) {
    Button approved = new Button(APPROVED);
    approved.getElement().setId("approve");
    approved.addStyleName("leftFiveMargin");
    widgets.add(approved);
    approved.setType(ButtonType.PRIMARY);
    approved.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        markReviewed(listContainer, exercise);
      }
    });
    approvedTooltip = addTooltip(approved, APPROVED_BUTTON_TOOLTIP);
    return approved;
  }

  private Button addAttnLLButton(final ListInterface listContainer, NavigationHelper widgets) {
    Button attention = new Button(ATTENTION_LL);
    attention.getElement().setId("attention");
    attention.addStyleName("leftFiveMargin");
    widgets.add(attention);
    attention.setType(ButtonType.WARNING);
    attention.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        markAttentionLL(listContainer, exercise);
      }
    });
    addTooltip(attention, MARK_FOR_LL_REVIEW);
    return attention;
  }

  @Override
  protected void nextWasPressed(ListInterface listContainer, HasID completedExercise) {
    //System.out.println("nextWasPressed : load next exercise " + completedExercise.getOldID() + " instance " +instance);
    super.nextWasPressed(listContainer, completedExercise);
    markReviewed(listContainer, completedExercise);
  }

  /**
   * @param listContainer
   * @param completedExercise
   * @see #addApprovedButton(mitll.langtest.client.list.ListInterface, mitll.langtest.client.exercise.NavigationHelper)
   * @see #nextWasPressed
   */
  private void markReviewed(ListInterface listContainer, HasID completedExercise) {
    if (isCourseContent()) {
      markReviewed(completedExercise);
      boolean allCorrect = incorrectFields.isEmpty();
      listContainer.setState(completedExercise.getID(), allCorrect ? STATE.APPROVED : STATE.DEFECT);
      listContainer.redraw();
      navigationHelper.clickNext(controller,completedExercise);
    }
  }

  /**
   * So if the attention LL button has been pressed, clicking next should not step on that setting
   *
   * @param listContainer
   * @param completedExercise
   * @see #addAttnLLButton(ListInterface, NavigationHelper)
   */
  private void markAttentionLL(ListInterface listContainer, HasID completedExercise) {
    if (isCourseContent()) {
      controller.getQCService().markState(completedExercise.getID(), STATE.ATTN_LL,
          new AsyncCallback<Void>() {
            @Override
            public void onFailure(Throwable caught) {
            }

            @Override
            public void onSuccess(Void result) {
            }
          });

      listContainer.setSecondState(completedExercise.getID(), STATE.ATTN_LL);
      listContainer.redraw();
      navigationHelper.clickNext(controller,completedExercise);
    }
  }

  private boolean isCourseContent() {
    return !instance.equals(Navigation.REVIEW) && !instance.equals(Navigation.COMMENT);
  }

  /**
   * @param completedExercise
   * @see #checkBoxWasClicked(boolean, String, com.google.gwt.user.client.ui.Panel, com.google.gwt.user.client.ui.FocusWidget)
   * @see #markReviewed
   */
  private void markReviewed(final HasID completedExercise) {
    boolean allCorrect = incorrectFields.isEmpty();
    //System.out.println("markReviewed : exercise " + completedExercise.getOldID() + " instance " + instance + " allCorrect " + allCorrect);

    controller.getQCService().markReviewed(completedExercise.getID(), allCorrect,
        new AsyncCallback<Void>() {
          @Override
          public void onFailure(Throwable caught) {
          }

          @Override
          public void onSuccess(Void result) {
            //System.out.println("\tmarkReviewed.onSuccess exercise " + completedExercise.getOldID() + " marked reviewed!");
          }
        }
    );
  }

  /**
   * No user recorder for QC
   *
   * @param service
   * @param controller    used in subclasses for audio control
   * @param toAddTo
   * @param screenPortion
   * @param exercise
   */
  @Override
  protected void addUserRecorder(LangTestDatabaseAsync service, ExerciseController controller, Panel toAddTo,
                                 float screenPortion, T exercise) {
  }

  /**
   * @return
   */
  @Override
  protected Widget getItemContent(T e) {
    Panel column = new FlowPanel();
    column.getElement().setId("QCNPFExercise_QuestionContent");
    column.addStyleName("floatLeft");
    column.setWidth("100%");

    Panel row = new FlowPanel();
    row.add(getComment());

    column.add(row);

//    if (e.getModifiedDateTimestamp() != 0) {
//      Heading widgets = new Heading(5, "Changed", new Date(e.getModifiedDateTimestamp()).toString());
//    //  if (!e.getAudioAttributes().isEmpty()) {
//    //    widgets.addStyleName("floatRight");
//    //  }
//      row.add(widgets);
//    }

    column.add(getEntry(e, FOREIGN_LANGUAGE, VOCABULARY, e.getForeignLanguage()));
    column.add(getEntry(e, TRANSLITERATION, ExerciseFormatter.TRANSLITERATION, e.getTransliteration()));
    column.add(getEntry(e, ENGLISH, ExerciseFormatter.ENGLISH_PROMPT, e.getEnglish()));

    // TODO:  put this back!!!

/*
    if (controller.getLanguage().equalsIgnoreCase("English")) {
      column.add(getEntry(e, MEANING, ExerciseFormatter.MEANING_PROMPT, e.getMeaning()));
    }
    else {
      column.add(getEntry(e, ENGLISH, ExerciseFormatter.ENGLISH_PROMPT, e.getEnglish()));
    }
    column.add(getEntry(e, CONTEXT, ExerciseFormatter.CONTEXT, e.getContext()));
    column.add(getEntry(e, CONTEXT_TRANSLATION, ExerciseFormatter.CONTEXT_TRANSLATION, e.getContextTranslation()));
*/

    return column;
  }

  private Heading getComment() {
    boolean isComment = instance.equals(Navigation.COMMENT);
    String columnLabel = isComment ? COMMENT : DEFECT;
    Heading heading = new Heading(4, columnLabel);
    heading.addStyleName("borderBottomQC");
    if (isComment) heading.setWidth("90px");
    return heading;
  }

  public void onResize() {
    super.onResize();
    for (RequiresResize rr : toResize) rr.onResize();
  }


  protected Widget getScoringAudioPanel(final T e) {
    if (!e.hasRefAudio()) {
      return addNoRefAudio(e);
    } else {
      Map<MiniUser, List<AudioAttribute>> malesMap = exercise.getUserMap(true);
      Map<MiniUser, List<AudioAttribute>> femalesMap = exercise.getUserMap(false);

      List<MiniUser> maleUsers = exercise.getSortedUsers(malesMap);
      List<MiniUser> femaleUsers = exercise.getSortedUsers(femalesMap);

      tabs = new ArrayList<>();
      TabPanel tabPanel = new TabPanel();
      addTabsForUsers(e, tabPanel, malesMap, maleUsers);
      addTabsForUsers(e, tabPanel, femalesMap, femaleUsers);

      if (!maleUsers.isEmpty() || !femaleUsers.isEmpty()) {
        tabPanel.selectTab(0);
      }
      return tabPanel;
    }
  }

  private Widget addNoRefAudio(T e) {
    ExerciseAnnotation refAudio = e.getAnnotation(REF_AUDIO);
    Panel column = new FlowPanel();
    column.addStyleName("blockStyle");
    column.add(getCommentWidget(REF_AUDIO, new Label(NO_AUDIO_RECORDED), refAudio, false));
    return column;
  }

  /**
   * For all the users, show a tab for each audio cut they've recorded (regular and slow speed).
   * Special logic included for default users so a QC person can set the gender.
   *
   * @param e
   * @param tabPanel
   * @param malesMap
   * @param maleUsers
   * @see #getScoringAudioPanel
   */
  private void addTabsForUsers(T e, TabPanel tabPanel, Map<MiniUser, List<AudioAttribute>> malesMap, List<MiniUser> maleUsers) {
    if (logger == null) logger = Logger.getLogger("QCNPFExercise");
    int me = controller.getUser();
    UserTitle userTitle = new UserTitle();
    for (MiniUser user : maleUsers) {
      String tabTitle = userTitle.getUserTitle(me, user);

     // logger.info("addTabsForUsers for user " + user + " got " + tabTitle);

      RememberTabAndContent tabAndContent = new RememberTabAndContent(IconType.QUESTION_SIGN, tabTitle, true);
      tabPanel.add(tabAndContent.getTab().asTabLink());
      tabs.add(tabAndContent);

      // TODO : when do we need this???
      tabAndContent.getContent().getElement().getStyle().setMarginRight(70, Style.Unit.PX);

      boolean allHaveBeenPlayed = true;

      List<AudioAttribute> audioAttributes = malesMap.get(user);
      for (AudioAttribute audio : audioAttributes) {
     //   logger.info("addTabsForUsers for " + e.getOldID() + " got " + audio);
        if (!audio.isHasBeenPlayed()) allHaveBeenPlayed = false;
        Pair panelForAudio1 = getPanelForAudio(e, audio);

        Widget panelForAudio = panelForAudio1.entry;
        tabAndContent.addWidget(panelForAudio1.audioPanel);
        toResize.add(panelForAudio1.audioPanel);

        if (user.getID() == BaseUserDAO.DEFAULT_USER_ID) {    // add widgets to mark gender on default audio
          addGenderAssignmentButtons(tabAndContent, audioAttributes, audio, panelForAudio);
        } else {
          tabAndContent.getContent().add(panelForAudio);
        }
        if (audio.isHasBeenPlayed()) {
          audioWasPlayed.add(panelForAudio1.audioPanel);
        }
      }

      if (allHaveBeenPlayed) {
        tabAndContent.getTab().setIcon(IconType.CHECK_SIGN);
      }
    }
  }

  private void addGenderAssignmentButtons(RememberTabAndContent tabAndContent, List<AudioAttribute> audioAttributes,
                                          AudioAttribute audio, Widget panelForAudio) {
    Panel vp = new VerticalPanel();
    Panel hp = new HorizontalPanel();

    final Button next = getNextButton();
    hp.add(getGenderGroup(tabAndContent, audio, next, audioAttributes));
    hp.add(next);
    next.setVisible(false);
    vp.add(hp);
    vp.add(panelForAudio);
    tabAndContent.getContent().add(vp);
  }

  /**
   * @see #addGenderAssignmentButtons(RememberTabAndContent, List, AudioAttribute, Widget)
   * @return
   */
  private Button getNextButton() {
    final Button next = new Button("Next");
    next.setType(ButtonType.SUCCESS);
    next.setIcon(IconType.ARROW_RIGHT);
    next.addStyleName("leftFiveMargin");
    next.addStyleName("topMargin");
    next.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        next.setEnabled(false);
        loadNext();
      }
    });
    return next;
  }

  /**
   * TODO : this may be undone if someone deletes the audio cut - since this essentially masks out old score
   * default audio.
   * <p>
   * TODO:  add list of all audio by this user.
   *
   * @param tabAndContent
   * @param audio
   * @param next
   * @param allByUser
   * @return
   * @see #addTabsForUsers
   */
  private DivWidget getGenderGroup(final RememberTabAndContent tabAndContent, final AudioAttribute audio,
                                   final Button next, final List<AudioAttribute> allByUser) {
    ButtonToolbar w = new ButtonToolbar();
    ButtonGroup buttonGroup = new ButtonGroup();
    buttonGroup.setToggle(ToggleType.RADIO);
    w.add(buttonGroup);

    final Button male = makeGroupButton(buttonGroup, "MALE");

    if (audio.getExid() == -1) {
      audio.setExid(exercise.getID());
    }
    male.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        markGender(DEFAULT_MALE, male, audio, tabAndContent, allByUser, next, true);
      }
    });

    final Button female = makeGroupButton(buttonGroup, "FEMALE");

    female.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        markGender(DEFAULT_FEMALE, female, audio, tabAndContent, allByUser, next, false);
      }
    });

    return w;
  }

  private void markGender(final MiniUser defaultWithGender,
                          final Button offButton,
                          final AudioAttribute audio,
                          final RememberTabAndContent tabAndContent,
                          final List<AudioAttribute> allByUser,
                          final Button next,
                          boolean isMale) {
    offButton.setEnabled(false);

    controller.getQCService().markGender(audio, isMale, new AsyncCallback<Void>() {
      @Override
      public void onFailure(Throwable caught) {
        offButton.setEnabled(true);
      }

      @Override
      public void onSuccess(Void result) {
        audio.setUser(defaultWithGender);
        showGenderChange(offButton, tabAndContent, allByUser, next);
      }
    });
  }

  /**
   * Only show next button if both reg and slow have been set (if both are there)
   *
   * @param onButton
   * @param tabAndContent
   * @param allByUser
   * @param next
   * @see #markGender(MiniUser, Button, AudioAttribute, RememberTabAndContent, List, Button, boolean)
   */
  private void showGenderChange(Button onButton,
                                RememberTabAndContent tabAndContent,
                                List<AudioAttribute> allByUser,
                                Button next) {
    onButton.setEnabled(true);
    tabAndContent.getTab().setHeading(getTabLabelFromAudio(allByUser));

    boolean anyDefault = false;
    for (AudioAttribute audioAttribute : allByUser) {
      if (isGenericDefault(audioAttribute)) {
        anyDefault = true;
        break;
      }
    }
    next.setVisible(!anyDefault);
  }

  private String getTabLabelFromAudio(Collection<AudioAttribute> allByUser) {
    StringBuilder builder = new StringBuilder();
    int i = 0;
    for (AudioAttribute audioAttribute : allByUser) {
      boolean genericDefault = isGenericDefault(audioAttribute);
//      logger.info("getTabLabelFromAudio For " + audioAttribute +
//          "\n\tgot " + audioAttribute.getUser() + " genericDefault " + genericDefault);
      builder.append(genericDefault ?
          GoodwaveExercisePanel.DEFAULT_SPEAKER :
          audioAttribute.isMale() ? MALE : FEMALE);
      if (++i < allByUser.size()) {
        /*if (audioAttribute != last)*/ builder.append("/");
      }
    }
    return builder.toString();
  }

  private boolean isGenericDefault(AudioAttribute audioAttribute) {
    MiniUser user = audioAttribute.getUser();
    return isDefaultNoGenderUser(user);
  }

  private boolean isDefaultNoGenderUser(MiniUser user) {
    return user.getID() == BaseUserDAO.DEFAULT_USER_ID;
  }

  private Button makeGroupButton(ButtonGroup buttonGroup, String title) {
    Button onButton = new Button(title);
    onButton.getElement().setId("MaleFemale" + "_" + title);
    controller.register(onButton, exercise.getID());
    buttonGroup.add(onButton);
    return onButton;
  }

/*  private String getUserTitle(int me, MiniUser user) {
    long id = user.getID();
    if (id == UserDAO.DEFAULT_USER_ID)        return GoodwaveExercisePanel.DEFAULT_SPEAKER;
    else if (id == UserDAO.DEFAULT_MALE_ID)   return "Default Male";
    else if (id == UserDAO.DEFAULT_FEMALE_ID) return "Default Female";
    else return
          (user.getID() == me) ? "by You (" + user.getUserID() + ")" : getUserTitle(user);
  }*/

/*
  private String getUserTitle(MiniUser user) {
    String suffix = user.getAge() < 99 ? " age " + user.getAge() : "";

    String userid = true//user.isAdmin()
        ? " (" + user.getUserID() + ")" : "";
    return (user.isMale() ? MALE : FEMALE) +
        userid +
        suffix;
  }
*/

  /**
   * Keep track of all audio elements -- have they all been played? If so, we can enable the approve & next buttons
   * Also, when all audio for a tab have been played, change tab icon to check
   *
   * @param e
   * @param audio
   * @return both the comment widget and the audio panel
   * @see #addTabsForUsers
   */
  private Pair getPanelForAudio(final T e, final AudioAttribute audio) {
    String audioRef = audio.getAudioRef();
    if (audioRef != null) {
      // if (logger == null) logger = Logger.getLogger("QCNPFExercise");
      //   logger.info("getPanelForAudio path before for " + e.getOldID() + " : " +audioRef + " and " +audio);
      audioRef = CompressedAudio.getPathNoSlashChange(audioRef);   // todo why do we have to do this?
      // logger.info("getPanelForAudio path after  " + audioRef);
    }
    String speed = audio.isRegularSpeed() ? " Regular speed" : " Slow speed";
    final ASRScoringAudioPanel audioPanel = new ASRScoringAudioPanel<T>(audioRef, e.getForeignLanguage(),  e.getTransliteration(), controller,
        controller.getProps().showSpectrogram(), 70, speed, e, instance);
    audioPanel.setShowColor(true);
    audioPanel.getElement().setId("ASRScoringAudioPanel");
    audioPanel.addPlayListener(new PlayListener() {
      @Override
      public void playStarted() {
        audioWasPlayed.add(audioPanel);
        logger.info("getPanelForAudio playing audio " + audio.getAudioRef() + " has " + tabs.size() + " tabs, now " + audioWasPlayed.size() + " played");
        //if (audioWasPlayed.size() == toResize.size()) {
        // all components played
        setApproveButtonState();
        // }
        for (RememberTabAndContent tabAndContent : tabs) {
          tabAndContent.checkAllPlayed(audioWasPlayed);
        }
        controller.logEvent(audioPanel, "qcPlayAudio", e.getID(), audio.getAudioRef());
      }

      @Override
      public void playStopped() {
      }
    });
    ExerciseAnnotation audioAnnotation = e.getAnnotation(audio.getAudioRef());

    Widget entry = getCommentWidget(audio.getAudioRef(), audioPanel, audioAnnotation, false);
    return new Pair(entry, audioPanel);
  }

  private static class Pair {
    final Widget entry;
    final AudioPanel audioPanel;

    Pair(Widget entry, AudioPanel audioPanel) {
      this.entry = entry;
      this.audioPanel = audioPanel;
    }
  }

  private Widget getEntry(T e, final String field, final String label, String value) {
    return getEntry(field, label, value, e.getAnnotation(field));
  }

  private Widget getEntry(final String field, final String label, String value, ExerciseAnnotation annotation) {
    return getCommentWidget(field, getContentWidget(label, value, true), annotation, true);
  }

  /**
   * @param field
   * @param annotation
   * @return
   */
  private Widget getCommentWidget(final String field, Widget content, ExerciseAnnotation annotation, boolean addLeftMargin) {
    final FocusWidget commentEntry = makeCommentEntry(field, annotation);

    boolean alreadyMarkedCorrect = annotation == null || annotation.getStatus() == null || annotation.getStatus().equals("correct");
    if (!alreadyMarkedCorrect) {
      incorrectFields.add(field);
    }
    final Panel commentRow = new FlowPanel();
    commentRow.getElement().setId("QCNPFExercise_commentRow_" + field);

    final Widget qcCol = getQCCheckBox(field, commentEntry, alreadyMarkedCorrect, commentRow);
    qcCol.getElement().setId("QCNPFExercise_qcCol_" + field);

    populateCommentRow(commentEntry, alreadyMarkedCorrect, commentRow);

    // comment to left, content to right

    Panel row = new FlowPanel();
    row.getElement().setId("QCNPFExercise_row_" + field);

    row.addStyleName("trueInlineStyle");
    qcCol.addStyleName("floatLeft");
    row.add(qcCol);
    if (addLeftMargin) {
      content.getElement().getStyle().setMarginLeft(80, Style.Unit.PX);
    }
    row.add(content);

    Panel rowContainer = new FlowPanel();
    rowContainer.getElement().setId("QCNPFExercise_rowContainer_" + field);
    rowContainer.addStyleName("topFiveMargin");
    rowContainer.addStyleName("blockStyle");
    rowContainer.add(row);
    rowContainer.add(commentRow);

    return rowContainer;
  }

  private Widget getQCCheckBox(String field, FocusWidget commentEntry, boolean alreadyMarkedCorrect, Panel commentRow) {
    return makeCheckBox(field, commentRow, commentEntry, alreadyMarkedCorrect);
  }

  /**
   * @param commentEntry
   * @param alreadyMarkedCorrect
   * @param commentRow
   * @see #getCommentWidget
   */
  private void populateCommentRow(FocusWidget commentEntry, boolean alreadyMarkedCorrect, Panel commentRow) {
    commentRow.setVisible(!alreadyMarkedCorrect);
    commentRow.add(getCommentLabel());
    commentRow.add(commentEntry);
  }

  /**
   * @param field
   * @param annotation
   * @return
   * @see #getCommentWidget
   */
  private FocusWidget makeCommentEntry(final String field, ExerciseAnnotation annotation) {
    final TextBox commentEntry = new TextBox();
    commentEntry.getElement().setId("QCNPFExercise_Comment_TextBox_" + field);
    commentEntry.addStyleName("topFiveMargin");
    if (annotation != null && annotation.isDefect()) {
      commentEntry.setText(annotation.getComment());
    }
    commentEntry.setVisibleLength(100);
    commentEntry.setWidth("400px");

    commentEntry.addStyleName("leftFiveMargin");
    commentEntry.addBlurHandler(new BlurHandler() {
      @Override
      public void onBlur(BlurEvent event) {
        addIncorrectComment(sanitize(commentEntry.getText()), field);
      }
    });
    addTooltip(commentEntry, COMMENT_TOOLTIP);
    return commentEntry;
  }

  private String sanitize(String text) {
    return SimpleHtmlSanitizer.sanitizeHtml(text).asString();
  }

  /**
   * @param field
   * @param commentRow
   * @param commentEntry
   * @param alreadyMarkedCorrect
   * @return
   * @see #getQCCheckBox
   */
  private CheckBox makeCheckBox(final String field, final Panel commentRow, final FocusWidget commentEntry,
                                boolean alreadyMarkedCorrect) {
    boolean isComment = instance.equals(Navigation.COMMENT);

    final CheckBox checkBox = new CheckBox("");
    checkBox.getElement().setId("CheckBox_" + field);
    checkBox.addStyleName(isComment ? "wideCenteredRadio" : "centeredRadio");
    checkBox.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        checkBoxWasClicked(checkBox.getValue(), field, commentRow, commentEntry);
      }
    });
    checkBox.setValue(!alreadyMarkedCorrect);
    if (!isComment) {
      addTooltip(checkBox, CHECKBOX_TOOLTIP);
    }
    return checkBox;
  }

  /**
   * @param isIncorrect
   * @param field
   * @param commentRow
   * @param commentEntry
   * @see #makeCheckBox
   */
  private void checkBoxWasClicked(boolean isIncorrect, String field, Panel commentRow, FocusWidget commentEntry) {
    commentRow.setVisible(isIncorrect);
    commentEntry.setFocus(isIncorrect);

    if (isIncorrect) {
      incorrectFields.add(field);
    } else {
      incorrectFields.remove(field);
      addCorrectComment(field);
    }

    //System.out.println("checkBoxWasClicked : instance = '" +instance +"'");
    if (isCourseContent()) {
      //String id = ;
      // System.out.println("\tcheckBoxWasClicked : instance = '" +instance +"'");
      //if (instance.equalsIgnoreCase(Navigation.CLASSROOM)) {
      STATE state = incorrectFields.isEmpty() ? STATE.UNSET : STATE.DEFECT;
      exercise.setState(state);
      listContainer.setState(exercise.getID(), state);
      //   System.out.println("\tcheckBoxWasClicked : state now = '" +state +"'");

      listContainer.redraw();
      //  }
      //  else {
      //    System.out.println("\tcheckBoxWasClicked : ignoring instance = '" +instance +"'");
      //   }

      setApproveButtonState();
      markReviewed(exercise);
      LangTest.EVENT_BUS.fireEvent(new DefectEvent(instance));
    }
  }

  /**
   * @see #checkBoxWasClicked(boolean, String, com.google.gwt.user.client.ui.Panel, com.google.gwt.user.client.ui.FocusWidget)
   */
  private void setApproveButtonState() {
    boolean allCorrect = incorrectFields.isEmpty();
    boolean allPlayed  = audioWasPlayed.size() == toResize.size();
    //System.out.println("\tsetApproveButtonState : allPlayed= '" +allPlayed +"' allCorrect " + allCorrect + " audio played " + audioWasPlayed.size() + " total " + toResize.size());

    String tooltipText = !allPlayed ? "Not all audio has been reviewed" : allCorrect ? APPROVED_BUTTON_TOOLTIP : APPROVED_BUTTON_TOOLTIP2;
    if (approvedButton != null) {   // comment tab doesn't have it...!
      approvedButton.setEnabled(allCorrect && allPlayed);
      approvedTooltip.setText(tooltipText);
      approvedTooltip.reconfigure();
    }

    if (navigationHelper != null) { // this called before nav helper exists
      navigationHelper.enableNextButton(allPlayed);
      nextTooltip.setText(tooltipText);
      nextTooltip.reconfigure();
    }
  }
}
