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

package mitll.langtest.client.custom.dialog;

import com.github.gwtbootstrap.client.ui.*;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.storage.client.Storage;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.LangTest;
import mitll.langtest.client.custom.INavigation;
import mitll.langtest.client.custom.tabs.RememberTabAndContent;
import mitll.langtest.client.exercise.*;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.client.list.PagingExerciseList;
import mitll.langtest.client.scoring.ASRScoringAudioPanel;
import mitll.langtest.client.sound.CompressedAudio;
import mitll.langtest.client.sound.PlayListener;
import mitll.langtest.shared.answer.AudioAnswer;
import mitll.langtest.shared.answer.AudioType;
import mitll.langtest.shared.exercise.*;
import mitll.langtest.shared.user.MiniUser;

import java.util.*;
import java.util.logging.Logger;

import static mitll.langtest.shared.answer.AudioType.REGULAR;
import static mitll.langtest.shared.answer.AudioType.SLOW;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 3/28/2014.
 */
public class ReviewEditableExercise<T extends CommonShell, U extends ClientExercise> extends EditableExerciseDialog<T, U> {
  private static final String MARKING_AUDIO_DEFECT = "marking audio defect";
  private final Logger logger = Logger.getLogger("ReviewEditableExercise");

  private static final String YOUR_RECORDING = "Your Recording";

  /**
   * @see #getDeleteButton(String, ClickHandler)
   */
  private static final String DELETE_AUDIO = "Delete Audio";
  private static final String DELETE_THIS_AUDIO_CUT = "Delete this audio cut.";
  private static final String TRANSLITERATION = "Transliteration";

  private static final String MARK_FIXED_TOOLTIP = "Mark item as fixed, removed defective audio, and remove item from the review list.";

  /**
   * @see #makeFixedButton
   */
  private static final String FIXED = "Mark Fixed";
  // private static final String DUPLICATE = "Duplicate";
  /**
   * @seex #getRemove
   */
/*  private static final String DELETE = "Delete";
  private static final String DELETE_THIS_ITEM = "Delete this item.";
  private static final String ARE_YOU_SURE = "Are you sure?";
  private static final String REALLY_DELETE_ITEM = "Really delete whole item and all audio cuts?";
  private static final List<String> MSGS = Collections.singletonList(REALLY_DELETE_ITEM);
  */
//  private static final String COPY_THIS_ITEM = "Copy this item.";
  private static final String REGULAR_SPEED = " Regular speed";
  private static final String SLOW_SPEED = " Slow speed";
  // private static final int DELAY_MILLIS = 5000;

  /**
   * @see #makeAudioRow
   */
  private static final String ADD_AUDIO = "Add audio";

  private final Set<Widget> audioWasPlayed = new HashSet<>();
  private final PagingExerciseList<T, U> exerciseList;

  private List<RememberTabAndContent> tabs;

  /**
   * @seex #checkForForeignChange
   * @see #getKeepAudio
   */
  private final CheckBox keepAudio = new CheckBox("Keep Audio even if text changes");

  /**
   * @param changedUserExercise
   * @param originalList
   * @param exerciseList
   * @paramx predefinedContent   - this should be a reference to the Learn tab exercise list, but it's not getting set.
   * @see mitll.langtest.client.custom.content.ReviewItemHelper#doInternalLayout
   */
  public ReviewEditableExercise(ExerciseController controller,
                                U changedUserExercise,

                                int originalList,
                                PagingExerciseList<T, U> exerciseList,
                                INavigation.VIEWS instanceName) {
    super(controller,
        changedUserExercise,
        originalList,
        exerciseList,
        instanceName);
    this.exerciseList = exerciseList;
  }

  public Panel addFields(final ListInterface<T, U> listInterface, final Panel toAddTo) {
    Panel widgets = super.addFields(listInterface, toAddTo);
    english.box.setEnabled(false);
    foreignLang.box.setEnabled(false);
    translit.box.setEnabled(false);
    context.box.setEnabled(false);
    contextTrans.box.setEnabled(false);
    return widgets;
  }

  private int currentTab = 0;

