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

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.CheckBox;
import com.github.gwtbootstrap.client.ui.*;
import com.github.gwtbootstrap.client.ui.TabPanel;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.base.TextBoxBase;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.github.gwtbootstrap.client.ui.constants.Placement;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.media.client.Audio;
import com.google.gwt.storage.client.Storage;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.*;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.custom.ReloadableContainer;
import mitll.langtest.client.custom.tabs.RememberTabAndContent;
import mitll.langtest.client.dialog.DialogHelper;
import mitll.langtest.client.exercise.BusyPanel;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.RecordAudioPanel;
import mitll.langtest.client.exercise.WaveformPostAudioRecordButton;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.client.list.PagingExerciseList;
import mitll.langtest.client.list.Reloadable;
import mitll.langtest.client.scoring.ASRScoringAudioPanel;
import mitll.langtest.client.scoring.EmptyScoreListener;
import mitll.langtest.client.scoring.GoodwaveExercisePanel;
import mitll.langtest.client.sound.CompressedAudio;
import mitll.langtest.client.sound.PlayListener;
import mitll.langtest.client.user.FormField;
import mitll.langtest.server.database.UserDAO;
import mitll.langtest.shared.AudioAnswer;
import mitll.langtest.shared.ExerciseAnnotation;
import mitll.langtest.shared.MiniUser;
import mitll.langtest.shared.Result;
import mitll.langtest.shared.custom.UserList;
import mitll.langtest.shared.exercise.*;

import java.util.*;
import java.util.logging.Logger;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 3/28/2014.
 */
public class ReviewEditableExercise extends EditableExerciseDialog {
  final Logger logger = Logger.getLogger("ReviewEditableExercise");

  private static final String YOUR_RECORDING = "Your Recording";
  private static final String DELETE_AUDIO = "Delete Audio";
  private static final String DELETE_THIS_AUDIO_CUT = "Delete this audio cut.";
  private static final String TRANSLITERATION = "Transliteration";

  private static final String MARK_FIXED_TOOLTIP = "Mark item as fixed, removed defective audio, and remove item from the review list.";

  /**
   * @see #makeFixedButton
   */
  private static final String FIXED = "Mark Fixed";
  private static final String DUPLICATE = "Duplicate";
  /**
   * @see #getRemove
   */
  private static final String DELETE = "Delete";
  private static final String DELETE_THIS_ITEM = "Delete this item.";
  private static final String ARE_YOU_SURE = "Are you sure?";
  private static final String REALLY_DELETE_ITEM = "Really delete whole item and all audio cuts?";
  private static final List<String> MSGS = Collections.singletonList(REALLY_DELETE_ITEM);
  private static final String COPY_THIS_ITEM = "Copy this item.";
  private static final String REGULAR_SPEED = " Regular speed";
  private static final String SLOW_SPEED = " Slow speed";
  private static final int DELAY_MILLIS = 5000;

  /**
   * @see #makeAudioRow
   */
  private static final String ADD_AUDIO = "Add audio";
  private static final String MALE = "Male";
  private static final String FEMALE = "Female";

  private final PagingExerciseList<CommonShell, CommonExercise> exerciseList;

  private FormField context;
  private FormField contextTrans;
  private final HTML contextAnno = new HTML();
  private final HTML contextTransAnno = new HTML();
  private String originalContext = "";
  private String originalContextTrans = "";
  private List<RememberTabAndContent> tabs;
  private CheckBox keepAudio = new CheckBox("Keep Audio even if text changes");

  /**
   * @param itemMarker
   * @param changedUserExercise
   * @param originalList
   * @param exerciseList
   * @paramx predefinedContent   - this should be a reference to the Learn tab exercise list, but it's not getting set.
   * @see mitll.langtest.client.custom.content.ReviewItemHelper#doInternalLayout(mitll.langtest.shared.custom.UserList, String)
   */
  public ReviewEditableExercise(LangTestDatabaseAsync service,
                                ExerciseController controller,
                                HasText itemMarker,
                                CommonExercise changedUserExercise,

                                UserList<CommonShell> originalList,
                                PagingExerciseList<CommonShell, CommonExercise> exerciseList,
                                ReloadableContainer predefinedContent,
                                String instanceName) {
    super(service, controller,
        null,
        itemMarker, changedUserExercise, originalList, exerciseList,
        predefinedContent,
        instanceName);
    this.exerciseList = exerciseList;
  }

