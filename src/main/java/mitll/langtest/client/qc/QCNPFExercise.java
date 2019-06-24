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

package mitll.langtest.client.qc;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.CheckBox;
import com.github.gwtbootstrap.client.ui.Label;
import com.github.gwtbootstrap.client.ui.TabPanel;
import com.github.gwtbootstrap.client.ui.TextBox;
import com.github.gwtbootstrap.client.ui.*;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.google.gwt.dom.client.Style;
import com.google.gwt.safehtml.shared.SimpleHtmlSanitizer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.*;
import mitll.langtest.client.custom.INavigation;
import mitll.langtest.client.custom.tabs.RememberTabAndContent;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.NavigationHelper;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.client.scoring.ASRScoringAudioPanel;
import mitll.langtest.client.scoring.AudioPanel;
import mitll.langtest.client.scoring.ExerciseOptions;
import mitll.langtest.client.scoring.GoodwaveExercisePanel;
import mitll.langtest.client.services.LangTestDatabaseAsync;
import mitll.langtest.client.sound.CompressedAudio;
import mitll.langtest.client.sound.PlayListener;
import mitll.langtest.server.database.user.BaseUserDAO;
import mitll.langtest.shared.ExerciseFormatter;
import mitll.langtest.shared.exercise.*;
import mitll.langtest.shared.user.MiniUser;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.logging.Logger;

public class QCNPFExercise<T extends ClientExercise> extends GoodwaveExercisePanel<T> {
  private Logger logger = Logger.getLogger("QCNPFExercise");

  private static final String CLICK_TO_INDICATE_ITEM_HAS_BEEN_REVIEWED = "Click to indicate item has been inspected.";
  private static final String UNINSPECTED_TOOLTIP = "Item has uninspected audio.";
  private static final String VOCABULARY = "Vocabulary:";

  private static final String DEFECT = "Defect?";
  public static final String FOREIGN_LANGUAGE = "foreignLanguage";
  public static final String TRANSLITERATION = "transliteration";
  public static final String ALTFL = "alternate";
  public static final String ENGLISH = "english";
  public static final String MEANING = "meaning";

  public static final String ALTCONTEXT = "altcontext";

  private static final String REF_AUDIO = "refAudio";
  /**
   * @see #addMarkInspected
   */
  private static final String APPROVED = "Mark Inspected";
  private static final String NO_AUDIO_RECORDED = "No Audio Recorded.";

  private static final String COMMENT_TOOLTIP = "Comments are optional.";
  private static final String CHECKBOX_TOOLTIP = "Check to indicate this field has a defect.";
  private static final String APPROVED_BUTTON_TOOLTIP = "Indicate item has no defects.";
  private static final String APPROVED_BUTTON_TOOLTIP2 = "Item has been marked with a defect";

  //  private static final int DEFAULT_MALE_ID = -2;
//  private static final int DEFAULT_FEMALE_ID = -3;
  private static final String MALE = "Male";
  /**
   * TODO : why ?
   */
  // private static final MiniUser DEFAULT_MALE = new MiniUser(DEFAULT_MALE_ID, 30, true, MiniUser.Gender.Male, MALE, false);
  private static final String FEMALE = "Female";
  // private static final MiniUser DEFAULT_FEMALE = new MiniUser(DEFAULT_FEMALE_ID, 30, false, MiniUser.Gender.Female, FEMALE, false);

  private Set<String> incorrectFields;
  /**
   * @see #initAudioWasPlayed
   */
  private List<RequiresResize> toResize;
  private Set<Widget> audioWasPlayed;
  private final ListInterface<?, ?> listContainer;
  private Button approvedButton;
  private Tooltip approvedTooltip;
  private Tooltip nextTooltip;
  private List<RememberTabAndContent> tabs;

  /**
   * @param e
   * @param controller
   * @param listContainer
   * @param instance
   * @see mitll.langtest.client.custom.content.NPFHelper#getFactory
   */
  public QCNPFExercise(T e,
                       ExerciseController controller,
                       ListInterface<?, ?> listContainer,
                       INavigation.VIEWS instance) {
    super(e, controller, listContainer, new ExerciseOptions(instance));
    this.listContainer = listContainer;
  }

  @Override
  protected void makeScorePanel(T e) {
    if (audioWasPlayed == null) {
      initAudioWasPlayed();
    }
  }