  /**
   * @return
   * @see #addFields
   */
  @Override
  protected Panel makeAudioRow() {
    AudioRefExercise audioAttributeExercise = newUserExercise;

    tabs = new ArrayList<>();

    TabPanel tabPanel = new TabPanel();
    tabLinks.clear();

    Collection<AudioAttribute> maleDisplayed = getDisplayedAudio(audioAttributeExercise, true);
    Collection<AudioAttribute> femaleDisplayed = getDisplayedAudio(audioAttributeExercise, false);

    AudioAttribute audioAttribute = getAudioAttribute(REGULAR);
    if (audioAttribute == null) {
      audioAttribute = getAudioAttribute(SLOW);
    }

    if (audioAttribute != null) {
      boolean isDisplayed = maleDisplayed.contains(audioAttribute) || femaleDisplayed.contains(audioAttribute);
      addNewOrYourRecordingTab(tabPanel, audioAttribute, isDisplayed);
    }

    addAudioByGender(audioAttributeExercise, tabPanel, true, maleDisplayed);
    addAudioByGender(audioAttributeExercise, tabPanel, false, femaleDisplayed);

    // put at end if not yours
    if (audioAttribute == null) {
      addNewOrYourRecordingTab(tabPanel, audioAttribute, false);
    }

    tabPanel.addShowHandler(showEvent -> currentTab = tabLinks.indexOf(showEvent.getTarget()));

    tabPanel.selectTab(currentTab);

    return tabPanel;
  }

  /**
   * TODO redo preferred voices...
   *
   * @param exercise
   * @param isMale
   * @return
   */
  private Collection<AudioAttribute> getDisplayedAudio(AudioRefExercise exercise, boolean isMale) {
    //Set<Integer> preferredVoices = controller.getProps().getPreferredVoices();
    Map<MiniUser, List<AudioAttribute>> malesMap = exercise.getMostRecentAudio(isMale, Collections.emptyList(), false);
    List<MiniUser> maleUsers = exercise.getSortedUsers(malesMap);
    return maleUsers.isEmpty() ? Collections.emptyList() : malesMap.get(maleUsers.get(0));
  }

  /**
   * Be sure to make it clear when the audio under the new audio tab is yours.
   *
   * @param tabPanel
   * @see
   */
  private void addNewOrYourRecordingTab(TabPanel tabPanel, AudioAttribute audioAttribute, boolean isDisplayed) {
    String addAudio = (audioAttribute == null) ? ADD_AUDIO : YOUR_RECORDING;
    RememberTabAndContent tabAndContent = getRememberTabAndContent(tabPanel, addAudio, false, false, isDisplayed);
    tabAndContent.getContent().add(getRecordingWidget());
    tabAndContent.getTab().setIcon(IconType.PLUS);
  }

  /**
   * @param audioAttributeExercise
   * @param tabPanel
   * @param isMale
   * @param displayed
   * @see #makeAudioRow
   */
  private void addAudioByGender(AudioRefExercise audioAttributeExercise,
                                TabPanel tabPanel,
                                boolean isMale,
                                Collection<AudioAttribute> displayed) {
    Map<MiniUser, List<AudioAttribute>> malesMap = audioAttributeExercise.getUserMap(isMale, false);
    List<MiniUser> maleUsers = audioAttributeExercise.getSortedUsers(malesMap);
    addTabsForUsers(newUserExercise, tabPanel, malesMap, maleUsers, displayed);
  }

  private DivWidget getRecordingWidget() {
//    DivWidget widget = new DivWidget();
//    widget.add(getRecordAudioWithAnno(widget, AudioType.REGULAR));
//    widget.add(getRecordAudioWithAnno(widget, AudioType.SLOW));
    DivWidget widget = new MyDivWidget();

    panels.clear();

    widget.add(getRecordAudioWithAnno(widget, REGULAR));
    widget.add(getRecordAudioWithAnno(widget, SLOW));

    return widget;
  }

  private final List<MyRecordAudioPanel> panels = new ArrayList<>();

  private Panel getRecordAudioWithAnno(DivWidget widget, AudioType audioTypeRegular) {
    MyRecordAudioPanel w = new MyRecordAudioPanel(widget, audioTypeRegular);

    panels.add(w);

    if (w.getAudioAttribute() == null) {
      return w;
    } else {
      Panel vert = new VerticalPanel();
      vert.add(w);
      Widget commentLine = getCommentLine(newUserExercise, w.getAudioAttribute());
      w.setComment(commentLine);
      if (commentLine != null) {
        vert.add(commentLine);
      }
      return vert;
    }
  }