  /**
   * TODO should move this stuff into a class that handles these basic operations
   *
   * @param newUserExercise
   * @param <S>
   */
  @Override
  public <S extends CommonShell & AudioRefExercise & AnnotationExercise> void setFields(S newUserExercise) {
    super.setFields(newUserExercise);

    final com.github.gwtbootstrap.client.ui.base.TextBoxBase box = context.box;

    box.setText(originalContext = newUserExercise.getContext());

    box.addBlurHandler(new BlurHandler() {
      @Override
      public void onBlur(BlurEvent event) {
        gotBlur();
        try {
          long uniqueID = originalList.getUniqueID();
          controller.logEvent(box, "TextBox", "UserList_" + uniqueID, "ContextBox = " + box.getValue());
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    });

    useAnnotation(newUserExercise, "context", contextAnno);
    useAnnotation(newUserExercise, "context translation", contextTransAnno);

    TextBoxBase box1 = contextTrans.box;
    box1.setText(originalContextTrans = newUserExercise.getContextTranslation());
    box1.addBlurHandler(new BlurHandler() {
      @Override
      public void onBlur(BlurEvent event) {
        gotBlur();
        try {
          long uniqueID = originalList.getUniqueID();
          controller.logEvent(box1, "TextBox", "UserList_" + uniqueID, "ContextTransBox = " + box1.getValue());
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    });
  }

  void grabInfoFromFormAndStuffInfoExercise(MutableExercise mutableExercise) {
    super.grabInfoFromFormAndStuffInfoExercise(mutableExercise);
    mutableExercise.setContext(context.getSafeText());
    mutableExercise.setContextTranslation(contextTrans.getSafeText());
  }

  protected void makeOptionalRows(DivWidget upper) {
    makeContextRow(upper);
    makeContextTransRow(upper);
  }

  private void makeContextRow(Panel container) {
    Panel row = new FluidRow();
    container.add(row);
    context = makeBoxAndAnno(row, "Context", "", contextAnno);
  }

  private void makeContextTransRow(Panel container) {
    Panel row = new FluidRow();
    container.add(row);
    contextTrans = makeBoxAndAnno(row, "Context Translation", "", contextTransAnno);
  }

  private int currentTab = 0;

  /**
   * @return
   * @see #addNew
   */
  @Override
  protected Panel makeAudioRow() {
    AudioAttributeExercise audioAttributeExercise = newUserExercise;

    tabs = new ArrayList<>();

    TabPanel tabPanel = new TabPanel();

    tabLinks.clear();

    Collection<AudioAttribute> maleDisplayed = getDisplayedAudio(audioAttributeExercise, true);
    Collection<AudioAttribute> femaleDisplayed = getDisplayedAudio(audioAttributeExercise, false);

    AudioAttribute audioAttribute = getAudioAttribute(Result.AUDIO_TYPE_REGULAR);
    if (audioAttribute == null) {
      audioAttribute = getAudioAttribute(Result.AUDIO_TYPE_SLOW);
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


    tabPanel.addShowHandler(new TabPanel.ShowEvent.Handler() {
      @Override
      public void onShow(TabPanel.ShowEvent showEvent) {
        currentTab = tabLinks.indexOf(showEvent.getTarget());
      }
    });

    tabPanel.selectTab(currentTab);

    return tabPanel;
  }

  private Collection<AudioAttribute> getDisplayedAudio(AudioAttributeExercise exercise, boolean isMale) {
    Set<Long> preferredVoices = controller.getProps().getPreferredVoices();
    Map<MiniUser, List<AudioAttribute>> malesMap = exercise.getMostRecentAudio(isMale, preferredVoices);
    List<MiniUser> maleUsers = exercise.getSortedUsers(malesMap);
    boolean maleEmpty = maleUsers.isEmpty();

    return maleEmpty ?
        Collections.emptyList() : malesMap.get(maleUsers.get(0));
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
  private void addAudioByGender(AudioAttributeExercise audioAttributeExercise,
                                TabPanel tabPanel,
                                boolean isMale,
                                Collection<AudioAttribute> displayed) {
    Map<MiniUser, List<AudioAttribute>> malesMap = audioAttributeExercise.getUserMap(isMale);
    List<MiniUser> maleUsers = audioAttributeExercise.getSortedUsers(malesMap);
    addTabsForUsers(newUserExercise, tabPanel, malesMap, maleUsers, displayed);
  }

  private DivWidget getRecordingWidget() {
    DivWidget widget = new MyDivWidget();

    panels.clear();

    Panel recordAudioWithAnno = getRecordAudioWithAnno(widget, Result.AUDIO_TYPE_REGULAR);
    widget.add(recordAudioWithAnno);
    widget.add(getRecordAudioWithAnno(widget, Result.AUDIO_TYPE_SLOW));

    return widget;
  }

  private List<MyRecordAudioPanel> panels = new ArrayList<>();

  private Panel getRecordAudioWithAnno(DivWidget widget, String audioTypeRegular) {
    MyRecordAudioPanel w = new MyRecordAudioPanel(widget, audioTypeRegular, instance);

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
  public AudioAttribute getAudioAttribute(String audioType) {
    AudioAttribute audioAttribute =
        audioType.equals(Result.AUDIO_TYPE_REGULAR) ?
            newUserExercise.getRecordingsBy(controller.getUser(), true) :
            audioType.equals(Result.AUDIO_TYPE_SLOW) ?
                newUserExercise.getRecordingsBy(controller.getUser(), false) : null;

    if (audioType.startsWith("context")) {
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
  private <X extends CommonShell & AnnotationExercise> void addTabsForUsers(X commonExercise,
                                                                            TabPanel tabPanel,
                                                                            Map<MiniUser, List<AudioAttribute>> userToAudio,
                                                                            List<MiniUser> users,
                                                                            Collection<AudioAttribute> displayed) {
    int me = controller.getUser();
    for (MiniUser user : users) {
      boolean byMe = (user.getId() == me);
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

  private List<TabLink> tabLinks = new ArrayList<>();

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
    long id = user.getId();
    if (id == UserDAO.DEFAULT_USER_ID) return GoodwaveExercisePanel.DEFAULT_SPEAKER;
    else if (id == UserDAO.DEFAULT_MALE_ID) return "Default Male";
    else if (id == UserDAO.DEFAULT_FEMALE_ID) return "Default Female";
    else return
          (user.getId() == me) ? "by You (" + user.getUserID() + ")" : getUserTitle(user);
  }

  private String getUserTitle(MiniUser user) {
    String suffix = user.getAge() < 99 ? " age " + user.getAge() : "";
    String userid = " (" + user.getUserID() + ")";
    return (user.isMale() ? MALE : FEMALE) +
        userid +
        suffix;
  }

  /**
   * Don't warn user to check if audio is consistent if there isn't any.
   *
   * @return
   */
  @Override
  boolean checkForForeignChange() {
    boolean didChange = super.checkForForeignChange();

    if (didChange) {
      if (hasAudio()) {
        setupPopover(keepAudio, getWarningHeader(), getWarningForFL(), Placement.TOP, DELAY_MILLIS, false);
      }
    }

    return didChange;
  }

  private final Set<Widget> audioWasPlayed = new HashSet<>();

  private <X extends CommonShell & AnnotationExercise> Widget getPanelForAudio(final X exercise,
                                                                               final AudioAttribute audio,
                                                                               RememberTabAndContent tabAndContent) {
    String audioRef = audio.getAudioRef();
    if (audioRef != null) {
      audioRef = CompressedAudio.getPathNoSlashChange(audioRef);   // todo why do we have to do this?
    }
    final ASRScoringAudioPanel audioPanel = new ASRScoringAudioPanel<X>(audioRef, exercise.getForeignLanguage(), exercise.getTransliteration(), service, controller,
        controller.getProps().showSpectrogram(), new EmptyScoreListener(), 70, audio.isRegularSpeed() ? REGULAR_SPEED : SLOW_SPEED, exercise.getID(),
        exercise, instance,
        audio.isRegularSpeed() ? Result.AUDIO_TYPE_REGULAR : Result.AUDIO_TYPE_SLOW) {
      /**
       * @see mitll.langtest.client.scoring.AudioPanel#addWidgets(String, String, String)
       * @return
       */
      @Override
      protected Widget getAfterPlayWidget() {
        return getDeleteButton(this, audio, exercise.getID(), "Delete this audio cut.  Original recorder can re-record.");
      }
    };
    audioPanel.setShowColor(true);
    audioPanel.getElement().setId("ASRScoringAudioPanel");
    noteAudioHasBeenPlayed(exercise.getID(), audio, audioPanel);
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
   * @param exerciseID
   * @param tip
   * @return
   * @see #getPanelForAudio
   */
  private Widget getDeleteButton(final Panel widgets, final AudioAttribute audio, final String exerciseID, String tip) {
    ClickHandler handler = new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        logger.info("marking audio defect for " + audio + " on " + exerciseID);
        service.markAudioDefect(audio, exerciseID, new AsyncCallback<Void>() {    // delete comment too?
          @Override
          public void onFailure(Throwable caught) {
          }

          @Override
          public void onSuccess(Void result) {
            widgets.getParent().setVisible(false);
            reloadLearnList();
            // TODO : need to update other lists too?
          }
        });
      }
    };

    return getDeleteButton(tip, handler);
  }

  /**
   * @see #doAfterEditComplete(ListInterface, boolean)
   */
  private void reloadLearnList() {
    Reloadable exerciseList = predefinedContentList.getReloadable();
    if (exerciseList != null) {
      exerciseList.clearCachedExercise();
      exerciseList.reload();
    } else {
//      logger.warning("reloadLearnList : no exercise list ref ");
    }
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
  private void noteAudioHasBeenPlayed(final String id, final AudioAttribute audio, final ASRScoringAudioPanel audioPanel) {
    audioPanel.addPlayListener(new PlayListener() {
      @Override
      public void playStarted() {
        audioWasPlayed.add(audioPanel);
        logger.info("now audio was played is size " + audioWasPlayed.size());
        for (RememberTabAndContent tabAndContent : tabs) {
          tabAndContent.checkAllPlayed(audioWasPlayed);
        }
        controller.logEvent(audioPanel, "qcPlayAudio", id, audio.getAudioRef());
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
   * @param ul
   * @param pagingContainer
   * @param toAddTo
   * @param normalSpeedRecording
   * @return
   * @see #addNew
   */
  @Override
  protected Panel getCreateButton(final UserList<CommonShell> ul,
                                  final ListInterface<CommonShell> pagingContainer,
                                  final Panel toAddTo,
                                  final ControlGroup normalSpeedRecording) {
    Panel row = new DivWidget();
    row.addStyleName("marginBottomTen");

    PrevNextList prevNext = getPrevNext(pagingContainer);
    prevNext.addStyleName("floatLeft");
    row.add(prevNext);

    if (newUserExercise.getCombinedMutableUserExercise().checkPredef()) {   // for now, only the owner of the list can remove or add to their list
      row.add(getRemove());
      row.add(getDuplicate());
    }

    row.add(getFixedButton(ul, pagingContainer, toAddTo, normalSpeedRecording));
    boolean keepAudioSelection = getKeepAudioSelection();

    //   logger.info("value is  " + keepAudioSelection);
    keepAudio.setValue(keepAudioSelection);
    keepAudio.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent clickEvent) {
        storeKeepAudio(keepAudio.getValue());
      }
    });
    keepAudio.getElement().getStyle().setMarginLeft(5, Style.Unit.PX);
    row.add(keepAudio);
    configureButtonRow(row);

    return row;
  }

  private Button getFixedButton(final UserList<CommonShell> ul,
                                final ListInterface<CommonShell> pagingContainer,
                                final Panel toAddTo,
                                final ControlGroup normalSpeedRecording) {
    final Button fixed = makeFixedButton();
    fixed.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        validateThenPost(foreignLang, rap, normalSpeedRecording, ul, pagingContainer, toAddTo, true, true);
      }
    });
    return fixed;
  }

  private Button getRemove() {
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
  }

  private void confirmThenDeleteItem() {
    DialogHelper dialogHelper = new DialogHelper(true);
    dialogHelper.show(ARE_YOU_SURE, MSGS, new DialogHelper.CloseListener() {
      @Override
      public void gotYes() {
        service.deleteItem(newUserExercise.getID(), new AsyncCallback<Boolean>() {
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
  }

  /**
   * @return
   */
  private Button getDuplicate() {
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
  }

  /**
   * Disable the button initially so we don't accidentally duplicate twice.
   *
   * @param duplicate
   */
  private void duplicateExercise(final Button duplicate) {
    duplicate.setEnabled(false);
    newUserExercise.getCombinedMutableUserExercise().setCreator(controller.getUser());
    CommonShell commonShell = exerciseList.byID(newUserExercise.getID());
    if (commonShell != null) {
      newUserExercise.setState(commonShell.getState());
      newUserExercise.setSecondState(commonShell.getSecondState());
    }
    //logger.info("to duplicate " + newUserExercise + " state " + newUserExercise.getState());
    service.duplicateExercise(newUserExercise, new AsyncCallback<CommonExercise>() {
      @Override
      public void onFailure(Throwable caught) {
        duplicate.setEnabled(true);
      }

      @Override
      public void onSuccess(CommonExercise result) {
        duplicate.setEnabled(true);

        exerciseList.addExerciseAfter(newUserExercise, result);
        exerciseList.redraw();
        originalList.addExerciseAfter(newUserExercise, result);
      }
    });
  }

  /**
   * @return
   * @see #getCreateButton(mitll.langtest.shared.custom.UserList, mitll.langtest.client.list.ListInterface, com.google.gwt.user.client.ui.Panel, com.github.gwtbootstrap.client.ui.ControlGroup)
   */
  private Button makeFixedButton() {
    Button fixed = new Button(FIXED);
    fixed.setType(ButtonType.PRIMARY);

    fixed.addStyleName("leftTenMargin");
    fixed.addStyleName("floatLeft");
    fixed.addStyleName("marginRight");

    fixed.addMouseOverHandler(new MouseOverHandler() {
      @Override
      public void onMouseOver(MouseOverEvent event) {
        checkForForeignChange();
      }
    });
    addTooltip(fixed, MARK_FIXED_TOOLTIP);
    return fixed;
  }

  /**
   * @see mitll.langtest.client.custom.dialog.NewUserExercise.CreateFirstRecordAudioPanel#makePostAudioRecordButton
   */
  @Override
  protected void audioPosted() {
    Boolean value = getKeepAudio();
    //  logger.info("did audioPosted keep audio = " + value);
    reallyChange(listInterface, false, value);
  }

  protected boolean getKeepAudio() {
    Boolean value = keepAudio.getValue();
    //  logger.info(this.getClass() + " : did getKeepAudio keep audio = " + value);
    return value;
  }

  /**
   * @see #isValidForeignPhrase(UserList, ListInterface, Panel, boolean)
   */
  @Override
  protected void checkIfNeedsRefAudio() {
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
   * @param pagingContainer
   * @param buttonClicked
   * @seex #doAfterEditComplete(mitll.langtest.client.list.ListInterface, boolean)
   * @see #reallyChange
   * @see #postEditItem
   */
  @Override
  protected void doAfterEditComplete(ListInterface<CommonShell> pagingContainer, boolean buttonClicked) {
    //  super.doAfterEditComplete(pagingContainer, buttonClicked);
    changeTooltip(pagingContainer);
    if (buttonClicked) {
      userSaidExerciseIsFixed();
    }
  }

  private void userSaidExerciseIsFixed() {
    final String id = newUserExercise.getID();
    int user = controller.getUser();
//    logger.info("doAfterEditComplete : forgetting exercise " + id + " current user " + user + " before list had " + ul.getExercises().size());

    if (!ul.remove(newUserExercise)) {
      logger.warning("\ndoAfterEditComplete : error - didn't remove " + id + " from ul " + ul);
    }
    if (!originalList.remove(newUserExercise)) {
      logger.warning("\ndoAfterEditComplete : error - didn't remove " + id + " from original " + originalList);
    }

    service.setExerciseState(id, STATE.FIXED, user, new AsyncCallback<Void>() {
      @Override
      public void onFailure(Throwable caught) {
      }

      @Override
      public void onSuccess(Void result) {
        logger.info("doAfterEditComplete : forgetting exercise " + id);
        exerciseList.forgetExercise(id);

        reloadLearnList();
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

    MyRecordAudioPanel(DivWidget widget, String audioType, String instance) {
      super(ReviewEditableExercise.this.newUserExercise,
          ReviewEditableExercise.this.controller,
          widget,
          ReviewEditableExercise.this.service,
          0, false, audioType, instance);
      this.audioType = audioType;
    }

    public void setComment(Widget comment) {
      this.comment = comment;
    }

    protected WaveformPostAudioRecordButton makePostAudioRecordButton(String audioType, final String recordButtonTitle) {
      return new MyWaveformPostAudioRecordButton(audioType, recordButtonTitle) {
        @Override
        public void useResult(AudioAnswer result) {
          super.useResult(result);
          if (result.isValid()) {
            newUserExercise.getMutableAudio().addAudio(result.getAudioAttribute());
            deleteButton.setEnabled(true);
          }
        }
      };
    }

    /**
     * @return
     * @see mitll.langtest.client.scoring.AudioPanel#addWidgets(String, String, String)
     */
    @Override
    protected Widget getAfterPlayWidget() {
      ClickHandler handler = new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          deleteButton.setEnabled(false);
          logger.info("marking audio defect for " + getAudioAttribute() + " on " + exerciseID);
          service.markAudioDefect(getAudioAttribute(), exerciseID, new AsyncCallback<Void>() {    // delete comment too?
            @Override
            public void onFailure(Throwable caught) {
            }

            @Override
            public void onSuccess(Void result) {
              getWaveform().setVisible(false);
              getPlayButton().setEnabled(false);
              if (comment != null) {
                comment.setVisible(false);
              }

              //     reloadLearnList();
            }
          });
        }
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