  private void initAudioWasPlayed() {
    audioWasPlayed = new HashSet<>();
    toResize = new ArrayList<>();
    incorrectFields = new HashSet<>();
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

  /**
   * @param controller
   * @param listContainer
   * @param addKeyHandler
   * @param includeListButtons
   * @return
   * @see mitll.langtest.client.scoring.GoodwaveExercisePanel#GoodwaveExercisePanel
   */
  protected NavigationHelper getNavigationHelper(ExerciseController controller,
                                                 final ListInterface<?, ?> listContainer,
                                                 boolean addKeyHandler, boolean includeListButtons) {
    NavigationHelper navHelper = new NavigationHelper(exercise, controller,
        (controller1, completedExercise) -> nextWasPressed(listContainer, completedExercise),
        listContainer, addKeyHandler) {
      /**
       * So only allow next button when all audio has been played
       * @param exercise
       */
      @Override
      public void enableNext(HasID exercise) {
        if (audioWasPlayed == null) {
          initAudioWasPlayed();
        }
        boolean allPlayed = allAudioHasBeenPlayed();
        next.setEnabled(allPlayed);
      }
    };

    //if (logger == null) logger = Logger.getLogger("QCNPFExercise");

    //logger.info("audio played   " + audioWasPlayed.size());
    //logger.info("audio to play  " + toResize.size());
    //setNextTooltip(navHelper);

    // if (getInstance().isFix()) {
    approvedButton = addMarkInspected(navHelper);
    //}

    setApproveButtonState();
    return navHelper;
  }

  private void setNextTooltip(NavigationHelper navHelper) {
    nextTooltip = addTooltip(navHelper.getNext(), allAudioHasBeenPlayed() ?
        CLICK_TO_INDICATE_ITEM_HAS_BEEN_REVIEWED :
        UNINSPECTED_TOOLTIP);
  }

  private boolean allAudioHasBeenPlayed() {
    return audioWasPlayed.size() == toResize.size();
  }

  /**
   * @param buttonRow
   * @return
   * @see GoodwaveExercisePanel#getNavigationHelper(ExerciseController, ListInterface, boolean, boolean)
   */
  private Button addMarkInspected(Panel buttonRow) {
    Button approved = new Button(APPROVED);
    approved.getElement().setId("approve");
    approved.addStyleName("leftFiveMargin");
    buttonRow.add(approved);
    approved.setType(ButtonType.PRIMARY);
    approved.addClickHandler(event -> markReviewedAndClick(exercise));
    approvedTooltip = addTooltip(approved, APPROVED_BUTTON_TOOLTIP);
    return approved;
  }

  @Override
  protected void nextWasPressed(ListInterface<?, ?> listContainer, HasID completedExercise) {
    super.nextWasPressed(listContainer, completedExercise);
    // markReviewed(listContainer, completedExercise);
    if (hasNoDefects()) {
    } else {
      markReviewed(completedExercise);
    }
    navigationHelper.clickNext(controller, completedExercise);
  }

  private boolean hasNoDefects() {
    return incorrectFields.isEmpty();
  }

  /**
   * @param completedExercise
   * @seex #nextWasPressed
   * @see #addMarkInspected
   */
  private void markReviewedAndClick(HasID completedExercise) {
    markReviewed(completedExercise);
//    boolean allCorrect = incorrectFields.isEmpty();
//    int id = completedExercise.getID();
    // logger.info("markReviewed : mark " + id + " = " + allCorrect + " incorrect fields " + incorrectFields.size() + " : " + incorrectFields);
//    listContainer.setState(id, allCorrect ? STATE.APPROVED : STATE.DEFECT);
//    listContainer.redraw();
    navigationHelper.clickNext(controller, completedExercise);
  }

  /**
   * @param completedExercise
   * @see #checkBoxWasClicked(boolean, String, com.google.gwt.user.client.ui.Panel, com.google.gwt.user.client.ui.FocusWidget)
   * @see #markReviewed
   */
  private void markReviewed(final HasID completedExercise) {
    boolean allCorrect = hasNoDefects();
    logger.info("markReviewed : exercise " + completedExercise.getID() + " allCorrect " + allCorrect);

    controller.getQCService().markReviewed(completedExercise.getID(), allCorrect,
        new AsyncCallback<Void>() {
          @Override
          public void onFailure(Throwable caught) {
            controller.handleNonFatalError("marking reviewed on exercise", caught);
          }

          @Override
          public void onSuccess(Void result) {
            logger.info("\tmarkReviewed.onSuccess exercise " + completedExercise.getID() + " marked reviewed!");
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
    column.addStyleName("floatLeftAndClear");
    column.setWidth("100%");

    Panel row = new FlowPanel();
    row.add(getComment());

    column.add(row);

    column.add(getEntry(e, FOREIGN_LANGUAGE, VOCABULARY, e.getFLToShow(), getExerciseID()));
    column.add(getEntry(e, TRANSLITERATION, ExerciseFormatter.TRANSLITERATION, e.getTransliteration(), getExerciseID()));
    column.add(getEntry(e, ENGLISH, ExerciseFormatter.ENGLISH_PROMPT, e.getEnglish(), getExerciseID()));

    if (e.hasContext()) {
      ClientExercise next = e.getDirectlyRelated().iterator().next();

      int id = next.getID();
      column.add(getEntry(next, FOREIGN_LANGUAGE, ExerciseFormatter.CONTEXT, next.getForeignLanguage(), id));
      column.add(getEntry(next, ENGLISH, ExerciseFormatter.CONTEXT_TRANSLATION, next.getEnglish(), id));
    }

    return column;
  }

  private Heading getComment() {
    // boolean isComment = getInstance().isQC();
    if (logger == null) logger = Logger.getLogger("QCNPFExercise");
//    logger.info("inst " + getInstance());
//    logger.info("isQC " + getInstance().isQC());

    Heading heading = new Heading(4, DEFECT);
    heading.addStyleName("borderBottomQC");
    // if (isComment) heading.setWidth("90px");
    return heading;
  }

  public void onResize() {
    super.onResize();
    for (RequiresResize rr : toResize) rr.onResize();
  }

  /**
   * @param e
   * @return
   * @see #getAudioPanel
   */
  protected Widget getScoringAudioPanel(final T e) {
    if (logger == null) logger = Logger.getLogger("QCNPFExercise");

    if (!e.hasRefAudio()) {
      //    logger.info("no : audio attr on " + e.getID() + " " + e.getAudioAttributes());
      return addNoRefAudio(e);
    } else {
      boolean context = e.isContext();

      ClientExercise toShow = this.exercise;
      if (options.getInstance() == INavigation.VIEWS.QC_SENTENCES) {
        context = true;
        if (this.exercise.getDirectlyRelated().isEmpty()) {
          logger.info("no context sentences for " + e.getID() + " " + e.getEnglish());
        } else {
          toShow = this.exercise.getDirectlyRelated().iterator().next();
        }
      }
      //logger.info("getScoringAudioPanel : audio attr on " + e.getID() + " " + e.getAudioAttributes() + " context " + context);
      Map<MiniUser, List<AudioAttribute>> malesMap = toShow.getUserMap(true, context);
      Map<MiniUser, List<AudioAttribute>> femalesMap = toShow.getUserMap(false, context);
      //logger.info("malesMap : "+malesMap.size());
      //logger.info("femalesMap : "+femalesMap.size());

      List<MiniUser> maleUsers = toShow.getSortedUsers(malesMap);
      List<MiniUser> femaleUsers = toShow.getSortedUsers(femalesMap);

//      maleUsers.forEach(u->logger.info(u.getUserID() + " male? " +u.getRealGender()));
//      femaleUsers.forEach(u->logger.info(u.getUserID() + " female? " +u.getRealGender()));

      tabs = new ArrayList<>();

      TabPanel tabPanel = new TabPanel();
      addTabsForUsers(toShow, tabPanel, malesMap, maleUsers);
      addTabsForUsers(toShow, tabPanel, femalesMap, femaleUsers);

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
    column.add(getCommentWidget(REF_AUDIO, new Label(NO_AUDIO_RECORDED), refAudio, e.getID()));
    return column;
  }

  /**
   * For all the users, show a tab for each audio cut they've recorded (regular and slow speed).
   * Special logic included for default users so a QC person can set the gender.
   *
   * @param exerciseWithAudio
   * @param tabPanel
   * @param malesMap
   * @param maleUsers
   * @see #getScoringAudioPanel
   */
  private void addTabsForUsers(ClientExercise exerciseWithAudio, TabPanel tabPanel, Map<MiniUser, List<AudioAttribute>> malesMap, List<MiniUser> maleUsers) {
    if (logger == null) logger = Logger.getLogger("QCNPFExercise");
    int me = controller.getUser();
    UserTitle userTitle = new UserTitle();
    for (MiniUser user : maleUsers) {
      String tabTitle = userTitle.getUserTitle(me, user);

      if (false) logger.info("addTabsForUsers for user " + user + " got " + tabTitle);

      RememberTabAndContent tabAndContent = new RememberTabAndContent(IconType.QUESTION_SIGN, tabTitle, true);
      tabPanel.add(tabAndContent.getTab().asTabLink());
      tabs.add(tabAndContent);

      // TODO : when do we need this???
      tabAndContent.getContent().getElement().getStyle().setMarginRight(70, Style.Unit.PX);

      boolean allHaveBeenPlayed = true;

      List<AudioAttribute> audioAttributes = malesMap.get(user);
      for (AudioAttribute audio : audioAttributes) {
        //    logger.info("addTabsForUsers for " + exerciseWithAudio.getID() + " got " + audio);
        if (!audio.isHasBeenPlayed()) allHaveBeenPlayed = false;
        Pair panelForAudio1 = getPanelForAudio(exerciseWithAudio, audio);

        Widget panelForAudio = panelForAudio1.entry;
        tabAndContent.addWidget(panelForAudio1.audioPanel);
        toResize.add(panelForAudio1.audioPanel);

//        if (user.getID() == BaseUserDAO.DEFAULT_USER_ID) {    // add widgets to mark gender on default audio
//          addGenderAssignmentButtons(tabAndContent, audioAttributes, audio, panelForAudio);
//        } else {
        tabAndContent.getContent().add(panelForAudio);
//        }
        if (audio.isHasBeenPlayed()) {
          audioWasPlayed.add(panelForAudio1.audioPanel);
        }
      }

      if (allHaveBeenPlayed) {
        tabAndContent.getTab().setIcon(IconType.CHECK_SIGN);
      }
    }
    setNextTooltip(navigationHelper);
    setApproveButtonState();
    navigationHelper.enableNext(exercise);
  }

 /* private void addGenderAssignmentButtons(RememberTabAndContent tabAndContent, List<AudioAttribute> audioAttributes,
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
  }*/

  /**
   * @return
   * @see #addGenderAssignmentButtons(RememberTabAndContent, List, AudioAttribute, Widget)
   */
/*  private Button getNextButton() {
    final Button next = new Button("Next");
    next.setType(ButtonType.SUCCESS);
    next.setIcon(IconType.ARROW_RIGHT);
    next.addStyleName("leftFiveMargin");
    next.addStyleName("topMargin");
    next.addClickHandler(event -> {
      next.setEnabled(false);
      loadNext();
    });
    return next;
  }*/

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
 /* private DivWidget getGenderGroup(final RememberTabAndContent tabAndContent, final AudioAttribute audio,
                                   final Button next, final List<AudioAttribute> allByUser) {
    ButtonToolbar w = new ButtonToolbar();
    ButtonGroup buttonGroup = new ButtonGroup();
    buttonGroup.setToggle(ToggleType.RADIO);
    w.add(buttonGroup);

    final Button male = makeGroupButton(buttonGroup, "MALE");

    if (audio.getExid() == -1) {
      audio.setExid(getExerciseID());
    }
    male.addClickHandler(event -> markGender(DEFAULT_MALE, male, audio, tabAndContent, allByUser, next, true));

    final Button female = makeGroupButton(buttonGroup, "FEMALE");

    female.addClickHandler(event -> markGender(DEFAULT_FEMALE, female, audio, tabAndContent, allByUser, next, false));

    return w;
  }*/

 /* private void markGender(final MiniUser defaultWithGender,
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
        controller.handleNonFatalError("marking gender on audio", caught);
      }

      @Override
      public void onSuccess(Void result) {
        audio.setUser(defaultWithGender);
        showGenderChange(offButton, tabAndContent, allByUser, next);
      }
    });
  }
*/

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
        /*if (audioAttribute != last)*/
        builder.append("/");
      }
    }
    return builder.toString();
  }

  private boolean isGenericDefault(AudioAttribute audioAttribute) {
    return isDefaultNoGenderUser(audioAttribute.getUserid());
  }

  /**
   * TODO: this is probably not right...
   *
   * @param userID
   * @return
   */
  private boolean isDefaultNoGenderUser(int userID) {
    return userID == BaseUserDAO.DEFAULT_USER_ID;
  }

  private Button makeGroupButton(ButtonGroup buttonGroup, String title) {
    Button onButton = new Button(title);
    onButton.getElement().setId("MaleFemale" + "_" + title);
    controller.register(onButton, getExerciseID());
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
  private Pair getPanelForAudio(final ClientExercise e, final AudioAttribute audio) {
    String audioRef = audio.getAudioRef();
    if (audioRef != null) {
      // if (logger == null) logger = Logger.getLogger("QCNPFExercise");
      //   logger.info("getPanelForAudio path before for " + e.getOldID() + " : " +audioRef + " and " +audio);
      audioRef = CompressedAudio.getPathNoSlashChange(audioRef);   // todo why do we have to do this?
      // logger.info("getPanelForAudio path after  " + audioRef);
    }

    String s = audio.getAudioType().toString();

    String speed = audio.isRegularSpeed() ? " Regular speed" : audio.isSlow() ? " Slow speed" : s + " audio";

    final ASRScoringAudioPanel audioPanel = new ASRScoringAudioPanel<>(audioRef, e.getFLToShow(), e.getTransliteration(), controller,
        controller.getProps().showSpectrogram(), 70, speed, e);

    audioPanel.setShowColor(true);
    audioPanel.getElement().setId("ASRScoringAudioPanel");
    audioPanel.addPlayListener(new PlayListener() {
      @Override
      public void playStarted() {
        audioWasPlayed.add(audioPanel);

        //   logger.info("getPanelForAudio playing audio " + audio.getAudioRef() + " has " + tabs.size() + " tabs, now " + audioWasPlayed.size() + " played");

        setApproveButtonState();

        for (RememberTabAndContent tabAndContent : tabs) {
          tabAndContent.checkAllPlayed(audioWasPlayed);
        }
        controller.logEvent(audioPanel, "qcPlayAudio", e.getID(), audio.getAudioRef());
      }

      @Override
      public void playStopped() {
      }
    });

//    logger.info("getPanelForAudio : comment widget for " + e.getID() + " " + e.getEnglish() + " " + e.getForeignLanguage() + " context " + e.isContext());
    return
        new Pair(getCommentWidget(audio.getAudioRef(), audioPanel, e.getAnnotation(audio.getAudioRef()), e.getID()), audioPanel);
  }

  private static class Pair {
    final Widget entry;
    final AudioPanel audioPanel;

    Pair(Widget entry, AudioPanel audioPanel) {
      this.entry = entry;
      this.audioPanel = audioPanel;
    }
  }

  /**
   * @param e
   * @param field
   * @param label
   * @param value
   * @param exerciseID
   * @return
   */
  private Widget getEntry(AnnotationExercise e, final String field, final String label, String value, int exerciseID) {
    ExerciseAnnotation annotation = e.getAnnotation(field);
    //  if (logger == null) logger = Logger.getLogger("QCNPFExercise");
    // logger.info("getEntry exid " + exerciseID + " : " + e.getID() + " field " + field + " label " + label + " value " + value +  " anno " + annotation);
    return getEntry(field, label, value, annotation, exerciseID);
  }

  private Widget getEntry(final String field, final String label, String value, ExerciseAnnotation annotation, int exerciseID) {
    return getCommentWidget(field, getContentWidget(label, value, true), annotation, exerciseID);
  }

  /**
   * @param field
   * @param annotation
   * @param exerciseID
   * @return
   * @see #getPanelForAudio
   * @see #getEntry(String, String, String, ExerciseAnnotation, int)
   */
  private Widget getCommentWidget(final String field, Widget content, ExerciseAnnotation annotation, int exerciseID) {
    final FocusWidget commentEntry = makeCommentEntry(field, annotation, exerciseID);

    boolean alreadyMarkedCorrect = annotation == null || annotation.getStatus() == null || annotation.getStatus().equals("correct");
    if (!alreadyMarkedCorrect) {
      incorrectFields.add(field);
    }
    final Panel commentRow = new FlowPanel();
    commentRow.getElement().setId("QCNPFExercise_commentRow_" + field);

    final Widget qcCol = getQCCheckBox(field, commentEntry, alreadyMarkedCorrect, commentRow);

    populateCommentRow(commentEntry, alreadyMarkedCorrect, commentRow);

    // comment to left, content to right
    //Panel rowWithCheckBox = getRowWithCheckbox(field, content, qcCol);
    return getRowContainer(field, commentRow, getRowWithCheckbox(field, content, qcCol));
  }

  @NotNull
  private Panel getRowWithCheckbox(String field, Widget content, Widget qcCol) {
    Panel rowWithCheckBox = new DivWidget();
    rowWithCheckBox.getElement().setId("QCNPFExercise_row_" + field);

    //  rowWithCheckBox.addStyleName("trueInlineStyle");
    rowWithCheckBox.addStyleName("inlineFlex");
    rowWithCheckBox.add(qcCol);
//    if (addLeftMargin) {
//      content.getElement().getStyle().setMarginLeft(80, Style.Unit.PX);
//    }
    rowWithCheckBox.add(content);
    return rowWithCheckBox;
  }

  @NotNull
  private Panel getRowContainer(String field, Panel commentRow, Panel row) {
    Panel rowContainer = new FlowPanel();
    rowContainer.getElement().setId("QCNPFExercise_rowContainer_" + field);
    rowContainer.addStyleName("topFiveMargin");
    // rowContainer.addStyleName("blockStyle");
    rowContainer.add(row);
    rowContainer.add(commentRow);
    return rowContainer;
  }

  private Widget getQCCheckBox(String field, FocusWidget commentEntry, boolean alreadyMarkedCorrect, Panel commentRow) {
    CheckBox checkBox = makeCheckBox(field, commentRow, commentEntry, alreadyMarkedCorrect);
    checkBox.addStyleName("floatLeftAndClear");
    checkBox.getElement().setId("QCNPFExercise_qcCol_" + field);

    return checkBox;
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
   * @param exerciseID
   * @return
   * @see #getCommentWidget
   */
  private FocusWidget makeCommentEntry(final String field, ExerciseAnnotation annotation, int exerciseID) {
    final TextBox commentEntry = new TextBox();
    commentEntry.getElement().setId("QCNPFExercise_Comment_TextBox_" + field);
    commentEntry.addStyleName("topFiveMargin");
    if (annotation != null && annotation.isDefect()) {
      commentEntry.setText(annotation.getComment());
    }
    commentEntry.setVisibleLength(100);
    commentEntry.setWidth("400px");

    commentEntry.addStyleName("leftFiveMargin");


    commentEntry.addBlurHandler(event -> {
//      if (false) {
//        logger.info("makeCommentEntry comment on " +
//            "\n\tex      " + exerciseID +
//            "\n\tfield   " + field +
//            "\n\tcomment " + commentEntry.getText());
//      }

      addIncorrectComment(exerciseID, field, sanitize(commentEntry.getText()));
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
    boolean isComment = getInstance().isQC();

    final CheckBox checkBox = new CheckBox("");
    checkBox.getElement().setId("CheckBox_" + field);
    checkBox.addStyleName(isComment ? "wideCenteredRadio" : "centeredRadio");
    checkBox.addClickHandler(event -> checkBoxWasClicked(checkBox.getValue(), field, commentRow, commentEntry));
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
      addCorrectComment(getExerciseID(), field);
    }

    STATE state = hasNoDefects() ? STATE.UNSET : STATE.DEFECT;
//    exercise.setState(state);
//    listContainer.setState(getExerciseID(), state);
    listContainer.redraw();
    setApproveButtonState();
    //markReviewed(exercise);
  }

  private int getExerciseID() {
    return exercise.getID();
  }

  /**
   * @see #checkBoxWasClicked(boolean, String, com.google.gwt.user.client.ui.Panel, com.google.gwt.user.client.ui.FocusWidget)
   */
  private void setApproveButtonState() {
    boolean allPlayed = allAudioHasBeenPlayed();
    //System.out.println("\tsetApproveButtonState : allPlayed= '" +allPlayed +"' allCorrect " + allCorrect + " audio played " + audioWasPlayed.size() + " total " + toResize.size());

    String tooltipText = !allPlayed ? "Not all audio has been reviewed" : hasNoDefects() ? APPROVED_BUTTON_TOOLTIP : APPROVED_BUTTON_TOOLTIP2;
    if (approvedButton != null) {   // comment tab doesn't have it...!
      approvedButton.setEnabled(allPlayed);
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