  /**
   * Worries about context type audio too.
   *
   * @return
   * @see #makeAudioRow
   */
  public AudioAttribute getAudioAttribute(AudioType audioType) {
    AudioAttribute audioAttribute =
        audioType.equals(REGULAR) ?
            newUserExercise.getRecordingsBy(controller.getUser(), true) :
            audioType.equals(SLOW) ?
                newUserExercise.getRecordingsBy(controller.getUser(), false) : null;

    if (audioType.isContext()) {
      for (AudioAttribute audioAttribute1 : newUserExercise.getAudioAttributes()) {
        Map<String, String> attributes = audioAttribute1.getAttributes();
        if (attributes.containsKey("context") && audioAttribute1.getUserid() == controller.getUser()) {
          return audioAttribute1;
        }
      }
      return null;
    } else {
      return audioAttribute;
    }
  }

  /**
   * Make tabs in order of users
   *
   * @param commonExercise
   * @param tabPanel
   * @param userToAudio
   * @param users
   * @see #makeAudioRow
   */
  private <X extends ClientExercise & AnnotationExercise> void addTabsForUsers(X commonExercise,
                                                                               TabPanel tabPanel,
                                                                               Map<MiniUser, List<AudioAttribute>> userToAudio,
                                                                               List<MiniUser> users,
                                                                               Collection<AudioAttribute> displayed) {
    int me = controller.getUser();
//    UserTitle userTitle = new UserTitle();
    for (MiniUser user : users) {
//      boolean byMe = (user.getID() == me);
//      if (!byMe) {
//        String tabTitle = userTitle.getUserTitle(me, user);
//
//        RememberTabAndContent tabAndContent = getRememberTabAndContent(tabPanel, tabTitle, true, true);
      boolean byMe = (user.getID() == me);
      if (!byMe) {
        List<AudioAttribute> audioAttributes = userToAudio.get(user);

        if (!audioAttributes.isEmpty()) {
          String tabTitle = getUserTitle(me, user);

          boolean isDisplayed = false;

//          logger.info("examining " + audioAttributes.size() + " for displayed " + displayed.size());
          for (AudioAttribute audio : audioAttributes) {
            boolean contains = displayed.contains(audio);
            //          logger.info("\tdisplayed contains " + audio.getID() + " = " + contains);

            isDisplayed |= contains;
          }

          RememberTabAndContent tabAndContent = getRememberTabAndContent(tabPanel, tabTitle, true, true, isDisplayed);

          boolean allHaveBeenPlayed = true;

          for (AudioAttribute audio : audioAttributes) {
            if (!audio.isHasBeenPlayed()) {
              allHaveBeenPlayed = false;
            }
            Widget panelForAudio = getPanelForAudio(commonExercise, audio, tabAndContent);
            if (audioAttributes.size() == 2 && audioAttributes.indexOf(audio) == 0) {
              panelForAudio.addStyleName("bottomFiveMargin");
            }
            tabAndContent.getContent().add(panelForAudio);
            if (audio.isHasBeenPlayed()) {
              audioWasPlayed.add(panelForAudio);
            }
          }

          if (allHaveBeenPlayed) {
            tabAndContent.getTab().setIcon(IconType.CHECK_SIGN);
          }
        }
      }
    }
  }

  private final List<TabLink> tabLinks = new ArrayList<>();

  private RememberTabAndContent getRememberTabAndContent(TabPanel tabPanel,
                                                         String tabTitle,
                                                         boolean addRightMargin,
                                                         boolean isCheckable, boolean appendEye) {
    RememberTabAndContent tabAndContent = new RememberTabAndContent(IconType.QUESTION_SIGN, tabTitle, isCheckable);
    TabLink child = tabAndContent.getTab().asTabLink();

    if (appendEye) {
      child.add(new Icon(IconType.EYE_OPEN));
      addTooltip(child, "This audio is visible.");
    }

    tabPanel.add(child);
    tabLinks.add(child);
    tabs.add(tabAndContent);

    // TODO : when do we need this???
    if (addRightMargin) {
      tabAndContent.getContent().getElement().getStyle().setMarginRight(70, Style.Unit.PX);
    }

//    if (appendEye) {
//      tabAndContent.getContent().addStyleName("checkboxGreen");
//    }
    return tabAndContent;
  }

  private String getUserTitle(int me, MiniUser user) {
/*    long id = user.getID();
    if (id == UserDAO.DEFAULT_USER_ID) return GoodwaveExercisePanel.DEFAULT_SPEAKER;
    else if (id == UserDAO.DEFAULT_MALE_ID) return "Default Male";
    else if (id == UserDAO.DEFAULT_FEMALE_ID) return "Default Female";
    else*/

    return
        (user.getID() == me) ? "by You (" + user.getUserID() + ")" : getUserTitle(user);
  }

  private String getUserTitle(MiniUser user) {
    String suffix = "";//user.getAge() < 99 ? " age " + user.getAge() : "";
    String userid = " (" + user.getUserID() + ")";
    return (user.isMale() ? "male" : "female") +
        userid +
        suffix;
  }

  /**
   * Don't warn user to check if audio is consistent if there isn't any.
   *
   * @return
   */
/*  @Override
  boolean checkForForeignChange() {
    boolean didChange = super.checkForForeignChange();

    if (didChange) {
      if (hasAudio()) {
        setupPopover(keepAudio, getWarningHeader(), getWarningForFL(), Placement.TOP, DELAY_MILLIS, false, true);
      }
    }

    return didChange;
  }*/
  private <X extends CommonShell & AnnotationExercise> Widget getPanelForAudio(final X exercise,
                                                                               final AudioAttribute audio,
                                                                               RememberTabAndContent tabAndContent) {
    String audioRef = audio.getAudioRef();
    if (audioRef != null) {
      audioRef = CompressedAudio.getPathNoSlashChange(audioRef);   // todo why do we have to do this?
    }
    final ASRScoringAudioPanel audioPanel = new ASRScoringAudioPanel<X>(audioRef, exercise.getFLToShow(),
        //exercise.getTransliteration(),
        "",
        controller,
        controller.getProps().showSpectrogram(), 70, audio.isRegularSpeed() ? REGULAR_SPEED : SLOW_SPEED,
        exercise
    ) {
      //audio.isRegularSpeed() ? Result.AUDIO_TYPE_REGULAR : Result.AUDIO_TYPE_SLOW) {

      /**
       * @see mitll.langtest.client.scoring.AudioPanel#addWidgets(String, String)
       * @return
       */
      @Override
      protected Widget getAfterPlayWidget() {
        return getDeleteButton(this, audio, exercise, "Delete this audio cut.  Original recorder can re-record.");
      }
    };
    audioPanel.setShowColor(true);
    audioPanel.getElement().setId("ASRScoringAudioPanel");
    noteAudioHasBeenPlayed(exercise, audio, audioPanel);
    tabAndContent.addWidget(audioPanel);
    //  toResize.add(audioPanel);

    Panel vert = new VerticalPanel();
    vert.add(audioPanel);
    Widget commentLine = getCommentLine(exercise, audio);
    if (commentLine != null) {
      vert.add(commentLine);
    }

    return vert;
  }

  /**
   * @param widgets
   * @param audio
   * @param exercise
   * @param tip
   * @return
   * @see #getPanelForAudio
   */
  private Widget getDeleteButton(final Panel widgets, final AudioAttribute audio, final HasID exercise, String tip) {
    return getDeleteButton(tip, event -> {
      logger.info("marking audio defect for " + audio + " on " + exercise.getID());
      controller.getQCService().markAudioDefect(audio, exercise, new AsyncCallback<Void>() {    // delete comment too?
        @Override
        public void onFailure(Throwable caught) {
          controller.handleNonFatalError(MARKING_AUDIO_DEFECT, caught);
        }

        @Override
        public void onSuccess(Void result) {
          widgets.getParent().setVisible(false);
          //   reloadLearnList();
          LangTest.EVENT_BUS.fireEvent(new AudioChangedEvent(instance.toString()));
          // TODO : need to update other lists too?
        }
      });
    });
  }

  private Button getDeleteButton(String tip, ClickHandler handler) {
    Button delete = new Button(DELETE_AUDIO);
    addTooltip(delete, tip);
    delete.addStyleName("leftFiveMargin");
    delete.setType(ButtonType.WARNING);
    delete.setIcon(IconType.REMOVE);
    delete.addClickHandler(handler);
    return delete;
  }

  /**
   * @param id
   * @param audio
   * @param audioPanel
   * @see #getPanelForAudio
   */
  private void noteAudioHasBeenPlayed(final HasID id, final AudioAttribute audio, final ASRScoringAudioPanel audioPanel) {
    audioPanel.addPlayListener(new PlayListener() {
      @Override
      public void playStarted() {
        audioWasPlayed.add(audioPanel);
        logger.info("now audio was played is size " + audioWasPlayed.size());
        for (RememberTabAndContent tabAndContent : tabs) {
          tabAndContent.checkAllPlayed(audioWasPlayed);
        }
        controller.logEvent(audioPanel, "qcPlayAudio", id.getID(), audio.getAudioRef());
      }

      @Override
      public void playStopped() {
      }
    });
  }

  /**
   * @param e
   * @param audio
   * @return
   * @see #getPanelForAudio
   */
  private <E extends AnnotationExercise> Widget getCommentLine(E e, AudioAttribute audio) {
    ExerciseAnnotation audioAnnotation = e.getAnnotation(audio.getAudioRef());
//    logger.info("annotation for " + audio.getAudioRef() + " is " + audioAnnotation);
    if (audioAnnotation != null && !audioAnnotation.isCorrect()) {
      HTML child = new HTML(audioAnnotation.getComment().isEmpty() ? "EMPTY COMMENT" : audioAnnotation.getComment());
      child.getElement().getStyle().setFontSize(14, Style.Unit.PX);
      child.getElement().getStyle().setFontWeight(Style.FontWeight.BOLD);
      return child;
    } else {
//      Map<String, ExerciseAnnotation> fieldToAnnotation = e.getFieldToAnnotation();
//      for (Map.Entry<String, ExerciseAnnotation> pair : fieldToAnnotation.entrySet()) {
//        logger.info("found " + pair.getKey() + " : " + pair.getValue());
//      }
      return null;
    }
  }

  @Override
  protected boolean shouldDisableNext() {
    return false;
  }

  /**
   * Three buttons - previous, next, mark fixed.
   * <p>
   * Add a fixed button, so we know when to clear the comments and remove this item from the reviewed list.
   *
   * @param toAddTo
   * @param normalSpeedRecording
   * @return
   * @see #addFields
   */
  @Override
  protected Panel getCreateButton(final Panel toAddTo,
                                  final ControlGroup normalSpeedRecording) {
    Panel row = new DivWidget();
    row.addStyleName("marginBottomTen");

//    PrevNextList prevNext = getPrevNext(pagingContainer);
//    prevNext.addStyleName("floatLeftAndClear");
//    row.add(prevNext);

 /*   if (newUserExercise.isPredefined()) {//getCombinedMutableUserExercise().checkPredef()) {   // for now, only the owner of the list can remove or add to their list
//    if (newUserExercise.getCombinedMutableUserExercise().checkPredef()) {   // for now, only the owner of the list can remove or add to their list
      //    row.add(getRemove());
      row.add(getDuplicate());
    }
*/
    row.add(getFixedButton(toAddTo, normalSpeedRecording));
    boolean keepAudioSelection = getKeepAudioSelection();

    //   logger.info("value is  " + keepAudioSelection);
    keepAudio.setValue(keepAudioSelection);
    keepAudio.addClickHandler(clickEvent -> storeKeepAudio(keepAudio.getValue()));
    keepAudio.getElement().getStyle().setMarginLeft(5, Style.Unit.PX);
    row.add(keepAudio);
    configureButtonRow(row);

    return row;
  }

  private Button getFixedButton(final Panel toAddTo, final ControlGroup normalSpeedRecording) {
    final Button fixed = makeFixedButton();
    fixed.addClickHandler(event -> validateThenPost(rap, normalSpeedRecording, toAddTo, true));
    return fixed;
  }

  /**
   * @param toAddTo
   * @param onClick
   * @seex mitll.langtest.client.custom.MarkDefectsChapterNPFHelper#addEventHandler
   * @see NewUserExercise#validateThenPost(RecordAudioPanel, ControlGroup, Panel, boolean)
   */
  @Override
  void afterValidForeignPhrase(final Panel toAddTo, boolean onClick) {
    super.afterValidForeignPhrase(toAddTo, onClick);
    LangTest.EVENT_BUS.fireEvent(new DefectEvent(instance.toString()));
  }

/*  private Button getRemove() {
    Button remove = new Button(DELETE);
    remove.setType(ButtonType.WARNING);
    remove.addStyleName("floatRight");
    remove.addStyleName("leftFiveMargin");
    addTooltip(remove, DELETE_THIS_ITEM);

    remove.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        confirmThenDeleteItem();
      }
    });
    return remove;
  }*/

/*  private void confirmThenDeleteItem() {
    DialogHelper dialogHelper = new DialogHelper(true);
    dialogHelper.show(ARE_YOU_SURE, MSGS, new DialogHelper.CloseListener() {
      @Override
      public void gotYes() {
        controller.getQCService().deleteItem(newUserExercise.getID(), new AsyncCallback<Boolean>() {
          @Override
          public void onFailure(Throwable caught) {
          }

          @Override
          public void onSuccess(Boolean result) {
            exerciseList.removeExercise(newUserExercise);
            originalList.remove(newUserExercise.getID());
          }
        });
      }

      @Override
      public void gotNo() {
      }
    });
  }*/

  /**
   * @return
   */
/*  private Button getDuplicate() {
    final Button duplicate = new Button(DUPLICATE);
    duplicate.setType(ButtonType.SUCCESS);
    duplicate.addStyleName("floatRight");
    addTooltip(duplicate, COPY_THIS_ITEM);

    duplicate.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        duplicateExercise(duplicate);
      }
    });
    return duplicate;
  }*/

  /**
   * Disable the button initially so we don't accidentally duplicate twice.
   *
   * @param duplicate
   */
 /* private void duplicateExercise(final Button duplicate) {
    duplicate.setEnabled(false);
    newUserExercise.getMutable().setCreator(controller.getUser());
    T commonShell = exerciseList.byHasID(newUserExercise);
    if (commonShell != null) {
      newUserExercise.setState(commonShell.getState());
      newUserExercise.setSecondState(commonShell.getSecondState());
    }
    //logger.info("to duplicate " + newUserExercise + " state " + newUserExercise.getState());
    listService.duplicateExercise(newUserExercise, new AsyncCallback<U>() {
      @Override
      public void onFailure(Throwable caught) {
        duplicate.setEnabled(true);
      }

      @Override
      public void onSuccess(U result) {
        duplicate.setEnabled(true);

        exerciseList.addExerciseAfter(newUserExercise, result);
        exerciseList.redraw();
        originalList.addExerciseAfter(newUserExercise, result);
      }
    });
  }*/

  /**
   * @return
   * @see NewUserExercise#getCreateButton(Panel, ControlGroup)
   */
  private Button makeFixedButton() {
    Button fixed = new Button(FIXED);
    fixed.setType(ButtonType.PRIMARY);

    fixed.addStyleName("leftTenMargin");
    fixed.addStyleName("floatLeftAndClear");
    fixed.addStyleName("marginRight");

    // fixed.addMouseOverHandler(event -> checkForForeignChange());
    addTooltip(fixed, MARK_FIXED_TOOLTIP);
    return fixed;
  }

  /**
   * @see mitll.langtest.client.custom.dialog.NewUserExercise.CreateFirstRecordAudioPanel#makePostAudioRecordButton
   */
  @Override
  protected void audioPosted() {
    reallyChange(false, getKeepAudio());
  }

  /**
   * @return
   * @see #postChangeIfDirty
   */
  protected boolean getKeepAudio() {
    return keepAudio.getValue();
  }

  private boolean getKeepAudioSelection() {
    return getKeepAudioSelection(getSelectedUserKey(controller, ""));
  }

  private boolean getKeepAudioSelection(String selectedUserKey) {
    if (Storage.isLocalStorageSupported()) {
      Storage localStorageIfSupported = Storage.getLocalStorageIfSupported();
      String item = localStorageIfSupported.getItem(selectedUserKey);
      // logger.info("value for " + selectedUserKey + "='" + item+ "'");
      if (item != null) {
        return item.toLowerCase().equals("true");
      } else {
        storeKeepAudio(true);
        return true;
      }
    }
    // else {
    return false;
    // }
  }

  private String getSelectedUserKey(ExerciseController controller, String appTitle) {
    return getStoragePrefix(controller, appTitle) + "keepAudio";
  }

  private String getStoragePrefix(ExerciseController controller, String appTitle) {
    return appTitle + ":" + controller.getUser() + ":";
  }

  private void storeKeepAudio(boolean shouldKeepAudio) {
    if (Storage.isLocalStorageSupported()) {
      Storage localStorageIfSupported = Storage.getLocalStorageIfSupported();
      localStorageIfSupported.setItem(getSelectedUserKey(controller, ""), "" + shouldKeepAudio);
    }
  }

  /**
   * TODOx : why do we have to do a sequence of server calls -- how about just one???
   *
   * @param buttonClicked
   * @seex #doAfterEditComplete(mitll.langtest.client.list.ListInterface, boolean)
   * @seex #postEditItem
   * @see #reallyChange
   */
  @Override
  protected void doAfterEditComplete(boolean buttonClicked) {
    super.doAfterEditComplete(buttonClicked);
    //changeTooltip(pagingContainer);
    if (buttonClicked) {
      userSaidExerciseIsFixed();
    }
  }

  private void userSaidExerciseIsFixed() {
    final int id = newUserExercise.getID();
    controller.getQCService().markState(id, STATE.FIXED, new AsyncCallback<Void>() {
      @Override
      public void onFailure(Throwable caught) {
      }

      @Override
      public void onSuccess(Void result) {
        logger.info("doAfterEditComplete : forgetting exercise " + id);
        exerciseList.forgetExercise(id);
      }
    });
  }

  @Override
  protected String getEnglishLabel() {
    return controller.getLanguage().equalsIgnoreCase("english") ? "Meaning<br/>" : "English<br/>";
  }

  @Override
  protected String getTransliterationLabel() {
    return TRANSLITERATION;
  }

  private class MyDivWidget extends DivWidget implements BusyPanel {
    @Override
    public boolean isBusy() {
      return false;
    }

    @Override
    public void setBusy(boolean v) {
      //  logger.info("this " + this.getClass() + " got a busy " + v);
      for (RecordAudioPanel ap : panels) {
        if (!ap.isRecording()) {
          ap.setEnabled(!v);
        } else {
          ap.setEnabled(v);
        }
      }
    }
  }

  private class MyRecordAudioPanel extends RecordAudioPanel implements BusyPanel {
    private Button deleteButton;
    private Widget comment;

    MyRecordAudioPanel(DivWidget widget, AudioType audioType) {
      super(ReviewEditableExercise.this.newUserExercise, ReviewEditableExercise.this.controller, widget,
          0, false, audioType);
      this.audioType = audioType;
    }

    public void setComment(Widget comment) {
      this.comment = comment;
    }

    protected WaveformPostAudioRecordButton makePostAudioRecordButton(AudioType audioType, final String recordButtonTitle) {
      return new MyWaveformPostAudioRecordButton(audioType, recordButtonTitle) {
        @Override
        public void useResult(AudioAnswer result) {
          super.useResult(result);
          if (result.isValid()) {
            newUserExercise.getMutableAudio().addAudio(result.getAudioAttribute());
            deleteButton.setEnabled(true);

            LangTest.EVENT_BUS.fireEvent(new AudioChangedEvent(instance.toString()));
          }
        }
      };
    }

    /**
     * TODO reconsider audio changed event...
     *
     * @return
     * @see mitll.langtest.client.scoring.AudioPanel#addWidgets(String, String)
     */
    @Override
    protected Widget getAfterPlayWidget() {
      ClickHandler handler = event -> {
        deleteButton.setEnabled(false);
        int id = exercise.getID();
        logger.info("marking audio defect for " + getAudioAttribute() + " on " + id);
        controller.getQCService().markAudioDefect(getAudioAttribute(), exercise, new AsyncCallback<Void>() {    // delete comment too?
          @Override
          public void onFailure(Throwable caught) {
            controller.getMessageHelper().handleNonFatalError(MARKING_AUDIO_DEFECT, caught);
          }

          @Override
          public void onSuccess(Void result) {
            getWaveform().setVisible(false);
            setEnabled(false);
            if (comment != null) {
              comment.setVisible(false);
            }
            LangTest.EVENT_BUS.fireEvent(new AudioChangedEvent(instance.toString()));
          }
        });
      };

      deleteButton = getDeleteButton(DELETE_THIS_AUDIO_CUT, handler);
      if (getAudioAttribute() == null) deleteButton.setEnabled(false);
      return deleteButton;
    }

    @Override
    protected float getWaveformHeight() {
      return 60;
    }

    @Override
    public boolean isBusy() {
      return false;
    }

    @Override
    public void setBusy(boolean v) {
      logger.info("this " + this.getClass() + " got a busy " + v);
    }
  }
}
